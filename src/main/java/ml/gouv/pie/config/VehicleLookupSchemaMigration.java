package ml.gouv.pie.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("vehicleLookupSchemaMigration")
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class VehicleLookupSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateVehicleLookupTables() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS vehicle_brands (
                        id BIGSERIAL PRIMARY KEY,
                        code VARCHAR(50) NOT NULL UNIQUE,
                        libelle VARCHAR(150) NOT NULL,
                        description VARCHAR(500),
                        actif BOOLEAN NOT NULL DEFAULT TRUE,
                        ordre INTEGER NOT NULL DEFAULT 0,
                        created_at TIMESTAMP,
                        updated_at TIMESTAMP
                    )
                    """);

            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS vehicle_types (
                        id BIGSERIAL PRIMARY KEY,
                        code VARCHAR(50) NOT NULL UNIQUE,
                        libelle VARCHAR(150) NOT NULL,
                        description VARCHAR(500),
                        actif BOOLEAN NOT NULL DEFAULT TRUE,
                        ordre INTEGER NOT NULL DEFAULT 0,
                        created_at TIMESTAMP,
                        updated_at TIMESTAMP
                    )
                    """);

            jdbcTemplate.execute("ALTER TABLE vehicle_brands ADD COLUMN IF NOT EXISTS actif BOOLEAN NOT NULL DEFAULT TRUE");
            jdbcTemplate.execute("ALTER TABLE vehicle_brands ADD COLUMN IF NOT EXISTS ordre INTEGER NOT NULL DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE vehicle_brands ADD COLUMN IF NOT EXISTS description VARCHAR(500)");
            jdbcTemplate.execute("ALTER TABLE vehicle_brands ADD COLUMN IF NOT EXISTS created_at TIMESTAMP");
            jdbcTemplate.execute("ALTER TABLE vehicle_brands ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP");

            jdbcTemplate.execute("ALTER TABLE vehicle_types ADD COLUMN IF NOT EXISTS actif BOOLEAN NOT NULL DEFAULT TRUE");
            jdbcTemplate.execute("ALTER TABLE vehicle_types ADD COLUMN IF NOT EXISTS ordre INTEGER NOT NULL DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE vehicle_types ADD COLUMN IF NOT EXISTS description VARCHAR(500)");
            jdbcTemplate.execute("ALTER TABLE vehicle_types ADD COLUMN IF NOT EXISTS created_at TIMESTAMP");
            jdbcTemplate.execute("ALTER TABLE vehicle_types ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP");

            jdbcTemplate.execute("ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS brand_id BIGINT REFERENCES vehicle_brands(id)");
            jdbcTemplate.execute("ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS vehicle_type_id BIGINT REFERENCES vehicle_types(id)");
            jdbcTemplate.execute("ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS vehicle_type VARCHAR(150)");
            jdbcTemplate.execute("ALTER TABLE vehicles ALTER COLUMN engine_number DROP NOT NULL");

            ensureDefaultLookups();

            log.info("Schéma vehicle_brands / vehicle_types vérifié");
        } catch (Exception ex) {
            log.warn("Migration vehicle lookup : {}", ex.getMessage());
        }
    }

    private void ensureDefaultLookups() {
        insertBrand("TVS", "TVS", 1);
        insertBrand("BOXER_BAJAJ", "Boxer (Bajaj)", 2);
        insertBrand("APSONIC", "Apsonic", 3);
        insertBrand("AOJUN", "Aojun", 4);
        insertBrand("SANYA", "Sanya", 5);
        insertBrand("HAOJUE", "Haojue", 6);
        insertBrand("DAYUN", "Dayun", 7);
        insertBrand("LIFAN", "Lifan", 8);
        insertBrand("JINCHENG", "Jincheng", 9);
        insertBrand("SENKE", "Senke", 10);
        insertBrand("HONDA", "Honda", 11);
        insertBrand("YAMAHA", "Yamaha", 12);
        insertBrand("SUZUKI", "Suzuki", 13);
        insertBrand("KAWASAKI", "Kawasaki", 14);
        insertBrand("KTM", "KTM", 15);
        insertBrand("ROYAL_ENFIELD", "Royal Enfield", 16);
        insertBrand("AUTRE", "Autre", 17);

        insertType("MOTO", "Moto", 1);
        insertType("JAKARTA", "Jakarta", 2);
        insertType("TRICYCLE", "Tricycle", 3);
        insertType("QUADRICYCLE", "Quadricycle", 4);
        insertType("SCOOTER", "Scooter", 5);
    }

    private void insertBrand(String code, String libelle, int ordre) {
        jdbcTemplate.update("""
                INSERT INTO vehicle_brands (code, libelle, actif, ordre, created_at, updated_at)
                VALUES (?, ?, TRUE, ?, NOW(), NOW())
                ON CONFLICT (code) DO NOTHING
                """, code, libelle, ordre);
    }

    private void insertType(String code, String libelle, int ordre) {
        jdbcTemplate.update("""
                INSERT INTO vehicle_types (code, libelle, actif, ordre, created_at, updated_at)
                VALUES (?, ?, TRUE, ?, NOW(), NOW())
                ON CONFLICT (code) DO NOTHING
                """, code, libelle, ordre);
    }
}
