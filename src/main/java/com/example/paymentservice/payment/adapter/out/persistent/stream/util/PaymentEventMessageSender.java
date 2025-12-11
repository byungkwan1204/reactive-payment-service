package com.example.paymentservice.payment.adapter.out.persistent.stream.util;

import com.example.paymentservice.common.LoggerUtil;
import com.example.paymentservice.common.StreamAdapter;
import com.example.paymentservice.payment.adapter.out.persistent.repository.PaymentOutboxRepository;
import com.example.paymentservice.payment.application.port.out.DispatchEventMessagePort;
import com.example.paymentservice.payment.domain.PaymentEventMessage;
import com.example.paymentservice.payment.domain.PaymentEventMessage.PaymentEventMessageType;
import jakarta.annotation.PostConstruct;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.sender.SenderResult;

@Slf4j
@StreamAdapter
@Configuration
@RequiredArgsConstructor
public class PaymentEventMessageSender implements DispatchEventMessagePort {

    private final PaymentOutboxRepository paymentOutboxRepository;

    // 특정 타입의 데이터를 동적으로 발행하기 위해서 Sinks를 사용한다.
    // Sinks란, Reactive 프로그래밍에서 데이터 스트림을 동적으로 생성하고, 방출하는 메커니즘을 제공한다.

    // 이 코드가 여기서 어떻게 동작하냐면, sender 라는 Sinks에 데이터를 동적으로 넣어주면 여기서 카프카로 메세지를 발행한다.
    private final Sinks.Many<Message<PaymentEventMessage>> sender = Sinks.many().unicast().onBackpressureBuffer();

    // 메세지 전송에 대한 결과 데이터를 동적으로 생성하고 방출하기 위한 Sinks
    private final Sinks.Many<SenderResult<String>> sendResult = Sinks.many().unicast().onBackpressureBuffer();

    /**
     * <h5> 카프카 토픽에 메세지를 발행할 수 있도록 바인딩 한다. </h5>
     */
    @Bean
    public Supplier<Flux<Message<PaymentEventMessage>>> send() {
        return () ->
            // PaymentEventMessage를 동적으로 발행하는 sender Sinks를 Flux로 변환해주면 메세지가 카프카로 발행된다.
            sender.asFlux()
                // 메세지를 발행하는 Stream 처리에 실패하더라도 Stream 처리가 계속 진행될 수 있도록 한다.
                .onErrorContinue(
                    (error, ignored) ->
                        LoggerUtil.error("sendEventMessage", error.getMessage() != null ? "failed to send eventMessage" : null, error));
    }

    /**
     * <h5> 메세지를 받아 볼 수 있는 채널을 빈으로 등록한다. </h5>
     */
    @Bean("payment-result")
    public FluxMessageChannel sendResultChannel() {
        return new FluxMessageChannel();
    }

    /**
     * <h5> inputChannel을 지정하고 @ServiceActivator를 통해서 메세지 전송 결과를 받아볼 수 있다. </h5>
     */
    @ServiceActivator(inputChannel = "payment-result")
    public void receiveSendResult(SenderResult<String> results) {

        // 메세지 전송 실패
        if (results.exception() != null) {
            LoggerUtil.error("sendEventMessage", results.exception().getMessage() != null ? "receive an exception for event message" : null, results.exception());
        }

        sendResult.emitNext(results, Sinks.EmitFailureHandler.FAIL_FAST);
    }

    /**
     * <h5> SendResult라는 Sinks가 발행하는 데이터를 처리한다. </h5>
     * <p> 카프카로 보낸 전송 결과 데이터를 받아서 성공적으로 발송된 메세지는 성공으로 마킹하고, <br>
     *     전송에 실패한 메시지는 실패로 마킹한다.
     * </p>
     * <li> 초기화 시점에 Stream을 구독해서 Stream 처리를 할 수 있도록 @PostConstruct를 붙여준다. </li>
     * <li> Bulk Head 패턴을 사용한다 </li>
     */
    @PostConstruct
    public void handleSendResult() {
        sendResult.asFlux()
            .flatMap(lt -> {

                // 성공
                if (lt.recordMetadata() != null) {
                    return paymentOutboxRepository.markMessageAsSent(lt.correlationMetadata(), PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS);

                // 실패
                } else {
                    return paymentOutboxRepository.markMessageAsFailure(lt.correlationMetadata(), PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS);
                }
            })
            .onErrorContinue((error, ignored) -> LoggerUtil.error("sendEventMessage", error.getMessage() != null ? "failed to mark the outbox message." : null, error))
            .subscribeOn(Schedulers.newSingle("handle-send-result-event-message"))
            .subscribe();
    }

    /**
     * <h5> PaymentEventMessagePublisher가 발행한 이벤트 메세지를 수신해서 <br> sender Sinks에 전달될 수 있도록 dispatchAfterCommit 함수를 작성한다. </h5>
     * <p> 트랜잭션이 커밋된 이후 발행될 수 있도록 @TransactionalEventListener를 붙여준다. </p>
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatchAfterCommit(PaymentEventMessage paymentEventMessage) {
        dispatch(paymentEventMessage);
    }


    /**
     * <h5> 메세지 릴레이 서비스에서 이벤트 메세지를 전달하기 위한 메서드 </h5>
     */
    @Override
    public void dispatch(PaymentEventMessage paymentEventMessage) {

        // Sinks에서 메세지를 발행할 때 실패하면 재시도 하지않고 바로 실패하도록 만든다.
        // 어차피 Transactional Outbox Pattern을 사용하면 성공할 때까지 스케줄링 되서 재시도하기 떄문에 문제 없다.
        sender.emitNext(createEventMessage(paymentEventMessage), Sinks.EmitFailureHandler.FAIL_FAST);
    }

    private Message<PaymentEventMessage> createEventMessage(PaymentEventMessage paymentEventMessage) {
        // Spring의 MessageBuilder를 통해 이벤트 메세지를 생성한다.
        return MessageBuilder
            // 메세지 본문
            .withPayload(paymentEventMessage)
            // 메세지가 전송되었는지 추적하기 위해서 CORRELATION_ID를 넣어준다.
            .setHeader(IntegrationMessageHeaderAccessor.CORRELATION_ID, paymentEventMessage.getPayload()
                .get("orderId"))
            // 메세지가 카프카 토픽에 파티셔닝 될 수 있도록 파티션 헤더를 추가한다.
            .setHeader(KafkaHeaders.PARTITION, paymentEventMessage.getMetadata()
                .getOrDefault("partitionKey", "0"))
            .build();
    }
}
