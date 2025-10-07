package com.example.paymentservice.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.paymentservice.payment.application.port.in.CheckoutCommand;
import com.example.paymentservice.payment.application.port.in.CheckoutUsecase;
import com.example.paymentservice.payment.domain.PaymentEvent;
import com.example.paymentservice.payment.domain.PaymentOrder;
import com.example.paymentservice.payment.test.PaymentDatabaseHelper;
import com.example.paymentservice.payment.test.PaymentTestConfiguration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import reactor.test.StepVerifier;

@SpringBootTest
@Import(PaymentTestConfiguration.class)
class CheckoutServiceTest {

    @Autowired
    private CheckoutUsecase checkoutUsecase;

    @Autowired
    private PaymentDatabaseHelper paymentDatabaseHelper;

    @BeforeEach
    void setUp() {
        paymentDatabaseHelper.clean().block();
    }

    @Test
    void should_save_PaymentEvent_and_PaymentOrder_successfully() {

        String orderId = UUID.randomUUID().toString();
        CheckoutCommand command = CheckoutCommand.builder()
            .cartId(1L)
            .buyerId(1L)
            .productIds(List.of(1L, 2L, 3L))
            .idempotencyKey(orderId)
            .build();

        StepVerifier.create(checkoutUsecase.checkout(command))
            .expectNextMatches(it ->
                it.getAmount().intValue() == 60000 && it.getOrderId().equals(orderId))
            .verifyComplete();

        PaymentEvent paymentEvent = paymentDatabaseHelper.getPaymentEvent(orderId);

        assertThat(paymentEvent.getOrderId()).isEqualTo(orderId);
        assertThat(paymentEvent.totalAmount()).isEqualTo(60000);
        assertThat(paymentEvent.getPaymentOrders().size()).isEqualTo(command.getProductIds().size());
        assertFalse(paymentEvent.isPaymentDone());
        assertTrue(paymentEvent.getPaymentOrders().stream().noneMatch(PaymentOrder::isLedgerUpdated));
        assertTrue(paymentEvent.getPaymentOrders().stream().noneMatch(PaymentOrder::isWalletUpdated));
    }

    @Test
    void should_fail_to_save_PaymentEvent_and_PaymentOrder_when_trying_to_save_for_the_second_time() {

        String orderId = UUID.randomUUID().toString();
        CheckoutCommand command = CheckoutCommand.builder()
            .cartId(1L)
            .buyerId(1L)
            .productIds(List.of(1L, 2L, 3L))
            .idempotencyKey(orderId)
            .build();

        checkoutUsecase.checkout(command).block();

        assertThrows(
            DataIntegrityViolationException.class,
            () -> checkoutUsecase.checkout(command).block());
    }
}