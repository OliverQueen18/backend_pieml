package ml.gouv.pie.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import ml.gouv.pie.entity.enums.Role;

import java.util.List;

@Data
public class UpdateStaffUserRequest {
    private String phone;
    private Role role;
    private Boolean enabled;

    @Size(min = 8)
    private String password;

    private List<Long> centerIds;
}
