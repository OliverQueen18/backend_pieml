package ml.gouv.pie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ml.gouv.pie.entity.enums.Role;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private boolean otpVerified;
    private boolean mustChangePassword;
    private List<String> permissions;
    private List<Long> centerIds;
    private List<String> centerNames;
}
