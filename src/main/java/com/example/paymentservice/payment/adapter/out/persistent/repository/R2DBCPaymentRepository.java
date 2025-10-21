package com.example.paymentservice.payment.adapter.out.persistent.repository;

import com.example.paymentservice.payment.adapter.out.persistent.util.MySQLDateTimeFormatter;
import com.example.paymentservice.payment.domain.PaymentEvent;
import com.example.paymentservice.payment.domain.PaymentStatus;
import com.example.paymentservice.payment.domain.PendingPaymentEvent;
import com.example.paymentservice.payment.domain.PendingPaymentOrder;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * R2DBC
 * - 리액티브 프로그래밍을 지원하여, Spring WebFLux와 같은 리액티브 스택에서 사용될 경우, 더 효율적이고 비동기적인 데이터 처리가 가능하다.
 * 장점: 높은 동시성과 I/O 효율성, 일관된 프로그래밍 모델 유지가 가능하다는 장점이 있다.
 * 단점: 트랜잭션 처리가 JDBC보다 복잡하고 제약이 많고, JPA처럼 복잡한 연관관계, 지연로딩, 캐시 등을 자동으로 지원하지 못하는 경우가 많다.
 *
 */
@Repository
@RequiredArgsConstructor
public class R2DBCPaymentRepository implements PaymentRepository {

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;

    private static final String INSERT_PAYMENT_EVENT_QUERY = """
    INSERT INTO payment_events (buyer_id, order_name, order_id) VALUES(:buyerId, :orderName, :orderId)
    """;

    private static final String LAST_INSERT_ID_QUERY = """
    SELECT LAST_INSERT_ID()
    """;

    private static final String INSERT_PAYMENT_ORDER_QUERY = """
    INSERT INTO payment_orders
    (payment_event_id, seller_id, order_id, product_id, amount, payment_order_status) VALUES %s
    """;

    private static final String SELECT_PENDING_PAYMENT_QUERY = """
                                                                SELECT pe.id as payment_event_id, pe.payment_key, pe.order_id, po.id as payment_order_id, po.payment_order_status, po.amount, po.failed_count, po.threshold
                                                                FROM payment_events pe 
                                                                INNER JOIN payment_orders po ON po.payment_event_id = pe.id
                                                                WHERE (po.payment_order_status = 'UNKNOWN' OR po.payment_order_status = 'EXECUTING' AND po.updated_at <= :updatedAt - INTERVAL 3 MINUTE)
                                                                AND po.failed_count < po.threshold
                                                                LIMIT 10
                                                                """;

    @Override
    public Mono<Void> save(PaymentEvent paymentEvent) {
        return insertPaymentEvent(paymentEvent)
            .flatMap(it -> selectPaymentEventId())
            .flatMap(paymentEventId -> insertPaymentOrders(paymentEvent, paymentEventId))
            .as(transactionalOperator::transactional)   // 해당되는 쿼리들을 하나의 트랜잭션으로 묶는다.
            .then();
    }

    @Override
    public Flux<PendingPaymentEvent> getPendingPayments() {
        return databaseClient.sql(SELECT_PENDING_PAYMENT_QUERY)
            .bind("updatedAt", LocalDateTime.now().format(MySQLDateTimeFormatter.formatter))
            .fetch()
            .all()
            .groupBy(row -> (Long) row.get("payment_event_id"))
            .flatMap(groupedFlex ->
                groupedFlex.collectList().map(results -> {

                    Map<String, Object> first = results.get(0);

                    return PendingPaymentEvent.builder()
                        .paymentEventId((Long) groupedFlex.key())
                        .paymentKey(first.get("payment_key").toString())
                        .orderId(first.get("order_id").toString())
                        .pendingPaymentOrders(
                            results.stream()
                                .map(r ->
                                         PendingPaymentOrder.builder()
                                             .paymentOrderId((Long) r.get("payment_order_id"))
                                             .status(PaymentStatus.get((String) r.get("payment_order_status")))
                                             .amount(((BigDecimal) r.get("amount")).longValue())
                                             .failedCount((Byte) r.get("failed_count"))
                                             .threshold((Byte) r.get("threshold"))
                                             .build())
                                .toList())
                        .build();
                }));
    }

    private Mono<Long> insertPaymentEvent(PaymentEvent paymentEvent) {
        return databaseClient.sql(INSERT_PAYMENT_EVENT_QUERY)
            .bind("buyerId", paymentEvent.getBuyerId())
            .bind("orderName", paymentEvent.getOrderName())
            .bind("orderId", paymentEvent.getOrderId())
            .fetch()
            .rowsUpdated();
    }

    private Mono<Long> selectPaymentEventId() {
        return databaseClient.sql(LAST_INSERT_ID_QUERY)
            .fetch()
            .first()
            .map(row -> ((BigInteger) row.get("LAST_INSERT_ID()")).longValue());
    }

    private Mono<Long> insertPaymentOrders(PaymentEvent paymentEvent, Long paymentEventId) {

        // 1️⃣ values (...) (...) (...) 문자열 만들기
        String valueClauses = paymentEvent.getPaymentOrders().stream()
            .map(o ->
                     String.format("(%d, %d, '%s', %d, %s, '%s')", paymentEventId, o.getSellerId(), o.getOrderId(), o.getProductId(), o.getAmount(), o.getPaymentStatus().name()))
            .collect(Collectors.joining(", "));

        // 2️⃣ 완성된 SQL
        String sql = String.format(INSERT_PAYMENT_ORDER_QUERY, valueClauses);

        // 3️⃣ 실행
        return databaseClient.sql(sql)
            .fetch()
            .rowsUpdated();
    }
}
