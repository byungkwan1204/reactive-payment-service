package com.example.paymentservice.payment.application.service;

import com.example.paymentservice.common.Usecase;
import com.example.paymentservice.payment.application.port.in.PaymentConfirmCommand;
import com.example.paymentservice.payment.application.port.in.PaymentConfirmUsecase;
import com.example.paymentservice.payment.application.port.out.PaymentExecutorPort;
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand;
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdatePort;
import com.example.paymentservice.payment.application.port.out.PaymentValidationPort;
import com.example.paymentservice.payment.domain.PaymentConfirmationResult;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Usecase
@RequiredArgsConstructor
public class PaymentConfirmService implements PaymentConfirmUsecase {

    private final PaymentStatusUpdatePort paymentStatusUpdatePort;
    private final PaymentValidationPort paymentValidationPort;
    private final PaymentExecutorPort paymentExecutorPort;

    private final PaymentErrorHandler paymentErrorHandler;

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
             .onErrorResume(error -> paymentErrorHandler.handlePaymentError(command, error));
    }
}
