package org.example.customapisvc.service;

import org.example.customapisvc.domain.Entity.ApiType;
import org.example.customapisvc.domain.Entity.CustomApi;
import org.example.customapisvc.dto.response.CustomApiResponseDto;
import org.example.customapisvc.event.publisher.EventPublisherService;
import org.example.customapisvc.repository.CustomApiRepository;
import org.example.customapisvc.service.impl.CustomApiServiceImpl;
import org.example.customapisvc.util.StructuredLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("커스텀 API 공유 기능 테스트")
class CustomApiSharingServiceTest {

    @Mock
    private CustomApiRepository customApiRepository;

    @InjectMocks
    private CustomApiServiceImpl customApiService;

    private CustomApi originalApi;
    private CustomApi linkApi;

    @BeforeEach
    void setUp() {
        originalApi = createTestApi("api-001", "user-123", "원본 API", "원본 설명");
        originalApi.setApiType(ApiType.ORIGINAL);
        originalApi.setPublic(false);
        
        linkApi = createTestApi("api-002", "user-456", "링크 API", "가져온 API");
        linkApi.setApiType(ApiType.LINK);
        linkApi.setOriginApi(originalApi);
    }

    @Test
    @DisplayName("커스텀 API 공유 설정 성공")
    void shareCustomApi_Success() {
        // given
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("api-001")).willReturn(Optional.of(originalApi));
        given(customApiRepository.save(any(CustomApi.class))).willReturn(originalApi);

        // when
        customApiService.shareCustomApi("api-001", "user-123", true);

        // then
        then(customApiRepository).should(times(1)).save(originalApi);
        assertThat(originalApi.isPublic()).isTrue();
    }

    @Test
    @DisplayName("다른 사용자의 API 공유시 권한 예외 발생")
    void shareCustomApi_UnauthorizedException() {
        // given
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("api-001")).willReturn(Optional.of(originalApi));

        // when & then
        assertThatThrownBy(() -> customApiService.shareCustomApi("api-001", "other-user", true))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("API 공유 권한이 없습니다.");
    }

    @Test
    @DisplayName("LINK 타입 API 공유시 예외 발생")
    void shareCustomApi_LinkTypeException() {
        // given
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
        CustomApi sharedApi1 = createTestApi("api-003", "user-111", "공유 API 1", "설명 1");
        CustomApi sharedApi2 = createTestApi("api-004", "user-222", "공유 API 2", "설명 2");
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
        assertThat(result.get(0).getCustomApiId()).isEqualTo("api-003");
        assertThat(result.get(1).getCustomApiId()).isEqualTo("api-004");
        assertThat(result.get(0).isPublic()).isTrue();
        assertThat(result.get(1).isPublic()).isTrue();
    }

    @Test
    @DisplayName("공유 API 가져오기 성공")
    void importSharedApi_Success() {
        // given
        originalApi.setPublic(true);
        
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("api-001")).willReturn(Optional.of(originalApi));
        given(customApiRepository.existsByOriginApiAndUserIdAndDeletedFalse(originalApi, "user-456")).willReturn(false);
        given(customApiRepository.save(any(CustomApi.class))).willAnswer(invocation -> {
            CustomApi savedApi = invocation.getArgument(0);
            savedApi.setCustomApiId("api-link-new");
            return savedApi;
        });

        // when
        CustomApiResponseDto result = customApiService.importSharedApi("api-001", "user-456");

        // then
        assertThat(result.getCustomApiId()).isEqualTo("api-link-new");
        assertThat(result.getUserId()).isEqualTo("user-456");
        assertThat(result.getApiType()).isEqualTo(ApiType.LINK);
        assertThat(result.getOriginApiId()).isEqualTo("api-001");
        assertThat(result.getOwnerUserId()).isEqualTo("user-123");
        then(customApiRepository).should(times(1)).save(any(CustomApi.class));
    }

    @Test
    @DisplayName("공개되지 않은 API 가져오기시 예외 발생")
    void importSharedApi_NotPublicException() {
        // given
        originalApi.setPublic(false); // 공개되지 않은 API
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("api-001")).willReturn(Optional.of(originalApi));

        // when & then
        assertThatThrownBy(() -> customApiService.importSharedApi("api-001", "user-456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이 API는 공유되지 않았거나 원본 API가 아닙니다.");
    }

    @Test
    @DisplayName("이미 가져온 API 중복 가져오기시 예외 발생")
    void importSharedApi_AlreadyImported() {
        // given
        originalApi.setPublic(true);
        
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("api-001")).willReturn(Optional.of(originalApi));
        given(customApiRepository.existsByOriginApiAndUserIdAndDeletedFalse(originalApi, "user-456")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> customApiService.importSharedApi("api-001", "user-456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이미 가져온 API입니다.");
    }

    @Test
    @DisplayName("자기 자신의 API 가져오기시 예외 발생")
    void importSharedApi_SelfImport() {
        // given
        originalApi.setPublic(true);
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("api-001")).willReturn(Optional.of(originalApi));

        // when & then
        assertThatThrownBy(() -> customApiService.importSharedApi("api-001", "user-123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("자기 자신의 API는 가져올 수 없습니다.");
    }

    private CustomApi createTestApi(String apiId, String userId, String name, String description) {
        CustomApi api = new CustomApi();
        api.setCustomApiId(apiId);
        api.setUserId(userId);
        api.setName(name);
        api.setDescription(description);
        api.setIsActive(true);
        api.setAiPlusActive(false);
        api.setApiType(ApiType.ORIGINAL);
        api.setPublic(false);
        api.setDeleted(false);
        api.setCreatedAt(LocalDateTime.now());
        api.setUpdatedAt(LocalDateTime.now());
        return api;
    }
}