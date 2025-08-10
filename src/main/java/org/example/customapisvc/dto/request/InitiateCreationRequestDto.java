package org.example.customapisvc.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "커스텀 API 생성 요청")
public class InitiateCreationRequestDto {

    @NotBlank(message = "사용자 ID는 필수입니다")
    @Schema(description = "API 생성을 요청한 사용자의 고유 ID (auth0_id)", example = "auth0|user123456", required = true)
    private String userId;

    @NotBlank(message = "플랜은 필수입니다")
    @Schema(description = "사용자의 현재 플랜", example = "FREE", required = true)
    private String plan;

    @NotBlank(message = "커스텀 API ID는 필수입니다")
    @Schema(description = "사용자가 지정했거나 AI가 생성한 새 커스텀 API의 고유 ID", example = "weather-news-api-001", required = true)
    private String customApiId;

    @NotEmpty(message = "도메인 목록은 필수입니다")
    @Schema(description = "AI가 추출한 관련 도메인 코드 목록", example = "[\"weather\", \"news\"]", required = true)
    private List<String> domains;

    @NotEmpty(message = "키워드 목록은 필수입니다")
    @Schema(description = "AI가 추출한 관련 키워드 코드 목록", example = "[\"air_quality\", \"breaking_news\"]", required = true)
    private List<String> keywords;

    @NotBlank(message = "사용자 요구사항은 필수입니다")
    @Schema(description = "사용자가 원하는 커스텀 API의 기능 설명", example = "서울의 현재 날씨와 미세먼지 정보를 확인하고 야외 활동 추천을 받고 싶어", required = true)
    private String userQuery;

}