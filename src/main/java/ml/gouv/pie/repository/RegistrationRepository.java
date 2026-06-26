package ml.gouv.pie.repository;

import ml.gouv.pie.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    Optional<Registration> findByDossierId(Long dossierId);

    boolean existsByRegistrationNumber(String registrationNumber);
}
