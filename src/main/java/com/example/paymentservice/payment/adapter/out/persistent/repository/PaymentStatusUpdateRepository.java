package com.example.paymentservice.payment.adapter.out.persistent.repository;

import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand;
import reactor.core.publisher.Mono;

public interface PaymentStatusUpdateRepository {

    Mono<Boolean> updatePaymentStatusToExecuting(String paymentKey, String orderId);

    Mono<Boolean> updatePaymentStatus(PaymentStatusUpdateCommand command);

}
