package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.entity.Dossier;
import ml.gouv.pie.entity.enums.DossierStatus;
import ml.gouv.pie.entity.enums.NotificationType;
import ml.gouv.pie.exception.BusinessException;
import ml.gouv.pie.repository.DossierRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final DossierRepository dossierRepository;
    private final DossierMapperService mapperService;
    private final NotificationService notificationService;

    @Transactional
    public DtoMapper.DossierDto validateDossier(Long dossierId) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new BusinessException("Dossier non trouvé", HttpStatus.NOT_FOUND));

        if (dossier.getStatus() != DossierStatus.SUBMITTED && dossier.getStatus() != DossierStatus.IN_REVIEW) {
            throw new BusinessException("Ce dossier ne peut pas être validé");
        }

        dossier.setStatus(DossierStatus.VALIDATED);
        dossierRepository.save(dossier);

        notificationService.create(dossier.getCitizen().getUser(),
                "Dossier " + dossier.getReferenceNumber() + " validé. Procédez au paiement.",
                NotificationType.SUCCESS);

        return mapperService.toDto(dossier);
    }

    @Transactional
    public DtoMapper.DossierDto rejectDossier(Long dossierId, String reason) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new BusinessException("Dossier non trouvé", HttpStatus.NOT_FOUND));

        dossier.setStatus(DossierStatus.REJECTED);
        dossier.setRejectionReason(reason);
        dossierRepository.save(dossier);

        notificationService.create(dossier.getCitizen().getUser(),
                "Dossier " + dossier.getReferenceNumber() + " rejeté: " + reason,
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
}
