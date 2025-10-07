package com.example.paymentservice.payment.adapter.out.web.toss.executor;

import com.example.paymentservice.payment.application.port.in.PaymentConfirmCommand;
import com.example.paymentservice.payment.domain.PaymentExecutionResult;
import reactor.core.publisher.Mono;

public interface PaymentExecutor {

    Mono<PaymentExecutionResult> execute(PaymentConfirmCommand command);
}
