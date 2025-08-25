package org.example.customapisvc.event.model;

import org.example.customapisvc.dto.ExternalApiInfoDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CustomApi 이벤트 모델들의 테스트
 */
class CustomApiEventsTest {

    @Test
    void CustomApiCreatedEventTest() {
        // Given
        String customApiId = "custom-api-123";
        String userId = "user-456";
        String name = "Weather API";
        String description = "날씨 정보를 제공하는 API";
        List<ExternalApiInfoDto> externalApiList = null;

        // When
        CustomApiCreatedEvent event = new CustomApiCreatedEvent(customApiId, userId, name, description, externalApiList);

        // Then
        assertEquals("CustomApiCreated", event.getEventType());
        assertEquals("custom-api-svc", event.getServiceName());
        assertNotNull(event.getEventId());
        assertNotNull(event.getTraceId());
        assertNotNull(event.getTimestamp());
        
        // Payload 검증
        CustomApiCreatedEvent.CustomApiCreatedPayload payload = event.getPayload();
        assertNotNull(payload);
        assertEquals(customApiId, payload.getCustomApiId());
        assertEquals(userId, payload.getUserId());
        assertEquals(name, payload.getName());
        assertEquals(description, payload.getDescription());
        assertEquals(externalApiList, payload.getExternalApiList());
    }

    @Test
    void CustomApiCreateFailedEventTest() {
        // Given
        String userId = "user-456";
        String name = "Weather API";
        String failureReason = "VALIDATION_ERROR";
        String errorMessage = "Invalid parameters";
        String failureStage = "AI_GENERATION";

        // When
        CustomApiCreateFailedEvent event = new CustomApiCreateFailedEvent(userId, name, failureReason, errorMessage, failureStage);

        // Then
        assertEquals("CustomApiCreateFailed", event.getEventType());
        assertEquals("custom-api-svc", event.getServiceName());
        assertNotNull(event.getEventId());
        assertNotNull(event.getTraceId());
        assertNotNull(event.getTimestamp());
        
        // Payload 검증
        CustomApiCreateFailedEvent.CustomApiCreateFailedPayload payload = event.getPayload();
        assertNotNull(payload);
        assertEquals(userId, payload.getUserId());
        assertEquals(name, payload.getName());
        assertEquals(failureReason, payload.getFailureReason());
        assertEquals(errorMessage, payload.getErrorMessage());
        assertEquals(failureStage, payload.getFailureStage());
    }

    @Test
    void CustomApiDeletedEventTest() {
        // Given
        String customApiId = "custom-api-123";
        String userId = "user-456";
        String name = "Weather API";
        String description = "날씨 정보를 제공하는 API";
        List<ExternalApiInfoDto> externalApiList = null; // 테스트를 위해 null로 설정
        String deletionReason = "USER_REQUEST";

        // When
        CustomApiDeletedEvent event = new CustomApiDeletedEvent(customApiId, userId, name, description, externalApiList, deletionReason);

        // Then
        assertEquals("CustomApiDeleted", event.getEventType());
        assertEquals("custom-api-svc", event.getServiceName());
        assertNotNull(event.getEventId());
        assertNotNull(event.getTraceId());
        assertNotNull(event.getTimestamp());
        
        // Payload 검증
        CustomApiDeletedEvent.CustomApiDeletedPayload payload = event.getPayload();
        assertNotNull(payload);
        assertEquals(customApiId, payload.getCustomApiId());
        assertEquals(userId, payload.getUserId());
        assertEquals(name, payload.getName());
        assertEquals(description, payload.getDescription());
        assertEquals(externalApiList, payload.getExternalApiList());
        assertEquals(deletionReason, payload.getDeletionReason());
    }

    @Test
    void BaseEventTest() {
        // Given & When
        CustomApiCreatedEvent event1 = new CustomApiCreatedEvent("api1", "user1", "name1", "desc1", null);
        CustomApiCreatedEvent event2 = new CustomApiCreatedEvent("api2", "user2", "name2", "desc2", null);

        // Then
        assertNotEquals(event1.getEventId(), event2.getEventId());
        assertNotEquals(event1.getTraceId(), event2.getTraceId());
        assertTrue(event1.getTimestamp().isBefore(event2.getTimestamp()) || 
                  event1.getTimestamp().isEqual(event2.getTimestamp()));
    }
}