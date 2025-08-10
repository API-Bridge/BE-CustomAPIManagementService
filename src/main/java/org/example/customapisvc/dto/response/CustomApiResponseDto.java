package org.example.customapisvc.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.customapisvc.dto.ExternalApiInfoDto;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
    private List<ExternalApiInfoDto> externalApiUrl_list;

    @Schema(description = "생성일시", example = "2023-08-08T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2023-08-08T10:30:00")
    private LocalDateTime updatedAt;

    public CustomApiResponseDto(String s, String s1, String 날씨_조회_api, String 날씨_정보를_조회하는_api, LocalDateTime now, LocalDateTime now1) {
    }
}