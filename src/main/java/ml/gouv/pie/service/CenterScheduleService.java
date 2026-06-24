package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.CenterAvailabilityDto;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.entity.Center;
import ml.gouv.pie.exception.BusinessException;
import ml.gouv.pie.repository.AppointmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CenterScheduleService {

    private static final Set<DayOfWeek> DEFAULT_OPENING_DAYS = EnumSet.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    );

    private final AppointmentRepository appointmentRepository;

    public Set<DayOfWeek> resolveOpeningDays(Center center) {
        Set<DayOfWeek> days = center.getOpeningDays();
        if (days == null || days.isEmpty()) {
            return EnumSet.copyOf(DEFAULT_OPENING_DAYS);
        }
        return days;
    }

    public LocalTime resolveOpeningTime(Center center) {
        return center.getOpeningTime() != null ? center.getOpeningTime() : LocalTime.of(8, 0);
    }

    public LocalTime resolveClosingTime(Center center) {
        return center.getClosingTime() != null ? center.getClosingTime() : LocalTime.of(17, 0);
    }

    public int resolveProcessingDelayDays(Center center) {
        return Math.max(0, center.getProcessingDelayDays());
    }

    @Transactional(readOnly = true)
    public LocalDate computeEarliestDate(Center center) {
        long upcoming = appointmentRepository.countByCenterIdAndAppointmentDateGreaterThanEqual(
                center.getId(), LocalDate.now());
        int capacityDelay = (int) (upcoming / Math.max(1, center.getDailyCapacity()));
        int openDaysToWait = resolveProcessingDelayDays(center) + capacityDelay;
        return addOpenDays(LocalDate.now(), openDaysToWait, resolveOpeningDays(center));
    }

    @Transactional(readOnly = true)
    public CenterAvailabilityDto getAvailability(Center center, int horizonDays) {
        Set<DayOfWeek> openingDays = resolveOpeningDays(center);
        LocalTime openingTime = resolveOpeningTime(center);
        LocalTime closingTime = resolveClosingTime(center);
        LocalDate earliest = computeEarliestDate(center);
        LocalDate end = LocalDate.now().plusDays(horizonDays);

        List<CenterAvailabilityDto.AvailableDayDto> availableDays = new ArrayList<>();
        for (LocalDate date = earliest; !date.isAfter(end); date = date.plusDays(1)) {
            if (!openingDays.contains(date.getDayOfWeek())) {
                continue;
            }
            long booked = appointmentRepository.countByCenterIdAndAppointmentDate(center.getId(), date);
            int remaining = Math.max(0, center.getDailyCapacity() - (int) booked);
            if (remaining > 0) {
                availableDays.add(CenterAvailabilityDto.AvailableDayDto.builder()
                        .date(date)
                        .booked((int) booked)
                        .remaining(remaining)
                        .build());
            }
        }

        return CenterAvailabilityDto.builder()
                .earliestDate(earliest)
                .processingDelayDays(resolveProcessingDelayDays(center))
                .dailyCapacity(center.getDailyCapacity())
                .openingTime(openingTime)
                .closingTime(closingTime)
                .openingDays(openingDays.stream().map(DayOfWeek::name).toList())
                .availableDays(availableDays)
                .build();
    }

    public void validateAppointment(Center center, LocalDate date, LocalTime time) {
        Set<DayOfWeek> openingDays = resolveOpeningDays(center);
        LocalTime openingTime = resolveOpeningTime(center);
        LocalTime closingTime = resolveClosingTime(center);
        LocalDate earliest = computeEarliestDate(center);

        if (date.isBefore(earliest)) {
            throw new BusinessException(
                    "Date trop proche. Première date disponible : " + earliest,
                    HttpStatus.BAD_REQUEST);
        }

        if (!openingDays.contains(date.getDayOfWeek())) {
            throw new BusinessException("Le centre est fermé ce jour-là", HttpStatus.BAD_REQUEST);
        }

        if (time.isBefore(openingTime) || time.isAfter(closingTime)) {
            throw new BusinessException(
                    "Horaire hors des heures d'ouverture (" + openingTime + " – " + closingTime + ")",
                    HttpStatus.BAD_REQUEST);
        }

        long booked = appointmentRepository.countByCenterIdAndAppointmentDate(center.getId(), date);
        if (booked >= center.getDailyCapacity()) {
            throw new BusinessException("Capacité du centre atteinte pour cette date", HttpStatus.BAD_REQUEST);
        }
    }

    public DtoMapper.CenterDto enrichCenterDto(Center center, DtoMapper.CenterDto dto) {
        Set<DayOfWeek> days = resolveOpeningDays(center);
        dto.setOpeningDays(days.stream().map(DayOfWeek::name).collect(Collectors.toList()));
        dto.setOpeningTime(resolveOpeningTime(center));
        dto.setClosingTime(resolveClosingTime(center));
        dto.setProcessingDelayDays(resolveProcessingDelayDays(center));
        return dto;
    }

    public Set<DayOfWeek> parseOpeningDays(List<String> days) {
        if (days == null || days.isEmpty()) {
            throw new BusinessException("Au moins un jour d'ouverture est requis", HttpStatus.BAD_REQUEST);
        }
        try {
            return days.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(DayOfWeek::valueOf)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Jour d'ouverture invalide", HttpStatus.BAD_REQUEST);
        }
    }

    public static LocalDate addOpenDays(LocalDate start, int openDaysToAdd, Set<DayOfWeek> openingDays) {
        if (openDaysToAdd <= 0) {
            LocalDate candidate = start;
            while (!openingDays.contains(candidate.getDayOfWeek())) {
                candidate = candidate.plusDays(1);
            }
            return candidate;
        }

        LocalDate date = start;
        int added = 0;
        while (added < openDaysToAdd) {
            date = date.plusDays(1);
            if (openingDays.contains(date.getDayOfWeek())) {
                added++;
            }
        }
        while (!openingDays.contains(date.getDayOfWeek())) {
            date = date.plusDays(1);
        }
        return date;
    }
}
