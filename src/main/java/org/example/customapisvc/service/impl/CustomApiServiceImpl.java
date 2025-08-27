package org.example.customapisvc.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customapisvc.domain.Entity.ApiType;
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
import java.util.UUID;
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
            
            // LINK 타입인 경우, 원본 API의 상태를 확인
            if (customApi.getApiType() == ApiType.LINK) {
                CustomApi originApi = customApi.getOriginApi();
                if (originApi == null || originApi.isDeleted()) {
                    throw new RuntimeException("원본 API가 삭제되어 이 API는 사용할 수 없습니다. customApiId: " + customApiId);
                }
                if (!originApi.getIsActive()) {
                    throw new RuntimeException("원본 API가 비활성화되어 이 API는 사용할 수 없습니다. customApiId: " + customApiId);
                }
            }
            
            // 커스텀 API가 비활성화되어 있는지 확인
            if (!customApi.getIsActive()) {
                structuredLogger.logBusinessEvent("CUSTOM_API_DISABLED_ACCESS_ATTEMPT", 
                    "Attempt to access disabled custom API", additionalFields);
                throw new RuntimeException("의존되는 외부 API의 영향으로 이 커스텀 API는 사용할 수 없습니다. 새 커스텀API 를 생성해주세요. customApiId: " + customApiId);
            }
            
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

    @Override
    @Transactional
    public void shareCustomApi(String customApiId, String userId, boolean share) {
        log.info("커스텀API 공유 설정: {} to {} by user: {}", customApiId, share, userId);

        CustomApi customApi = customApiRepository.findByCustomApiIdAndDeletedFalse(customApiId)
                .orElseThrow(() -> new RuntimeException("커스텀API를 찾을 수 없습니다. customApiId: " + customApiId));

        // 소유권 및 타입 확인
        if (!customApi.getUserId().equals(userId)) {
            throw new RuntimeException("API 공유 권한이 없습니다.");
        }
        if (customApi.getApiType() != ApiType.ORIGINAL) {
            throw new RuntimeException("원본 API만 공유할 수 있습니다.");
        }

        customApi.setPublic(share);
        customApiRepository.save(customApi);
        log.info("Custom API {} share status set to {}", customApiId, share);
    }

    @Override
    public List<CustomApiResponseDto> getSharedApis() {
        log.debug("모든 공유된 커스텀API 조회");
        List<CustomApi> sharedApis = customApiRepository.findByIsPublicTrueAndApiTypeAndDeletedFalse(ApiType.ORIGINAL);
        return sharedApis.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomApiResponseDto importSharedApi(String originApiId, String importerUserId) {
        log.info("User {} is importing shared API {}", importerUserId, originApiId);

        // 1. 원본 API 조회 및 검증
        CustomApi originApi = customApiRepository.findByCustomApiIdAndDeletedFalse(originApiId)
                .orElseThrow(() -> new RuntimeException("가져올 원본 API를 찾을 수 없습니다. ID: " + originApiId));

        if (!originApi.isPublic() || originApi.getApiType() != ApiType.ORIGINAL) {
            throw new RuntimeException("이 API는 공유되지 않았거나 원본 API가 아닙니다.");
        }

        // 2. 자기 자신의 API는 가져올 수 없음
        if (originApi.getUserId().equals(importerUserId)) {
            throw new RuntimeException("자기 자신의 API는 가져올 수 없습니다.");
        }

        // 3. 이미 가져왔는지 확인
        if (customApiRepository.existsByOriginApiAndUserIdAndDeletedFalse(originApi, importerUserId)) {
            throw new RuntimeException("이미 가져온 API입니다.");
        }

        // 4. LINK 타입의 새로운 CustomApi 엔티티 생성
        CustomApi linkedApi = new CustomApi();
        linkedApi.setCustomApiId(UUID.randomUUID().toString()); // 새로운 고유 ID 부여
        linkedApi.setUserId(importerUserId); // 가져온 사람의 ID
        linkedApi.setName(originApi.getName()); // 이름은 원본과 동일하게 시작
        linkedApi.setDescription(originApi.getDescription()); // 설명도 복사
        linkedApi.setApiType(ApiType.LINK);
        linkedApi.setOriginApi(originApi); // 원본 API 참조 설정
        linkedApi.setIsActive(originApi.getIsActive()); // 원본의 활성 상태를 따라감
        linkedApi.setAiPlusActive(originApi.getAiPlusActive());

        CustomApi savedLinkedApi = customApiRepository.save(linkedApi);

        log.info("User {} successfully imported API {} as new API {}", importerUserId, originApiId, savedLinkedApi.getCustomApiId());
        return convertToResponse(savedLinkedApi);
    }

    @Override
    @Transactional
    public void handlePlanDowngrade(String userId, String newPlan) {
        log.info("플랜 다운그레이드 처리 시작 - userId: {}, newPlan: {}", userId, newPlan);
        
        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("user_id", userId);
        additionalFields.put("new_plan", newPlan);
        
        try {
            // 새로운 플랜별 최대 개수 결정
            int maxAllowedApis = getMaxApiCountByPlan(newPlan);
            additionalFields.put("max_allowed_apis", maxAllowedApis);
            
            structuredLogger.logBusinessEvent("PLAN_DOWNGRADE_PROCESSING_START", 
                "Starting plan downgrade processing for user", additionalFields);
            
            // 사용자의 모든 커스텀 API를 생성일시 오름차순으로 조회 (오래된 순)
            List<CustomApi> userApis = customApiRepository.findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId);
            int totalApis = userApis.size();
            
            additionalFields.put("total_apis", totalApis);
            
            if (totalApis <= maxAllowedApis) {
                log.info("기존 API 개수가 플랜 제한에 맞음 - userId: {}, totalApis: {}, maxAllowed: {}", 
                    userId, totalApis, maxAllowedApis);
                
                // 모든 API를 활성화 (혹시 비활성화된 것이 있을 수 있음)
                if (totalApis > 0) {
                    List<String> allApiIds = userApis.stream()
                        .map(CustomApi::getCustomApiId)
                        .collect(Collectors.toList());
                    
                    int activatedCount = customApiRepository.updateActiveStatusByCustomApiIds(allApiIds, true);
                    additionalFields.put("activated_count", activatedCount);
                    
                    structuredLogger.logBusinessEvent("PLAN_DOWNGRADE_ALL_ACTIVATED", 
                        "All APIs activated as total count is within limit", additionalFields);
                }
                
                structuredLogger.logBusinessEvent("PLAN_DOWNGRADE_PROCESSING_COMPLETED", 
                    "Successfully processed plan downgrade for user", additionalFields);
                return;
            }
            
            // 제한 초과: 오래된 API부터 maxAllowedApis 개만 활성화, 나머지는 비활성화
            List<CustomApi> apisToActivate = userApis.subList(0, maxAllowedApis);
            List<CustomApi> apisToDeactivate = userApis.subList(maxAllowedApis, totalApis);
            
            int apisToActivateCount = apisToActivate.size();
            int apisToDeactivateCount = apisToDeactivate.size();
            
            additionalFields.put("apis_to_activate_count", apisToActivateCount);
            additionalFields.put("apis_to_deactivate_count", apisToDeactivateCount);
            
            log.info("플랜 제한 초과로 인한 API 상태 변경 - userId: {}, 활성화: {}개, 비활성화: {}개", 
                userId, apisToActivateCount, apisToDeactivateCount);
            
            // 활성화할 API들 (오래된 API부터 maxAllowedApis 개)
            if (apisToActivateCount > 0) {
                List<String> activateApiIds = apisToActivate.stream()
                    .map(CustomApi::getCustomApiId)
                    .collect(Collectors.toList());
                
                int activatedCount = customApiRepository.updateActiveStatusByCustomApiIds(activateApiIds, true);
                additionalFields.put("actually_activated_count", activatedCount);
                
                log.info("{}개의 API가 활성화되었습니다.", activatedCount);
            }
            
            // 비활성화할 API들 (나머지 최신 API들)
            if (apisToDeactivateCount > 0) {
                List<String> deactivateApiIds = apisToDeactivate.stream()
                    .map(CustomApi::getCustomApiId)
                    .collect(Collectors.toList());
                
                int deactivatedCount = customApiRepository.updateActiveStatusByCustomApiIds(deactivateApiIds, false);
                additionalFields.put("actually_deactivated_count", deactivatedCount);
                
                log.info("{}개의 API가 비활성화되었습니다.", deactivatedCount);
            }
            
            structuredLogger.logBusinessEvent("PLAN_DOWNGRADE_PROCESSING_COMPLETED", 
                "Successfully processed plan downgrade for user", additionalFields);
            
            log.info("플랜 다운그레이드 처리 완료 - userId: {}, newPlan: {}", userId, newPlan);
            
        } catch (Exception e) {
            structuredLogger.logError("PLAN_DOWNGRADE_PROCESSING_FAILED", 
                "Failed to process plan downgrade for user", e, additionalFields);
            
            log.error("플랜 다운그레이드 처리 실패 - userId: {}, newPlan: {}", userId, newPlan, e);
            throw new RuntimeException("플랜 다운그래이드 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void handlePlanUpgrade(String userId, String newPlan) {
        log.info("플랜 업그레이드 처리 시작 - userId: {}, newPlan: {}", userId, newPlan);
        
        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("user_id", userId);
        additionalFields.put("new_plan", newPlan);
        
        try {
            // 새로운 플랜별 최대 개수 결정
            int maxAllowedApis = getMaxApiCountByPlan(newPlan);
            additionalFields.put("max_allowed_apis", maxAllowedApis);
            
            structuredLogger.logBusinessEvent("PLAN_UPGRADE_PROCESSING_START", 
                "Starting plan upgrade processing for user", additionalFields);
            
            // 사용자의 모든 커스텀 API를 생성일시 오름차순으로 조회 (오래된 순)
            List<CustomApi> allUserApis = customApiRepository.findByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId);
            int totalApis = allUserApis.size();
            
            additionalFields.put("total_apis", totalApis);
            
            if (totalApis == 0) {
                log.info("사용자에게 커스텀 API가 없음 - userId: {}", userId);
                structuredLogger.logBusinessEvent("PLAN_UPGRADE_NO_APIS", 
                    "No custom APIs found for user", additionalFields);
                return;
            }
            
            if (totalApis <= maxAllowedApis) {
                // 모든 API를 활성화 (비활성화된 것이 있을 수 있음)
                List<String> allApiIds = allUserApis.stream()
                    .map(CustomApi::getCustomApiId)
                    .collect(Collectors.toList());
                
                int activatedCount = customApiRepository.updateActiveStatusByCustomApiIds(allApiIds, true);
                additionalFields.put("activated_count", activatedCount);
                
                log.info("모든 API 활성화 완료 - userId: {}, totalApis: {}, activatedCount: {}", 
                    userId, totalApis, activatedCount);
                
                structuredLogger.logBusinessEvent("PLAN_UPGRADE_ALL_ACTIVATED", 
                    "All APIs activated as total count is within new plan limit", additionalFields);
            } else {
                // 새로운 플랜 제한까지만 활성화 (오래된 API부터)
                List<CustomApi> apisToActivate = allUserApis.subList(0, maxAllowedApis);
                List<String> activateApiIds = apisToActivate.stream()
                    .map(CustomApi::getCustomApiId)
                    .collect(Collectors.toList());
                
                int activatedCount = customApiRepository.updateActiveStatusByCustomApiIds(activateApiIds, true);
                additionalFields.put("activated_count", activatedCount);
                additionalFields.put("apis_to_activate_count", maxAllowedApis);
                
                log.info("제한 개수만큼 API 활성화 완료 - userId: {}, maxAllowed: {}, activatedCount: {}", 
                    userId, maxAllowedApis, activatedCount);
                
                structuredLogger.logBusinessEvent("PLAN_UPGRADE_LIMITED_ACTIVATION", 
                    "Activated APIs up to new plan limit", additionalFields);
            }
            
            structuredLogger.logBusinessEvent("PLAN_UPGRADE_PROCESSING_COMPLETED", 
                "Successfully processed plan upgrade for user", additionalFields);
            
            log.info("플랜 업그레이드 처리 완료 - userId: {}, newPlan: {}", userId, newPlan);
            
        } catch (Exception e) {
            structuredLogger.logError("PLAN_UPGRADE_PROCESSING_FAILED", 
                "Failed to process plan upgrade for user", e, additionalFields);
            
            log.error("플랜 업그레이드 처리 실패 - userId: {}, newPlan: {}", userId, newPlan, e);
            throw new RuntimeException("플랜 업그레이드 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 플랜별 최대 커스텀 API 개수 반환
     * 
     * @param plan 구독 플랜 (FREE, PRO)
     * @return 최대 개수
     */
    private int getMaxApiCountByPlan(String plan) {
        if (plan == null) {
            return 3; // 기본값 (FREE)
        }
        
        switch (plan.toUpperCase()) {
            case "PRO":
                return 5;
            case "FREE":
            default:
                return 3;
        }
    }

    //엔티티 -> DTO 변환 메소드
    private CustomApiResponseDto convertToResponse(CustomApi customApi) {
        String originApiId = null;
        String ownerUserId = null;

        // LINK 타입인 경우, 원본 API에서 추가 정보를 가져옵니다.
        if (customApi.getApiType() == ApiType.LINK && customApi.getOriginApi() != null) {
            CustomApi origin = customApi.getOriginApi();
            originApiId = origin.getCustomApiId();
            ownerUserId = origin.getUserId(); // 원본의 소유자
        } else {
            // ORIGINAL 타입인 경우, 자기 자신이 소유자입니다.
            ownerUserId = customApi.getUserId();
        }

        // getExternalApiUrlList()는 엔티티 내부에서 LINK/ORIGINAL 타입을 이미 처리합니다.
        return new CustomApiResponseDto(
                customApi.getCustomApiId(),
                customApi.getUserId(),
                customApi.getName(),
                customApi.getDescription(),
                customApi.getExternalApiUrlList(),
                customApi.getAiPlusActive(),
                customApi.getCreatedAt(),
                customApi.getUpdatedAt(),
                customApi.getApiType(),
                customApi.isPublic(),
                originApiId,
                ownerUserId,
                customApi.getCallCount() // 일일 호출 횟수 추가
        );
    }
}