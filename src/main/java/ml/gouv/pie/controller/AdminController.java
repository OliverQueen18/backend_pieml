package ml.gouv.pie.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.*;
import ml.gouv.pie.entity.User;
import ml.gouv.pie.entity.enums.DossierStatus;
import ml.gouv.pie.entity.enums.ProfileChangeRequestStatus;
import ml.gouv.pie.service.AdminService;
import ml.gouv.pie.service.AppointmentService;
import ml.gouv.pie.service.DocumentService;
import ml.gouv.pie.service.PlateDeliveryService;
import ml.gouv.pie.service.ProfileChangeRequestService;
import ml.gouv.pie.service.ValidationService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ValidationService validationService;
    private final DocumentService documentService;
    private final AppointmentService appointmentService;
    private final PlateDeliveryService plateDeliveryService;
    private final ProfileChangeRequestService profileChangeRequestService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DtoMapper.AdminDashboardStatsDto>> dashboard(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getDashboardStats(user)));
    }

    @GetMapping("/dashboard/charts")
    public ResponseEntity<ApiResponse<DtoMapper.AdminDashboardChartsDto>> dashboardCharts(
            @RequestParam(defaultValue = "30d") String period,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getDashboardCharts(period, user)));
    }

    @GetMapping("/dossiers")
    public ResponseEntity<ApiResponse<List<DtoMapper.AdminDossierSummaryDto>>> listDossiers(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) DossierStatus status,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String citizen,
            @RequestParam(required = false) String chassis) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.listDossiers(status, reference, citizen, chassis, user)));
    }

    @GetMapping("/dossiers/{id}")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> getDossier(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getDossier(id, user)));
    }

    @GetMapping("/dossiers/{dossierId}/documents/{documentId}/file")
    public ResponseEntity<Resource> getDocumentFile(
            @PathVariable Long dossierId,
            @PathVariable Long documentId) {
        ml.gouv.pie.entity.Document document = documentService.getDocumentForDossier(dossierId, documentId);
        Resource resource = documentService.loadDocumentResource(document);
        return documentService.buildFileResponse(document, resource);
    }

    @PostMapping("/dossiers/bulk/validate")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<DtoMapper.DossierDto>>> validateBulk(
            @Valid @RequestBody DossierBulkActionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Dossiers validés",
                validationService.validateDossiers(request.getDossierIds())));
    }

    @PostMapping("/dossiers/bulk/reject")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<DtoMapper.DossierDto>>> rejectBulk(
            @Valid @RequestBody DossierBulkActionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Dossiers rejetés",
                validationService.rejectDossiers(
                        request.getDossierIds(),
                        request.getReason() != null ? request.getReason() : "Documents non conformes")));
    }

    @DeleteMapping("/dossiers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDossier(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        adminService.deleteDossier(id, user);
        return ResponseEntity.ok(ApiResponse.ok("Dossier supprimé", null));
    }

    @PostMapping("/dossiers/{dossierId}/documents/validate")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> validateDocuments(
            @PathVariable Long dossierId,
            @Valid @RequestBody DocumentBulkActionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Documents validés",
                documentService.validateDocuments(dossierId, request.getDocumentIds())));
    }

    @PostMapping("/dossiers/{dossierId}/documents/reject")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> rejectDocuments(
            @PathVariable Long dossierId,
            @Valid @RequestBody DocumentBulkActionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Documents rejetés",
                documentService.rejectDocuments(dossierId, request.getDocumentIds())));
    }

    @DeleteMapping("/dossiers/{dossierId}/documents/{documentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> deleteDocument(
            @PathVariable Long dossierId,
            @PathVariable Long documentId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Document supprimé",
                documentService.deleteDocument(dossierId, documentId)));
    }

    @PostMapping("/dossiers/{id}/validate")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> validate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Dossier validé", validationService.validateDossier(id)));
    }

    @PostMapping("/dossiers/{id}/confirm-appointment")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> confirmAppointment(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Rendez-vous confirmé",
                appointmentService.confirmAppointment(id, request)));
    }

    @PostMapping("/dossiers/{id}/start-immatriculation")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN', 'SUPER_ADMIN', 'IMMATRICULATEUR')")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> startImmatriculation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Immatriculation démarrée",
                validationService.startImmatriculation(id)));
    }

    @PostMapping("/dossiers/{id}/complete-immatriculation")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN', 'SUPER_ADMIN', 'IMMATRICULATEUR')")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> completeImmatriculation(
            @PathVariable Long id,
            @Valid @RequestBody CompleteImmatriculationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Dossier immatriculé",
                validationService.completeImmatriculation(id, request.getRegistrationNumber())));
    }

    @PostMapping("/dossiers/{id}/cancel-immatriculation")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN', 'SUPER_ADMIN', 'IMMATRICULATEUR')")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> cancelImmatriculation(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Immatriculation annulée",
                validationService.cancelImmatriculation(id, body.getOrDefault("reason", ""))));
    }

    @GetMapping("/centers/{id}/availability")
    public ResponseEntity<ApiResponse<CenterAvailabilityDto>> getCenterAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.getCenterAvailability(id)));
    }

    @PostMapping("/dossiers/{id}/reject")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> reject(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok("Dossier rejeté",
                validationService.rejectDossier(id, body.getOrDefault("reason", "Documents non conformes"))));
    }

    @PostMapping("/dossiers/{id}/remise-plaque")
    @PreAuthorize("hasAnyRole('IMMATRICULATEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> savePlateDelivery(
            @PathVariable Long id,
            @RequestParam String plateNumber,
            @RequestParam java.time.LocalDate deliveryDate,
            @RequestParam String collectorFirstName,
            @RequestParam String collectorLastName,
            @RequestParam String collectorPhone,
            @RequestParam String collectorAddress,
            @RequestParam(required = false) org.springframework.web.multipart.MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Remise de plaque enregistrée",
                plateDeliveryService.savePlateDelivery(
                        id, plateNumber, deliveryDate, collectorFirstName, collectorLastName,
                        collectorPhone, collectorAddress, file)));
    }

    @GetMapping("/dossiers/{dossierId}/remise-plaque/file")
    @PreAuthorize("hasAnyRole('IMMATRICULATEUR', 'ADMIN', 'SUPER_ADMIN', 'VALIDATEUR')")
    public ResponseEntity<Resource> getPlateDeliveryFile(@PathVariable Long dossierId) {
        var delivery = plateDeliveryService.getForDossier(dossierId);
        Resource resource = plateDeliveryService.loadAttachmentResource(delivery);
        return plateDeliveryService.buildFileResponse(delivery, resource);
    }

    @GetMapping("/citizens")
    public ResponseEntity<ApiResponse<List<DtoMapper.AdminCitizenDto>>> listCitizens() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listCitizens()));
    }

    @DeleteMapping("/citizens/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCitizen(@PathVariable Long id) {
        adminService.deleteCitizen(id);
        return ResponseEntity.ok(ApiResponse.ok("Compte citoyen désactivé", null));
    }

    @GetMapping("/profile-change-requests")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN', 'SUPER_ADMIN', 'IMMATRICULATEUR', 'AUDIT', 'UTILISATEUR')")
    public ResponseEntity<ApiResponse<List<AdminProfileChangeRequestDto>>> listProfileChangeRequests(
            @RequestParam(required = false) ProfileChangeRequestStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(profileChangeRequestService.listForAdmin(status)));
    }

    @GetMapping("/profile-change-requests/pending-count")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN', 'SUPER_ADMIN', 'IMMATRICULATEUR', 'AUDIT', 'UTILISATEUR')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> profileChangeRequestsPendingCount() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", profileChangeRequestService.countPending())));
    }

    @GetMapping("/profile-change-requests/{id}/file")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN', 'SUPER_ADMIN', 'IMMATRICULATEUR')")
    public ResponseEntity<Resource> getProfileChangeRequestFile(@PathVariable Long id) {
        return profileChangeRequestService.loadAttachment(id);
    }

    @PostMapping("/profile-change-requests/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminProfileChangeRequestDto>> approveProfileChangeRequest(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Réclamation approuvée",
                profileChangeRequestService.approve(id)));
    }

    @PostMapping("/profile-change-requests/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminProfileChangeRequestDto>> rejectProfileChangeRequest(
            @PathVariable Long id,
            @RequestBody(required = false) ProfileChangeRejectRequest request) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(ApiResponse.ok(
                "Réclamation rejetée",
                profileChangeRequestService.reject(id, reason)));
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<DtoMapper.AdminUserDto>>> listUsers() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listStaffUsers()));
    }

    @PostMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DtoMapper.AdminUserDto>> createUser(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateStaffUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Utilisateur créé", adminService.createStaffUser(request, user)));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DtoMapper.AdminUserDto>> updateUser(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateStaffUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Utilisateur mis à jour", adminService.updateStaffUser(id, request, user)));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        adminService.deleteStaffUser(id);
        return ResponseEntity.ok(ApiResponse.ok("Utilisateur désactivé", null));
    }

    @PostMapping("/users/{id}/reset-password")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resetUserPassword(@PathVariable Long id) {
        adminService.resetStaffPassword(id);
        return ResponseEntity.ok(ApiResponse.ok(
                "Mot de passe temporaire généré et envoyé par email", null));
    }

    @GetMapping("/centers")
    public ResponseEntity<ApiResponse<List<DtoMapper.CenterDto>>> listCenters(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listCenters(user)));
    }

    @PostMapping("/centers")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DtoMapper.CenterDto>> createCenter(
            @Valid @RequestBody CenterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Centre créé", adminService.createCenter(request)));
    }

    @PutMapping("/centers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DtoMapper.CenterDto>> updateCenter(
            @PathVariable Long id,
            @Valid @RequestBody CenterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Centre mis à jour", adminService.updateCenter(id, request)));
    }

    @DeleteMapping("/centers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCenter(@PathVariable Long id) {
        adminService.deleteCenter(id);
        return ResponseEntity.ok(ApiResponse.ok("Centre supprimé ou désactivé", null));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<DtoMapper.AdminRoleDto>>> listRoles() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listRoles()));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<DtoMapper.PermissionDto>>> listPermissions() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listPermissions()));
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DtoMapper.AdminRoleDto>> updateRolePermissions(
            @PathVariable Long id,
            @Valid @RequestBody RolePermissionsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Permissions mises à jour", adminService.updateRolePermissions(id, request)));
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DtoMapper.AdminRoleDto>> createRole(
            @Valid @RequestBody RoleDefinitionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Rôle créé", adminService.createRole(request)));
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DtoMapper.AdminRoleDto>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleDefinitionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Rôle mis à jour", adminService.updateRole(id, request)));
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        adminService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.ok("Rôle supprimé", null));
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'AUDIT', 'UTILISATEUR')")
    public ResponseEntity<ApiResponse<List<DtoMapper.AdminNotificationDto>>> listNotifications() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listNotifications()));
    }

    @PostMapping("/notifications")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DtoMapper.AdminNotificationDto>> createNotification(
            @Valid @RequestBody AdminNotificationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Notification envoyée", adminService.createNotification(request)));
    }

    @PutMapping("/notifications/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DtoMapper.AdminNotificationDto>> updateNotification(
            @PathVariable Long id,
            @Valid @RequestBody AdminNotificationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Notification mise à jour", adminService.updateNotification(id, request)));
    }

    @DeleteMapping("/notifications/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long id) {
        adminService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.ok("Notification supprimée", null));
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'AUDIT', 'PUBLIC')")
    public ResponseEntity<ApiResponse<List<DtoMapper.AdminPaymentDto>>> listPayments(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listPayments(user)));
    }

    @GetMapping("/payments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'AUDIT', 'PUBLIC')")
    public ResponseEntity<ApiResponse<DtoMapper.AdminPaymentDto>> getPayment(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getPayment(id, user)));
    }

    @PutMapping("/payments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DtoMapper.AdminPaymentDto>> updatePayment(
            @PathVariable Long id,
            @Valid @RequestBody AdminPaymentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Paiement mis à jour", adminService.updatePayment(id, request)));
    }

    @DeleteMapping("/payments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePayment(@PathVariable Long id) {
        adminService.deletePayment(id);
        return ResponseEntity.ok(ApiResponse.ok("Paiement supprimé", null));
    }
}
