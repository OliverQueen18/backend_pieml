package ml.gouv.pie.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TariffRequest {

    @NotBlank(message = "Le code est obligatoire")
    @Size(max = 50)
    private String code;

    @NotBlank(message = "Le libellé est obligatoire")
    @Size(max = 150)
    private String libelle;

    @Size(max = 500)
    private String description;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0", message = "Le montant doit être positif ou nul")
    private BigDecimal amount;

    @DecimalMin(value = "0", message = "Les frais de service doivent être positifs ou nuls")
    private BigDecimal serviceFee = BigDecimal.ZERO;

    private boolean actif = true;

    private int ordre = 0;
}
