package ml.gouv.pie.repository;

import ml.gouv.pie.entity.PlateDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlateDeliveryRepository extends JpaRepository<PlateDelivery, Long> {
    Optional<PlateDelivery> findByDossierId(Long dossierId);
}
