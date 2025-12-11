package com.example.paymentservice.payment.application.port.out;

import com.example.paymentservice.payment.domain.PaymentEventMessage;

public interface DispatchEventMessagePort {

    /*
     * <h4> 메세지 전달 </h4>
     */
    void dispatch(PaymentEventMessage paymentEventMessage);
}
