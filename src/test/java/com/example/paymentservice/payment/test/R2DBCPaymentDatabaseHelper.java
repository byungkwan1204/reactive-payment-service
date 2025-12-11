package com.example.paymentservice.payment.test;

import com.example.paymentservice.payment.domain.PaymentEvent;
import com.example.paymentservice.payment.domain.PaymentMethod;
import com.example.paymentservice.payment.domain.PaymentOrder;
import com.example.paymentservice.payment.domain.PaymentStatus;
import com.example.paymentservice.payment.domain.PaymentType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

public class R2DBCPaymentDatabaseHelper implements PaymentDatabaseHelper {

    final DatabaseClient databaseClient;
    final TransactionalOperator transactionalOperator;

    public R2DBCPaymentDatabaseHelper(DatabaseClient databaseClient, TransactionalOperator transactionalOperator) {
        this.databaseClient = databaseClient;
        this.transactionalOperator = transactionalOperator;
    }

    private static final String SELECT_PAYMENT_QUERY = """
                                                       SELECT * FROM payment_events pe 
                                                       INNER JOIN payment_orders po ON pe.order_id = po.order_id
                                                       WHERE pe.order_id = :order_id
                                                       """;

    private static final String DELETE_PAYMENT_EVENT_QUERY = """
                                                             DELETE FROM payment_events
                                                             """;

    private static final String DELETE_PAYMENT_ORDER_QUERY = """
                                                             DELETE FROM payment_orders
                                                             """;

    private static final String DELETE_PAYMENT_ORDER_HISTORY_QUERY = """
                                                                     DELETE FROM payment_order_histories
                                                                     """;

    private static final String DELETE_OUTBOX_QUERY = """
                                                      DELETE FROM outboxes
                                                      """;

    @Override
    public PaymentEvent getPaymentEvent(String orderId) {
        return Mono.from(databaseClient.sql(SELECT_PAYMENT_QUERY)
                             .bind("order_id", orderId)
                             .fetch()
                             .all()
                             .groupBy(row -> (Long) row.get("payment_event_id"))
                             .flatMap(groupedFlux -> groupedFlux.collectList()
                                 .map(results -> {

                                     Map<String, Object> first = results.get(0);

                                     return PaymentEvent.builder()
                                         .id(groupedFlux.key())
                                         .orderId(first.get("order_id").toString())
                                         .orderName(first.get("order_name").toString())
                                         .buyerId((Long) first.get("buyer_id"))
                                         .paymentKey(first.get("payment_key").toString())
                                         .paymentType(first.get("type") != null ? PaymentType.get(first.get("type").toString()) : null)
                                         .paymentMethod(first.get("method") != null ? PaymentMethod.valueOf(first.get("method").toString()) : null)
                                         .approvedAt(first.get("approved_at") != null ? (LocalDateTime) first.get("approved_at") : null)
                                         .isPaymentDone((Boolean) first.get("is_payment_done"))
                                         .paymentOrders(results.stream()
                                                            .map(result -> PaymentOrder.builder()
                                                                .id((Long) result.get("id"))
                                                                .paymentEventId(groupedFlux.key())
                                                                .sellerId((Long) result.get("seller_id"))
                                                                .orderId(result.get("order_id").toString())
                                                                .productId((Long) result.get("product_id"))
                                                                .amount((BigDecimal) result.get("amount"))
                                                                .paymentStatus(PaymentStatus.get(result.get("payment_order_status").toString()))
                                                                .isLedgerUpdated((Boolean) result.get("ledger_updated"))
                                                                .isWalletUpdated((Boolean) result.get("wallet_updated"))
                                                                .build())
                                                            .toList())
                                         .build();
                                 }))).block();
    }

    @Override
    public Mono<Void> clean() {
        return deletePaymentOrderHistories()
            .then(deletePaymentOrders())
            .then(deletePaymentEvents())
            .then(deleteOutboxes())
            .as(transactionalOperator::transactional)
            .then( );
    }

    private Mono<Long> deletePaymentOrderHistories() {
        return databaseClient.sql(DELETE_PAYMENT_ORDER_HISTORY_QUERY)
            .fetch()
            .rowsUpdated();
    }

    private Mono<Long> deletePaymentOrders() {
        return databaseClient.sql(DELETE_PAYMENT_ORDER_QUERY)
            .fetch()
            .rowsUpdated();
    }

    private Mono<Long> deletePaymentEvents() {
        return databaseClient.sql(DELETE_PAYMENT_EVENT_QUERY)
            .fetch()
            .rowsUpdated();
    }

    private Mono<Long> deleteOutboxes() {
        return databaseClient.sql(DELETE_OUTBOX_QUERY)
            .fetch()
            .rowsUpdated();
    }
}
