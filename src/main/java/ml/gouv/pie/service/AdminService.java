package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.AdminNotificationRequest;
import ml.gouv.pie.dto.AdminPaymentUpdateRequest;
import ml.gouv.pie.dto.CenterRequest;
import ml.gouv.pie.dto.CreateStaffUserRequest;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.dto.RoleDefinitionRequest;
import ml.gouv.pie.dto.RolePermissionsRequest;
import ml.gouv.pie.dto.UpdateStaffUserRequest;
import ml.gouv.pie.entity.*;
import ml.gouv.pie.entity.enums.DossierStatus;
import ml.gouv.pie.entity.enums.PaymentStatus;
import ml.gouv.pie.entity.enums.Permission;
import ml.gouv.pie.entity.enums.Role;
import ml.gouv.pie.util.DefaultRolePermissions;
import ml.gouv.pie.exception.BusinessException;
import ml.gouv.pie.repository.*;
import ml.gouv.pie.util.PasswordGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.sql.Timestamp;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final DossierRepository dossierRepository;
    private final UserRepository userRepository;
    private final CitizenRepository citizenRepository;
    private final CenterRepository centerRepository;
    private final AppointmentRepository appointmentRepository;
    private final CenterScheduleService centerScheduleService;
    private final DossierMapperService dossierMapperService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RoleDefinitionRepository roleDefinitionRepository;
    private final NotificationRepository notificationRepository;
    private final PaymentRepository paymentRepository;

    public DtoMapper.AdminDashboardStatsDto getDashboardStats() {
        long total = dossierRepository.countAllDossiers();
        long enCours = Arrays.stream(DossierStatus.values())
                .filter(s -> s != DossierStatus.COMPLETED && s != DossierStatus.REJECTED)
                .mapToLong(dossierRepository::countByStatus)
                .sum();
        return DtoMapper.AdminDashboardStatsDto.builder()
                .totalDossiers(total)
                .dossiersEnCours(enCours)
                .dossiersValides(dossierRepository.countValidatedDossiers())
                .dossiersRejetes(dossierRepository.countByStatus(DossierStatus.REJECTED))
                .immatriculations(dossierRepository.countCompletedDossiers())
                .totalCitoyens(citizenRepository.count())
                .totalUtilisateurs(userRepository.count())
                .rendezVousPlanifies(dossierRepository.countByStatus(DossierStatus.APPOINTMENT_SCHEDULED))
                .build();
    }

    public DtoMapper.AdminDashboardChartsDto getDashboardCharts(String period) {
        String normalized = normalizePeriod(period);
        LocalDateTime sinceDateTime = resolveSinceDateTime(normalized);
        LocalDate sinceDate = sinceDateTime.toLocalDate();

        List<DtoMapper.DashboardChartPointDto> dossiersByPeriod = mapChartRows(
                "6m".equals(normalized)
                        ? dossierRepository.countDossiersByMonthSince(Timestamp.valueOf(sinceDateTime))
                        : dossierRepository.countDossiersByDaySince(Timestamp.valueOf(sinceDateTime))
        );

        Map<Long, DtoMapper.DashboardCenterStatDto> centerStats = new LinkedHashMap<>();
        for (Center center : centerRepository.findAllByOrderByCityAscNameAsc()) {
            centerStats.put(center.getId(), DtoMapper.DashboardCenterStatDto.builder()
                    .centerId(center.getId())
                    .centerName(center.getName())
                    .city(center.getCity())
                    .appointments(0)
                    .dossiers(0)
                    .build());
        }
        for (Object[] row : appointmentRepository.countAppointmentsByCenterSince(java.sql.Date.valueOf(sinceDate))) {
            String name = (String) row[0];
            String city = (String) row[1];
            long count = ((Number) row[2]).longValue();
            centerStats.values().stream()
                    .filter(c -> c.getCenterName().equals(name) && c.getCity().equals(city))
                    .findFirst()
                    .ifPresent(c -> c.setAppointments(count));
        }

        List<DtoMapper.AdminNotificationDto> recentNotifications = notificationRepository
                .findAllWithUserOrderByCreatedAtDesc().stream()
                .limit(8)
                .map(this::toNotificationDto)
                .toList();

        return DtoMapper.AdminDashboardChartsDto.builder()
                .period(normalized)
                .dossiersByPeriod(dossiersByPeriod)
                .statsByCenter(centerStats.values().stream().toList())
                .recentNotifications(recentNotifications)
                .build();
    }

    private String normalizePeriod(String period) {
        if (period == null || period.isBlank()) {
            return "30d";
        }
        return switch (period.trim().toLowerCase()) {
            case "7d", "30d", "6m" -> period.trim().toLowerCase();
            default -> "30d";
        };
    }

    private LocalDateTime resolveSinceDateTime(String period) {
        LocalDateTime now = LocalDateTime.now();
        return switch (period) {
            case "7d" -> now.minusDays(7);
            case "6m" -> now.minusMonths(6);
            default -> now.minusDays(30);
        };
    }

    private List<DtoMapper.DashboardChartPointDto> mapChartRows(List<Object[]> rows) {
        return rows.stream()
                .map(row -> DtoMapper.DashboardChartPointDto.builder()
                        .label(String.valueOf(row[0]))
                        .value(((Number) row[1]).longValue())
                        .build())
                .toList();
    }

    public List<DtoMapper.AdminDossierSummaryDto> listDossiers(
            DossierStatus status, String reference, String citizen, String chassis, User currentUser) {
        String ref = blankToNull(reference);
        String cit = blankToNull(citizen);
        String ch = blankToNull(chassis);
        User actor = resolveUserWithCenters(currentUser);
        return dossierRepository.searchForAdmin(
                status,
                toLikePattern(ref),
                toLikePattern(cit),
                toLikePattern(ch)
        ).stream()
                .filter(d -> canAccessDossier(actor, d))
                .map(this::toSummary)
                .toList();
    }

    private String toLikePattern(String value) {
        if (value == null) return null;
        return "%" + value.toLowerCase() + "%";
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    public DtoMapper.DossierDto getDossier(Long id, User currentUser) {
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Dossier non trouvé", HttpStatus.NOT_FOUND));
        if (!canAccessDossier(resolveUserWithCenters(currentUser), dossier)) {
            throw new BusinessException("Accès non autorisé à ce dossier", HttpStatus.FORBIDDEN);
        }
        return dossierMapperService.toDto(dossier);
    }

    @Transactional
    public void deleteDossier(Long id, User currentUser) {
        Role role = currentUser.getRole();
        if (role != Role.ADMIN && role != Role.SUPER_ADMIN) {
            throw new BusinessException("Suppression réservée aux administrateurs", HttpStatus.FORBIDDEN);
        }
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Dossier non trouvé", HttpStatus.NOT_FOUND));
        if (dossier.getStatus() == DossierStatus.COMPLETED || dossier.getStatus() == DossierStatus.PAID
                || dossier.getStatus() == DossierStatus.APPOINTMENT_SCHEDULED) {
            throw new BusinessException("Ce dossier ne peut plus être supprimé");
        }
        dossierRepository.delete(dossier);
    }

    public List<DtoMapper.AdminCitizenDto> listCitizens() {
        return citizenRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toCitizenDto)
                .toList();
    }

    public List<DtoMapper.AdminUserDto> listStaffUsers() {
        return userRepository.findStaffWithCentersByRoleNot(Role.CITOYEN).stream()
                .map(this::toUserDto)
                .toList();
    }

    @Transactional
    public DtoMapper.AdminUserDto createStaffUser(CreateStaffUserRequest request, User actor) {
        if (request.getRole() == Role.CITOYEN) {
            throw new BusinessException("Utilisez l'inscription publique pour les citoyens");
        }
        validateAssignableCenters(actor, request.getCenterIds());
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Cet email est déjà utilisé");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Ce numéro est déjà utilisé");
        }

        User user = User.builder()
                .email(request.getEmail().trim().toLowerCase())
                .phone(request.getPhone().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(true)
                .otpVerified(true)
                .build();
        userRepository.save(user);
        applyCenters(user, request.getCenterIds());
        userRepository.save(user);
        return toUserDto(user);
    }

    @Transactional
    public DtoMapper.AdminUserDto updateStaffUser(Long id, UpdateStaffUserRequest request, User actor) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Utilisateur non trouvé", HttpStatus.NOT_FOUND));
        if (user.getRole() == Role.CITOYEN) {
            throw new BusinessException("Modification réservée aux comptes staff");
        }
        if (request.getCenterIds() != null) {
            validateAssignableCenters(actor, request.getCenterIds());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getRole() != null) {
            if (request.getRole() == Role.CITOYEN) {
                throw new BusinessException("Impossible de convertir en citoyen via cette interface");
            }
            user.setRole(request.getRole());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setMustChangePassword(false);
        }
        if (request.getCenterIds() != null) {
            applyCenters(user, request.getCenterIds());
        }
        userRepository.save(user);
        return toUserDto(user);
    }

    @Transactional
    public void resetStaffPassword(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Utilisateur non trouvé", HttpStatus.NOT_FOUND));
        if (user.getRole() == Role.CITOYEN) {
            throw new BusinessException("Réinitialisation réservée aux comptes staff");
        }
        if (!user.isEnabled()) {
            throw new BusinessException("Impossible de réinitialiser un compte inactif");
        }

        String temporaryPassword = PasswordGenerator.generateTemporary(12);
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        userRepository.save(user);

        String displayName = staffFirstName(user);
        emailService.sendStaffTemporaryPasswordEmail(user, temporaryPassword, displayName);
    }

    @Transactional
    public void deleteStaffUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Utilisateur non trouvé", HttpStatus.NOT_FOUND));
        if (user.getRole() == Role.CITOYEN) {
            throw new BusinessException("Suppression réservée aux comptes staff");
        }
        if (user.getRole() == Role.SUPER_ADMIN) {
            throw new BusinessException("Impossible de supprimer un super administrateur");
        }
        user.setEnabled(false);
        userRepository.save(user);
    }

    @Transactional
    public void deleteCitizen(Long id) {
        Citizen citizen = citizenRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Citoyen non trouvé", HttpStatus.NOT_FOUND));
        User user = citizen.getUser();
        if (user.getRole() != Role.CITOYEN) {
            throw new BusinessException("Ce compte n'est pas un citoyen", HttpStatus.BAD_REQUEST);
        }
        if (!user.isEnabled()) {
            throw new BusinessException("Ce compte est déjà désactivé", HttpStatus.BAD_REQUEST);
        }
        user.setEnabled(false);
        userRepository.save(user);
    }

    public List<DtoMapper.CenterDto> listCenters(User currentUser) {
        User actor = resolveUserWithCenters(currentUser);
        List<Center> centers = centerRepository.findAllByOrderByCityAscNameAsc();
        if (actor.getRole() == Role.ADMIN || actor.getRole() == Role.SUPER_ADMIN) {
            return centers.stream().map(this::toCenterDto).toList();
        }
        Set<Long> allowed = actor.getCenters().stream().map(Center::getId).collect(Collectors.toSet());
        if (allowed.isEmpty()) {
            return List.of();
        }
        return centers.stream()
                .filter(c -> allowed.contains(c.getId()))
                .map(this::toCenterDto)
                .toList();
    }

    @Transactional
    public DtoMapper.CenterDto createCenter(CenterRequest request) {
        validateSchedule(request);
        Center center = Center.builder()
                .name(request.getName().trim())
                .city(request.getCity().trim())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .dailyCapacity(request.getDailyCapacity())
                .active(request.isActive())
                .openingDays(centerScheduleService.parseOpeningDays(request.getOpeningDays()))
                .openingTime(request.getOpeningTime())
                .closingTime(request.getClosingTime())
                .processingDelayDays(request.getProcessingDelayDays())
                .build();
        centerRepository.save(center);
        return toCenterDto(center);
    }

    @Transactional
    public DtoMapper.CenterDto updateCenter(Long id, CenterRequest request) {
        validateSchedule(request);
        Center center = centerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Centre non trouvé", HttpStatus.NOT_FOUND));
        center.setName(request.getName().trim());
        center.setCity(request.getCity().trim());
        center.setAddress(request.getAddress());
        center.setLatitude(request.getLatitude());
        center.setLongitude(request.getLongitude());
        center.setDailyCapacity(request.getDailyCapacity());
        center.setActive(request.isActive());
        center.setOpeningDays(centerScheduleService.parseOpeningDays(request.getOpeningDays()));
        center.setOpeningTime(request.getOpeningTime());
        center.setClosingTime(request.getClosingTime());
        center.setProcessingDelayDays(request.getProcessingDelayDays());
        centerRepository.save(center);
        return toCenterDto(center);
    }

    @Transactional
    public void deleteCenter(Long id) {
        Center center = centerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Centre non trouvé", HttpStatus.NOT_FOUND));

        if (appointmentRepository.existsByCenterId(id)) {
            center.setActive(false);
            centerRepository.save(center);
            return;
        }

        centerRepository.delete(center);
    }

    private void validateSchedule(CenterRequest request) {
        if (!request.getClosingTime().isAfter(request.getOpeningTime())) {
            throw new BusinessException(
                    "L'heure de fermeture doit être après l'heure d'ouverture",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private DtoMapper.AdminDossierSummaryDto toSummary(Dossier dossier) {
        Citizen citizen = dossier.getCitizen();
        String vehicleLabel = null;
        String chassisNumber = null;
        if (dossier.getVehicle() != null) {
            Vehicle v = dossier.getVehicle();
            vehicleLabel = v.getBrand() + " " + v.getModel();
            chassisNumber = v.getChassisNumber();
        }
        int requiredDocuments = dossier.getDocuments().size();
        int uploadedDocuments = (int) dossier.getDocuments().stream()
                .filter(d -> d.getFileName() != null && !d.getFileName().isBlank())
                .count();
        String centerName = null;
        java.time.LocalDate appointmentDate = null;
        java.time.LocalTime appointmentTime = null;
        if (dossier.getAppointment() != null) {
            Appointment appointment = dossier.getAppointment();
            centerName = appointment.getCenter().getName();
            appointmentDate = appointment.getAppointmentDate();
            appointmentTime = appointment.getAppointmentTime();
        }
        return DtoMapper.AdminDossierSummaryDto.builder()
                .id(dossier.getId())
                .referenceNumber(dossier.getReferenceNumber())
                .status(dossier.getStatus())
                .citizenName(citizen.getFirstName() + " " + citizen.getLastName())
                .citizenEmail(citizen.getUser().getEmail())
                .vehicleLabel(vehicleLabel)
                .chassisNumber(chassisNumber)
                .uploadedDocuments(uploadedDocuments)
                .requiredDocuments(requiredDocuments)
                .centerName(centerName)
                .appointmentDate(appointmentDate)
                .appointmentTime(appointmentTime)
                .createdAt(dossier.getCreatedAt())
                .updatedAt(dossier.getUpdatedAt())
                .build();
    }

    public User resolveUserWithCenters(User principal) {
        if (principal == null) {
            return null;
        }
        return userRepository.findByEmailWithCenters(principal.getEmail()).orElse(principal);
    }

    boolean canAccessDossier(User user, Dossier dossier) {
        if (user == null) {
            return true;
        }
        Role role = user.getRole();
        if (role == Role.ADMIN || role == Role.SUPER_ADMIN) {
            return true;
        }
        Set<Long> centerIds = user.getCenters().stream().map(Center::getId).collect(Collectors.toSet());
        if (role == Role.VALIDATEUR
                && (dossier.getStatus() == DossierStatus.SUBMITTED
                || dossier.getStatus() == DossierStatus.IN_REVIEW)) {
            return true;
        }
        if (dossier.getAppointment() != null && !centerIds.isEmpty()) {
            return centerIds.contains(dossier.getAppointment().getCenter().getId());
        }
        return false;
    }

    private void validateAssignableCenters(User actor, List<Long> centerIds) {
        if (centerIds == null || centerIds.isEmpty()) {
            return;
        }
        User resolved = resolveUserWithCenters(actor);
        if (resolved.getRole() == Role.ADMIN || resolved.getRole() == Role.SUPER_ADMIN) {
            return;
        }
        Set<Long> allowed = resolved.getCenters().stream().map(Center::getId).collect(Collectors.toSet());
        boolean invalid = centerIds.stream().anyMatch(id -> !allowed.contains(id));
        if (invalid) {
            throw new BusinessException("Vous ne pouvez associer que vos propres centres", HttpStatus.FORBIDDEN);
        }
    }

    private DtoMapper.AdminCitizenDto toCitizenDto(Citizen citizen) {
        User user = citizen.getUser();
        return DtoMapper.AdminCitizenDto.builder()
                .id(citizen.getId())
                .firstName(citizen.getFirstName())
                .lastName(citizen.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .nina(citizen.getNina())
                .dossierCount(dossierRepository.countByCitizenId(citizen.getId()))
                .enabled(user.isEnabled())
                .address(citizen.getAddress())
                .latitude(citizen.getLatitude())
                .longitude(citizen.getLongitude())
                .createdAt(citizen.getCreatedAt())
                .build();
    }

    private DtoMapper.AdminUserDto toUserDto(User user) {
        Citizen citizen = user.getCitizen();
        List<Long> centerIds = user.getCenters().stream().map(Center::getId).toList();
        List<String> centerNames = user.getCenters().stream()
                .map(c -> c.getName() + " (" + c.getCity() + ")")
                .toList();
        return DtoMapper.AdminUserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .firstName(citizen != null ? citizen.getFirstName() : staffFirstName(user))
                .lastName(citizen != null ? citizen.getLastName() : "")
                .createdAt(user.getCreatedAt())
                .mustChangePassword(user.isMustChangePassword())
                .centerIds(centerIds)
                .centerNames(centerNames)
                .build();
    }

    private void applyCenters(User user, List<Long> centerIds) {
        if (centerIds == null) {
            return;
        }
        if (centerIds.isEmpty()) {
            user.getCenters().clear();
            return;
        }
        Set<Center> centers = new HashSet<>();
        for (Long centerId : centerIds) {
            Center center = centerRepository.findById(centerId)
                    .orElseThrow(() -> new BusinessException("Centre introuvable : " + centerId, HttpStatus.BAD_REQUEST));
            centers.add(center);
        }
        user.setCenters(centers);
    }

    private String staffFirstName(User user) {
        String local = user.getEmail().substring(0, user.getEmail().indexOf('@'));
        local = local.replace('.', ' ').replace('_', ' ');
        if (local.isEmpty()) return "Utilisateur";
        return local.substring(0, 1).toUpperCase() + local.substring(1);
    }

    private DtoMapper.CenterDto toCenterDto(Center center) {
        DtoMapper.CenterDto dto = DtoMapper.CenterDto.builder()
                .id(center.getId())
                .name(center.getName())
                .city(center.getCity())
                .address(center.getAddress())
                .latitude(center.getLatitude())
                .longitude(center.getLongitude())
                .dailyCapacity(center.getDailyCapacity())
                .active(center.isActive())
                .build();
        return centerScheduleService.enrichCenterDto(center, dto);
    }

    // ── Rôles ──────────────────────────────────────────────────────────────

    public List<DtoMapper.AdminRoleDto> listRoles() {
        return roleDefinitionRepository.findAllByOrderByCodeAsc().stream()
                .map(this::toRoleDto)
                .toList();
    }

    public List<DtoMapper.PermissionDto> listPermissions() {
        return Arrays.stream(Permission.values())
                .map(p -> DtoMapper.PermissionDto.builder()
                        .code(p.name())
                        .label(p.getLabel())
                        .category(p.getCategory())
                        .build())
                .toList();
    }

    @Transactional
    public DtoMapper.AdminRoleDto updateRolePermissions(Long id, RolePermissionsRequest request) {
        RoleDefinition role = roleDefinitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Rôle non trouvé", HttpStatus.NOT_FOUND));
        if (role.getCode() == Role.SUPER_ADMIN && request.getPermissions() != null
                && request.getPermissions().size() < Permission.values().length) {
            throw new BusinessException("Le super administrateur doit conserver toutes les permissions");
        }
        Set<Permission> next = request.getPermissions() == null
                ? EnumSet.noneOf(Permission.class)
                : EnumSet.copyOf(request.getPermissions());
        role.setPermissions(next);
        roleDefinitionRepository.save(role);
        return toRoleDto(role);
    }

    @Transactional
    public DtoMapper.AdminRoleDto createRole(RoleDefinitionRequest request) {
        if (roleDefinitionRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Ce rôle existe déjà");
        }
        RoleDefinition role = RoleDefinition.builder()
                .code(request.getCode())
                .label(request.getLabel().trim())
                .description(blankToNull(request.getDescription()))
                .active(request.getActive() == null || request.getActive())
                .systemRole(false)
                .permissions(EnumSet.copyOf(DefaultRolePermissions.forRole(request.getCode())))
                .build();
        roleDefinitionRepository.save(role);
        return toRoleDto(role);
    }

    @Transactional
    public DtoMapper.AdminRoleDto updateRole(Long id, RoleDefinitionRequest request) {
        RoleDefinition role = roleDefinitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Rôle non trouvé", HttpStatus.NOT_FOUND));
        if (request.getLabel() != null && !request.getLabel().isBlank()) {
            role.setLabel(request.getLabel().trim());
        }
        if (request.getDescription() != null) {
            role.setDescription(blankToNull(request.getDescription()));
        }
        if (request.getActive() != null) {
            if (role.isSystemRole() && !request.getActive()) {
                throw new BusinessException("Impossible de désactiver un rôle système");
            }
            role.setActive(request.getActive());
        }
        roleDefinitionRepository.save(role);
        return toRoleDto(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        RoleDefinition role = roleDefinitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Rôle non trouvé", HttpStatus.NOT_FOUND));
        if (role.isSystemRole()) {
            throw new BusinessException("Impossible de supprimer un rôle système");
        }
        long count = userRepository.countByRole(role.getCode());
        if (count > 0) {
            throw new BusinessException("Ce rôle est encore attribué à " + count + " utilisateur(s)");
        }
        roleDefinitionRepository.delete(role);
    }

    // ── Notifications ──────────────────────────────────────────────────────

    public List<DtoMapper.AdminNotificationDto> listNotifications() {
        return notificationRepository.findAllWithUserOrderByCreatedAtDesc().stream()
                .map(this::toNotificationDto)
                .toList();
    }

    @Transactional
    public DtoMapper.AdminNotificationDto createNotification(AdminNotificationRequest request) {
        if (request.getUserEmail() == null || request.getUserEmail().isBlank()) {
            throw new BusinessException("L'email du destinataire est obligatoire");
        }
        User user = userRepository.findByEmail(request.getUserEmail().trim().toLowerCase())
                .orElseThrow(() -> new BusinessException("Utilisateur non trouvé", HttpStatus.NOT_FOUND));
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BusinessException("Le message est obligatoire");
        }
        if (request.getType() == null) {
            throw new BusinessException("Le type est obligatoire");
        }

        boolean sendEmail = request.getSendEmail() == null || request.getSendEmail();
        Notification notification = Notification.builder()
                .user(user)
                .message(request.getMessage().trim())
                .type(request.getType())
                .read(false)
                .build();
        notificationRepository.save(notification);
        if (sendEmail) {
            emailService.sendNotificationEmail(user, request.getType(), notification.getMessage());
        }
        return toNotificationDto(notification);
    }

    @Transactional
    public DtoMapper.AdminNotificationDto updateNotification(Long id, AdminNotificationRequest request) {
        Notification notification = notificationRepository.findByIdWithUser(id)
                .orElseThrow(() -> new BusinessException("Notification non trouvée", HttpStatus.NOT_FOUND));
        if (request.getMessage() != null && !request.getMessage().isBlank()) {
            notification.setMessage(request.getMessage().trim());
        }
        if (request.getType() != null) {
            notification.setType(request.getType());
        }
        if (request.getRead() != null) {
            notification.setRead(request.getRead());
        }
        notificationRepository.save(notification);
        return toNotificationDto(notification);
    }

    @Transactional
    public void deleteNotification(Long id) {
        if (!notificationRepository.existsById(id)) {
            throw new BusinessException("Notification non trouvée", HttpStatus.NOT_FOUND);
        }
        notificationRepository.deleteById(id);
    }

    // ── Paiements ────────────────────────────────────────────────────────

    public List<DtoMapper.AdminPaymentDto> listPayments() {
        return paymentRepository.findAllWithDossierOrderByIdDesc().stream()
                .map(this::toPaymentDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DtoMapper.AdminPaymentDto getPayment(Long id) {
        Payment payment = paymentRepository.findByIdWithDossier(id)
                .orElseThrow(() -> new BusinessException("Paiement non trouvé", HttpStatus.NOT_FOUND));
        return toPaymentDto(payment);
    }

    @Transactional
    public DtoMapper.AdminPaymentDto updatePayment(Long id, AdminPaymentUpdateRequest request) {
        Payment payment = paymentRepository.findByIdWithDossier(id)
                .orElseThrow(() -> new BusinessException("Paiement non trouvé", HttpStatus.NOT_FOUND));

        payment.setStatus(request.getStatus());
        if (request.getPaymentMethod() != null) {
            payment.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getTransactionId() != null && !request.getTransactionId().isBlank()) {
            payment.setTransactionId(request.getTransactionId().trim());
        }
        if (request.getStatus() == PaymentStatus.COMPLETED && payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDateTime.now());
        }
        if (request.getStatus() == PaymentStatus.PENDING || request.getStatus() == PaymentStatus.FAILED) {
            payment.setPaymentDate(null);
        }

        paymentRepository.save(payment);
        Dossier dossier = payment.getDossier();
        if (request.getStatus() == PaymentStatus.COMPLETED) {
            dossier.setStatus(DossierStatus.PAID);
        } else if (request.getStatus() == PaymentStatus.PENDING || request.getStatus() == PaymentStatus.PROCESSING) {
            dossier.setStatus(DossierStatus.PAYMENT_PENDING);
        }
        dossierRepository.save(dossier);
        return toPaymentDto(payment);
    }

    @Transactional
    public void deletePayment(Long id) {
        Payment payment = paymentRepository.findByIdWithDossier(id)
                .orElseThrow(() -> new BusinessException("Paiement non trouvé", HttpStatus.NOT_FOUND));
        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.FAILED) {
            throw new BusinessException("Seuls les paiements en attente ou échoués peuvent être supprimés");
        }
        Dossier dossier = payment.getDossier();
        dossier.setPayment(null);
        dossier.setStatus(DossierStatus.VALIDATED);
        dossierRepository.save(dossier);
        paymentRepository.delete(payment);
    }

    private DtoMapper.AdminRoleDto toRoleDto(RoleDefinition role) {
        List<String> permissionCodes = role.getPermissions().stream()
                .map(Permission::name)
                .sorted()
                .toList();
        if (permissionCodes.isEmpty()) {
            permissionCodes = DefaultRolePermissions.forRole(role.getCode()).stream()
                    .map(Permission::name)
                    .sorted()
                    .toList();
        }
        return DtoMapper.AdminRoleDto.builder()
                .id(role.getId())
                .code(role.getCode())
                .label(role.getLabel())
                .description(role.getDescription())
                .active(role.isActive())
                .systemRole(role.isSystemRole())
                .userCount(userRepository.countByRole(role.getCode()))
                .createdAt(role.getCreatedAt())
                .permissions(permissionCodes)
                .build();
    }

    private DtoMapper.AdminNotificationDto toNotificationDto(Notification notification) {
        User user = notification.getUser();
        Citizen citizen = user.getCitizen();
        String userName;
        if (citizen != null) {
            userName = citizen.getFirstName() + " " + citizen.getLastName();
        } else {
            userName = staffFirstName(user);
        }
        return DtoMapper.AdminNotificationDto.builder()
                .id(notification.getId())
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userName(userName.trim())
                .message(notification.getMessage())
                .type(notification.getType())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private DtoMapper.AdminPaymentDto toPaymentDto(Payment payment) {
        Dossier dossier = payment.getDossier();
        Citizen citizen = dossier.getCitizen();
        return DtoMapper.AdminPaymentDto.builder()
                .id(payment.getId())
                .dossierId(dossier.getId())
                .dossierReference(dossier.getReferenceNumber())
                .citizenName(citizen.getFirstName() + " " + citizen.getLastName())
                .citizenEmail(citizen.getUser().getEmail())
                .amount(payment.getAmount())
                .serviceFee(payment.getServiceFee())
                .totalAmount(payment.getTotalAmount())
                .transactionId(payment.getTransactionId())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .paymentDate(payment.getPaymentDate())
                .build();
    }
}
