package com.example.paymentservice.payment.adapter.in.web.view;

import com.example.paymentservice.common.IdempotencyCreator;
import com.example.paymentservice.common.WebAdapter;
import com.example.paymentservice.payment.adapter.in.web.request.CheckoutRequest;
import com.example.paymentservice.payment.application.port.in.CheckoutCommand;
import com.example.paymentservice.payment.application.port.in.CheckoutUsecase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

@WebAdapter
@Controller
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutUsecase checkoutUsecase;

    /**
     * <h4> 결제 위젯 페이지 호출 </h4>
     */
    @GetMapping("/")
    public Mono<String> checkoutPage(CheckoutRequest request, Model model) {

        CheckoutCommand command = CheckoutCommand.builder()
            .cartId(request.getCartId())
            .buyerId(request.getBuyerId())
            .productIds(request.getProductIds())
            .idempotencyKey(IdempotencyCreator.create(request.getSeed()))
            .build();

        return checkoutUsecase.checkout(command)
            .map(it -> {
                model.addAttribute("orderId", it.getOrderId());
                model.addAttribute("orderName", it.getOrderName());
                model.addAttribute("amount", it.getAmount());
                return "checkout";
            });
    }
}
