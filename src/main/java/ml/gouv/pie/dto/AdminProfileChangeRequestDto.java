package ml.gouv.pie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfileChangeRequestDto {
    private Long id;
    private Long citizenId;
    private String citizenFirstName;
    private String citizenLastName;
    private String citizenEmail;
    private String citizenPhone;
    private String citizenNina;
    private String field;
    private String currentValue;
    private String requestedValue;
    private Double requestedLatitude;
    private Double requestedLongitude;
    private String reason;
    private String fileName;
    private String status;
    private LocalDateTime createdAt;
}
