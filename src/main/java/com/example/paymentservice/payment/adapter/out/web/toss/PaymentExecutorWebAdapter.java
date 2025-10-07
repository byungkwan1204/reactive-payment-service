package com.example.paymentservice.payment.adapter.out.web.toss;

import com.example.paymentservice.common.WebAdapter;
import com.example.paymentservice.payment.adapter.out.web.toss.executor.PaymentExecutor;
import com.example.paymentservice.payment.application.port.in.PaymentConfirmCommand;
import com.example.paymentservice.payment.application.port.out.PaymentExecutorPort;
import com.example.paymentservice.payment.domain.PaymentExecutionResult;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@WebAdapter
@RequiredArgsConstructor
public class PaymentExecutorWebAdapter implements PaymentExecutorPort {

    private final PaymentExecutor paymentExecutor;

    @Override
    public Mono<PaymentExecutionResult> execute(PaymentConfirmCommand command) {
        return paymentExecutor.execute(command);
    }
}
