package com.example.paymentservice.payment.application.port.in;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentConfirmCommand {

    private String paymentKey;
    private String orderId;
    private Long amount;
}
