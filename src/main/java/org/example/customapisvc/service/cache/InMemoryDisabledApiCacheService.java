package org.example.customapisvc.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis가 없을 때 사용하는 인메모리 기반 비활성화 API 캐시 서비스
 * Redis가 설정되지 않은 경우 대체 구현체로 사용됨
 */
@Slf4j
@Service
@ConditionalOnMissingBean(StringRedisTemplate.class)
public class InMemoryDisabledApiCacheService implements DisabledApiCache {
    
    // 인메모리 저장소 (Thread-safe)
    private final Set<String> disabledApis = ConcurrentHashMap.newKeySet();
    
    /**
     * 비활성화된 모든 API의 ID 목록을 가져옵니다.
     * @return 비활성화된 API ID의 Set
     */
    public Set<String> getDisabledApis() {
        log.debug("Getting disabled APIs from in-memory cache. Count: {}", disabledApis.size());
        return new HashSet<>(disabledApis);
    }
    
    /**
     * 특정 API가 비활성화되었는지 확인.
     * @param apiId 확인할 API의 ID
     * @return 비활성화 여부 (true: 비활성화됨)
     */
    public boolean isApiDisabled(String apiId) {
        boolean disabled = disabledApis.contains(apiId);
        log.debug("Checking if API {} is disabled: {}", apiId, disabled);
        return disabled;
    }

}