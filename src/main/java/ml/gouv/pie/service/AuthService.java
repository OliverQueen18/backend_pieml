package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.AuthResponse;
import ml.gouv.pie.dto.ForgotPasswordRequest;
import ml.gouv.pie.dto.LoginRequest;
import ml.gouv.pie.dto.OtpVerifyRequest;
import ml.gouv.pie.dto.RegisterRequest;
import ml.gouv.pie.dto.ResetPasswordRequest;
import ml.gouv.pie.dto.ResendOtpRequest;
import ml.gouv.pie.entity.Citizen;
import ml.gouv.pie.entity.User;
import ml.gouv.pie.entity.Center;
import ml.gouv.pie.entity.enums.NotificationType;
import ml.gouv.pie.entity.enums.Role;
import ml.gouv.pie.exception.BusinessException;
import ml.gouv.pie.util.LoginIdentifierUtils;
import ml.gouv.pie.repository.CitizenRepository;
import ml.gouv.pie.repository.NotificationRepository;
import ml.gouv.pie.repository.UserRepository;
import ml.gouv.pie.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CitizenRepository citizenRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final OtpService otpService;
    private final PermissionService permissionService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Cet email est déjà utilisé");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Ce numéro de téléphone est déjà utilisé");
        }
        if (citizenRepository.existsByNina(request.getNina())) {
            throw new BusinessException("Ce NINA est déjà enregistré");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.CITOYEN)
                .enabled(false)
                .otpVerified(false)
                .build();
        userRepository.save(user);

        Citizen citizen = Citizen.builder()
                .user(user)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .nina(request.getNina())
                .address(request.getAddress().trim())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
        citizenRepository.save(citizen);
        user.setCitizen(citizen);

        String otp = otpService.generateAndStore(user);
        emailService.sendOtpEmail(user, otp, request.getFirstName());

        notificationService.create(user,
                "Un code de vérification a été envoyé à votre adresse email.",
                NotificationType.INFO, false);

        return buildAuthResponse(user, null);
    }

    @Transactional
    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Utilisateur non trouvé", HttpStatus.NOT_FOUND));

        if (!otpService.isValid(user, request.getOtp())) {
            throw new BusinessException("Code OTP invalide ou expiré");
        }

        user.setOtpVerified(true);
        user.setEnabled(true);
        otpService.clear(user);

        String token = jwtService.generateToken(user);
        notificationService.create(user, "Compte activé avec succès", NotificationType.SUCCESS);

        return buildAuthResponse(user, token);
    }

    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new BusinessException("Utilisateur non trouvé", HttpStatus.NOT_FOUND));

        if (user.isOtpVerified() || user.isEnabled()) {
            throw new BusinessException("Ce compte est déjà activé");
        }

        String otp = otpService.generateAndStore(user);
        String firstName = user.getCitizen() != null ? user.getCitizen().getFirstName() : "Utilisateur";
        emailService.sendOtpEmail(user, otp, firstName);

        notificationService.create(user,
                "Un nouveau code de vérification a été envoyé à votre adresse email.",
                NotificationType.INFO, false);
    }

    @Transactional
    public void cancelPendingRegistration(ResendOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElse(null);
        if (user == null) {
            return;
        }
        if (user.isOtpVerified() || user.isEnabled()) {
            throw new BusinessException("Ce compte est déjà activé");
        }
        if (user.getRole() != Role.CITOYEN) {
            throw new BusinessException("Annulation impossible pour ce compte");
        }

        otpService.clear(user);
        notificationRepository.deleteByUserId(user.getId());
        citizenRepository.findByUserId(user.getId()).ifPresent(citizenRepository::delete);
        userRepository.delete(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = LoginIdentifierUtils.findUser(userRepository, request.getIdentifier())
                .orElseThrow(() -> new BusinessException("Identifiants incorrects", HttpStatus.UNAUTHORIZED));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword()));

        if (!user.isEnabled()) {
            throw new BusinessException("Compte non activé. Vérifiez votre email pour le code OTP.");
        }

        user = userRepository.findByEmailWithCenters(user.getEmail()).orElse(user);
        String token = jwtService.generateToken(user);
        return buildAuthResponse(user, token);
    }

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        LoginIdentifierUtils.findUser(userRepository, request.getIdentifier()).ifPresent(user -> {
            if (!user.isEnabled()) {
                return;
            }
            String otp = otpService.generateAndStore(user);
            String firstName = user.getCitizen() != null ? user.getCitizen().getFirstName() : "Utilisateur";
            emailService.sendPasswordResetEmail(user, otp, firstName);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = LoginIdentifierUtils.findUser(userRepository, request.getIdentifier())
                .orElseThrow(() -> new BusinessException("Code invalide ou expiré"));

        if (!otpService.isValid(user, request.getOtp())) {
            throw new BusinessException("Code invalide ou expiré");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        otpService.clear(user);
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        Citizen citizen = user.getCitizen();
        String firstName;
        String lastName;
        if (citizen != null) {
            firstName = citizen.getFirstName();
            lastName = citizen.getLastName();
        } else {
            String local = user.getEmail().substring(0, user.getEmail().indexOf('@'));
            local = local.replace('.', ' ').replace('_', ' ');
            firstName = local.isEmpty() ? "Utilisateur" : capitalize(local);
            lastName = "";
        }
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstName(firstName)
                .lastName(lastName)
                .role(user.getRole())
                .otpVerified(user.isOtpVerified())
                .mustChangePassword(user.isMustChangePassword())
                .permissions(permissionService.resolvePermissionCodes(user.getRole()))
                .centerIds(user.getCenters().stream().map(Center::getId).toList())
                .centerNames(user.getCenters().stream()
                        .map(c -> c.getName() + " (" + c.getCity() + ")")
                        .toList())
                .build();
    }

    private String capitalize(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}
