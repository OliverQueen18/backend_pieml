package ml.gouv.pie.entity;

import jakarta.persistence.*;
import lombok.*;
import ml.gouv.pie.entity.enums.DossierStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dossiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private Citizen citizen;

    @OneToOne(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DossierStatus status = DossierStatus.DRAFT;

    private String rejectionReason;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Document> documents = new ArrayList<>();

    @OneToOne(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    @OneToOne(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processing_center_id")
    private Center processingCenter;

    @OneToOne(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    private Registration registration;

    @OneToOne(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    private VehicleDeclaration vehicleDeclaration;

    @OneToOne(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    private PlateDelivery plateDelivery;

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
