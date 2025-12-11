package com.example.paymentservice.payment.adapter.out.persistent.repository;

import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand;
import com.example.paymentservice.payment.domain.PaymentEventMessage;
import com.example.paymentservice.payment.domain.PaymentEventMessage.PaymentEventMessageType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaymentOutboxRepository {

    Mono<PaymentEventMessage> insertOutbox(PaymentStatusUpdateCommand command);

    // 마킹할 메시지를 식별하기 위해 멱등성 키와 이벤트 메세지 타입이 필요하다.
    Mono<Boolean> markMessageAsSent(String idempotencyKey, PaymentEventMessageType type);

    Mono<Boolean> markMessageAsFailure(String idempotencyKey, PaymentEventMessageType type);

    Flux<PaymentEventMessage> getPendingPaymentOutboxes();
}