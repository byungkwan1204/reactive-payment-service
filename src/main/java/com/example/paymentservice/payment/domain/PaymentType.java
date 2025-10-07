package com.example.paymentservice.payment.domain;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentType {

    NORMAL ("일반 결제");

    private final String description;

    public static PaymentType get(String type) {
        return Arrays.stream(values())
            .filter(value -> value.name().equals(type))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException(
                    String.format("PaymentType (type: %s) 은 올바르지 않은 결제 타입입니다.", type)));
    }
}
