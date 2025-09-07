# API 호출 횟수 일일 초기화 기능 구현 계획

## 1. 목표

매일 00:00 (KST, 서울 시간)을 기준으로 모든 커스텀 API의 호출 횟수(`callCount`)를 0으로 초기화하여, 일일 API 사용량을 정확하게 측정하고 관리할 수 있는 기반을 마련한다.

## 2. 핵심 전략

- **DB 초기화 스케줄러 도입**: 서울 시간(KST) 기준 매일 자정에 데이터베이스의 `callCount` 필드를 0으로 초기화하는 새로운 스케줄러(`ApiCallCountResetScheduler`)를 추가한다.
- **데이터 정합성 보장**: 자정 초기화 직전에 Redis에 임시 저장된 모든 카운트 데이터를 데이터베이스에 최종적으로 동기화하여, 단 한 건의 호출도 누락되지 않도록 보장한다.
- **기존 로직과의 연계**: 기존의 주기적 동기화 로직(`ApiCallCountSyncService`)은 그대로 유지하여 실시간 집계 성능을 보장하고, 새로운 초기화 스케줄러는 이와 독립적으로 동작하면서도 데이터의 최종 동기화를 책임진다.

## 3. 구현 단계

1.  **`CustomApi` 엔티티 수정**:
    -   일일 호출 횟수를 저장할 `callCount` 필드를 추가한다.

2.  **`CustomApiRepository` 수정**:
    -   Redis의 카운트를 DB에 더하는 `incrementCallCount` 메서드를 추가한다.
    -   모든 API의 `callCount`를 0으로 일괄 업데이트하는 `resetAllCallCounts` 메서드를 추가한다.

3.  **`ApiCallCountSyncService` 구현 및 리팩토링**:
    -   Redis의 카운트를 DB에 주기적으로 동기화하는 서비스를 구현한다.
    -   핵심 동기화 로직을 별도의 public 메서드로 분리하여, 초기화 스케줄러에서도 재사용할 수 있도록 구조를 개선한다.

4.  **`ApiCallCountResetScheduler` 신규 생성**:
    -   `@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")` 어노테이션을 사용하여 서울 기준 자정 실행을 예약한다.
    -   **실행 로직**:
        1.  먼저 `ApiCallCountSyncService`의 최종 동기화 메서드를 호출하여 Redis의 모든 데이터를 DB로 옮긴다.
        2.  그 다음, `customApiRepository.resetAllCallCounts()`를 호출하여 DB 카운트를 초기화한다.

## 4. 기대 효과

- API의 실시간 응답 성능에 영향을 주지 않으면서 일일 호출 횟수를 정확하게 추적할 수 있다.
- 서버의 물리적 위치나 기본 시간대에 관계없이, 항상 비즈니스 요구사항인 서울 시간을 기준으로 정확하게 초기화가 수행된다.
- 기존 시스템에 미치는 영향을 최소화하면서 새로운 기능을 안정적으로 추가할 수 있다.