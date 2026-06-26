package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.entity.Document;
import ml.gouv.pie.entity.Dossier;
import ml.gouv.pie.entity.Registration;
import ml.gouv.pie.entity.enums.DocumentStatus;
import ml.gouv.pie.entity.enums.DossierStatus;
import ml.gouv.pie.entity.enums.NotificationType;
import ml.gouv.pie.exception.BusinessException;
import ml.gouv.pie.repository.DossierRepository;
import ml.gouv.pie.repository.RegistrationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private static final Set<DossierStatus> REJECTABLE_BEFORE_IMMATRICULATION = EnumSet.of(
            DossierStatus.PAID,
            DossierStatus.VALIDATED,
            DossierStatus.APPOINTMENT_SCHEDULED);

    private final DossierRepository dossierRepository;
    private final RegistrationRepository registrationRepository;
    private final DossierMapperService mapperService;
    private final NotificationService notificationService;

    @Transactional
    public DtoMapper.DossierDto validateDossier(Long dossierId) {
        Dossier dossier = loadDossier(dossierId);

        if (dossier.getStatus() != DossierStatus.PAID) {
            throw new BusinessException("Seuls les dossiers payés peuvent être validés");
        }

        ensureAllRequiredDocumentsValidated(dossier);

        dossier.setStatus(DossierStatus.VALIDATED);
        dossierRepository.save(dossier);

        notificationService.create(dossier.getCitizen().getUser(),
                "Dossier " + dossier.getReferenceNumber()
                        + " validé. Le rendez-vous pour le contrôle physique sera confirmé prochainement.",
                NotificationType.DOSSIER);

        return mapperService.toDto(dossier);
    }

    @Transactional
    public DtoMapper.DossierDto startImmatriculation(Long dossierId) {
        Dossier dossier = loadDossier(dossierId);

        if (dossier.getStatus() != DossierStatus.APPOINTMENT_SCHEDULED) {
            throw new BusinessException("Seuls les dossiers avec un RDV de contrôle physique confirmé peuvent démarrer l'immatriculation");
        }

        dossier.setStatus(DossierStatus.IMMATRICULATION_IN_PROGRESS);
        dossierRepository.save(dossier);

        notificationService.create(dossier.getCitizen().getUser(),
                "Immatriculation en cours pour le dossier " + dossier.getReferenceNumber() + ".",
                NotificationType.DOSSIER);

        return mapperService.toDto(dossier);
    }

    @Transactional
    public DtoMapper.DossierDto completeImmatriculation(Long dossierId, String registrationNumber) {
        Dossier dossier = loadDossier(dossierId);

        if (dossier.getStatus() != DossierStatus.IMMATRICULATION_IN_PROGRESS) {
            throw new BusinessException("Seuls les dossiers en cours d'immatriculation peuvent être finalisés");
        }

        String normalizedNumber = registrationNumber.trim().toUpperCase(Locale.ROOT);
        if (normalizedNumber.isBlank()) {
            throw new BusinessException("Le numéro d'immatriculation est obligatoire");
        }

        registrationRepository.findByDossierId(dossierId).ifPresent(existing -> {
            if (!existing.getRegistrationNumber().equalsIgnoreCase(normalizedNumber)
                    && registrationRepository.existsByRegistrationNumber(normalizedNumber)) {
                throw new BusinessException("Ce numéro d'immatriculation est déjà attribué");
            }
        });
        if (!registrationRepository.findByDossierId(dossierId).isPresent()
                && registrationRepository.existsByRegistrationNumber(normalizedNumber)) {
            throw new BusinessException("Ce numéro d'immatriculation est déjà attribué");
        }

        Registration registration = dossier.getRegistration();
        if (registration == null) {
            registration = Registration.builder()
                    .dossier(dossier)
                    .registrationNumber(normalizedNumber)
                    .registrationDate(LocalDateTime.now())
                    .build();
            dossier.setRegistration(registration);
        } else {
            registration.setRegistrationNumber(normalizedNumber);
            registration.setRegistrationDate(LocalDateTime.now());
        }
        registrationRepository.save(registration);

        if (dossier.getVehicle() != null) {
            dossier.getVehicle().setRegistrationNumber(normalizedNumber);
        }

        dossier.setStatus(DossierStatus.COMPLETED);
        dossierRepository.save(dossier);

        notificationService.create(dossier.getCitizen().getUser(),
                "Dossier " + dossier.getReferenceNumber()
                        + " immatriculé — N° " + normalizedNumber,
                NotificationType.DOSSIER);

        return mapperService.toDto(dossier);
    }

    @Transactional
    public DtoMapper.DossierDto cancelImmatriculation(Long dossierId, String reason) {
        Dossier dossier = loadDossier(dossierId);

        if (dossier.getStatus() != DossierStatus.IMMATRICULATION_IN_PROGRESS) {
            throw new BusinessException("Seule une immatriculation en cours peut être annulée");
        }

        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Le motif d'annulation est obligatoire");
        }

        dossier.setStatus(DossierStatus.REJECTED);
        dossier.setRejectionReason(reason.trim());
        dossierRepository.save(dossier);

        notificationService.create(dossier.getCitizen().getUser(),
                "Immatriculation annulée pour le dossier " + dossier.getReferenceNumber() + " : " + reason.trim(),
                NotificationType.WARNING);

        return mapperService.toDto(dossier);
    }

    @Transactional
    public DtoMapper.DossierDto rejectDossier(Long dossierId, String reason) {
        Dossier dossier = loadDossier(dossierId);

        if (!REJECTABLE_BEFORE_IMMATRICULATION.contains(dossier.getStatus())) {
            throw new BusinessException("Ce dossier ne peut plus être rejeté à ce stade");
        }

        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Le motif de rejet est obligatoire");
        }

        dossier.setStatus(DossierStatus.REJECTED);
        dossier.setRejectionReason(reason.trim());
        dossierRepository.save(dossier);

        notificationService.create(dossier.getCitizen().getUser(),
                "Dossier " + dossier.getReferenceNumber() + " rejeté: " + reason.trim(),
                NotificationType.WARNING);

        return mapperService.toDto(dossier);
    }

    @Transactional
    public List<DtoMapper.DossierDto> validateDossiers(List<Long> dossierIds) {
        return dossierIds.stream().map(this::validateDossier).toList();
    }

    @Transactional
    public List<DtoMapper.DossierDto> rejectDossiers(List<Long> dossierIds, String reason) {
        return dossierIds.stream().map(id -> rejectDossier(id, reason)).toList();
    }

    private void ensureAllRequiredDocumentsValidated(Dossier dossier) {
        List<Document> required = dossier.getDocuments().stream()
                .filter(d -> d.getTypeDocument().isObligatoire())
                .toList();

        if (required.isEmpty()) {
            throw new BusinessException("Aucun document requis configuré pour ce dossier");
        }

        for (Document document : required) {
            if (document.getFileName() == null || document.getFileName().isBlank()) {
                throw new BusinessException(
                        "Le document « " + document.getTypeDocument().getLibelle() + " » n'est pas téléversé");
            }
            if (document.getStatus() != DocumentStatus.VALIDATED) {
                throw new BusinessException(
                        "Le document « " + document.getTypeDocument().getLibelle() + " » doit être validé individuellement");
            }
        }
    }

    private Dossier loadDossier(Long dossierId) {
        return dossierRepository.findDetailedById(dossierId)
                .orElseThrow(() -> new BusinessException("Dossier non trouvé", HttpStatus.NOT_FOUND));
    }
}
