package ml.gouv.pie.repository;

import ml.gouv.pie.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByDossierId(Long dossierId);
    List<Payment> findByDossierCitizenIdOrderByPaymentDateDesc(Long citizenId);

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.dossier d
            JOIN FETCH d.citizen c
            JOIN FETCH c.user
            LEFT JOIN FETCH d.processingCenter
            LEFT JOIN FETCH d.vehicle v
            LEFT JOIN FETCH v.vehicleTypeEntity
            ORDER BY p.id DESC
            """)
    List<Payment> findAllWithDossierOrderByIdDesc();

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.dossier d
            JOIN FETCH d.citizen c
            JOIN FETCH c.user
            LEFT JOIN FETCH d.processingCenter
            LEFT JOIN FETCH d.vehicle v
            LEFT JOIN FETCH v.vehicleTypeEntity
            WHERE p.id = :id
            """)
    Optional<Payment> findByIdWithDossier(Long id);
}
