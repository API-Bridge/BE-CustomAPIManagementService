package org.example.customapisvc.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "외부 API 리스트 조회 요청")
public class ExternalApiRequestDto {

    @NotEmpty(message = "도메인 목록은 필수입니다")
    private List<String> domains;

    @NotEmpty(message = "키워드 목록은 필수입니다")
    private List<String> keywords;

}
