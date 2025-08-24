package org.example.customapisvc.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 외부API 관리서비스로부터 받는 실제 응답 구조
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalApiServiceResponseDto {

    private boolean success;
    private String message;
    private DataWrapper data;
    private String timestamp;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataWrapper {
        private int totalCount;
        private Summary summary;
        private Map<String, List<ApiWithParameters>> results;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Summary {
        private int requestedDomains;
        private int requestedKeywords;
        private int matchedCombinations;
        private int totalApis;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiWithParameters {
        private ApiInfo api;
        private List<ParameterInfo> parameters;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiInfo {
        private String apiId;
        private String apiName;
        private String apiUrl;
        private String apiIssuer;
        private String apiOwner;
        private String apiDomain;
        private String apiKeyword;
        private String httpMethod;
        private String apiDescription;
        private boolean apiEffectiveness;
        private String createdAt;
        private String updatedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParameterInfo {
        private String apiId;
        private String paramName;
        private String paramType;
        private boolean isRequired;
        private String defaultValue;
        private String paramDescription;
    }
}