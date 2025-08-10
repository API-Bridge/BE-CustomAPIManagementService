package org.example.customapisvc.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customapisvc.dto.ExternalApiInfoDto;
import org.example.customapisvc.dto.request.ExternalApiRequestDto;
import org.example.customapisvc.dto.response.ExternalApiResponseDto;
import org.example.customapisvc.service.ExternalApiService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 외부API서비스와 통신하는 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiServiceImpl implements ExternalApiService {

    private final WebClient externalApiWebClient;

    @Override
    public ExternalApiResponseDto getExternalApiList(ExternalApiRequestDto request) {
        log.info("외부API서비스에 API 리스트 요청 - domains: {}, keywords: {}", 
                request.getDomains(), request.getKeywords());

        try {
            ExternalApiResponseDto response = externalApiWebClient
                    .post()
                    .uri("/external-apis/search") // 외부API서비스의 엔드포인트
                    .body(Mono.just(request), ExternalApiRequestDto.class)
                    .retrieve()
                    .bodyToMono(ExternalApiResponseDto.class)
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(1000))
                            .maxBackoff(Duration.ofSeconds(5)))
                    .timeout(Duration.ofSeconds(10))
                    .block();

            log.info("외부API서비스로부터 {} 개의 API 수신", 
                    response != null && response.getExternalApiList() != null ? 
                    response.getExternalApiList().size() : 0);

            return response;

        } catch (Exception e) {
            log.error("외부API서비스 호출 실패 - domains: {}, keywords: {}", 
                    request.getDomains(), request.getKeywords(), e);
            throw new RuntimeException("외부API서비스 호출에 실패했습니다: " + e.getMessage());
        }
    }
}