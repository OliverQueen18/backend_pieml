package ml.gouv.pie.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ml.gouv.pie.entity.enums.Permission;
import ml.gouv.pie.entity.enums.Role;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component("roleSchemaMigration")
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class RoleSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateRoleDefinitions() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS role_definitions (
                        id BIGSERIAL PRIMARY KEY,
                        code VARCHAR(32) NOT NULL UNIQUE,
                        label VARCHAR(120) NOT NULL,
                        description VARCHAR(500),
                        active BOOLEAN NOT NULL DEFAULT TRUE,
                        system_role BOOLEAN NOT NULL DEFAULT FALSE,
                        created_at TIMESTAMP
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS role_permissions (
                        role_id BIGINT NOT NULL REFERENCES role_definitions(id) ON DELETE CASCADE,
                        permission VARCHAR(64) NOT NULL,
                        PRIMARY KEY (role_id, permission)
                    )
                    """);
            refreshPermissionCheckConstraint();
            refreshRoleCodeCheckConstraints();
            log.info("Schéma role_definitions vérifié");
        } catch (Exception ex) {
            log.warn("Migration role_definitions : {}", ex.getMessage());
        }
    }

    private void refreshPermissionCheckConstraint() {
        String allowedValues = Arrays.stream(Permission.values())
                .map(p -> "'" + p.name() + "'")
                .collect(Collectors.joining(", "));

        jdbcTemplate.execute("ALTER TABLE role_permissions DROP CONSTRAINT IF EXISTS role_permissions_permission_check");
        jdbcTemplate.execute("""
                ALTER TABLE role_permissions
                ADD CONSTRAINT role_permissions_permission_check
                CHECK (permission IN (%s))
                """.formatted(allowedValues));
        log.info("Contrainte role_permissions_permission_check mise à jour");
    }

    private void refreshRoleCodeCheckConstraints() {
        String allowedRoles = Arrays.stream(Role.values())
                .map(r -> "'" + r.name() + "'")
                .collect(Collectors.joining(", "));

        dropMatchingCheckConstraints("role_definitions", "code");
        dropMatchingCheckConstraints("users", "role");

        jdbcTemplate.execute("ALTER TABLE role_definitions DROP CONSTRAINT IF EXISTS role_definitions_code_check");
        jdbcTemplate.execute("""
                ALTER TABLE role_definitions
                ADD CONSTRAINT role_definitions_code_check
                CHECK (code IN (%s))
                """.formatted(allowedRoles));

        jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
        jdbcTemplate.execute("""
                ALTER TABLE users
                ADD CONSTRAINT users_role_check
                CHECK (role IN (%s))
                """.formatted(allowedRoles));

        log.info("Contraintes de rôles mises à jour (inclut AUDIT)");
    }

    private void dropMatchingCheckConstraints(String tableName, String columnHint) {
        List<String> names = jdbcTemplate.query("""
                SELECT c.conname
                FROM pg_constraint c
                JOIN pg_class rel ON rel.oid = c.conrelid
                JOIN pg_namespace n ON n.oid = rel.relnamespace
                WHERE c.contype = 'c'
                  AND n.nspname = 'public'
                  AND rel.relname = ?
                  AND pg_get_constraintdef(c.oid) ILIKE ?
                """,
                (rs, rowNum) -> rs.getString(1),
                tableName,
                "%" + columnHint + "%");

        for (String name : names) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP CONSTRAINT IF EXISTS " + name);
        }
    }
}
