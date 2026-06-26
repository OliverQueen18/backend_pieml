package ml.gouv.pie.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompleteImmatriculationRequest {
    @NotBlank(message = "Le numéro d'immatriculation est obligatoire")
    private String registrationNumber;
}
