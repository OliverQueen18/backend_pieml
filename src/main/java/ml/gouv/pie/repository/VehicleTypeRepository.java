package ml.gouv.pie.repository;

import ml.gouv.pie.entity.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VehicleTypeRepository extends JpaRepository<VehicleType, Long> {
    @Query("SELECT t FROM VehicleType t WHERE t.actif = true ORDER BY t.ordre ASC, t.libelle ASC")
    List<VehicleType> findActiveOrdered();

    @Query("SELECT t FROM VehicleType t ORDER BY t.ordre ASC, t.libelle ASC")
    List<VehicleType> findAllOrdered();

    Optional<VehicleType> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
}
