package org.example.customapisvc.service;

import org.example.customapisvc.repository.CustomApiRepository;
import org.example.customapisvc.unit.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * ApiCallCountSyncService 단위 테스트
 */
@DisplayName("API 호출 횟수 동기화 서비스 테스트")
class ApiCallCountSyncServiceTest extends BaseUnitTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private CustomApiRepository customApiRepository;

    @InjectMocks
    private ApiCallCountSyncService apiCallCountSyncService;


    @Test
    @DisplayName("Redis 카운트 증가 - 정상 케이스")
    void incrementApiCallCount_Success() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        String customApiId = "test-api-id";
        String expectedKey = "api_call_count:" + customApiId;

        // when
        apiCallCountSyncService.incrementApiCallCount(customApiId);

        // then
        then(valueOperations).should().increment(expectedKey);
    }

    @Test
    @DisplayName("Redis 카운트 조회 - 값이 존재하는 경우")
    void getRedisCallCount_WhenCountExists() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        String customApiId = "test-api-id";
        String expectedKey = "api_call_count:" + customApiId;
        String countValue = "5";
        given(valueOperations.get(expectedKey)).willReturn(countValue);

        // when
        Long result = apiCallCountSyncService.getRedisCallCount(customApiId);

        // then
        assertThat(result).isEqualTo(5L);
    }

    @Test
    @DisplayName("Redis 카운트 조회 - 값이 존재하지 않는 경우")
    void getRedisCallCount_WhenCountNotExists() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        String customApiId = "test-api-id";
        String expectedKey = "api_call_count:" + customApiId;
        given(valueOperations.get(expectedKey)).willReturn(null);

        // when
        Long result = apiCallCountSyncService.getRedisCallCount(customApiId);

        // then
        assertThat(result).isEqualTo(0L);
    }

    @Test
    @DisplayName("모든 카운트 동기화 - Redis에 데이터가 없는 경우")
    void syncAllCallCounts_WhenNoRedisData() {
        // given
        given(stringRedisTemplate.keys("api_call_count:*")).willReturn(null);

        // when
        apiCallCountSyncService.syncAllCallCounts();

        // then
        then(stringRedisTemplate).should().keys("api_call_count:*");
        then(customApiRepository).should(times(0)).incrementCallCount(any(), any());
    }

    @Test
    @DisplayName("모든 카운트 동기화 - Redis에 데이터가 있는 경우")
    void syncAllCallCounts_WhenRedisDataExists() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        String key1 = "api_call_count:api-1";
        String key2 = "api_call_count:api-2";
        Set<String> keys = Set.of(key1, key2);
        
        given(stringRedisTemplate.keys("api_call_count:*")).willReturn(keys);
        given(valueOperations.get(key1)).willReturn("3");
        given(valueOperations.get(key2)).willReturn("7");
        given(customApiRepository.incrementCallCount("api-1", 3L)).willReturn(1);
        given(customApiRepository.incrementCallCount("api-2", 7L)).willReturn(1);

        // when
        apiCallCountSyncService.syncAllCallCounts();

        // then
        then(customApiRepository).should().incrementCallCount("api-1", 3L);
        then(customApiRepository).should().incrementCallCount("api-2", 7L);
        then(stringRedisTemplate).should().delete(key1);
        then(stringRedisTemplate).should().delete(key2);
    }

    @Test
    @DisplayName("모든 카운트 동기화 - 카운트가 0인 경우")
    void syncAllCallCounts_WhenCountIsZero() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        String key = "api_call_count:api-1";
        Set<String> keys = Set.of(key);
        
        given(stringRedisTemplate.keys("api_call_count:*")).willReturn(keys);
        given(valueOperations.get(key)).willReturn("0");

        // when
        apiCallCountSyncService.syncAllCallCounts();

        // then
        then(stringRedisTemplate).should().keys("api_call_count:*");
        then(valueOperations).should().get(key);
        then(customApiRepository).should(times(0)).incrementCallCount(any(), any());
        then(stringRedisTemplate).should(times(0)).delete((String) any());
    }

    @Test
    @DisplayName("모든 카운트 동기화 - DB 업데이트 실패")
    void syncAllCallCounts_WhenDatabaseUpdateFails() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        String key = "api_call_count:api-1";
        Set<String> keys = Set.of(key);
        
        given(stringRedisTemplate.keys("api_call_count:*")).willReturn(keys);
        given(valueOperations.get(key)).willReturn("5");
        given(customApiRepository.incrementCallCount("api-1", 5L)).willReturn(0); // 실패 시나리오

        // when
        apiCallCountSyncService.syncAllCallCounts();

        // then
        then(stringRedisTemplate).should().keys("api_call_count:*");
        then(valueOperations).should().get(key);
        then(customApiRepository).should().incrementCallCount("api-1", 5L);
        then(stringRedisTemplate).should(times(0)).delete((String) any()); // 실패 시 Redis 키 삭제 안됨
    }
}