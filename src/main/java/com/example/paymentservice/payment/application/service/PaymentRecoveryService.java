package com.example.paymentservice.payment.application.service;

import com.example.paymentservice.common.Usecase;
import com.example.paymentservice.payment.application.port.in.PaymentConfirmCommand;
import com.example.paymentservice.payment.application.port.in.PaymentRecoveryUsecase;
import com.example.paymentservice.payment.application.port.out.LoadPendingPaymentPort;
import com.example.paymentservice.payment.application.port.out.PaymentExecutorPort;
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand;
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdatePort;
import com.example.paymentservice.payment.application.port.out.PaymentValidationPort;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Usecase
@RequiredArgsConstructor
public class PaymentRecoveryService implements PaymentRecoveryUsecase {

    private final LoadPendingPaymentPort loadPendingPaymentPort;
    private final PaymentValidationPort paymentValidationPort;
    private final PaymentExecutorPort paymentExecutorPort;
    private final PaymentStatusUpdatePort paymentStatusUpdatePort;

    private final PaymentErrorHandler paymentErrorHandler;

    private static final Scheduler scheduler = Schedulers.newSingle("recovery");

    @Scheduled(fixedDelay = 180, initialDelay = 100, timeUnit = TimeUnit.SECONDS)
    @Override
    public void recovery() {
        loadPendingPaymentPort.getPendingPayments()
        .map(it ->
                 PaymentConfirmCommand.builder()
                     .paymentKey(it.getPaymentKey())
                     .orderId(it.getOrderId())
                     .amount(it.totalAmount())
                     .build()
            )
            .parallel(2)
            .runOn(Schedulers.parallel())
            .flatMap(command ->
                 paymentValidationPort.isValid(command.getOrderId(), command.getAmount()).thenReturn(command)
                     .flatMap(paymentExecutorPort::execute)
                     .flatMap(it -> paymentStatusUpdatePort.updatePaymentStatus(PaymentStatusUpdateCommand.ofExecutionResult(it)).thenReturn(it))
                     .onErrorResume(error -> paymentErrorHandler.handlePaymentError(command, error).then(Mono.error(error)))
            )
            .sequential()   // 병렬로 처리된 데이터를 하나의 Flux 로 합친다.
            .doOnEach(it -> {
                if (it.hasError() && !it.isOnComplete()) {
                    System.out.println("recovery failure, orderId: " + (it.get() != null ? it.get().getOrderId() : null));
                } else {
                    System.out.println("recovery success, orderId: " + (it.get() != null ? it.get().getOrderId() : null));
                }
            })
            // 중요한 처리 작업을 시스템에 다른 부분에 영향을 주지않도록 격리하는 방법을 bulk-head 패턴이라고 한다.
            // 즉, 아래와 같이 recovery 처리 전용 스케줄러 (스레드풀) 를 기반으로 처리하는 방법을 말한다.
            // 만약 스케줄러를 공용으로 사용한다면, 리소스에 대한 경합이 발생해서 충분한 CPU를 얻지못하는 문제가 발생하거나
            // 하나의 작업 처리중에 발생한 예외로 인해서 스케줄러의 다른 작업으로 전파되서 해당 작업에 영향을 미칠 수 있다.
            // 따라서 시스템의 안정성을 위해서 각 작업마다 격리하는게 중요하다.
            .subscribeOn(scheduler)
            .subscribe();    // 병렬 데이터 스트림을 끝내고 이 스트림을 처리할 수 있도록 구독 한다.
    }
}
