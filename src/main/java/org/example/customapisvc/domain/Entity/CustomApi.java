package org.example.customapisvc.domain.Entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.example.customapisvc.dto.ExternalApiInfoDto;

import java.util.ArrayList;
import java.util.List;

/**
 * 커스텀 API 엔티티
 * 사용자가 AI를 통해 생성한 커스텀 API의 메타데이터를 관리합니다.
 * 
 * 주요 기능:
 * - 커스텀 API 기본 정보 (이름, 설명)
 * - 선별된 외부 API 목록 (JSON 형태로 저장)
 * - 사용자별 API 관리
 * - 소프트 삭제 지원
 */
@Entity
@Table(name = "custom_api", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_deleted", columnList = "deleted"),
    @Index(name = "idx_user_id_deleted", columnList = "user_id, deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomApi extends BaseEntity {

    @Id
    @Column(name = "custom_api_id", length = 36, nullable = false)
    private String customApiId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "external_api_name", columnDefinition = "JSON")
    private String externalApiUrlListJson;
    
    @Transient
    private List<ExternalApiInfoDto> externalApiUrlList;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    public void setExternalApiUrlList(List<ExternalApiInfoDto> externalApiUrlList) {
        this.externalApiUrlList = externalApiUrlList;
        // 직렬화 (직렬화: 객체를 JSON으로 변환하는 과정)
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.externalApiUrlListJson = mapper.writeValueAsString(externalApiUrlList);
        } catch (JsonProcessingException e) {
            this.externalApiUrlListJson = "[]";
        }
    }
    
    public List<ExternalApiInfoDto> getExternalApiUrlList() {
        if (externalApiUrlList == null && externalApiUrlListJson != null) {
            // Json 에서 역직렬화 (역직렬화: JSON을 객체로 변환하는 과정)
            try {
                ObjectMapper mapper = new ObjectMapper();
                this.externalApiUrlList = mapper.readValue(externalApiUrlListJson, 
                    new TypeReference<List<ExternalApiInfoDto>>() {});
            } catch (JsonProcessingException e) {
                this.externalApiUrlList = new ArrayList<>();
            }
        }
        return externalApiUrlList != null ? externalApiUrlList : new ArrayList<>();
    }

}