package com.example.paymentservice.payment.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data @Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    private Long id = null;
    private Long buyerId;
    private String orderName;
    private String orderId;
    private String paymentKey = null;
    private PaymentType paymentType = null;
    private PaymentMethod paymentMethod = null;
    private LocalDateTime approvedAt = null;
    private List<PaymentOrder> paymentOrders = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    public Boolean isPaymentDone = false;

    public Long totalAmount() {
        return paymentOrders.stream()
            .map(PaymentOrder::getAmount) // BigDecimal 추출
            .reduce(BigDecimal.ZERO, BigDecimal::add)  // 모두 더함
            .longValue();   // Long으로 변환
    }

    public boolean isPaymentDone() {
        return isPaymentDone;
    }

    public boolean isSuccess() {
        return this.paymentOrders.stream().allMatch(order -> PaymentStatus.SUCCESS == order.getPaymentStatus());
    }

    public boolean isFailure() {
        return this.paymentOrders.stream().allMatch(order -> PaymentStatus.FAILURE == order.getPaymentStatus());
    }

    public boolean isUnknown() {
        return this.paymentOrders.stream().allMatch(order -> PaymentStatus.UNKNOWN == order.getPaymentStatus());
    }
}
