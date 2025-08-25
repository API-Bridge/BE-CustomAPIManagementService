package org.example.customapisvc.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 외부 API 삭제 이벤트
 * 외부 API가 삭제되었을 때 발행되는 이벤트
 * 이 이벤트를 수신하여 해당 외부 API를 사용하는 모든 커스텀 API를 비활성화 처리
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExternalApiDeletedEvent extends BaseEvent {

    /**
     * 외부 API 삭제 이벤트 생성자
     * 
     * @param externalApiId 삭제된 외부 API ID
     * @param externalApiName 삭제된 외부 API 이름
     * @param externalApiUrl 삭제된 외부 API URL
     * @param deletionReason 삭제 사유
     */
    public ExternalApiDeletedEvent(String externalApiId, String externalApiName, String externalApiUrl, String deletionReason) {
        super("ExternalApiDeleted");
        this.payload = new ExternalApiDeletedPayload(externalApiId, externalApiName, externalApiUrl, deletionReason);
    }

    /** 이벤트의 실제 내용을 담는 페이로드 */
    private ExternalApiDeletedPayload payload;

    @Override
    public ExternalApiDeletedPayload getPayload() {
        return payload;
    }

    /**
     * 외부 API 삭제 이벤트의 페이로드
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalApiDeletedPayload {
        /** 삭제된 외부 API의 ID */
        private String externalApiId;
        
        /** 삭제된 외부 API의 이름 */
        private String externalApiName;
        
        /** 삭제된 외부 API의 URL */
        private String externalApiUrl;
        
        /** 외부 API 삭제 사유 (선택사항) */
        private String deletionReason;
    }
}