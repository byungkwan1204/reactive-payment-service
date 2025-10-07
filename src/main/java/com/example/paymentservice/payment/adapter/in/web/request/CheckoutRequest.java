package com.example.paymentservice.payment.adapter.in.web.request;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    private Long cartId = 1L;

    private List<Long> productIds = List.of(1L, 2L, 3L);

    private Long buyerId = 1L;

    // 요청을 구별하는 역할
    // 현재 구입하는 물건과 미래의 물건이 동일한 경우 즉, 동일한 물건에 대한 요청을 구별하기 위함
    private String seed = LocalDateTime.now().toString();
}
