package com.example.paymentservice.payment.domain;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingPaymentEvent {

    private Long paymentEventId;
    private String paymentKey;
    private String orderId;
    private List<PendingPaymentOrder> pendingPaymentOrders;

    public Long totalAmount() {
        return pendingPaymentOrders.stream()
            .mapToLong(PendingPaymentOrder::getAmount)
            .sum();
    }
}
