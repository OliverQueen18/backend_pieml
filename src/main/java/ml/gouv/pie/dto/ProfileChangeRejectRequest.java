package ml.gouv.pie.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileChangeRejectRequest {
    @Size(max = 1000)
    private String reason;
}
