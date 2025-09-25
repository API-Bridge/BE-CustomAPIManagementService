package org.example.customapisvc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExternalApiInfoDto {
    
    private String apiId;
    private String apiName;
    private String endpoint;
    private String httpMethod;
    
    // httpMethod getter with default value
    public String getHttpMethod() {
        return httpMethod != null ? httpMethod : "GET";
    }
    private List<ApiParameter> parameters;
    private String reason;

    public ExternalApiInfoDto(String apiId, String apiName, List<ApiParameter> parameters) {
        this.apiId = apiId;
        this.apiName = apiName;
        this.parameters = parameters;
    }

    public ExternalApiInfoDto(String apiId, String apiName, String endpoint, List<ApiParameter> parameters) {
        this.apiId = apiId;
        this.apiName = apiName;
        this.endpoint = endpoint;
        this.parameters = parameters;
    }

    public ExternalApiInfoDto(String apiId, String apiName, String endpoint, String httpMethod, List<ApiParameter> parameters) {
        this.apiId = apiId;
        this.apiName = apiName;
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.parameters = parameters;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ApiParameter {
        private String parameterId;
        private String paramName;
        private String paramType;
        @JsonProperty("isRequired")
        private boolean isRequired = true;  // 항상 true로 고정
        private String paramDescription;
        private String defaultValue;

        public ApiParameter(String parameterId, String paramName, String paramType, boolean isRequired, String paramDescription, String defaultValue) {
            this.parameterId = parameterId;
            this.paramName = paramName;
            this.paramType = paramType;
            this.isRequired = true;  // 항상 true로 고정
            this.paramDescription = paramDescription;
            this.defaultValue = defaultValue;
        }

        public ApiParameter(String paramName, String paramType, String paramDescription, boolean isRequired) {
            this.paramName = paramName;
            this.paramType = paramType;
            this.paramDescription = paramDescription;
            this.isRequired = true;  // 항상 true로 고정
        }
    }
}