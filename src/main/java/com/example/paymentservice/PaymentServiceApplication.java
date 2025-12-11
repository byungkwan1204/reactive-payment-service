package com.example.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PaymentServiceApplication {

//    @Bean
//    Function<Flux<Message<String>>, Mono<Void>> consume() {
//        return messageFlux -> messageFlux.flatMap(message -> {
//                System.out.println("processing = " + message.getPayload());
//                return Mono.empty();
//            })
//            .then();
//    }

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }

}
