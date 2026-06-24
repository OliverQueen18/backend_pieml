package ml.gouv.pie.repository;

import ml.gouv.pie.entity.Center;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CenterRepository extends JpaRepository<Center, Long> {
    List<Center> findByActiveTrueOrderByCityAscNameAsc();
    List<Center> findAllByOrderByCityAscNameAsc();
}
