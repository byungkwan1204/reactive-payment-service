package com.example.paymentservice.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import com.example.paymentservice.payment.application.port.in.CheckoutCommand;
import com.example.paymentservice.payment.application.port.in.CheckoutUsecase;
import com.example.paymentservice.payment.application.port.in.PaymentConfirmCommand;
import com.example.paymentservice.payment.application.port.out.PaymentExecutorPort;
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdatePort;
import com.example.paymentservice.payment.application.port.out.PaymentValidationPort;
import com.example.paymentservice.payment.domain.CheckoutResult;
import com.example.paymentservice.payment.domain.PGConfirmationStatus;
import com.example.paymentservice.payment.domain.PaymentConfirmationResult;
import com.example.paymentservice.payment.domain.PaymentEvent;
import com.example.paymentservice.payment.domain.PaymentExecutionResult;
import com.example.paymentservice.payment.domain.PaymentExecutionResult.PaymentExecutionFailure;
import com.example.paymentservice.payment.domain.PaymentExecutionResult.PaymentExtraDetails;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

@SpringBootTest
@Import(PaymentTestConfiguration.class)
@ExtendWith(MockitoExtension.class)
class PaymentConfirmServiceTest {

    @Autowired
    CheckoutUsecase checkoutUsecase;
    @Autowired
    PaymentStatusUpdatePort paymentStatusUpdatePort;
    @Autowired
    PaymentValidationPort paymentValidationPort;
    @Autowired
    PaymentDatabaseHelper paymentDatabaseHelper;

    @MockitoBean
    PaymentExecutorPort mockPaymentExecutorPort;

    @Autowired
    PaymentConfirmService paymentConfirmService;

    @BeforeEach
    void setUp() {
        paymentDatabaseHelper.clean().block();
    }

    @Test
    void should_be_marked_as_SUCCESS_if_Payment_Confirmation_success_in_PG() {
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
                              .pgConfirmationStatus(PGConfirmationStatus.DONE)
                              .approveAt(LocalDateTime.now())
                              .pgRawData("{}")
                              .build())
            .isSuccess(true)
            .isRetryable(false)
            .isUnknown(false)
            .isFailure(false)
            .build();

        Mockito.when(mockPaymentExecutorPort.execute(any(PaymentConfirmCommand.class)))
            .thenReturn(Mono.just(paymentExecutionResult));

        PaymentConfirmationResult paymentConfirmationResult =
            paymentConfirmService.confirm(paymentConfirmCommand).block();
        assertThat(paymentConfirmationResult).isNotNull();

        PaymentEvent paymentEvent = paymentDatabaseHelper.getPaymentEvent(orderId);

        assertThat(paymentConfirmationResult.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertTrue(paymentEvent.getPaymentOrders().stream().allMatch(order -> order.getPaymentStatus() == PaymentStatus.SUCCESS));
        assertThat(paymentEvent.getPaymentType()).isEqualTo(paymentExecutionResult.getExtraDetails().getType());
        assertThat(paymentEvent.getPaymentMethod()).isEqualTo(paymentExecutionResult.getExtraDetails().getMethod());
        assertThat(paymentEvent.getOrderName()).isEqualTo(paymentExecutionResult.getExtraDetails().getOrderName());
        assertThat(paymentEvent.getApprovedAt()).isEqualTo(paymentExecutionResult.getExtraDetails().getApproveAt().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void should_be_marked_as_FAILURE_if_Payment_Confirmation_fails_on_PG() {
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
                              .pgConfirmationStatus(PGConfirmationStatus.DONE)
                              .approveAt(LocalDateTime.now())
                              .pgRawData("{}")
                              .build())
            .failure(PaymentExecutionFailure.builder()
                         .errorCode("ERROR")
                         .message("Test Error")
                         .build())
            .isSuccess(false)
            .isRetryable(false)
            .isUnknown(false)
            .isFailure(true)
            .build();

        Mockito.when(mockPaymentExecutorPort.execute(any(PaymentConfirmCommand.class)))
            .thenReturn(Mono.just(paymentExecutionResult));

        PaymentConfirmationResult paymentConfirmationResult =
            paymentConfirmService.confirm(paymentConfirmCommand).block();
        assertThat(paymentConfirmationResult).isNotNull();

        PaymentEvent paymentEvent = paymentDatabaseHelper.getPaymentEvent(orderId);

        assertThat(paymentConfirmationResult.getStatus()).isEqualTo(PaymentStatus.FAILURE);
        assertTrue(paymentEvent.getPaymentOrders().stream().allMatch(order -> order.getPaymentStatus() == PaymentStatus.FAILURE));
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
                              .pgConfirmationStatus(PGConfirmationStatus.DONE)
                              .approveAt(LocalDateTime.now())
                              .pgRawData("{}")
                              .build())
            .failure(PaymentExecutionFailure.builder()
                         .errorCode("ERROR")
                         .message("Test Error")
                         .build())
            .isSuccess(false)
            .isRetryable(false)
            .isUnknown(true)
            .isFailure(false)
            .build();

        Mockito.when(mockPaymentExecutorPort.execute(any(PaymentConfirmCommand.class)))
            .thenReturn(Mono.just(paymentExecutionResult));

        PaymentConfirmationResult paymentConfirmationResult =
            paymentConfirmService.confirm(paymentConfirmCommand).block();
        assertThat(paymentConfirmationResult).isNotNull();

        PaymentEvent paymentEvent = paymentDatabaseHelper.getPaymentEvent(orderId);

        assertThat(paymentConfirmationResult.getStatus()).isEqualTo(PaymentStatus.UNKNOWN);
        assertTrue(paymentEvent.getPaymentOrders().stream().allMatch(order -> order.getPaymentStatus() == PaymentStatus.UNKNOWN));
    }
}