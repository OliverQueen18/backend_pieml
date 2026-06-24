package ml.gouv.pie.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ml.gouv.pie.entity.enums.Permission;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
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
}
