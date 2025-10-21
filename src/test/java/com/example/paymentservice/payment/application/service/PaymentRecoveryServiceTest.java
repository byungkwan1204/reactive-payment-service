package com.example.paymentservice.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.paymentservice.payment.adapter.out.web.toss.exception.PSPConfirmationException;
import com.example.paymentservice.payment.application.port.in.CheckoutCommand;
import com.example.paymentservice.payment.application.port.in.CheckoutUsecase;
import com.example.paymentservice.payment.application.port.in.PaymentConfirmCommand;
import com.example.paymentservice.payment.application.port.out.LoadPendingPaymentPort;
import com.example.paymentservice.payment.application.port.out.PaymentExecutorPort;
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand;
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdatePort;
import com.example.paymentservice.payment.application.port.out.PaymentValidationPort;
import com.example.paymentservice.payment.domain.CheckoutResult;
import com.example.paymentservice.payment.domain.PSPConfirmationStatus;
import com.example.paymentservice.payment.domain.PaymentExecutionResult;
import com.example.paymentservice.payment.domain.PaymentExecutionResult.PaymentExtraDetails;
import com.example.paymentservice.payment.domain.PaymentExecutionResult.PaymentFailure;
import com.example.paymentservice.payment.domain.PaymentMethod;
import com.example.paymentservice.payment.domain.PaymentStatus;
import com.example.paymentservice.payment.domain.PaymentType;
import com.example.paymentservice.payment.test.PaymentDatabaseHelper;
import com.example.paymentservice.payment.test.PaymentTestConfiguration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import reactor.core.publisher.Mono;

@Slf4j
@ExtendWith(MockitoExtension.class)
@Import(PaymentTestConfiguration.class)
@SpringBootTest
class PaymentRecoveryServiceTest {

    @Autowired
    private CheckoutUsecase checkoutUsecase;
    @Autowired
    private PaymentRecoveryService paymentRecoveryService;

    @MockitoSpyBean
    private LoadPendingPaymentPort loadPendingPaymentPort;
    @MockitoSpyBean
    private PaymentValidationPort paymentValidationPort;
    @MockitoSpyBean
    private PaymentStatusUpdatePort paymentStatusUpdatePort;
    @MockitoSpyBean
    private PaymentErrorHandler paymentErrorHandler;

    @MockitoBean
    private PaymentExecutorPort mockPaymentExecutorPort;

    @Autowired
    private PaymentDatabaseHelper paymentDatabaseHelper;

    @BeforeEach
    void setUp() {
        paymentDatabaseHelper.clean().block();
    }

    @Test
    void should_fail_to_recovery_when_an_unknown_exception_occurs() throws Exception {

        PaymentConfirmCommand paymentConfirmCommand = createUnknownStatusPaymentEvent();
        PaymentExecutionResult paymentExecutionResult = createPaymentExecutionResult(paymentConfirmCommand);

        Mockito.when(mockPaymentExecutorPort.execute(paymentConfirmCommand))
            .thenThrow(new PSPConfirmationException("UNKNOWN_ERROR", "test_error_message", false, false, true, true));

        paymentRecoveryService.recovery();

        Thread.sleep(10000);

    }

    @Test
    void should_recovery_payments() throws Exception {

        PaymentConfirmCommand paymentConfirmCommand = createUnknownStatusPaymentEvent();
        PaymentExecutionResult paymentExecutionResult = createPaymentExecutionResult(paymentConfirmCommand);

        Mockito.when(mockPaymentExecutorPort.execute(paymentConfirmCommand))
            .thenReturn(Mono.just(paymentExecutionResult));

        paymentRecoveryService.recovery();

        Thread.sleep(10000);
    }

    private PaymentConfirmCommand createUnknownStatusPaymentEvent() {
        String orderId = UUID.randomUUID().toString();
        String paymentKey = UUID.randomUUID().toString();

        CheckoutCommand checkoutCommand = CheckoutCommand.builder()
            .cartId(1L)
            .buyerId(1L)
            .productIds(List.of(1L, 2L))
            .idempotencyKey(orderId)
            .build();

        CheckoutResult checkoutResult = checkoutUsecase.checkout(checkoutCommand).block();
        assertThat(checkoutResult).isNotNull();

        PaymentConfirmCommand paymentConfirmCommand = PaymentConfirmCommand.builder()
            .paymentKey(paymentKey)
            .orderId(checkoutResult.getOrderId())
            .amount(checkoutResult.getAmount())
            .build();

        paymentStatusUpdatePort.updatePaymentStatusToExecuting(paymentConfirmCommand.getPaymentKey(), paymentConfirmCommand.getOrderId()).block();

        PaymentStatusUpdateCommand paymentStatusUpdateCommand = PaymentStatusUpdateCommand.builder()
            .paymentKey(paymentConfirmCommand.getPaymentKey())
            .orderId(paymentConfirmCommand.getOrderId())
            .status(PaymentStatus.UNKNOWN)
            .failure(PaymentFailure.builder().errorCode("UNKNOWN").message("UNKNOWN").build())
            .build();

        paymentStatusUpdatePort.updatePaymentStatus(paymentStatusUpdateCommand).block();

        return paymentConfirmCommand;
    }

    private PaymentExecutionResult createPaymentExecutionResult(PaymentConfirmCommand paymentConfirmCommand) {
        return PaymentExecutionResult.builder()
            .paymentKey(paymentConfirmCommand.getPaymentKey())
            .orderId(paymentConfirmCommand.getOrderId())
            .extraDetails(
                PaymentExtraDetails.builder()
                    .type(PaymentType.NORMAL)
                    .method(PaymentMethod.EASY_PAY)
                    .totalAmount(paymentConfirmCommand.getAmount())
                    .orderName("test_order_name")
                    .pspConfirmationStatus(PSPConfirmationStatus.DONE)
                    .approveAt(LocalDateTime.now())
                    .pspRawData("{}")
                    .build())
            .isSuccess(true)
            .isFailure(false)
            .isUnknown(false)
            .isRetryable(false)
            .build();
    }
}