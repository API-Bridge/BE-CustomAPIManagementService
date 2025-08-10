-- H2 데이터베이스용 테스트 데이터
-- Custom API 데이터
INSERT INTO custom_api (custom_api_id, user_id, name, description, deleted, created_at, updated_at) VALUES 
('api-001', 'user-123', '날씨 조회 API', '사용자 위치 기반 날씨 정보를 조회하는 API', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api-002', 'user-123', '상품 추천 API', '사용자 선호도를 기반으로 상품을 추천하는 API', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api-003', 'user-456', '주문 조회 API', '사용자의 주문 내역을 조회하는 API', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);