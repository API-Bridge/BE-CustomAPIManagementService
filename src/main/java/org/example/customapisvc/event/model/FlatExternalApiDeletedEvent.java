package org.example.customapisvc.event.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 외부 서비스에서 발행되는 평면적 구조의 외부 API 삭제 이벤트
 * 다른 마이크로서비스에서 발행하는 이벤트 형식에 맞춰 설계
 */
@Data
@NoArgsConstructor
public class FlatExternalApiDeletedEvent {
    
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private String sourceService;
    private String correlationId;
    
    @JsonProperty("apiId")
    private String apiId;
    
    @JsonProperty("apiName") 
    private String apiName;
    
    @JsonProperty("apiUrl")
    private String apiUrl;
    
    @JsonProperty("deletedBy")
    private String deletedBy;
    
    @JsonProperty("deletionReason")
    private String deletionReason;
    
    /**
     * 기존 ExternalApiDeletedEvent 형식으로 변환
     */
    public ExternalApiDeletedEvent toExternalApiDeletedEvent() {
        return new ExternalApiDeletedEvent(apiId, apiName, apiUrl, deletionReason);
    }
}