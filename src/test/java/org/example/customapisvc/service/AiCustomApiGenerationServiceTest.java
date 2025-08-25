package org.example.customapisvc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.customapisvc.domain.Entity.CustomApi;
import org.example.customapisvc.dto.ExternalApiInfoDto;
import org.example.customapisvc.dto.request.InitiateCreationRequestDto;
import org.example.customapisvc.dto.response.CustomApiResponseDto;
import org.example.customapisvc.dto.response.ExternalApiResponseDto;
import org.example.customapisvc.repository.CustomApiRepository;
import org.example.customapisvc.service.ExternalApiService;
import org.example.customapisvc.service.impl.AiCustomApiGenerationServiceImpl;
import org.example.customapisvc.unit.BaseUnitTest;
import org.example.customapisvc.util.GenerateTextFromTextInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("AiCustomApiGenerationService 단위 테스트")
class AiCustomApiGenerationServiceTest extends BaseUnitTest {

    @InjectMocks
    private AiCustomApiGenerationServiceImpl aiCustomApiGenerationService;

    @Mock
    private GenerateTextFromTextInput geminiService;

    @Mock
    private CustomApiRepository customApiRepository;

    @Mock
    private ExternalApiService externalApiService;

    @Mock
    private ObjectMapper objectMapper;

    private InitiateCreationRequestDto testRequest;
    private CustomApi testCustomApi;

    @BeforeEach
    void setUp() {
        testRequest = new InitiateCreationRequestDto();
        testRequest.setUserId("auth0|user123456");
        testRequest.setCustomApiId("weather-news-api-001");
        testRequest.setDomains(List.of("weather", "news"));
        testRequest.setKeywords(List.of("air_quality", "breaking_news"));
        testRequest.setUserQuery("서울의 현재 날씨와 미세먼지 정보를 확인하고, 관련 최신 뉴스를 제공하는 API가 필요함.");

        testCustomApi = new CustomApi();
        testCustomApi.setCustomApiId("weather-news-api-001");
        testCustomApi.setUserId("auth0|user123456");
        testCustomApi.setName("날씨-뉴스 통합 조회 API");
        testCustomApi.setExternalApiUrlList(
                List.of(
                        new ExternalApiInfoDto("api-001", "날씨 조회 API", List.of( new ExternalApiInfoDto.ApiParameter("city", "INPUT", "도시 이름", true))),
                        new ExternalApiInfoDto("api-002", "뉴스 검색 API", List.of( new ExternalApiInfoDto.ApiParameter("query", "INPUT", "검색어", true))),
                        new ExternalApiInfoDto("api-003", "미세먼지 조회 API", List.of( new ExternalApiInfoDto.ApiParameter("location", "INPUT", "지역명", true)))
                )
        );
        testCustomApi.setDescription("날씨와 뉴스 정보를 통합하여 조회하는 커스텀 API");
        testCustomApi.setCreatedAt(LocalDateTime.now());
        testCustomApi.setUpdatedAt(LocalDateTime.now());
        testCustomApi.setDeleted(false);
    }

    @Test
    @DisplayName("AI 커스텀 API 생성 성공 - 정상적인 JSON 응답")
    void generateCustomApiWithAi_Success_ValidJsonResponse() {
        // given
        // 외부API서비스 응답 Mock
        List<ExternalApiInfoDto> externalApiList = List.of(
                new ExternalApiInfoDto("api-001", "날씨 조회 API", 
                    List.of(new ExternalApiInfoDto.ApiParameter("city", "INPUT", "도시 이름", true))),
                new ExternalApiInfoDto("api-002", "뉴스 검색 API", 
                    List.of(new ExternalApiInfoDto.ApiParameter("query", "INPUT", "검색어", true))),
                new ExternalApiInfoDto("api-003", "미세먼지 조회 API", 
                    List.of(new ExternalApiInfoDto.ApiParameter("location", "INPUT", "지역명", true)))
        );
        ExternalApiResponseDto externalApiResponse = new ExternalApiResponseDto(externalApiList);
        given(externalApiService.getExternalApiList(any())).willReturn(externalApiResponse);

        // AI가 반환할 응답 
        String aiJsonResponse = """
                {
                  "name": "날씨-뉴스 통합 조회 API",
                  "description": "실시간 날씨 정보와 최신 뉴스를 통합하여 제공하는 맞춤형 API 서비스입니다.",
                  "selectedApis": [
                    {
                      "apiId": "api-001",
                      "apiName": "날씨 조회 API",
                      "callOrder": 1,
                      "reason": "사용자가 날씨 정보를 확인하려고 합니다."
                    },
                    {
                      "apiId": "api-002",
                      "apiName": "뉴스 검색 API",
                      "callOrder": 2,
                      "reason": "사용자가 최신 뉴스를 보려고 합니다."
                    }
                  ]
                }
                """;

        try {
            // ObjectMapper mock 설정
            given(objectMapper.writeValueAsString(any())).willReturn("[{\"apiId\":\"api-001\",\"apiName\":\"날씨 조회 API\"}]");
            
            // JsonNode mock 설정
            JsonNode mockJsonNode = mock(JsonNode.class);
            JsonNode nameNode = mock(JsonNode.class);
            JsonNode descriptionNode = mock(JsonNode.class);
            JsonNode selectedApisNode = mock(JsonNode.class);
            JsonNode apiNode = mock(JsonNode.class);
            JsonNode apiIdNode = mock(JsonNode.class);
            JsonNode reasonNode = mock(JsonNode.class);
            JsonNode callOrderNode = mock(JsonNode.class);
            
            given(nameNode.asText()).willReturn("날씨-뉴스 통합 조회 API");
            given(descriptionNode.asText()).willReturn("실시간 날씨 정보와 최신 뉴스를 통합하여 제공하는 맞춤형 API 서비스입니다.");
            given(mockJsonNode.get("name")).willReturn(nameNode);
            given(mockJsonNode.get("description")).willReturn(descriptionNode);
            given(mockJsonNode.has("selectedApis")).willReturn(true);
            given(mockJsonNode.get("selectedApis")).willReturn(selectedApisNode);
            given(selectedApisNode.size()).willReturn(2);
            given(selectedApisNode.get(0)).willReturn(apiNode);
            given(selectedApisNode.get(1)).willReturn(apiNode);
            given(apiNode.get("apiId")).willReturn(apiIdNode);
            given(apiNode.get("reason")).willReturn(reasonNode);
            given(apiNode.has("callOrder")).willReturn(true);
            given(apiNode.get("callOrder")).willReturn(callOrderNode);
            given(apiIdNode.asText()).willReturn("api-001");
            given(reasonNode.asText()).willReturn("사용자가 날씨 정보를 확인하려고 합니다.");
            given(callOrderNode.asInt()).willReturn(1);
            
            given(objectMapper.readTree(anyString())).willReturn(mockJsonNode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        // AI 응답과 데이터베이스 저장 설정
        given(geminiService.generateText(anyString())).willReturn(aiJsonResponse);
        given(customApiRepository.save(any(CustomApi.class))).willReturn(testCustomApi);

        // when
        CustomApiResponseDto result = aiCustomApiGenerationService.generateCustomApiWithAi(testRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getCustomApiId()).isEqualTo("weather-news-api-001");
        assertThat(result.getUserId()).isEqualTo("auth0|user123456");
        assertThat(result.getName()).isEqualTo("날씨-뉴스 통합 조회 API");
        assertThat(result.getDescription()).contains("날씨와 뉴스");

        then(externalApiService).should().getExternalApiList(any());
        then(geminiService).should().generateText(anyString());
        then(customApiRepository).should().save(any(CustomApi.class));
    }

    @Test
    @DisplayName("AI 커스텀 API 생성 실패 - Gemini 서비스 오류")
    void generateCustomApiWithAi_Failure_GeminiServiceError() {
        // given
        ExternalApiResponseDto externalApiResponse = new ExternalApiResponseDto(List.of());
        given(externalApiService.getExternalApiList(any())).willReturn(externalApiResponse);
        given(geminiService.generateText(anyString()))
                .willThrow(new RuntimeException("Gemini API 호출 실패"));

        // when & then
        assertThatThrownBy(() -> aiCustomApiGenerationService.generateCustomApiWithAi(testRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("AI를 활용한 커스텀 API 생성에 실패했습니다");

        then(externalApiService).should().getExternalApiList(any());
        then(geminiService).should().generateText(anyString());
        then(customApiRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("AI 커스텀 API 생성 실패 - 데이터베이스 저장 오류")
    void generateCustomApiWithAi_Failure_DatabaseError() {
        // given
        ExternalApiResponseDto externalApiResponse = new ExternalApiResponseDto(List.of());
        given(externalApiService.getExternalApiList(any())).willReturn(externalApiResponse);
        
        String aiJsonResponse = """
                {
                  "name": "날씨-뉴스 통합 조회 API",
                  "description": "실시간 날씨 정보와 최신 뉴스를 통합하여 제공하는 맞춤형 API 서비스입니다."
                }
                """;

        try {
            // ObjectMapper mock 설정
            given(objectMapper.writeValueAsString(any())).willReturn("[{\"apiId\":\"api-001\",\"apiName\":\"날씨 조회 API\"}]");
            
            // JsonNode mock 설정
            JsonNode mockJsonNode = mock(JsonNode.class);
            JsonNode nameNode = mock(JsonNode.class);
            JsonNode descriptionNode = mock(JsonNode.class);
            given(nameNode.asText()).willReturn("날씨-뉴스 통합 조회 API");
            given(descriptionNode.asText()).willReturn("실시간 날씨 정보와 최신 뉴스를 통합하여 제공하는 맞춤형 API 서비스입니다.");
            given(mockJsonNode.get("name")).willReturn(nameNode);
            given(mockJsonNode.get("description")).willReturn(descriptionNode);
            given(mockJsonNode.has("selectedApis")).willReturn(false);
            given(objectMapper.readTree(anyString())).willReturn(mockJsonNode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        given(geminiService.generateText(anyString())).willReturn(aiJsonResponse);
        given(customApiRepository.save(any(CustomApi.class)))
                .willThrow(new RuntimeException("데이터베이스 연결 오류"));

        // when & then
        assertThatThrownBy(() -> aiCustomApiGenerationService.generateCustomApiWithAi(testRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("AI를 활용한 커스텀 API 생성에 실패했습니다");

        then(externalApiService).should().getExternalApiList(any());
        then(geminiService).should().generateText(anyString());
        then(customApiRepository).should().save(any(CustomApi.class));
    }
}