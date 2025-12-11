package com.example.paymentservice.payment.adapter.out.persistent.repository;

import com.example.paymentservice.common.ObjectMapperUtil;
import com.example.paymentservice.payment.adapter.out.persistent.stream.util.PartitionKeyUtil;
import com.example.paymentservice.payment.adapter.out.persistent.util.MySQLDateTimeFormatter;
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand;
import com.example.paymentservice.payment.domain.PaymentEventMessage;
import com.example.paymentservice.payment.domain.PaymentEventMessage.PaymentEventMessageType;
import com.example.paymentservice.payment.domain.PaymentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class R2DBCPaymentOutboxRepository implements PaymentOutboxRepository {

    private final DatabaseClient databaseClient;
    private final PartitionKeyUtil partitionKeyUtil;


    private static final String INSERT_OUTBOX_QUERY = """
                                                      INSERT INTO outboxes (idempotency_key, type, partition_key, payload, metadata)
                                                      VALUES(:idempotencyKey, :type, :partitionKey, :payload, :metadata)
                                                      """;

    private static final String UPDATE_OUTBOX_MESSAGE_AS_SENT_QUERY = """
                                                                      UPDATE outboxes SET status = 'SUCCESS' 
                                                                      WHERE idempotency_key = :idempotencyKey
                                                                      AND type = :type
                                                                      """;

    private static final String UPDATE_OUTBOX_MESSAGE_AS_FAILURE_QUERY = """
                                                                      UPDATE outboxes SET status = 'FAILURE' 
                                                                      WHERE idempotency_key = :idempotencyKey
                                                                      AND type = :type
                                                                      """;

    private static final String SELECT_PENDING_PAYMENT_OUTBOX_QUERY = """
                                                                      SELECT * 
                                                                      FROM outboxes
                                                                      WHERE (status = 'INIT' OR status = 'FAILURE') 
                                                                      AND created_at <= :createdAt - INTERVAL 1 MINUTE
                                                                      AND type = 'PAYMENT_CONFIRMATION_SUCCESS'
                                                                      """;

    @Override
    public Mono<PaymentEventMessage> insertOutbox(PaymentStatusUpdateCommand command) {

        if (command.getStatus() != PaymentStatus.SUCCESS) throw new IllegalStateException();

        PaymentEventMessage paymentEventMessage = createPaymentEventMessage(command);

        String payload;
        String metadata;
        try {
            payload = ObjectMapperUtil.getObjectMapper().writeValueAsString(paymentEventMessage.getPayload());
            metadata = ObjectMapperUtil.getObjectMapper().writeValueAsString(paymentEventMessage.getMetadata());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return databaseClient.sql(INSERT_OUTBOX_QUERY)
            .bind("idempotencyKey", paymentEventMessage.getPayload().get("orderId"))
            .bind("partitionKey", paymentEventMessage.getMetadata().getOrDefault("partitionKey", 0))
            .bind("type", paymentEventMessage.getType().name())
            .bind("payload", payload)
            .bind("metadata", metadata)
            .fetch()
            .rowsUpdated()
            .thenReturn(paymentEventMessage);
    }

    @Override
    public Mono<Boolean> markMessageAsSent(String idempotencyKey, PaymentEventMessageType type) {
        return databaseClient.sql(UPDATE_OUTBOX_MESSAGE_AS_SENT_QUERY)
            .bind("idempotencyKey", idempotencyKey)
            .bind("type", type.name())
            .fetch()
            .rowsUpdated()
            .thenReturn(true);
    }

    @Override
    public Mono<Boolean> markMessageAsFailure(String idempotencyKey, PaymentEventMessageType type) {
        return databaseClient.sql(UPDATE_OUTBOX_MESSAGE_AS_FAILURE_QUERY)
            .bind("idempotencyKey", idempotencyKey)
            .bind("type", type.name())
            .fetch()
            .rowsUpdated()
            .thenReturn(true);
    }

    @Override
    public Flux<PaymentEventMessage> getPendingPaymentOutboxes() {
        return databaseClient.sql(SELECT_PENDING_PAYMENT_OUTBOX_QUERY)
            .bind("createdAt", LocalDateTime.now().format(MySQLDateTimeFormatter.formatter))
            .fetch()
            .all()
            .handle((row, sink) -> {
                     try {
                         sink.next(PaymentEventMessage.builder()
                                       .type(PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS)
                                       .payload(ObjectMapperUtil.getObjectMapper().readValue((String) row.get("payload"), new TypeReference<>() {}))
                                       .metadata(ObjectMapperUtil.getObjectMapper().readValue((String) row.get("metadata"), new TypeReference<>() {}))
                                       .build());
                     } catch (JsonProcessingException e) {
                         sink.error(new RuntimeException("Failed to deserialize PaymentEventMessage", e));
                     }
                 });
    }

    private PaymentEventMessage createPaymentEventMessage(PaymentStatusUpdateCommand command) {
        return PaymentEventMessage.builder()
            .type(PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS)
            .payload(Map.of("orderId", command.getOrderId()))
            .metadata(Map.of("partitionKey", partitionKeyUtil.createPartitionKey(command.getOrderId().hashCode())))
            .build();
    }
}
