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
    external_api_name JSON NULL COMMENT '선별된 외부 API 목록 정보 (JSON 형태)',
    
    -- Audit Fields (BaseEntity)
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시 (자동 설정)',
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시 (자동 갱신)',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '소프트 삭제 플래그',
    
    -- Constraints
    PRIMARY KEY (custom_api_id),
    
    -- Indexes for Query Performance
    INDEX idx_user_id (user_id) COMMENT '사용자별 API 조회',
    INDEX idx_deleted (deleted) COMMENT '삭제되지 않은 API 조회',
    INDEX idx_user_id_deleted (user_id, deleted) COMMENT '사용자별 활성 API 조회 (복합 인덱스)'
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='사용자가 AI를 통해 생성한 커스텀 API의 메타데이터를 관리하는 테이블';