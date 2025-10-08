package com.example.paymentservice.payment.adapter.out.web.toss.executor;

import com.example.paymentservice.payment.adapter.out.web.toss.response.TossPaymentConfirmationResponse;
import com.example.paymentservice.payment.application.port.in.PaymentConfirmCommand;
import com.example.paymentservice.payment.domain.PSPConfirmationStatus;
import com.example.paymentservice.payment.domain.PaymentExecutionResult;
import com.example.paymentservice.payment.domain.PaymentExecutionResult.PaymentExtraDetails;
import com.example.paymentservice.payment.domain.PaymentMethod;
import com.example.paymentservice.payment.domain.PaymentType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * <h4> 실제로 외부에 API 요청을 보내는 역할 </h4>
 */
@Component
@RequiredArgsConstructor
public class TossPaymentExecutor implements PaymentExecutor {

    private final WebClient tossPaymentWebClient;

    private static final String uri = "/v1/payments/confirm";

    public Mono<String> execute(String paymentKey, String orderId, String amount) {
        return tossPaymentWebClient.post()
            .uri(uri)
            .bodyValue(
                Map.of(
                    "paymentKey", paymentKey,
                    "orderId", orderId,
                    "amount", amount))
            .retrieve()
            .bodyToMono(String.class);
    }

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
                         .build());
    }
}
