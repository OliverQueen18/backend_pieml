package ml.gouv.pie.util;

import ml.gouv.pie.entity.User;
import ml.gouv.pie.repository.UserRepository;

import java.util.Optional;

public final class LoginIdentifierUtils {

    private LoginIdentifierUtils() {
    }

    public static Optional<User> findUser(UserRepository userRepository, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }

        String trimmed = identifier.trim();
        if (trimmed.contains("@")) {
            return userRepository.findByEmail(trimmed);
        }

        String normalized = normalizePhone(trimmed);
        return userRepository.findByPhone(normalized)
                .or(() -> userRepository.findByPhone(trimmed))
                .or(() -> userRepository.findByPhone(normalized.replace("+", "")));
    }

    public static String normalizePhone(String phone) {
        String digits = phone.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+223")) {
            return digits;
        }
        if (digits.startsWith("223") && digits.length() >= 11) {
            return "+" + digits;
        }
        if (digits.length() == 8) {
            return "+223" + digits;
        }
        return phone.trim();
    }
}
