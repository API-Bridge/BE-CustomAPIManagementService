package org.example.customapisvc.service;

import org.example.customapisvc.domain.Entity.ApiShare;
import org.example.customapisvc.domain.Entity.ApiType;
import org.example.customapisvc.domain.Entity.CustomApi;
import org.example.customapisvc.dto.response.CustomApiResponseDto;
import org.example.customapisvc.event.publisher.EventPublisherService;
import org.example.customapisvc.repository.ApiShareRepository;
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
@DisplayName("커스텀 API 공유 기능 테스트 (Association Table 모델)")
class CustomApiSharingAssociationTableTest {

    @Mock
    private CustomApiRepository customApiRepository;
    
    @Mock
    private ApiShareRepository apiShareRepository;

    @InjectMocks
    private CustomApiServiceImpl customApiService;

    private CustomApi originalApi;

    @BeforeEach
    void setUp() {
        // Association Table 모델: 모든 API는 원본
        originalApi = createTestApi("api-001", "user-123", "원본 API", "원본 설명");
        originalApi.setPublic(false);
    }

    @Test
    @DisplayName("커스텀 API 공유 성공 테스트 (Association Table 모델)")
    void shareCustomApi_Success() {
        // given
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("api-001")).willReturn(Optional.of(originalApi));
        given(customApiRepository.save(any(CustomApi.class))).willReturn(originalApi);
        
        // when
        customApiService.shareCustomApi("api-001", "user-123", true);
        
        // then
        assertThat(originalApi.isPublic()).isTrue();
        then(customApiRepository).should(times(1)).save(originalApi);
    }

    @Test
    @DisplayName("권한 없는 사용자의 API 공유 시도 시 예외 발생")
    void shareCustomApi_UnauthorizedException() {
        // given
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("api-001")).willReturn(Optional.of(originalApi));
        
        // when & then
        assertThatThrownBy(() -> customApiService.shareCustomApi("api-001", "other-user", true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("API 공유 권한이 없습니다");
    }

    @Test
    @DisplayName("공유된 커스텀 API 목록 조회 (Association Table 모델)")
    void getSharedApis() {
        // given
        CustomApi sharedApi1 = createTestApi("api-003", "user-111", "공유 API 1", "설명 1");
        CustomApi sharedApi2 = createTestApi("api-004", "user-222", "공유 API 2", "설명 2");
        sharedApi1.setPublic(true);
        sharedApi2.setPublic(true);
        
        List<CustomApi> mockSharedApis = Arrays.asList(sharedApi1, sharedApi2);
        given(customApiRepository.findByIsPublicTrueAndDeletedFalse()).willReturn(mockSharedApis);
        
        // when
        List<CustomApiResponseDto> result = customApiService.getSharedApis();
        
        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("공유 API 1");
        assertThat(result.get(1).getName()).isEqualTo("공유 API 2");
    }
    
    @Test
    @DisplayName("공유 API 가져오기 성공 (Association Table 모델)")
    void importSharedApi_Success() {
        // given
        originalApi.setPublic(true);
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("api-001")).willReturn(Optional.of(originalApi));
        given(apiShareRepository.existsByOriginApiAndUserId(originalApi, "importer-123")).willReturn(false);
        given(apiShareRepository.save(any(ApiShare.class))).willAnswer(invocation -> {
            ApiShare share = invocation.getArgument(0);
            share.setShareId(1L);
            return share;
        });
        
        // when
        CustomApiResponseDto result = customApiService.importSharedApi("api-001", "importer-123");
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getCustomApiId()).isEqualTo("api-001");
        assertThat(result.getUserId()).isEqualTo("importer-123");
        assertThat(result.getApiType()).isEqualTo(ApiType.LINK);
        assertThat(result.getOriginApiId()).isEqualTo("api-001");
        assertThat(result.getOwnerUserId()).isEqualTo("user-123");
        
        then(apiShareRepository).should(times(1)).save(any(ApiShare.class));
    }
    
    @Test
    @DisplayName("비공개 API 가져오기 시도 시 예외 발생")
    void importSharedApi_NotPublicException() {
        // given
        originalApi.setPublic(false);
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("api-001")).willReturn(Optional.of(originalApi));
        
        // when & then
        assertThatThrownBy(() -> customApiService.importSharedApi("api-001", "importer-123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이 API는 공유되지 않았습니다");
    }
    
    @Test
    @DisplayName("이미 가져온 API 재가져오기 시도 시 예외 발생")
    void importSharedApi_AlreadyImportedException() {
        // given
        originalApi.setPublic(true);
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("api-001")).willReturn(Optional.of(originalApi));
        given(apiShareRepository.existsByOriginApiAndUserId(originalApi, "importer-123")).willReturn(true);
        
        // when & then
        assertThatThrownBy(() -> customApiService.importSharedApi("api-001", "importer-123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 가져온 API입니다");
    }

    @Test
    @DisplayName("자기 자신의 API 가져오기 시도 시 예외 발생")
    void importSharedApi_SelfImportException() {
        // given
        originalApi.setPublic(true);
        given(customApiRepository.findByCustomApiIdAndDeletedFalse("api-001")).willReturn(Optional.of(originalApi));
        
        // when & then
        assertThatThrownBy(() -> customApiService.importSharedApi("api-001", "user-123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("자기 자신의 API는 가져올 수 없습니다");
    }

    private CustomApi createTestApi(String id, String userId, String name, String description) {
        CustomApi api = new CustomApi();
        api.setCustomApiId(id);
        api.setUserId(userId);
        api.setName(name);
        api.setDescription(description);
        api.setIsActive(true);
        api.setCreatedAt(LocalDateTime.now());
        api.setUpdatedAt(LocalDateTime.now());
        return api;
    }
}