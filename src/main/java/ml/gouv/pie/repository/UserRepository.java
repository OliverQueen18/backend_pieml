package ml.gouv.pie.repository;

import ml.gouv.pie.entity.User;
import ml.gouv.pie.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.centers WHERE u.role <> :role ORDER BY u.createdAt DESC")
    List<User> findStaffWithCentersByRoleNot(Role role);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.centers WHERE u.email = :email")
    Optional<User> findByEmailWithCenters(String email);

    List<User> findByRoleNotOrderByCreatedAtDesc(Role role);
    long countByRole(Role role);

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN u.centers c
            WHERE c.id = :centerId
              AND u.role <> ml.gouv.pie.entity.enums.Role.CITOYEN
              AND u.enabled = true
            """)
    List<User> findActiveStaffByCenterId(@Param("centerId") Long centerId);
}
