package org.example.customapisvc.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customapisvc.util.StructuredLogger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis 연결 상태 모니터링 컴포넌트
 * ELK 스택으로 캐시 시스템 상태 로깅
 * Redis가 설정된 경우에만 활성화됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisHealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;
    private final StructuredLogger structuredLogger;

    public void checkHealth() {
        long startTime = System.currentTimeMillis();
        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("component", "redis");
        additionalFields.put("type", "cache");
        
        try {
            RedisConnection connection = redisConnectionFactory.getConnection();
            String pong = connection.ping();
            connection.close();
            
            long responseTime = System.currentTimeMillis() - startTime;
            additionalFields.put("response_time_ms", responseTime);
            additionalFields.put("ping_response", pong);
            
            if (responseTime > 500) {
                structuredLogger.logSystemEvent("redis", "SLOW_RESPONSE", responseTime, additionalFields);
                log.warn("Redis response is slow: {}ms", responseTime);
            } else {
                structuredLogger.logSystemEvent("redis", "HEALTHY", responseTime, additionalFields);
                log.debug("Redis connection successful: {}ms, ping: {}", responseTime, pong);
            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            additionalFields.put("response_time_ms", responseTime);
            additionalFields.put("error_message", e.getMessage());
            
            structuredLogger.logSystemEvent("redis", "CONNECTION_FAILED", responseTime, additionalFields);
            structuredLogger.logError("REDIS_CONNECTION_ERROR", 
                "Failed to connect to Redis", e, additionalFields);
                
            log.error("Redis connection failed after {}ms: {}", responseTime, e.getMessage());
        }
    }
}