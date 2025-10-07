package com.example.paymentservice.payment.adapter.out.persistent.repository;

import reactor.core.publisher.Mono;

public interface PaymentValidationRepository {

    Mono<Boolean> isValid(String orderId, Long amount);
}
