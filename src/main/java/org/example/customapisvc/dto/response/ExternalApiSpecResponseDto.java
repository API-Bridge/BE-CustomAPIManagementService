package org.example.customapisvc.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 외부API 명세서 서비스로부터 받는 실제 응답 구조
 * /api/v1/api/external-api-specs/search-by-name 엔드포인트 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalApiSpecResponseDto {

    private String apiId;
    private String apiName;
    private String apiDescription;
    private String apiIssuer;
    private String apiUrl;
    private String httpMethod;
    private boolean isActive;
    private String createdAt;
    private String updatedAt;
    private String credentialId;
    private String organizationName;
    private Long domainId;
    private String domainName;
    private Long keywordId;
    private String keywordName;
    private List<ParameterInfo> parameters;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParameterInfo {
        private String parameterId;
        private String paramName;
        private String paramType;
        private boolean isRequired;
        private String paramDescription;
        private String defaultValue;
        private Object additionalFields;
    }
}