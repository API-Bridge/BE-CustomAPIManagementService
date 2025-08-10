package org.example.customapisvc.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.customapisvc.dto.ExternalApiInfoDto;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "외부 API리스트 응답 DTO")
public class ExternalApiResponseDto {

    @NotEmpty(message = "외부 API 목록은 필수입니다")
    @Schema(description = "AI가 선택한 외부 API 목록", example = "[{\"apiId\": \"api-001\", \"apiName\": \"날씨 조회 API\"}]", required = true)
    private List<ExternalApiInfoDto> externalApiList;
}
