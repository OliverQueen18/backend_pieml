package ml.gouv.pie.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContactRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 120, message = "Le nom ne doit pas dépasser 120 caractères")
    private String name;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    private String email;

    @NotBlank(message = "Le sujet est obligatoire")
    @Size(max = 200, message = "Le sujet ne doit pas dépasser 200 caractères")
    private String subject;

    @NotBlank(message = "Le message est obligatoire")
    @Size(max = 2000, message = "Le message ne doit pas dépasser 2000 caractères")
    private String message;
}
