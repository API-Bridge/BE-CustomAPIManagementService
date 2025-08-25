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
     * 특정 사용자의 삭제되지 않은 커스텀 API 개수 조회
     * 
     * @param userId 조회할 사용자 ID
     * @return 해당 사용자의 활성 커스텀 API 개수
     */
    @Query("SELECT COUNT(ca) FROM CustomApi ca WHERE ca.userId = :userId AND ca.deleted = false")
    long countActiveCustomApisByUserId(@Param("userId") String userId);

    /**
     * 특정 외부 API를 사용하는 모든 커스텀 API를 비활성화 처리
     * 외부 API 삭제 이벤트 수신 시 호출되어 해당 외부 API를 사용하는 모든 커스텀 API를 isActive=false로 설정
     * 
     * @param externalApiId 비활성화할 외부 API ID
     * @return 비활성화 처리된 커스텀 API 개수
     */
    @Modifying
    @Query("UPDATE CustomApi ca SET ca.isActive = false, ca.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE ca.deleted = false AND ca.isActive = true AND " +
           "JSON_CONTAINS(ca.externalApiUrlListJson, JSON_OBJECT('id', :externalApiId))")
    int deactivateAllByExternalApiId(@Param("externalApiId") String externalApiId);

    /**
     * 특정 외부 API를 사용하는 삭제되지 않고 활성화된 커스텀 API 개수 조회
     * 
     * @param externalApiId 조회할 외부 API ID
     * @return 해당 외부 API를 사용하는 활성 커스텀 API 개수
     */
    @Query("SELECT COUNT(ca) FROM CustomApi ca WHERE ca.deleted = false AND ca.isActive = true AND " +
           "JSON_CONTAINS(ca.externalApiUrlListJson, JSON_OBJECT('id', :externalApiId))")
    long countActiveCustomApisByExternalApiId(@Param("externalApiId") String externalApiId);
}