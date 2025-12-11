package com.example.paymentservice.payment.domain;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalEventPublisher;
import reactor.core.publisher.Mono;

@Component
public class PaymentEventMessagePublisher {

    private final TransactionalEventPublisher transactionalEventPublisher;

    public PaymentEventMessagePublisher(ApplicationEventPublisher publisher) {
        this.transactionalEventPublisher = new TransactionalEventPublisher(publisher);
    }

    public Mono<PaymentEventMessage> publishEvent(PaymentEventMessage paymentEventMessage) {

        // publishEvent 함수를 통해서 이벤트를 발행
        // 이 이벤트는 트랜잭션이 성공적으로 커밋될 때까지 이벤트 발행을 지연시킬 수 있다.
        // 따라서 트랜잭션이 롤백되면 이벤트는 발행되지 않는다.
        return transactionalEventPublisher.publishEvent(paymentEventMessage)
            .thenReturn(paymentEventMessage);
    }
}
