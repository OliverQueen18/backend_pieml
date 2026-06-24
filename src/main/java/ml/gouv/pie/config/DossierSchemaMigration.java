package ml.gouv.pie.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("dossierSchemaMigration")
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class DossierSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateProcessingCenter() {
        try {
            jdbcTemplate.execute("""
                    ALTER TABLE dossiers
                    ADD COLUMN IF NOT EXISTS processing_center_id BIGINT
                    REFERENCES centers(id)
                    """);
            log.info("Schéma dossiers — centre de traitement vérifié");
        } catch (Exception ex) {
            log.warn("Migration dossiers processing_center_id : {}", ex.getMessage());
        }
    }
}
