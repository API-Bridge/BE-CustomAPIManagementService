package org.example.customapisvc.dto.response;

import org.example.customapisvc.domain.Entity.ApiType;
import org.example.customapisvc.dto.ExternalApiInfoDto;
import org.example.customapisvc.unit.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CustomApiResponseDto 단위 테스트
 */
@DisplayName("CustomApiResponseDto 테스트")
class CustomApiResponseDtoTest extends BaseUnitTest {

    @Test
    @DisplayName("호출 횟수를 포함한 완전한 생성자 테스트")
    void fullConstructorWithCallCount() {
        // given
        String customApiId = "api-123";
        String userId = "user-456";
        String name = "테스트 API";
        String description = "테스트용 API";
        List<ExternalApiInfoDto> externalApis = Collections.emptyList();
        Boolean aiPlusActive = false;
        LocalDateTime now = LocalDateTime.now();
        ApiType apiType = ApiType.ORIGINAL;
        boolean isPublic = false;
        String originApiId = null;
        String ownerUserId = "user-456";
        Long callCount = 42L;

        // when
        CustomApiResponseDto dto = new CustomApiResponseDto(
                customApiId, userId, name, description, externalApis,
                aiPlusActive, now, now, apiType, isPublic, originApiId, ownerUserId, callCount
        );

        // then
        assertThat(dto.getCustomApiId()).isEqualTo(customApiId);
        assertThat(dto.getUserId()).isEqualTo(userId);
        assertThat(dto.getName()).isEqualTo(name);
        assertThat(dto.getDescription()).isEqualTo(description);
        assertThat(dto.getExternalApiUrl_list()).isEqualTo(externalApis);
        assertThat(dto.getAiPlusActive()).isEqualTo(aiPlusActive);
        assertThat(dto.getCreatedAt()).isEqualTo(now);
        assertThat(dto.getUpdatedAt()).isEqualTo(now);
        assertThat(dto.getApiType()).isEqualTo(apiType);
        assertThat(dto.isPublic()).isEqualTo(isPublic);
        assertThat(dto.getOriginApiId()).isEqualTo(originApiId);
        assertThat(dto.getOwnerUserId()).isEqualTo(ownerUserId);
        assertThat(dto.getCallCount()).isEqualTo(callCount);
    }

    @Test
    @DisplayName("기본 생성자와 setter를 통한 호출 횟수 설정 테스트")
    void defaultConstructorWithCallCountSetter() {
        // given
        CustomApiResponseDto dto = new CustomApiResponseDto();
        Long expectedCallCount = 150L;

        // when
        dto.setCallCount(expectedCallCount);

        // then
        assertThat(dto.getCallCount()).isEqualTo(expectedCallCount);
    }

    @Test
    @DisplayName("호출 횟수가 null인 경우 테스트")
    void callCountNullValue() {
        // given
        CustomApiResponseDto dto = new CustomApiResponseDto();

        // when
        dto.setCallCount(null);

        // then
        assertThat(dto.getCallCount()).isNull();
    }
}