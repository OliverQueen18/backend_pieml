package ml.gouv.pie.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "L'email ou le téléphone est requis")
    private String identifier;
}
