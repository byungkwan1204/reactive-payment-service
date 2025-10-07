package com.example.paymentservice.payment.adapter.in.web.view;

import com.example.paymentservice.common.WebAdapter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

@WebAdapter
@Controller
public class PaymentController {

    /**
     * <h4> 성공 페이지 호출 </h4>
     */
    @GetMapping("/success")
    public Mono<String> successPage() {
        return Mono.just("success");
    }

    /**
     * <h4> 실패 페이지 호출 </h4>
     */
    @GetMapping("/fail")
    public Mono<String> failPage() {
        return Mono.just("fail");
    }
}
