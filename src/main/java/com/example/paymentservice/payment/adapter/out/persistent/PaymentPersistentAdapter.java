package com.example.paymentservice.payment.adapter.out.persistent;

import com.example.paymentservice.common.PersistentAdapter;
import com.example.paymentservice.payment.adapter.out.persistent.repository.PaymentRepository;
import com.example.paymentservice.payment.adapter.out.persistent.repository.PaymentStatusUpdateRepository;
import com.example.paymentservice.payment.adapter.out.persistent.repository.PaymentValidationRepository;
import com.example.paymentservice.payment.application.port.out.LoadPendingPaymentPort;
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand;
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdatePort;
import com.example.paymentservice.payment.application.port.out.PaymentValidationPort;
import com.example.paymentservice.payment.application.port.out.SavePaymentPort;
import com.example.paymentservice.payment.domain.PaymentEvent;
import com.example.paymentservice.payment.domain.PendingPaymentEvent;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@PersistentAdapter
@RequiredArgsConstructor
public class PaymentPersistentAdapter implements SavePaymentPort, PaymentStatusUpdatePort, PaymentValidationPort, LoadPendingPaymentPort {

    private final PaymentRepository paymentRepository;
    private final PaymentStatusUpdateRepository paymentStatusUpdateRepository;
    private final PaymentValidationRepository paymentValidationRepository;

    @Override
    public Mono<Void> save(PaymentEvent paymentEvent) {
        return paymentRepository.save(paymentEvent);
    }

    @Override
    public Mono<Boolean> updatePaymentStatusToExecuting(String paymentKey, String orderId) {
        return paymentStatusUpdateRepository.updatePaymentStatusToExecuting(paymentKey, orderId);
    }

    @Override
    public Mono<Boolean> isValid(String orderId, Long amount) {
        return paymentValidationRepository.isValid(orderId, amount);
    }

    @Override
    public Mono<Boolean> updatePaymentStatus(PaymentStatusUpdateCommand command) {
        return paymentStatusUpdateRepository.updatePaymentStatus(command);
    }

    @Override
    public Flux<PendingPaymentEvent> getPendingPayments() {
        return paymentRepository.getPendingPayments();
    }
}
