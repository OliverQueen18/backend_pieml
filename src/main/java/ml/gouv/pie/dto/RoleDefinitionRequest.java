package ml.gouv.pie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ml.gouv.pie.entity.enums.Role;

@Data
public class RoleDefinitionRequest {
    @NotNull
    private Role code;

    @NotBlank
    @Size(max = 120)
    private String label;

    @Size(max = 500)
    private String description;

    private Boolean active;
}
