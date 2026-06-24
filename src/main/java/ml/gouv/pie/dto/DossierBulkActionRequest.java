package ml.gouv.pie.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class DossierBulkActionRequest {
    @NotEmpty
    private List<Long> dossierIds;
    private String reason;
}
