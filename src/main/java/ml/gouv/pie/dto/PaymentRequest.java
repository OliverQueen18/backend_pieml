package ml.gouv.pie.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ml.gouv.pie.entity.enums.PaymentMethod;

@Data
public class PaymentRequest {

    @NotNull
    private PaymentMethod paymentMethod;

    @NotNull
    private Long centerId;
}
