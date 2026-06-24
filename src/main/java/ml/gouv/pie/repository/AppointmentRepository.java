package ml.gouv.pie.repository;

import ml.gouv.pie.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    Optional<Appointment> findByDossierId(Long dossierId);
    List<Appointment> findByDossierCitizenIdOrderByAppointmentDateDesc(Long citizenId);
    long countByCenterIdAndAppointmentDate(Long centerId, LocalDate date);
    long countByCenterIdAndAppointmentDateGreaterThanEqual(Long centerId, LocalDate date);
    boolean existsByCenterId(Long centerId);

    @Query(value = """
            SELECT c.name, c.city, COUNT(a.id)
            FROM appointments a
            INNER JOIN centers c ON c.id = a.center_id
            WHERE a.appointment_date >= :sinceDate
            GROUP BY c.id, c.name, c.city
            ORDER BY COUNT(a.id) DESC
            """, nativeQuery = true)
    List<Object[]> countAppointmentsByCenterSince(@Param("sinceDate") java.sql.Date sinceDate);
}
