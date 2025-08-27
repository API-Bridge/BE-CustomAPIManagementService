package org.example.customapisvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customapisvc.dto.common.BaseResponse;
import org.example.customapisvc.event.model.CustomApiCalledEvent;
import org.example.customapisvc.event.publisher.EventPublisherService;
import org.example.customapisvc.service.ApiCallCountSyncService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/**
 * 테스트용 컨트롤러
 * 개발 및 테스트 목적으로 이벤트 발행과 Redis 작업을 직접 테스트할 수 있는 엔드포인트 제공
 */
@Tag(name = "Test", description = "테스트용 API")
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
// dev와 test 프로필에서 활성화
@ConditionalOnBean(ApiCallCountSyncService.class)
public class TestController {

    private final EventPublisherService eventPublisher;
    private final ApiCallCountSyncService apiCallCountSyncService;

    @Operation(summary = "CustomApiCalled 이벤트 발행 테스트", 
               description = "테스트용으로 CustomApiCalled 이벤트를 custom_api_events 토픽에 발행합니다.")
    @PostMapping("/publish-api-called-event")
    public BaseResponse<String> publishCustomApiCalledEvent(
            @Parameter(description = "커스텀 API ID", required = true, example = "api-001")
            @RequestParam String customApiId,
            @Parameter(description = "사용자 ID", required = false, example = "user-123")
            @RequestParam(defaultValue = "test-user") String userId,
            @Parameter(description = "요청 소스", required = false, example = "test-client")
            @RequestParam(defaultValue = "test-client") String requestSource) {
        
        try {
            // CustomApiCalled 이벤트 생성
            CustomApiCalledEvent event = new CustomApiCalledEvent(customApiId, userId, requestSource);
            
            // custom_api_events 토픽에 이벤트 발행
            eventPublisher.publishEvent("custom_api_events", event);
            
            log.info("CustomApiCalled 이벤트 발행 완료 - customApiId: {}, userId: {}, eventId: {}", 
                    customApiId, userId, event.getEventId());
            
            return BaseResponse.success(
                String.format("이벤트 발행 완료 - eventId: %s", event.getEventId()),
                "CustomApiCalled 이벤트가 성공적으로 발행되었습니다."
            );
            
        } catch (Exception e) {
            log.error("CustomApiCalled 이벤트 발행 실패 - customApiId: {}, userId: {}", customApiId, userId, e);
            return BaseResponse.error("이벤트 발행에 실패했습니다: " + e.getMessage());
        }
    }

    @Operation(summary = "Redis 카운트 직접 증가 테스트",
               description = "테스트용으로 Redis에 직접 API 호출 카운트를 증가시킵니다.")
    @PostMapping("/increment-count-direct")
    public BaseResponse<String> incrementCountDirect(
            @Parameter(description = "커스텀 API ID", required = true, example = "api-001")
            @RequestParam String customApiId) {
        
        try {
            // Redis에 직접 카운트 증가
            apiCallCountSyncService.incrementApiCallCount(customApiId);
            
            // 현재 Redis 카운트 조회
            Long currentCount = apiCallCountSyncService.getRedisCallCount(customApiId);
            
            log.info("Redis 카운트 직접 증가 완료 - customApiId: {}, currentCount: {}", customApiId, currentCount);
            
            return BaseResponse.success(
                String.format("현재 카운트: %d", currentCount),
                "Redis 카운트가 성공적으로 증가되었습니다."
            );
            
        } catch (Exception e) {
            log.error("Redis 카운트 증가 실패 - customApiId: {}", customApiId, e);
            return BaseResponse.error("카운트 증가에 실패했습니다: " + e.getMessage());
        }
    }

    @Operation(summary = "Redis 카운트 조회",
               description = "특정 커스텀 API의 Redis 카운트를 조회합니다.")
    @GetMapping("/redis-count/{customApiId}")
    public BaseResponse<Long> getRedisCount(
            @Parameter(description = "커스텀 API ID", required = true, example = "api-001")
            @PathVariable String customApiId) {
        
        try {
            Long count = apiCallCountSyncService.getRedisCallCount(customApiId);
            
            log.info("Redis 카운트 조회 - customApiId: {}, count: {}", customApiId, count);
            
            return BaseResponse.success(count, "Redis 카운트를 성공적으로 조회했습니다.");
            
        } catch (Exception e) {
            log.error("Redis 카운트 조회 실패 - customApiId: {}", customApiId, e);
            return BaseResponse.error("카운트 조회에 실패했습니다: " + e.getMessage());
        }
    }

    @Operation(summary = "수동 동기화 테스트",
               description = "Redis 카운트를 즉시 DB에 동기화합니다.")
    @PostMapping("/manual-sync")
    public BaseResponse<String> manualSync() {
        
        try {
            log.info("수동 동기화 시작");
            apiCallCountSyncService.syncAllCallCounts();
            
            return BaseResponse.success("동기화 완료", "Redis 카운트가 성공적으로 DB에 동기화되었습니다.");
            
        } catch (Exception e) {
            log.error("수동 동기화 실패", e);
            return BaseResponse.error("동기화에 실패했습니다: " + e.getMessage());
        }
    }
}