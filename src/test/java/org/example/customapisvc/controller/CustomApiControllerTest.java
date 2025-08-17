package org.example.customapisvc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.customapisvc.dto.ExternalApiInfoDto;
import org.example.customapisvc.dto.request.InitiateCreationRequestDto;
import org.example.customapisvc.dto.response.CustomApiResponseDto;
import org.example.customapisvc.service.AiCustomApiGenerationService;
import org.example.customapisvc.service.CustomApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(value = CustomApiController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
@ActiveProfiles("test")
@DisplayName("CustomApiController 테스트")
class CustomApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomApiService customApiService;
    
    @MockitoBean
    private AiCustomApiGenerationService aiCustomApiGenerationService;

    private CustomApiResponseDto testResponse1;
    private CustomApiResponseDto testResponse2;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        
        testResponse1 = new CustomApiResponseDto(
                "api-001", "user-123", "날씨 조회 API",
                "날씨 정보를 조회하는 API", Collections.emptyList(), now, now);
        
        testResponse2 = new CustomApiResponseDto(
                "api-002", "user-123", "상품 추천 API",
                "사용자 맞춤 상품을 추천하는 API", Collections.emptyList(), now, now);
    }

    @Test
    @DisplayName("GET /custom-apis - 사용자의 커스텀 API 목록 조회 성공")
    void getCustomApisByUserId_Success() throws Exception {
        // given
        List<CustomApiResponseDto> mockResponse = Arrays.asList(testResponse1, testResponse2);
        given(customApiService.getCustomApisByUserId("user-123")).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/custom-apis")
                        .param("userId", "user-123"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("커스텀 API 목록을 성공적으로 조회했습니다."))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].customApiId").value("api-001"))
                .andExpect(jsonPath("$.data[0].name").value("날씨 조회 API"))
                .andExpect(jsonPath("$.data[1].customApiId").value("api-002"));

        then(customApiService).should().getCustomApisByUserId("user-123");
    }

    @Test
    @DisplayName("GET /custom-apis - 빈 목록 조회")
    void getCustomApisByUserId_EmptyList() throws Exception {
        // given
        given(customApiService.getCustomApisByUserId("user-456")).willReturn(Collections.emptyList());

        // when & then
        mockMvc.perform(get("/custom-apis")
                        .param("userId", "user-456"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("GET /custom-apis/{customApiId} - 커스텀 API 단건 조회 성공")
    void getCustomApiById_Success() throws Exception {
        // given
        given(customApiService.getCustomApiById("api-001")).willReturn(testResponse1);

        // when & then
        mockMvc.perform(get("/custom-apis/api-001"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("커스텀 API를 성공적으로 조회했습니다."))
                .andExpect(jsonPath("$.data.customApiId").value("api-001"))
                .andExpect(jsonPath("$.data.name").value("날씨 조회 API"));
    }

    @Test
    @DisplayName("GET /custom-apis/{customApiId} - 존재하지 않는 API 조회시 예외")
    void getCustomApiById_NotFound() throws Exception {
        // given
        given(customApiService.getCustomApiById("nonexistent"))
                .willThrow(new RuntimeException("Custom API not found: nonexistent"));

        // when & then
        mockMvc.perform(get("/custom-apis/nonexistent"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /custom-apis/search - 이름으로 커스텀 API 검색 성공")
    void searchCustomApisByName_Success() throws Exception {
        // given
        List<CustomApiResponseDto> mockResponse = Arrays.asList(testResponse1);
        given(customApiService.searchCustomApisByName("user-123", "날씨")).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/custom-apis/search")
                        .param("userId", "user-123")
                        .param("name", "날씨"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("커스텀 API 검색을 성공적으로 완료했습니다."))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("날씨 조회 API"));
    }

    @Test
    @DisplayName("POST /custom-apis/ai-generate - AI를 이용한 커스텀 API 생성 성공")
    void createCustomApiWithAi_Success() throws Exception {
        // given
        // 1. AI 생성에 필요한 완전한 요청 DTO를 생성합니다.
        List<ExternalApiInfoDto.ApiParameter> weatherApiParams = List.of(new ExternalApiInfoDto.ApiParameter("city", "string", "날씨를 조회할 도시 이름", true));
        ExternalApiInfoDto weatherApi = new ExternalApiInfoDto("api-weather-01", "날씨 정보 API", weatherApiParams);
        List<ExternalApiInfoDto> externalApis = List.of(weatherApi);

        InitiateCreationRequestDto request = new InitiateCreationRequestDto(
                "auth0|user-test-12345",
                "FREE",
                "my-custom-weather-api-1", 
                List.of("weather"),
                List.of("air_quality"),
                "서울의 현재 날씨와 미세먼지 정보를 확인하고 싶어."
        );

        // 2. AI 서비스가 반환할 응답을 미리 정의합니다.
        CustomApiResponseDto mockResponse = new CustomApiResponseDto(
                "my-custom-weather-api-1",
                "auth0|user-test-12345",
                "AI가 생성한 날씨 API",
                "AI가 사용자의 요청에 따라 생성한 API입니다.",
                externalApis,
                now, now);

        // 3. Mockito 설정: any() 매처에 올바른 DTO 클래스(InitiateCreationRequest.class)를 사용합니다.
        given(aiCustomApiGenerationService.generateCustomApiWithAi(any(InitiateCreationRequestDto.class))).willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/custom-apis/ai-generate") // 역할을 명확히 하는 엔드포인트 사용을 권장합니다.
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("AI를 통해 커스텀 API가 성공적으로 생성되었습니다."))
                .andExpect(jsonPath("$.data.customApiId").value("my-custom-weather-api-1"))
                .andExpect(jsonPath("$.data.name").value("AI가 생성한 날씨 API"));
    }

    @Test
    @DisplayName("POST /custom-apis/ai-generate - 유효성 검증 실패 (필수 필드 누락)")
    void createCustomApi_ValidationFailed() throws Exception {
        // given
        // @NotBlank, @NotEmpty 등의 제약조건을 위반하도록 비어있는 DTO 객체를 생성합니다.
        InitiateCreationRequestDto invalidRequest = new InitiateCreationRequestDto();

        // when & then
        // 성공 케이스와 동일한 엔드포인트를 테스트하여 일관성을 유지합니다.
        mockMvc.perform(post("/custom-apis/ai-generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /custom-apis/{customApiId} - 커스텀 API 삭제 성공")
    void deleteCustomApi_Success() throws Exception {
        // given
        doNothing().when(customApiService).deleteCustomApi("api-001", "user-123");

        // when & then
        mockMvc.perform(delete("/custom-apis/api-001")
                        .param("userId", "user-123"))
                .andDo(print())
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("커스텀 API가 성공적으로 삭제되었습니다."));

        then(customApiService).should().deleteCustomApi("api-001", "user-123");
    }

    @Test
    @DisplayName("DELETE /custom-apis/{customApiId} - 권한 없는 사용자의 삭제 시도")
    void deleteCustomApi_Unauthorized() throws Exception {
        // given
        doThrow(new RuntimeException("Unauthorized to delete Custom API: api-001"))
                .when(customApiService).deleteCustomApi("api-001", "other-user");

        // when & then
        mockMvc.perform(delete("/custom-apis/api-001")
                        .param("userId", "other-user"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /custom-apis - userId 파라미터 누락시 400 에러")
    void getCustomApisByUserId_MissingParameter() throws Exception {
        // when & then
        mockMvc.perform(get("/custom-apis"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}