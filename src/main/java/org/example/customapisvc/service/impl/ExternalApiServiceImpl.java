package org.example.customapisvc.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customapisvc.dto.ExternalApiInfoDto;
import org.example.customapisvc.dto.request.ExternalApiRequestDto;
import org.example.customapisvc.dto.response.ExternalApiResponseDto;
import org.example.customapisvc.dto.response.ExternalApiServiceResponseDto;
import org.example.customapisvc.service.ExternalApiService;
import org.example.customapisvc.util.StructuredLogger;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
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
        additionalFields.put("endpoint", "/api/v1/external-apis/bulk-search");
        
        long startTime = System.currentTimeMillis();
        
        try {
            structuredLogger.logBusinessEvent("EXTERNAL_API_REQUEST_START", 
                "Started external API search request", additionalFields);
                
            // 실제 외부API 관리서비스 응답을 받음
            ExternalApiServiceResponseDto rawResponse = externalApiWebClient
                    .post()
                    .uri("/api/v1/external-apis/bulk-search") // 외부API서비스의 엔드포인트
                    .body(Mono.just(request), ExternalApiRequestDto.class)
                    .retrieve()
                    .bodyToMono(ExternalApiServiceResponseDto.class)
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
     * 외부API 관리서비스 응답을 우리 표준 형식으로 변환
     */
    private ExternalApiResponseDto convertToStandardFormat(ExternalApiServiceResponseDto rawResponse) {
        if (rawResponse == null || rawResponse.getData() == null || rawResponse.getData().getResults() == null) {
            log.warn("외부API 관리서비스에서 빈 응답을 받았습니다.");
            return new ExternalApiResponseDto(new ArrayList<>());
        }

        List<ExternalApiInfoDto> externalApiList = new ArrayList<>();
        
        // 각 카테고리별 API들을 순회하며 변환
        for (Map.Entry<String, List<ExternalApiServiceResponseDto.ApiWithParameters>> entry : 
             rawResponse.getData().getResults().entrySet()) {
            
            String category = entry.getKey();
            List<ExternalApiServiceResponseDto.ApiWithParameters> apiList = entry.getValue();
            
            for (ExternalApiServiceResponseDto.ApiWithParameters apiWithParams : apiList) {
                if (apiWithParams.getApi() != null) {
                    ExternalApiInfoDto convertedApi = convertToExternalApiInfoDto(apiWithParams, category);
                    externalApiList.add(convertedApi);
                }
            }
        }
        
        log.info("외부API 관리서비스 응답을 변환 완료: {} -> {} 개의 API", 
                rawResponse.getData().getTotalCount(), externalApiList.size());
        
        return new ExternalApiResponseDto(externalApiList);
    }

    /**
     * 단일 API 정보를 우리 형식으로 변환
     */
    private ExternalApiInfoDto convertToExternalApiInfoDto(ExternalApiServiceResponseDto.ApiWithParameters apiWithParams, String category) {
        ExternalApiServiceResponseDto.ApiInfo api = apiWithParams.getApi();
        List<ExternalApiServiceResponseDto.ParameterInfo> paramInfos = apiWithParams.getParameters();
        
        // 파라미터 변환
        List<ExternalApiInfoDto.ApiParameter> convertedParams = new ArrayList<>();
        if (paramInfos != null) {
            for (ExternalApiServiceResponseDto.ParameterInfo paramInfo : paramInfos) {
                // paramType 매핑: STRING -> INPUT, 실제 OUTPUT 파라미터는 별도 로직 필요
                String paramType = determineParamType(paramInfo);
                
                ExternalApiInfoDto.ApiParameter convertedParam = new ExternalApiInfoDto.ApiParameter(
                    paramInfo.getParamName(),
                    paramType,
                    paramInfo.getParamDescription(),
                    paramInfo.isRequired()
                );
                convertedParams.add(convertedParam);
            }
        }
        
        return new ExternalApiInfoDto(
            api.getApiId(),
            api.getApiName(),
            convertedParams
        );
    }

    /**
     * 파라미터 타입 결정 로직
     */
    private String determineParamType(ExternalApiServiceResponseDto.ParameterInfo paramInfo) {
        // 현재는 모든 파라미터를 INPUT으로 처리
        // 실제로는 API 스키마 정보를 기반으로 OUTPUT 파라미터도 식별해야 함
        return "INPUT";
    }
}