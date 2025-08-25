package org.example.customapisvc.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.example.customapisvc.dto.ExternalApiInfoDto;

import java.util.List;

/**
 * 커스텀 API 삭제 이벤트
 * 커스텀 API가 삭제되었을 때 발행되는 이벤트
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomApiDeletedEvent extends BaseEvent {

    /**
     * 커스텀 API 삭제 이벤트 생성자
     * 
     * @param customApiId 삭제된 커스텀 API ID
     * @param userId 사용자 ID
     * @param name 삭제된 커스텀 API 이름
     * @param description 삭제된 커스텀 API 설명
     * @param externalApiList 삭제된 커스텀 API에서 사용되던 외부 API 목록
     * @param deletionReason 삭제 사유
     */
    public CustomApiDeletedEvent(String customApiId, String userId, String name, String description, List<ExternalApiInfoDto> externalApiList, String deletionReason) {
        super("CustomApiDeleted");
        this.payload = new CustomApiDeletedPayload(customApiId, userId, name, description, externalApiList, deletionReason);
    }

    /** 이벤트의 실제 내용을 담는 페이로드 */
    private CustomApiDeletedPayload payload;

    @Override
    public CustomApiDeletedPayload getPayload() {
        return payload;
    }

    /**
     * 커스텀 API 삭제 이벤트의 페이로드
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomApiDeletedPayload {
        /** 삭제된 커스텀 API ID */
        private String customApiId;
        
        /** 커스텀 API를 삭제한 사용자 ID */
        private String userId;
        
        /** 삭제된 커스텀 API 이름 */
        private String name;
        
        /** 삭제된 커스텀 API 설명 */
        private String description;
        
        /** 삭제된 커스텀 API에서 사용되던 외부 API 목록 */
        private List<ExternalApiInfoDto> externalApiList;
        
        /** 삭제 사유 (선택사항) */
        private String deletionReason;
    }
}