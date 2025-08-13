package org.example.customapisvc.service.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Set;

/**
 * 비활성화된 외부 API 목록을 Redis 캐시에서 관리하는 서비스
 *
 * 주요 기능:
 * - 비활성화 API 추가/삭제
 * - 비활성화 API 목록 조회
 * - 특정 API의 비활성화 여부 확인
 */
@Service
@RequiredArgsConstructor
public class DisabledApiCacheService {

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
    }

    /**
     * 특정 API를 비활성화 목록에 추가. (현재는 필요없음)
     * @param apiId 비활성화할 API의 ID
     */
    public void addDisabledApi(String apiId) {
        setOperations.add(DISABLED_APIS_KEY, apiId);
    }

    /**
     * 비활성화 목록에서 특정 API를 제거. (현재는 필요없음)
     * @param apiId 제거할 API의 ID
     */
    public void removeDisabledApi(String apiId) {
        setOperations.remove(DISABLED_APIS_KEY, apiId);
    }

    /**
     * 비활성화된 모든 API의 ID 목록을 가져옵니다.
     * @return 비활성화된 API ID의 Set
     */
    public Set<String> getDisabledApis() {
        return setOperations.members(DISABLED_APIS_KEY);
    }

    /**
     * 특정 API가 비활성화되었는지 확인.
     * @param apiId 확인할 API의 ID
     * @return 비활성화 여부 (true: 비활성화됨)
     */
    public boolean isApiDisabled(String apiId) {
        return setOperations.isMember(DISABLED_APIS_KEY, apiId);
    }
}
