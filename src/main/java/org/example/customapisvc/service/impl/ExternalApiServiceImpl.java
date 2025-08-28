package org.example.customapisvc.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customapisvc.dto.ExternalApiInfoDto;
import org.example.customapisvc.dto.request.ExternalApiRequestDto;
import org.example.customapisvc.dto.response.ExternalApiResponseDto;
import org.example.customapisvc.dto.response.ExternalApiSpecResponseDto;
import org.example.customapisvc.service.ExternalApiService;
import org.example.customapisvc.util.StructuredLogger;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        additionalFields.put("endpoint", "/api/v1/api/external-api-specs/search-by-name");
        
        long startTime = System.currentTimeMillis();
        
        try {
            structuredLogger.logBusinessEvent("EXTERNAL_API_REQUEST_START",
                "Started external API search request", additionalFields);
                
            // 실제 외부API 관리서비스 응답을 받음
            List<ExternalApiSpecResponseDto> rawResponse = externalApiWebClient
                    .get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/api/v1/api/external-api-specs/search-by-name");
                        
                        // 여러 도메인을 쿼리 파라미터로 추가 (콤마로 구분된 문자열 형태)
                        if (request.getDomains() != null && !request.getDomains().isEmpty()) {
                            String domainsParam = String.join(",", request.getDomains());
                            uriBuilder.queryParam("domains", domainsParam);
                        }
                        
                        // 여러 키워드를 쿼리 파라미터로 추가 (콤마로 구분된 문자열 형태)
                        if (request.getKeywords() != null && !request.getKeywords().isEmpty()) {
                            String keywordsParam = String.join(",", request.getKeywords());
                            uriBuilder.queryParam("keywords", keywordsParam);
                        }
                        
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .bodyToFlux(ExternalApiSpecResponseDto.class)
                    .collectList()
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(1000))
                            .maxBackoff(Duration.ofSeconds(5)))
                    .timeout(Duration.ofSeconds(10))
                    .block();

            // 응답을 우리 형식으로 변환
            ExternalApiResponseDto response = convertToStandardFormat(rawResponse);

            long responseTime = System.currentTimeMillis() - startTime;
            int resultCount = response.getExternalApiList() != null ?
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

    /**
     * 외부API 명세서 서비스 응답을 우리 표준 형식으로 변환
     */
    private ExternalApiResponseDto convertToStandardFormat(List<ExternalApiSpecResponseDto> rawResponse) {
        if (rawResponse == null || rawResponse.isEmpty()) {
            log.warn("외부API 명세서 서비스에서 빈 응답을 받았습니다.");
            return new ExternalApiResponseDto(new ArrayList<>());
        }

        List<ExternalApiInfoDto> externalApiList = new ArrayList<>();
        
        // 각 API 명세서를 순회하며 변환
        for (ExternalApiSpecResponseDto apiSpec : rawResponse) {
            if (apiSpec != null) {
                ExternalApiInfoDto convertedApi = convertToExternalApiInfoDto(apiSpec);
                externalApiList.add(convertedApi);
            }
        }
        
        log.info("외부API 명세서 서비스 응답을 변환 완료: {} 개의 API", externalApiList.size());
        
        return new ExternalApiResponseDto(externalApiList);
    }

    /**
     * 단일 API 명세서 정보를 우리 형식으로 변환
     */
    private ExternalApiInfoDto convertToExternalApiInfoDto(ExternalApiSpecResponseDto apiSpec) {
        // 파라미터 변환
        List<ExternalApiInfoDto.ApiParameter> convertedParams = new ArrayList<>();
        if (apiSpec.getParameters() != null) {
            for (ExternalApiSpecResponseDto.ParameterInfo paramInfo : apiSpec.getParameters()) {
                ExternalApiInfoDto.ApiParameter convertedParam = new ExternalApiInfoDto.ApiParameter(
                    paramInfo.getParameterId(),
                    paramInfo.getParamName(),
                    paramInfo.getParamType(),
                    paramInfo.isRequired(),
                    paramInfo.getParamDescription(),
                    paramInfo.getDefaultValue()
                );
                convertedParams.add(convertedParam);
            }
        }
        
        return new ExternalApiInfoDto(
            apiSpec.getApiId(),
            apiSpec.getApiName(),
            apiSpec.getApiUrl(),
            apiSpec.getHttpMethod(),
            convertedParams
        );
    }
}