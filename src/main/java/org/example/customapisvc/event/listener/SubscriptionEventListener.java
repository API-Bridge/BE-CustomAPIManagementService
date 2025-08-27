package org.example.customapisvc.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customapisvc.util.StructuredLogger;
import org.example.customapisvc.event.model.UserSubscriptionUpdateEvent;
import org.example.customapisvc.service.CustomApiService;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 구독(Subscription) 관련 이벤트를 처리하는 Kafka 리스너
 * SubscriptionEvents 토픽을 구독하여 사용자 플랜 변경 이벤트를 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!dev")  // dev 프로필이 아닐 때만 활성화 (실제 Kafka 사용 시)
public class SubscriptionEventListener {

    private final CustomApiService customApiService;
    private final StructuredLogger structuredLogger;

    /**
     * 사용자 구독 플랜 업데이트 이벤트 처리
     * 플랜 다운그레이드 시 커스텀 API 개수 제한에 따라 활성화/비활성화 처리
     * 
     * @param userSubscriptionUpdateEvent 사용자 구독 업데이트 이벤트 객체
     * @param partition Kafka 파티션 정보
     * @param offset Kafka 오프셋 정보
     * @param acknowledgment Kafka 수동 커밋을 위한 객체
     */
    @KafkaListener(
            topics = "SubscriptionEvents",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserSubscriptionUpdateEvent(
            @Payload UserSubscriptionUpdateEvent userSubscriptionUpdateEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("사용자 구독 플랜 업데이트 이벤트 수신 - eventId: {}, userId: {}, previousPlan: {}, newPlan: {}, partition: {}, offset: {}", 
                userSubscriptionUpdateEvent.getEventId(), 
                userSubscriptionUpdateEvent.getPayload().getUserId(),
                userSubscriptionUpdateEvent.getPayload().getPreviousPlan(),
                userSubscriptionUpdateEvent.getPayload().getNewPlan(),
                partition, offset);

        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("event_id", userSubscriptionUpdateEvent.getEventId());
        additionalFields.put("trace_id", userSubscriptionUpdateEvent.getTraceId());
        additionalFields.put("user_id", userSubscriptionUpdateEvent.getPayload().getUserId());
        additionalFields.put("previous_plan", userSubscriptionUpdateEvent.getPayload().getPreviousPlan());
        additionalFields.put("new_plan", userSubscriptionUpdateEvent.getPayload().getNewPlan());
        additionalFields.put("update_reason", userSubscriptionUpdateEvent.getPayload().getUpdateReason());
        additionalFields.put("event_type", userSubscriptionUpdateEvent.getEventType());
        additionalFields.put("service_name", userSubscriptionUpdateEvent.getServiceName());

        try {
            structuredLogger.logBusinessEvent("USER_SUBSCRIPTION_UPDATE_EVENT_RECEIVED", 
                "Received user subscription update event from Kafka", additionalFields);

            String userId = userSubscriptionUpdateEvent.getPayload().getUserId();
            String previousPlan = userSubscriptionUpdateEvent.getPayload().getPreviousPlan();
            String newPlan = userSubscriptionUpdateEvent.getPayload().getNewPlan();

            if (userId == null || userId.trim().isEmpty()) {
                structuredLogger.logError("INVALID_USER_SUBSCRIPTION_UPDATE_EVENT", 
                    "User ID is null or empty in user subscription update event", 
                    new IllegalArgumentException("Invalid userId"), additionalFields);
                acknowledgment.acknowledge();
                return;
            }

            // 플랜 다운그레이드인지 확인
            if (isPlanDowngrade(previousPlan, newPlan)) {
                log.info("플랜 다운그레이드 감지 - userId: {}, {} -> {}", userId, previousPlan, newPlan);
                additionalFields.put("is_downgrade", true);
                
                // 커스텀 API 활성화/비활성화 처리
                customApiService.handlePlanDowngrade(userId, newPlan);
                
                structuredLogger.logBusinessEvent("PLAN_DOWNGRADE_PROCESSED", 
                    "Successfully processed plan downgrade for user", additionalFields);
            } else if (isPlanUpgrade(previousPlan, newPlan)) {
                log.info("플랜 업그레이드 감지 - userId: {}, {} -> {}", userId, previousPlan, newPlan);
                additionalFields.put("is_upgrade", true);
                
                // 커스텀 API 재활성화 처리
                customApiService.handlePlanUpgrade(userId, newPlan);
                
                structuredLogger.logBusinessEvent("PLAN_UPGRADE_PROCESSED", 
                    "Successfully processed plan upgrade for user", additionalFields);
            } else {
                log.info("동일 레벨 플랜 - userId: {}, {} -> {}", userId, previousPlan, newPlan);
                additionalFields.put("is_same_level", true);
                
                structuredLogger.logBusinessEvent("PLAN_SAME_LEVEL", 
                    "Same level plan detected, no action needed", additionalFields);
            }

            acknowledgment.acknowledge();
            
            structuredLogger.logBusinessEvent("USER_SUBSCRIPTION_UPDATE_EVENT_PROCESSED", 
                "Successfully processed user subscription update event", additionalFields);

        } catch (Exception e) {
            structuredLogger.logError("USER_SUBSCRIPTION_UPDATE_EVENT_PROCESSING_ERROR", 
                "Failed to process user subscription update event", e, additionalFields);
            
            log.error("사용자 구독 플랜 업데이트 이벤트 처리 중 오류 발생 - eventId: {}, userId: {}", 
                    userSubscriptionUpdateEvent.getEventId(), 
                    userSubscriptionUpdateEvent.getPayload().getUserId(), e);
            
            // 에러 발생 시에도 acknowledge하여 무한 재처리 방지
            acknowledgment.acknowledge();
        }
    }

    /**
     * 플랜 다운그레이드 여부를 확인
     * PRO > FREE 순서로 등급을 매겨 이전 플랜보다 낮은 등급인지 확인
     * 
     * @param previousPlan 이전 플랜
     * @param newPlan 새로운 플랜
     * @return 다운그레이드 여부
     */
    private boolean isPlanDowngrade(String previousPlan, String newPlan) {
        if (previousPlan == null || newPlan == null) {
            return false;
        }

        int previousLevel = getPlanLevel(previousPlan.toUpperCase());
        int newLevel = getPlanLevel(newPlan.toUpperCase());

        return previousLevel > newLevel;
    }

    /**
     * 플랜 업그레이드 여부를 확인
     * FREE < PRO 순서로 등급을 매겨 이전 플랜보다 높은 등급인지 확인
     * 
     * @param previousPlan 이전 플랜
     * @param newPlan 새로운 플랜
     * @return 업그레이드 여부
     */
    private boolean isPlanUpgrade(String previousPlan, String newPlan) {
        if (previousPlan == null || newPlan == null) {
            return false;
        }

        int previousLevel = getPlanLevel(previousPlan.toUpperCase());
        int newLevel = getPlanLevel(newPlan.toUpperCase());

        return previousLevel < newLevel;
    }

    /**
     * 플랜의 우선순위 레벨을 반환
     * 
     * @param plan 플랜명 (FREE, PRO)
     * @return 플랜 레벨 (높을수록 상위 플랜)
     */
    private int getPlanLevel(String plan) {
        switch (plan) {
            case "PRO":
                return 2;
            case "FREE":
                return 1;
            default:
                return 0;
        }
    }
}