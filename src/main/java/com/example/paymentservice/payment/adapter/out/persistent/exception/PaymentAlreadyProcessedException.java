package com.example.paymentservice.payment.adapter.out.persistent.exception;

import com.example.paymentservice.payment.domain.PaymentStatus;
import lombok.Getter;

@Getter
public class PaymentAlreadyProcessedException extends RuntimeException {

    private final PaymentStatus status;
    private String message;

    public PaymentAlreadyProcessedException(PaymentStatus status, String message) {
        super(message);
        this.status = status;
    }
}
