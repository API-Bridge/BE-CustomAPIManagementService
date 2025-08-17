package org.example.customapisvc.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * ELK 스택을 위한 구조화된 로깅 유틸리티
 * 
 * 로그 포맷:
 * - Timestamp: ISO 8601 형식
 * - Service Name: custom-api-svc
 * - Request ID/Trace ID: 요청별 고유 식별자
 * - Log Level: INFO, WARN, ERROR, DEBUG
 * - Message: 구체적인 로그 내용
 * - Host Name: 서버 호스트명
 * - IP Address: 서버 IP 주소
 * - Additional Fields: 컨텍스트별 추가 정보
 */
@Slf4j
@Component
public class StructuredLogger {

    private static final String SERVICE_NAME = "custom-api-svc";
    private static final String HOST_NAME = getHostName();
    private static final String IP_ADDRESS = getIpAddress();

    /**
     * 비즈니스 로직 관련 로그 작성
     */
    public void logBusinessEvent(String event, String details, Map<String, Object> additionalFields) {
        Map<String, Object> logData = createBaseLogData();
        logData.put("event_type", "business");
        logData.put("event", event);
        logData.put("details", details);
        if (additionalFields != null) {
            logData.putAll(additionalFields);
        }
        
        log.info("Business Event: {} - {} | Data: {}", event, details, logData);
    }

    /**
     * 시스템 모니터링 관련 로그 작성
     */
    public void logSystemEvent(String component, String status, long responseTime, Map<String, Object> additionalFields) {
        Map<String, Object> logData = createBaseLogData();
        logData.put("event_type", "system");
        logData.put("component", component);
        logData.put("status", status);
        logData.put("response_time_ms", responseTime);
        if (additionalFields != null) {
            logData.putAll(additionalFields);
        }
        
        log.info("System Event: {} - {} ({}ms) | Data: {}", component, status, responseTime, logData);
    }

    /**
     * 사용자 활동 관련 로그 작성
     */
    public void logUserActivity(String userId, String action, String resource, Map<String, Object> additionalFields) {
        Map<String, Object> logData = createBaseLogData();
        logData.put("event_type", "user_activity");
        logData.put("user_id", userId);
        logData.put("action", action);
        logData.put("resource", resource);
        if (additionalFields != null) {
            logData.putAll(additionalFields);
        }
        
        log.info("User Activity: {} performed {} on {} | Data: {}", userId, action, resource, logData);
    }

    /**
     * 보안 관련 로그 작성
     */
    public void logSecurityEvent(String event, String severity, String details, Map<String, Object> additionalFields) {
        Map<String, Object> logData = createBaseLogData();
        logData.put("event_type", "security");
        logData.put("event", event);
        logData.put("severity", severity);
        logData.put("details", details);
        if (additionalFields != null) {
            logData.putAll(additionalFields);
        }
        
        if ("HIGH".equals(severity) || "CRITICAL".equals(severity)) {
            log.error("Security Event: {} - {} | Data: {}", event, details, logData);
        } else {
            log.warn("Security Event: {} - {} | Data: {}", event, details, logData);
        }
    }

    /**
     * 성능 관련 로그 작성
     */
    public void logPerformanceEvent(String operation, long executionTime, String status, Map<String, Object> additionalFields) {
        Map<String, Object> logData = createBaseLogData();
        logData.put("event_type", "performance");
        logData.put("operation", operation);
        logData.put("execution_time_ms", executionTime);
        logData.put("status", status);
        if (additionalFields != null) {
            logData.putAll(additionalFields);
        }
        
        if (executionTime > 5000) { // 5초 이상이면 경고
            log.warn("Performance Issue: {} took {}ms | Data: {}", operation, executionTime, logData);
        } else {
            log.info("Performance: {} completed in {}ms | Data: {}", operation, executionTime, logData);
        }
    }

    /**
     * 에러 로그 작성
     */
    public void logError(String errorType, String message, Throwable throwable, Map<String, Object> additionalFields) {
        Map<String, Object> logData = createBaseLogData();
        logData.put("event_type", "error");
        logData.put("error_type", errorType);
        logData.put("error_message", message);
        if (throwable != null) {
            logData.put("exception_class", throwable.getClass().getSimpleName());
            logData.put("stack_trace", throwable.getMessage());
        }
        if (additionalFields != null) {
            logData.putAll(additionalFields);
        }
        
        if (throwable != null) {
            log.error("Error: {} - {} | Data: {}", errorType, message, logData, throwable);
        } else {
            log.error("Error: {} - {} | Data: {}", errorType, message, logData);
        }
    }

    /**
     * 기본 로그 데이터 생성
     */
    private Map<String, Object> createBaseLogData() {
        Map<String, Object> logData = new HashMap<>();
        
        // 기본 필드들
        logData.put("timestamp", Instant.now().toString());
        logData.put("service_name", SERVICE_NAME);
        logData.put("host_name", HOST_NAME);
        logData.put("ip_address", IP_ADDRESS);
        
        // MDC에서 트레이스 정보 가져오기
        String traceId = MDC.get("traceId");
        String spanId = MDC.get("spanId");

        logData.put("trace_id", Objects.requireNonNullElseGet(traceId, this::generateTraceId));
        
        if (spanId != null) {
            logData.put("span_id", spanId);
        }
        
        return logData;
    }

    /**
     * 새로운 Trace ID 생성
     */
    private String generateTraceId() {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        MDC.put("traceId", traceId);
        return traceId;
    }

    /**
     * 호스트명 가져오기
     */
    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown-host";
        }
    }

    /**
     * IP 주소 가져오기
     */
    private static String getIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "unknown-ip";
        }
    }

    /**
     * MDC에 사용자 정보 설정
     */
    public void setUserContext(String userId) {
        if (userId != null) {
            MDC.put("userId", userId);
        }
    }

    /**
     * MDC에 요청 정보 설정
     */
    public void setRequestContext(String method, String uri, String userAgent, String clientIp) {
        if (method != null) MDC.put("http_method", method);
        if (uri != null) MDC.put("request_uri", uri);
        if (userAgent != null) MDC.put("user_agent", userAgent);
        if (clientIp != null) MDC.put("client_ip", clientIp);
    }

    /**
     * MDC 컨텍스트 정리
     */
    public void clearContext() {
        MDC.clear();
    }
}