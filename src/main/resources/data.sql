-- =====================================================
-- 개발환경 더미 데이터 (애플리케이션 시작시 자동 실행)
-- =====================================================

-- Custom API 더미 데이터
INSERT INTO custom_api (custom_api_id, user_id, name, description, created_at, updated_at, deleted) VALUES
('api-001', 'user-123', '사용자 정보 조회 API', '사용자의 기본 정보를 조회하는 API', NOW(), NOW(), false),
('api-002', 'user-456', '상품 검색 API', '상품을 검색하는 API', NOW(), NOW(), false),
('api-003', 'user-123', '주문 조회 API', '사용자의 주문 내역을 조회하는 API', NOW(), NOW(), false);