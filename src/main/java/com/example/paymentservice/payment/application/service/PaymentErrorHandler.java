package com.example.paymentservice.payment.application.service;

import com.example.paymentservice.payment.adapter.out.persistent.exception.PaymentAlreadyProcessedException;
import com.example.paymentservice.payment.adapter.out.persistent.exception.PaymentValidationException;
import com.example.paymentservice.payment.adapter.out.web.toss.exception.PSPConfirmationException;
import com.example.paymentservice.payment.application.port.in.PaymentConfirmCommand;
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand;
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdatePort;
import com.example.paymentservice.payment.domain.PaymentConfirmationResult;
import com.example.paymentservice.payment.domain.PaymentExecutionResult.PaymentFailure;
import com.example.paymentservice.payment.domain.PaymentStatus;
import io.netty.handler.timeout.TimeoutException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PaymentErrorHandler {

    private final PaymentStatusUpdatePort paymentStatusUpdatePort;

    public Mono<PaymentConfirmationResult> handlePaymentError(PaymentConfirmCommand command, Throwable error) {
        if (error instanceof PaymentAlreadyProcessedException e) {

            PaymentFailure failure =
                new PaymentFailure(e.getClass().getSimpleName(), Optional.ofNullable(e.getMessage()).orElse(""));

            return Mono.just(new PaymentConfirmationResult(e.getStatus(), failure));
        }

        PaymentStatus status;
        PaymentFailure failure;

        if (error instanceof PSPConfirmationException e) {
            status = e.paymentStatus();
            failure = new PaymentFailure(e.getClass().getSimpleName(), Optional.ofNullable(e.getMessage()).orElse(""));
        } else if (error instanceof PaymentValidationException e) {
            status = PaymentStatus.FAILURE;
            failure = new PaymentFailure(e.getClass().getSimpleName(), Optional.ofNullable(e.getMessage()).orElse(""));
        } else if (error instanceof TimeoutException e) {
            status = PaymentStatus.UNKNOWN;
            failure = new PaymentFailure(e.getClass().getSimpleName(), Optional.ofNullable(e.getMessage()).orElse(""));
        } else {
            status = PaymentStatus.UNKNOWN;
            failure = new PaymentFailure(error.getClass().getSimpleName(), Optional.ofNullable(error.getMessage()).orElse(""));
        }

        PaymentStatusUpdateCommand paymentStatusUpdateCommand = PaymentStatusUpdateCommand.builder()
            .paymentKey(command.getPaymentKey())
            .orderId(command.getOrderId())
            .status(status)
            .failure(failure)
            .build();

        return paymentStatusUpdatePort.updatePaymentStatus(paymentStatusUpdateCommand)
            .map(result -> PaymentConfirmationResult.builder()
                .status(status)
                .failure(failure)
                .build());
    }
}
