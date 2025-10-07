package com.example.paymentservice.payment.adapter.out.persistent.exception;

import lombok.Getter;

@Getter
public class PaymentValidationException extends RuntimeException {

    public PaymentValidationException(String message) {
        super(message);
    }
}
