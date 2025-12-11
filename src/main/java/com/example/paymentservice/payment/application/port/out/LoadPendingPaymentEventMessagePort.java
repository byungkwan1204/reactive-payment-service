package com.example.paymentservice.payment.application.port.out;

import com.example.paymentservice.payment.domain.PaymentEventMessage;
import reactor.core.publisher.Flux;

public interface LoadPendingPaymentEventMessagePort {

    Flux<PaymentEventMessage> getPendingPaymentEventMessage();
}
