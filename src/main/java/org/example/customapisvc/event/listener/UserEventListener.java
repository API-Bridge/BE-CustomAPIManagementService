package org.example.customapisvc.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customapisvc.event.model.UserDeletedEvent;
import org.example.customapisvc.service.CustomApiService;
import org.example.customapisvc.util.StructuredLogger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 관련 이벤트 리스너
 * 다른 마이크로서비스에서 발행되는 사용자 관련 이벤트를 수신하여 처리
 * 
 * 주요 기능:
 * - 사용자 삭제 이벤트 수신 및 처리
 * - 해당 사용자의 모든 커스텀 API 삭제 처리
 * - 이벤트 처리 결과 로깅 및 모니터링
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!dev")  // dev 프로필이 아닐 때만 활성화 (실제 Kafka 사용 시)
public class UserEventListener {

    private final CustomApiService customApiService;
    private final StructuredLogger structuredLogger;

    /**
     * 사용자 삭제 이벤트 리스너
     * user-events 토픽에서 USER_DELETED 이벤트를 수신하여 처리
     * 
     * @param userDeletedEvent 사용자 삭제 이벤트 객체
     * @param partition Kafka 파티션 정보
     * @param offset Kafka 오프셋 정보
     * @param acknowledgment 수동 커밋을 위한 Acknowledgment
     */
    @KafkaListener(
            topics = "user-events",
            groupId = "custom-api-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserDeletedEvent(
            @Payload UserDeletedEvent userDeletedEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        log.info("사용자 삭제 이벤트 수신 - eventId: {}, userId: {}, partition: {}, offset: {}", 
                userDeletedEvent.getEventId(), userDeletedEvent.getUserId(), partition, offset);
        
        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("event_id", userDeletedEvent.getEventId());
        additionalFields.put("user_id", userDeletedEvent.getUserId());
        additionalFields.put("event_type", userDeletedEvent.getEventType());
        additionalFields.put("source_service", userDeletedEvent.getSourceService());
        additionalFields.put("correlation_id", userDeletedEvent.getCorrelationId());
        additionalFields.put("partition", partition);
        additionalFields.put("offset", offset);
        additionalFields.put("deletion_reason", userDeletedEvent.getDeletionReason());
        
        try {
            structuredLogger.logBusinessEvent("USER_DELETED_EVENT_RECEIVED", 
                "Received user deletion event from Kafka", additionalFields);
            
            // 사용자 ID 유효성 검증
            if (userDeletedEvent.getUserId() == null || userDeletedEvent.getUserId().trim().isEmpty()) {
                structuredLogger.logError("INVALID_USER_DELETED_EVENT", 
                    "User ID is null or empty in user deletion event", new IllegalArgumentException("Invalid userId"), additionalFields);
                log.error("유효하지 않은 사용자 삭제 이벤트: userId가 null 또는 빈 문자열입니다.");
                acknowledgment.acknowledge(); // 잘못된 이벤트는 스킵
                return;
            }
            
            // 해당 사용자의 모든 커스텀 API 삭제 처리
            int deletedCount = customApiService.deleteAllCustomApisByUserId(userDeletedEvent.getUserId());
            additionalFields.put("deleted_custom_apis_count", deletedCount);
            
            structuredLogger.logBusinessEvent("USER_DELETED_EVENT_PROCESSED", 
                "Successfully processed user deletion event", additionalFields);
            
            log.info("사용자 삭제 이벤트 처리 완료 - userId: {}, 삭제된 커스텀 API 개수: {}", 
                    userDeletedEvent.getUserId(), deletedCount);
            
            // 메시지 처리 완료 확인 (수동 커밋)
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            structuredLogger.logError("USER_DELETED_EVENT_PROCESSING_ERROR", 
                "Failed to process user deletion event", e, additionalFields);
            
            log.error("사용자 삭제 이벤트 처리 중 오류 발생 - eventId: {}, userId: {}", 
                    userDeletedEvent.getEventId(), userDeletedEvent.getUserId(), e);
            
            // 에러 발생 시에도 메시지를 확인하여 재처리 방지
            // 실제 운영환경에서는 DLQ(Dead Letter Queue) 설정을 고려해야 함
            acknowledgment.acknowledge();
        }
    }
}