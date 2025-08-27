package org.example.customapisvc.scheduler;

import org.example.customapisvc.repository.CustomApiRepository;
import org.example.customapisvc.service.ApiCallCountSyncService;
import org.example.customapisvc.unit.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;

/**
 * ApiCallCountResetScheduler 단위 테스트
 */
@DisplayName("API 호출 횟수 일일 초기화 스케줄러 테스트")
class ApiCallCountResetSchedulerTest extends BaseUnitTest {

    @Mock
    private ApiCallCountSyncService apiCallCountSyncService;

    @Mock
    private CustomApiRepository customApiRepository;

    @InjectMocks
    private ApiCallCountResetScheduler scheduler;

    @Test
    @DisplayName("일일 초기화 - 정상 실행")
    void resetDailyApiCallCounts_Success() {
        // given
        given(customApiRepository.resetAllCallCounts()).willReturn(10);

        // when
        scheduler.resetDailyApiCallCounts();

        // then
        then(apiCallCountSyncService).should().syncAllCallCounts();
        then(customApiRepository).should().resetAllCallCounts();
    }

    @Test
    @DisplayName("일일 초기화 - 동기화 단계에서 예외 발생")
    void resetDailyApiCallCounts_SyncException() {
        // given
        doThrow(new RuntimeException("Redis 연결 실패"))
            .when(apiCallCountSyncService).syncAllCallCounts();

        // when & then
        try {
            scheduler.resetDailyApiCallCounts();
        } catch (RuntimeException e) {
            // 예외 발생이 예상됨
        }

        then(apiCallCountSyncService).should().syncAllCallCounts();
        // DB 초기화는 실행되지 않아야 함
        then(customApiRepository).should(times(0)).resetAllCallCounts();
    }

    @Test
    @DisplayName("일일 초기화 - DB 리셋 단계에서 예외 발생")
    void resetDailyApiCallCounts_ResetException() {
        // given
        doThrow(new RuntimeException("DB 연결 실패"))
            .when(customApiRepository).resetAllCallCounts();

        // when & then
        try {
            scheduler.resetDailyApiCallCounts();
        } catch (RuntimeException e) {
            // 예외 발생이 예상됨
        }

        then(apiCallCountSyncService).should().syncAllCallCounts();
        then(customApiRepository).should().resetAllCallCounts();
    }

    @Test
    @DisplayName("수동 초기화 - 정상 실행")
    void manualReset_Success() {
        // given
        given(customApiRepository.resetAllCallCounts()).willReturn(5);

        // when
        scheduler.manualReset();

        // then
        then(apiCallCountSyncService).should().syncAllCallCounts();
        then(customApiRepository).should().resetAllCallCounts();
    }
}