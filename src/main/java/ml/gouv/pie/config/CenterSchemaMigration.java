package ml.gouv.pie.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("centerSchemaMigration")
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class CenterSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateCenterScheduleColumns() {
        try {
            jdbcTemplate.execute("ALTER TABLE centers ADD COLUMN IF NOT EXISTS opening_time TIME");
            jdbcTemplate.execute("ALTER TABLE centers ADD COLUMN IF NOT EXISTS closing_time TIME");
            jdbcTemplate.execute("ALTER TABLE centers ADD COLUMN IF NOT EXISTS processing_delay_days INTEGER");
            jdbcTemplate.execute("ALTER TABLE centers ADD COLUMN IF NOT EXISTS phone VARCHAR(30)");

            jdbcTemplate.update("""
                    UPDATE centers
                    SET opening_time = TIME '08:00:00'
                    WHERE opening_time IS NULL
                    """);

            jdbcTemplate.update("""
                    UPDATE centers
                    SET closing_time = TIME '17:00:00'
                    WHERE closing_time IS NULL
                    """);

            jdbcTemplate.update("""
                    UPDATE centers
                    SET processing_delay_days = 3
                    WHERE processing_delay_days IS NULL
                    """);

            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS center_opening_days (
                        center_id BIGINT NOT NULL REFERENCES centers(id),
                        day_of_week VARCHAR(16) NOT NULL,
                        PRIMARY KEY (center_id, day_of_week)
                    )
                    """);

            jdbcTemplate.update("""
                    INSERT INTO center_opening_days (center_id, day_of_week)
                    SELECT c.id, d.day
                    FROM centers c
                    CROSS JOIN (VALUES
                        ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY')
                    ) AS d(day)
                    WHERE NOT EXISTS (
                        SELECT 1 FROM center_opening_days cod WHERE cod.center_id = c.id
                    )
                    """);

            jdbcTemplate.execute("ALTER TABLE centers ALTER COLUMN opening_time SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE centers ALTER COLUMN closing_time SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE centers ALTER COLUMN processing_delay_days SET NOT NULL");

            log.info("Schéma centers — horaires et délai de traitement vérifiés");
        } catch (Exception ex) {
            log.warn("Migration centers horaires : {}", ex.getMessage());
        }
    }
}
