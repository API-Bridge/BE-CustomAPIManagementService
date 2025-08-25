package org.example.customapisvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.customapisvc.dto.common.BaseResponse;
import org.example.customapisvc.dto.request.InitiateCreationRequestDto;
import org.example.customapisvc.dto.response.CustomApiResponseDto;
import org.example.customapisvc.service.AiCustomApiGenerationService;
import org.example.customapisvc.service.CustomApiService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Custom API", description = "커스텀 API 관리")
@RestController
@RequestMapping("/custom-apis")
@RequiredArgsConstructor
public class CustomApiController {

    private final CustomApiService customApiService;
    private final AiCustomApiGenerationService aiCustomApiGenerationService;

    @Operation(summary = "사용자의 커스텀 API 목록 조회", description = "특정 사용자가 생성한 모든 커스텀 API를 조회합니다.") //TODO: 사용자의 플랜 기반으로 갯수 제한 로직 추가
    @GetMapping
    public BaseResponse<List<CustomApiResponseDto>> getCustomApisByUserId(
            @Parameter(description = "사용자 ID", required = true, example = "user-123")
            @RequestParam String userId) {
        
        List<CustomApiResponseDto> customApis = customApiService.getCustomApisByUserId(userId);
        return BaseResponse.success(customApis, "커스텀 API 목록을 성공적으로 조회했습니다.");
    }

    @GetMapping("/{customApiId}")
    public BaseResponse<CustomApiResponseDto> getCustomApiById(
            @Parameter(description = "커스텀 API ID", required = true, example = "api-001")
            @PathVariable String customApiId) {
        
        CustomApiResponseDto customApi = customApiService.getCustomApiById(customApiId);
        return BaseResponse.success(customApi, "커스텀 API를 성공적으로 조회했습니다.");
    }

    @Operation(summary = "커스텀 API 이름 검색", description = "사용자의 커스텀 API를 이름으로 검색합니다.")
    @GetMapping("/search")
    public BaseResponse<List<CustomApiResponseDto>> searchCustomApisByName(
            @Parameter(description = "사용자 ID", required = true, example = "user-123")
            @RequestParam String userId,
            @Parameter(description = "검색할 이름 (부분 일치)", required = true, example = "날씨")
            @RequestParam String name) {
        
        List<CustomApiResponseDto> customApis = customApiService.searchCustomApisByName(userId, name);
        return BaseResponse.success(customApis, "커스텀 API 검색을 성공적으로 완료했습니다.");
    }

    @Operation(summary = "AI 기반 커스텀 API 생성", description = "AI를 활용하여 자동으로 커스텀 API를 생성합니다.")
    @PostMapping("/ai-generate")
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<CustomApiResponseDto> generateCustomApiWithAi(
            @Parameter(description = "AI 커스텀 API 생성 개시 요청", required = true)
            @Valid @RequestBody InitiateCreationRequestDto request) {
        
        try {
            CustomApiResponseDto customApi = aiCustomApiGenerationService.generateCustomApiWithAi(request);
            return BaseResponse.success(customApi, "AI를 통해 커스텀 API가 성공적으로 생성되었습니다.");
        } catch (Exception e) {
            // 생성 실패 시 별도 처리는 AiCustomApiGenerationService에서 담당
            throw e;
        }
    }

    @Operation(summary = "커스텀 API 삭제", description = "커스텀 API를 삭제합니다. (Soft Delete 방식)")
    @DeleteMapping("/{customApiId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public BaseResponse<Void> deleteCustomApi(
            @Parameter(description = "커스텀 API ID", required = true, example = "api-001")
            @PathVariable String customApiId,
            @Parameter(description = "사용자 ID", required = true, example = "user-123")
            @RequestParam String userId) {
        
        customApiService.deleteCustomApi(customApiId, userId);
        return BaseResponse.success(null, "커스텀 API가 성공적으로 삭제되었습니다.");
    }
}