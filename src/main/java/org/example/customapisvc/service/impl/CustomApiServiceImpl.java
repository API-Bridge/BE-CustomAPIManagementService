package org.example.customapisvc.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customapisvc.domain.Entity.CustomApi;
import org.example.customapisvc.dto.response.CustomApiResponseDto;
import org.example.customapisvc.event.model.CustomApiDeletedEvent;
import org.example.customapisvc.event.publisher.EventPublisherService;
import org.example.customapisvc.repository.CustomApiRepository;
import org.example.customapisvc.service.CustomApiService;
import org.example.customapisvc.util.StructuredLogger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomApiServiceImpl implements CustomApiService {

    private final CustomApiRepository customApiRepository;
    private final StructuredLogger structuredLogger;
    private final EventPublisherService eventPublisherService;

    @Override
    public List<CustomApiResponseDto> getCustomApisByUserId(String userId) {
        log.debug("사용자 이름으로 커스텀API 조회: {}", userId);
        
        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("user_id", userId);
        
        structuredLogger.logUserActivity(userId, "GET_CUSTOM_APIS", "custom_apis", additionalFields);
        
        List<CustomApi> customApis = customApiRepository.findByUserIdAndDeletedFalse(userId);
        
        additionalFields.put("result_count", customApis.size());
        structuredLogger.logBusinessEvent("CUSTOM_API_RETRIEVAL", 
            "Retrieved " + customApis.size() + " custom APIs for user", additionalFields);
        
        return customApis.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CustomApiResponseDto getCustomApiById(String customApiId) {
        log.debug("ID로 커스텀API 조회: {}", customApiId);
        
        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("custom_api_id", customApiId);
        
        try {
            CustomApi customApi = customApiRepository.findByCustomApiIdAndDeletedFalse(customApiId)
                    .orElseThrow(() -> new RuntimeException("커스텀API를 찾을 수 없습니다. customApiId: " + customApiId));
            
            structuredLogger.logBusinessEvent("CUSTOM_API_RETRIEVED", 
                "Successfully retrieved custom API by ID", additionalFields);
            
            return convertToResponse(customApi);
        } catch (RuntimeException e) {
            structuredLogger.logError("CUSTOM_API_NOT_FOUND", 
                "Custom API not found for ID: " + customApiId, e, additionalFields);
            throw e;
        }
    }

    @Override
    public List<CustomApiResponseDto> searchCustomApisByName(String userId, String name) {
        log.debug("사용자 이름으로 커스텀API 검색: {} 다음 이름을 포함한 커스텀API 검색: {}", userId, name);
        
        List<CustomApi> customApis = customApiRepository.findByUserIdAndNameContainingAndDeletedFalse(userId, name);
        return customApis.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public void deleteCustomApi(String customApiId, String userId) {
        log.info("Deleting custom API: {} by user: {}", customApiId, userId);
        
        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("custom_api_id", customApiId);
        additionalFields.put("user_id", userId);
        
        structuredLogger.logUserActivity(userId, "DELETE_CUSTOM_API", "custom_api", additionalFields);
        
        try {
            CustomApi customApi = customApiRepository.findByCustomApiIdAndDeletedFalse(customApiId)
                    .orElseThrow(() -> new RuntimeException("커스텀API 를 찾을 수 없습니다. customApiId: " + customApiId));
            
            // 사용자 권한 확인
            if (!customApi.getUserId().equals(userId)) {
                structuredLogger.logSecurityEvent("UNAUTHORIZED_DELETE_ATTEMPT", "HIGH", 
                    "User attempted to delete API they don't own", additionalFields);
                throw new RuntimeException("커스텀API 삭제 권한이 없습니다. customApiId: " + customApiId);
            }
            
            // 삭제 전에 이벤트 발행을 위한 데이터 수집
            String apiName = customApi.getName();
            String description = customApi.getDescription();
            var externalApiList = customApi.getExternalApiUrlList();
            
            // Soft Delete
            customApi.setDeleted(true);
            customApiRepository.save(customApi);
            
            // 커스텀 API 삭제 이벤트 발행
            try {
                CustomApiDeletedEvent deletedEvent = new CustomApiDeletedEvent(
                    customApiId, userId, apiName, description, externalApiList, "USER_DELETION"
                );
                eventPublisherService.publishEvent("custom-api-events", deletedEvent);
                
                additionalFields.put("event_published", true);
                structuredLogger.logBusinessEvent("CUSTOM_API_DELETED_EVENT_PUBLISHED", 
                    "커스텀API가 성공적으로 삭제 되었습니다.", additionalFields);
                
            } catch (Exception eventException) {
                additionalFields.put("event_published", false);
                additionalFields.put("event_error", eventException.getMessage());
                structuredLogger.logError("CUSTOM_API_DELETED_EVENT_PUBLISH_FAILED", 
                    "Failed to publish custom API deletion event", eventException, additionalFields);
                // 이벤트 발행 실패는 비즈니스 로직에 영향을 주지 않도록 로깅만 수행
            }
            
            structuredLogger.logBusinessEvent("CUSTOM_API_DELETED", 
                "Custom API successfully soft deleted", additionalFields);
            
            log.info("커스텀API가 성공적으로 삭제되었습니다. customApiId: {}", customApiId);
        } catch (RuntimeException e) {
            structuredLogger.logError("CUSTOM_API_DELETE_FAILED", 
                "Failed to delete custom API", e, additionalFields);
            throw e;
        }
    }

    @Override
    @Transactional
    public int deleteAllCustomApisByUserId(String userId) {
        log.info("사용자 삭제 이벤트 처리 - 모든 커스텀 API 삭제: {}", userId);
        
        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("user_id", userId);
        
        try {
            // 삭제 전 해당 사용자의 활성 커스텀 API 개수 조회
            long activeApiCount = customApiRepository.countActiveCustomApisByUserId(userId);
            additionalFields.put("active_api_count", activeApiCount);
            
            structuredLogger.logBusinessEvent("USER_DELETED_EVENT_PROCESSING", 
                "Processing user deletion event - found " + activeApiCount + " active APIs", additionalFields);
            
            if (activeApiCount == 0) {
                log.info("삭제할 커스텀 API가 없습니다. userId: {}", userId);
                return 0;
            }
            
            // 모든 커스텀 API 소프트 삭제 실행
            int deletedCount = customApiRepository.softDeleteAllByUserId(userId);
            additionalFields.put("deleted_count", deletedCount);
            
            structuredLogger.logBusinessEvent("USER_CUSTOM_APIS_DELETED", 
                "Successfully deleted all custom APIs for user due to user deletion event", additionalFields);
            
            log.info("사용자 삭제 이벤트 처리 완료 - {} 개의 커스텀 API가 삭제되었습니다. userId: {}", deletedCount, userId);
            return deletedCount;
            
        } catch (Exception e) {
            structuredLogger.logError("USER_DELETION_EVENT_PROCESSING_FAILED", 
                "Failed to process user deletion event", e, additionalFields);
            log.error("사용자 삭제 이벤트 처리 실패. userId: {}", userId, e);
            throw new RuntimeException("사용자 삭제 이벤트 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int deactivateCustomApisByExternalApiId(String externalApiId) {
        log.info("외부 API 삭제 이벤트 처리 - 관련 커스텀 API 비활성화: {}", externalApiId);
        
        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("external_api_id", externalApiId);
        
        try {
            // 비활성화 전 해당 외부 API를 사용하는 활성 커스텀 API 개수 조회
            long activeApiCount = customApiRepository.countActiveCustomApisByExternalApiId(externalApiId);
            additionalFields.put("active_api_count", activeApiCount);
            
            structuredLogger.logBusinessEvent("EXTERNAL_API_DELETED_EVENT_PROCESSING", 
                "Processing external API deletion event - found " + activeApiCount + " active custom APIs", additionalFields);
            
            if (activeApiCount == 0) {
                log.info("비활성화할 커스텀 API가 없습니다. externalApiId: {}", externalApiId);
                return 0;
            }
            
            // 해당 외부 API를 사용하는 모든 커스텀 API 비활성화 실행
            int deactivatedCount = customApiRepository.deactivateAllByExternalApiId(externalApiId);
            additionalFields.put("deactivated_count", deactivatedCount);
            
            structuredLogger.logBusinessEvent("EXTERNAL_API_CUSTOM_APIS_DEACTIVATED", 
                "Successfully deactivated all custom APIs using deleted external API", additionalFields);
            
            log.info("외부 API 삭제 이벤트 처리 완료 - {} 개의 커스텀 API가 비활성화되었습니다. externalApiId: {}", deactivatedCount, externalApiId);
            return deactivatedCount;
            
        } catch (Exception e) {
            structuredLogger.logError("EXTERNAL_API_DELETION_EVENT_PROCESSING_FAILED", 
                "Failed to process external API deletion event", e, additionalFields);
            log.error("외부 API 삭제 이벤트 처리 실패. externalApiId: {}", externalApiId, e);
            throw new RuntimeException("외부 API 삭제 이벤트 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    //엔티티 -> DTO 변환 메소드
    private CustomApiResponseDto convertToResponse(CustomApi customApi) { // TODO: 외부 API 정보도 함께 반환해야 함
        return new CustomApiResponseDto(
                customApi.getCustomApiId(),
                customApi.getUserId(),
                customApi.getName(),
                customApi.getDescription(),
                customApi.getExternalApiUrlList(),
                customApi.getCreatedAt(),
                customApi.getUpdatedAt()
        );
    }
}