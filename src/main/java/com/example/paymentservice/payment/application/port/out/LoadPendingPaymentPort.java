package com.example.paymentservice.payment.application.port.out;

import com.example.paymentservice.payment.domain.PendingPaymentEvent;
import reactor.core.publisher.Flux;

public interface LoadPendingPaymentPort {

    Flux<PendingPaymentEvent> getPendingPayments();
}
