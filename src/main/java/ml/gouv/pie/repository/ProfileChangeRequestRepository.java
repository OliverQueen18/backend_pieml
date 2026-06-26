package ml.gouv.pie.repository;

import ml.gouv.pie.entity.ProfileChangeRequest;
import ml.gouv.pie.entity.enums.ProfileChangeField;
import ml.gouv.pie.entity.enums.ProfileChangeRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfileChangeRequestRepository extends JpaRepository<ProfileChangeRequest, Long> {
    List<ProfileChangeRequest> findByCitizen_IdOrderByCreatedAtDesc(Long citizenId);

    List<ProfileChangeRequest> findAllByOrderByCreatedAtDesc();

    List<ProfileChangeRequest> findByStatusOrderByCreatedAtDesc(ProfileChangeRequestStatus status);

    long countByStatus(ProfileChangeRequestStatus status);

    boolean existsByCitizen_IdAndFieldAndStatus(
            Long citizenId,
            ProfileChangeField field,
            ProfileChangeRequestStatus status);
}
