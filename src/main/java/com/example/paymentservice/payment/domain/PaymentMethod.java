package com.example.paymentservice.payment.domain;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentMethod {

    EASY_PAY ("간편결제");

    private final String method;

    public static PaymentMethod get(String method) {
        return Arrays.stream(values())
            .filter(value -> value.method.equals(method))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException(
                    String.format("PaymentMethod (method: %s) 은 올바르지 않은 결제 방법입니다.", method)));
    }
}
