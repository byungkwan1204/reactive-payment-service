package com.example.paymentservice.payment.application.service;

import com.example.paymentservice.common.Usecase;
import com.example.paymentservice.payment.adapter.out.persistent.exception.PaymentAlreadyProcessedException;
import com.example.paymentservice.payment.adapter.out.persistent.exception.PaymentValidationException;
import com.example.paymentservice.payment.adapter.out.web.toss.exception.PSPConfirmationException;
import com.example.paymentservice.payment.application.port.in.PaymentConfirmCommand;
import com.example.paymentservice.payment.application.port.in.PaymentConfirmUsecase;
import com.example.paymentservice.payment.application.port.out.PaymentExecutorPort;
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand;
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdatePort;
import com.example.paymentservice.payment.application.port.out.PaymentValidationPort;
import com.example.paymentservice.payment.domain.PaymentConfirmationResult;
import com.example.paymentservice.payment.domain.PaymentExecutionResult.PaymentFailure;
import com.example.paymentservice.payment.domain.PaymentStatus;
import io.netty.handler.timeout.TimeoutException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Usecase
@RequiredArgsConstructor
public class PaymentConfirmService implements PaymentConfirmUsecase {

    private final PaymentStatusUpdatePort paymentStatusUpdatePort;
    private final PaymentValidationPort paymentValidationPort;
    private final PaymentExecutorPort paymentExecutorPort;

    @Override
    public Mono<PaymentConfirmationResult> confirm(PaymentConfirmCommand command) {
         return paymentStatusUpdatePort.updatePaymentStatusToExecuting(command.getPaymentKey(), command.getOrderId())
             .filterWhen(result -> paymentValidationPort.isValid(command.getOrderId(), command.getAmount()))
             .then(paymentExecutorPort.execute(command))
             .flatMap(result ->
                 paymentStatusUpdatePort.updatePaymentStatus(
                     PaymentStatusUpdateCommand.builder()
                         .paymentKey(result.getPaymentKey())
                         .orderId(result.getOrderId())
                         .status(result.paymentStatus())
                         .extraDetails(result.getExtraDetails())
                         .failure(result.getFailure())
                         .build())
                     .thenReturn(result))
             .map(result ->
                      PaymentConfirmationResult.builder()
                          .status(result.paymentStatus())
                          .failure(result.getFailure())
                          .build())
             // 에러 핸들링
             .onErrorResume(error -> handlePaymentError(command, error));
    }

    private Mono<PaymentConfirmationResult> handlePaymentError(PaymentConfirmCommand command, Throwable error) {
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
