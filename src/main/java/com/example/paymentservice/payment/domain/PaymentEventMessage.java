package com.example.paymentservice.payment.domain;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEventMessage {

    private PaymentEventMessageType type;
    private Map<String, Object> payload;
    private Map<String, Object> metadata;

    @Getter
    @AllArgsConstructor
    public enum PaymentEventMessageType {

        PAYMENT_CONFIRMATION_SUCCESS ("결제 승인 완료 이벤트");

        private final String description;
    }
}
