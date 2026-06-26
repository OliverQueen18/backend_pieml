package ml.gouv.pie.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "plate_deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlateDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", nullable = false, unique = true)
    private Dossier dossier;

    @Column(nullable = false, length = 30)
    private String plateNumber;

    @Column(nullable = false)
    private LocalDate deliveryDate;

    @Column(nullable = false, length = 100)
    private String collectorFirstName;

    @Column(nullable = false, length = 100)
    private String collectorLastName;

    @Column(nullable = false, length = 30)
    private String collectorPhone;

    @Column(nullable = false, length = 500)
    private String collectorAddress;

    private String fileName;

    private String filePath;

    private Long fileSize;

    private String contentType;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
