package com.example.paymentservice.payment.test;

import com.example.paymentservice.payment.domain.PaymentEvent;
import reactor.core.publisher.Mono;

public interface PaymentDatabaseHelper {

    /**
     * orderId를 주었을 떄, PaymentEvent를 가져오도록 한다.
     */
    PaymentEvent getPaymentEvent(String orderId);

    Mono<Void> clean();
}
