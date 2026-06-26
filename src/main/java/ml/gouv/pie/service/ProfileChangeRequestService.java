package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.AdminProfileChangeRequestDto;
import ml.gouv.pie.dto.ProfileChangeRequestDto;
import ml.gouv.pie.entity.Citizen;
import ml.gouv.pie.entity.ProfileChangeRequest;
import ml.gouv.pie.entity.User;
import ml.gouv.pie.entity.enums.NotificationType;
import ml.gouv.pie.entity.enums.ProfileChangeField;
import ml.gouv.pie.entity.enums.ProfileChangeRequestStatus;
import ml.gouv.pie.exception.BusinessException;
import ml.gouv.pie.repository.CitizenRepository;
import ml.gouv.pie.repository.ProfileChangeRequestRepository;
import ml.gouv.pie.repository.UserRepository;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProfileChangeRequestService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{8,15}$");
    private static final Pattern NINA_PATTERN = Pattern.compile("^[A-Za-z0-9]{15}$");

    private final ProfileChangeRequestRepository requestRepository;
    private final CitizenRepository citizenRepository;
    private final UserRepository userRepository;
    private final StoredFileService storedFileService;
    private final NotificationService notificationService;
    private final DocumentService documentService;

    @Transactional(readOnly = true)
    public List<AdminProfileChangeRequestDto> listForAdmin(ProfileChangeRequestStatus status) {
        List<ProfileChangeRequest> requests = status == null
                ? requestRepository.findAllByOrderByCreatedAtDesc()
                : requestRepository.findByStatusOrderByCreatedAtDesc(status);
        return requests.stream().map(this::toAdminDto).toList();
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return requestRepository.countByStatus(ProfileChangeRequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> loadAttachment(Long requestId) {
        ProfileChangeRequest request = loadRequest(requestId);
        Path path = storedFileService.resolve(request.getFilePath());
        if (!Files.isRegularFile(path)) {
            throw new BusinessException("Fichier introuvable sur le serveur", HttpStatus.NOT_FOUND);
        }
        Resource resource = new FileSystemResource(path);
        String contentType = resolveContentType(request);
        return documentService.buildFileResponse(request.getFileName(), resource, contentType);
    }

    @Transactional
    public AdminProfileChangeRequestDto approve(Long requestId) {
        ProfileChangeRequest request = loadRequest(requestId);
        if (request.getStatus() != ProfileChangeRequestStatus.PENDING) {
            throw new BusinessException("Cette réclamation a déjà été traitée");
        }

        Citizen citizen = request.getCitizen();
        User user = citizen.getUser();
        applyApprovedChange(request, citizen, user);
        citizenRepository.save(citizen);
        userRepository.save(user);

        request.setStatus(ProfileChangeRequestStatus.APPROVED);
        requestRepository.save(request);

        notificationService.create(
                user,
                "Votre demande de modification de profil (« " + fieldLabel(request.getField()) + " ») a été approuvée.",
                NotificationType.INFO);

        return toAdminDto(request);
    }

    @Transactional
    public AdminProfileChangeRequestDto reject(Long requestId, String reason) {
        ProfileChangeRequest request = loadRequest(requestId);
        if (request.getStatus() != ProfileChangeRequestStatus.PENDING) {
            throw new BusinessException("Cette réclamation a déjà été traitée");
        }

        String rejectionReason = reason != null && !reason.isBlank()
                ? reason.trim()
                : "Demande non acceptée par l'administration";

        request.setStatus(ProfileChangeRequestStatus.REJECTED);
        requestRepository.save(request);

        User user = request.getCitizen().getUser();
        notificationService.create(
                user,
                "Votre demande de modification de profil (« " + fieldLabel(request.getField()) + " ») a été rejetée. "
                        + "Motif : " + rejectionReason,
                NotificationType.WARNING);

        return toAdminDto(request);
    }

    @Transactional(readOnly = true)
    public List<ProfileChangeRequestDto> listForCitizen(User authUser) {
        Citizen citizen = loadCitizen(authUser.getEmail());
        return requestRepository.findByCitizen_IdOrderByCreatedAtDesc(citizen.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ProfileChangeRequestDto submit(
            User authUser,
            ProfileChangeField field,
            String requestedValue,
            Double requestedLatitude,
            Double requestedLongitude,
            String reason,
            MultipartFile file) {
        Citizen citizen = loadCitizen(authUser.getEmail());
        User user = citizen.getUser();

        if (requestRepository.existsByCitizen_IdAndFieldAndStatus(
                citizen.getId(), field, ProfileChangeRequestStatus.PENDING)) {
            throw new BusinessException("Une réclamation est déjà en cours pour ce champ");
        }

        if (file == null || file.isEmpty()) {
            throw new BusinessException("La pièce justificative est obligatoire");
        }
        validateJustificatif(file);

        String normalizedValue = normalizeRequestedValue(field, requestedValue);
        validateRequestedValue(field, normalizedValue, requestedLatitude, requestedLongitude, citizen, user);

        String storedPath = storeAttachment(citizen, file);

        ProfileChangeRequest request = ProfileChangeRequest.builder()
                .citizen(citizen)
                .field(field)
                .requestedValue(normalizedValue)
                .requestedLatitude(field == ProfileChangeField.ADDRESS ? requestedLatitude : null)
                .requestedLongitude(field == ProfileChangeField.ADDRESS ? requestedLongitude : null)
                .reason(reason.trim())
                .fileName(file.getOriginalFilename())
                .filePath(storedPath)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .status(ProfileChangeRequestStatus.PENDING)
                .build();
        requestRepository.save(request);

        notificationService.create(
                user,
                "Votre demande de modification de profil (« " + fieldLabel(field) + " ») a été enregistrée. "
                        + "Elle sera traitée par l'administration.",
                NotificationType.INFO);

        return toDto(request);
    }

    private void validateRequestedValue(
            ProfileChangeField field,
            String value,
            Double latitude,
            Double longitude,
            Citizen citizen,
            User user) {
        switch (field) {
            case FIRST_NAME -> {
                if (value.equalsIgnoreCase(citizen.getFirstName())) {
                    throw new BusinessException("La nouvelle valeur est identique à l'actuelle");
                }
            }
            case LAST_NAME -> {
                if (value.equalsIgnoreCase(citizen.getLastName())) {
                    throw new BusinessException("La nouvelle valeur est identique à l'actuelle");
                }
            }
            case NINA -> {
                if (!NINA_PATTERN.matcher(value).matches()) {
                    throw new BusinessException("Le NINA doit contenir exactement 15 caractères alphanumériques");
                }
                if (value.equalsIgnoreCase(citizen.getNina())) {
                    throw new BusinessException("La nouvelle valeur est identique à l'actuelle");
                }
                if (citizenRepository.existsByNina(value)) {
                    throw new BusinessException("Ce NINA est déjà enregistré");
                }
            }
            case PHONE -> {
                if (!PHONE_PATTERN.matcher(value).matches()) {
                    throw new BusinessException("Numéro de téléphone invalide");
                }
                if (value.equals(user.getPhone())) {
                    throw new BusinessException("La nouvelle valeur est identique à l'actuelle");
                }
            }
            case EMAIL -> {
                if (!EMAIL_PATTERN.matcher(value).matches()) {
                    throw new BusinessException("Email invalide");
                }
                if (value.equalsIgnoreCase(user.getEmail())) {
                    throw new BusinessException("La nouvelle valeur est identique à l'actuelle");
                }
            }
            case ADDRESS -> {
                if (value.length() > 500) {
                    throw new BusinessException("L'adresse ne doit pas dépasser 500 caractères");
                }
                if (latitude == null || longitude == null) {
                    throw new BusinessException("La localisation sur la carte est obligatoire pour une nouvelle adresse");
                }
                if (value.equalsIgnoreCase(citizen.getAddress())
                        && latitude.equals(citizen.getLatitude())
                        && longitude.equals(citizen.getLongitude())) {
                    throw new BusinessException("La nouvelle adresse est identique à l'actuelle");
                }
            }
        }
    }

    private String normalizeRequestedValue(ProfileChangeField field, String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("La nouvelle valeur est obligatoire");
        }
        String trimmed = value.trim();
        return switch (field) {
            case FIRST_NAME, LAST_NAME, NINA -> trimmed.toUpperCase(Locale.ROOT);
            case EMAIL -> trimmed.toLowerCase(Locale.ROOT);
            case PHONE, ADDRESS -> trimmed;
        };
    }

    private String storeAttachment(Citizen citizen, MultipartFile file) {
        try {
            Path dir = storedFileService.uploadRoot()
                    .resolve("profile-requests")
                    .resolve(String.valueOf(citizen.getId()));
            Files.createDirectories(dir);
            String extension = getExtension(file.getOriginalFilename());
            Path target = dir.resolve(UUID.randomUUID() + extension);
            Files.copy(file.getInputStream(), target);
            return storedFileService.toStoredPath(target);
        } catch (IOException ex) {
            throw new BusinessException("Impossible d'enregistrer la pièce justificative", HttpStatus.INTERNAL_SERVER_ERROR);
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

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    private Citizen loadCitizen(String email) {
        return citizenRepository.findByUser_Email(email)
                .orElseThrow(() -> new BusinessException("Profil citoyen introuvable", HttpStatus.NOT_FOUND));
    }

    private ProfileChangeRequestDto toDto(ProfileChangeRequest request) {
        return ProfileChangeRequestDto.builder()
                .id(request.getId())
                .field(request.getField().name())
                .requestedValue(request.getRequestedValue())
                .requestedLatitude(request.getRequestedLatitude())
                .requestedLongitude(request.getRequestedLongitude())
                .reason(request.getReason())
                .fileName(request.getFileName())
                .status(request.getStatus().name())
                .createdAt(request.getCreatedAt())
                .build();
    }

    private AdminProfileChangeRequestDto toAdminDto(ProfileChangeRequest request) {
        Citizen citizen = request.getCitizen();
        User user = citizen.getUser();
        return AdminProfileChangeRequestDto.builder()
                .id(request.getId())
                .citizenId(citizen.getId())
                .citizenFirstName(citizen.getFirstName())
                .citizenLastName(citizen.getLastName())
                .citizenEmail(user.getEmail())
                .citizenPhone(user.getPhone())
                .citizenNina(citizen.getNina())
                .field(request.getField().name())
                .currentValue(currentValue(request.getField(), citizen, user))
                .requestedValue(request.getRequestedValue())
                .requestedLatitude(request.getRequestedLatitude())
                .requestedLongitude(request.getRequestedLongitude())
                .reason(request.getReason())
                .fileName(request.getFileName())
                .status(request.getStatus().name())
                .createdAt(request.getCreatedAt())
                .build();
    }

    private String currentValue(ProfileChangeField field, Citizen citizen, User user) {
        return switch (field) {
            case FIRST_NAME -> citizen.getFirstName();
            case LAST_NAME -> citizen.getLastName();
            case NINA -> citizen.getNina();
            case PHONE -> user.getPhone();
            case EMAIL -> user.getEmail();
            case ADDRESS -> citizen.getAddress();
        };
    }

    private void applyApprovedChange(ProfileChangeRequest request, Citizen citizen, User user) {
        ProfileChangeField field = request.getField();
        String value = request.getRequestedValue();
        switch (field) {
            case FIRST_NAME -> citizen.setFirstName(value);
            case LAST_NAME -> citizen.setLastName(value);
            case NINA -> {
                Optional<Citizen> existing = citizenRepository.findByNina(value);
                if (existing.isPresent() && !existing.get().getId().equals(citizen.getId())) {
                    throw new BusinessException("Ce NINA est déjà enregistré");
                }
                citizen.setNina(value);
            }
            case PHONE -> {
                if (userRepository.existsByPhone(value) && !value.equals(user.getPhone())) {
                    throw new BusinessException("Ce numéro de téléphone est déjà utilisé");
                }
                user.setPhone(value);
            }
            case EMAIL -> {
                if (userRepository.existsByEmail(value) && !value.equalsIgnoreCase(user.getEmail())) {
                    throw new BusinessException("Cet email est déjà utilisé");
                }
                user.setEmail(value);
            }
            case ADDRESS -> {
                citizen.setAddress(value);
                citizen.setLatitude(request.getRequestedLatitude());
                citizen.setLongitude(request.getRequestedLongitude());
            }
        }
    }

    private ProfileChangeRequest loadRequest(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException("Réclamation introuvable", HttpStatus.NOT_FOUND));
    }

    private String resolveContentType(ProfileChangeRequest request) {
        if (request.getContentType() != null && !request.getContentType().isBlank()) {
            return request.getContentType();
        }
        String fileName = request.getFileName();
        if (fileName == null) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private String fieldLabel(ProfileChangeField field) {
        return switch (field) {
            case FIRST_NAME -> "Prénom";
            case LAST_NAME -> "Nom";
            case NINA -> "NINA";
            case PHONE -> "Téléphone";
            case EMAIL -> "Email";
            case ADDRESS -> "Adresse";
        };
    }
}
