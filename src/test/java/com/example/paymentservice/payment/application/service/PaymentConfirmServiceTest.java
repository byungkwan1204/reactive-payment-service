package com.example.paymentservice.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.paymentservice.payment.adapter.out.persistent.exception.PaymentValidationException;
import com.example.paymentservice.payment.adapter.out.web.toss.exception.PSPConfirmationException;
import com.example.paymentservice.payment.adapter.out.web.toss.exception.TossPaymentError;
import com.example.paymentservice.payment.application.port.in.CheckoutCommand;
import com.example.paymentservice.payment.application.port.in.CheckoutUsecase;
import com.example.paymentservice.payment.application.port.in.PaymentConfirmCommand;
import com.example.paymentservice.payment.application.port.out.PaymentExecutorPort;
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdatePort;
import com.example.paymentservice.payment.application.port.out.PaymentValidationPort;
import com.example.paymentservice.payment.domain.CheckoutResult;
import com.example.paymentservice.payment.domain.PSPConfirmationStatus;
import com.example.paymentservice.payment.domain.PaymentConfirmationResult;
import com.example.paymentservice.payment.domain.PaymentEvent;
import com.example.paymentservice.payment.domain.PaymentExecutionResult;
import com.example.paymentservice.payment.domain.PaymentExecutionResult.PaymentExtraDetails;
import com.example.paymentservice.payment.domain.PaymentExecutionResult.PaymentFailure;
import com.example.paymentservice.payment.domain.PaymentMethod;
import com.example.paymentservice.payment.domain.PaymentStatus;
import com.example.paymentservice.payment.domain.PaymentType;
import com.example.paymentservice.payment.test.PaymentDatabaseHelper;
import com.example.paymentservice.payment.test.PaymentTestConfiguration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import reactor.core.publisher.Mono;

@SpringBootTest
@Import(PaymentTestConfiguration.class)
@ExtendWith(MockitoExtension.class)
class PaymentConfirmServiceTest {

    @Autowired
    CheckoutUsecase checkoutUsecase;
    @Autowired
    PaymentDatabaseHelper paymentDatabaseHelper;
    @Autowired
    PaymentConfirmService paymentConfirmService;
    @Autowired
    PaymentStatusUpdatePort paymentStatusUpdatePort;

    @MockitoSpyBean
    PaymentValidationPort paymentValidationPort;

    @MockitoSpyBean
    PaymentExecutorPort paymentExecutorPort;


    @BeforeEach
    void setUp() {
        paymentDatabaseHelper.clean().block();
    }

    @Test
    void should_be_marked_as_SUCCESS_if_Payment_Confirmation_success_in_PSP() {
        String orderId = UUID.randomUUID().toString();

        CheckoutCommand checloutCommand = CheckoutCommand.builder()
            .cartId(1L)
            .buyerId(1L)
            .productIds(List.of(1L, 2L, 3L))
            .idempotencyKey(orderId)
            .build();

        CheckoutResult checkoutResult = checkoutUsecase.checkout(checloutCommand).block();
        assertThat(checkoutResult).isNotNull();

        PaymentConfirmCommand paymentConfirmCommand =
            PaymentConfirmCommand.builder()
                .paymentKey(UUID.randomUUID().toString())
                .orderId(orderId)
                .amount(checkoutResult.getAmount())
                .build();

        PaymentExecutionResult paymentExecutionResult = PaymentExecutionResult.builder()
            .paymentKey(paymentConfirmCommand.getPaymentKey())
            .orderId(paymentConfirmCommand.getOrderId())
            .extraDetails(PaymentExtraDetails.builder()
                              .type(PaymentType.NORMAL)
                              .method(PaymentMethod.EASY_PAY)
                              .totalAmount(paymentConfirmCommand.getAmount())
                              .orderName("test_order_name")
                              .pspConfirmationStatus(PSPConfirmationStatus.DONE)
                              .approveAt(LocalDateTime.now())
                              .pspRawData("{}")
                              .build())
            .isSuccess(true)
            .isRetryable(false)
            .isUnknown(false)
            .isFailure(false)
            .build();

        Mockito.when(paymentExecutorPort.execute(paymentConfirmCommand))
            .thenReturn(Mono.just(paymentExecutionResult));

        PaymentConfirmationResult paymentConfirmationResult =
            paymentConfirmService.confirm(paymentConfirmCommand).block();
        assertThat(paymentConfirmationResult).isNotNull();

        PaymentEvent paymentEvent = paymentDatabaseHelper.getPaymentEvent(orderId);

        assertThat(paymentConfirmationResult.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertTrue(paymentEvent.isSuccess());
        assertThat(paymentEvent.getPaymentType()).isEqualTo(paymentExecutionResult.getExtraDetails().getType());
        assertThat(paymentEvent.getPaymentMethod()).isEqualTo(paymentExecutionResult.getExtraDetails().getMethod());
        assertThat(paymentEvent.getOrderName()).isEqualTo(paymentExecutionResult.getExtraDetails().getOrderName());
        assertThat(paymentEvent.getApprovedAt()).isEqualTo(paymentExecutionResult.getExtraDetails().getApproveAt().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void should_be_marked_as_FAILURE_if_Payment_Confirmation_fails_on_PSP() {
        String orderId = UUID.randomUUID().toString();

        CheckoutCommand checloutCommand = CheckoutCommand.builder()
            .cartId(1L)
            .buyerId(1L)
            .productIds(List.of(1L, 2L, 3L))
            .idempotencyKey(orderId)
            .build();

        CheckoutResult checkoutResult = checkoutUsecase.checkout(checloutCommand).block();
        assertThat(checkoutResult).isNotNull();

        PaymentConfirmCommand paymentConfirmCommand =
            PaymentConfirmCommand.builder()
                .paymentKey(UUID.randomUUID().toString())
                .orderId(orderId)
                .amount(checkoutResult.getAmount())
                .build();

        PaymentExecutionResult paymentExecutionResult = PaymentExecutionResult.builder()
            .paymentKey(paymentConfirmCommand.getPaymentKey())
            .orderId(paymentConfirmCommand.getOrderId())
            .extraDetails(PaymentExtraDetails.builder()
                              .type(PaymentType.NORMAL)
                              .method(PaymentMethod.EASY_PAY)
                              .totalAmount(paymentConfirmCommand.getAmount())
                              .orderName("test_order_name")
                              .pspConfirmationStatus(PSPConfirmationStatus.DONE)
                              .approveAt(LocalDateTime.now())
                              .pspRawData("{}")
                              .build())
            .failure(PaymentFailure.builder()
                         .errorCode("ERROR")
                         .message("Test Error")
                         .build())
            .isSuccess(false)
            .isRetryable(false)
            .isUnknown(false)
            .isFailure(true)
            .build();

        Mockito.when(paymentExecutorPort.execute(paymentConfirmCommand))
            .thenReturn(Mono.just(paymentExecutionResult));

        PaymentConfirmationResult paymentConfirmationResult =
            paymentConfirmService.confirm(paymentConfirmCommand).block();
        assertThat(paymentConfirmationResult).isNotNull();

        PaymentEvent paymentEvent = paymentDatabaseHelper.getPaymentEvent(orderId);

        assertThat(paymentConfirmationResult.getStatus()).isEqualTo(PaymentStatus.FAILURE);
        assertTrue(paymentEvent.isFailure());
    }

    @Test
    void should_be_marked_as_UNKNOWN_if_Payment_Confirmation_fails_due_to_an_unknown_exception() {
        String orderId = UUID.randomUUID().toString();

        CheckoutCommand checloutCommand = CheckoutCommand.builder()
            .cartId(1L)
            .buyerId(1L)
            .productIds(List.of(1L, 2L, 3L))
            .idempotencyKey(orderId)
            .build();

        CheckoutResult checkoutResult = checkoutUsecase.checkout(checloutCommand).block();
        assertThat(checkoutResult).isNotNull();

        PaymentConfirmCommand paymentConfirmCommand =
            PaymentConfirmCommand.builder()
                .paymentKey(UUID.randomUUID().toString())
                .orderId(orderId)
                .amount(checkoutResult.getAmount())
                .build();

        PaymentExecutionResult paymentExecutionResult = PaymentExecutionResult.builder()
            .paymentKey(paymentConfirmCommand.getPaymentKey())
            .orderId(paymentConfirmCommand.getOrderId())
            .extraDetails(PaymentExtraDetails.builder()
                              .type(PaymentType.NORMAL)
                              .method(PaymentMethod.EASY_PAY)
                              .totalAmount(paymentConfirmCommand.getAmount())
                              .orderName("test_order_name")
                              .pspConfirmationStatus(PSPConfirmationStatus.DONE)
                              .approveAt(LocalDateTime.now())
                              .pspRawData("{}")
                              .build())
            .failure(PaymentFailure.builder()
                         .errorCode("ERROR")
                         .message("Test Error")
                         .build())
            .isSuccess(false)
            .isRetryable(false)
            .isUnknown(true)
            .isFailure(false)
            .build();

        Mockito.when(paymentExecutorPort.execute(paymentConfirmCommand))
            .thenReturn(Mono.just(paymentExecutionResult));

        PaymentConfirmationResult paymentConfirmationResult =
            paymentConfirmService.confirm(paymentConfirmCommand).block();
        assertThat(paymentConfirmationResult).isNotNull();

        PaymentEvent paymentEvent = paymentDatabaseHelper.getPaymentEvent(orderId);

        assertThat(paymentConfirmationResult.getStatus()).isEqualTo(PaymentStatus.UNKNOWN);
        assertTrue(paymentEvent.isUnknown());
    }

    @Test
    void should_handle_PSPConfirmationException() {

        String orderId = UUID.randomUUID().toString();

        CheckoutCommand checloutCommand = CheckoutCommand.builder()
            .cartId(1L)
            .buyerId(1L)
            .productIds(List.of(1L, 2L, 3L))
            .idempotencyKey(orderId)
            .build();

        CheckoutResult checkoutResult = checkoutUsecase.checkout(checloutCommand).block();
        assertThat(checkoutResult).isNotNull();

        PaymentConfirmCommand paymentConfirmCommand =
            PaymentConfirmCommand.builder()
                .paymentKey(UUID.randomUUID().toString())
                .orderId(orderId)
                .amount(checkoutResult.getAmount())
                .build();

        PSPConfirmationException pspConfirmationException =
            new PSPConfirmationException(
                TossPaymentError.REJECT_ACCOUNT_PAYMENT.name(), TossPaymentError.REJECT_ACCOUNT_PAYMENT.getDescription(), false, true, false, false);


        Mockito.when(paymentExecutorPort.execute(paymentConfirmCommand))
            .thenReturn(Mono.error(pspConfirmationException));

        PaymentConfirmationResult paymentConfirmationResult =
            paymentConfirmService.confirm(paymentConfirmCommand).block();
        assertThat(paymentConfirmationResult).isNotNull();

        PaymentEvent paymentEvent = paymentDatabaseHelper.getPaymentEvent(orderId);

        assertThat(paymentConfirmationResult.getStatus()).isEqualTo(PaymentStatus.FAILURE);
        assertTrue(paymentEvent.isFailure());
    }

    @Test
    void should_handle_PaymentValidationException() {

        String orderId = UUID.randomUUID().toString();

        CheckoutCommand checloutCommand = CheckoutCommand.builder()
            .cartId(1L)
            .buyerId(1L)
            .productIds(List.of(1L, 2L, 3L))
            .idempotencyKey(orderId)
            .build();

        CheckoutResult checkoutResult = checkoutUsecase.checkout(checloutCommand).block();
        assertThat(checkoutResult).isNotNull();

        PaymentConfirmCommand paymentConfirmCommand =
            PaymentConfirmCommand.builder()
                .paymentKey(UUID.randomUUID().toString())
                .orderId(orderId)
                .amount(checkoutResult.getAmount())
                .build();

        PaymentValidationException paymentValidationException = new PaymentValidationException("결제 유효성 검증에서 실패하였습니다.");

        Mockito.when(paymentValidationPort.isValid(orderId, paymentConfirmCommand.getAmount()))
            .thenReturn(Mono.error(paymentValidationException));

        PaymentConfirmationResult paymentConfirmationResult =
            paymentConfirmService.confirm(paymentConfirmCommand).block();

        PaymentEvent paymentEvent = paymentDatabaseHelper.getPaymentEvent(orderId);

        assertThat(paymentConfirmationResult).isNotNull();
        assertThat(paymentConfirmationResult.getStatus()).isEqualTo(PaymentStatus.FAILURE);
        assertTrue(paymentEvent.isFailure());
    }

    @Test
    @Tag("ExternalIntegration")
    void should_send_the_event_message_to_the_external_message_system_after_the_payment_confirmation_has_bean_successful() throws InterruptedException {

        String orderId = UUID.randomUUID().toString();

        CheckoutCommand checloutCommand = CheckoutCommand.builder()
            .cartId(1L)
            .buyerId(1L)
            .productIds(List.of(1L, 2L, 3L))
            .idempotencyKey(orderId)
            .build();

        CheckoutResult checkoutResult = checkoutUsecase.checkout(checloutCommand).block();
        assertThat(checkoutResult).isNotNull();

        PaymentConfirmCommand paymentConfirmCommand =
            PaymentConfirmCommand.builder()
                .paymentKey(UUID.randomUUID().toString())
                .orderId(orderId)
                .amount(checkoutResult.getAmount())
                .build();

        PaymentExecutionResult paymentExecutionResult = PaymentExecutionResult.builder()
            .paymentKey(paymentConfirmCommand.getPaymentKey())
            .orderId(paymentConfirmCommand.getOrderId())
            .extraDetails(PaymentExtraDetails.builder()
                              .type(PaymentType.NORMAL)
                              .method(PaymentMethod.EASY_PAY)
                              .totalAmount(paymentConfirmCommand.getAmount())
                              .orderName("test_order_name")
                              .pspConfirmationStatus(PSPConfirmationStatus.DONE)
                              .approveAt(LocalDateTime.now())
                              .pspRawData("{}")
                              .build())
            .isSuccess(true)
            .isRetryable(false)
            .isUnknown(false)
            .isFailure(false)
            .build();

        Mockito.when(paymentExecutorPort.execute(paymentConfirmCommand))
            .thenReturn(Mono.just(paymentExecutionResult));

        paymentConfirmService.confirm(paymentConfirmCommand).block();

        Thread.sleep(10000);
    }
}