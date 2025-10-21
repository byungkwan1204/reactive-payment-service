package com.example.paymentservice.payment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingPaymentOrder {

    private Long paymentOrderId;
    private PaymentStatus status;
    private Long amount;
    private Byte failedCount;
    private Byte threshold;

}
