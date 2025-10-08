package com.example.paymentservice.payment.adapter.out.web.toss.executor;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.paymentservice.payment.adapter.out.web.toss.exception.PSPConfirmationException;
import com.example.paymentservice.payment.adapter.out.web.toss.exception.TossPaymentError;
import com.example.paymentservice.payment.application.port.in.PaymentConfirmCommand;
import com.example.paymentservice.payment.test.PSPTestWebClientConfiguration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PSPTestWebClientConfiguration.class)
@Tag("TooLongTime")
class TossPaymentExecutorTest {

    @Autowired
    PSPTestWebClientConfiguration pspTestWebClientConfiguration;

    @Test
    void should_handle_correctly_various_TossPaymentError_scenarios() {
        generateErrorScenarios()
            .forEach(errorScenario -> {

                PaymentConfirmCommand command = PaymentConfirmCommand.builder()
                    .paymentKey(UUID.randomUUID().toString())
                    .orderId(UUID.randomUUID().toString())
                    .amount(10000L)
                    .build();

                TossPaymentExecutor paymentExecutor =
                    new TossPaymentExecutor(
                        pspTestWebClientConfiguration.createTestTossWebClient(
                            Collections.singletonList(Pair.of("TossPayments-Test-Code", errorScenario.errorCode))));

                try {
                    paymentExecutor.execute(command).block();
                } catch (PSPConfirmationException e) {
                    assertThat(e.getIsSuccess()).isEqualTo(errorScenario.isSuccess);
                    assertThat(e.getIsFailure()).isEqualTo(errorScenario.isFailure);
                    assertThat(e.getIsUnknown()).isEqualTo(errorScenario.isUnknown);
                }
            });
    }

    private List<ErrorScenarios> generateErrorScenarios() {
        return Arrays.stream(TossPaymentError.values())
            .map(error ->
                    ErrorScenarios.builder()
                        .errorCode(error.name())
                        .isFailure(error.isFailure())
                        .isUnknown(error.isUnknown())
                        .isSuccess(error.isSuccess())
                        .build())
            .toList();

    }

    @Data @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    static class ErrorScenarios {
        private String errorCode;
        private Boolean isFailure;
        private Boolean isUnknown;
        private Boolean isSuccess;
    }
}