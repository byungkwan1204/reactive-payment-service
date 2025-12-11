package com.example.paymentservice.payment.adapter.out.persistent.stream.util;

import com.example.paymentservice.payment.domain.PaymentEventMessage;
import com.example.paymentservice.payment.domain.PaymentEventMessage.PaymentEventMessageType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("ExternalIntegration")
class PaymentEventMessageSenderTest {

    @Autowired
    private PaymentEventMessageSender paymentEventMessageSender;

    @Test
    void should_send_eventMessage_by_using_partitionKey() throws InterruptedException {

        List<PaymentEventMessage> paymentEventMessages = List.of(
            PaymentEventMessage.builder()
              .type(PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS)
              .payload(Map.of("orderId", UUID.randomUUID()
                  .toString()))
              .metadata(Map.of("partitionKey", 0))
              .build(), PaymentEventMessage.builder()
              .type(PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS)
              .payload(Map.of("orderId", UUID.randomUUID()
                  .toString()))
              .metadata(Map.of("partitionKey", 1))
              .build(), PaymentEventMessage.builder()
              .type(PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS)
              .payload(Map.of("orderId", UUID.randomUUID()
                  .toString()))
              .metadata(Map.of("partitionKey", 2))
              .build(), PaymentEventMessage.builder()
              .type(PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS)
              .payload(Map.of("orderId", UUID.randomUUID()
                  .toString()))
              .metadata(Map.of("partitionKey", 3))
              .build(), PaymentEventMessage.builder()
              .type(PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS)
              .payload(Map.of("orderId", UUID.randomUUID()
                  .toString()))
              .metadata(Map.of("partitionKey", 4))
              .build(), PaymentEventMessage.builder()
              .type(PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS)
              .payload(Map.of("orderId", UUID.randomUUID()
                  .toString()))
              .metadata(Map.of("partitionKey", 5))
              .build(), PaymentEventMessage.builder()
              .type(PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS)
              .payload(Map.of("orderId", UUID.randomUUID()
                  .toString()))
              .metadata(Map.of("partitionKey", 6))
              .build());

        paymentEventMessages.forEach(paymentEventMessageSender::dispatch);

        Thread.sleep(10000);
    }
}