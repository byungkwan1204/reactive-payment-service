package com.example.paymentservice.payment.application.port.out;

import com.example.paymentservice.payment.application.port.in.PaymentConfirmCommand;
import com.example.paymentservice.payment.domain.PaymentExecutionResult;
import reactor.core.publisher.Mono;

public interface PaymentExecutorPort {

    Mono<PaymentExecutionResult> execute(PaymentConfirmCommand command);
}
