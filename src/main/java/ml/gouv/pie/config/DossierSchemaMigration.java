package ml.gouv.pie.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ml.gouv.pie.entity.enums.DossierStatus;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

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
            refreshStatusCheckConstraint();
            log.info("Schéma dossiers — centre de traitement et statuts vérifiés");
        } catch (Exception ex) {
            log.warn("Migration dossiers : {}", ex.getMessage());
        }
    }

    private void refreshStatusCheckConstraint() {
        String allowedValues = Arrays.stream(DossierStatus.values())
                .map(s -> "'" + s.name() + "'")
                .collect(Collectors.joining(", "));

        jdbcTemplate.execute("ALTER TABLE dossiers DROP CONSTRAINT IF EXISTS dossiers_status_check");
        jdbcTemplate.execute("""
                ALTER TABLE dossiers
                ADD CONSTRAINT dossiers_status_check
                CHECK (status IN (%s))
                """.formatted(allowedValues));
        log.info("Contrainte dossiers_status_check mise à jour");
    }
}
