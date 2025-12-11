package com.example.paymentservice.payment.application.port.in;

/**
 * <h4> 아직 카프카 토픽으로 전송되지않은 이벤트 메세지들을 가져와서 전송한다. </h4>
 */
public interface PaymentEventMessageRelayUsecase {

    void relay();
}
