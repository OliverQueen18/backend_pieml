package ml.gouv.pie.entity;

import jakarta.persistence.*;
import lombok.*;
import ml.gouv.pie.entity.enums.ProfileChangeField;
import ml.gouv.pie.entity.enums.ProfileChangeRequestStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "profile_change_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "citizen_id", nullable = false)
    private Citizen citizen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProfileChangeField field;

    @Column(nullable = false, length = 500)
    private String requestedValue;

    private Double requestedLatitude;

    private Double requestedLongitude;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, length = 500)
    private String filePath;

    private String contentType;

    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private ProfileChangeRequestStatus status = ProfileChangeRequestStatus.PENDING;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
