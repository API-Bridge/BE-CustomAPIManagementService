package org.example.customapisvc.integration;

import org.example.customapisvc.domain.Entity.CustomApi;
import org.example.customapisvc.repository.CustomApiRepository;
import org.example.customapisvc.scheduler.ApiCallCountResetScheduler;
import org.example.customapisvc.service.ApiCallCountSyncService;
import org.example.customapisvc.testdata.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API 호출 횟수 시스템 통합 테스트
 * Redis와 데이터베이스 간의 실제 통합을 테스트
 */
@SpringBootTest(properties = {"scheduling.enabled=false"})
@ActiveProfiles("test")
@Transactional
@DisplayName("API 호출 횟수 시스템 통합 테스트")
class ApiCallCountIntegrationTest {

    @Autowired(required = false)
    private ApiCallCountSyncService apiCallCountSyncService;

    @Autowired(required = false)
    private ApiCallCountResetScheduler resetScheduler;

    @Autowired
    private CustomApiRepository customApiRepository;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    private CustomApi testApi;

    @BeforeEach
    void setUp() {
        // Redis 사용 가능 여부 확인
        if (stringRedisTemplate != null) {
            try {
                // Redis 키 정리
                stringRedisTemplate.delete(stringRedisTemplate.keys("api_call_count:*"));
            } catch (Exception e) {
                // Redis 연결 실패 시 로그만 남기고 테스트 계속
                System.out.println("Redis 연결 실패, 테스트는 DB만으로 진행: " + e.getMessage());
            }
        }

        // 테스트 API 생성
        testApi = TestDataFactory.CustomApiTestData.createCustomApi(
            "integration-test-api", 
            "test-user", 
            "통합 테스트 API", 
            "통합 테스트용 API", 
            false
        );
        testApi.setCallCount(0L);
        customApiRepository.save(testApi);
    }

    @Test
    @DisplayName("전체 플로우 통합 테스트 - Redis 사용 가능한 경우")
    void completeFlow_WithRedis() {
        // Redis 또는 ApiCallCountSyncService가 없으면 테스트 스킵
        if (stringRedisTemplate == null || apiCallCountSyncService == null) {
            return;
        }

        String apiId = testApi.getCustomApiId();

        // 1. Redis에 API 호출 횟수 증가
        apiCallCountSyncService.incrementApiCallCount(apiId);
        apiCallCountSyncService.incrementApiCallCount(apiId);
        apiCallCountSyncService.incrementApiCallCount(apiId);

        // Redis에 값 확인
        Long redisCount = apiCallCountSyncService.getRedisCallCount(apiId);
        assertThat(redisCount).isEqualTo(3L);

        // DB에는 아직 반영 안됨
        Long dbCount = customApiRepository.findCallCountByCustomApiId(apiId);
        assertThat(dbCount).isEqualTo(0L);

        // 2. 동기화 실행
        apiCallCountSyncService.syncAllCallCounts();

        // 동기화 후 DB 확인
        dbCount = customApiRepository.findCallCountByCustomApiId(apiId);
        assertThat(dbCount).isEqualTo(3L);

        // Redis는 초기화됨
        redisCount = apiCallCountSyncService.getRedisCallCount(apiId);
        assertThat(redisCount).isEqualTo(0L);

        // 3. 추가 호출 후 일일 리셋
        apiCallCountSyncService.incrementApiCallCount(apiId);
        apiCallCountSyncService.incrementApiCallCount(apiId);

        // 일일 리셋 실행 (최종 동기화 + DB 리셋)
        if (resetScheduler != null) {
            resetScheduler.manualReset();
        }

        // 최종 상태 확인
        dbCount = customApiRepository.findCallCountByCustomApiId(apiId);
        assertThat(dbCount).isEqualTo(0L);

        redisCount = apiCallCountSyncService.getRedisCallCount(apiId);
        assertThat(redisCount).isEqualTo(0L);
    }

    @Test
    @DisplayName("DB 직접 동기화 테스트")
    void directDatabaseSync() {
        String apiId = testApi.getCustomApiId();

        // DB 호출 횟수 직접 증가
        int updated = customApiRepository.incrementCallCount(apiId, 10L);
        assertThat(updated).isEqualTo(1);

        // 값 확인
        Long dbCount = customApiRepository.findCallCountByCustomApiId(apiId);
        assertThat(dbCount).isEqualTo(10L);

        // 추가 증가
        customApiRepository.incrementCallCount(apiId, 5L);
        dbCount = customApiRepository.findCallCountByCustomApiId(apiId);
        assertThat(dbCount).isEqualTo(15L);

        // 전체 리셋
        int resetCount = customApiRepository.resetAllCallCounts();
        assertThat(resetCount).isGreaterThanOrEqualTo(1);

        dbCount = customApiRepository.findCallCountByCustomApiId(apiId);
        assertThat(dbCount).isEqualTo(0L);
    }

    @Test
    @DisplayName("다수 API 동시 처리 테스트")
    void multipleApiHandling() {
        // 추가 API 생성
        CustomApi api2 = TestDataFactory.CustomApiTestData.createCustomApi(
            "integration-test-api-2", 
            "test-user", 
            "통합 테스트 API 2", 
            "통합 테스트용 API 2", 
            false
        );
        api2.setCallCount(0L);
        customApiRepository.save(api2);

        String apiId1 = testApi.getCustomApiId();
        String apiId2 = api2.getCustomApiId();

        // Redis와 ApiCallCountSyncService 사용 가능한 경우에만 테스트
        if (stringRedisTemplate != null && apiCallCountSyncService != null) {
            // 각 API에 다른 횟수로 호출
            for (int i = 0; i < 3; i++) {
                apiCallCountSyncService.incrementApiCallCount(apiId1);
            }
            for (int i = 0; i < 7; i++) {
                apiCallCountSyncService.incrementApiCallCount(apiId2);
            }

            // 동기화
            apiCallCountSyncService.syncAllCallCounts();

            // 각 API별 확인
            Long count1 = customApiRepository.findCallCountByCustomApiId(apiId1);
            Long count2 = customApiRepository.findCallCountByCustomApiId(apiId2);

            assertThat(count1).isEqualTo(3L);
            assertThat(count2).isEqualTo(7L);
        } else {
            // Redis 없이 DB 직접 테스트
            customApiRepository.incrementCallCount(apiId1, 3L);
            customApiRepository.incrementCallCount(apiId2, 7L);

            Long count1 = customApiRepository.findCallCountByCustomApiId(apiId1);
            Long count2 = customApiRepository.findCallCountByCustomApiId(apiId2);

            assertThat(count1).isEqualTo(3L);
            assertThat(count2).isEqualTo(7L);
        }

        // 전체 리셋
        int resetCount = customApiRepository.resetAllCallCounts();
        assertThat(resetCount).isGreaterThanOrEqualTo(2);

        // 모든 API가 0으로 초기화됨
        Long count1 = customApiRepository.findCallCountByCustomApiId(apiId1);
        Long count2 = customApiRepository.findCallCountByCustomApiId(apiId2);

        assertThat(count1).isEqualTo(0L);
        assertThat(count2).isEqualTo(0L);
    }
}