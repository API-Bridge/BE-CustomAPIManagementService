-- =====================================================
-- 커스텀 API 서비스 스키마 (테스트 환경용 - H2)
-- =====================================================

-- 커스텀 API의 메타 정보
CREATE TABLE IF NOT EXISTS custom_api (
    -- Primary Key
    custom_api_id VARCHAR(36) NOT NULL,
    
    -- Business Fields
    user_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    external_api_url_list_json TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ai_plus_active BOOLEAN NOT NULL DEFAULT FALSE,
    api_type VARCHAR(10) NOT NULL DEFAULT 'ORIGINAL',
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    origin_api_id VARCHAR(36),
    call_count BIGINT NOT NULL DEFAULT 0,
    
    -- Audit Fields (BaseEntity)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Constraints
    PRIMARY KEY (custom_api_id)
);

-- Indexes for Query Performance
CREATE INDEX idx_user_id ON custom_api (user_id);
CREATE INDEX idx_deleted ON custom_api (deleted);  
CREATE INDEX idx_user_id_deleted ON custom_api (user_id, deleted);
CREATE INDEX idx_is_active ON custom_api (is_active);
CREATE INDEX idx_api_type ON custom_api (api_type);
CREATE INDEX idx_is_public ON custom_api (is_public);
CREATE INDEX idx_origin_api_id ON custom_api (origin_api_id);
CREATE INDEX idx_call_count ON custom_api (call_count);