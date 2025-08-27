package org.example.customapisvc.service;

import org.example.customapisvc.unit.BaseUnitTest;
import org.example.customapisvc.domain.Entity.ApiType;
import org.example.customapisvc.domain.Entity.CustomApi;
import org.example.customapisvc.repository.CustomApiRepository;
import org.example.customapisvc.service.impl.CustomApiServiceImpl;
import org.example.customapisvc.util.StructuredLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("CustomApiService - Plan Downgrade Tests")
class CustomApiPlanDowngradeServiceTest extends BaseUnitTest {

    @InjectMocks
    private CustomApiServiceImpl customApiService;

    @Mock
    private CustomApiRepository customApiRepository;

    @Mock
    private StructuredLogger structuredLogger;

    @BeforeEach
    void setUp() {
        customApiService = new CustomApiServiceImpl(customApiRepository, structuredLogger, null);
    }

    @Test
    @DisplayName("PRO에서 FREE 다운그레이드 - 6개 API 중 3개(가장 오래된)만 활성화")
    void handlePlanDowngrade_ProToFree_ShouldActivate3OutOf6() {
        // Given
        String userId = "user123";
        String newPlan = "FREE";
        
        List<CustomApi> userApis = createMockCustomApis(userId, 6);
        when(customApiRepository.findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId))
            .thenReturn(userApis);
        when(customApiRepository.updateActiveStatusByCustomApiIds(any(), anyBoolean()))
            .thenReturn(3);

        // When
        customApiService.handlePlanDowngrade(userId, newPlan);

        // Then
        verify(customApiRepository).findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId);
        verify(customApiRepository, times(2)).updateActiveStatusByCustomApiIds(any(), anyBoolean());
    }

    @Test
    @DisplayName("PRO에서 FREE 다운그레이드 - 3개 API 모두 활성화")
    void handlePlanDowngrade_ProToFree_ShouldActivateAll3() {
        // Given
        String userId = "user123";
        String newPlan = "FREE";
        
        List<CustomApi> userApis = createMockCustomApis(userId, 3);
        when(customApiRepository.findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId))
            .thenReturn(userApis);
        when(customApiRepository.updateActiveStatusByCustomApiIds(any(), anyBoolean()))
            .thenReturn(3);

        // When
        customApiService.handlePlanDowngrade(userId, newPlan);

        // Then
        verify(customApiRepository).findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId);
        verify(customApiRepository).updateActiveStatusByCustomApiIds(any(), eq(true));
        verify(customApiRepository, never()).updateActiveStatusByCustomApiIds(any(), eq(false));
    }

    @Test
    @DisplayName("API 개수가 플랜 제한보다 적을 때 - 모든 API 활성화")
    void handlePlanDowngrade_LessApisThanLimit_ShouldActivateAll() {
        // Given
        String userId = "user123";
        String newPlan = "FREE";
        
        List<CustomApi> userApis = createMockCustomApis(userId, 2);
        when(customApiRepository.findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId))
            .thenReturn(userApis);
        when(customApiRepository.updateActiveStatusByCustomApiIds(any(), anyBoolean()))
            .thenReturn(2);

        // When
        customApiService.handlePlanDowngrade(userId, newPlan);

        // Then
        verify(customApiRepository).findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId);
        verify(customApiRepository).updateActiveStatusByCustomApiIds(any(), eq(true));
    }

    @Test
    @DisplayName("API가 없을 때 - 아무 작업 안함")
    void handlePlanDowngrade_NoApis_ShouldDoNothing() {
        // Given
        String userId = "user123";
        String newPlan = "FREE";

        when(customApiRepository.findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId))
            .thenReturn(Collections.emptyList());

        // When
        customApiService.handlePlanDowngrade(userId, newPlan);

        // Then
        verify(customApiRepository).findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId);
        verify(customApiRepository, never()).updateActiveStatusByCustomApiIds(any(), anyBoolean());
    }

    @Test
    @DisplayName("알 수 없는 플랜 - FREE 플랜으로 처리")
    void handlePlanDowngrade_UnknownPlan_ShouldTreatAsFree() {
        // Given
        String userId = "user123";
        String newPlan = "UNKNOWN_PLAN";
        
        List<CustomApi> userApis = createMockCustomApis(userId, 3);
        when(customApiRepository.findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId))
            .thenReturn(userApis);
        when(customApiRepository.updateActiveStatusByCustomApiIds(any(), anyBoolean()))
            .thenReturn(3);

        // When
        customApiService.handlePlanDowngrade(userId, newPlan);

        // Then
        verify(customApiRepository).findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId);
        verify(customApiRepository).updateActiveStatusByCustomApiIds(any(), eq(true));
    }

    @Test
    @DisplayName("null 플랜 - FREE 플랜으로 처리")
    void handlePlanDowngrade_NullPlan_ShouldTreatAsFree() {
        // Given
        String userId = "user123";
        String newPlan = null;
        
        List<CustomApi> userApis = createMockCustomApis(userId, 2);
        when(customApiRepository.findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId))
            .thenReturn(userApis);
        when(customApiRepository.updateActiveStatusByCustomApiIds(any(), anyBoolean()))
            .thenReturn(2);

        // When
        customApiService.handlePlanDowngrade(userId, newPlan);

        // Then
        verify(customApiRepository).findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId);
        verify(customApiRepository).updateActiveStatusByCustomApiIds(any(), eq(true));
    }

    @Test
    @DisplayName("Repository 예외 발생 시 - RuntimeException으로 래핑")
    void handlePlanDowngrade_RepositoryException_ShouldThrowRuntimeException() {
        // Given
        String userId = "user123";
        String newPlan = "FREE";

        when(customApiRepository.findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> customApiService.handlePlanDowngrade(userId, newPlan));
        
        verify(structuredLogger).logError(eq("PLAN_DOWNGRADE_PROCESSING_FAILED"), 
            anyString(), any(RuntimeException.class), any());
    }

    private List<CustomApi> createMockCustomApis(String userId, int count) {
        return Arrays.stream(new int[count])
            .mapToObj(i -> {
                CustomApi api = new CustomApi();
                api.setCustomApiId(UUID.randomUUID().toString());
                api.setUserId(userId);
                api.setName("API " + i);
                api.setCreatedAt(LocalDateTime.now().minusDays(i));
                return api;
            })
            .toList();
    }
}