-- =====================================================
-- 커스텀 API 서비스 더미 데이터
-- =====================================================

-- 더미 데이터 삽입 전 기존 데이터 삭제 (개발환경 전용)
DELETE FROM custom_api WHERE custom_api_id IN (
    'api-001', 'api-002', 'api-003', 'api-004', 'api-005', 
    'api-006', 'api-007', 'api-008', 'api-009', 'api-010', 
    '폭염특보 및 주택정보', 'covid-info-api', 'transport-schedule-api'
);

-- 사용자별 커스텀 API 더미 데이터
INSERT INTO custom_api (
    custom_api_id, 
    user_id, 
    name, 
    description, 
    external_api_url_list_json, 
    is_active,
    ai_plus_active,
    is_public,
    call_count,
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
    '[{"apiId":"weather-api-001","apiName":"OpenWeatherMap API","endpoint":"https://api.openweathermap.org/data/2.5/weather","parameters":[{"paramName":"q","paramType":"String","paramDescription":"도시 이름","necessary":true},{"paramName":"appid","paramType":"String","paramDescription":"API 키","necessary":true},{"paramName":"units","paramType":"String","paramDescription":"온도 단위 (metric, imperial)","necessary":false}]},{"apiId":"weather-api-002","apiName":"WeatherAPI","endpoint":"https://api.weatherapi.com/v1/current.json","parameters":[{"paramName":"key","paramType":"String","paramDescription":"API 키","necessary":true},{"paramName":"q","paramType":"String","paramDescription":"위치 정보","necessary":true}]}]',
    TRUE,
    FALSE,
    TRUE,
    0,
    '2024-01-15 09:30:00',
    '2024-01-15 09:30:00',
    FALSE
),
(
    'api-002', 
    'user-123', 
    '뉴스 헤드라인 API', 
    '최신 뉴스 헤드라인을 가져오는 커스텀 API', 
    '[{"apiId":"news-api-001","apiName":"NewsAPI","endpoint":"https://newsapi.org/v2/top-headlines","parameters":[{"paramName":"country","paramType":"String","paramDescription":"국가 코드 (kr, us 등)","necessary":false},{"paramName":"category","paramType":"String","paramDescription":"뉴스 카테고리","necessary":false},{"paramName":"apiKey","paramType":"String","paramDescription":"API 키","necessary":true}]},{"apiId":"news-api-002","apiName":"Guardian API","endpoint":"https://content.guardianapis.com/search","parameters":[{"paramName":"q","paramType":"String","paramDescription":"검색 키워드","necessary":false},{"paramName":"api-key","paramType":"String","paramDescription":"API 키","necessary":true}]}]',
    TRUE,
    FALSE,
    FALSE,
    0,
    '2024-01-20 14:15:30',
    '2024-01-25 16:20:15',
    FALSE
),
(
    'api-003', 
    'user-123', 
    '환율 정보 API', 
    '실시간 환율 정보를 조회하는 커스텀 API', 
    '[{"apiId":"exchange-api-001","apiName":"Exchange Rates API","endpoint":"https://api.exchangerate-api.com/v4/latest","parameters":[{"paramName":"base","paramType":"String","paramDescription":"기준 통화","necessary":false}]},{"apiId":"exchange-api-002","apiName":"Fixer.io","endpoint":"https://api.fixer.io/latest","parameters":[{"paramName":"access_key","paramType":"String","paramDescription":"API 키","necessary":true}]}]',
    TRUE,
    FALSE,
    TRUE,
    0,
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
    '[{"apiId":"stock-api-001","apiName":"Alpha Vantage","endpoint":"https://www.alphavantage.co/query","parameters":[{"paramName":"function","paramType":"String","paramDescription":"API 기능 (TIME_SERIES_DAILY 등)","necessary":true},{"paramName":"symbol","paramType":"String","paramDescription":"주식 심볼","necessary":true},{"paramName":"apikey","paramType":"String","paramDescription":"API 키","necessary":true}]},{"apiId":"stock-api-002","apiName":"Yahoo Finance","endpoint":"https://query1.finance.yahoo.com/v8/finance/chart","parameters":[{"paramName":"symbol","paramType":"String","paramDescription":"주식 심볼","necessary":true}]}]',
    TRUE,
    FALSE,
    TRUE,
    0,
    '2024-01-18 10:20:45',
    '2024-02-05 13:30:25',
    FALSE
),
(
    'api-005', 
    'user-456', 
    '결제 처리 API', 
    '온라인 결제를 처리하는 통합 커스텀 API', 
    '[{"apiId":"payment-api-001","apiName":"Stripe API","endpoint":"https://api.stripe.com/v1/charges","parameters":[{"paramName":"amount","paramType":"Integer","paramDescription":"결제 금액 (센트 단위)","necessary":true},{"paramName":"currency","paramType":"String","paramDescription":"통화 코드","necessary":true},{"paramName":"source","paramType":"String","paramDescription":"결제 소스","necessary":true}]},{"apiId":"payment-api-002","apiName":"PayPal API","endpoint":"https://api.paypal.com/v2/payments","parameters":[{"paramName":"intent","paramType":"String","paramDescription":"결제 의도","necessary":true},{"paramName":"purchase_units","paramType":"Array","paramDescription":"구매 단위","necessary":true}]}]',
    TRUE,
    FALSE,
    FALSE,
    0,
    '2024-02-03 15:50:10',
    '2024-02-10 09:15:35',
    FALSE
),
(
    'api-006', 
    'user-456', 
    '이메일 발송 API', 
    '이메일 발송 서비스를 통합하는 커스텀 API', 
    '[{"apiId":"email-api-001","apiName":"SendGrid API","endpoint":"https://api.sendgrid.com/v3/mail/send","parameters":[{"paramName":"personalizations","paramType":"Array","paramDescription":"수신자 정보","necessary":true},{"paramName":"from","paramType":"Object","paramDescription":"발신자 정보","necessary":true},{"paramName":"subject","paramType":"String","paramDescription":"이메일 제목","necessary":true},{"paramName":"content","paramType":"Array","paramDescription":"이메일 내용","necessary":true}]},{"apiId":"email-api-002","apiName":"Mailgun API","endpoint":"https://api.mailgun.net/v3/messages","parameters":[{"paramName":"from","paramType":"String","paramDescription":"발신자","necessary":true},{"paramName":"to","paramType":"String","paramDescription":"수신자","necessary":true},{"paramName":"subject","paramType":"String","paramDescription":"제목","necessary":true},{"paramName":"text","paramType":"String","paramDescription":"텍스트 내용","necessary":false}]}]',
    TRUE,
    FALSE,
    TRUE,
    0,
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
    '[{"apiId":"sns-api-001","apiName":"Twitter API v2","endpoint":"https://api.twitter.com/2/tweets","parameters":[{"paramName":"text","paramType":"String","paramDescription":"트윗 내용","necessary":true},{"paramName":"media","paramType":"Object","paramDescription":"미디어 첨부","necessary":false}]},{"apiId":"sns-api-002","apiName":"Facebook Graph API","endpoint":"https://graph.facebook.com/v18.0/me/feed","parameters":[{"paramName":"message","paramType":"String","paramDescription":"포스트 메시지","necessary":true},{"paramName":"link","paramType":"String","paramDescription":"링크 URL","necessary":false}]}]',
    TRUE,
    FALSE,
    TRUE,
    0,
    '2024-01-25 13:40:30',
    '2024-02-08 16:25:50',
    FALSE
),
(
    'api-008', 
    'user-789', 
    '위치 기반 맛집 추천 API', 
    '현재 위치 기반으로 맛집을 추천하는 커스텀 API', 
    '[{"apiId":"places-api-001","apiName":"Google Places API","endpoint":"https://maps.googleapis.com/maps/api/place/nearbysearch/json","parameters":[{"paramName":"location","paramType":"String","paramDescription":"위도,경도 좌표","necessary":true},{"paramName":"radius","paramType":"Integer","paramDescription":"검색 반경 (미터)","necessary":true},{"paramName":"type","paramType":"String","paramDescription":"장소 유형","necessary":false},{"paramName":"key","paramType":"String","paramDescription":"API 키","necessary":true}]},{"apiId":"places-api-002","apiName":"Yelp Fusion API","endpoint":"https://api.yelp.com/v3/businesses/search","parameters":[{"paramName":"location","paramType":"String","paramDescription":"검색 위치","necessary":true},{"paramName":"term","paramType":"String","paramDescription":"검색어","necessary":false},{"paramName":"radius","paramType":"Integer","paramDescription":"검색 반경","necessary":false}]}]',
    TRUE,
    FALSE,
    FALSE,
    0,
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
    '[{"apiId":"test-api-001","apiName":"Test API","endpoint":"https://api.test.com/v1/data","parameters":[{"paramName":"format","paramType":"String","paramDescription":"응답 형식","necessary":false}]}]',
    FALSE,
    FALSE,
    FALSE,
    0,
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
    '[{"apiId":"crypto-price-api-001","apiName":"CoinGecko API","endpoint":"https://api.coingecko.com/api/v3/simple/price","parameters":[{"paramName":"ids","paramType":"String","paramDescription":"조회할 코인 목록 (bitcoin,ethereum)","necessary":true},{"paramName":"vs_currencies","paramType":"String","paramDescription":"기준 통화 (usd,krw)","necessary":true},{"paramName":"price_data","paramType":"Integer","paramDescription":"코인별 가격 정보","necessary":true}]},{"apiId":"crypto-price-api-002","apiName":"Binance API","endpoint":"https://api.binance.com/api/v3/ticker/price","parameters":[{"paramName":"symbol","paramType":"String","paramDescription":"거래 심볼 (BTCUSDT)","necessary":false},{"paramName":"price","paramType":"Integer","paramDescription":"현재 가격","necessary":false}]}]',
    TRUE,
    FALSE,
    TRUE,
    0,
    '2024-02-12 16:30:20',
    '2024-02-12 16:30:20',
    FALSE
),

(
 '폭염특보 및 주택정보',
'google-oauth2|115305840705313410648',
 '폭염특보 및 주택통계 통합 API',
 '이 API는 두 개의 외부 API를 조합하여 사용자의 요구사항인 폭염 특보 정보와 주택 통계 정보를 제공합니다.  `과거 폭염특보 목록` API는 과거 폭염 특보 정보를 제공하며, `주택통계` API는 주택 통계 정보를 제공합니다.  두 API의 데이터를 통합하여 사용자에게 필요한 정보를 효율적으로 제공합니다.  두 API 모두 한국어로 응답하며, 필요에 따라  연도 및 월별 필터링이 가능합니다.',
 '[{"apiId": "3d0ad254-a587-47f1-9627-8c5a75fdb53f", "reason": "사용자의 폭염특보(도메인=weather, 키워드=heatwave_report) 요구사항을 직접적으로 충족하는 API입니다.  과거 폭염 특보 목록을 제공하며,  `searchYear`와 `searchMonth` 파라미터를 통해 특정 년도와 월의 데이터를 조회할 수 있어 유용성이 높습니다.", "apiName": "과거 폭염특보 목록", "endpoint": "https://sgisapi.kostat.go.kr/OpenAPI3/ndsm/prevHwSpcnwsList.json", "httpMethod": "GET", "parameters": [{"necessary": "true", "paramName": "accessToken", "paramType": "String", "parameterId": "Heat-wave-warning-001", "defaultValue": "2753e209-940d-4af9-abbd-f75ef9eaedf4", "paramDescription": "액세스키"}, {"necessary": "true", "paramName": "searchYear", "paramType": "String", "parameterId": "Heat-wave-warning-002", "defaultValue": "2023", "paramDescription": "폭염 발생 년도"}, {"necessary": true, "paramName": "searchMonth", "paramType": "String", "parameterId": "Heat-wave-warning-003", "defaultValue": "1", "paramDescription": "폭염 발생 월"}]}, {"apiId": "housing-stats-001", "reason": "사용자의 주택통계(도메인=realestate, 키워드=real_estate_trend) 요구사항을 충족하는 API입니다. 주택 통계 데이터를 제공하며, `year` 파라미터를 통해 특정 연도의 데이터를 조회할 수 있습니다.", "apiName": "주택통계", "endpoint": "https://sgisapi.kostat.go.kr/OpenAPI3/stats/house.json", "httpMethod": "GET", "parameters": [{"necessary": true, "paramName": "accessToken", "paramType": "string", "parameterId": "housing-stats-param-001", "defaultValue": "2753e209-940d-4af9-abbd-f75ef9eaedf4", "paramDescription": "액세스키"}, {"necessary": false, "paramName": "year", "paramType": "string", "parameterId": "housing-stats-param-002", "defaultValue": "", "paramDescription": "조회연도"}]}]',
 TRUE,
 FALSE,
 FALSE,
 0,
 '2025-09-05 16:30:20',
 '2025-09-05 16:30:20',
 FALSE
),

-- google-oauth2|115305840705313410648 사용자의 추가 API들
(
    'covid-info-api',
    'google-oauth2|115305840705313410648',
    'COVID-19 정보 통합 API',
    '이 API는 COVID-19 확진자 정보와 백신 접종 현황을 통합하여 제공하는 커스텀 API입니다. 실시간 확진자 데이터와 백신 접종 통계를 함께 조회할 수 있어 코로나19 관련 종합적인 정보를 한번에 확인할 수 있습니다.',
    '[{\"apiId\": \"covid-cases-api-001\", \"reason\": \"COVID-19 확진자 현황 정보를 제공하는 API입니다. 지역별, 일별 확진자 수를 조회할 수 있습니다.\", \"apiName\": \"COVID-19 확진자 현황\", \"endpoint\": \"https://openapi.data.go.kr/openapi/service/rest/Covid19/getCovid19InfStateJson\", \"httpMethod\": \"GET\", \"parameters\": [{\"necessary\": true, \"paramName\": \"serviceKey\", \"paramType\": \"String\", \"parameterId\": \"covid-cases-param-001\", \"defaultValue\": \"test-service-key\", \"paramDescription\": \"서비스키\"}, {\"necessary\": false, \"paramName\": \"pageNo\", \"paramType\": \"String\", \"parameterId\": \"covid-cases-param-002\", \"defaultValue\": \"1\", \"paramDescription\": \"페이지번호\"}, {\"necessary\": false, \"paramName\": \"numOfRows\", \"paramType\": \"String\", \"parameterId\": \"covid-cases-param-003\", \"defaultValue\": \"10\", \"paramDescription\": \"한 페이지 결과 수\"}]}, {\"apiId\": \"vaccine-status-api-002\", \"reason\": \"COVID-19 백신 접종 현황 정보를 제공하는 API입니다. 전국 및 지역별 백신 접종률을 조회할 수 있습니다.\", \"apiName\": \"COVID-19 백신 접종 현황\", \"endpoint\": \"https://openapi.data.go.kr/openapi/service/rest/Covid19/getCovid19VaccineStatJson\", \"httpMethod\": \"GET\", \"parameters\": [{\"necessary\": true, \"paramName\": \"serviceKey\", \"paramType\": \"String\", \"parameterId\": \"vaccine-status-param-001\", \"defaultValue\": \"test-service-key\", \"paramDescription\": \"서비스키\"}, {\"necessary\": false, \"paramName\": \"startCreateDt\", \"paramType\": \"String\", \"parameterId\": \"vaccine-status-param-002\", \"defaultValue\": \"20210301\", \"paramDescription\": \"검색 시작일자\"}, {\"necessary\": false, \"paramName\": \"endCreateDt\", \"paramType\": \"String\", \"parameterId\": \"vaccine-status-param-003\", \"defaultValue\": \"20210331\", \"paramDescription\": \"검색 종료일자\"}]}]',
    TRUE,
    FALSE,
    TRUE,
    0,
    '2025-09-08 10:15:30',
    '2025-09-08 10:15:30',
    FALSE
),

(
    'transport-schedule-api',
    'google-oauth2|115305840705313410648',
    '대중교통 통합 시간표 API',
    '이 API는 지하철과 버스의 실시간 운행 정보를 통합하여 제공하는 커스텀 API입니다. 서울 지하철 실시간 도착정보와 버스 운행 정보를 함께 조회하여 대중교통 이용 계획을 세우는데 도움을 줍니다. GPS 기반 위치 정보를 활용하여 가장 가까운 정류장의 정보를 제공합니다.',
    '[{\"apiId\": \"subway-api-001\", \"reason\": \"서울 지하철 실시간 도착 정보를 제공하는 API입니다. 역명 기반으로 지하철 도착 예정 시간을 조회할 수 있습니다.\", \"apiName\": \"지하철 실시간 도착정보\", \"endpoint\": \"http://swopenapi.seoul.go.kr/api/subway\", \"httpMethod\": \"GET\", \"parameters\": [{\"necessary\": true, \"paramName\": \"key\", \"paramType\": \"String\", \"parameterId\": \"subway-param-001\", \"defaultValue\": \"sample-key\", \"paramDescription\": \"인증키\"}, {\"necessary\": true, \"paramName\": \"type\", \"paramType\": \"String\", \"parameterId\": \"subway-param-002\", \"defaultValue\": \"json\", \"paramDescription\": \"요청파일타입\"}, {\"necessary\": true, \"paramName\": \"service\", \"paramType\": \"String\", \"parameterId\": \"subway-param-003\", \"defaultValue\": \"realtimeStationArrival\", \"paramDescription\": \"서비스명\"}, {\"necessary\": false, \"paramName\": \"stationName\", \"paramType\": \"String\", \"parameterId\": \"subway-param-004\", \"defaultValue\": \"홍대입구\", \"paramDescription\": \"지하철역명\"}]}, {\"apiId\": \"bus-api-002\", \"reason\": \"서울 버스 실시간 위치 및 도착 정보를 제공하는 API입니다. 정류소별 버스 도착 예정 시간을 조회할 수 있습니다.\", \"apiName\": \"버스 도착정보\", \"endpoint\": \"http://ws.bus.go.kr/api/rest/arrive/getArrInfoByRoute\", \"httpMethod\": \"GET\", \"parameters\": [{\"necessary\": true, \"paramName\": \"serviceKey\", \"paramType\": \"String\", \"parameterId\": \"bus-param-001\", \"defaultValue\": \"test-service-key\", \"paramDescription\": \"서비스키\"}, {\"necessary\": false, \"paramName\": \"stId\", \"paramType\": \"String\", \"parameterId\": \"bus-param-002\", \"defaultValue\": \"01001\", \"paramDescription\": \"정류소고유번호\"}, {\"necessary\": false, \"paramName\": \"busRouteId\", \"paramType\": \"String\", \"parameterId\": \"bus-param-003\", \"defaultValue\": \"100100001\", \"paramDescription\": \"노선ID\"}, {\"necessary\": false, \"paramName\": \"ord\", \"paramType\": \"String\", \"parameterId\": \"bus-param-004\", \"defaultValue\": \"1\", \"paramDescription\": \"정류소순번\"}]}]',
    TRUE,
    FALSE,
    TRUE,
    0,
    '2025-09-08 14:45:15',
    '2025-09-08 14:45:15',
    FALSE
);

-- 인덱스 최적화를 위한 통계 업데이트
-- ANALYZE TABLE custom_api;