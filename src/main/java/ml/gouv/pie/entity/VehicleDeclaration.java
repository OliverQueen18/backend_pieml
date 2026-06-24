package ml.gouv.pie.entity;

import jakarta.persistence.*;
import lombok.*;
import ml.gouv.pie.entity.enums.VehicleDeclarationType;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_declarations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleDeclaration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", nullable = false, unique = true)
    private Dossier dossier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleDeclarationType declarationType;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    private Long fileSize;

    private String contentType;

    @Column(nullable = false)
    private LocalDateTime declaredAt;
}
