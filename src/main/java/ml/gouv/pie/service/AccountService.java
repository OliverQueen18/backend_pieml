package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.AuthResponse;
import ml.gouv.pie.dto.ChangePasswordRequest;
import ml.gouv.pie.dto.RequiredPasswordChangeRequest;
import ml.gouv.pie.entity.Citizen;
import ml.gouv.pie.entity.User;
import ml.gouv.pie.entity.Center;
import ml.gouv.pie.exception.BusinessException;
import ml.gouv.pie.repository.UserRepository;
import ml.gouv.pie.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PermissionService permissionService;

    @Transactional
    public AuthResponse changePassword(User authUser, ChangePasswordRequest request) {
        User user = loadUser(authUser.getEmail());
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Mot de passe actuel incorrect", HttpStatus.UNAUTHORIZED);
        }
        applyNewPassword(user, request.getNewPassword());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse completeRequiredPasswordChange(User authUser, RequiredPasswordChangeRequest request) {
        User user = loadUser(authUser.getEmail());
        if (!user.isMustChangePassword()) {
            throw new BusinessException("Aucun changement de mot de passe requis", HttpStatus.BAD_REQUEST);
        }
        applyNewPassword(user, request.getNewPassword());
        return buildAuthResponse(user);
    }

    private void applyNewPassword(User user, String newPassword) {
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException("Le nouveau mot de passe doit être différent de l'actuel");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    private User loadUser(String email) {
        return userRepository.findByEmailWithCenters(email)
                .orElseThrow(() -> new BusinessException("Utilisateur non trouvé", HttpStatus.NOT_FOUND));
    }

    private AuthResponse buildAuthResponse(User user) {
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
                .token(jwtService.generateToken(user))
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
