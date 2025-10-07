package com.example.paymentservice.payment.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentExecutionResult {

    private String paymentKey;
    private String orderId;
    private PaymentExtraDetails extraDetails = null;
    private PaymentExecutionFailure failure;
    private Boolean isSuccess;
    private Boolean isFailure = null;
    private Boolean isUnknown;
    private Boolean isRetryable;

    @Builder
    public PaymentExecutionResult(String paymentKey, String orderId, PaymentExtraDetails extraDetails, PaymentExecutionFailure failure, Boolean isSuccess, Boolean isFailure, Boolean isUnknown, Boolean isRetryable) {
        this.paymentKey = paymentKey;
        this.orderId = orderId;
        this.extraDetails = extraDetails;
        this.failure = failure;
        this.isSuccess = isSuccess;
        this.isFailure = isFailure;
        this.isUnknown = isUnknown;
        this.isRetryable = isRetryable;

        if (!(Boolean.TRUE.equals(isSuccess)
              || Boolean.TRUE.equals(isFailure)
              || Boolean.TRUE.equals(isUnknown))) {
            throw new IllegalArgumentException(
                String.format("결제 (orderId: %s) 는 올바르지 않은 결제 상태입니다.", orderId));
        }
    }

    public PaymentStatus paymentStatus() {

        if (Boolean.TRUE.equals(isSuccess)) {
            return PaymentStatus.SUCCESS;
        } else if (Boolean.TRUE.equals(isFailure)) {
            return PaymentStatus.FAILURE;
        } else if (Boolean.TRUE.equals(isUnknown)) {
            return PaymentStatus.UNKNOWN;
        } else {
            throw new IllegalStateException(
                String.format("결제 (orderId: %s) 는 올바르지 않은 결제 상태 입니다.", orderId));
        }
    }

    // 결제 상세 정보 클래스
    @Getter @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentExtraDetails {
        private PaymentType type;
        private PaymentMethod method;
        private LocalDateTime approveAt;
        private String orderName;
        private PGConfirmationStatus pgConfirmationStatus;
        private Long totalAmount;
        private String pgRawData;
    }

    @Getter @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentExecutionFailure {

        private String errorCode;
        private String message;
    }
}

