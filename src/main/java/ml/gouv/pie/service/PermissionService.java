package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.entity.User;
import ml.gouv.pie.entity.enums.Permission;
import ml.gouv.pie.entity.enums.Role;
import ml.gouv.pie.repository.RoleDefinitionRepository;
import ml.gouv.pie.util.DefaultRolePermissions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final RoleDefinitionRepository roleDefinitionRepository;

    @Transactional(readOnly = true)
    public Set<Permission> resolvePermissions(Role role) {
        return roleDefinitionRepository.findByCode(role)
                .filter(def -> def.isActive() && def.getPermissions() != null && !def.getPermissions().isEmpty())
                .map(def -> Set.copyOf(def.getPermissions()))
                .orElseGet(() -> DefaultRolePermissions.forRole(role));
    }

    @Transactional(readOnly = true)
    public List<String> resolvePermissionCodes(Role role) {
        return resolvePermissions(role).stream()
                .map(Permission::name)
                .sorted()
                .collect(Collectors.toList());
    }

    public boolean hasPermission(User user, Permission permission) {
        if (user.getRole() == Role.SUPER_ADMIN) {
            return true;
        }
        return resolvePermissions(user.getRole()).contains(permission);
    }
}
