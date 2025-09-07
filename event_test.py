#!/usr/bin/env python3
"""
이벤트 발행 테스트 스크립트
CustomApiCalled 이벤트를 Kafka에 발행하고 Redis 상태 변화를 모니터링
"""

import json
import uuid
import time
from datetime import datetime
from kafka import KafkaProducer, KafkaConsumer
import redis
import requests

# 설정
KAFKA_BOOTSTRAP_SERVERS = 'localhost:9092'
KAFKA_TOPIC = 'custom_api_events'
REDIS_HOST = 'localhost'
REDIS_PORT = 6379
API_BASE_URL = 'http://localhost:8083/api'

def create_kafka_producer():
    """Kafka 프로듀서 생성"""
    return KafkaProducer(
        bootstrap_servers=[KAFKA_BOOTSTRAP_SERVERS],
        value_serializer=lambda x: json.dumps(x).encode('utf-8'),
        key_serializer=lambda x: x.encode('utf-8') if x else None
    )

def create_redis_client():
    """Redis 클라이언트 생성"""
    return redis.StrictRedis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=True)

def create_custom_api_called_event(custom_api_id, user_id, request_source):
    """CustomApiCalled 이벤트 생성"""
    now = datetime.now().isoformat()
    event_id = str(uuid.uuid4())
    trace_id = str(uuid.uuid4())
    
    return {
        "eventId": event_id,
        "eventType": "CustomApiCalled",
        "serviceName": "custom-api-svc",
        "traceId": trace_id,
        "timestamp": now,
        "payload": {
            "customApiId": custom_api_id,
            "userId": user_id,
            "requestSource": request_source,
            "calledAt": now
        }
    }

def send_event_to_kafka(producer, event):
    """Kafka에 이벤트 발행"""
    try:
        future = producer.send(
            KAFKA_TOPIC,
            key=event['eventId'],
            value=event
        )
        result = future.get(timeout=10)
        print(f"✅ 이벤트 발행 성공: {event['eventId']}")
        print(f"   파티션: {result.partition}, 오프셋: {result.offset}")
        return True
    except Exception as e:
        print(f"❌ 이벤트 발행 실패: {e}")
        return False

def check_redis_count(redis_client, custom_api_id):
    """Redis에서 API 호출 횟수 확인"""
    try:
        key = f"api_call_count:{custom_api_id}"
        count = redis_client.get(key)
        return int(count) if count else 0
    except Exception as e:
        print(f"❌ Redis 조회 실패: {e}")
        return None

def main():
    print("🔍 이벤트-Redis 연동 테스트 시작")
    print("=" * 50)
    
    # 클라이언트 생성
    try:
        producer = create_kafka_producer()
        redis_client = create_redis_client()
        print("✅ Kafka Producer, Redis Client 생성 완료")
    except Exception as e:
        print(f"❌ 클라이언트 생성 실패: {e}")
        return
    
    # 테스트 API ID
    test_api_id = "api-001"  # 실제 존재하는 API ID 사용
    
    # 이벤트 발행 전 Redis 상태 확인
    print(f"\n🔍 이벤트 발행 전 Redis 상태 확인")
    before_count = check_redis_count(redis_client, test_api_id)
    print(f"   Redis 카운트 (before): {before_count}")
    
    # 이벤트 생성 및 발행
    print(f"\n📤 이벤트 발행")
    event = create_custom_api_called_event(test_api_id, "test-user", "event-test")
    print(f"   이벤트 ID: {event['eventId']}")
    print(f"   Custom API ID: {event['payload']['customApiId']}")
    
    success = send_event_to_kafka(producer, event)
    if not success:
        return
    
    # 이벤트 처리 대기
    print(f"\n⏳ 이벤트 처리 대기 (5초)")
    time.sleep(5)
    
    # 이벤트 발행 후 Redis 상태 확인
    print(f"\n🔍 이벤트 발행 후 Redis 상태 확인")
    after_count = check_redis_count(redis_client, test_api_id)
    print(f"   Redis 카운트 (after): {after_count}")
    
    # 결과 분석
    print(f"\n📊 테스트 결과")
    print(f"   이벤트 발행: {'✅ 성공' if success else '❌ 실패'}")
    print(f"   Redis 카운트 변화: {before_count} -> {after_count}")
    
    if after_count is not None and before_count is not None:
        count_increased = after_count > before_count
        print(f"   카운트 증가: {'✅ 성공' if count_increased else '❌ 실패'}")
        
        if count_increased:
            print(f"\n🎉 테스트 성공! 이벤트가 정상적으로 Redis에 반영됨")
        else:
            print(f"\n❌ 테스트 실패! 이벤트가 Redis에 반영되지 않음")
            print(f"   - 가능한 원인:")
            print(f"     1. 이벤트 리스너에서 예외 발생")
            print(f"     2. Redis 연결 실패")
            print(f"     3. 페이로드 캐스팅 오류")
    else:
        print(f"\n❌ Redis 상태를 확인할 수 없음")
    
    # 추가 테스트: 여러 이벤트 발행
    print(f"\n🔄 추가 테스트: 3개 이벤트 연속 발행")
    for i in range(3):
        event = create_custom_api_called_event(test_api_id, f"test-user-{i}", "batch-test")
        send_event_to_kafka(producer, event)
        time.sleep(1)
    
    print(f"\n⏳ 배치 이벤트 처리 대기 (10초)")
    time.sleep(10)
    
    final_count = check_redis_count(redis_client, test_api_id)
    print(f"\n📊 최종 Redis 카운트: {final_count}")
    
    if final_count is not None and after_count is not None:
        expected_count = after_count + 3
        print(f"   예상 카운트: {expected_count}")
        print(f"   배치 테스트: {'✅ 성공' if final_count == expected_count else '❌ 실패'}")
    
    producer.close()
    print(f"\n✅ 테스트 완료")

if __name__ == "__main__":
    main()