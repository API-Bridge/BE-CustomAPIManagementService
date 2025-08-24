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
    
    public InMemoryDisabledApiCacheService() {
        log.warn("⚠️  Redis를 사용할 수 없습니다 - 인메모리 캐시로 비활성화 API 관리 (서버 재시작 시 데이터 손실)");
    }
    
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

}