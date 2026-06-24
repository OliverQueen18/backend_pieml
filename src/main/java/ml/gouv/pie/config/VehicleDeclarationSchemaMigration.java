package ml.gouv.pie.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("vehicleDeclarationSchemaMigration")
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class VehicleDeclarationSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateVehicleDeclarations() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS vehicle_declarations (
                        id BIGSERIAL PRIMARY KEY,
                        dossier_id BIGINT NOT NULL UNIQUE REFERENCES dossiers(id),
                        declaration_type VARCHAR(16) NOT NULL,
                        file_name VARCHAR(255) NOT NULL,
                        file_path VARCHAR(512) NOT NULL,
                        file_size BIGINT,
                        content_type VARCHAR(128),
                        declared_at TIMESTAMP NOT NULL
                    )
                    """);
            log.info("Schéma vehicle_declarations vérifié");
        } catch (Exception ex) {
            log.warn("Migration vehicle_declarations : {}", ex.getMessage());
        }
    }
}
