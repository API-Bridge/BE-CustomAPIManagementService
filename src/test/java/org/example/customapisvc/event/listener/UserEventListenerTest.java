package org.example.customapisvc.event.listener;

import org.example.customapisvc.event.model.UserDeletedEvent;
import org.example.customapisvc.service.CustomApiService;
import org.example.customapisvc.util.StructuredLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * UserEventListener 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UserEventListenerTest {

    @Mock
    private CustomApiService customApiService;
    
    @Mock
    private StructuredLogger structuredLogger;
    
    @Mock
    private Acknowledgment acknowledgment;
    
    @InjectMocks
    private UserEventListener userEventListener;
    
    private UserDeletedEvent userDeletedEvent;
    
    @BeforeEach
    void setUp() {
        userDeletedEvent = new UserDeletedEvent("test-user-123", "사용자 요청에 의한 탈퇴");
    }
    
    @Test
    void handleUserDeletedEvent_shouldProcessSuccessfully() {
        // Given
        int deletedCount = 3;
        when(customApiService.deleteAllCustomApisByUserId("test-user-123")).thenReturn(deletedCount);
        
        // When
        userEventListener.handleUserDeletedEvent(userDeletedEvent, 0, 100L, acknowledgment);
        
        // Then
        verify(customApiService, times(1)).deleteAllCustomApisByUserId("test-user-123");
        verify(structuredLogger, times(1)).logBusinessEvent(eq("USER_DELETED_EVENT_RECEIVED"), anyString(), any());
        verify(structuredLogger, times(1)).logBusinessEvent(eq("USER_DELETED_EVENT_PROCESSED"), anyString(), any());
        verify(acknowledgment, times(1)).acknowledge();
    }
    
    @Test
    void handleUserDeletedEvent_shouldSkipWhenUserIdIsNull() {
        // Given
        UserDeletedEvent invalidEvent = new UserDeletedEvent(null, "테스트 삭제 사유");
        
        // When
        userEventListener.handleUserDeletedEvent(invalidEvent, 0, 100L, acknowledgment);
        
        // Then
        verify(customApiService, never()).deleteAllCustomApisByUserId(anyString());
        verify(structuredLogger, times(1)).logError(eq("INVALID_USER_DELETED_EVENT"), anyString(), any(), any());
        verify(acknowledgment, times(1)).acknowledge();
    }
    
    @Test
    void handleUserDeletedEvent_shouldSkipWhenUserIdIsBlank() {
        // Given
        UserDeletedEvent invalidEvent = new UserDeletedEvent("   ", "테스트 삭제 사유");
        
        // When
        userEventListener.handleUserDeletedEvent(invalidEvent, 0, 100L, acknowledgment);
        
        // Then
        verify(customApiService, never()).deleteAllCustomApisByUserId(anyString());
        verify(structuredLogger, times(1)).logError(eq("INVALID_USER_DELETED_EVENT"), anyString(), any(), any());
        verify(acknowledgment, times(1)).acknowledge();
    }
    
    @Test
    void handleUserDeletedEvent_shouldHandleServiceException() {
        // Given
        RuntimeException exception = new RuntimeException("Database connection failed");
        when(customApiService.deleteAllCustomApisByUserId("test-user-123")).thenThrow(exception);
        
        // When
        userEventListener.handleUserDeletedEvent(userDeletedEvent, 0, 100L, acknowledgment);
        
        // Then
        verify(customApiService, times(1)).deleteAllCustomApisByUserId("test-user-123");
        verify(structuredLogger, times(1)).logBusinessEvent(eq("USER_DELETED_EVENT_RECEIVED"), anyString(), any());
        verify(structuredLogger, times(1)).logError(eq("USER_DELETED_EVENT_PROCESSING_ERROR"), anyString(), eq(exception), any());
        verify(acknowledgment, times(1)).acknowledge(); // Acknowledge even on error to prevent reprocessing
    }
    
    @Test
    void handleUserDeletedEvent_shouldHandleWhenNoCustomApisToDelete() {
        // Given
        when(customApiService.deleteAllCustomApisByUserId("test-user-123")).thenReturn(0);
        
        // When
        userEventListener.handleUserDeletedEvent(userDeletedEvent, 0, 100L, acknowledgment);
        
        // Then
        verify(customApiService, times(1)).deleteAllCustomApisByUserId("test-user-123");
        verify(structuredLogger, times(1)).logBusinessEvent(eq("USER_DELETED_EVENT_RECEIVED"), anyString(), any());
        verify(structuredLogger, times(1)).logBusinessEvent(eq("USER_DELETED_EVENT_PROCESSED"), anyString(), any());
        verify(acknowledgment, times(1)).acknowledge();
    }
}