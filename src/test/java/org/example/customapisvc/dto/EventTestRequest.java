package org.example.customapisvc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 테스트용 이벤트 발행 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "CustomApiCalled 이벤트 발행 테스트 요청")
public class EventTestRequest {
    
    @Schema(description = "커스텀 API ID", example = "api-001", required = true)
    private String customApiId;
    
    @Schema(description = "사용자 ID", example = "user-123")
    private String userId = "test-user";
    
    @Schema(description = "요청 소스", example = "mobile-app")
    private String requestSource = "test-client";
}