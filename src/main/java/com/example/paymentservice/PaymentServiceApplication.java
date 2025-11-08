package com.example.paymentservice;

import java.util.function.Function;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.EnableScheduling;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@EnableScheduling
@SpringBootApplication
public class PaymentServiceApplication {

    @Bean
    Function<Flux<Message<String>>, Mono<Void>> consume() {
        return messageFlux -> messageFlux.flatMap(message -> {
                System.out.println("processing = " + message.getPayload());
                return Mono.empty();
            })
            .then();
    }

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }

}
