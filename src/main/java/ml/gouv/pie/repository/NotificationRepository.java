package ml.gouv.pie.repository;

import ml.gouv.pie.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndReadFalse(Long userId);
    void deleteByUserId(Long userId);

    @Query("SELECT n FROM Notification n JOIN FETCH n.user ORDER BY n.createdAt DESC")
    List<Notification> findAllWithUserOrderByCreatedAtDesc();

    @Query("SELECT n FROM Notification n JOIN FETCH n.user WHERE n.id = :id")
    java.util.Optional<Notification> findByIdWithUser(Long id);
}
