package ml.gouv.pie.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    /** Email ou numéro de téléphone (+223…) */
    @NotBlank(message = "L'email ou le téléphone est requis")
    @JsonAlias("email")
    private String identifier;

    @NotBlank
    private String password;
}
