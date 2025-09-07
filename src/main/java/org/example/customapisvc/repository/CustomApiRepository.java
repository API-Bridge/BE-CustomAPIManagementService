package org.example.customapisvc.repository;

import org.example.customapisvc.domain.Entity.CustomApi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomApiRepository extends JpaRepository<CustomApi, String> {

    List<CustomApi> findByUserIdAndDeletedFalse(String userId);

    Optional<CustomApi> findByCustomApiIdAndDeletedFalse(String customApiId);

    @Query("SELECT ca FROM CustomApi ca WHERE ca.userId = :userId AND ca.name LIKE %:name% AND ca.deleted = false")
    List<CustomApi> findByUserIdAndNameContainingAndDeletedFalse(@Param("userId") String userId, @Param("name") String name);

    boolean existsByCustomApiIdAndDeletedFalse(String customApiId);

    /**
     * 특정 사용자의 모든 커스텀 API를 소프트 삭제 처리
     * 사용자 삭제 이벤트 수신 시 호출되어 해당 사용자의 모든 커스텀 API를 deleted=true로 설정
     * 
     * @param userId 삭제할 사용자 ID
     * @return 삭제 처리된 커스텀 API 개수
     */
    @Modifying
    @Query("UPDATE CustomApi ca SET ca.deleted = true, ca.updatedAt = CURRENT_TIMESTAMP WHERE ca.userId = :userId AND ca.deleted = false")
    int softDeleteAllByUserId(@Param("userId") String userId);

    /**
     * 특정 사용자의 삭제되지 않고 활성화된 커스텀 API 개수 조회
     * 플랜별 생성 제한 검증 시 사용 (isActive=false인 API는 제외)
     * 
     * @param userId 조회할 사용자 ID
     * @return 해당 사용자의 활성 커스텀 API 개수
     */
    @Query("SELECT COUNT(ca) FROM CustomApi ca WHERE ca.userId = :userId AND ca.deleted = false AND ca.isActive = true")
    long countActiveCustomApisByUserId(@Param("userId") String userId);

    /**
     * 특정 외부 API를 사용하는 모든 커스텀 API를 비활성화 처리
     * 외부 API 삭제 이벤트 수신 시 호출되어 해당 외부 API를 사용하는 모든 커스텀 API를 isActive=false로 설정
     * 
     * @param externalApiId 비활성화할 외부 API ID
     * @return 비활성화 처리된 커스텀 API 개수
     */
    @Modifying
    @Query(value = "UPDATE custom_api ca SET ca.is_active = false, ca.updated_at = CURRENT_TIMESTAMP " +
           "WHERE ca.deleted = false AND ca.is_active = true AND " +
           "JSON_CONTAINS(ca.external_api_url_list_json, JSON_OBJECT('id', :externalApiId))", nativeQuery = true)
    int deactivateAllByExternalApiId(@Param("externalApiId") String externalApiId);

    /**
     * 특정 외부 API를 사용하는 삭제되지 않고 활성화된 커스텀 API 개수 조회
     * 
     * @param externalApiId 조회할 외부 API ID
     * @return 해당 외부 API를 사용하는 활성 커스텀 API 개수
     */
    @Query(value = "SELECT COUNT(ca.custom_api_id) FROM custom_api ca WHERE ca.deleted = false AND ca.is_active = true AND " +
           "JSON_CONTAINS(ca.external_api_url_list_json, JSON_OBJECT('id', :externalApiId))", nativeQuery = true)
    long countActiveCustomApisByExternalApiId(@Param("externalApiId") String externalApiId);

    // Association Table 모델: 공유된 API 목록 조회 (모든 API가 원본이므로 is_public=true만 확인)
    List<CustomApi> findByIsPublicTrueAndDeletedFalse();
    

    /**
     * 사용자의 모든 커스텀 API를 생성일시 역순으로 정렬하여 조회 (최근 생성된 것부터)
     * 플랜 다운그레이드 시 최신 API부터 활성화하고 나머지는 비활성화하기 위해 사용
     * 
     * @param userId 조회할 사용자 ID
     * @return 생성일시 역순으로 정렬된 커스텀 API 목록
     */
    @Query("SELECT ca FROM CustomApi ca WHERE ca.userId = :userId AND ca.deleted = false ORDER BY ca.createdAt DESC")
    List<CustomApi> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(@Param("userId") String userId);

    /**
     * 사용자의 삭제되지 않은 커스텀 API 목록을 생성일시 오름차순(오래된 순)으로 조회
     * 
     * @param userId 사용자 ID
     * @return 생성일시 오름차순으로 정렬된 커스텀 API 목록
     */
    @Query("SELECT ca FROM CustomApi ca WHERE ca.userId = :userId AND ca.deleted = false ORDER BY ca.createdAt ASC")
    List<CustomApi> findByUserIdAndDeletedFalseOrderByCreatedAtAsc(@Param("userId") String userId);

    /**
     * 특정 커스텀 API들의 활성화 상태를 일괄 업데이트
     * 플랜 다운그레이드 시 특정 API들의 활성화 상태를 변경할 때 사용
     * 
     * @param customApiIds 업데이트할 커스텀 API ID 목록
     * @param isActive 설정할 활성화 상태
     * @return 업데이트된 커스텀 API 개수
     */
    @Modifying
    @Query("UPDATE CustomApi ca SET ca.isActive = :isActive, ca.updatedAt = CURRENT_TIMESTAMP WHERE ca.customApiId IN :customApiIds AND ca.deleted = false")
    int updateActiveStatusByCustomApiIds(@Param("customApiIds") List<String> customApiIds, @Param("isActive") Boolean isActive);

    /**
     * Redis의 호출 횟수를 데이터베이스의 callCount에 누적
     * 
     * @param customApiId 커스텀 API ID
     * @param count 누적할 호출 횟수
     * @return 업데이트된 레코드 수
     */
    @Modifying
    @Query("UPDATE CustomApi ca SET ca.callCount = ca.callCount + :count, ca.updatedAt = CURRENT_TIMESTAMP WHERE ca.customApiId = :customApiId AND ca.deleted = false")
    int incrementCallCount(@Param("customApiId") String customApiId, @Param("count") Long count);

    /**
     * 모든 커스텀 API의 호출 횟수를 0으로 초기화 (일일 리셋)
     * 
     * @return 초기화된 레코드 수
     */
    @Modifying
    @Query("UPDATE CustomApi ca SET ca.callCount = 0, ca.updatedAt = CURRENT_TIMESTAMP WHERE ca.deleted = false")
    int resetAllCallCounts();

    /**
     * 특정 커스텀 API의 현재 호출 횟수 조회
     * 
     * @param customApiId 커스텀 API ID
     * @return 현재 호출 횟수 (없으면 0)
     */
    @Query("SELECT ca.callCount FROM CustomApi ca WHERE ca.customApiId = :customApiId AND ca.deleted = false")
    Long findCallCountByCustomApiId(@Param("customApiId") String customApiId);

    /**
     * 공유된(public) 커스텀 API를 이름으로 검색 (Association Table 모델)
     * 모든 사용자의 공유된 API 중에서 이름에 특정 문자열이 포함된 API들을 조회
     * 
     * @param name 검색할 이름 (부분 일치)
     * @return 검색된 공유 커스텀 API 목록
     */
    @Query("SELECT ca FROM CustomApi ca WHERE ca.isPublic = true AND ca.name LIKE %:name% AND ca.deleted = false")
    List<CustomApi> findSharedApisByNameContaining(@Param("name") String name);
}