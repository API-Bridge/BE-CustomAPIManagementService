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
    @AllArgsConstructor
    public static class ApiParameter {
        private String parameterId;
        private String paramName;
        private String paramType;
        @JsonProperty("necessary")
        private boolean isRequired;
        private String paramDescription;
        private String defaultValue;

        public ApiParameter(String city, String string, String 날씨를_조회할_도시_이름, boolean b) {
        }
    }
}