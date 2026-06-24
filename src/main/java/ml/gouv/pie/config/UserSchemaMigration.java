package ml.gouv.pie.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("userSchemaMigration")
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class UserSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateUserColumns() {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE");

            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS user_centers (
                        user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        center_id BIGINT NOT NULL REFERENCES centers(id) ON DELETE CASCADE,
                        PRIMARY KEY (user_id, center_id)
                    )
                    """);

            log.info("Schéma users — must_change_password et user_centers vérifiés");
        } catch (Exception ex) {
            log.warn("Migration users : {}", ex.getMessage());
        }
    }
}
