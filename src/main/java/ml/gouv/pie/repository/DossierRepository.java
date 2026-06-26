package ml.gouv.pie.repository;

import ml.gouv.pie.entity.Dossier;
import ml.gouv.pie.entity.enums.DossierStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DossierRepository extends JpaRepository<Dossier, Long> {
    List<Dossier> findByCitizenIdOrderByCreatedAtDesc(Long citizenId);
    List<Dossier> findAllByOrderByCreatedAtDesc();
    List<Dossier> findByStatusOrderByCreatedAtDesc(DossierStatus status);
    Optional<Dossier> findByReferenceNumber(String referenceNumber);
    long countByCitizenIdAndStatus(Long citizenId, DossierStatus status);

    @Query("""
            SELECT DISTINCT d FROM Dossier d
            LEFT JOIN FETCH d.citizen c
            LEFT JOIN FETCH c.user
            LEFT JOIN FETCH d.vehicle
            LEFT JOIN FETCH d.documents doc
            LEFT JOIN FETCH doc.typeDocument
            LEFT JOIN FETCH d.payment
            LEFT JOIN FETCH d.appointment a
            LEFT JOIN FETCH a.center
            LEFT JOIN FETCH d.processingCenter
            LEFT JOIN FETCH d.vehicleDeclaration
            LEFT JOIN FETCH d.plateDelivery
            LEFT JOIN FETCH d.registration
            WHERE d.id = :id
            """)
    Optional<Dossier> findDetailedById(@Param("id") Long id);

    long countByStatus(DossierStatus status);
    long countByCitizenId(Long citizenId);

    @Query("""
            SELECT d FROM Dossier d
            JOIN d.citizen c
            JOIN c.user u
            LEFT JOIN d.vehicle v
            LEFT JOIN FETCH d.plateDelivery
            WHERE (:status IS NULL OR d.status = :status)
            AND (:referencePattern IS NULL OR LOWER(d.referenceNumber) LIKE :referencePattern)
            AND (:citizenPattern IS NULL OR LOWER(CONCAT(c.firstName, ' ', c.lastName)) LIKE :citizenPattern
                 OR LOWER(u.email) LIKE :citizenPattern)
            AND (:chassisPattern IS NULL OR LOWER(v.chassisNumber) LIKE :chassisPattern)
            ORDER BY d.createdAt DESC
            """)
    List<Dossier> searchForAdmin(
            @Param("status") DossierStatus status,
            @Param("referencePattern") String referencePattern,
            @Param("citizenPattern") String citizenPattern,
            @Param("chassisPattern") String chassisPattern);

    @Query("SELECT COUNT(d) FROM Dossier d")
    long countAllDossiers();

    @Query("SELECT COUNT(d) FROM Dossier d WHERE d.status IN ('VALIDATED', 'PAID', 'APPOINTMENT_SCHEDULED', 'IMMATRICULATION_IN_PROGRESS', 'COMPLETED')")
    long countValidatedDossiers();

    @Query("SELECT COUNT(d) FROM Dossier d WHERE d.status = 'COMPLETED'")
    long countCompletedDossiers();

    @Query(value = """
            SELECT TO_CHAR(DATE(created_at), 'DD/MM') AS label,
                   COUNT(*) AS total
            FROM dossiers
            WHERE created_at >= :since
            GROUP BY DATE(created_at)
            ORDER BY DATE(created_at)
            """, nativeQuery = true)
    List<Object[]> countDossiersByDaySince(@Param("since") java.sql.Timestamp since);

    @Query(value = """
            SELECT TO_CHAR(DATE_TRUNC('month', created_at), 'MM/YYYY') AS label,
                   COUNT(*) AS total
            FROM dossiers
            WHERE created_at >= :since
            GROUP BY DATE_TRUNC('month', created_at)
            ORDER BY DATE_TRUNC('month', created_at)
            """, nativeQuery = true)
    List<Object[]> countDossiersByMonthSince(@Param("since") java.sql.Timestamp since);
}
