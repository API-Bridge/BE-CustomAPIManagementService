-- =====================================================
-- 커스텀 API 서비스 더미 데이터
-- =====================================================

-- 더미 데이터 삽입 전 기존 데이터 삭제 (개발환경 전용)
DELETE FROM custom_api WHERE custom_api_id IN (
    'api-001', 'api-002', 'api-003', 'api-004', 'api-005', 
    'api-006', 'api-007', 'api-008', 'api-009', 'api-010'
);

-- 사용자별 커스텀 API 더미 데이터
INSERT INTO custom_api (
    custom_api_id, 
    user_id, 
    name, 
    description, 
    external_api_name, 
    is_active,
    ai_plus_active,
    created_at, 
    updated_at, 
    deleted
) VALUES 
-- user-123의 API들
(
    'api-001', 
    'user-123', 
    '날씨 정보 조회 API', 
    '현재 위치의 날씨 정보를 조회하는 커스텀 API', 
    '[{"name":"OpenWeatherMap API","endpoint":"https://api.openweathermap.org/data/2.5/weather"},{"name":"WeatherAPI","endpoint":"https://api.weatherapi.com/v1/current.json"}]',
    TRUE,
    FALSE,
    '2024-01-15 09:30:00',
    '2024-01-15 09:30:00',
    FALSE
),
(
    'api-002', 
    'user-123', 
    '뉴스 헤드라인 API', 
    '최신 뉴스 헤드라인을 가져오는 커스텀 API', 
    '[{"name":"NewsAPI","endpoint":"https://newsapi.org/v2/top-headlines"},{"name":"Guardian API","endpoint":"https://content.guardianapis.com/search"}]',
    TRUE,
    FALSE,
    '2024-01-20 14:15:30',
    '2024-01-25 16:20:15',
    FALSE
),
(
    'api-003', 
    'user-123', 
    '환율 정보 API', 
    '실시간 환율 정보를 조회하는 커스텀 API', 
    '[{"name":"Exchange Rates API","endpoint":"https://api.exchangerate-api.com/v4/latest"},{"name":"Fixer.io","endpoint":"https://api.fixer.io/latest"}]',
    TRUE,
    FALSE,
    '2024-02-01 11:45:20',
    '2024-02-01 11:45:20',
    FALSE
),

-- user-456의 API들
(
    'api-004', 
    'user-456', 
    '주식 시세 조회 API', 
    '실시간 주식 시세 정보를 조회하는 커스텀 API', 
    '[{"name":"Alpha Vantage","endpoint":"https://www.alphavantage.co/query"},{"name":"Yahoo Finance","endpoint":"https://query1.finance.yahoo.com/v8/finance/chart"}]',
    TRUE,
    FALSE,
    '2024-01-18 10:20:45',
    '2024-02-05 13:30:25',
    FALSE
),
(
    'api-005', 
    'user-456', 
    '결제 처리 API', 
    '온라인 결제를 처리하는 통합 커스텀 API', 
    '[{"name":"Stripe API","endpoint":"https://api.stripe.com/v1/charges"},{"name":"PayPal API","endpoint":"https://api.paypal.com/v2/payments"}]',
    TRUE,
    FALSE,
    '2024-02-03 15:50:10',
    '2024-02-10 09:15:35',
    FALSE
),
(
    'api-006', 
    'user-456', 
    '이메일 발송 API', 
    '이메일 발송 서비스를 통합하는 커스텀 API', 
    '[{"name":"SendGrid API","endpoint":"https://api.sendgrid.com/v3/mail/send"},{"name":"Mailgun API","endpoint":"https://api.mailgun.net/v3/messages"}]',
    TRUE,
    FALSE,
    '2024-02-07 08:30:15',
    '2024-02-07 08:30:15',
    FALSE
),

-- user-789의 API들
(
    'api-007', 
    'user-789', 
    'SNS 포스팅 API', 
    '여러 SNS 플랫폼에 동시 포스팅하는 커스텀 API', 
    '[{"name":"Twitter API v2","endpoint":"https://api.twitter.com/2/tweets"},{"name":"Facebook Graph API","endpoint":"https://graph.facebook.com/v18.0/me/feed"}]',
    TRUE,
    FALSE,
    '2024-01-25 13:40:30',
    '2024-02-08 16:25:50',
    FALSE
),
(
    'api-008', 
    'user-789', 
    '위치 기반 맛집 추천 API', 
    '현재 위치 기반으로 맛집을 추천하는 커스텀 API', 
    '[{"name":"Google Places API","endpoint":"https://maps.googleapis.com/maps/api/place/nearbysearch/json"},{"name":"Yelp Fusion API","endpoint":"https://api.yelp.com/v3/businesses/search"}]',
    TRUE,
    FALSE,
    '2024-02-05 12:15:45',
    '2024-02-05 12:15:45',
    FALSE
),

-- 삭제된 API (Soft Delete 테스트용)
(
    'api-009', 
    'user-123', 
    '삭제된 API 테스트', 
    '소프트 삭제 테스트를 위한 API', 
    '[{"name":"Test API","endpoint":"https://api.test.com/v1/data"}]',
    FALSE,
    FALSE,
    '2024-01-10 10:00:00',
    '2024-01-15 10:00:00',
    TRUE
),

-- 추가 테스트용 API
(
    'api-010', 
    'user-456', 
    '암호화폐 시세 API', 
    '비트코인 및 주요 암호화폐 시세 조회 API', 
    '[{"name":"CoinGecko API","endpoint":"https://api.coingecko.com/api/v3/simple/price"},{"name":"Binance API","endpoint":"https://api.binance.com/api/v3/ticker/price"}]',
    TRUE,
    FALSE,
    '2024-02-12 16:30:20',
    '2024-02-12 16:30:20',
    FALSE
);

-- 인덱스 최적화를 위한 통계 업데이트
-- ANALYZE TABLE custom_api;