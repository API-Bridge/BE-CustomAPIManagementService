package org.example.customapisvc.service.cache;

import java.util.Set;

/**
 * 비활성화된 API 캐시 인터페이스
 * Redis 구현체와 인메모리 구현체가 공통으로 구현하는 인터페이스
 */
public interface DisabledApiCache {
    
    /**
     * 비활성화된 모든 API의 ID 목록을 가져옵니다.
     * @return 비활성화된 API ID의 Set
     */
    Set<String> getDisabledApis();

}