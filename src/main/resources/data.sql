-- =====================================================
-- 개발환경 더미 데이터 (애플리케이션 시작시 자동 실행)
-- =====================================================

-- Custom API 더미 데이터
INSERT INTO custom_api (custom_api_id, user_id, name, description, created_at, updated_at, deleted) VALUES
('api-001', 'user-123', '사용자 정보 및 날씨 조회 API', '사용자의 위치 정보를 기반으로 날씨 정보를 함께 조회하는 커스텀 API', NOW(), NOW(), false),
('api-002', 'user-456', '주문 내역 및 배송 상태 조회 API', '사용자의 주문 내역과 배송 상태를 한 번에 조회하는 커스텀 API', NOW(), NOW(), false),
('api-003', 'user-123', '상품 추천 및 리뷰 조회 API', '사용자의 구매 이력을 바탕으로 상품을 추천하고 리뷰를 조회하는 API', NOW(), NOW(), false);

-- Workflow Step 더미 데이터
INSERT INTO workflow_step (step_id, custom_api_id, external_api_id, execution_group, alias, created_at, updated_at, deleted) VALUES
-- API-001의 워크플로우 (사용자정보 -> 날씨조회)
('step-001-1', 'api-001', 'external-api-user', 1, '사용자정보조회', NOW(), NOW(), false),
('step-001-2', 'api-001', 'external-api-weather', 2, '날씨조회', NOW(), NOW(), false),

-- API-002의 워크플로우 (주문조회 -> 배송조회)
('step-002-1', 'api-002', 'external-api-order', 1, '주문조회', NOW(), NOW(), false),
('step-002-2', 'api-002', 'external-api-delivery', 2, '배송조회', NOW(), NOW(), false),

-- API-003의 워크플로우 (사용자정보 -> 추천상품조회 & 리뷰조회 병렬처리)
('step-003-1', 'api-003', 'external-api-user', 1, '사용자정보조회', NOW(), NOW(), false),
('step-003-2', 'api-003', 'external-api-recommendation', 2, '상품추천조회', NOW(), NOW(), false),
('step-003-3', 'api-003', 'external-api-review', 2, '리뷰조회', NOW(), NOW(), false);

-- Parameter Mapping 더미 데이터
INSERT INTO parameter_mapping (mapping_id, step_id, target_param_name, source_type, source_step_alias, source_data_path, static_value, created_at, updated_at, deleted) VALUES
-- API-001 파라미터 매핑
('mapping-001-1-1', 'step-001-1', 'userId', 'INITIAL_REQUEST', NULL, '$.userId', NULL, NOW(), NOW(), false),
('mapping-001-2-1', 'step-001-2', 'location', 'STEP_OUTPUT', '사용자정보조회', '$.data.location', NULL, NOW(), NOW(), false),
('mapping-001-2-2', 'step-001-2', 'units', 'STATIC_VALUE', NULL, NULL, 'metric', NOW(), NOW(), false),

-- API-002 파라미터 매핑
('mapping-002-1-1', 'step-002-1', 'userId', 'INITIAL_REQUEST', NULL, '$.userId', NULL, NOW(), NOW(), false),
('mapping-002-1-2', 'step-002-1', 'status', 'STATIC_VALUE', NULL, NULL, 'ACTIVE', NOW(), NOW(), false),
('mapping-002-2-1', 'step-002-2', 'orderId', 'STEP_OUTPUT', '주문조회', '$.data.orderId', NULL, NOW(), NOW(), false),

-- API-003 파라미터 매핑
('mapping-003-1-1', 'step-003-1', 'userId', 'INITIAL_REQUEST', NULL, '$.userId', NULL, NOW(), NOW(), false),
('mapping-003-2-1', 'step-003-2', 'userId', 'STEP_OUTPUT', '사용자정보조회', '$.data.userId', NULL, NOW(), NOW(), false),
('mapping-003-2-2', 'step-003-2', 'category', 'STEP_OUTPUT', '사용자정보조회', '$.data.preferredCategory', NULL, NOW(), NOW(), false),
('mapping-003-3-1', 'step-003-3', 'userId', 'STEP_OUTPUT', '사용자정보조회', '$.data.userId', NULL, NOW(), NOW(), false),
('mapping-003-3-2', 'step-003-3', 'limit', 'STATIC_VALUE', NULL, NULL, '10', NOW(), NOW(), false);