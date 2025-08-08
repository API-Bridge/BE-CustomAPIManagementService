package org.example.customapisvc.event.publisher;

import org.example.customapisvc.event.model.BaseEvent;

/**
 * 이벤트 발행 서비스 인터페이스
 * 환경에 따라 다른 구현체를 제공하기 위한 추상화 계층
 */
public interface EventPublisherService {
    
    /**
     * 이벤트를 지정된 토픽에 발행
     * 
     * @param topic 이벤트를 발행할 토픽
     * @param event 발행할 이벤트 객체
     */
    void publishEvent(String topic, BaseEvent event);
}