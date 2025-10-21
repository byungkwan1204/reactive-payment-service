package com.example.paymentservice.payment.application.port.out;

import com.example.paymentservice.payment.domain.PaymentExecutionResult;
import com.example.paymentservice.payment.domain.PaymentExecutionResult.PaymentFailure;
import com.example.paymentservice.payment.domain.PaymentExecutionResult.PaymentExtraDetails;
import com.example.paymentservice.payment.domain.PaymentStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentStatusUpdateCommand {

    private String paymentKey;
    private String orderId;
    private PaymentStatus status;
    private PaymentExtraDetails extraDetails = null;
    private PaymentFailure failure = null;

    @Builder
    public PaymentStatusUpdateCommand(String paymentKey, String orderId, PaymentStatus status, PaymentExtraDetails extraDetails, PaymentFailure failure) {
        this.paymentKey = paymentKey;
        this.orderId = orderId;
        this.status = status;
        this.extraDetails = extraDetails;
        this.failure = failure;

        if (!(PaymentStatus.SUCCESS == status
            || PaymentStatus.FAILURE == status
            || PaymentStatus.UNKNOWN == status)) {

            throw new IllegalArgumentException(
                String.format("결제 상태 (status: %s) 는 올바르지 않은 결제 상태입니다.", status));
        }

        if (PaymentStatus.SUCCESS == status) {
            if (extraDetails == null) {
                throw new IllegalArgumentException(
                    "PaymentStatus 값이 SUCCESS 라면 PaymentExtraDetails 는 null 이 되면 안됩니다.");
            }

        } else if (PaymentStatus.FAILURE == status) {
            if (failure == null) {
                throw new IllegalArgumentException(
                    "PaymentStatus 값이 FAILURE 라면 PaymentFailure 는 null 이 되면 안됩니다.");
            }
        }
    }

    public static PaymentStatusUpdateCommand ofExecutionResult(PaymentExecutionResult paymentExecutionResult) {
        return PaymentStatusUpdateCommand.builder()
            .paymentKey(paymentExecutionResult.getPaymentKey())
            .orderId(paymentExecutionResult.getOrderId())
            .status(paymentExecutionResult.paymentStatus())
            .extraDetails(paymentExecutionResult.getExtraDetails())
            .failure(paymentExecutionResult.getFailure())
            .build();
    }
}
