package com.example.paymentservice.payment.adapter.out.web.toss.exception;

import com.example.paymentservice.payment.domain.PaymentStatus;
import lombok.Getter;

@Getter
public class PSPConfirmationException extends RuntimeException {

    private final String errorCode;
    private final String errorMessage;
    private final Boolean isSuccess;
    private final Boolean isFailure;
    private final Boolean isUnknown;
    private final Boolean isRetryableError;
    private final Throwable cause;

    public PSPConfirmationException(String errorCode, String errorMessage, Boolean isSuccess, Boolean isFailure, Boolean isUnknown, Boolean isRetryableError, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.isSuccess = isSuccess;
        this.isFailure = isFailure;
        this.isUnknown = isUnknown;
        this.isRetryableError = isRetryableError;
        this.cause = cause;

        if (!(isSuccess || isFailure || isUnknown)) {
            throw new IllegalArgumentException(
                String.format("%s 는 올바르지 않은 결제 상태를 가지고 있습니다.", this.getClass().getSimpleName()));
        }
    }

    public PSPConfirmationException(String errorCode, String errorMessage, Boolean isSuccess, Boolean isFailure, Boolean isUnknown, Boolean isRetryableError) {
        this(errorCode, errorMessage, isSuccess, isFailure, isUnknown, isRetryableError, null);
    }

    public PaymentStatus paymentStatus() {

        if (isSuccess) {
            return PaymentStatus.SUCCESS;
        } else if (isFailure) {
            return PaymentStatus.FAILURE;
        } else if (isUnknown) {
            return PaymentStatus.UNKNOWN;
        } else {
            throw new IllegalStateException(
                String.format("%s 는 올바르지 않은 결제 상태를 가지고 있습니다.", this.getClass().getSimpleName()));
        }
    }
}
