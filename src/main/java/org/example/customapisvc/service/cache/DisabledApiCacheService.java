package org.example.customapisvc.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.util.Set;

/**
 * 비활성화된 외부 API 목록을 Redis 캐시에서 관리하는 서비스
 * Redis가 설정된 경우에만 활성화됨
 *
 * 주요 기능:
 * - 비활성화 API 추가/삭제
 * - 비활성화 API 목록 조회
 * - 특정 API의 비활성화 여부 확인
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(StringRedisTemplate.class)
public class DisabledApiCacheService implements DisabledApiCache {

    // Redis에서 비활성화된 API 목록을 저장할 Set의 키
    private static final String DISABLED_APIS_KEY = "disabled_apis";

    private final StringRedisTemplate stringRedisTemplate; // Redis 템플릿
    private SetOperations<String, String> setOperations;// SetOperations 인스턴스

    /**
     * 의존성 주입 후, Redis의 Set 자료구조를 다루는 setOperations를 초기화.
     */
    @PostConstruct
    public void init() {
        this.setOperations = stringRedisTemplate.opsForSet();
        log.info("✅ Redis 연결이 설정되었습니다 - Redis 기반 비활성화 API 캐시 사용");
    }

    /**
     * 비활성화된 모든 API의 ID 목록을 가져옵니다.
     * @return 비활성화된 API ID의 Set
     */
    public Set<String> getDisabledApis() {
        return setOperations.members(DISABLED_APIS_KEY);
    }

}
