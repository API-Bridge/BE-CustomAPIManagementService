package org.example.customapisvc.dto.response.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User 서비스로부터 받은 사용자 정보 응답")
public class UserInfoResponseDto {

    @Schema(description = "사용자 고유 ID", example = "auth0|user123456")
    private String userId;

    @Schema(description = "사용자 이메일", example = "user@example.com")
    private String email;

    @Schema(description = "사용자 이름", example = "홍길동")
    private String name;

    @Schema(description = "사용자 현재 플랜", example = "PRO")
    private String plan;

    @Schema(description = "계정 활성화 상태", example = "true")
    private Boolean isActive;

    @Schema(description = "계정 생성일", example = "2023-01-01T00:00:00Z")
    private String createdAt;

    @Schema(description = "마지막 수정일", example = "2023-12-01T00:00:00Z")
    private String updatedAt;
}