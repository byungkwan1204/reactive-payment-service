package com.example.paymentservice.payment.adapter.out.persistent.repository;

import com.example.paymentservice.payment.adapter.out.persistent.exception.PaymentAlreadyProcessedException;
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand;
import com.example.paymentservice.payment.domain.PaymentEventMessagePublisher;
import com.example.paymentservice.payment.domain.PaymentStatus;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;

@Repository
@RequiredArgsConstructor
public class R2DBCPaymentStatusUpdateRepository implements PaymentStatusUpdateRepository {

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentEventMessagePublisher paymentEventMessagePublisher;

    private static final String SELECT_PAYMENT_ORDER_STATUS_QUERY = """
                                                                    SELECT id, payment_order_status FROM payment_orders
                                                                    where order_id = :orderId
                                                                    """;

    private static final String INSERT_PAYMENT_HISTORY_QUERY = """
                                                               INSERT INTO payment_order_histories
                                                               (payment_order_id, previous_status, new_status, reason) VALUES %s
                                                               """;

    private static final String UPDATE_PAYMENT_ORDER_STATUS_QUERY = """
                                                                    UPDATE payment_orders 
                                                                    SET payment_order_status = :status, updated_at = CURRENT_TIMESTAMP
                                                                    WHERE order_id = :orderId
                                                                    """;

    private static final String UPDATE_PAYMENT_KEY_QUERY = """
                                                           UPDATE payment_events
                                                           SET payment_key = :paymentKey
                                                           WHERE order_id = :orderId
                                                           """;

    private static final String UPDATE_PAYMENT_EVENT_EXTRA_DETAILS_QUERY = """
                                                                           UPDATE payment_events
                                                                           SET order_name = :orderName, method = :method, approved_at = :approvedAt, type = :type, updated_at = CURRENT_TIMESTAMP
                                                                           WHERE order_id = :orderId
                                                                           """;

    private static final String INCREMENT_PAYMENT_ORDER_FAILED_COUNT_QUERY = """
                                                                             UPDATE payment_orders
                                                                             SET failed_count = failed_count + 1
                                                                             WHERE order_id = :orderId
                                                                             """;

    @Override
    public Mono<Boolean> updatePaymentStatusToExecuting(String paymentKey, String orderId) {
        return checkPreviousPaymentOrderStatus(orderId)
            .flatMap(it -> insertPaymentHistory(it, PaymentStatus.EXECUTING, "PAYMENT_CONFIRMATION_START"))
            .then(updatePaymentOrderStatus(orderId, PaymentStatus.EXECUTING))
            .then(updatePaymentKey(orderId, paymentKey))
            .as(transactionalOperator::transactional)
            .thenReturn(true)
        ;
    }

    @Override
    public Mono<Boolean> updatePaymentStatus(PaymentStatusUpdateCommand command) {

        switch (command.getStatus()) {
            case SUCCESS -> { return updatePaymentStatusToSuccess(command); }
            case FAILURE -> { return updatePaymentStatusToFailure(command); }
            case UNKNOWN -> { return updatePaymentStatusToUnknown(command); }
            default -> throw new IllegalStateException(
                String.format("결제 상태 (status: %s) 는 올바르지 않은 결제 상태입니다.", command.getStatus()));
        }
    }

    private Mono<List<Pair<Long, String>>> checkPreviousPaymentOrderStatus(String orderId) {

        return selectPaymentOrderStatus(orderId)
            .handle((Pair<Long, String> paymentOrder, SynchronousSink<Pair<Long, String>> sink) -> {

                String status = paymentOrder.getRight();
                PaymentStatus paymentStatus = PaymentStatus.get(status);

                switch (paymentStatus) {
                    case NOT_STARTED, UNKNOWN, EXECUTING ->
                        sink.next(paymentOrder);
                    case SUCCESS ->
                        sink.error(new PaymentAlreadyProcessedException(PaymentStatus.SUCCESS, "이미 처리 성공한 결제 입니다."));
                    case FAILURE ->
                        sink.error(new PaymentAlreadyProcessedException(PaymentStatus.FAILURE, "이미 처리 실패한 결제 입니다."));
                    default ->
                        sink.error(new IllegalArgumentException(String.format("지원하지 않는 결제 상태 입니다: %s", paymentStatus)));
                }
            })
            .collectList();
    }

    private Flux<Pair<Long, String>> selectPaymentOrderStatus(String orderId) {
        return databaseClient.sql(SELECT_PAYMENT_ORDER_STATUS_QUERY)
            .bind("orderId", orderId)
            .fetch()
            .all()
            .map( row -> Pair.of((Long) row.get("id"), (String) row.get("payment_order_status")));
    }

    private Mono<Long> insertPaymentHistory(List<Pair<Long, String>> paymentOrderIdToStatus, PaymentStatus status, String reason) {

        if (CollectionUtils.isEmpty(paymentOrderIdToStatus)) {
            return Mono.just(0L);
        }

        String valueClauses = paymentOrderIdToStatus.stream()
            .map(pair ->
                String.format("(%d, '%s', '%s', '%s')", pair.getLeft(), pair.getRight(), status, reason))
            .collect(Collectors.joining(", "));

        String sql = String.format(INSERT_PAYMENT_HISTORY_QUERY, valueClauses);

        return databaseClient.sql(sql)
            .fetch()
            .rowsUpdated();
    }

    private Mono<Long> updatePaymentKey(String orderId, String paymentKey) {
        return databaseClient.sql(UPDATE_PAYMENT_KEY_QUERY)
            .bind("paymentKey", paymentKey)
            .bind("orderId", orderId)
            .fetch()
            .rowsUpdated();
    }

    private Mono<Long> updatePaymentOrderStatus(String orderId, PaymentStatus status) {
        return databaseClient.sql(UPDATE_PAYMENT_ORDER_STATUS_QUERY)
            .bind("orderId", orderId)
            .bind("status", status)
            .fetch()
            .rowsUpdated();
    }

    private Mono<Boolean> updatePaymentStatusToSuccess(PaymentStatusUpdateCommand command) {
        return selectPaymentOrderStatus(command.getOrderId())
            .collectList()
            .flatMap(it -> insertPaymentHistory(it, command.getStatus(), "PAYMENT_CONFIRMATION_DONE"))
            .then(updatePaymentOrderStatus(command.getOrderId(), command.getStatus()))
            .then(updatePaymentEventExtraDetails(command))
            .then(paymentOutboxRepository.insertOutbox(command))    // 이벤트에 실패한 메시지들을 스케줄링으로 재발행하기 위한 아웃박스 패턴
            .flatMap(paymentEventMessagePublisher::publishEvent)    // 실시간 + 스케줄링 시 DB 부하 감소를 위해 즉시 발행
            .as(transactionalOperator::transactional)
            .thenReturn(true);
    }

    private Mono<Boolean> updatePaymentStatusToFailure(PaymentStatusUpdateCommand command) {
        return selectPaymentOrderStatus(command.getOrderId())
            .collectList()
            .flatMap(it -> insertPaymentHistory(it, command.getStatus(), command.getFailure().toString()))
            .then(updatePaymentOrderStatus(command.getOrderId(), command.getStatus()))
            .as(transactionalOperator::transactional)
            .thenReturn(true);

    }

    private Mono<Boolean> updatePaymentStatusToUnknown(PaymentStatusUpdateCommand command) {
        return selectPaymentOrderStatus(command.getOrderId())
            .collectList()
            .flatMap(it -> insertPaymentHistory(it, command.getStatus(), command.getFailure().toString()))
            .then(updatePaymentOrderStatus(command.getOrderId(), command.getStatus()))
            .then(incrementPaymentOrderFailedCount(command))
            .as(transactionalOperator::transactional)
            .thenReturn(true);
    }

    private Mono<Long> updatePaymentEventExtraDetails(PaymentStatusUpdateCommand command) {
        return databaseClient.sql(UPDATE_PAYMENT_EVENT_EXTRA_DETAILS_QUERY)
            .bind("orderName", command.getExtraDetails().getOrderName())
            .bind("method", command.getExtraDetails().getMethod())
            .bind("approvedAt", command.getExtraDetails().getApproveAt().toString())
            .bind("orderId", command.getOrderId())
            .bind("type", command.getExtraDetails().getType())
            .fetch()
            .rowsUpdated();
    }

    private Mono<Long> incrementPaymentOrderFailedCount(PaymentStatusUpdateCommand command) {
        return databaseClient.sql(INCREMENT_PAYMENT_ORDER_FAILED_COUNT_QUERY)
            .bind("orderId", command.getOrderId())
            .fetch()
            .rowsUpdated();
    }
}
