package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.dto.VehicleRequest;
import ml.gouv.pie.entity.*;
import ml.gouv.pie.entity.enums.*;
import ml.gouv.pie.exception.BusinessException;
import ml.gouv.pie.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DossierService {

    private final DossierRepository dossierRepository;
    private final CitizenRepository citizenRepository;
    private final VehicleRepository vehicleRepository;
    private final DossierMapperService mapperService;
    private final NotificationService notificationService;
    private final TypeDocumentService typeDocumentService;
    private final VehicleBrandService vehicleBrandService;
    private final VehicleTypeService vehicleTypeService;
    private final EmailService emailService;

    @Value("${app.auto-validate:false}")
    private boolean autoValidate;

    @Transactional
    public DtoMapper.DossierDto createDossier(String email, VehicleRequest request) {
        Citizen citizen = getCitizenByEmail(email);

        if (vehicleRepository.existsByChassisNumber(request.getChassisNumber())) {
            throw new BusinessException("Ce numéro de châssis est déjà enregistré");
        }

        int currentYear = Year.now().getValue();
        if (request.getYear() < 1980 || request.getYear() > currentYear) {
            throw new BusinessException("Année de fabrication invalide");
        }

        List<TypeDocument> activeTypes = typeDocumentService.getActiveEntities();
        if (activeTypes.isEmpty()) {
            throw new BusinessException("Aucun type de document configuré");
        }

        String ref = generateReferenceNumber();

        Dossier dossier = Dossier.builder()
                .referenceNumber(ref)
                .citizen(citizen)
                .status(DossierStatus.DRAFT)
                .build();
        dossierRepository.save(dossier);

        VehicleBrand brandEntity = vehicleBrandService.getActiveById(request.getBrandId());
        VehicleType typeEntity = vehicleTypeService.getActiveById(request.getVehicleTypeId());

        String brandLabel;
        if ("AUTRE".equals(brandEntity.getCode())) {
            if (request.getBrandOther() == null || request.getBrandOther().isBlank()) {
                throw new BusinessException("Précisez la marque pour « Autre »");
            }
            brandLabel = request.getBrandOther().trim().toUpperCase();
        } else {
            brandLabel = brandEntity.getLibelle();
        }

        String engineNumber = request.getEngineNumber();
        if (engineNumber != null && !engineNumber.isBlank()) {
            engineNumber = engineNumber.trim().toUpperCase();
        } else {
            engineNumber = null;
        }

        Vehicle vehicle = Vehicle.builder()
                .dossier(dossier)
                .brandEntity(brandEntity)
                .vehicleTypeEntity(typeEntity)
                .brand(brandLabel)
                .vehicleType(typeEntity.getLibelle())
                .model(request.getModel())
                .engineCapacity(request.getEngineCapacity())
                .engineNumber(engineNumber)
                .chassisNumber(request.getChassisNumber())
                .color(request.getColor())
                .year(request.getYear())
                .countryOfOrigin(request.getCountryOfOrigin())
                .build();
        dossier.setVehicle(vehicle);

        for (TypeDocument typeDocument : activeTypes) {
            Document doc = Document.builder()
                    .dossier(dossier)
                    .typeDocument(typeDocument)
                    .status(DocumentStatus.PENDING)
                    .build();
            dossier.getDocuments().add(doc);
        }

        dossierRepository.save(dossier);
        notificationService.create(citizen.getUser(),
                "Nouveau dossier créé: " + ref, NotificationType.DOSSIER);

        return mapperService.toDto(dossier);
    }

    @Transactional(readOnly = true)
    public List<DtoMapper.DossierDto> getMyDossiers(String email) {
        Citizen citizen = getCitizenByEmail(email);
        return dossierRepository.findByCitizenIdOrderByCreatedAtDesc(citizen.getId())
                .stream()
                .map(mapperService::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DtoMapper.DossierDto getDossier(String email, Long dossierId) {
        Dossier dossier = getOwnedDossier(email, dossierId);
        return mapperService.toDto(dossier);
    }

    @Transactional
    public DtoMapper.DossierDto submitDossier(String email, Long dossierId) {
        Dossier dossier = getOwnedDossier(email, dossierId);

        if (dossier.getStatus() != DossierStatus.DRAFT) {
            throw new BusinessException("Ce dossier ne peut pas être soumis");
        }

        boolean allRequiredUploaded = dossier.getDocuments().stream()
                .filter(d -> d.getTypeDocument().isObligatoire())
                .allMatch(d -> d.getStatus() == DocumentStatus.UPLOADED);

        if (!allRequiredUploaded) {
            throw new BusinessException("Tous les documents obligatoires doivent être téléversés");
        }

        dossier.setStatus(autoValidate ? DossierStatus.VALIDATED : DossierStatus.SUBMITTED);
        dossierRepository.save(dossier);

        String ref = dossier.getReferenceNumber();
        String notifMessage = autoValidate
                ? "Dossier " + ref + " validé. Procédez au paiement."
                : "Dossier " + ref + " soumis pour validation";
        notificationService.create(dossier.getCitizen().getUser(), notifMessage, NotificationType.DOSSIER, false);
        emailService.sendDossierSubmittedEmail(dossier.getCitizen().getUser(), ref);

        return mapperService.toDto(dossier);
    }

    @Transactional
    public void deleteDraftDossier(String email, Long dossierId) {
        Dossier dossier = getOwnedDossier(email, dossierId);

        if (dossier.getStatus() != DossierStatus.DRAFT) {
            throw new BusinessException("Seuls les dossiers en brouillon peuvent être supprimés");
        }

        for (Document doc : dossier.getDocuments()) {
            if (doc.getFilePath() != null && !doc.getFilePath().isBlank()) {
                try {
                    Files.deleteIfExists(Paths.get(doc.getFilePath()));
                } catch (IOException ignored) {
                    // continue even if file removal fails
                }
            }
        }

        dossierRepository.delete(dossier);
    }

    @Transactional(readOnly = true)
    public DtoMapper.DossierDto findByReference(String reference) {
        Dossier dossier = dossierRepository.findByReferenceNumber(reference)
                .orElseThrow(() -> new BusinessException("Dossier non trouvé", HttpStatus.NOT_FOUND));
        Dossier detailed = dossierRepository.findDetailedById(dossier.getId())
                .orElseThrow(() -> new BusinessException("Dossier non trouvé", HttpStatus.NOT_FOUND));
        return mapperService.toDto(detailed);
    }

    @Transactional(readOnly = true)
    public DtoMapper.DashboardStatsDto getDashboardStats(String email) {
        Citizen citizen = getCitizenByEmail(email);

        long enCours = dossierRepository.countByCitizenIdAndStatus(citizen.getId(), DossierStatus.DRAFT)
                + dossierRepository.countByCitizenIdAndStatus(citizen.getId(), DossierStatus.SUBMITTED)
                + dossierRepository.countByCitizenIdAndStatus(citizen.getId(), DossierStatus.IN_REVIEW)
                + dossierRepository.countByCitizenIdAndStatus(citizen.getId(), DossierStatus.PAYMENT_PENDING);

        long valides = dossierRepository.countByCitizenIdAndStatus(citizen.getId(), DossierStatus.VALIDATED)
                + dossierRepository.countByCitizenIdAndStatus(citizen.getId(), DossierStatus.PAID);

        long rdv = dossierRepository.countByCitizenIdAndStatus(citizen.getId(), DossierStatus.APPOINTMENT_SCHEDULED);
        long immat = dossierRepository.countByCitizenIdAndStatus(citizen.getId(), DossierStatus.COMPLETED);

        return DtoMapper.DashboardStatsDto.builder()
                .dossiersEnCours(enCours)
                .dossiersValides(valides)
                .rendezVous(rdv)
                .immatriculations(immat)
                .totalDossiers(dossierRepository.countAllDossiers())
                .totalValides(dossierRepository.countValidatedDossiers())
                .totalImmatriculations(dossierRepository.countCompletedDossiers())
                .satisfactionRate(97)
                .build();
    }

    private String generateReferenceNumber() {
        int year = Year.now().getValue();
        long count = dossierRepository.count() + 1;
        return String.format("MD%d/%06d", year, count);
    }

    Dossier getOwnedDossier(String email, Long dossierId) {
        Citizen citizen = getCitizenByEmail(email);
        Dossier dossier = dossierRepository.findDetailedById(dossierId)
                .orElseThrow(() -> new BusinessException("Dossier non trouvé", HttpStatus.NOT_FOUND));

        if (!dossier.getCitizen().getId().equals(citizen.getId())) {
            throw new BusinessException("Accès non autorisé", HttpStatus.FORBIDDEN);
        }
        return dossier;
    }

    public Dossier getDossierEntity(Long dossierId) {
        return dossierRepository.findById(dossierId)
                .orElseThrow(() -> new BusinessException("Dossier non trouvé", HttpStatus.NOT_FOUND));
    }

    private Citizen getCitizenByEmail(String email) {
        return citizenRepository.findByUser_Email(email)
                .orElseThrow(() -> new BusinessException("Profil citoyen non trouvé", HttpStatus.NOT_FOUND));
    }
}
