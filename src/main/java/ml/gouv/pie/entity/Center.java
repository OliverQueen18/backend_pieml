package ml.gouv.pie.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "centers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Center {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    private String address;

    @Column(length = 30)
    private String phone;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    private boolean active = true;

    private int dailyCapacity = 50;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "center_opening_days", joinColumns = @JoinColumn(name = "center_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    @Builder.Default
    private Set<DayOfWeek> openingDays = EnumSet.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    );

    @Builder.Default
    private LocalTime openingTime = LocalTime.of(8, 0);

    @Builder.Default
    private LocalTime closingTime = LocalTime.of(17, 0);

    @Builder.Default
    private int processingDelayDays = 3;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
