package ml.gouv.pie.repository;

import ml.gouv.pie.entity.VehicleDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleDeclarationRepository extends JpaRepository<VehicleDeclaration, Long> {
    Optional<VehicleDeclaration> findByDossierId(Long dossierId);
    boolean existsByDossierId(Long dossierId);
}
