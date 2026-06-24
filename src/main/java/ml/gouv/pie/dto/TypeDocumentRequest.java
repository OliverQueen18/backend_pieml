package ml.gouv.pie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TypeDocumentRequest {

    @NotBlank(message = "Le code est obligatoire")
    @Size(max = 50)
    private String code;

    @NotBlank(message = "Le libellé est obligatoire")
    @Size(max = 150)
    private String libelle;

    @Size(max = 500)
    private String description;

    private boolean obligatoire = true;

    private boolean actif = true;

    private int ordre = 0;
}
