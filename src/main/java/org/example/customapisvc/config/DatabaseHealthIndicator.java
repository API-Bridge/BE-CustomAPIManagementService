package org.example.customapisvc.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customapisvc.util.StructuredLogger;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 데이터베이스 연결 상태 모니터링 컴포넌트
 * ELK 스택으로 시스템 상태 로깅
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseHealthIndicator {

    private final DataSource dataSource;
    private final StructuredLogger structuredLogger;

    public void checkHealth() {
        long startTime = System.currentTimeMillis();
        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("component", "database");
        additionalFields.put("type", "mysql");
        
        try (Connection connection = dataSource.getConnection()) {
            long responseTime = System.currentTimeMillis() - startTime;
            
            additionalFields.put("response_time_ms", responseTime);
            additionalFields.put("connection_valid", connection.isValid(5));
            
            if (responseTime > 1000) {
                structuredLogger.logSystemEvent("database", "SLOW_CONNECTION", responseTime, additionalFields);
                log.warn("Database connection is slow: {}ms", responseTime);
            } else {
                structuredLogger.logSystemEvent("database", "HEALTHY", responseTime, additionalFields);
                log.debug("Database connection successful: {}ms", responseTime);
            }
        } catch (SQLException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            additionalFields.put("response_time_ms", responseTime);
            additionalFields.put("error_message", e.getMessage());
            
            structuredLogger.logSystemEvent("database", "CONNECTION_FAILED", responseTime, additionalFields);
            structuredLogger.logError("DATABASE_CONNECTION_ERROR", 
                "Failed to connect to database", e, additionalFields);
                
            log.error("Database connection failed after {}ms: {}", responseTime, e.getMessage());
        }
    }
}