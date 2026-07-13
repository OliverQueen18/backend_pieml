package ml.gouv.pie.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.*;
import ml.gouv.pie.entity.User;
import ml.gouv.pie.entity.enums.ProfileChangeField;
import ml.gouv.pie.entity.enums.VehicleDeclarationType;
import ml.gouv.pie.service.*;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/citizen")
@RequiredArgsConstructor
public class CitizenController {

    private final DossierService dossierService;
    private final DocumentService documentService;
    private final PaymentService paymentService;
    private final AppointmentService appointmentService;
    private final VehicleDeclarationService vehicleDeclarationService;
    private final PlateDeliveryService plateDeliveryService;
    private final NotificationService notificationService;
    private final CitizenProfileService profileService;
    private final ProfileChangeRequestService profileChangeRequestService;
    private final ContactService contactService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DtoMapper.DashboardStatsDto>> dashboard(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(dossierService.getDashboardStats(user.getEmail())));
    }

    @GetMapping("/dossiers")
    public ResponseEntity<ApiResponse<List<DtoMapper.DossierDto>>> myDossiers(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(dossierService.getMyDossiers(user.getEmail())));
    }

    @GetMapping("/dossiers/{id}")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> getDossier(
            @AuthenticationPrincipal User user, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(dossierService.getDossier(user.getEmail(), id)));
    }

    @PostMapping("/dossiers")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> createDossier(
            @AuthenticationPrincipal User user, @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Dossier créé", dossierService.createDossier(user.getEmail(), request)));
    }

    @PostMapping("/dossiers/{id}/submit")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> submitDossier(
            @AuthenticationPrincipal User user, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Dossier soumis", dossierService.submitDossier(user.getEmail(), id)));
    }

    @PostMapping("/dossiers/{id}/resubmit")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> resubmitRejectedDossier(
            @AuthenticationPrincipal User user, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Dossier resoumis",
                dossierService.resubmitRejectedDossier(user.getEmail(), id)));
    }

    @DeleteMapping("/dossiers/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDraftDossier(
            @AuthenticationPrincipal User user, @PathVariable Long id) {
        dossierService.deleteDraftDossier(user.getEmail(), id);
        return ResponseEntity.ok(ApiResponse.ok("Dossier supprimé", null));
    }

    @PostMapping("/dossiers/{id}/declaration")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> declareVehicle(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestParam VehicleDeclarationType declarationType,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Déclaration enregistrée",
                vehicleDeclarationService.declareVehicle(user.getEmail(), id, declarationType, file)));
    }

    @GetMapping("/dossiers/{id}/declaration/file")
    public ResponseEntity<Resource> getDeclarationFile(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        var declaration = vehicleDeclarationService.getOwnedDeclaration(user.getEmail(), id);
        Resource resource = vehicleDeclarationService.loadDeclarationResource(declaration);
        return documentService.buildFileResponse(
                declaration.getFileName(),
                resource,
                vehicleDeclarationService.resolveContentType(declaration));
    }

    @GetMapping("/dossiers/{id}/remise-plaque/file")
    public ResponseEntity<Resource> getPlateDeliveryFile(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        var delivery = plateDeliveryService.getOwnedForDossier(user.getEmail(), id);
        Resource resource = plateDeliveryService.loadAttachmentResource(delivery);
        return plateDeliveryService.buildFileResponse(delivery, resource);
    }

    @PostMapping("/dossiers/{id}/documents")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> uploadDocument(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestParam Long typeDocumentId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok("Document téléversé",
                documentService.uploadDocument(user.getEmail(), id, typeDocumentId, file)));
    }

    @GetMapping("/dossiers/{dossierId}/documents/{documentId}/file")
    public ResponseEntity<Resource> getDocumentFile(
            @AuthenticationPrincipal User user,
            @PathVariable Long dossierId,
            @PathVariable Long documentId) {
        ml.gouv.pie.entity.Document document = documentService.getOwnedDocument(
                user.getEmail(), dossierId, documentId);
        Resource resource = documentService.loadDocumentResource(document);
        return documentService.buildFileResponse(document, resource);
    }

    @PostMapping("/dossiers/{id}/payment")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> initiatePayment(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.initiatePayment(user.getEmail(), id, request)));
    }

    @PostMapping("/dossiers/{id}/payment/confirm")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> confirmPayment(
            @AuthenticationPrincipal User user, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Paiement confirmé",
                paymentService.confirmPayment(user.getEmail(), id)));
    }

    @GetMapping("/centers")
    public ResponseEntity<ApiResponse<List<DtoMapper.CenterDto>>> getCenters() {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.getActiveCenters()));
    }

    @GetMapping("/centers/{id}/availability")
    public ResponseEntity<ApiResponse<CenterAvailabilityDto>> getCenterAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.getCenterAvailability(id)));
    }

    @PostMapping("/dossiers/{id}/appointment")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> scheduleAppointment(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Rendez-vous confirmé",
                appointmentService.scheduleAppointment(user.getEmail(), id, request)));
    }

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<DtoMapper.NotificationDto>>> notifications(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getMyNotifications(user)));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<CitizenProfileDto>> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getProfile(user)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<CitizenProfileDto>> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Profil mis à jour", profileService.updateProfile(user, request)));
    }

    @GetMapping("/profile/change-requests")
    public ResponseEntity<ApiResponse<List<ProfileChangeRequestDto>>> listProfileChangeRequests(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(profileChangeRequestService.listForCitizen(user)));
    }

    @PostMapping(value = "/profile/change-requests", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ProfileChangeRequestDto>> submitProfileChangeRequest(
            @AuthenticationPrincipal User user,
            @RequestParam ProfileChangeField field,
            @RequestParam String requestedValue,
            @RequestParam(required = false) Double requestedLatitude,
            @RequestParam(required = false) Double requestedLongitude,
            @RequestParam String reason,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Réclamation enregistrée",
                profileChangeRequestService.submit(
                        user, field, requestedValue, requestedLatitude, requestedLongitude, reason, file)));
    }

    @PutMapping("/security/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(user, request);
        return ResponseEntity.ok(ApiResponse.ok("Mot de passe modifié avec succès", null));
    }

    @PostMapping("/contact")
    public ResponseEntity<ApiResponse<Void>> contact(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ContactRequest request) {
        contactService.sendContactMessage(request, user);
        return ResponseEntity.ok(ApiResponse.ok("Message envoyé. Nous vous répondrons sous 48 h.", null));
    }

    @GetMapping("/track")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> trackDossier(
            @RequestParam("reference") String reference) {
        return ResponseEntity.ok(ApiResponse.ok(dossierService.findByReference(reference.trim())));
    }
}
