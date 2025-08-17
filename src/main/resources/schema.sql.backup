-- =====================================================
-- 커스텀 API 서비스 (Custom API Service) 데이터베이스 스키마
-- =====================================================

CREATE DATABASE IF NOT EXISTS custom_api_service_db
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE custom_api_service_db;

-- 커스텀 API의 메타 정보
CREATE TABLE custom_api (
                            custom_api_id VARCHAR(36) NOT NULL COMMENT 'PK. 커스텀 API 고유 식별자',
                            user_id VARCHAR(36) NOT NULL COMMENT '사용자 ID (다른 서비스의 user_id를 참조하지만 FK 제약조건 없음)',
                            name VARCHAR(255) NOT NULL COMMENT '커스텀 API의 이름 (예: "고객 위치 기반 날씨 및 구매내역 조회")',
                            description TEXT NULL COMMENT '커스텀 API에 대한 설명',
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
                            PRIMARY KEY (custom_api_id),
                            INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='사용자가 생성한 커스텀 API의 메타 정보를 관리합니다.';

-- 커스텀 API의 핵심 로직인 호출 순서와 병렬 처리 정의
CREATE TABLE workflow_step (
                               step_id VARCHAR(36) NOT NULL COMMENT 'PK. 워크플로우 단계의 고유 식별자',
                               custom_api_id VARCHAR(36) NOT NULL COMMENT '커스텀 API ID (custom_api 테이블 참조)',
                               external_api_id VARCHAR(36) NOT NULL COMMENT '호출할 외부 API 식별자 (API 관리 서비스의 external_api.api_id)',
                               execution_group INT NOT NULL COMMENT '호출 순서 및 병렬 처리의 핵심. 낮은 그룹부터 순서대로 실행되며, 같은 그룹은 병렬 처리 가능.',
                               alias VARCHAR(255) NOT NULL COMMENT '이 워크플로우 내에서 사용할 단계의 별칭 (예: "사용자정보조회", "날씨조회")',
                               PRIMARY KEY (step_id),
                               INDEX idx_custom_api_id (custom_api_id),
                               INDEX idx_external_api_id (external_api_id),
                               INDEX idx_execution_group (execution_group)
    -- MSA 환경에서는 외래키 제약조건 제거
    -- 서비스 간 참조는 이벤트와 API 호출을 통해 처리
    -- CONSTRAINT fk_workflow_steps_to_custom_apis FOREIGN KEY (custom_api_id) REFERENCES custom_api (custom_api_id),
    -- CONSTRAINT fk_workflow_steps_to_external_api FOREIGN KEY (external_api_id) REFERENCES external_api (api_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='커스텀 API의 핵심 로직인 호출 순서와 병렬 처리를 정의합니다.';

-- 각 워크플로우 단계에 필요한 파라미터 정의
CREATE TABLE parameter_mapping (
                                   mapping_id VARCHAR(36) NOT NULL COMMENT 'PK. 파라미터 매핑의 고유 식별자',
                                   step_id VARCHAR(36) NOT NULL COMMENT '파라미터가 사용될 workflow_step의 step_id',
                                   target_param_name VARCHAR(255) NOT NULL COMMENT '이 단계의 외부 API가 요구하는 파라미터 이름 (예: userId, location)',
                                   source_type VARCHAR(50) NOT NULL COMMENT '파라미터 값의 출처 (INITIAL_REQUEST, STEP_OUTPUT, STATIC_VALUE)',
                                   source_step_alias VARCHAR(255) NULL COMMENT 'source_type이 STEP_OUTPUT일 때, 어떤 단계의 결과값을 참조할지 workflow_step의 alias 지정',
                                   source_data_path VARCHAR(255) NULL COMMENT 'source_type이 STEP_OUTPUT 또는 INITIAL_REQUEST일 때, 응답/요청 JSON에서 값을 추출할 경로 (예: $.data.user.id)',
                                   static_value TEXT NULL COMMENT 'source_type이 STATIC_VALUE일 때 사용할 고정된 값',
                                   PRIMARY KEY (mapping_id),
                                   INDEX idx_step_id (step_id),
                                   INDEX idx_source_type (source_type)
    -- MSA 환경에서는 외래키 제약조건 제거
    -- CONSTRAINT fk_parameter_mappings_to_workflow_steps FOREIGN KEY (step_id) REFERENCES workflow_step (step_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='각 워크플로우 단계(API 호출)에 필요한 파라미터가 어디서 오는지 정의합니다.';