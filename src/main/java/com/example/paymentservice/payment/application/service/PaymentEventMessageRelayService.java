package com.example.paymentservice.payment.application.service;

import com.example.paymentservice.common.LoggerUtil;
import com.example.paymentservice.common.Usecase;
import com.example.paymentservice.payment.application.port.in.PaymentEventMessageRelayUsecase;
import com.example.paymentservice.payment.application.port.out.DispatchEventMessagePort;
import com.example.paymentservice.payment.application.port.out.LoadPendingPaymentEventMessagePort;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

//@Profile("dev")
@Usecase
@RequiredArgsConstructor
public class PaymentEventMessageRelayService implements PaymentEventMessageRelayUsecase {

    private final LoadPendingPaymentEventMessagePort loadPendingPaymentEventMessagePort;
    private final DispatchEventMessagePort dispatchEventMessagePort;

    private static final Scheduler scheduler = Schedulers.newSingle("message-relay");

    // 재진입 방지 플래그
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedDelay = 100, initialDelay = 100, timeUnit = TimeUnit.SECONDS)
    @Override
    public void relay() {

        if (running.getAndSet(true)) {
            return;
        }

        loadPendingPaymentEventMessagePort.getPendingPaymentEventMessage()
            .doOnNext(dispatchEventMessagePort::dispatch)
            .onErrorContinue((err, ignore) -> LoggerUtil.error("messageRelay", StringUtils.hasText(err.getMessage()) ? "failed to relay message" : "", err))
            .doFinally(signal -> running.set(false))
            .subscribeOn(scheduler)
            .subscribe();
    }
}
