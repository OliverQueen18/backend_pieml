package ml.gouv.pie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CenterAvailabilityDto {

    private LocalDate earliestDate;
    private int processingDelayDays;
    private int dailyCapacity;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private List<String> openingDays;
    private List<AvailableDayDto> availableDays;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailableDayDto {
        private LocalDate date;
        private int booked;
        private int remaining;
    }
}
