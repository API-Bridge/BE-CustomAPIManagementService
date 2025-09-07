package org.example.customapisvc.domain.Entity;

/**
 * @deprecated Association Table 모델로 마이그레이션됨에 따라 더 이상 사용되지 않습니다.
 * API 공유 관계는 ApiShare 엔티티와 api_share 테이블에서 관리됩니다.
 * 
 * 호환성을 위해 DTO에서는 계속 사용되지만, 엔티티 레벨에서는 제거될 예정입니다.
 * 
 * @see org.example.customapisvc.domain.Entity.ApiShare
 */
@Deprecated
public enum ApiType {
    ORIGINAL, // 직접 생성한 원본 API (Association Table 모델에서는 모든 API가 원본)
    LINK      // 다른 사용자의 API를 가져온 연결된 API (현재는 런타임에 계산됨)
}