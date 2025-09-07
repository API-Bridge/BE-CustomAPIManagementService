package org.example.customapisvc.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "커스텀 API 상세 조회 응답 (외부 마이크로서비스용)")
public class CustomApiDetailResponseDto {

    @Schema(description = "커스텀 API ID", example = "api-010")
    private String customApiId;

    @Schema(description = "사용자 ID", example = "user-456")
    private String userId;

    @Schema(description = "커스텀 API 이름", example = "비트코인 이더리움 가격 조회 API")
    private String name;

    @Schema(description = "커스텀 API 설명", example = "실시간 암호화폐 가격을 조회하는 커스텀 API")
    private String description;

    @Schema(description = "외부 API 목록")
    private List<ExternalApiDetail> externalApiUrl_list;

    @Schema(description = "생성일시", example = "2025-08-27T14:30:25.123456")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-08-27T14:30:25.123456")
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "외부 API 상세 정보")
    public static class ExternalApiDetail {

        @Schema(description = "API ID", example = "crypto-price-api-001")
        private String apiId;

        @Schema(description = "API 이름", example = "CoinGecko 가격 조회 API")
        private String apiName;

        @Schema(description = "API 엔드포인트 URL", example = "https://api.coingecko.com/api/v3/simple/price")
        private String endpoint;

        @Schema(description = "HTTP 메소드", example = "GET")
        private String httpMethod;

        @Schema(description = "파라미터 목록")
        private List<ApiParameterDetail> parameters;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "API 파라미터 상세 정보")
    public static class ApiParameterDetail {

        @Schema(description = "파라미터 이름", example = "coins")
        private String paramName;

        @Schema(description = "파라미터 데이터 타입", example = "String")
        private String paramType;

        @Schema(description = "파라미터 설명", example = "조회할 코인 목록 (bitcoin,ethereum)")
        private String description;

        @Schema(description = "필수 여부", example = "true")
        private boolean necessary;

        @Schema(description = "기본값", example = "bitcoin,ethereum")
        private String defaultValue;
    }
}