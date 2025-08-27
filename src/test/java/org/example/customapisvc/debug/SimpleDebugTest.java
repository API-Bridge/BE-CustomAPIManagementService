package org.example.customapisvc.debug;

import org.example.customapisvc.event.model.CustomApiCalledEvent;
import org.example.customapisvc.service.ApiCallCountSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 단순한 디버그 테스트
 * Bean 생성 및 이벤트/서비스 동작 확인
 */
@SpringBootTest(properties = {"scheduling.enabled=false"})
@ActiveProfiles("test")
class SimpleDebugTest {

    @Autowired(required = false)
    private ApiCallCountSyncService apiCallCountSyncService;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void checkBeanCreation() {
        // Bean 생성 여부 확인
        if (apiCallCountSyncService == null) {
            System.out.println("❌ ApiCallCountSyncService Bean이 생성되지 않음");
        } else {
            System.out.println("✅ ApiCallCountSyncService Bean 생성됨");
        }

        if (stringRedisTemplate == null) {
            System.out.println("❌ StringRedisTemplate Bean이 생성되지 않음");
        } else {
            System.out.println("✅ StringRedisTemplate Bean 생성됨");
        }

        // 이벤트 객체 생성 테스트
        try {
            CustomApiCalledEvent event = new CustomApiCalledEvent("test-api", "test-user", "web");
            System.out.println("✅ CustomApiCalledEvent 생성 성공");
            System.out.println("   - Event ID: " + event.getEventId());
            System.out.println("   - Payload: " + event.getTypedPayload());
            System.out.println("   - Custom API ID: " + event.getTypedPayload().getCustomApiId());
        } catch (Exception e) {
            System.out.println("❌ CustomApiCalledEvent 생성 실패: " + e.getMessage());
        }

        // ApiCallCountSyncService 직접 테스트
        if (apiCallCountSyncService != null) {
            try {
                System.out.println("🔍 ApiCallCountSyncService 직접 호출 테스트 시작");
                apiCallCountSyncService.incrementApiCallCount("test-debug-api");
                System.out.println("✅ incrementApiCallCount 호출 성공");

                if (stringRedisTemplate != null) {
                    Long count = apiCallCountSyncService.getRedisCallCount("test-debug-api");
                    System.out.println("✅ getRedisCallCount 결과: " + count);
                    assertThat(count).isEqualTo(1L);
                } else {
                    System.out.println("⚠️ Redis가 없어서 카운트 확인 불가");
                }
            } catch (Exception e) {
                System.out.println("❌ ApiCallCountSyncService 호출 실패: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}