package org.example.customapisvc.event.listener;

import org.example.customapisvc.event.model.ExternalApiDeletedEvent;
import org.example.customapisvc.service.CustomApiService;
import org.example.customapisvc.util.StructuredLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ExternalApiEventListener 테스트
 * 외부 API 삭제 이벤트 처리 로직을 검증
 */
@SpringBootTest
@ActiveProfiles("test")
class ExternalApiEventListenerTest {

    @Mock
    private CustomApiService customApiService;

    @Mock
    private StructuredLogger structuredLogger;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private ExternalApiEventListener externalApiEventListener;

    private ExternalApiDeletedEvent testEvent;
    private final String testExternalApiId = "external-api-123";
    private final String testExternalApiName = "Test Weather API";
    private final String testExternalApiUrl = "https://api.weather.test/v1";
    private final String testDeletionReason = "API deprecated";

    @BeforeEach
    void setUp() {
        testEvent = new ExternalApiDeletedEvent(testExternalApiId, testExternalApiName, testExternalApiUrl, testDeletionReason);
    }

    @Test
    void 외부API삭제이벤트_정상처리_성공() {
        // Given
        int expectedDeactivatedCount = 3;
        when(customApiService.deactivateCustomApisByExternalApiId(testExternalApiId))
                .thenReturn(expectedDeactivatedCount);

        // When
        externalApiEventListener.handleExternalApiDeletedEvent(testEvent, 0, 100L, acknowledgment);

        // Then
        verify(customApiService).deactivateCustomApisByExternalApiId(testExternalApiId);
        verify(structuredLogger).logBusinessEvent(eq("EXTERNAL_API_DELETED_EVENT_RECEIVED"), any(), any());
        verify(structuredLogger).logBusinessEvent(eq("EXTERNAL_API_DELETED_EVENT_PROCESSED"), any(), any());
        verify(acknowledgment).acknowledge();
        verifyNoMoreInteractions(structuredLogger);
    }

    @Test
    void 외부API삭제이벤트_유효하지않은ID_스킵() {
        // Given
        ExternalApiDeletedEvent invalidEvent = new ExternalApiDeletedEvent(null, testExternalApiName, testExternalApiUrl, testDeletionReason);

        // When
        externalApiEventListener.handleExternalApiDeletedEvent(invalidEvent, 0, 100L, acknowledgment);

        // Then
        verify(customApiService, never()).deactivateCustomApisByExternalApiId(any());
        verify(structuredLogger).logBusinessEvent(eq("EXTERNAL_API_DELETED_EVENT_RECEIVED"), any(), any());
        verify(structuredLogger).logError(eq("INVALID_EXTERNAL_API_DELETED_EVENT"), any(), any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void 외부API삭제이벤트_빈ID_스킵() {
        // Given
        ExternalApiDeletedEvent invalidEvent = new ExternalApiDeletedEvent("", testExternalApiName, testExternalApiUrl, testDeletionReason);

        // When
        externalApiEventListener.handleExternalApiDeletedEvent(invalidEvent, 0, 100L, acknowledgment);

        // Then
        verify(customApiService, never()).deactivateCustomApisByExternalApiId(any());
        verify(structuredLogger).logBusinessEvent(eq("EXTERNAL_API_DELETED_EVENT_RECEIVED"), any(), any());
        verify(structuredLogger).logError(eq("INVALID_EXTERNAL_API_DELETED_EVENT"), any(), any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void 외부API삭제이벤트_처리중_예외발생() {
        // Given
        RuntimeException exception = new RuntimeException("Database connection failed");
        when(customApiService.deactivateCustomApisByExternalApiId(testExternalApiId))
                .thenThrow(exception);

        // When
        externalApiEventListener.handleExternalApiDeletedEvent(testEvent, 0, 100L, acknowledgment);

        // Then
        verify(customApiService).deactivateCustomApisByExternalApiId(testExternalApiId);
        verify(structuredLogger).logBusinessEvent(eq("EXTERNAL_API_DELETED_EVENT_RECEIVED"), any(), any());
        verify(structuredLogger).logError(eq("EXTERNAL_API_DELETED_EVENT_PROCESSING_ERROR"), any(), eq(exception), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void 외부API삭제이벤트_비활성화대상없음_정상처리() {
        // Given
        int expectedDeactivatedCount = 0;
        when(customApiService.deactivateCustomApisByExternalApiId(testExternalApiId))
                .thenReturn(expectedDeactivatedCount);

        // When
        externalApiEventListener.handleExternalApiDeletedEvent(testEvent, 0, 100L, acknowledgment);

        // Then
        verify(customApiService).deactivateCustomApisByExternalApiId(testExternalApiId);
        verify(structuredLogger).logBusinessEvent(eq("EXTERNAL_API_DELETED_EVENT_RECEIVED"), any(), any());
        verify(structuredLogger).logBusinessEvent(eq("EXTERNAL_API_DELETED_EVENT_PROCESSED"), any(), any());
        verify(acknowledgment).acknowledge();
    }
}