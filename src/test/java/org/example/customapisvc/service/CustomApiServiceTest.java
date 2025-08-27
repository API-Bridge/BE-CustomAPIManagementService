package org.example.customapisvc.service;

import org.example.customapisvc.domain.Entity.ApiType;
import org.example.customapisvc.domain.Entity.CustomApi;
import org.example.customapisvc.dto.ExternalApiInfoDto;
import org.example.customapisvc.dto.request.InitiateCreationRequestDto;
import org.example.customapisvc.dto.response.CustomApiResponseDto;
import org.example.customapisvc.event.publisher.EventPublisherService;
import org.example.customapisvc.repository.CustomApiRepository;
import org.example.customapisvc.service.impl.AiCustomApiGenerationServiceImpl;
import org.example.customapisvc.service.impl.CustomApiServiceImpl;
import org.example.customapisvc.testdata.TestDataFactory;
import org.example.customapisvc.util.StructuredLogger;
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
    
    @Mock
    private StructuredLogger structuredLogger;
    
    @Mock
    private EventPublisherService eventPublisherService;

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
                new ExternalApiInfoDto.ApiParameter("city-param", "city", "string", true, "날씨를 조회할 도시 이름 (예: 서울)", null)
        );

// 1-2. 미세먼지 API 파라미터
        List<ExternalApiInfoDto.ApiParameter> dustApiParams = List.of(
                new ExternalApiInfoDto.ApiParameter("location-param", "location", "string", true, "미세먼지를 조회할 지역 (예: 서울)", null)
        );

// 1-3. 뉴스 API 파라미터
        List<ExternalApiInfoDto.ApiParameter> newsApiParams = List.of(
                new ExternalApiInfoDto.ApiParameter("query-param", "query", "string", true, "검색할 키워드", null),
                new ExternalApiInfoDto.ApiParameter("count-param", "count", "integer", false, "가져올 뉴스 기사 수 (기본값: 10)", "10")
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

    // =============== 공유 기능 테스트 ===============
    
    @Test
    @DisplayName("커스텀 API 공유 설정 성공")
    void shareCustomApi_Success() {
        // given
        CustomApi originalApi = TestDataFactory.CustomApiTestData.createCustomApi("api-001", "user-123", "날씨 API", "날씨 정보 API");
        originalApi.setApiType(ApiType.ORIGINAL);
        originalApi.setPublic(false);
        
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("api-001")).willReturn(Optional.of(originalApi));
        given(customApiRepository.save(any(CustomApi.class))).willReturn(originalApi);

        // when
        customApiService.shareCustomApi("api-001", "user-123", true);

        // then
        then(customApiRepository).should(times(1)).save(originalApi);
        assertThat(originalApi.isPublic()).isTrue();
    }

    @Test
    @DisplayName("LINK 타입 API 공유시 예외 발생")
    void shareCustomApi_LinkTypeException() {
        // given
        CustomApi linkApi = TestDataFactory.CustomApiTestData.createCustomApi("api-002", "user-456", "링크 API", "가져온 API");
        linkApi.setApiType(ApiType.LINK);
        
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("api-002")).willReturn(Optional.of(linkApi));

        // when & then
        assertThatThrownBy(() -> customApiService.shareCustomApi("api-002", "user-456", true))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("원본 API만 공유할 수 있습니다.");
    }

    @Test
    @DisplayName("공유된 API 목록 조회 성공")
    void getSharedApis_Success() {
        // given
        CustomApi sharedApi1 = TestDataFactory.CustomApiTestData.createCustomApi("api-001", "user-123", "공유 API 1", "설명 1");
        CustomApi sharedApi2 = TestDataFactory.CustomApiTestData.createCustomApi("api-002", "user-456", "공유 API 2", "설명 2");
        sharedApi1.setApiType(ApiType.ORIGINAL);
        sharedApi2.setApiType(ApiType.ORIGINAL);
        sharedApi1.setPublic(true);
        sharedApi2.setPublic(true);
        
        List<CustomApi> mockSharedApis = Arrays.asList(sharedApi1, sharedApi2);
        given(customApiRepository.findByIsPublicTrueAndApiTypeAndDeletedFalse(ApiType.ORIGINAL)).willReturn(mockSharedApis);

        // when
        List<CustomApiResponseDto> result = customApiService.getSharedApis();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCustomApiId()).isEqualTo("api-001");
        assertThat(result.get(1).getCustomApiId()).isEqualTo("api-002");
    }

    @Test
    @DisplayName("공유 API 가져오기 성공")
    void importSharedApi_Success() {
        // given
        CustomApi originalApi = TestDataFactory.CustomApiTestData.createCustomApi("api-origin", "user-123", "원본 API", "원본 설명");
        originalApi.setApiType(ApiType.ORIGINAL);
        originalApi.setPublic(true);
        
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("api-origin")).willReturn(Optional.of(originalApi));
        given(customApiRepository.existsByOriginApiAndUserIdAndDeletedFalse(originalApi, "user-456")).willReturn(false);
        given(customApiRepository.save(any(CustomApi.class))).willAnswer(invocation -> {
            CustomApi savedApi = invocation.getArgument(0);
            savedApi.setCustomApiId("api-link-new");
            return savedApi;
        });

        // when
        CustomApiResponseDto result = customApiService.importSharedApi("api-origin", "user-456");

        // then
        assertThat(result.getCustomApiId()).isEqualTo("api-link-new");
        assertThat(result.getUserId()).isEqualTo("user-456");
        assertThat(result.getApiType()).isEqualTo(ApiType.LINK);
        then(customApiRepository).should(times(1)).save(any(CustomApi.class));
    }

    @Test
    @DisplayName("이미 가져온 API 중복 가져오기시 예외 발생")
    void importSharedApi_AlreadyImported() {
        // given
        CustomApi originalApi = TestDataFactory.CustomApiTestData.createCustomApi("api-origin", "user-123", "원본 API", "원본 설명");
        originalApi.setApiType(ApiType.ORIGINAL);
        originalApi.setPublic(true);
        
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("api-origin")).willReturn(Optional.of(originalApi));
        given(customApiRepository.existsByOriginApiAndUserIdAndDeletedFalse(originalApi, "user-456")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> customApiService.importSharedApi("api-origin", "user-456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이미 가져온 API입니다.");
    }

    @Test
    @DisplayName("자기 자신의 API 가져오기시 예외 발생")
    void importSharedApi_SelfImport() {
        // given
        CustomApi originalApi = TestDataFactory.CustomApiTestData.createCustomApi("api-origin", "user-123", "원본 API", "원본 설명");
        originalApi.setApiType(ApiType.ORIGINAL);
        originalApi.setPublic(true);
        
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("api-origin")).willReturn(Optional.of(originalApi));

        // when & then
        assertThatThrownBy(() -> customApiService.importSharedApi("api-origin", "user-123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("자기 자신의 API는 가져올 수 없습니다.");
    }

}