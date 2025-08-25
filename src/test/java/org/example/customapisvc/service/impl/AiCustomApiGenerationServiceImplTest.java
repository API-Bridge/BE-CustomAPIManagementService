package org.example.customapisvc.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.customapisvc.domain.Entity.CustomApi;
import org.example.customapisvc.dto.ExternalApiInfoDto;
import org.example.customapisvc.dto.request.ExternalApiRequestDto;
import org.example.customapisvc.dto.request.InitiateCreationRequestDto;
import org.example.customapisvc.dto.response.ExternalApiResponseDto;
import org.example.customapisvc.repository.CustomApiRepository;
import org.example.customapisvc.service.ExternalApiService;
import org.example.customapisvc.service.cache.DisabledApiCacheService;
import org.example.customapisvc.util.GenerateTextFromTextInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiCustomApiGenerationServiceImplTest {

    @InjectMocks
    private AiCustomApiGenerationServiceImpl aiCustomApiGenerationService;

    @Mock
    private GenerateTextFromTextInput geminiService;

    @Mock
    private CustomApiRepository customApiRepository;

    @Mock
    private ExternalApiService externalApiService;

    @Mock
    private DisabledApiCacheService disabledApiCacheService;

    @Spy
    private ObjectMapper objectMapper;

    private InitiateCreationRequestDto requestDto;

    @BeforeEach
    void setUp() {
        requestDto = new InitiateCreationRequestDto();
        requestDto.setUserId("test-user");
        requestDto.setCustomApiId("custom-api-1");
        requestDto.setDomains(Collections.singletonList("Weather"));
        requestDto.setKeywords(Collections.singletonList("Current"));
        requestDto.setUserQuery("Get current weather");
    }

    @Test
    @DisplayName("AI 커스텀 API 생성 시 비활성화된 외부 API를 필터링해야 한다")
    void generateCustomApiWithAi_shouldFilterDisabledApis() throws Exception {
        // Given
        // 1. 외부 API 서비스가 반환할 3개의 API 목록 생성
        ExternalApiInfoDto api1 = new ExternalApiInfoDto("api-1", "Weather API 1", Collections.emptyList());
        ExternalApiInfoDto api2 = new ExternalApiInfoDto("api-2", "Weather API 2", Collections.emptyList());
        ExternalApiInfoDto api3 = new ExternalApiInfoDto("api-3", "Weather API 3", Collections.emptyList());
        List<ExternalApiInfoDto> externalApiList = Arrays.asList(api1, api2, api3);
        ExternalApiResponseDto externalApiResponse = new ExternalApiResponseDto(externalApiList);

        when(externalApiService.getExternalApiList(any(ExternalApiRequestDto.class)))
                .thenReturn(externalApiResponse);

        // 2. 캐시 서비스가 비활성화된 API ID ("api-2")를 반환하도록 설정
        Set<String> disabledApiIds = new HashSet<>(Collections.singletonList("api-2"));
        when(disabledApiCacheService.getDisabledApis()).thenReturn(disabledApiIds);

        // 3. AI 서비스가 응답할 가짜 JSON 설정
        String fakeAiResponse = "{\"name\":\"Current Weather API\",\"description\":\"Provides current weather.\",\"selectedApis\":[{\"apiId\":\"api-1\", \"reason\":\"reason\", \"callOrder\":1}]}";
        when(geminiService.generateText(any(String.class))).thenReturn(fakeAiResponse);

        // 4. Repository save 동작 설정
        CustomApi savedApi = new CustomApi();
        savedApi.setCustomApiId("custom-api-1");
        savedApi.setUserId("test-user");
        savedApi.setName("Current Weather API");
        savedApi.setDescription("Provides current weather.");
        savedApi.setExternalApiUrlList(Collections.singletonList(api1));
        savedApi.setCreatedAt(LocalDateTime.now());
        savedApi.setUpdatedAt(LocalDateTime.now());
        when(customApiRepository.save(any(CustomApi.class))).thenReturn(savedApi);


        // When
        aiCustomApiGenerationService.generateCustomApiWithAi(requestDto);

        // Then
        // AI에게 전달된 프롬프트를 캡처
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiService).generateText(promptCaptor.capture());

        String capturedPrompt = promptCaptor.getValue();

        // 프롬프트에 포함된 API 목록이 올바르게 필터링되었는지 확인
        assertThat(capturedPrompt).contains("api-1");
        assertThat(capturedPrompt).contains("api-3");
        assertThat(capturedPrompt).doesNotContain("api-2");

        System.out.println("Test passed: Disabled APIs are correctly filtered.");
    }
}