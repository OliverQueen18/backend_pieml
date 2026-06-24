package ml.gouv.pie.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ml.gouv.pie.entity.enums.Permission;

import java.util.List;

@Data
public class RolePermissionsRequest {
    @NotNull
    private List<Permission> permissions;
}
