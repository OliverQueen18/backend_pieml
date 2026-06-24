package ml.gouv.pie.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("citizenSchemaMigration")
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class CitizenSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateCitizenLocationColumns() {
        try {
            jdbcTemplate.execute("ALTER TABLE citizens ADD COLUMN IF NOT EXISTS address VARCHAR(500)");
            jdbcTemplate.execute("ALTER TABLE citizens ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION");
            jdbcTemplate.execute("ALTER TABLE citizens ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION");

            jdbcTemplate.update("""
                    UPDATE citizens
                    SET address = 'Bamako, Mali'
                    WHERE address IS NULL OR TRIM(address) = ''
                    """);

            jdbcTemplate.update("""
                    UPDATE citizens
                    SET latitude = 12.6392
                    WHERE latitude IS NULL
                    """);

            jdbcTemplate.update("""
                    UPDATE citizens
                    SET longitude = -8.0029
                    WHERE longitude IS NULL
                    """);

            jdbcTemplate.execute("ALTER TABLE citizens ALTER COLUMN address SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE citizens ALTER COLUMN latitude SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE citizens ALTER COLUMN longitude SET NOT NULL");

            log.info("Schéma citizens — colonnes adresse/coordonnées vérifiées");
        } catch (Exception ex) {
            log.warn("Migration citizens adresse/coordonnées : {}", ex.getMessage());
        }
    }
}
