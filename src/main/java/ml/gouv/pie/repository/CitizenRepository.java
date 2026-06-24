package ml.gouv.pie.repository;

import ml.gouv.pie.entity.Citizen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CitizenRepository extends JpaRepository<Citizen, Long> {
    List<Citizen> findAllByOrderByCreatedAtDesc();
    Optional<Citizen> findByUserId(Long userId);
    Optional<Citizen> findByUser_Email(String email);
    Optional<Citizen> findByNina(String nina);
    boolean existsByNina(String nina);
}
