package com.example.paymentservice.payment.adapter.out.web.toss.executor;

import com.example.paymentservice.payment.adapter.out.web.toss.exception.PSPConfirmationException;
import com.example.paymentservice.payment.adapter.out.web.toss.exception.TossPaymentError;
import com.example.paymentservice.payment.adapter.out.web.toss.response.TossPaymentConfirmationResponse;
import com.example.paymentservice.payment.adapter.out.web.toss.response.TossPaymentConfirmationResponse.TossFailureResponse;
import com.example.paymentservice.payment.application.port.in.PaymentConfirmCommand;
import com.example.paymentservice.payment.domain.PSPConfirmationStatus;
import com.example.paymentservice.payment.domain.PaymentExecutionResult;
import com.example.paymentservice.payment.domain.PaymentExecutionResult.PaymentExtraDetails;
import com.example.paymentservice.payment.domain.PaymentMethod;
import com.example.paymentservice.payment.domain.PaymentType;
import io.netty.handler.timeout.TimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * <h4> 실제로 외부에 API 요청을 보내는 역할 </h4>
 */
@Component
@RequiredArgsConstructor
public class TossPaymentExecutor implements PaymentExecutor {

    private final WebClient tossPaymentWebClient;

//    private static final String uri = "/v1/payments/confirm";
    private static final String uri = "/v1/payments/key-in";

    @Override
    public Mono<PaymentExecutionResult> execute(PaymentConfirmCommand command) {
        return tossPaymentWebClient.post()
            .uri(uri)
            .header("Idempotency-Key", command.getOrderId())
            .bodyValue(
                Map.of(
                    "paymentKey", command.getPaymentKey(),
                    "orderId", command.getOrderId(),
                    "amount", command.getAmount()))
            .retrieve()
            .onStatus(
                statusCode -> statusCode.is4xxClientError() || statusCode.is5xxServerError(),
                clientResponse ->
                    clientResponse.bodyToMono(TossFailureResponse.class)
                        .flatMap(response -> {
                            TossPaymentError error = TossPaymentError.get(response.getCode());
                            return Mono.error(
                                new PSPConfirmationException(
                                    error.name(),
                                    error.getDescription(),
                                    error.isSuccess(),
                                    error.isFailure(),
                                    error.isUnknown(),
                                    error.isRetryableError()
                                ));
                        }))
            .bodyToMono(TossPaymentConfirmationResponse.class)
            .map(response ->
                     PaymentExecutionResult.builder()
                         .paymentKey(command.getPaymentKey())
                         .orderId(command.getOrderId())
                         .extraDetails(PaymentExtraDetails.builder()
                                           .type(PaymentType.get(response.getType()))
                                           .method(PaymentMethod.get(response.getMethod()))
                                           .approveAt(LocalDateTime.parse(response.getApprovedAt(), DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                                           .pspRawData(response.toString())
                                           .orderName(response.getOrderName())
                                           .pspConfirmationStatus(PSPConfirmationStatus.get(response.getStatus()))
                                           .totalAmount((long) response.getTotalAmount())
                                           .build())
                         .isSuccess(true)
                         .isFailure(false)
                         .isUnknown(false)
                         .isRetryable(false)
                         .build())
            // BackOff 전략 (N번째 재시도 전, 이전의 2배 시간을 기다림 - 뒤로 물러서기)
            // 최대 재시도 횟수: 2회
            // 최초 지연 시간: 1초 ex) 1회 - 1초, 2회 - 2초, 3회 - 4초 ...
            // jitter: 0.1(10%) 랜덤 대기 시간 부여, (여러 요청이 동시에 실패했을 때 전부 같은 타이밍에 재시도하면 트래픽 폭주 위험)
            // 	- 예를 들어, 1초 지연이라면 0.9초~1.1초 사이에서 랜덤하게 대기 후 재시도.
            .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)).jitter(0.1)
                           .filter(throwable ->
                                       // 필요한 예외에만 재시도 처리를 함으로써 불필요한 재시도를 줄일 수 있다.
                                       (throwable instanceof PSPConfirmationException
                                            && ((PSPConfirmationException) throwable).getIsRetryableError())
                                       || throwable instanceof TimeoutException)
//                           .doBeforeRetry(retrySignal -> {
//
//                               Throwable failure = retrySignal.failure();
//                               if (failure instanceof PSPConfirmationException e) {
//                                   System.out.printf(
//                                       "before retry hook: retryCount: %d, errorCode: %s, isUnknown: %b, isFailure: %b %n",
//                                       retrySignal.totalRetries(), e.getErrorCode(), e.getIsUnknown(), e.getIsFailure());
//                               }
//                           })
                            // 재시도가 모두 소진되었을 경우 예외 발생
                           .onRetryExhaustedThrow(
                               ((retryBackoffSpec, retrySignal) -> retrySignal.failure())));
    }
}
