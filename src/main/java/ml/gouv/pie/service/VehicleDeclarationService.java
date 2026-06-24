package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.entity.Dossier;
import ml.gouv.pie.entity.Vehicle;
import ml.gouv.pie.entity.VehicleDeclaration;
import ml.gouv.pie.entity.enums.DossierStatus;
import ml.gouv.pie.entity.enums.NotificationType;
import ml.gouv.pie.entity.enums.VehicleDeclarationType;
import ml.gouv.pie.exception.BusinessException;
import ml.gouv.pie.repository.VehicleDeclarationRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleDeclarationService {

    private final VehicleDeclarationRepository declarationRepository;
    private final DossierService dossierService;
    private final DossierMapperService mapperService;
    private final NotificationService notificationService;
    private final StoredFileService storedFileService;

    @Transactional
    public DtoMapper.DossierDto declareVehicle(String email, Long dossierId,
                                               VehicleDeclarationType declarationType,
                                               MultipartFile file) {
        Dossier dossier = dossierService.getOwnedDossier(email, dossierId);

        if (dossier.getStatus() != DossierStatus.COMPLETED) {
            throw new BusinessException("Seuls les dossiers immatriculés peuvent faire l'objet d'une déclaration");
        }

        if (declarationRepository.existsByDossierId(dossierId)) {
            throw new BusinessException("Une déclaration a déjà été enregistrée pour ce dossier");
        }

        if (file == null || file.isEmpty()) {
            throw new BusinessException("La pièce justificative est obligatoire");
        }

        validateJustificatif(file);

        try {
            Path dir = storedFileService.dossierDirectory(dossier.getReferenceNumber(), "declarations");
            Files.createDirectories(dir);

            String ext = getExtension(file.getOriginalFilename());
            String storedName = declarationType.name().toLowerCase(Locale.ROOT)
                    + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path target = dir.resolve(storedName);
            Files.copy(file.getInputStream(), target);

            VehicleDeclaration declaration = VehicleDeclaration.builder()
                    .dossier(dossier)
                    .declarationType(declarationType)
                    .fileName(file.getOriginalFilename())
                    .filePath(storedFileService.toStoredPath(target))
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .declaredAt(LocalDateTime.now())
                    .build();
            dossier.setVehicleDeclaration(declaration);
            declarationRepository.save(declaration);

            dossier.setStatus(mapStatus(declarationType));
            applyDeclarationSideEffects(dossier, declarationType);

            String label = declarationLabel(declarationType);
            notificationService.create(
                    dossier.getCitizen().getUser(),
                    "Déclaration enregistrée pour le dossier " + dossier.getReferenceNumber() + " : " + label,
                    NotificationType.DOSSIER,
                    false);

            return mapperService.toDto(dossier);
        } catch (IOException e) {
            throw new BusinessException("Erreur lors du téléversement de la pièce justificative",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public VehicleDeclaration getOwnedDeclaration(String email, Long dossierId) {
        dossierService.getOwnedDossier(email, dossierId);
        return declarationRepository.findByDossierId(dossierId)
                .orElseThrow(() -> new BusinessException("Aucune déclaration trouvée", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Resource loadDeclarationResource(VehicleDeclaration declaration) {
        Path path = storedFileService.resolve(declaration.getFilePath());
        if (!Files.isRegularFile(path)) {
            throw new BusinessException("Fichier introuvable", HttpStatus.NOT_FOUND);
        }
        return new FileSystemResource(path);
    }

    public String resolveContentType(VehicleDeclaration declaration) {
        if (declaration.getContentType() != null && !declaration.getContentType().isBlank()) {
            return declaration.getContentType();
        }
        String fileName = declaration.getFileName();
        if (fileName == null) {
            return "application/octet-stream";
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    private void validateJustificatif(MultipartFile file) {
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        boolean pdf = contentType.contains("pdf") || name.endsWith(".pdf");
        boolean image = contentType.startsWith("image/")
                || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                || name.endsWith(".gif") || name.endsWith(".webp");
        if (!pdf && !image) {
            throw new BusinessException("Formats acceptés : PDF ou image (JPG, PNG)");
        }
    }

    private DossierStatus mapStatus(VehicleDeclarationType type) {
        return switch (type) {
            case STOLEN -> DossierStatus.STOLEN;
            case LOST -> DossierStatus.LOST;
            case SOLD -> DossierStatus.SOLD;
        };
    }

    private void applyDeclarationSideEffects(Dossier dossier, VehicleDeclarationType type) {
        if (type != VehicleDeclarationType.SOLD) {
            return;
        }
        Vehicle vehicle = dossier.getVehicle();
        if (vehicle == null) {
            return;
        }
        String chassis = vehicle.getChassisNumber();
        if (chassis != null && !chassis.contains("#SOLD-")) {
            vehicle.setChassisNumber(chassis + "#SOLD-" + dossier.getId());
        }
    }

    private String declarationLabel(VehicleDeclarationType type) {
        return switch (type) {
            case STOLEN -> "Engin volé";
            case LOST -> "Engin perdu";
            case SOLD -> "Engin vendu";
        };
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
