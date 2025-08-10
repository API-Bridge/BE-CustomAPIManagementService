package org.example.customapisvc.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "커스텀 API 생성 요청")
public class CustomApiCreateRequestDto {

    @NotBlank(message = "사용자 ID는 필수입니다")
    @Size(max = 36, message = "사용자 ID는 36자를 초과할 수 없습니다")
    @Schema(description = "사용자 ID", example = "user-123", required = true)
    private String userId;

    @NotBlank(message = "API 이름은 필수입니다")
    @Size(max = 255, message = "API 이름은 255자를 초과할 수 없습니다")
    @Schema(description = "사용자가 지정한 커스텀 API 이름", example = "사용자 정보 및 날씨 조회 API", required = true)
    private String name;

    @Size(max = 1000, message = "설명은 1000자를 초과할 수 없습니다")
    @Schema(description = "커스텀 API 설명", example = "사용자의 위치 정보를 기반으로 날씨 정보를 함께 조회하는 커스텀 API")
    private String description;

    @Schema(description = "요구 데이터 도메인", hidden = true)
    private String domain;

    @Schema(description = "요구 데이터 키워드", hidden = true)
    private String keyword;
}