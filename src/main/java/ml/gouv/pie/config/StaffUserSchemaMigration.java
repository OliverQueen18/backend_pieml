package ml.gouv.pie.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("staffUserSchemaMigration")
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class StaffUserSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateStaffUserColumns() {
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS first_name VARCHAR(120)");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS last_name VARCHAR(120)");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS address VARCHAR(500)");
            log.info("Colonnes staff users vérifiées (first_name, last_name, address)");
        } catch (Exception ex) {
            log.warn("Migration staff users : {}", ex.getMessage());
        }
    }
}
