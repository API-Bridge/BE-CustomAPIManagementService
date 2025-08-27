package org.example.customapisvc.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.customapisvc.domain.Entity.ApiType;
import org.example.customapisvc.dto.ExternalApiInfoDto;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "커스텀 API 응답")
public class CustomApiResponseDto {

    @Schema(description = "커스텀 API ID", example = "api-001")
    private String customApiId;

    @Schema(description = "사용자 ID", example = "user-123")
    private String userId;

    @Schema(description = "커스텀 API 이름", example = "사용자 정보 및 날씨 조회 API")
    private String name;

    @Schema(description = "커스텀 API 설명", example = "사용자의 위치 정보를 기반으로 날씨 정보를 함께 조회하는 커스텀 API")
    private String description;

    @Schema(description = "외부API 이름 및 호출구조", example = "[{'api_name':'날씨 조회 API'}]")
    @JsonProperty("externalApiUrl_list")
    private List<ExternalApiInfoDto> externalApiUrl_list;

    @Schema(description = "AI Plus 활성화 여부", example = "false")
    private Boolean aiPlusActive;

    @Schema(description = "API 타입 (ORIGINAL: 직접 생성, LINK: 가져온 API)", example = "ORIGINAL")
    private ApiType apiType;

    @Schema(description = "공개(공유) 여부", example = "false")
    private boolean isPublic;

    @Schema(description = "원본 API ID (가져온 API인 경우)", example = "api-origin-001")
    private String originApiId;

    @Schema(description = "원본 API 소유자 ID (가져온 API인 경우)", example = "user-creator-456")
    private String ownerUserId;

    @Schema(description = "생성일시", example = "2023-08-08T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2023-08-08T10:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "일일 호출 횟수", example = "152")
    private Long callCount;

    public CustomApiResponseDto(String customApiId, String userId, String name, String description, List<ExternalApiInfoDto> externalApiUrlList, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.customApiId = customApiId;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.externalApiUrl_list = externalApiUrlList;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public CustomApiResponseDto(String customApiId, String userId, String name, String description, List<ExternalApiInfoDto> externalApiUrlList, Boolean aiPlusActive, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.customApiId = customApiId;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.externalApiUrl_list = externalApiUrlList;
        this.aiPlusActive = aiPlusActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // 공유 기능을 포함한 새로운 생성자
    public CustomApiResponseDto(String customApiId, String userId, String name, String description, List<ExternalApiInfoDto> externalApiUrlList, Boolean aiPlusActive, LocalDateTime createdAt, LocalDateTime updatedAt, ApiType apiType, boolean isPublic, String originApiId, String ownerUserId) {
        this.customApiId = customApiId;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.externalApiUrl_list = externalApiUrlList;
        this.aiPlusActive = aiPlusActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.apiType = apiType;
        this.isPublic = isPublic;
        this.originApiId = originApiId;
        this.ownerUserId = ownerUserId;
    }

    // 호출 횟수를 포함한 완전한 생성자
    public CustomApiResponseDto(String customApiId, String userId, String name, String description, List<ExternalApiInfoDto> externalApiUrlList, Boolean aiPlusActive, LocalDateTime createdAt, LocalDateTime updatedAt, ApiType apiType, boolean isPublic, String originApiId, String ownerUserId, Long callCount) {
        this.customApiId = customApiId;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.externalApiUrl_list = externalApiUrlList;
        this.aiPlusActive = aiPlusActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.apiType = apiType;
        this.isPublic = isPublic;
        this.originApiId = originApiId;
        this.ownerUserId = ownerUserId;
        this.callCount = callCount;
    }
}