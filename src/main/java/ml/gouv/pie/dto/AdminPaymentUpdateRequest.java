package ml.gouv.pie.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ml.gouv.pie.entity.enums.PaymentMethod;
import ml.gouv.pie.entity.enums.PaymentStatus;

@Data
public class AdminPaymentUpdateRequest {
    @NotNull
    private PaymentStatus status;

    private PaymentMethod paymentMethod;
    private String transactionId;
}
