package com.example.paymentservice.payment.application.service;

import com.example.paymentservice.payment.adapter.out.persistent.repository.PaymentOutboxRepository;
import com.example.paymentservice.payment.application.port.in.PaymentEventMessageRelayUsecase;
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand;
import com.example.paymentservice.payment.domain.PSPConfirmationStatus;
import com.example.paymentservice.payment.domain.PaymentExecutionResult;
import com.example.paymentservice.payment.domain.PaymentExecutionResult.PaymentExtraDetails;
import com.example.paymentservice.payment.domain.PaymentMethod;
import com.example.paymentservice.payment.domain.PaymentType;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Hooks;

@Tag("ExternalIntegration")
@SpringBootTest
class PaymentEventMessageRelayServiceTest {

    @Autowired
    private PaymentOutboxRepository paymentOutboxRepository;
    @Autowired
    private PaymentEventMessageRelayUsecase paymentEventMessageRelayUsecase;

    @Test
    void should_dispatch_external_message_system() throws InterruptedException {

        Hooks.onOperatorDebug();

        PaymentStatusUpdateCommand command =
            PaymentStatusUpdateCommand.ofExecutionResult(
                PaymentExecutionResult.builder()
                    .paymentKey(UUID.randomUUID().toString())
                    .orderId(UUID.randomUUID().toString())
                    .extraDetails(PaymentExtraDetails.builder()
                                      .type(PaymentType.NORMAL)
                                      .method(PaymentMethod.EASY_PAY)
                                      .approveAt(LocalDateTime.now())
                                      .orderName("test_order_name")
                                      .pspConfirmationStatus(PSPConfirmationStatus.DONE)
                                      .totalAmount(50000L)
                                      .pspRawData("{}")
                                      .build())
                    .isSuccess(true)
                    .isFailure(false)
                    .isUnknown(false)
                    .isRetryable(false)
                    .build());

        paymentOutboxRepository.insertOutbox(command).block();

        paymentEventMessageRelayUsecase.relay();

        Thread.sleep(1000);
    }
}