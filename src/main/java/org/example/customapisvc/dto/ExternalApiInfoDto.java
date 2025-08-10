package org.example.customapisvc.dto;

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
    private List<ApiParameter> parameters;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiParameter {
        private String paramName;
        private String paramType; // INPUT, OUTPUT
        private String description;
        private boolean necessary; // TRUE, FALSE (INPUT 파라미터만 해당)
    }
}