package ml.gouv.pie.repository;

import ml.gouv.pie.entity.RoleDefinition;
import ml.gouv.pie.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleDefinitionRepository extends JpaRepository<RoleDefinition, Long> {
    Optional<RoleDefinition> findByCode(Role code);
    boolean existsByCode(Role code);
    List<RoleDefinition> findAllByOrderByCodeAsc();
}
