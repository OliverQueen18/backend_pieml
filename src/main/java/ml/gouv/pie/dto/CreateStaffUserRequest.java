package ml.gouv.pie.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ml.gouv.pie.entity.enums.Role;

import java.util.List;

@Data
public class CreateStaffUserRequest {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String phone;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String address;

    @NotBlank @Size(min = 8)
    private String password;

    @NotNull
    private Role role;

    private List<Long> centerIds;
}
