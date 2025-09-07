-- =====================================================
-- 커스텀 API 서비스 스키마 (운영/개발 환경용)
-- =====================================================

-- 커스텀 API의 메타 정보 (Association Table 모델로 변경)
CREATE TABLE IF NOT EXISTS custom_api (
    -- Primary Key
    custom_api_id VARCHAR(36) NOT NULL COMMENT 'PK. 커스텀 API 고유 식별자',
    
    -- Business Fields
    user_id VARCHAR(36) NOT NULL COMMENT '사용자 ID (Auth0 사용자 식별자)',
    name VARCHAR(255) NOT NULL COMMENT '커스텀 API의 이름',
    description TEXT COMMENT '커스텀 API에 대한 상세 설명',
    external_api_url_list_json JSON COMMENT '선별된 외부 API 목록 정보 (JSON 형태)',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'API 활성화 상태',
    ai_plus_active BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'AI+ 기능 활성화 상태',
    is_public BOOLEAN NOT NULL DEFAULT FALSE COMMENT '공개(전체 공유) 여부',
    call_count BIGINT NOT NULL DEFAULT 0 COMMENT '일일 호출 횟수',
    
    -- Audit Fields (BaseEntity)
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시 (자동 설정)',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시 (자동 갱신)',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '소프트 삭제 플래그',
    
    -- Constraints
    PRIMARY KEY (custom_api_id),
    
    -- Indexes
    INDEX idx_user_id (user_id) COMMENT '사용자별 API 조회',
    INDEX idx_deleted (deleted) COMMENT '삭제되지 않은 API 조회',
    INDEX idx_user_id_deleted (user_id, deleted) COMMENT '사용자별 활성 API 조회',
    INDEX idx_is_active (is_active) COMMENT 'API 활성화 상태 조회',
    INDEX idx_is_public (is_public) COMMENT '공개 API 조회',
    INDEX idx_call_count (call_count) COMMENT '호출 횟수 기준 조회'
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='사용자가 AI를 통해 생성한 커스텀 API의 메타데이터를 관리하는 테이블 (순수 API 정보만 저장)';

-- API 공유(가져오기) 관계를 관리하는 Association Table
CREATE TABLE IF NOT EXISTS api_share (
    -- Primary Key
    share_id BIGINT AUTO_INCREMENT COMMENT 'PK. 공유 관계 고유 식별자',
    
    -- Business Fields
    origin_api_id VARCHAR(36) NOT NULL COMMENT '공유된 원본 API ID',
    user_id VARCHAR(36) NOT NULL COMMENT '이 API를 자신의 목록으로 가져간 사용자 ID',
    alias VARCHAR(255) COMMENT '사용자가 지정한 별명 (향후 확장용)',
    
    -- Audit Fields
    shared_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '공유(가져오기) 일시',
    last_used_at DATETIME(6) COMMENT '마지막 사용 일시 (향후 확장용)',
    
    -- Constraints
    PRIMARY KEY (share_id),
    CONSTRAINT uq_origin_api_user UNIQUE (origin_api_id, user_id) COMMENT '한 사용자는 동일한 API를 한 번만 가져올 수 있음',
    
    -- Foreign Keys
    CONSTRAINT fk_share_origin_api FOREIGN KEY (origin_api_id) REFERENCES custom_api(custom_api_id) ON DELETE CASCADE,
    
    -- Indexes
    INDEX idx_share_user_id (user_id) COMMENT '사용자별로 가져온 API 목록 조회',
    INDEX idx_share_origin_api (origin_api_id) COMMENT '원본 API별 공유자 목록 조회'
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='API 공유(가져오기) 관계를 관리하는 테이블 - 어떤 사용자가 어떤 API를 가져갔는지 추적';