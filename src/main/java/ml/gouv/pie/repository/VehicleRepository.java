package ml.gouv.pie.repository;

import ml.gouv.pie.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    boolean existsByChassisNumber(String chassisNumber);
    boolean existsByBrandEntityId(Long brandId);
    boolean existsByVehicleTypeEntityId(Long vehicleTypeId);
}
