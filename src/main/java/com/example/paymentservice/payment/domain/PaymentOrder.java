package com.example.paymentservice.payment.domain;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data @Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrder {

    private Long id = null;
    private Long paymentEventId = null;
    private Long sellerId;
    private Long productId;
    private String orderId;
    private BigDecimal amount;
    private PaymentStatus paymentStatus;

    @Setter(AccessLevel.NONE)
    public Boolean isLedgerUpdated = false;
    @Setter(AccessLevel.NONE)
    private Boolean isWalletUpdated = false;

    public boolean isLedgerUpdated() {
        return isLedgerUpdated;
    }

    public boolean isWalletUpdated() {
        return isWalletUpdated;
    }
}
