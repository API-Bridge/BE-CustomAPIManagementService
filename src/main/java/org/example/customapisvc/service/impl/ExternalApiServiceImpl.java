package org.example.customapisvc.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customapisvc.dto.request.ExternalApiRequestDto;
import org.example.customapisvc.dto.response.ExternalApiResponseDto;
import org.example.customapisvc.service.ExternalApiService;
import org.example.customapisvc.util.StructuredLogger;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 외부API서비스와 통신하는 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiServiceImpl implements ExternalApiService {

    private final WebClient externalApiWebClient;
    private final StructuredLogger structuredLogger;

    @Override
    public ExternalApiResponseDto getExternalApiList(ExternalApiRequestDto request) {
        log.info("외부API서비스에 API 리스트 요청 - domains: {}, keywords: {}", 
                request.getDomains(), request.getKeywords());

        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("domains", request.getDomains());
        additionalFields.put("keywords", request.getKeywords());
        additionalFields.put("external_service", "external-api-service");
        additionalFields.put("endpoint", "/external-apis/search");
        
        long startTime = System.currentTimeMillis();
        
        try {
            structuredLogger.logBusinessEvent("EXTERNAL_API_REQUEST_START", 
                "Started external API search request", additionalFields);
                
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

            long responseTime = System.currentTimeMillis() - startTime;
            int resultCount = response != null && response.getExternalApiList() != null ? 
                             response.getExternalApiList().size() : 0;
            
            additionalFields.put("response_time_ms", responseTime);
            additionalFields.put("result_count", resultCount);
            additionalFields.put("status", "SUCCESS");
            
            structuredLogger.logSystemEvent("external-api-service", "SUCCESS", responseTime, additionalFields);
            structuredLogger.logBusinessEvent("EXTERNAL_API_REQUEST_SUCCESS", 
                "Successfully received " + resultCount + " APIs from external service", additionalFields);

            log.info("외부API서비스로부터 {} 개의 API 수신", resultCount);

            return response;

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            additionalFields.put("response_time_ms", responseTime);
            additionalFields.put("status", "FAILED");
            additionalFields.put("error_message", e.getMessage());
            
            structuredLogger.logSystemEvent("external-api-service", "FAILED", responseTime, additionalFields);
            structuredLogger.logError("EXTERNAL_API_REQUEST_FAILED", 
                "Failed to call external API service", e, additionalFields);
                
            log.error("외부API서비스 호출 실패 - domains: {}, keywords: {}", 
                    request.getDomains(), request.getKeywords(), e);
            throw new RuntimeException("외부API서비스 호출에 실패했습니다: " + e.getMessage());
        }
    }
}