package ml.gouv.pie.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

@Data
public class CenterRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String city;

    private String address;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    @Min(1)
    private int dailyCapacity = 50;

    private boolean active = true;

    @NotEmpty
    private List<String> openingDays;

    @NotNull
    private LocalTime openingTime;

    @NotNull
    private LocalTime closingTime;

    @Min(0)
    private int processingDelayDays = 3;
}
