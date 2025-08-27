package org.example.customapisvc.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 커스텀 API 호출 이벤트
 * 사용자가 커스텀 API를 실제로 호출했을 때 발생하는 이벤트
 * AI 서비스나 다른 서비스에서 커스텀 API를 사용할 때 발행되는 이벤트
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomApiCalledEvent extends BaseEvent {

    /** 커스텀 API 호출 이벤트 페이로드 */
    private CustomApiCalledPayload payload;

    /**
     * 커스텀 API 호출 이벤트 생성자
     * 
     * @param customApiId 호출된 커스텀 API ID
     * @param userId 호출한 사용자 ID
     * @param requestSource 요청 소스 (예: "ai-service", "web-app")
     */
    public CustomApiCalledEvent(String customApiId, String userId, String requestSource) {
        super("CustomApiCalled");
        this.payload = CustomApiCalledPayload.builder()
                .customApiId(customApiId)
                .userId(userId)
                .requestSource(requestSource)
                .calledAt(LocalDateTime.now())
                .build();
    }

    @Override
    public Object getPayload() {
        return this.payload;
    }

    /**
     * 타입이 지정된 페이로드 반환 메서드
     * 
     * @return CustomApiCalledPayload 타입의 페이로드
     */
    public CustomApiCalledPayload getTypedPayload() {
        return this.payload;
    }

    /**
     * 커스텀 API 호출 이벤트 페이로드 클래스
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomApiCalledPayload {
        /** 호출된 커스텀 API ID */
        private String customApiId;
        
        /** 호출한 사용자 ID */
        private String userId;
        
        /** 요청 소스 (ai-service, web-app 등) */
        private String requestSource;
        
        /** 호출 시간 */
        private LocalDateTime calledAt;
    }
}