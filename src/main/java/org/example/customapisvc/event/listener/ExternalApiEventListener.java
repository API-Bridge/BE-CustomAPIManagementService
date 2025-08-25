package org.example.customapisvc.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customapisvc.event.model.ExternalApiDeletedEvent;
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
 * 외부 API 관련 이벤트 리스너
 * 다른 마이크로서비스에서 발행되는 외부 API 관련 이벤트를 수신하여 처리
 * 
 * 주요 기능:
 * - 외부 API 삭제 이벤트 수신 및 처리
 * - 해당 외부 API를 사용하는 모든 커스텀 API 비활성화 처리
 * - 이벤트 처리 결과 로깅 및 모니터링
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!dev")  // dev 프로필이 아닐 때만 활성화 (실제 Kafka 사용 시)
public class ExternalApiEventListener {

    private final CustomApiService customApiService;
    private final StructuredLogger structuredLogger;

    /**
     * 외부 API 삭제 이벤트 리스너
     * api-usage-logs 토픽에서 EXTERNAL_API_DELETED 이벤트를 수신하여 처리
     * 
     * @param externalApiDeletedEvent 외부 API 삭제 이벤트 객체
     * @param partition Kafka 파티션 정보
     * @param offset Kafka 오프셋 정보
     * @param acknowledgment 수동 커밋을 위한 Acknowledgment
     */
    @KafkaListener(
            topics = "api-usage-logs",
            groupId = "custom-api-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleExternalApiDeletedEvent(
            @Payload ExternalApiDeletedEvent externalApiDeletedEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        log.info("외부 API 삭제 이벤트 수신 - eventId: {}, externalApiId: {}, externalApiName: {}, partition: {}, offset: {}", 
                externalApiDeletedEvent.getEventId(), externalApiDeletedEvent.getPayload().getExternalApiId(), 
                externalApiDeletedEvent.getPayload().getExternalApiName(), partition, offset);
        
        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("event_id", externalApiDeletedEvent.getEventId());
        additionalFields.put("trace_id", externalApiDeletedEvent.getTraceId());
        additionalFields.put("external_api_id", externalApiDeletedEvent.getPayload().getExternalApiId());
        additionalFields.put("external_api_name", externalApiDeletedEvent.getPayload().getExternalApiName());
        additionalFields.put("external_api_url", externalApiDeletedEvent.getPayload().getExternalApiUrl());
        additionalFields.put("event_type", externalApiDeletedEvent.getEventType());
        additionalFields.put("service_name", externalApiDeletedEvent.getServiceName());
        additionalFields.put("partition", partition);
        additionalFields.put("offset", offset);
        additionalFields.put("deletion_reason", externalApiDeletedEvent.getPayload().getDeletionReason());
        
        try {
            structuredLogger.logBusinessEvent("EXTERNAL_API_DELETED_EVENT_RECEIVED", 
                "Received external API deletion event from Kafka", additionalFields);
            
            // 외부 API ID 유효성 검증
            String externalApiId = externalApiDeletedEvent.getPayload().getExternalApiId();
            if (externalApiId == null || externalApiId.trim().isEmpty()) {
                structuredLogger.logError("INVALID_EXTERNAL_API_DELETED_EVENT", 
                    "External API ID is null or empty in external API deletion event", new IllegalArgumentException("Invalid externalApiId"), additionalFields);
                log.error("유효하지 않은 외부 API 삭제 이벤트: externalApiId가 null 또는 빈 문자열입니다.");
                acknowledgment.acknowledge(); // 잘못된 이벤트는 스킵
                return;
            }
            
            // 해당 외부 API를 사용하는 모든 커스텀 API 비활성화 처리
            int deactivatedCount = customApiService.deactivateCustomApisByExternalApiId(externalApiId);
            additionalFields.put("deactivated_custom_apis_count", deactivatedCount);
            
            structuredLogger.logBusinessEvent("EXTERNAL_API_DELETED_EVENT_PROCESSED", 
                "Successfully processed external API deletion event", additionalFields);
            
            log.info("외부 API 삭제 이벤트 처리 완료 - externalApiId: {}, 비활성화된 커스텀 API 개수: {}", 
                    externalApiId, deactivatedCount);
            
            // 메시지 처리 완료 확인 (수동 커밋)
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            structuredLogger.logError("EXTERNAL_API_DELETED_EVENT_PROCESSING_ERROR", 
                "Failed to process external API deletion event", e, additionalFields);
            
            log.error("외부 API 삭제 이벤트 처리 중 오류 발생 - eventId: {}, externalApiId: {}", 
                    externalApiDeletedEvent.getEventId(), externalApiDeletedEvent.getPayload().getExternalApiId(), e);
            
            // 에러 발생 시에도 메시지를 확인하여 재처리 방지
            // 실제 운영환경에서는 DLQ(Dead Letter Queue) 설정을 고려해야 함
            acknowledgment.acknowledge();
        }
    }
}