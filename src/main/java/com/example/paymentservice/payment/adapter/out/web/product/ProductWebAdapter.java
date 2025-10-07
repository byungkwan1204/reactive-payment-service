package com.example.paymentservice.payment.adapter.out.web.product;

import com.example.paymentservice.common.WebAdapter;
import com.example.paymentservice.payment.adapter.out.web.product.client.ProductClient;
import com.example.paymentservice.payment.application.port.out.LoadProductPort;
import com.example.paymentservice.payment.domain.Product;
import java.util.List;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

/**
 * Web 기반 요청을 통해 상품 정보를 가지고오기 때문에 web 패키지에 위치시킨다.
 * 실제 서비스가 MSA로 구성되어있다는 가정하에 고려한 설계이다.
 */
@WebAdapter
@RequiredArgsConstructor
public class ProductWebAdapter implements LoadProductPort {

    private final ProductClient productClient;


    @Override
    public Flux<Product> getProducts(Long cartId, List<Long> productIds) {
        return productClient.getProducts(cartId, productIds);
    }
}
