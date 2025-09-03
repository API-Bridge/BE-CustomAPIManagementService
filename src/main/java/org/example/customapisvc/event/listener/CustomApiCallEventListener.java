package org.example.customapisvc.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customapisvc.event.model.CustomApiCalledEvent;
import org.example.customapisvc.event.model.CustomApiCreateFailedEvent;
import org.example.customapisvc.service.ApiCallCountSyncService;
import org.example.customapisvc.util.StructuredLogger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 커스텀 API 이벤트 리스너
 * custom_api_events 토픽에서 다양한 커스텀 API 관련 이벤트를 수신하여 처리
 * 
 * 주요 기능:
 * - CustomApiCalled 이벤트: API 호출 횟수를 Redis에 카운트
 * - CustomApiCreateFailed 이벤트: API 생성 실패 로깅 및 모니터링
 * - 이벤트 처리 결과 로깅 및 모니터링
 */
@Slf4j
@Component
@RequiredArgsConstructor
// 모든 프로필에서 활성화
@ConditionalOnBean(ApiCallCountSyncService.class)
@KafkaListener(
        topics = "custom_api_events",
        groupId = "custom-api-service-group",
        containerFactory = "kafkaListenerContainerFactory"
)
public class CustomApiCallEventListener {

    private final ApiCallCountSyncService apiCallCountSyncService;
    private final StructuredLogger structuredLogger;

    /**
     * 커스텀 API 호출 이벤트 핸들러
     * CustomApiCalled 이벤트를 수신하여 API 호출 횟수를 Redis에 카운트
     * 
     * @param customApiCalledEvent 커스텀 API 호출 이벤트 객체
     * @param partition Kafka 파티션 정보
     * @param offset Kafka 오프셋 정보
     * @param acknowledgment 수동 커밋을 위한 Acknowledgment
     */
    @KafkaHandler
    public void handleCustomApiCalledEvent(
            @Payload CustomApiCalledEvent customApiCalledEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        CustomApiCalledEvent.CustomApiCalledPayload payload = customApiCalledEvent.getTypedPayload();
        String customApiId = payload.getCustomApiId();
        String userId = payload.getUserId();
        String requestSource = payload.getRequestSource();
        
        log.info("커스텀 API 호출 이벤트 수신 - eventId: {}, customApiId: {}, userId: {}, requestSource: {}, partition: {}, offset: {}", 
                customApiCalledEvent.getEventId(), customApiId, userId, requestSource, partition, offset);
        
        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("event_id", customApiCalledEvent.getEventId());
        additionalFields.put("trace_id", customApiCalledEvent.getTraceId());
        additionalFields.put("custom_api_id", customApiId);
        additionalFields.put("user_id", userId);
        additionalFields.put("request_source", requestSource);
        additionalFields.put("event_type", customApiCalledEvent.getEventType());
        additionalFields.put("service_name", customApiCalledEvent.getServiceName());
        additionalFields.put("partition", partition);
        additionalFields.put("offset", offset);
        additionalFields.put("called_at", payload.getCalledAt());
        
        try {
            structuredLogger.logBusinessEvent("CUSTOM_API_CALLED_EVENT_RECEIVED", 
                "Received custom API called event from Kafka", additionalFields);
            
            // 커스텀 API ID 유효성 검증
            if (customApiId == null || customApiId.trim().isEmpty()) {
                structuredLogger.logError("INVALID_CUSTOM_API_CALLED_EVENT", 
                    "Custom API ID is null or empty in custom API called event", 
                    new IllegalArgumentException("Invalid customApiId"), additionalFields);
                log.error("유효하지 않은 커스텀 API 호출 이벤트: customApiId가 null 또는 빈 문자열입니다.");
                acknowledgment.acknowledge(); // 잘못된 이벤트는 스킵
                return;
            }
            
            // Redis에 API 호출 횟수 증가
            apiCallCountSyncService.incrementApiCallCount(customApiId);
            
            structuredLogger.logBusinessEvent("CUSTOM_API_CALLED_EVENT_PROCESSED", 
                "Successfully processed custom API called event and incremented call count", additionalFields);
            
            log.info("커스텀 API 호출 이벤트 처리 완료 - customApiId: {}, userId: {}, requestSource: {}", 
                    customApiId, userId, requestSource);
            
            // 메시지 처리 완료 확인 (수동 커밋)
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            structuredLogger.logError("CUSTOM_API_CALLED_EVENT_PROCESSING_ERROR", 
                "Failed to process custom API called event", e, additionalFields);
            
            log.error("커스텀 API 호출 이벤트 처리 중 오류 발생 - eventId: {}, customApiId: {}, userId: {}", 
                    customApiCalledEvent.getEventId(), customApiId, userId, e);
            
            // 에러 발생 시에도 메시지를 확인하여 재처리 방지
            // 실제 운영환경에서는 DLQ(Dead Letter Queue) 설정을 고려해야 함
            acknowledgment.acknowledge();
        }
    }

    /**
     * 커스텀 API 생성 실패 이벤트 핸들러
     * CustomApiCreateFailed 이벤트를 수신하여 로깅 및 모니터링 처리
     * 
     * @param customApiCreateFailedEvent 커스텀 API 생성 실패 이벤트 객체
     * @param partition Kafka 파티션 정보
     * @param offset Kafka 오프셋 정보
     * @param acknowledgment 수동 커밋을 위한 Acknowledgment
     */
    @KafkaHandler
    public void handleCustomApiCreateFailedEvent(
            @Payload CustomApiCreateFailedEvent customApiCreateFailedEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        CustomApiCreateFailedEvent.CustomApiCreateFailedPayload payload = customApiCreateFailedEvent.getPayload();
        String userId = payload.getUserId();
        String name = payload.getName();
        String failureReason = payload.getFailureReason();
        String errorMessage = payload.getErrorMessage();
        String failureStage = payload.getFailureStage();
        
        log.warn("커스텀 API 생성 실패 이벤트 수신 - eventId: {}, userId: {}, name: {}, failureReason: {}, failureStage: {}, partition: {}, offset: {}", 
                customApiCreateFailedEvent.getEventId(), userId, name, failureReason, failureStage, partition, offset);
        
        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("event_id", customApiCreateFailedEvent.getEventId());
        additionalFields.put("trace_id", customApiCreateFailedEvent.getTraceId());
        additionalFields.put("user_id", userId);
        additionalFields.put("api_name", name);
        additionalFields.put("failure_reason", failureReason);
        additionalFields.put("error_message", errorMessage);
        additionalFields.put("failure_stage", failureStage);
        additionalFields.put("event_type", customApiCreateFailedEvent.getEventType());
        additionalFields.put("service_name", customApiCreateFailedEvent.getServiceName());
        additionalFields.put("partition", partition);
        additionalFields.put("offset", offset);
        
        try {
            structuredLogger.logBusinessEvent("CUSTOM_API_CREATE_FAILED_EVENT_RECEIVED", 
                "Received custom API create failed event from Kafka", additionalFields);
            
            // 추가적인 실패 처리 로직이 필요한 경우 여기에 구현
            // 예: 실패 통계 업데이트, 알림 발송, 재시도 로직 등
            
            structuredLogger.logBusinessEvent("CUSTOM_API_CREATE_FAILED_EVENT_PROCESSED", 
                "Successfully processed custom API create failed event", additionalFields);
            
            log.info("커스텀 API 생성 실패 이벤트 처리 완료 - userId: {}, name: {}, failureReason: {}", 
                    userId, name, failureReason);
            
            // 메시지 처리 완료 확인 (수동 커밋)
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            structuredLogger.logError("CUSTOM_API_CREATE_FAILED_EVENT_PROCESSING_ERROR", 
                "Failed to process custom API create failed event", e, additionalFields);
            
            log.error("커스텀 API 생성 실패 이벤트 처리 중 오류 발생 - eventId: {}, userId: {}, name: {}", 
                    customApiCreateFailedEvent.getEventId(), userId, name, e);
            
            // 에러 발생 시에도 메시지를 확인하여 재처리 방지
            acknowledgment.acknowledge();
        }
    }
}