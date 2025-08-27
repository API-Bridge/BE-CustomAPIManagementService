-- =====================================================
-- 커스텀 API 서비스 스키마
-- =====================================================

-- 커스텀 API의 메타 정보
CREATE TABLE IF NOT EXISTS custom_api (
    -- Primary Key
    custom_api_id VARCHAR(36) NOT NULL COMMENT 'PK. 커스텀 API 고유 식별자',
    
    -- Business Fields
    user_id VARCHAR(36) NOT NULL COMMENT '사용자 ID (Auth0 사용자 식별자)',
    name VARCHAR(255) NOT NULL COMMENT '커스텀 API의 이름',
    description TEXT NULL COMMENT '커스텀 API에 대한 상세 설명',
    external_api_url_list_json JSON NULL COMMENT '선별된 외부 API 목록 정보 (JSON 형태)',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'API 활성화 상태',
    ai_plus_active BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'AI+ 기능 활성화 상태',
    api_type ENUM('ORIGINAL', 'LINK') NOT NULL DEFAULT 'ORIGINAL' COMMENT 'API 타입 (ORIGINAL: 직접 생성, LINK: 가져온 API)',
    is_public BOOLEAN NOT NULL DEFAULT FALSE COMMENT '공개(공유) 여부',
    origin_api_id VARCHAR(36) NULL COMMENT '원본 API ID (LINK 타입인 경우)',
    
    -- Audit Fields (BaseEntity)
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시 (자동 설정)',
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시 (자동 갱신)',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '소프트 삭제 플래그',
    
    -- Constraints
    PRIMARY KEY (custom_api_id),
    
    -- Foreign Keys
    FOREIGN KEY (origin_api_id) REFERENCES custom_api(custom_api_id) ON DELETE SET NULL COMMENT '원본 API 참조',
    
    -- Indexes for Query Performance
    INDEX idx_user_id (user_id) COMMENT '사용자별 API 조회',
    INDEX idx_deleted (deleted) COMMENT '삭제되지 않은 API 조회',
    INDEX idx_user_id_deleted (user_id, deleted) COMMENT '사용자별 활성 API 조회 (복합 인덱스)',
    INDEX idx_is_active (is_active) COMMENT 'API 활성화 상태 조회',
    INDEX idx_api_type (api_type) COMMENT 'API 타입별 조회',
    INDEX idx_is_public (is_public) COMMENT '공개 API 조회',
    INDEX idx_origin_api_id (origin_api_id) COMMENT '원본 API 기준 조회'
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='사용자가 AI를 통해 생성한 커스텀 API의 메타데이터를 관리하는 테이블';