package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.entity.*;
import ml.gouv.pie.entity.enums.DossierStatus;
import ml.gouv.pie.entity.enums.NotificationType;
import ml.gouv.pie.exception.BusinessException;
import ml.gouv.pie.repository.DossierRepository;
import ml.gouv.pie.repository.PlateDeliveryRepository;
import ml.gouv.pie.repository.RegistrationRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlateDeliveryService {

    private static final Set<DossierStatus> ELIGIBLE_STATUSES = EnumSet.of(
            DossierStatus.COMPLETED
    );

    private final PlateDeliveryRepository plateDeliveryRepository;
    private final RegistrationRepository registrationRepository;
    private final DossierRepository dossierRepository;
    private final DossierMapperService mapperService;
    private final NotificationService notificationService;
    private final StoredFileService storedFileService;
    private final DocumentService documentService;

    @Transactional
    public DtoMapper.DossierDto savePlateDelivery(
            Long dossierId,
            String plateNumber,
            LocalDate deliveryDate,
            String collectorFirstName,
            String collectorLastName,
            String collectorPhone,
            String collectorAddress,
            MultipartFile file) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new BusinessException("Dossier non trouvé", HttpStatus.NOT_FOUND));

        if (!ELIGIBLE_STATUSES.contains(dossier.getStatus())) {
            throw new BusinessException("La remise de plaque n'est pas possible pour ce dossier à ce stade");
        }

        validateFields(plateNumber, deliveryDate, collectorFirstName, collectorLastName, collectorPhone, collectorAddress);

        PlateDelivery delivery = dossier.getPlateDelivery();
        boolean isNew = delivery == null;
        if (isNew) {
            if (file == null || file.isEmpty()) {
                throw new BusinessException("La pièce justificative est obligatoire");
            }
            delivery = PlateDelivery.builder().dossier(dossier).build();
        } else if (file != null && !file.isEmpty()) {
            deleteStoredFile(delivery.getFilePath());
        } else if (delivery.getFilePath() == null || delivery.getFilePath().isBlank()) {
            throw new BusinessException("La pièce justificative est obligatoire");
        }

        delivery.setPlateNumber(plateNumber.trim().toUpperCase(Locale.ROOT));
        delivery.setDeliveryDate(deliveryDate);
        delivery.setCollectorFirstName(collectorFirstName.trim());
        delivery.setCollectorLastName(collectorLastName.trim());
        delivery.setCollectorPhone(collectorPhone.trim());
        delivery.setCollectorAddress(collectorAddress.trim());

        if (file != null && !file.isEmpty()) {
            storeAttachment(dossier, delivery, file);
        }

        dossier.setPlateDelivery(delivery);
        plateDeliveryRepository.save(delivery);

        applySideEffects(dossier, delivery);

        notificationService.create(
                dossier.getCitizen().getUser(),
                "Remise de plaque enregistrée pour le dossier " + dossier.getReferenceNumber()
                        + " — N° " + delivery.getPlateNumber(),
                NotificationType.DOSSIER);

        return mapperService.toDto(dossier);
    }

    @Transactional(readOnly = true)
    public PlateDelivery getForDossier(Long dossierId) {
        return plateDeliveryRepository.findByDossierId(dossierId)
                .orElseThrow(() -> new BusinessException("Aucune remise de plaque enregistrée", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public PlateDelivery getOwnedForDossier(String email, Long dossierId) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new BusinessException("Dossier non trouvé", HttpStatus.NOT_FOUND));
        if (!dossier.getCitizen().getUser().getEmail().equalsIgnoreCase(email)) {
            throw new BusinessException("Accès non autorisé", HttpStatus.FORBIDDEN);
        }
        return getForDossier(dossierId);
    }

    @Transactional(readOnly = true)
    public Resource loadAttachmentResource(PlateDelivery delivery) {
        Path path = storedFileService.resolve(delivery.getFilePath());
        if (!Files.isRegularFile(path)) {
            throw new BusinessException("Fichier introuvable", HttpStatus.NOT_FOUND);
        }
        return new FileSystemResource(path);
    }

    public ResponseEntity<Resource> buildFileResponse(PlateDelivery delivery, Resource resource) {
        return documentService.buildFileResponse(
                delivery.getFileName(),
                resource,
                resolveContentType(delivery));
    }

    public String resolveContentType(PlateDelivery delivery) {
        if (delivery.getContentType() != null && !delivery.getContentType().isBlank()) {
            return delivery.getContentType();
        }
        String fileName = delivery.getFileName();
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

    private void applySideEffects(Dossier dossier, PlateDelivery delivery) {
        if (dossier.getVehicle() != null) {
            dossier.getVehicle().setRegistrationNumber(delivery.getPlateNumber());
        }

        Registration registration = dossier.getRegistration();
        if (registration == null) {
            registration = Registration.builder()
                    .dossier(dossier)
                    .registrationNumber(delivery.getPlateNumber())
                    .registrationDate(delivery.getDeliveryDate().atStartOfDay())
                    .build();
            dossier.setRegistration(registration);
        } else {
            registration.setRegistrationNumber(delivery.getPlateNumber());
            registration.setRegistrationDate(delivery.getDeliveryDate().atStartOfDay());
        }
        registrationRepository.save(registration);

        if (dossier.getStatus() != DossierStatus.COMPLETED) {
            dossier.setStatus(DossierStatus.COMPLETED);
        }
    }

    private void storeAttachment(Dossier dossier, PlateDelivery delivery, MultipartFile file) {
        validateJustificatif(file);
        try {
            Path dir = storedFileService.dossierDirectory(dossier.getReferenceNumber(), "remise-plaque");
            Files.createDirectories(dir);

            String ext = getExtension(file.getOriginalFilename());
            String storedName = "pj_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path target = dir.resolve(storedName);
            Files.copy(file.getInputStream(), target);

            delivery.setFileName(file.getOriginalFilename());
            delivery.setFilePath(storedFileService.toStoredPath(target));
            delivery.setFileSize(file.getSize());
            delivery.setContentType(file.getContentType());
        } catch (IOException e) {
            throw new BusinessException("Erreur lors du téléversement de la pièce justificative",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateFields(
            String plateNumber,
            LocalDate deliveryDate,
            String collectorFirstName,
            String collectorLastName,
            String collectorPhone,
            String collectorAddress) {
        if (plateNumber == null || plateNumber.isBlank()) {
            throw new BusinessException("Le numéro de plaque est obligatoire");
        }
        if (deliveryDate == null) {
            throw new BusinessException("La date de remise est obligatoire");
        }
        if (collectorFirstName == null || collectorFirstName.isBlank()) {
            throw new BusinessException("Le prénom du retirant est obligatoire");
        }
        if (collectorLastName == null || collectorLastName.isBlank()) {
            throw new BusinessException("Le nom du retirant est obligatoire");
        }
        if (collectorPhone == null || collectorPhone.isBlank()) {
            throw new BusinessException("Le numéro de téléphone du retirant est obligatoire");
        }
        if (collectorAddress == null || collectorAddress.isBlank()) {
            throw new BusinessException("L'adresse du retirant est obligatoire");
        }
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

    private void deleteStoredFile(String filePath) {
        if (filePath == null || filePath.isBlank()) return;
        try {
            Path path = storedFileService.resolve(filePath);
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
            // best effort cleanup
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
