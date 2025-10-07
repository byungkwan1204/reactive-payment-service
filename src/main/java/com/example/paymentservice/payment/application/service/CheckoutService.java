package com.example.paymentservice.payment.application.service;

import com.example.paymentservice.common.Usecase;
import com.example.paymentservice.payment.application.port.in.CheckoutCommand;
import com.example.paymentservice.payment.application.port.in.CheckoutUsecase;
import com.example.paymentservice.payment.application.port.out.LoadProductPort;
import com.example.paymentservice.payment.application.port.out.SavePaymentPort;
import com.example.paymentservice.payment.domain.CheckoutResult;
import com.example.paymentservice.payment.domain.PaymentEvent;
import com.example.paymentservice.payment.domain.PaymentOrder;
import com.example.paymentservice.payment.domain.PaymentStatus;
import com.example.paymentservice.payment.domain.Product;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Usecase
@RequiredArgsConstructor
public class CheckoutService implements CheckoutUsecase {

    private final LoadProductPort loadProductPort;
    private final SavePaymentPort savePaymentPort;

    @Override
    public Mono<CheckoutResult> checkout(CheckoutCommand command) {
        return loadProductPort.getProducts(command.getCartId(), command.getProductIds())
            .collectList()
            .map(it -> createPaymentEvent(command, it))
            .flatMap(it -> savePaymentPort.save(it).thenReturn(it))
            .map(it ->
                     CheckoutResult.builder()
                         .amount(it.totalAmount())
                         .orderId(it.getOrderId())
                         .orderName(it.getOrderName())
                         .build());
    }

    private PaymentEvent createPaymentEvent(CheckoutCommand command, List<Product> products) {
        return PaymentEvent.builder()
            .buyerId(command.getBuyerId())
            .orderId(command.getIdempotencyKey())
            .orderName(products.stream().map(Product::getName).collect(Collectors.joining(", ")))
            .paymentOrders(products.stream()
                               .map(product ->
                                        PaymentOrder.builder()
                                            .sellerId(product.getSellerId())
                                            .orderId(command.getIdempotencyKey())
                                            .productId(product.getId())
                                            .amount(product.getAmount())
                                            .paymentStatus(PaymentStatus.NOT_STARTED)
                                        .build())
                               .toList())
            .build();
    }
}
