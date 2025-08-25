package org.example.customapisvc.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.example.customapisvc.dto.ExternalApiInfoDto;

import java.util.List;

/**
 * 커스텀 API 생성 이벤트
 * 커스텀 API가 성공적으로 생성되었을 때 발행되는 이벤트
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomApiCreatedEvent extends BaseEvent {

    /**
     * 커스텀 API 생성 이벤트 생성자
     * 
     * @param customApiId 생성된 커스텀 API ID
     * @param userId 사용자 ID
     * @param name 커스텀 API 이름
     * @param description 커스텀 API 설명
     * @param externalApiList 사용된 외부 API 목록
     */
    public CustomApiCreatedEvent(String customApiId, String userId, String name, String description, List<ExternalApiInfoDto> externalApiList) {
        super("CustomApiCreated");
        this.payload = new CustomApiCreatedPayload(customApiId, userId, name, description, externalApiList);
    }

    /** 이벤트의 실제 내용을 담는 페이로드 */
    private CustomApiCreatedPayload payload;

    @Override
    public CustomApiCreatedPayload getPayload() {
        return payload;
    }

    /**
     * 커스텀 API 생성 이벤트의 페이로드
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomApiCreatedPayload {
        /** 생성된 커스텀 API ID */
        private String customApiId;
        
        /** 커스텀 API를 생성한 사용자 ID */
        private String userId;
        
        /** 커스텀 API 이름 */
        private String name;
        
        /** 커스텀 API 설명 */
        private String description;
        
        /** 사용된 외부 API 목록 */
        private List<ExternalApiInfoDto> externalApiList;
    }
}