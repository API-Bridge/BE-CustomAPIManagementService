package org.example.customapisvc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customapisvc.util.StructuredLogger;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 활동 로깅을 위한 HTTP 요청 인터셉터
 * 모든 API 호출에 대한 사용자 활동 추적 및 로깅
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityLoggingInterceptor implements HandlerInterceptor {

    private final StructuredLogger structuredLogger;

    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);
        
        // 사용자 정보 추출
        String userId = extractUserId(request);
        String userAgent = request.getHeader("User-Agent");
        String clientIp = getClientIpAddress(request);
        String method = request.getMethod();
        String uri = request.getRequestURI();
        
        // MDC에 컨텍스트 정보 설정
        structuredLogger.setUserContext(userId);
        structuredLogger.setRequestContext(method, uri, userAgent, clientIp);
        
        // 사용자 활동 로깅
        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("http_method", method);
        additionalFields.put("request_uri", uri);
        additionalFields.put("user_agent", userAgent);
        additionalFields.put("client_ip", clientIp);
        additionalFields.put("session_id", request.getSession(false) != null ? request.getSession().getId() : null);
        
        if (userId != null) {
            structuredLogger.logUserActivity(userId, "API_REQUEST", uri, additionalFields);
        } else {
            // 익명 사용자 요청도 로깅 (보안 모니터링용)
            additionalFields.put("anonymous_request", true);
            structuredLogger.logUserActivity("anonymous", "API_REQUEST", uri, additionalFields);
        }
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, @NotNull HttpServletResponse response,
                                @NotNull Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute("startTime");
        if (startTime != null) {
            long responseTime = System.currentTimeMillis() - startTime;
            
            String userId = extractUserId(request);
            String method = request.getMethod();
            String uri = request.getRequestURI();
            int statusCode = response.getStatus();
            
            Map<String, Object> additionalFields = new HashMap<>();
            additionalFields.put("http_method", method);
            additionalFields.put("request_uri", uri);
            additionalFields.put("status_code", statusCode);
            additionalFields.put("response_time_ms", responseTime);
            additionalFields.put("client_ip", getClientIpAddress(request));
            
            if (ex != null) {
                additionalFields.put("exception", ex.getClass().getSimpleName());
                additionalFields.put("error_message", ex.getMessage());
                
                structuredLogger.logError("API_REQUEST_ERROR", 
                    "API request completed with error", ex, additionalFields);
            } else {
                // 응답 시간에 따른 성능 로깅
                if (responseTime > 5000) {
                    structuredLogger.logPerformanceEvent("API_REQUEST", responseTime, "SLOW", additionalFields);
                } else if (responseTime > 2000) {
                    structuredLogger.logPerformanceEvent("API_REQUEST", responseTime, "MODERATE", additionalFields);
                } else {
                    structuredLogger.logPerformanceEvent("API_REQUEST", responseTime, "FAST", additionalFields);
                }
                
                // 상태 코드별 로깅
                if (statusCode >= 500) {
                    structuredLogger.logError("SERVER_ERROR", 
                        "Server error occurred during API request", null, additionalFields);
                } else if (statusCode >= 400) {
                    structuredLogger.logSecurityEvent("CLIENT_ERROR", "MEDIUM", 
                        "Client error occurred: " + statusCode, additionalFields);
                }
            }
            
            // 사용자별 API 호출 패턴 분석을 위한 로깅
            if (userId != null) {
                logUserApiPattern(userId, method, uri, statusCode, responseTime);
            }
        }
        
        // MDC 정리
        structuredLogger.clearContext();
    }

    /**
     * 요청에서 사용자 ID 추출 (헤더나 파라미터에서)
     */
    private String extractUserId(HttpServletRequest request) {
        // X-User-ID 헤더에서 사용자 ID 추출
        String userId = request.getHeader("X-User-ID");
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }
        
        // userId 파라미터에서 사용자 ID 추출
        userId = request.getParameter("userId");
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }
        
        return null;
    }

    /**
     * 클라이언트 IP 주소 추출 (프록시 고려)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 사용자별 API 호출 패턴 분석을 위한 로깅
     */
    private void logUserApiPattern(String userId, String method, String uri, int statusCode, long responseTime) {
        Map<String, Object> patternFields = new HashMap<>();
        patternFields.put("user_id", userId);
        patternFields.put("api_endpoint", uri);
        patternFields.put("http_method", method);
        patternFields.put("status_code", statusCode);
        patternFields.put("response_time_ms", responseTime);
        patternFields.put("timestamp", System.currentTimeMillis());
        
        // API 사용 빈도 분석을 위한 카테고리 분류
        String apiCategory = categorizeApiEndpoint(uri);
        patternFields.put("api_category", apiCategory);
        
        structuredLogger.logUserActivity(userId, "API_USAGE_PATTERN", apiCategory, patternFields);
    }

    /**
     * API 엔드포인트를 카테고리별로 분류
     */
    private String categorizeApiEndpoint(String uri) {
        if (uri.contains("/custom-apis")) return "CUSTOM_API_MANAGEMENT";
        if (uri.contains("/external-apis")) return "EXTERNAL_API_SEARCH";
        if (uri.contains("/health")) return "HEALTH_CHECK";
        if (uri.contains("/actuator")) return "MONITORING";
        return "OTHER";
    }
}