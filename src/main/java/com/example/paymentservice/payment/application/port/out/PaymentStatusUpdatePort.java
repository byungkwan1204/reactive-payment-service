package com.example.paymentservice.payment.application.port.out;

import reactor.core.publisher.Mono;

/**
 * 결제 상태를 변경하는 인터페이스
 */
public interface PaymentStatusUpdatePort {

    /**
     * <h4> NOT_STARTED -> EXECUTING 으로 변경 </h4>
     * <li> EXECUTING으로 변경함으로써 추후 복구가 가능해진다. </li>
     * @param paymentKey
     * @param orderId
     * @return
     */
    Mono<Boolean> updatePaymentStatusToExecuting(String paymentKey, String orderId);

    /**
     * 결제 상태 업데이트
     * @param command
     * @return
     */
    Mono<Boolean> updatePaymentStatus(PaymentStatusUpdateCommand command);
}
