package ml.gouv.pie.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("profileChangeSchemaMigration")
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class ProfileChangeSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrate() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS profile_change_requests (
                        id BIGSERIAL PRIMARY KEY,
                        citizen_id BIGINT NOT NULL REFERENCES citizens(id),
                        field VARCHAR(32) NOT NULL,
                        requested_value VARCHAR(500) NOT NULL,
                        requested_latitude DOUBLE PRECISION,
                        requested_longitude DOUBLE PRECISION,
                        reason VARCHAR(2000) NOT NULL,
                        file_name VARCHAR(255) NOT NULL,
                        file_path VARCHAR(500) NOT NULL,
                        content_type VARCHAR(128),
                        file_size BIGINT,
                        status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                        created_at TIMESTAMP NOT NULL DEFAULT NOW()
                    )
                    """);
            log.info("Schéma profile_change_requests vérifié");
        } catch (Exception ex) {
            log.warn("Migration profile_change_requests : {}", ex.getMessage());
        }
    }
}
