package com.example.paymentservice.payment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResult {

    private Long amount;
    private String orderId;
    private String orderName;
}
