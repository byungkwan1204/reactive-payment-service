package com.example.paymentservice.payment.adapter.in.web.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TossPaymentConfirmRequest {

    private String paymentKey;
    private String orderId;
    private Long amount;
}