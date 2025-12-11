package com.example.paymentservice.payment.application.service;

import com.example.paymentservice.common.LoggerUtil;
import com.example.paymentservice.common.Usecase;
import com.example.paymentservice.payment.application.port.in.PaymentEventMessageRelayUsecase;
import com.example.paymentservice.payment.application.port.out.DispatchEventMessagePort;
import com.example.paymentservice.payment.application.port.out.LoadPendingPaymentEventMessagePort;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Profile("dev")
@Usecase
@RequiredArgsConstructor
public class PaymentEventMessageRelayService implements PaymentEventMessageRelayUsecase {

    private final LoadPendingPaymentEventMessagePort loadPendingPaymentEventMessagePort;
    private final DispatchEventMessagePort dispatchEventMessagePort;

    private static final Scheduler scheduler = Schedulers.newSingle("message-relay");

    @Scheduled(fixedDelay = 100, initialDelay = 100, timeUnit = TimeUnit.SECONDS)
    @Override
    public void relay() {
        loadPendingPaymentEventMessagePort.getPendingPaymentEventMessage()
            .doOnNext(dispatchEventMessagePort::dispatch)
            .onErrorContinue((err, ignore) -> LoggerUtil.error("messageRelay", StringUtils.hasText(err.getMessage()) ? "failed to relay message" : "", err))
            .subscribeOn(scheduler)
            .subscribe();
    }
}
