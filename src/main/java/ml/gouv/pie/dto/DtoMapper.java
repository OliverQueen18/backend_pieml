package ml.gouv.pie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ml.gouv.pie.entity.enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class DtoMapper {

    private DtoMapper() {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleLookupDto {
        private Long id;
        private String code;
        private String libelle;
        private String description;
        private boolean actif;
        private int ordre;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleDto {
        private Long brandId;
        private Long vehicleTypeId;
        private String brand;
        private String vehicleType;
        private String model;
        private String engineCapacity;
        private String engineNumber;
        private String chassisNumber;
        private String color;
        private Integer year;
        private String countryOfOrigin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeDocumentDto {
        private Long id;
        private String code;
        private String libelle;
        private String description;
        private boolean obligatoire;
        private boolean actif;
        private int ordre;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentDto {
        private Long id;
        private TypeDocumentDto typeDocument;
        private DocumentStatus status;
        private String fileName;
        private Long fileSize;
        private String contentType;
        private LocalDateTime uploadedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentDto {
        private Long id;
        private BigDecimal amount;
        private BigDecimal serviceFee;
        private BigDecimal totalAmount;
        private String transactionId;
        private PaymentStatus status;
        private PaymentMethod paymentMethod;
        private LocalDateTime paymentDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppointmentDto {
        private Long id;
        private Long centerId;
        private String centerName;
        private String centerCity;
        private String centerAddress;
        private Double centerLatitude;
        private Double centerLongitude;
        private LocalDate appointmentDate;
        private LocalTime appointmentTime;
        private AppointmentStatus status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingCenterDto {
        private Long id;
        private String name;
        private String city;
        private String address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleDeclarationDto {
        private Long id;
        private VehicleDeclarationType declarationType;
        private String fileName;
        private Long fileSize;
        private String contentType;
        private LocalDateTime declaredAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DossierDto {
        private Long id;
        private String referenceNumber;
        private DossierStatus status;
        private String rejectionReason;
        private VehicleDto vehicle;
        private List<DocumentDto> documents;
        private PaymentDto payment;
        private AppointmentDto appointment;
        private ProcessingCenterDto processingCenter;
        private VehicleDeclarationDto vehicleDeclaration;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardStatsDto {
        private long dossiersEnCours;
        private long dossiersValides;
        private long rendezVous;
        private long immatriculations;
        private long totalDossiers;
        private long totalValides;
        private long totalImmatriculations;
        private int satisfactionRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationDto {
        private Long id;
        private String message;
        private NotificationType type;
        private boolean read;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CenterDto {
        private Long id;
        private String name;
        private String city;
        private String address;
        private Double latitude;
        private Double longitude;
        private int dailyCapacity;
        private boolean active;
        private List<String> openingDays;
        private LocalTime openingTime;
        private LocalTime closingTime;
        private int processingDelayDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicStatsDto {
        private long dossiersDeposes;
        private long dossiersValides;
        private long immatriculations;
        private int satisfactionRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminDashboardStatsDto {
        private long totalDossiers;
        private long dossiersEnCours;
        private long dossiersValides;
        private long dossiersRejetes;
        private long immatriculations;
        private long totalCitoyens;
        private long totalUtilisateurs;
        private long rendezVousPlanifies;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardChartPointDto {
        private String label;
        private long value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardCenterStatDto {
        private Long centerId;
        private String centerName;
        private String city;
        private long appointments;
        private long dossiers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminDashboardChartsDto {
        private String period;
        private List<DashboardChartPointDto> dossiersByPeriod;
        private List<DashboardCenterStatDto> statsByCenter;
        private List<AdminNotificationDto> recentNotifications;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminDossierSummaryDto {
        private Long id;
        private String referenceNumber;
        private DossierStatus status;
        private String citizenName;
        private String citizenEmail;
        private String vehicleLabel;
        private String chassisNumber;
        private int uploadedDocuments;
        private int requiredDocuments;
        private String centerName;
        private java.time.LocalDate appointmentDate;
        private java.time.LocalTime appointmentTime;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminUserDto {
        private Long id;
        private String email;
        private String phone;
        private Role role;
        private boolean enabled;
        private String firstName;
        private String lastName;
        private LocalDateTime createdAt;
        private boolean mustChangePassword;
        private List<Long> centerIds;
        private List<String> centerNames;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminCitizenDto {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String nina;
        private long dossierCount;
        private boolean enabled;
        private String address;
        private Double latitude;
        private Double longitude;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminRoleDto {
        private Long id;
        private Role code;
        private String label;
        private String description;
        private boolean active;
        private boolean systemRole;
        private long userCount;
        private LocalDateTime createdAt;
        private List<String> permissions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionDto {
        private String code;
        private String label;
        private String category;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminNotificationDto {
        private Long id;
        private Long userId;
        private String userEmail;
        private String userName;
        private String message;
        private NotificationType type;
        private boolean read;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminPaymentDto {
        private Long id;
        private Long dossierId;
        private String dossierReference;
        private String citizenName;
        private String citizenEmail;
        private BigDecimal amount;
        private BigDecimal serviceFee;
        private BigDecimal totalAmount;
        private String transactionId;
        private PaymentStatus status;
        private PaymentMethod paymentMethod;
        private LocalDateTime paymentDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TariffDto {
        private Long id;
        private String code;
        private String libelle;
        private String description;
        private BigDecimal amount;
        private BigDecimal serviceFee;
        private boolean actif;
        private int ordre;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
