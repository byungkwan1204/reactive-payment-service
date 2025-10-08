package com.example.paymentservice.payment.adapter.out.web.toss.config;

import io.netty.handler.timeout.ReadTimeoutHandler;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * <h4> 토스페이먼츠로 연결을 전달해줄 WebClient를 빈으로 등록 </h4>
 * - WebClient는 Spring WebFlux에서 비동기적인 Http 요청을 생성해서 보낼 수 있는 클라이언트로, 이를 통해 네트워크 통신을 할 수 있다.
 */
@Configuration
public class TossWebClientConfiguration {

    @Value("${PSP.toss.url}") private String baseUrl;
    @Value("${PSP.toss.secretKey}") private String secretKey;

    @Bean
    public WebClient tosPaymentWebClient() {

        /*
         * 토스페이먼츠 API를 호출하기 위해서는 인증정보를 제공해줘야하는데,
         * 이를 위해서 secretKey를 인코딩해서 요청 헤더에 넣어준다.
         */

        String encodeSecretKey = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        return WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodeSecretKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            // clientConnector: webClient는 기본적으로 비동기/논블로킹이지만 커스텀 (타임아웃, 커넥션 풀 등) 하게 사용할 수 있게 해준다.
            .clientConnector(reactorClientHttpConnector())
            // codecs: webClient가 request/response 바디를 직렬화/역직렬할 때 사용할 코덱 설정
            // defaultCodecs: 기본 코덱(Jackson, ByteBuffer, String, FormData)
            .codecs(ClientCodecConfigurer::defaultCodecs)
            .build();
    }

    private ClientHttpConnector reactorClientHttpConnector() {
        ConnectionProvider provider = ConnectionProvider.builder("toss-payment").build();

        HttpClient clientBase = HttpClient.create(provider)
            .doOnConnected(
                connection -> connection.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS)));

        return new ReactorClientHttpConnector(clientBase);
    }
}
