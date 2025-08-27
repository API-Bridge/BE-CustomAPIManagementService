package org.example.customapisvc.event.listener;

import org.example.customapisvc.event.model.CustomApiCalledEvent;
import org.example.customapisvc.service.ApiCallCountSyncService;
import org.example.customapisvc.util.StructuredLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.BDDMockito.*;

/**
 * CustomApiCallEventListener 테스트 클래스
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("커스텀 API 호출 이벤트 리스너 테스트")
class CustomApiCallEventListenerTest {

    @Mock
    private ApiCallCountSyncService apiCallCountSyncService;

    @Mock
    private StructuredLogger structuredLogger;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private CustomApiCallEventListener customApiCallEventListener;

    @Test
    @DisplayName("커스텀 API 호출 이벤트 처리 - 정상 케이스")
    void handleCustomApiCalledEvent_Success() {
        // given
        String customApiId = "test-api-001";
        String userId = "user-123";
        String requestSource = "ai-service";
        
        CustomApiCalledEvent event = new CustomApiCalledEvent(customApiId, userId, requestSource);
        event.setEventId("event-123");
        event.setTraceId("trace-123");
        
        // when
        customApiCallEventListener.handleCustomApiCalledEvent(event, 0, 100L, acknowledgment);
        
        // then
        then(apiCallCountSyncService).should().incrementApiCallCount(customApiId);
        then(acknowledgment).should().acknowledge();
        then(structuredLogger).should().logBusinessEvent(eq("CUSTOM_API_CALLED_EVENT_RECEIVED"), anyString(), any());
        then(structuredLogger).should().logBusinessEvent(eq("CUSTOM_API_CALLED_EVENT_PROCESSED"), anyString(), any());
    }

    @Test
    @DisplayName("커스텀 API 호출 이벤트 처리 - customApiId null")
    void handleCustomApiCalledEvent_NullCustomApiId() {
        // given
        CustomApiCalledEvent event = new CustomApiCalledEvent();
        event.setEventId("event-123");
        event.setTraceId("trace-123");
        event.setPayload(CustomApiCalledEvent.CustomApiCalledPayload.builder()
                .customApiId(null)
                .userId("user-123")
                .requestSource("ai-service")
                .build());
        
        // when
        customApiCallEventListener.handleCustomApiCalledEvent(event, 0, 100L, acknowledgment);
        
        // then
        then(apiCallCountSyncService).should(never()).incrementApiCallCount(any());
        then(acknowledgment).should().acknowledge();
        then(structuredLogger).should().logError(eq("INVALID_CUSTOM_API_CALLED_EVENT"), anyString(), any(), any());
    }

    @Test
    @DisplayName("커스텀 API 호출 이벤트 처리 - customApiId 빈 문자열")
    void handleCustomApiCalledEvent_EmptyCustomApiId() {
        // given
        CustomApiCalledEvent event = new CustomApiCalledEvent();
        event.setEventId("event-123");
        event.setTraceId("trace-123");
        event.setPayload(CustomApiCalledEvent.CustomApiCalledPayload.builder()
                .customApiId("")
                .userId("user-123")
                .requestSource("ai-service")
                .build());
        
        // when
        customApiCallEventListener.handleCustomApiCalledEvent(event, 0, 100L, acknowledgment);
        
        // then
        then(apiCallCountSyncService).should(never()).incrementApiCallCount(any());
        then(acknowledgment).should().acknowledge();
        then(structuredLogger).should().logError(eq("INVALID_CUSTOM_API_CALLED_EVENT"), anyString(), any(), any());
    }

    @Test
    @DisplayName("커스텀 API 호출 이벤트 처리 - 카운트 증가 실패")
    void handleCustomApiCalledEvent_IncrementFailed() {
        // given
        String customApiId = "test-api-001";
        String userId = "user-123";
        String requestSource = "ai-service";
        
        CustomApiCalledEvent event = new CustomApiCalledEvent(customApiId, userId, requestSource);
        event.setEventId("event-123");
        event.setTraceId("trace-123");
        
        RuntimeException exception = new RuntimeException("Redis connection failed");
        doThrow(exception).when(apiCallCountSyncService).incrementApiCallCount(customApiId);
        
        // when
        customApiCallEventListener.handleCustomApiCalledEvent(event, 0, 100L, acknowledgment);
        
        // then
        then(apiCallCountSyncService).should().incrementApiCallCount(customApiId);
        then(acknowledgment).should().acknowledge(); // 에러 발생 시에도 acknowledge
        then(structuredLogger).should().logError(eq("CUSTOM_API_CALLED_EVENT_PROCESSING_ERROR"), anyString(), eq(exception), any());
    }
}