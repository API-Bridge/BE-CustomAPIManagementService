package org.example.customapisvc.service;

import org.example.customapisvc.domain.Entity.CustomApi;
import org.example.customapisvc.dto.ExternalApiInfoDto;
import org.example.customapisvc.dto.request.InitiateCreationRequestDto;
import org.example.customapisvc.dto.response.CustomApiResponseDto;
import org.example.customapisvc.repository.CustomApiRepository;
import org.example.customapisvc.service.impl.AiCustomApiGenerationServiceImpl;
import org.example.customapisvc.service.impl.CustomApiServiceImpl;
import org.example.customapisvc.testdata.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomApiService 테스트")
class CustomApiServiceTest {

    @Mock
    private CustomApiRepository customApiRepository;

    @Mock
    private AiCustomApiGenerationService aiCustomApiGenerationService;

    @InjectMocks
    private CustomApiServiceImpl customApiService;

    @InjectMocks
    private AiCustomApiGenerationServiceImpl AIcustomApiServiceImpl;

    private CustomApi testCustomApi1;
    private CustomApi testCustomApi2;

    @BeforeEach
    void setUp() {
        testCustomApi1 = TestDataFactory.CustomApiTestData.createCustomApi("api-001", "user-123", "날씨 조회 API", "날씨 정보를 조회하는 API");
        testCustomApi2 = TestDataFactory.CustomApiTestData.createCustomApi("api-002", "user-123", "상품 추천 API", "사용자 맞춤 상품을 추천하는 API");
    }

    @Test
    @DisplayName("사용자 ID로 커스텀 API 목록 조회 성공")
    void getCustomApisByUserId_Success() {
        // given
        List<CustomApi> mockCustomApis = Arrays.asList(testCustomApi1, testCustomApi2);
        given(customApiRepository.findByUserIdAndDeletedFalse("user-123")).willReturn(mockCustomApis);

        // when
        List<CustomApiResponseDto> result = customApiService.getCustomApisByUserId("user-123");

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCustomApiId()).isEqualTo("api-001");
        assertThat(result.get(0).getName()).isEqualTo("날씨 조회 API");
        assertThat(result.get(1).getCustomApiId()).isEqualTo("api-002");
        assertThat(result.get(1).getName()).isEqualTo("상품 추천 API");
        
        then(customApiRepository).should(times(1)).findByUserIdAndDeletedFalse("user-123");
    }

    @Test
    @DisplayName("커스텀 API 단건 조회 성공")
    void getCustomApiById_Success() {
        // given
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("api-001")).willReturn(Optional.of(testCustomApi1));

        // when
        CustomApiResponseDto result = customApiService.getCustomApiById("api-001");

        // then
        assertThat(result.getCustomApiId()).isEqualTo("api-001");
        assertThat(result.getName()).isEqualTo("날씨 조회 API");
        assertThat(result.getDescription()).isEqualTo("날씨 정보를 조회하는 API");
        
        then(customApiRepository).should(times(1)).findByCustomApiIdAndDeletedFalse("api-001");
    }

    @Test
    @DisplayName("존재하지 않는 커스텀 API 조회시 예외 발생")
    void getCustomApiById_NotFound() {
        // given
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("nonexistent")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> customApiService.getCustomApiById("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Custom API not found: nonexistent");
    }

    @Test
    @DisplayName("이름으로 커스텀 API 검색 성공")
    void searchCustomApisByName_Success() {
        // given
        List<CustomApi> mockCustomApis = Collections.singletonList(testCustomApi1);
        given(customApiRepository.findByUserIdAndNameContainingAndDeletedFalse("user-123", "날씨")).willReturn(mockCustomApis);

        // when
        List<CustomApiResponseDto> result = customApiService.searchCustomApisByName("user-123", "날씨");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).contains("날씨");
        
        then(customApiRepository).should(times(1)).findByUserIdAndNameContainingAndDeletedFalse("user-123", "날씨");
    }

    @Test
    @DisplayName("커스텀 API 생성 성공")
    void createCustomApi_Success() {
        // given
// 1. 각 외부 API가 받을 파라미터 정보를 생성합니다.
// 1-1. 날씨 API 파라미터
        List<ExternalApiInfoDto.ApiParameter> weatherApiParams = List.of(
                new ExternalApiInfoDto.ApiParameter("city", "string", "날씨를 조회할 도시 이름 (예: 서울)", true)
        );

// 1-2. 미세먼지 API 파라미터
        List<ExternalApiInfoDto.ApiParameter> dustApiParams = List.of(
                new ExternalApiInfoDto.ApiParameter("location", "string", "미세먼지를 조회할 지역 (예: 서울)", true)
        );

// 1-3. 뉴스 API 파라미터
        List<ExternalApiInfoDto.ApiParameter> newsApiParams = List.of(
                new ExternalApiInfoDto.ApiParameter("query", "string", "검색할 키워드", true),
                new ExternalApiInfoDto.ApiParameter("count", "integer", "가져올 뉴스 기사 수 (기본값: 10)", false)
        );

// 2. 테스트에 사용할 외부 API 정보(ExternalApiInfoDto) 목록을 생성합니다.
//    (description 필드와 함께 파라미터 리스트를 전달)
        ExternalApiInfoDto weatherApi = new ExternalApiInfoDto("api-weather-01", "날씨 정보 API",weatherApiParams);
        ExternalApiInfoDto dustApi = new ExternalApiInfoDto("api-dust-01", "미세먼지 정보 API",dustApiParams);
        ExternalApiInfoDto newsApi = new ExternalApiInfoDto("api-news-01", "뉴스 검색 API",newsApiParams);
        List<ExternalApiInfoDto> externalApis = List.of(weatherApi, dustApi, newsApi);

// 3. 테스트의 입력값으로 사용될 InitiateCreationRequest 객체를 생성합니다.
        InitiateCreationRequestDto request = new InitiateCreationRequestDto(
                "auth0|user-test-12345",                                     // userId
                "PRO",                                                       // plan
                "my-custom-weather-api-1",                                   // customApiId
                List.of("weather", "news"),                                  // domains
                List.of("air_quality", "breaking_news"),                     // keywords
                "서울의 현재 날씨와 미세먼지 정보를 확인하고, 관련 최신 뉴스를 보고 싶어." // userQuery
        );

// 4. (필요시) AI 생성 서비스나 Repository가 반환할 것으로 예상되는 객체를 미리 정의합니다.
        CustomApi expectedApi = new CustomApi();
        expectedApi.setName("AI가 생성한 날씨와 뉴스 API");
        expectedApi.setDescription("사용자 요청에 따라 날씨, 미세먼지, 뉴스 정보를 조합하여 제공합니다.");
// ... 기타 필요한 필드 설정

        // when
        CustomApiResponseDto result = AIcustomApiServiceImpl.generateCustomApiWithAi(request);

        // then
        assertThat(result.getCustomApiId()).startsWith("api-");
        assertThat(result.getName()).isEqualTo("새로운 API");
        assertThat(result.getDescription()).isEqualTo("새로운 API 설명");
        assertThat(result.getUserId()).isEqualTo("user-123");
        
        then(customApiRepository).should(times(1)).save(any(CustomApi.class));
    }

    @Test
    @DisplayName("커스텀 API 삭제 성공")
    void deleteCustomApi_Success() {
        // given
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("api-001")).willReturn(Optional.of(testCustomApi1));
        given(customApiRepository.save(any(CustomApi.class))).willReturn(testCustomApi1);

        // when
        customApiService.deleteCustomApi("api-001", "user-123");

        // then
        then(customApiRepository).should(times(1)).findByCustomApiIdAndDeletedFalse("api-001");
        then(customApiRepository).should(times(1)).save(testCustomApi1);
        assertThat(testCustomApi1.getDeleted()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 커스텀 API 삭제시 예외 발생")
    void deleteCustomApi_NotFound() {
        // given
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("nonexistent")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> customApiService.deleteCustomApi("nonexistent", "user-123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Custom API not found: nonexistent");
    }

    @Test
    @DisplayName("다른 사용자의 커스텀 API 삭제시 권한 예외 발생")
    void deleteCustomApi_Unauthorized() {
        // given
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("api-001")).willReturn(Optional.of(testCustomApi1));

        // when & then
        assertThatThrownBy(() -> customApiService.deleteCustomApi("api-001", "other-user"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unauthorized to delete Custom API: api-001");
    }

}