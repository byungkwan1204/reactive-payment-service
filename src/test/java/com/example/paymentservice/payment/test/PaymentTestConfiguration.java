package com.example.paymentservice.payment.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * DB를 조회할 수 있는 Helper 클래스
 */
@TestConfiguration
public class PaymentTestConfiguration {

    @Bean
    public PaymentDatabaseHelper paymentDatabaseHelper(DatabaseClient databaseClient, TransactionalOperator transactionalOperator) {
        return new R2DBCPaymentDatabaseHelper(databaseClient, transactionalOperator);
    }
}
