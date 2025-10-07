package com.example.paymentservice.payment.adapter.in.web.api;

import com.example.paymentservice.common.WebAdapter;
import com.example.paymentservice.payment.adapter.in.web.request.TossPaymentConfirmRequest;
import com.example.paymentservice.payment.adapter.in.web.response.ApiResponse;
import com.example.paymentservice.payment.application.port.in.PaymentConfirmCommand;
import com.example.paymentservice.payment.application.port.in.PaymentConfirmUsecase;
import com.example.paymentservice.payment.domain.PaymentConfirmationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@WebAdapter
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/toss")
public class TossPaymentController {

    private final PaymentConfirmUsecase paymentConfirmUsecase;

    /**
     * <h4> success.html -> 결제 승인 API 호출 </h4>
     */
    @PostMapping("/confirm")
    public Mono<ResponseEntity<ApiResponse<PaymentConfirmationResult>>> confirm(@RequestBody TossPaymentConfirmRequest request) {

        PaymentConfirmCommand command = PaymentConfirmCommand.builder()
            .paymentKey(request.getPaymentKey())
            .orderId(request.getOrderId())
            .amount(request.getAmount())
            .build();

        return paymentConfirmUsecase.confirm(command)
            .map(result ->
                ResponseEntity.ok(ApiResponse.with(HttpStatus.OK, "", result)));
    }
}
