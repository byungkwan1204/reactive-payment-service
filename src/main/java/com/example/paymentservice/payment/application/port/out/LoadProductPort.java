package com.example.paymentservice.payment.application.port.out;

import com.example.paymentservice.payment.domain.Product;
import java.util.List;
import reactor.core.publisher.Flux;

/**
 * LoadProductPort는 application 내부 로직이 외부에 접근하기위해 만들어진 인터페이스이다.
 * 따라서, out 패키지에 위치시킨다.
 */
public interface LoadProductPort {

    /**
     * 상품 정보 조회
     * @param cartId
     * @param productIds
     * @return 여러개의 Product들을 가져와야하기 때문에 Flux 사용.
     * - Mono: 0또는 1개의 비동기 결과 (단일값 비동기)
     * - Flux: 0개 이상 다수의 비동기 결과 (다중값 비동기)
     */
    Flux<Product> getProducts(Long cartId, List<Long> productIds);
}
