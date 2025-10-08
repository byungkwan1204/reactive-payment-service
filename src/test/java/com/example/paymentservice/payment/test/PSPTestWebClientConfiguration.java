package com.example.paymentservice.payment.test;

import java.util.Base64;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@TestConfiguration
public class PSPTestWebClientConfiguration {

    @Value("${PSP.toss.url}")
    String baseUrl;

    @Value("${PSP.toss.secretKey}")
    String secretKey;

    public WebClient createTestTossWebClient(List<Pair<String, String>> customHeaderKeyValue) {

        String encodedSecretKey = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());

        return WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedSecretKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeaders(httpHeaders -> {
                for (Pair<String, String> customHeader : customHeaderKeyValue) {
                    httpHeaders.add(customHeader.getLeft(), customHeader.getRight());
                }
            })
            .clientConnector(reactorClientHttpConnector())
            .build();
    }

    private ClientHttpConnector reactorClientHttpConnector() {
        return new ReactorClientHttpConnector(HttpClient.create(ConnectionProvider.builder("test-toss-payment").build()));
    }
}
