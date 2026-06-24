package ml.gouv.pie.repository;

import ml.gouv.pie.entity.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TariffRepository extends JpaRepository<Tariff, Long> {
    List<Tariff> findAllByOrderByOrdreAscLibelleAsc();

    List<Tariff> findByActifTrueOrderByOrdreAscLibelleAsc();

    Optional<Tariff> findByCodeAndActifTrue(String code);

    Optional<Tariff> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
