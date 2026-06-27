package ml.gouv.pie.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import ml.gouv.pie.entity.enums.Role;

import java.util.List;

@Data
public class UpdateStaffUserRequest {
    private String phone;
    private String firstName;
    private String lastName;
    private String address;
    private Role role;
    private Boolean enabled;

    @Size(min = 8)
    private String password;

    private List<Long> centerIds;
}
