package com.example.paymentservice.payment.application.port.in;

import com.example.paymentservice.payment.domain.CheckoutResult;
import reactor.core.publisher.Mono;

/**
 * <h4> Checkout 처리를 담당하는 Usecase </h4>
 */
public interface CheckoutUsecase {

    Mono<CheckoutResult> checkout(CheckoutCommand command);
}
