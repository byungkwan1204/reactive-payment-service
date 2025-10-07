package com.example.paymentservice.payment.adapter.out.web.product.client;

import com.example.paymentservice.payment.domain.Product;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 결제 기능에 집중하기위해 상품정보를 제공해주는 ProductService를 구현하지않는다.
 * 따라서, 실제 ProductService를 흉내내는 MockProductClient를 사용한다.
 * MockProductClient에서는 상품정보를 제공할 수 있도록 하드코딩 한다.
 */
@Component
public class MockProductClient implements ProductClient {


    @Override
    public Flux<Product> getProducts(Long cartId, List<Long> productIds) {
        return Flux.fromIterable(
            productIds.stream().map(it ->
                new Product(
                    it,
                    new BigDecimal(it * 10000),
                    2,
                    "test_product_" + it,
                    1L)
            ).toList());
    }
}
