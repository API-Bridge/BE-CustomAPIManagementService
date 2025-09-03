# 커스텀 API 공유 기능 구현 계획

이 문서는 커스텀 API 공유 및 가져오기(Import) 기능 구현을 위한 단계별 계획을 설명합니다.
"원본-링크(Original-Link)" 모델을 기반으로 하며, AI 코딩 어시스턴트가 이해하고 구현하기 쉽도록 각 단계의 목표와 코드 변경 사항을 명확하게 제시합니다.

---

### **구현 모델: 원본-링크 (Original-Link)**

-   **공유 (Share)**: 사용자가 자신의 `ORIGINAL` API를 공개(`isPublic=true`)합니다. 이 API는 '원본'이 됩니다.
-   **가져오기 (Import)**: 다른 사용자가 공개된 '원본' API를 자신의 대시보드로 가져옵니다. 이때, 사용자의 대시보드에는 원본을 참조하는 `LINK` 타입의 새로운 API가 생성됩니다.
-   **장점**: 원본 API의 핵심 로직(예: 외부 API 목록)이 변경되면, 모든 `LINK` 타입 API에 즉시 반영됩니다. 데이터 복제가 아니므로 일관성 유지가 용이합니다.

---

## **구현 계획: 단계별 파일 수정 가이드**

### **1단계: 도메인 모델 확장**

데이터베이스 스키마와 엔티티를 수정하여 공유 기능을 지원합니다. API의 종류(원본, 링크), 공유 상태, 원본 API 참조를 추가합니다.

#### **파일 1: `ApiType.java` 생성**

API의 종류를 명확하게 정의하기 위해 새로운 Enum 파일을 생성합니다.

```diff
--- /dev/null
+++ b/CustomAPISvc/src/main/java/org/example/customapisvc/domain/Entity/ApiType.java
@@ -0,0 +1,7 @@
+package org.example.customapisvc.domain.Entity;
+
+public enum ApiType {
+    ORIGINAL, // 직접 생성한 원본 API
+    LINK      // 다른 사용자의 API를 가져온 연결된 API
+}
```

#### **파일 2: `CustomApi.java` 수정**

`CustomApi` 엔티티에 공유 관련 필드와 관계를 추가합니다. 또한, 부정확했던 `external_api_name` 컬럼명을 `external_api_url_list_json`으로 수정하여 명확성을 높입니다.

```diff
--- a/CustomAPISvc/src/main/java/org/example/customapisvc/domain/Entity/CustomApi.java
+++ b/CustomAPISvc/src/main/java/org/example/customapisvc/domain/Entity/CustomApi.java
@@ -1,11 +1,14 @@
 package org.example.customapisvc.domain.Entity;
 
+import com.fasterxml.jackson.annotation.JsonIgnore;
 import com.fasterxml.jackson.core.JsonProcessingException;
 import com.fasterxml.jackson.core.type.TypeReference;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import jakarta.persistence.*;
 import lombok.Getter;
+import lombok.ToString;
 import lombok.Setter;
 import lombok.NoArgsConstructor;
 import lombok.AllArgsConstructor;
@@ -25,7 +28,11 @@
     @Index(name = "idx_user_id", columnList = "user_id"),
     @Index(name = "idx_deleted", columnList = "deleted"),
     @Index(name = "idx_user_id_deleted", columnList = "user_id, deleted"),
-    @Index(name = "idx_is_active", columnList = "is_active")
+    @Index(name = "idx_is_active", columnList = "is_active"),
+    @Index(name = "idx_api_type", columnList = "api_type"),
+    @Index(name = "idx_is_public", columnList = "is_public"),
+    @Index(name = "idx_origin_api_id", columnList = "origin_api_id")
 })
 @Getter
 @Setter
@@ -42,8 +49,8 @@
     @Column(name = "name", nullable = false)
     private String name;
 
-    @Column(name = "external_api_name", columnDefinition = "JSON")
+    @Column(name = "external_api_url_list_json", columnDefinition = "JSON")
     private String externalApiUrlListJson;
     
     @Transient
@@ -58,6 +65,23 @@
     @Column(name = "ai_plus_active", nullable = false)
     private Boolean aiPlusActive = false;
 
+    @Enumerated(EnumType.STRING)
+    @Column(name = "api_type", nullable = false)
+    private ApiType apiType = ApiType.ORIGINAL; // 기본값은 ORIGINAL
+
+    @Column(name = "is_public", nullable = false)
+    private boolean isPublic = false; // 공유 여부, 기본값은 false
+
+    @ManyToOne(fetch = FetchType.LAZY)
+    @JoinColumn(name = "origin_api_id")
+    @ToString.Exclude
+    @JsonIgnore
+    private CustomApi originApi; // LINK 타입일 경우, 원본 API를 참조
+
+    @OneToMany(mappedBy = "originApi", fetch = FetchType.LAZY)
+    @ToString.Exclude
+    private List<CustomApi> linkedApis = new ArrayList<>(); // 원본 API에 연결된 링크들
+
     public void setExternalApiUrlList(List<ExternalApiInfoDto> externalApiUrlList) {
         this.externalApiUrlList = externalApiUrlList;
         // 직렬화 (직렬화: 객체를 JSON으로 변환하는 과정)
@@ -70,6 +94,12 @@
     }
     
     public List<ExternalApiInfoDto> getExternalApiUrlList() {
+        // LINK 타입인 경우, 항상 원본 API의 목록을 반환합니다.
+        if (this.apiType == ApiType.LINK && this.originApi != null) {
+            return this.originApi.getExternalApiUrlList();
+        }
+
+        // ORIGINAL 타입인 경우, 자신의 목록을 반환합니다.
         if (externalApiUrlList == null && externalApiUrlListJson != null) {
             // Json 에서 역직렬화 (역직렬화: JSON을 객체로 변환하는 과정)
             try {

```

### **2단계: DTO(Data Transfer Object) 수정**

API 응답에 공유 관련 정보를 포함시켜 프론트엔드에서 "가져온 API"를 특별하게 표시할 수 있도록 합니다.

#### **파일 3: `CustomApiResponseDto.java` 수정**

```diff
--- a/CustomAPISvc/src/main/java/org/example/customapisvc/dto/response/CustomApiResponseDto.java
+++ b/CustomAPISvc/src/main/java/org/example/customapisvc/dto/response/CustomApiResponseDto.java
@@ -4,6 +4,7 @@
 import lombok.Getter;
 import lombok.NoArgsConstructor;
 import lombok.Setter;
+import org.example.customapisvc.domain.Entity.ApiType;
 import org.example.customapisvc.dto.ExternalApiInfoDto;
 
 import java.time.LocalDateTime;
@@ -34,6 +35,18 @@
     @Schema(description = "AI Plus 활성화 여부", example = "false")
     private Boolean aiPlusActive;
 
+    @Schema(description = "API 타입 (ORIGINAL: 직접 생성, LINK: 가져온 API)", example = "ORIGINAL")
+    private ApiType apiType;
+
+    @Schema(description = "공개(공유) 여부", example = "false")
+    private boolean isPublic;
+
+    @Schema(description = "원본 API ID (가져온 API인 경우)", example = "api-origin-001")
+    private String originApiId;
+
+    @Schema(description = "원본 API 소유자 ID (가져온 API인 경우)", example = "user-creator-456")
+    private String ownerUserId;
+
     @Schema(description = "생성일시", example = "2023-08-08T10:30:00")
     private LocalDateTime createdAt;
 
@@ -56,4 +69,20 @@
         this.aiPlusActive = aiPlusActive;
         this.createdAt = createdAt;
         this.updatedAt = updatedAt;
     }
+
+    // 공유 기능을 포함한 새로운 생성자
+    public CustomApiResponseDto(String customApiId, String userId, String name, String description, List<ExternalApiInfoDto> externalApiUrlList, Boolean aiPlusActive, LocalDateTime createdAt, LocalDateTime updatedAt, ApiType apiType, boolean isPublic, String originApiId, String ownerUserId) {
+        this.customApiId = customApiId;
+        this.userId = userId;
+        this.name = name;
+        this.description = description;
+        this.externalApiUrl_list = externalApiUrlList;
+        this.aiPlusActive = aiPlusActive;
+        this.createdAt = createdAt;
+        this.updatedAt = updatedAt;
+        this.apiType = apiType;
+        this.isPublic = isPublic;
+        this.originApiId = originApiId;
+        this.ownerUserId = ownerUserId;
+    }
 }

```

### **3단계: 리포지토리(Repository) 확장**

데이터베이스에서 공유 API를 조회하거나 중복 가져오기를 방지하기 위한 새로운 쿼리 메소드를 추가합니다.

#### **파일 4: `CustomApiRepository.java` 수정**

```diff
--- a/CustomAPISvc/src/main/java/org/example/customapisvc/repository/CustomApiRepository.java
+++ b/CustomAPISvc/src/main/java/org/example/customapisvc/repository/CustomApiRepository.java
@@ -1,5 +1,6 @@
 package org.example.customapisvc.repository;
 
+import org.example.customapisvc.domain.Entity.ApiType;
 import org.example.customapisvc.domain.Entity.CustomApi;
 import org.springframework.data.jpa.repository.JpaRepository;
 import org.springframework.data.jpa.repository.Modifying;
@@ -54,4 +55,10 @@
     @Query(value = "SELECT COUNT(ca.custom_api_id) FROM custom_api ca WHERE ca.deleted = false AND ca.is_active = true AND " +
            "JSON_CONTAINS(ca.external_api_url_list_json, JSON_OBJECT('id', :externalApiId))", nativeQuery = true)
     long countActiveCustomApisByExternalApiId(@Param("externalApiId") String externalApiId);
+
+    // 공유된 원본 API 목록 조회
+    List<CustomApi> findByIsPublicTrueAndApiTypeAndDeletedFalse(ApiType apiType);
+
+    // 사용자가 이미 특정 원본 API를 가져왔는지 확인
+    boolean existsByOriginApiAndUserIdAndDeletedFalse(CustomApi originApi, String userId);
 }

```

### **4단계: 서비스 인터페이스(Service Interface) 설계**

공유, 가져오기 등 새로운 비즈니스 로직을 처리할 메소드를 서비스 인터페이스에 정의합니다.

#### **파일 5: `CustomApiService.java` 수정**

```diff
--- a/CustomAPISvc/src/main/java/org/example/customapisvc/service/CustomApiService.java
+++ b/CustomAPISvc/src/main/java/org/example/customapisvc/service/CustomApiService.java
@@ -1,6 +1,5 @@
 package org.example.customapisvc.service;
 
-import org.example.customapisvc.dto.request.InitiateCreationRequestDto;
 import org.example.customapisvc.dto.response.CustomApiResponseDto;
 import org.springframework.stereotype.Service;
 
@@ -35,4 +34,26 @@
      * @return 비활성화 처리된 커스텀 API 개수
      */
     int deactivateCustomApisByExternalApiId(String externalApiId);
+
+    /**
+     * 커스텀 API를 다른 사용자와 공유하거나 공유를 취소합니다.
+     * @param customApiId 공유할 커스텀 API의 ID
+     * @param userId 요청한 사용자 ID (소유권 확인용)
+     * @param share true면 공유, false면 공유 취소
+     */
+    void shareCustomApi(String customApiId, String userId, boolean share);
+
+    /**
+     * 모든 사용자에게 공유된 커스텀 API 목록을 조회합니다.
+     * @return 공유된 API 목록
+     */
+    List<CustomApiResponseDto> getSharedApis();
+
+    /**
+     * 공유된 커스텀 API를 현재 사용자의 대시보드로 가져옵니다. (Import)
+     * @param originApiId 가져올 원본 API의 ID
+     * @param importerUserId API를 가져오는 사용자 ID
+     * @return 가져오기 후 생성된 새로운 API 정보
+     */
+    CustomApiResponseDto importSharedApi(String originApiId, String importerUserId);
 }

```

### **5단계: 서비스 로직 구현**

정의된 인터페이스에 맞춰 실제 비즈니스 로직을 `CustomApiServiceImpl`에 구현합니다.

#### **파일 6: `CustomApiServiceImpl.java` 수정**

```diff
--- a/CustomAPISvc/src/main/java/org/example/customapisvc/service/impl/CustomApiServiceImpl.java
+++ b/CustomAPISvc/src/main/java/org/example/customapisvc/service/impl/CustomApiServiceImpl.java
@@ -2,6 +2,7 @@
 
 import lombok.RequiredArgsConstructor;
 import lombok.extern.slf4j.Slf4j;
+import org.example.customapisvc.domain.Entity.ApiType;
 import org.example.customapisvc.domain.Entity.CustomApi;
 import org.example.customapisvc.dto.response.CustomApiResponseDto;
 import org.example.customapisvc.event.model.CustomApiDeletedEvent;
@@ -14,6 +15,7 @@
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
+import java.util.UUID;
 import java.util.stream.Collectors;
 
 @Slf4j
@@ -60,6 +62,18 @@
         try {
             CustomApi customApi = customApiRepository.findByCustomApiIdAndDeletedFalse(customApiId)
                     .orElseThrow(() -> new RuntimeException("커스텀API를 찾을 수 없습니다. customApiId: " + customApiId));
+            
+            // LINK 타입인 경우, 원본 API의 상태를 확인
+            if (customApi.getApiType() == ApiType.LINK) {
+                CustomApi originApi = customApi.getOriginApi();
+                if (originApi == null || originApi.isDeleted()) {
+                    throw new RuntimeException("원본 API가 삭제되어 이 API는 사용할 수 없습니다. customApiId: " + customApiId);
+                }
+                if (!originApi.getIsActive()) {
+                    throw new RuntimeException("원본 API가 비활성화되어 이 API는 사용할 수 없습니다. customApiId: " + customApiId);
+                }
+            }
             
             // 커스텀 API가 비활성화되어 있는지 확인
             if (!customApi.getIsActive()) {
@@ -226,15 +240,105 @@
         }
     }
 
+    @Override
+    @Transactional
+    public void shareCustomApi(String customApiId, String userId, boolean share) {
+        log.info("Setting share status for custom API: {} to {} by user: {}", customApiId, share, userId);
+
+        CustomApi customApi = customApiRepository.findByCustomApiIdAndDeletedFalse(customApiId)
+                .orElseThrow(() -> new RuntimeException("커스텀API를 찾을 수 없습니다. customApiId: " + customApiId));
+
+        // 소유권 및 타입 확인
+        if (!customApi.getUserId().equals(userId)) {
+            throw new RuntimeException("API 공유 권한이 없습니다.");
+        }
+        if (customApi.getApiType() != ApiType.ORIGINAL) {
+            throw new RuntimeException("원본 API만 공유할 수 있습니다.");
+        }
+
+        customApi.setPublic(share);
+        customApiRepository.save(customApi);
+        log.info("Custom API {} share status set to {}", customApiId, share);
+    }
+
+    @Override
+    public List<CustomApiResponseDto> getSharedApis() {
+        log.debug("모든 공유된 커스텀API 조회");
+        List<CustomApi> sharedApis = customApiRepository.findByIsPublicTrueAndApiTypeAndDeletedFalse(ApiType.ORIGINAL);
+        return sharedApis.stream()
+                .map(this::convertToResponse)
+                .collect(Collectors.toList());
+    }
+
+    @Override
+    @Transactional
+    public CustomApiResponseDto importSharedApi(String originApiId, String importerUserId) {
+        log.info("User {} is importing shared API {}", importerUserId, originApiId);
+
+        // 1. 원본 API 조회 및 검증
+        CustomApi originApi = customApiRepository.findByCustomApiIdAndDeletedFalse(originApiId)
+                .orElseThrow(() -> new RuntimeException("가져올 원본 API를 찾을 수 없습니다. ID: " + originApiId));
+
+        if (!originApi.isPublic() || originApi.getApiType() != ApiType.ORIGINAL) {
+            throw new RuntimeException("이 API는 공유되지 않았거나 원본 API가 아닙니다.");
+        }
+
+        // 2. 자기 자신의 API는 가져올 수 없음
+        if (originApi.getUserId().equals(importerUserId)) {
+            throw new RuntimeException("자기 자신의 API는 가져올 수 없습니다.");
+        }
+
+        // 3. 이미 가져왔는지 확인
+        if (customApiRepository.existsByOriginApiAndUserIdAndDeletedFalse(originApi, importerUserId)) {
+            throw new RuntimeException("이미 가져온 API입니다.");
+        }
+
+        // 4. LINK 타입의 새로운 CustomApi 엔티티 생성
+        CustomApi linkedApi = new CustomApi();
+        linkedApi.setCustomApiId(UUID.randomUUID().toString()); // 새로운 고유 ID 부여
+        linkedApi.setUserId(importerUserId); // 가져온 사람의 ID
+        linkedApi.setName(originApi.getName()); // 이름은 원본과 동일하게 시작
+        linkedApi.setDescription(originApi.getDescription()); // 설명도 복사
+        linkedApi.setApiType(ApiType.LINK);
+        linkedApi.setOriginApi(originApi); // 원본 API 참조 설정
+        linkedApi.setIsActive(originApi.getIsActive()); // 원본의 활성 상태를 따라감
+        linkedApi.setAiPlusActive(originApi.getAiPlusActive());
+
+        CustomApi savedLinkedApi = customApiRepository.save(linkedApi);
+
+        log.info("User {} successfully imported API {} as new API {}", importerUserId, originApiId, savedLinkedApi.getCustomApiId());
+        return convertToResponse(savedLinkedApi);
+    }
+
     //엔티티 -> DTO 변환 메소드
-    private CustomApiResponseDto convertToResponse(CustomApi customApi) { // TODO: 외부 API 정보도 함께 반환해야 함
-        return new CustomApiResponseDto(
+    private CustomApiResponseDto convertToResponse(CustomApi customApi) {
+        String originApiId = null;
+        String ownerUserId = null;
+
+        // LINK 타입인 경우, 원본 API에서 추가 정보를 가져옵니다.
+        if (customApi.getApiType() == ApiType.LINK && customApi.getOriginApi() != null) {
+            CustomApi origin = customApi.getOriginApi();
+            originApiId = origin.getCustomApiId();
+            ownerUserId = origin.getUserId(); // 원본의 소유자
+        } else {
+            // ORIGINAL 타입인 경우, 자기 자신이 소유자입니다.
+            ownerUserId = customApi.getUserId();
+        }
+
+        // getExternalApiUrlList()는 엔티티 내부에서 LINK/ORIGINAL 타입을 이미 처리합니다.
+        return new CustomApiResponseDto(
                 customApi.getCustomApiId(),
                 customApi.getUserId(),
                 customApi.getName(),
                 customApi.getDescription(),
                 customApi.getExternalApiUrlList(),
+                customApi.getAiPlusActive(),
                 customApi.getCreatedAt(),
-                customApi.getUpdatedAt()
+                customApi.getUpdatedAt(),
+                customApi.getApiType(),
+                customApi.isPublic(),
+                originApiId,
+                ownerUserId
         );
     }
 }

```