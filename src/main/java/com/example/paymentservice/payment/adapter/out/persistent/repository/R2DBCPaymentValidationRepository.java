package com.example.paymentservice.payment.adapter.out.persistent.repository;

import com.example.paymentservice.payment.adapter.out.persistent.exception.PaymentValidationException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class R2DBCPaymentValidationRepository implements PaymentValidationRepository {

    private final DatabaseClient databaseClient;

    private static final String SELECT_PAYMENT_TOTAL_AMOUNT_QUERY = """
                                                                    SELECT SUM(amount) as total_amount
                                                                    FROM payment_orders
                                                                    WHERE order_id = :orderId
                                                                    """;

    @Override
    public Mono<Boolean> isValid(String orderId, Long amount) {
        return databaseClient.sql(SELECT_PAYMENT_TOTAL_AMOUNT_QUERY)
            .bind("orderId", orderId)
            .fetch()
            .first()
            .handle((row, sink) -> {
                if (((BigDecimal) row.get("total_amount")).longValue() == amount) {
                    sink.next(true);
                } else {
                    sink.error(
                        new PaymentValidationException(
                            String.format("결제 (orderId: %s) 에서 금액 (amount: %d)이 올바르지 않습니다.", orderId, amount)));
                }
            });
    }
}
