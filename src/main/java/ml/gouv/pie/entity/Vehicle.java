package ml.gouv.pie.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", nullable = false, unique = true)
    private Dossier dossier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private VehicleBrand brandEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id")
    private VehicleType vehicleTypeEntity;

    @Column(nullable = false)
    private String brand;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(nullable = false)
    private String model;

    private String engineCapacity;

    private String engineNumber;

    @Column(nullable = false, unique = true)
    private String chassisNumber;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private Integer year;

    private String countryOfOrigin;

    private String registrationNumber;
}
