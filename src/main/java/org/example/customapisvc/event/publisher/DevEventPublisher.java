package org.example.customapisvc.event.publisher;

import org.example.customapisvc.event.model.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 개발환경용 더미 이벤트 발행자
 * Kafka 없이 개발할 수 있도록 로그만 출력하는 EventPublisher
 */
@Slf4j
@Component
@Profile("dev")  // dev 프로필에서만 활성화
public class DevEventPublisher implements EventPublisherService {

    /**
     * 개발환경에서는 실제 이벤트 발행 대신 로그만 출력
     * 
     * @param topic 이벤트를 발행할 Kafka 토픽
     * @param event 발행할 이벤트 객체 (BaseEvent 상속)
     */
    @Override
    public void publishEvent(String topic, BaseEvent event) {
        log.info("[DEV] Event published to topic: {}, eventType: {}, eventId: {}", 
                topic, event.getEventType(), event.getEventId());
        log.debug("[DEV] Event content: {}", event);
    }
}