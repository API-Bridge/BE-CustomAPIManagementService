package org.example.customapisvc.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customapisvc.dto.response.user.UserInfoResponseDto;
import org.example.customapisvc.service.UserService;
import org.example.customapisvc.util.StructuredLogger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * User 서비스를 Mock으로 처리하는 개발용 구현체
 * application-dev.yml에서 user-service.mock.enabled=true일 때 활성화
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "user-service.mock.enabled", havingValue = "true")
public class MockUserServiceImpl implements UserService {

    private final StructuredLogger structuredLogger;

    @Override
    public UserInfoResponseDto getUserInfo(String userId) {
        log.info("Mock User 서비스에서 사용자 정보 반환 - userId: {}", userId);

        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("user_id", userId);
        additionalFields.put("external_service", "user-service-mock");
        additionalFields.put("endpoint", "mock:/api/v1/users/info");
        
        long startTime = System.currentTimeMillis();
        
        try {
            structuredLogger.logBusinessEvent("USER_INFO_REQUEST_START", 
                "Started mock user info request", additionalFields);
            
            // Mock 데이터 생성
            UserInfoResponseDto mockResponse = createMockUserInfo(userId);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            additionalFields.put("response_time_ms", responseTime);
            additionalFields.put("status", "SUCCESS");
            additionalFields.put("user_plan", mockResponse.getPlan());
            additionalFields.put("user_active", mockResponse.getIsActive());
            additionalFields.put("mock_service", true);
            
            structuredLogger.logSystemEvent("user-service-mock", "SUCCESS", responseTime, additionalFields);
            structuredLogger.logBusinessEvent("USER_INFO_REQUEST_SUCCESS", 
                "Successfully returned mock user info", additionalFields);

            log.info("Mock User 서비스에서 사용자 정보 생성 완료 - userId: {}, plan: {}", 
                userId, mockResponse.getPlan());

            return mockResponse;

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            additionalFields.put("response_time_ms", responseTime);
            additionalFields.put("status", "FAILED");
            additionalFields.put("error_message", e.getMessage());
            additionalFields.put("mock_service", true);
            
            structuredLogger.logSystemEvent("user-service-mock", "FAILED", responseTime, additionalFields);
            structuredLogger.logError("USER_INFO_REQUEST_FAILED", 
                "Failed to create mock user info", e, additionalFields);
                
            log.error("Mock User 서비스에서 오류 발생 - userId: {}", userId, e);
            throw new RuntimeException("Mock User 서비스에서 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * Mock 사용자 정보 생성
     */
    private UserInfoResponseDto createMockUserInfo(String userId) {
        UserInfoResponseDto mockUser = new UserInfoResponseDto();
        mockUser.setUserId(userId);
        mockUser.setEmail("mock-user@example.com");
        mockUser.setName("Mock 사용자");
        
        // userId 기반으로 다른 플랜 할당
        if (userId.contains("pro") || userId.contains("premium")) {
            mockUser.setPlan("PRO");
        } else if (userId.contains("basic")) {
            mockUser.setPlan("BASIC");
        } else {
            mockUser.setPlan("FREE");
        }
        
        mockUser.setIsActive(true);
        
        String now = Instant.now().toString();
        mockUser.setCreatedAt(now);
        mockUser.setUpdatedAt(now);
        
        return mockUser;
    }
}