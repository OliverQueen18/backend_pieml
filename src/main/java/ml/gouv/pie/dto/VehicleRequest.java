package ml.gouv.pie.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VehicleRequest {

    @NotNull(message = "La marque est obligatoire")
    private Long brandId;

    @NotNull(message = "Le type d'engin est obligatoire")
    private Long vehicleTypeId;

    @Size(max = 150)
    private String brandOther;

    @NotBlank
    private String model;

    private String engineCapacity;

    private String engineNumber;

    @NotBlank
    private String chassisNumber;

    @NotBlank
    private String color;

    @NotNull
    @Min(1980)
    @Max(value = 2100, message = "Année de fabrication invalide")
    private Integer year;

    private String countryOfOrigin;
}
