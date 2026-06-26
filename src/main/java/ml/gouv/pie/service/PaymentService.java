package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.dto.PaymentRequest;
import ml.gouv.pie.entity.Center;
import ml.gouv.pie.entity.Dossier;
import ml.gouv.pie.entity.Payment;
import ml.gouv.pie.entity.User;
import ml.gouv.pie.entity.enums.DossierStatus;
import ml.gouv.pie.entity.enums.NotificationType;
import ml.gouv.pie.entity.enums.PaymentStatus;
import ml.gouv.pie.exception.BusinessException;
import ml.gouv.pie.repository.CenterRepository;
import ml.gouv.pie.repository.PaymentRepository;
import ml.gouv.pie.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CenterRepository centerRepository;
    private final UserRepository userRepository;
    private final DossierService dossierService;
    private final DossierMapperService mapperService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final TariffService tariffService;

    @Transactional
    public DtoMapper.DossierDto initiatePayment(String email, Long dossierId, PaymentRequest request) {
        Dossier dossier = dossierService.getOwnedDossier(email, dossierId);

        if (dossier.getStatus() != DossierStatus.SUBMITTED
                && dossier.getStatus() != DossierStatus.PAYMENT_PENDING
                && dossier.getStatus() != DossierStatus.VALIDATED) {
            throw new BusinessException("Le dossier doit être soumis avant le paiement");
        }

        Center center = centerRepository.findById(request.getCenterId())
                .orElseThrow(() -> new BusinessException("Centre introuvable"));
        if (!center.isActive()) {
            throw new BusinessException("Ce centre n'est pas disponible");
        }
        dossier.setProcessingCenter(center);

        BigDecimal registrationFee = tariffService.getRegistrationFee();
        BigDecimal serviceFee = tariffService.getServiceFee();

        Payment payment = dossier.getPayment();
        if (payment == null) {
            payment = Payment.builder()
                    .dossier(dossier)
                    .amount(registrationFee)
                    .serviceFee(serviceFee)
                    .totalAmount(registrationFee.add(serviceFee))
                    .status(PaymentStatus.PENDING)
                    .build();
            dossier.setPayment(payment);
        }

        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setTransactionId("TP-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        paymentRepository.save(payment);

        dossier.setStatus(DossierStatus.PAYMENT_PENDING);
        return mapperService.toDto(dossier);
    }

    @Transactional
    public DtoMapper.DossierDto confirmPayment(String email, Long dossierId) {
        Dossier dossier = dossierService.getOwnedDossier(email, dossierId);
        Payment payment = dossier.getPayment();

        if (payment == null || payment.getStatus() != PaymentStatus.PROCESSING) {
            throw new BusinessException("Aucun paiement en cours");
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);

        dossier.setStatus(DossierStatus.PAID);
        User user = dossier.getCitizen().getUser();
        notificationService.create(user,
                "Paiement confirmé pour le dossier " + dossier.getReferenceNumber(),
                NotificationType.PAYMENT, false);
        Center center = dossier.getProcessingCenter();
        String centerLabel = center != null ? center.getCity() + " — " + center.getName() : null;
        String vehicleTypeLabel = null;
        if (dossier.getVehicle() != null) {
            var vt = dossier.getVehicle().getVehicleTypeEntity();
            vehicleTypeLabel = vt != null ? vt.getLibelle() : dossier.getVehicle().getVehicleType();
        }
        emailService.sendPaymentReceipt(
                user,
                dossier.getReferenceNumber(),
                payment.getTransactionId(),
                payment.getTotalAmount().toPlainString(),
                payment.getPaymentDate().toString(),
                centerLabel,
                vehicleTypeLabel
        );

        if (center != null) {
            var staff = userRepository.findActiveStaffByCenterId(center.getId());
            emailService.sendCenterNewDossierNotification(staff, dossier, center);
        }

        return mapperService.toDto(dossier);
    }
}
