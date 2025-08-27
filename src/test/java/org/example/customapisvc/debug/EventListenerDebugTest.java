package org.example.customapisvc.debug;

import org.example.customapisvc.event.listener.CustomApiCallEventListener;
import org.example.customapisvc.event.model.CustomApiCalledEvent;
import org.example.customapisvc.service.ApiCallCountSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.mock;

/**
 * 이벤트 리스너 디버그 테스트
 * 페이로드 캐스팅과 Redis 업데이트 문제를 진단하기 위한 테스트
 */
@SpringBootTest(properties = {"scheduling.enabled=false"})
@ActiveProfiles("test")
@DisplayName("이벤트 리스너 디버그 테스트")
class EventListenerDebugTest {

    @Autowired(required = false)
    private CustomApiCallEventListener eventListener;

    @Autowired(required = false)
    private ApiCallCountSyncService apiCallCountSyncService;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("이벤트 리스너 직접 호출 테스트")
    void directEventListenerCall() {
        System.out.println("=== 디버그 테스트 시작 ===");
        System.out.println("eventListener: " + (eventListener != null ? "OK" : "NULL"));
        System.out.println("apiCallCountSyncService: " + (apiCallCountSyncService != null ? "OK" : "NULL"));
        System.out.println("stringRedisTemplate: " + (stringRedisTemplate != null ? "OK" : "NULL"));
        
        // 서비스들이 없으면 테스트 스킵
        if (eventListener == null || apiCallCountSyncService == null) {
            System.out.println("필요한 Bean들이 없어서 테스트 스킵");
            return;
        }

        // CustomApiCalledEvent 생성
        CustomApiCalledEvent event = new CustomApiCalledEvent("test-api-123", "test-user", "web-app");
        System.out.println("생성된 이벤트: " + event);
        System.out.println("이벤트 페이로드: " + event.getPayload());
        System.out.println("타입이 지정된 페이로드: " + event.getTypedPayload());

        // Redis 사전 상태 확인
        if (stringRedisTemplate != null) {
            try {
                Long beforeCount = apiCallCountSyncService.getRedisCallCount("test-api-123");
                System.out.println("Redis 호출 전 카운트: " + beforeCount);
            } catch (Exception e) {
                System.out.println("Redis 연결 실패: " + e.getMessage());
            }
        }

        // 이벤트 리스너 직접 호출
        Acknowledgment mockAck = mock(Acknowledgment.class);
        
        try {
            System.out.println("이벤트 리스너 호출 시작");
            eventListener.handleCustomApiCalledEvent(event, 0, 0L, mockAck);
            System.out.println("이벤트 리스너 호출 완료");
        } catch (Exception e) {
            System.out.println("이벤트 리스너 호출 중 오류: " + e.getMessage());
            e.printStackTrace();
        }

        // Redis 사후 상태 확인
        if (stringRedisTemplate != null && apiCallCountSyncService != null) {
            try {
                Long afterCount = apiCallCountSyncService.getRedisCallCount("test-api-123");
                System.out.println("Redis 호출 후 카운트: " + afterCount);
            } catch (Exception e) {
                System.out.println("Redis 연결 실패: " + e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("ApiCallCountSyncService 직접 호출 테스트")
    void directServiceCall() {
        System.out.println("=== 직접 서비스 호출 테스트 시작 ===");
        System.out.println("apiCallCountSyncService: " + (apiCallCountSyncService != null ? "OK" : "NULL"));
        
        if (apiCallCountSyncService == null) {
            System.out.println("ApiCallCountSyncService가 없어서 테스트 스킵");
            return;
        }

        try {
            System.out.println("ApiCallCountSyncService 직접 호출 시작");
            apiCallCountSyncService.incrementApiCallCount("direct-test-api");
            System.out.println("ApiCallCountSyncService 직접 호출 완료");

            if (stringRedisTemplate != null) {
                Long count = apiCallCountSyncService.getRedisCallCount("direct-test-api");
                System.out.println("직접 호출 후 Redis 카운트: " + count);
            }
        } catch (Exception e) {
            System.out.println("ApiCallCountSyncService 직접 호출 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
}