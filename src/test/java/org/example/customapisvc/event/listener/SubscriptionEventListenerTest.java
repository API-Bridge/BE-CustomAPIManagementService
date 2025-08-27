package org.example.customapisvc.event.listener;

import org.example.customapisvc.util.StructuredLogger;
import org.example.customapisvc.event.model.UserSubscriptionUpdateEvent;
import org.example.customapisvc.service.CustomApiService;
import org.example.customapisvc.unit.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("구독 이벤트 리스너 테스트")
class SubscriptionEventListenerTest extends BaseUnitTest {

    private SubscriptionEventListener subscriptionEventListener;

    @Mock
    private CustomApiService customApiService;

    @Mock
    private StructuredLogger structuredLogger;

    @Mock
    private Acknowledgment acknowledgment;

    @BeforeEach
    void setUp() {
        subscriptionEventListener = new SubscriptionEventListener(customApiService, structuredLogger);
    }

    @Test
    @DisplayName("플랜 다운그레이드 이벤트 처리 성공")
    void handleUserSubscriptionUpdateEvent_PlanDowngrade_Success() {
        // Given
        String userId = "user123";
        String previousPlan = "PRO";
        String newPlan = "FREE";
        UserSubscriptionUpdateEvent event = new UserSubscriptionUpdateEvent(userId, previousPlan, newPlan, "User requested downgrade");

        // When
        subscriptionEventListener.handleUserSubscriptionUpdateEvent(event, 0, 100L, acknowledgment);

        // Then
        verify(customApiService).handlePlanDowngrade(userId, newPlan);
        verify(acknowledgment).acknowledge();
        verify(structuredLogger, times(3)).logBusinessEvent(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("플랜 업그레이드 이벤트 처리 - 액션 없음")
    void handleUserSubscriptionUpdateEvent_PlanUpgrade_NoAction() {
        // Given
        String userId = "user123";
        String previousPlan = "FREE";
        String newPlan = "PRO";
        UserSubscriptionUpdateEvent event = new UserSubscriptionUpdateEvent(userId, previousPlan, newPlan, "User requested upgrade");

        // When
        subscriptionEventListener.handleUserSubscriptionUpdateEvent(event, 0, 100L, acknowledgment);

        // Then
        verify(customApiService, never()).handlePlanDowngrade(anyString(), anyString());
        verify(acknowledgment).acknowledge();
        verify(structuredLogger, times(3)).logBusinessEvent(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("동일 플랜 이벤트 처리 - 액션 없음")
    void handleUserSubscriptionUpdateEvent_SamePlan_NoAction() {
        // Given
        String userId = "user123";
        String previousPlan = "PRO";
        String newPlan = "PRO";
        UserSubscriptionUpdateEvent event = new UserSubscriptionUpdateEvent(userId, previousPlan, newPlan, "Plan renewal");

        // When
        subscriptionEventListener.handleUserSubscriptionUpdateEvent(event, 0, 100L, acknowledgment);

        // Then
        verify(customApiService, never()).handlePlanDowngrade(anyString(), anyString());
        verify(customApiService, never()).handlePlanUpgrade(anyString(), anyString());
        verify(acknowledgment).acknowledge();
        verify(structuredLogger, times(3)).logBusinessEvent(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("유효하지 않은 사용자 ID 처리")
    void handleUserSubscriptionUpdateEvent_InvalidUserId_ShouldAcknowledge() {
        // Given
        UserSubscriptionUpdateEvent event = new UserSubscriptionUpdateEvent(null, "PRO", "FREE", "Downgrade");

        // When
        subscriptionEventListener.handleUserSubscriptionUpdateEvent(event, 0, 100L, acknowledgment);

        // Then
        verify(customApiService, never()).handlePlanDowngrade(anyString(), anyString());
        verify(acknowledgment).acknowledge();
        verify(structuredLogger).logError(eq("INVALID_USER_SUBSCRIPTION_UPDATE_EVENT"), 
            anyString(), any(IllegalArgumentException.class), any());
    }

    @Test
    @DisplayName("서비스 처리 중 예외 발생 시에도 acknowledge")
    void handleUserSubscriptionUpdateEvent_ServiceException_ShouldAcknowledge() {
        // Given
        String userId = "user123";
        String previousPlan = "PRO";
        String newPlan = "FREE";
        UserSubscriptionUpdateEvent event = new UserSubscriptionUpdateEvent(userId, previousPlan, newPlan, "Downgrade");
        
        doThrow(new RuntimeException("Service error")).when(customApiService).handlePlanDowngrade(userId, newPlan);

        // When
        subscriptionEventListener.handleUserSubscriptionUpdateEvent(event, 0, 100L, acknowledgment);

        // Then
        verify(customApiService).handlePlanDowngrade(userId, newPlan);
        verify(acknowledgment).acknowledge();
        verify(structuredLogger).logError(eq("USER_SUBSCRIPTION_UPDATE_EVENT_PROCESSING_ERROR"), 
            anyString(), any(RuntimeException.class), any());
    }

    @Test
    @DisplayName("FREE에서 PRO로 업그레이드 - 재활성화 처리")
    void handleUserSubscriptionUpdateEvent_FreeToPro_HandleUpgrade() {
        // Given
        String userId = "user123";
        String previousPlan = "FREE";
        String newPlan = "PRO";
        UserSubscriptionUpdateEvent event = new UserSubscriptionUpdateEvent(userId, previousPlan, newPlan, "Upgrade to Pro");

        // When
        subscriptionEventListener.handleUserSubscriptionUpdateEvent(event, 0, 100L, acknowledgment);

        // Then
        verify(customApiService, never()).handlePlanDowngrade(anyString(), anyString());
        verify(customApiService).handlePlanUpgrade(userId, newPlan);
        verify(acknowledgment).acknowledge();
        verify(structuredLogger, times(3)).logBusinessEvent(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("PRO에서 FREE로 다운그레이드 - 처리 필요")
    void handleUserSubscriptionUpdateEvent_ProToFree_ShouldProcess() {
        // Given
        String userId = "user123";
        String previousPlan = "PRO";
        String newPlan = "FREE";
        UserSubscriptionUpdateEvent event = new UserSubscriptionUpdateEvent(userId, previousPlan, newPlan, "Downgrade to Free");

        // When
        subscriptionEventListener.handleUserSubscriptionUpdateEvent(event, 0, 100L, acknowledgment);

        // Then
        verify(customApiService).handlePlanDowngrade(userId, newPlan);
        verify(acknowledgment).acknowledge();
        verify(structuredLogger, times(3)).logBusinessEvent(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("플랜 업그레이드 처리 중 예외 발생")
    void handleUserSubscriptionUpdateEvent_PlanUpgradeException_ShouldAcknowledge() {
        // Given
        String userId = "user123";
        String previousPlan = "FREE";
        String newPlan = "PRO";
        UserSubscriptionUpdateEvent event = new UserSubscriptionUpdateEvent(userId, previousPlan, newPlan, "Upgrade to Pro");

        doThrow(new RuntimeException("Service error")).when(customApiService).handlePlanUpgrade(userId, newPlan);

        // When
        subscriptionEventListener.handleUserSubscriptionUpdateEvent(event, 0, 100L, acknowledgment);

        // Then
        verify(customApiService).handlePlanUpgrade(userId, newPlan);
        verify(acknowledgment).acknowledge();
        verify(structuredLogger).logError(eq("USER_SUBSCRIPTION_UPDATE_EVENT_PROCESSING_ERROR"), 
            anyString(), any(RuntimeException.class), any());
    }

}