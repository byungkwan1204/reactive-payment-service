package com.example.paymentservice.payment.adapter.out.persistent.repository;

import com.example.paymentservice.payment.domain.PaymentEvent;
import reactor.core.publisher.Mono;

public interface PaymentRepository {

    Mono<Void> save(PaymentEvent paymentEvent);
}
