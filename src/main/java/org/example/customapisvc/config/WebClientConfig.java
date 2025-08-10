package org.example.customapisvc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient 설정 클래스
 * 외부 마이크로서비스와의 통신을 위한 WebClient 구성
 */
@Configuration
public class WebClientConfig {

    @Value("${external-api-service.base-url}")
    private String externalApiServiceBaseUrl;

    /**
     * 외부API서비스와 통신하기 위한 WebClient Bean
     */
    @Bean
    public WebClient externalApiWebClient() {
        return WebClient.builder()
                .baseUrl(externalApiServiceBaseUrl)
                .build();
    }
}