package org.example.customapisvc.domain.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
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
    @Index(name = "idx_user_id_deleted", columnList = "user_id, deleted"),
    @Index(name = "idx_is_active", columnList = "is_active"),
    @Index(name = "idx_is_public", columnList = "is_public")
    // Association Table 모델: api_type, origin_api_id 인덱스 제거
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

    @Column(name = "external_api_url_list_json", columnDefinition = "JSON")
    private String externalApiUrlListJson;
    
    @Transient
    private List<ExternalApiInfoDto> externalApiUrlList;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "ai_plus_active", nullable = false)
    private Boolean aiPlusActive = false;

    // Association Table 모델: 자기참조 관계 제거
    // api_type, origin_api_id 필드 삭제됨
    // linkedApis 관계도 삭제됨
    
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false; // 전체 공개 여부, 기본값은 false

    @Column(name = "call_count", nullable = false)
    private Long callCount = 0L; // 일일 호출 횟수

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
        // Association Table 모델: 모든 API가 원본이므로 직접 데이터 반환
        // (공유 관계는 api_share 테이블에서 별도 관리)
        
        if (externalApiUrlList == null && externalApiUrlListJson != null) {
            // Json에서 역직렬화
            try {
                ObjectMapper mapper = new ObjectMapper();
                
                // 먼저 새로운 구조로 시도
                try {
                    this.externalApiUrlList = mapper.readValue(externalApiUrlListJson, 
                        new TypeReference<List<ExternalApiInfoDto>>() {});
                } catch (JsonProcessingException ex) {
                    // 실패하면 기존 구조(name, endpoint)로 파싱 후 변환
                    List<LegacyApiInfo> legacyList = mapper.readValue(externalApiUrlListJson, 
                        new TypeReference<List<LegacyApiInfo>>() {});
                    
                    this.externalApiUrlList = legacyList.stream()
                        .map(legacy -> new ExternalApiInfoDto(
                            legacy.getName(), // apiId로 name 사용
                            legacy.getName(), // apiName으로 name 사용
                            legacy.getEndpoint(), // endpoint 추가
                            "GET", // 기본값으로 GET 설정 (legacy 데이터에는 HTTP 메소드 정보가 없음)
                            new ArrayList<>() // 빈 파라미터 리스트
                        ))
                        .collect(java.util.stream.Collectors.toList());
                }
            } catch (JsonProcessingException e) {
                this.externalApiUrlList = new ArrayList<>();
            }
        }
        return externalApiUrlList != null ? externalApiUrlList : new ArrayList<>();
    }

    // 기존 JSON 구조를 위한 임시 클래스
    private static class LegacyApiInfo {
        private String name;
        private String endpoint;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
    }

}