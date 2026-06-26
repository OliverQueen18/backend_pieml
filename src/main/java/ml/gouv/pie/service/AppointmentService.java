package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.AppointmentRequest;
import ml.gouv.pie.dto.CenterAvailabilityDto;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.entity.Appointment;
import ml.gouv.pie.entity.Center;
import ml.gouv.pie.entity.Dossier;
import ml.gouv.pie.entity.User;
import ml.gouv.pie.entity.enums.AppointmentStatus;
import ml.gouv.pie.entity.enums.DossierStatus;
import ml.gouv.pie.entity.enums.NotificationType;
import ml.gouv.pie.exception.BusinessException;
import ml.gouv.pie.repository.AppointmentRepository;
import ml.gouv.pie.repository.CenterRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final CenterRepository centerRepository;
    private final CenterScheduleService centerScheduleService;
    private final DossierService dossierService;
    private final DossierMapperService mapperService;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<DtoMapper.CenterDto> getActiveCenters() {
        return centerRepository.findByActiveTrueOrderByCityAscNameAsc().stream()
                .map(center -> {
                    DtoMapper.CenterDto dto = DtoMapper.CenterDto.builder()
                            .id(center.getId())
                            .name(center.getName())
                            .city(center.getCity())
                            .address(center.getAddress())
                            .phone(center.getPhone())
                            .latitude(center.getLatitude())
                            .longitude(center.getLongitude())
                            .dailyCapacity(center.getDailyCapacity())
                            .active(center.isActive())
                            .build();
                    return centerScheduleService.enrichCenterDto(center, dto);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public CenterAvailabilityDto getCenterAvailability(Long centerId) {
        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new BusinessException("Centre non trouvé", HttpStatus.NOT_FOUND));
        if (!center.isActive()) {
            throw new BusinessException("Ce centre n'est pas actif", HttpStatus.BAD_REQUEST);
        }
        return centerScheduleService.getAvailability(center, 90);
    }

    @Transactional
    public DtoMapper.DossierDto scheduleAppointment(String email, Long dossierId, AppointmentRequest request) {
        Dossier dossier = dossierService.getOwnedDossier(email, dossierId);

        if (dossier.getStatus() != DossierStatus.PAID) {
            throw new BusinessException("Le paiement doit être effectué avant de prendre rendez-vous");
        }

        Center center = centerRepository.findById(request.getCenterId())
                .orElseThrow(() -> new BusinessException("Centre non trouvé", HttpStatus.NOT_FOUND));

        if (!center.isActive()) {
            throw new BusinessException("Ce centre n'est pas actif");
        }

        centerScheduleService.validateAppointment(center, request.getAppointmentDate(), request.getAppointmentTime());

        Appointment appointment = dossier.getAppointment();
        if (appointment == null) {
            appointment = Appointment.builder().dossier(dossier).build();
            dossier.setAppointment(appointment);
        }

        appointment.setCenter(center);
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointmentRepository.save(appointment);

        User user = dossier.getCitizen().getUser();
        notificationService.create(user,
                "Demande de rendez-vous enregistrée au centre " + center.getName()
                        + " le " + request.getAppointmentDate() + ". En attente de confirmation.",
                NotificationType.APPOINTMENT, false);

        return mapperService.toDto(dossier);
    }

    @Transactional
    public DtoMapper.DossierDto confirmAppointment(Long dossierId, AppointmentRequest request) {
        Dossier dossier = dossierService.getDossierEntity(dossierId);

        if (dossier.getStatus() != DossierStatus.VALIDATED) {
            throw new BusinessException("Seuls les dossiers validés peuvent avoir un rendez-vous confirmé");
        }

        Center center = centerRepository.findById(request.getCenterId())
                .orElseThrow(() -> new BusinessException("Centre non trouvé", HttpStatus.NOT_FOUND));

        if (!center.isActive()) {
            throw new BusinessException("Ce centre n'est pas actif");
        }

        centerScheduleService.validateAppointment(center, request.getAppointmentDate(), request.getAppointmentTime());

        Appointment appointment = dossier.getAppointment();
        if (appointment == null) {
            appointment = Appointment.builder().dossier(dossier).build();
            dossier.setAppointment(appointment);
        }

        appointment.setCenter(center);
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(appointment);

        dossier.setStatus(DossierStatus.APPOINTMENT_SCHEDULED);
        User user = dossier.getCitizen().getUser();
        notificationService.create(user,
                "Rendez-vous de contrôle physique confirmé au centre " + center.getName()
                        + " le " + request.getAppointmentDate(),
                NotificationType.APPOINTMENT, false);
        emailService.sendAppointmentConvocation(
                user,
                dossier.getReferenceNumber(),
                center.getName(),
                center.getAddress() != null ? center.getAddress() : center.getCity(),
                request.getAppointmentDate().toString(),
                request.getAppointmentTime().toString()
        );

        return mapperService.toDto(dossier);
    }
}
