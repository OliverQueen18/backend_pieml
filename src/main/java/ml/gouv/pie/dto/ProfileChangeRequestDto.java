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
public class ProfileChangeRequestDto {
    private Long id;
    private String field;
    private String requestedValue;
    private Double requestedLatitude;
    private Double requestedLongitude;
    private String reason;
    private String fileName;
    private String status;
    private LocalDateTime createdAt;
}
