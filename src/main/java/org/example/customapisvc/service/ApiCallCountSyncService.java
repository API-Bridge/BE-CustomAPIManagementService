package org.example.customapisvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customapisvc.repository.CustomApiRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Redis에 저장된 API 호출 횟수를 데이터베이스와 동기화하는 서비스
 * 
 * 주요 기능:
 * - Redis의 일시적 카운트를 주기적으로 DB에 반영
 * - 실시간 성능과 데이터 정합성을 모두 보장
 * - 일일 초기화 스케줄러에서 최종 동기화 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(StringRedisTemplate.class)
public class ApiCallCountSyncService {

    private static final String API_CALL_COUNT_PREFIX = "api_call_count:";
    
    private final StringRedisTemplate stringRedisTemplate;
    private final CustomApiRepository customApiRepository;

    /**
     * Redis의 호출 카운트를 데이터베이스에 동기화 (5분마다 실행)
     */
    @Scheduled(fixedRate = 300000) // 5분 = 300,000ms
    @Transactional
    public void syncCallCounts() {
        try {
            syncAllCallCounts();
        } catch (Exception e) {
            log.error("API 호출 횟수 동기화 중 오류 발생", e);
        }
    }

    /**
     * 모든 Redis 카운트를 DB에 동기화하고 Redis에서 제거
     * 스케줄러와 일일 리셋에서 공용으로 사용
     */
    @Transactional
    public void syncAllCallCounts() {
        Set<String> keys = stringRedisTemplate.keys(API_CALL_COUNT_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            log.debug("동기화할 API 호출 카운트가 없습니다");
            return;
        }

        int syncedCount = 0;
        int errorCount = 0;

        for (String key : keys) {
            try {
                String customApiId = key.replace(API_CALL_COUNT_PREFIX, "");
                String countStr = stringRedisTemplate.opsForValue().get(key);
                
                if (countStr != null && !countStr.isEmpty()) {
                    Long count = Long.parseLong(countStr);
                    if (count > 0) {
                        int updated = customApiRepository.incrementCallCount(customApiId, count);
                        if (updated > 0) {
                            stringRedisTemplate.delete(key);
                            syncedCount++;
                            log.debug("API {} 호출 횟수 {} 동기화 완료", customApiId, count);
                        } else {
                            log.warn("API {} 호출 횟수 동기화 실패 - 해당 API가 존재하지 않거나 삭제됨", customApiId);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("API 호출 횟수 동기화 중 오류 - Key: {}", key, e);
                errorCount++;
            }
        }

        if (syncedCount > 0 || errorCount > 0) {
            log.info("API 호출 횟수 동기화 완료 - 성공: {}, 오류: {}", syncedCount, errorCount);
        }
    }

    /**
     * Redis에 API 호출 횟수 증가 (API 호출 시점에 사용)
     * 
     * @param customApiId 커스텀 API ID
     */
    public void incrementApiCallCount(String customApiId) {
        log.info("[DEBUG] incrementApiCallCount 호출 - customApiId: {}", customApiId);
        
        try {
            String key = API_CALL_COUNT_PREFIX + customApiId;
            log.info("[DEBUG] Redis key 생성: {}", key);
            
            // 증가 전 현재 값 조회
            String beforeValue = stringRedisTemplate.opsForValue().get(key);
            log.info("[DEBUG] 증가 전 Redis 값: {}", beforeValue);
            
            // 카운트 증가
            Long afterValue = stringRedisTemplate.opsForValue().increment(key);
            log.info("[DEBUG] 증가 후 Redis 값: {}", afterValue);
            
            log.info("API {} 호출 횟수 Redis 증가 성공: {} -> {}", customApiId, beforeValue, afterValue);
        } catch (Exception e) {
            log.error("API {} 호출 횟수 Redis 증가 실패", customApiId, e);
            throw e; // 예외를 다시 던져서 리스너에서 캐치할 수 있도록 함
        }
    }

    /**
     * Redis의 특정 API 호출 횟수 조회
     * 
     * @param customApiId 커스텀 API ID
     * @return Redis의 현재 카운트 (없으면 0)
     */
    public Long getRedisCallCount(String customApiId) {
        try {
            String key = API_CALL_COUNT_PREFIX + customApiId;
            String countStr = stringRedisTemplate.opsForValue().get(key);
            return countStr != null ? Long.parseLong(countStr) : 0L;
        } catch (Exception e) {
            log.error("Redis에서 API {} 호출 횟수 조회 실패", customApiId, e);
            return 0L;
        }
    }
}