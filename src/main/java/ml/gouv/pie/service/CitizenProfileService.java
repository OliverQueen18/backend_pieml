package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.ChangePasswordRequest;
import ml.gouv.pie.dto.CitizenProfileDto;
import ml.gouv.pie.dto.UpdateProfileRequest;
import ml.gouv.pie.entity.Citizen;
import ml.gouv.pie.entity.User;
import ml.gouv.pie.exception.BusinessException;
import ml.gouv.pie.repository.CitizenRepository;
import ml.gouv.pie.repository.UserRepository;
import ml.gouv.pie.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CitizenProfileService {

    private final UserRepository userRepository;
    private final CitizenRepository citizenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public CitizenProfileDto getProfile(User authUser) {
        User user = loadUser(authUser.getEmail());
        Citizen citizen = loadCitizen(user.getEmail());
        return toDto(user, citizen, null);
    }

    @Transactional
    public CitizenProfileDto updateProfile(User authUser, UpdateProfileRequest request) {
        throw new BusinessException(
                "La modification directe du profil n'est plus autorisée. "
                        + "Soumettez une réclamation avec pièce justificative depuis votre espace citoyen.",
                HttpStatus.FORBIDDEN);
    }

    @Transactional
    public void changePassword(User authUser, ChangePasswordRequest request) {
        User user = loadUser(authUser.getEmail());
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Mot de passe actuel incorrect", HttpStatus.UNAUTHORIZED);
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("Le nouveau mot de passe doit être différent de l'actuel");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Utilisateur non trouvé", HttpStatus.NOT_FOUND));
    }

    private Citizen loadCitizen(String email) {
        return citizenRepository.findByUser_Email(email)
                .orElseThrow(() -> new BusinessException("Profil citoyen introuvable", HttpStatus.NOT_FOUND));
    }

    private CitizenProfileDto toDto(User user, Citizen citizen, String token) {
        return CitizenProfileDto.builder()
                .firstName(citizen.getFirstName())
                .lastName(citizen.getLastName())
                .nina(citizen.getNina())
                .phone(user.getPhone())
                .email(user.getEmail())
                .address(citizen.getAddress())
                .latitude(citizen.getLatitude())
                .longitude(citizen.getLongitude())
                .token(token)
                .build();
    }
}
