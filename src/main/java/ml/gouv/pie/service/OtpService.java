package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.entity.User;
import ml.gouv.pie.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.expiration-minutes:10}")
    private int expirationMinutes;

    @Transactional
    public String generateAndStore(User user) {
        String otp = generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(expirationMinutes));
        userRepository.save(user);
        return otp;
    }

    public boolean isValid(User user, String submittedOtp) {
        if (user.getOtpCode() == null || user.getOtpExpiresAt() == null) {
            return false;
        }
        return user.getOtpCode().equals(submittedOtp)
                && user.getOtpExpiresAt().isAfter(LocalDateTime.now());
    }

    public int getExpirationMinutes() {
        return expirationMinutes;
    }

    @Transactional
    public void clear(User user) {
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        userRepository.save(user);
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
