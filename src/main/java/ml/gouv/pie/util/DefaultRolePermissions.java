package ml.gouv.pie.util;

import ml.gouv.pie.entity.enums.Permission;
import ml.gouv.pie.entity.enums.Role;

import java.util.EnumSet;
import java.util.Set;

public final class DefaultRolePermissions {

    private DefaultRolePermissions() {
    }

    public static Set<Permission> forRole(Role role) {
        return switch (role) {
            case SUPER_ADMIN, ADMIN -> EnumSet.allOf(Permission.class);
            case VALIDATEUR -> EnumSet.of(
                    Permission.ADMIN_DASHBOARD_VIEW,
                    Permission.DOSSIERS_VIEW,
                    Permission.DOSSIERS_VALIDATE,
                    Permission.DOSSIERS_REJECT,
                    Permission.CITIZENS_VIEW
            );
            case IMMATRICULATEUR -> EnumSet.of(
                    Permission.ADMIN_DASHBOARD_VIEW,
                    Permission.DOSSIERS_VIEW,
                    Permission.CITIZENS_VIEW,
                    Permission.APPOINTMENTS_MANAGE,
                    Permission.IMMATRICULATION_PROCESS
            );
            case CITOYEN -> EnumSet.noneOf(Permission.class);
        };
    }
}
