package com.example.paymentservice.payment.adapter.out.web.product.client;

import com.example.paymentservice.payment.domain.Product;
import java.util.List;
import reactor.core.publisher.Flux;

/**
 * 상품 정보는 web 요청을 통해 가져오기 때문에 client를 생성한다.
 */
public interface ProductClient {

    Flux<Product> getProducts(Long cartId, List<Long> productIds);
}
