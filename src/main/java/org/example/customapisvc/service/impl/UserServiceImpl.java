package org.example.customapisvc.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customapisvc.dto.response.user.UserInfoResponseDto;
import org.example.customapisvc.service.UserService;
import org.example.customapisvc.util.StructuredLogger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * User 서비스와 통신하는 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "user-service.mock.enabled", havingValue = "false", matchIfMissing = true)
public class UserServiceImpl implements UserService {

    private final WebClient userServiceWebClient;
    private final StructuredLogger structuredLogger;

    @Override
    public UserInfoResponseDto getUserInfo(String userId) {
        log.info("User 서비스에 사용자 정보 요청 - userId: {}", userId);

        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("user_id", userId);
        additionalFields.put("external_service", "user-service");
        additionalFields.put("endpoint", "/users/{userId}");
        
        long startTime = System.currentTimeMillis();
        
        try {
            structuredLogger.logBusinessEvent("USER_INFO_REQUEST_START", 
                "Started user info request", additionalFields);
                
            UserInfoResponseDto response = userServiceWebClient
                    .get()
                    .uri("/users/{userId}", userId)
                    .retrieve()
                    .bodyToMono(UserInfoResponseDto.class)
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(1000))
                            .maxBackoff(Duration.ofSeconds(5)))
                    .timeout(Duration.ofSeconds(10))
                    .block();

            long responseTime = System.currentTimeMillis() - startTime;
            
            additionalFields.put("response_time_ms", responseTime);
            additionalFields.put("status", "SUCCESS");
            additionalFields.put("user_plan", response != null ? response.getPlan() : "UNKNOWN");
            additionalFields.put("user_active", response != null ? response.getIsActive() : false);
            
            structuredLogger.logSystemEvent("user-service", "SUCCESS", responseTime, additionalFields);
            structuredLogger.logBusinessEvent("USER_INFO_REQUEST_SUCCESS", 
                "Successfully received user info from User service", additionalFields);

            log.info("User 서비스로부터 사용자 정보 수신 - userId: {}, plan: {}", 
                userId, response != null ? response.getPlan() : "UNKNOWN");

            if (response == null) {
                throw new RuntimeException("User 서비스에서 사용자 정보를 찾을 수 없습니다");
            }

            if (!Boolean.TRUE.equals(response.getIsActive())) {
                throw new RuntimeException("비활성화된 사용자입니다");
            }

            return response;

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            additionalFields.put("response_time_ms", responseTime);
            additionalFields.put("status", "FAILED");
            additionalFields.put("error_message", e.getMessage());
            
            structuredLogger.logSystemEvent("user-service", "FAILED", responseTime, additionalFields);
            structuredLogger.logError("USER_INFO_REQUEST_FAILED", 
                "Failed to call User service", e, additionalFields);
                
            log.error("User 서비스 호출 실패 - userId: {}", userId, e);
            throw new RuntimeException("User 서비스 호출에 실패했습니다: " + e.getMessage());
        }
    }
}