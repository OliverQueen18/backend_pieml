package ml.gouv.pie.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ml.gouv.pie.entity.enums.NotificationType;

@Data
public class AdminNotificationRequest {
    @Email
    private String userEmail;

    @Size(max = 500)
    private String message;

    private NotificationType type;

    private Boolean sendEmail;
    private Boolean read;
}
