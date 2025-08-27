package org.example.customapisvc.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customapisvc.repository.CustomApiRepository;
import org.example.customapisvc.service.ApiCallCountSyncService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * API 호출 횟수 일일 초기화 스케줄러
 * 
 * 매일 서울 시간(KST) 기준 00:00에 실행되어:
 * 1. Redis의 모든 카운트를 DB에 최종 동기화
 * 2. DB의 모든 callCount를 0으로 초기화
 * 
 * 이를 통해 정확한 일일 API 사용량 측정 기반을 제공합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ApiCallCountResetScheduler {

    private final ApiCallCountSyncService apiCallCountSyncService;
    private final CustomApiRepository customApiRepository;

    /**
     * 매일 자정(KST) API 호출 횟수 초기화
     * cron: "초 분 시 일 월 요일"
     * zone: "Asia/Seoul" - 서울 시간 기준
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void resetDailyApiCallCounts() {
        log.info("=== API 호출 횟수 일일 초기화 시작 ===");
        
        try {
            // 1단계: Redis의 모든 카운트를 DB에 최종 동기화
            log.info("1단계: Redis 카운트 최종 동기화 실행");
            apiCallCountSyncService.syncAllCallCounts();
            
            // 2단계: DB의 모든 API 호출 횟수를 0으로 초기화
            log.info("2단계: 데이터베이스 호출 횟수 초기화 실행");
            int resetCount = customApiRepository.resetAllCallCounts();
            
            log.info("=== API 호출 횟수 일일 초기화 완료 - 초기화된 API 수: {} ===", resetCount);
            
        } catch (Exception e) {
            log.error("API 호출 횟수 일일 초기화 중 오류 발생", e);
            throw e; // 트랜잭션 롤백을 위해 예외 재발생
        }
    }

    /**
     * 테스트 목적으로 수동 초기화 실행
     * 운영 환경에서는 사용하지 않음
     */
    public void manualReset() {
        log.warn("수동 API 호출 횟수 초기화 실행 - 운영 환경에서는 사용 금지");
        resetDailyApiCallCounts();
    }
}