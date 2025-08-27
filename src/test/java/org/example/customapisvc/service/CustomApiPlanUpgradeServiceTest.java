package org.example.customapisvc.service;

import org.example.customapisvc.unit.BaseUnitTest;
import org.example.customapisvc.domain.Entity.ApiType;
import org.example.customapisvc.domain.Entity.CustomApi;
import org.example.customapisvc.repository.CustomApiRepository;
import org.example.customapisvc.service.impl.CustomApiServiceImpl;
import org.example.customapisvc.util.StructuredLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("CustomApiService - Plan Upgrade Tests")
class CustomApiPlanUpgradeServiceTest extends BaseUnitTest {

    @InjectMocks
    private CustomApiServiceImpl customApiService;

    @Mock
    private CustomApiRepository customApiRepository;

    @Mock
    private StructuredLogger structuredLogger;

    @Test
    @DisplayName("플랜 업그레이드 - FREE에서 PRO로, 3개 API 모두 활성화")
    void handlePlanUpgrade_FreeToProWith3Apis_ShouldActivateAll() {
        // Given
        String userId = "user123";
        String newPlan = "PRO";
        List<CustomApi> userApis = Arrays.asList(
            createMockCustomApi("api1", userId, false),
            createMockCustomApi("api2", userId, true),
            createMockCustomApi("api3", userId, false)
        );

        when(customApiRepository.findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId))
            .thenReturn(userApis);
        when(customApiRepository.updateActiveStatusByCustomApiIds(any(), eq(true)))
            .thenReturn(3);

        // When
        assertDoesNotThrow(() -> customApiService.handlePlanUpgrade(userId, newPlan));

        // Then
        verify(customApiRepository).findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId);
        verify(customApiRepository).updateActiveStatusByCustomApiIds(
            Arrays.asList("api1", "api2", "api3"), true);
        verify(structuredLogger).logBusinessEvent(eq("PLAN_UPGRADE_ALL_ACTIVATED"), 
            anyString(), any());
    }

    @Test
    @DisplayName("플랜 업그레이드 - FREE에서 PRO로, 6개 API 중 5개만 활성화")
    void handlePlanUpgrade_FreeToProWith6Apis_ShouldActivate5() {
        // Given
        String userId = "user123";
        String newPlan = "PRO";
        List<CustomApi> userApis = Arrays.asList(
            createMockCustomApi("api1", userId, false),
            createMockCustomApi("api2", userId, false),
            createMockCustomApi("api3", userId, false),
            createMockCustomApi("api4", userId, false),
            createMockCustomApi("api5", userId, false),
            createMockCustomApi("api6", userId, false)
        );

        when(customApiRepository.findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId))
            .thenReturn(userApis);
        when(customApiRepository.updateActiveStatusByCustomApiIds(any(), eq(true)))
            .thenReturn(5);

        // When
        assertDoesNotThrow(() -> customApiService.handlePlanUpgrade(userId, newPlan));

        // Then
        verify(customApiRepository).findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId);
        verify(customApiRepository).updateActiveStatusByCustomApiIds(
            Arrays.asList("api1", "api2", "api3", "api4", "api5"), true);
        verify(structuredLogger).logBusinessEvent(eq("PLAN_UPGRADE_LIMITED_ACTIVATION"), 
            anyString(), any());
    }

    @Test
    @DisplayName("플랜 업그레이드 - API가 없는 경우 아무 처리 없음")
    void handlePlanUpgrade_NoApis_ShouldDoNothing() {
        // Given
        String userId = "user123";
        String newPlan = "PRO";

        when(customApiRepository.findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId))
            .thenReturn(Collections.emptyList());

        // When
        assertDoesNotThrow(() -> customApiService.handlePlanUpgrade(userId, newPlan));

        // Then
        verify(customApiRepository).findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId);
        verify(customApiRepository, never()).updateActiveStatusByCustomApiIds(any(), anyBoolean());
        verify(structuredLogger).logBusinessEvent(eq("PLAN_UPGRADE_NO_APIS"), anyString(), any());
    }

    @Test
    @DisplayName("플랜 업그레이드 - 알 수 없는 플랜은 FREE로 처리")
    void handlePlanUpgrade_UnknownPlan_ShouldTreatAsFree() {
        // Given
        String userId = "user123";
        String newPlan = "UNKNOWN";
        List<CustomApi> userApis = Arrays.asList(
            createMockCustomApi("api1", userId, false)
        );

        when(customApiRepository.findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId))
            .thenReturn(userApis);
        when(customApiRepository.updateActiveStatusByCustomApiIds(any(), eq(true)))
            .thenReturn(1);

        // When
        assertDoesNotThrow(() -> customApiService.handlePlanUpgrade(userId, newPlan));

        // Then
        verify(customApiRepository).updateActiveStatusByCustomApiIds(
            Arrays.asList("api1"), true);
    }

    @Test
    @DisplayName("플랜 업그레이드 - null 플랜은 FREE로 처리")
    void handlePlanUpgrade_NullPlan_ShouldTreatAsFree() {
        // Given
        String userId = "user123";
        String newPlan = null;
        List<CustomApi> userApis = Arrays.asList(
            createMockCustomApi("api1", userId, false)
        );

        when(customApiRepository.findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId))
            .thenReturn(userApis);
        when(customApiRepository.updateActiveStatusByCustomApiIds(any(), eq(true)))
            .thenReturn(1);

        // When
        assertDoesNotThrow(() -> customApiService.handlePlanUpgrade(userId, newPlan));

        // Then
        verify(customApiRepository).updateActiveStatusByCustomApiIds(
            Arrays.asList("api1"), true);
    }

    @Test
    @DisplayName("플랜 업그레이드 - Repository 예외 시 RuntimeException 발생")
    void handlePlanUpgrade_RepositoryException_ShouldThrowRuntimeException() {
        // Given
        String userId = "user123";
        String newPlan = "PRO";

        when(customApiRepository.findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        assertThrows(RuntimeException.class, 
            () -> customApiService.handlePlanUpgrade(userId, newPlan));
        
        verify(structuredLogger).logError(eq("PLAN_UPGRADE_PROCESSING_FAILED"), 
            anyString(), any(RuntimeException.class), any());
    }

    private CustomApi createMockCustomApi(String customApiId, String userId, boolean isActive) {
        CustomApi customApi = new CustomApi();
        customApi.setCustomApiId(customApiId);
        customApi.setUserId(userId);
        customApi.setName("Test API " + customApiId);
        customApi.setDescription("Test Description");
        customApi.setApiType(ApiType.ORIGINAL);
        customApi.setIsActive(isActive);
        customApi.setDeleted(false);
        customApi.setCreatedAt(LocalDateTime.now());
        return customApi;
    }
}