package com.example.paymentservice.payment.application.port.in;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutCommand {

    private Long cartId;
    private Long buyerId;
    private List<Long> productIds;

    // 멱등성을 보장하기위한 Key
    private String idempotencyKey;
}
