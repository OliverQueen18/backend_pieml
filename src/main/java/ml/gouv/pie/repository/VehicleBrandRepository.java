package ml.gouv.pie.repository;

import ml.gouv.pie.entity.VehicleBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VehicleBrandRepository extends JpaRepository<VehicleBrand, Long> {
    @Query("SELECT b FROM VehicleBrand b WHERE b.actif = true ORDER BY b.ordre ASC, b.libelle ASC")
    List<VehicleBrand> findActiveOrdered();

    @Query("SELECT b FROM VehicleBrand b ORDER BY b.ordre ASC, b.libelle ASC")
    List<VehicleBrand> findAllOrdered();

    Optional<VehicleBrand> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
}
