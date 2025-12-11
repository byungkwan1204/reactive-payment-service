package com.example.paymentservice.payment.domain;

import com.example.paymentservice.payment.domain.PaymentExecutionResult.PaymentFailure;
import java.util.Objects;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 클라이언트 응답 객체
 */
@Data
@NoArgsConstructor
public class PaymentConfirmationResult {

    private PaymentStatus status;
    private PaymentFailure failure;
    private String message;

    @Builder
    public PaymentConfirmationResult(PaymentStatus status, PaymentFailure failure) {
        this.status = status;
        this.failure = failure;

        if (status == PaymentStatus.FAILURE) {
            Objects.requireNonNull(failure);
            if (failure == null) {
                throw new IllegalArgumentException(
                    "결제 상태 FAILURE 일 때 PaymentFailure 는 null 값이 될 수 없습니다.");
            }
        }

        switch (status) {
            case SUCCESS -> message = "결제 처리에 성공하였습니다.";
            case FAILURE -> message = "결제 처리에 실패하였습니다.";
            case UNKNOWN -> message = "결제 처리 중에 알 수 없는 에러가 발생하였습니다.";
            default ->
                throw new IllegalStateException(
                    String.format("현재 결제 상태 (status: %s) 는 올바르지 않은 상태입니다.", status));
        }
    }
}
