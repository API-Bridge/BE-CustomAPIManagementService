package org.example.customapisvc.repository;

import org.example.customapisvc.domain.Entity.ApiShare;
import org.example.customapisvc.domain.Entity.CustomApi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiShareRepository extends JpaRepository<ApiShare, Long> {

    /**
     * 특정 사용자가 특정 원본 API를 이미 가져왔는지 확인
     * 
     * @param originApi 원본 API 엔티티
     * @param userId 사용자 ID
     * @return 이미 가져온 경우 true
     */
    boolean existsByOriginApiAndUserId(CustomApi originApi, String userId);


    /**
     * 특정 사용자가 특정 원본 API를 가져온 공유 관계 조회
     * 
     * @param originApi 원본 API 엔티티
     * @param userId 사용자 ID
     * @return 공유 관계 (없으면 Optional.empty())
     */
    Optional<ApiShare> findByOriginApiAndUserId(CustomApi originApi, String userId);

    /**
     * 특정 사용자가 가져온 API 중에서 특정 원본 API ID를 가진 공유 관계 조회
     * 
     * @param originApiId 원본 API ID
     * @param userId 사용자 ID
     * @return 공유 관계 (없으면 Optional.empty())
     */
    @Query("SELECT ash FROM ApiShare ash WHERE ash.originApi.customApiId = :originApiId AND ash.userId = :userId")
    Optional<ApiShare> findByOriginApiIdAndUserId(@Param("originApiId") String originApiId, @Param("userId") String userId);

    /**
     * 특정 사용자가 가져온 모든 API의 원본 API 정보를 함께 조회
     * 페치 조인을 사용하여 N+1 문제 방지
     * 
     * @param userId 사용자 ID
     * @return 원본 API 정보가 포함된 공유 관계 목록
     */
    @Query("SELECT ash FROM ApiShare ash JOIN FETCH ash.originApi ca WHERE ash.userId = :userId AND ca.deleted = false")
    List<ApiShare> findByUserIdWithOriginApi(@Param("userId") String userId);

    /**
     * 특정 원본 API를 공유한 모든 관계를 삭제
     * 원본 API가 삭제될 때 CASCADE로 자동 삭제되지만, 명시적 삭제가 필요한 경우 사용
     * 
     * @param originApi 원본 API 엔티티
     * @return 삭제된 공유 관계 개수
     */
    long deleteByOriginApi(CustomApi originApi);

    /**
     * 특정 사용자의 특정 원본 API 공유 관계 삭제
     * 사용자가 가져온 API를 제거할 때 사용
     * 
     * @param originApi 원본 API 엔티티
     * @param userId 사용자 ID
     * @return 삭제된 공유 관계 개수
     */
    long deleteByOriginApiAndUserId(CustomApi originApi, String userId);

    /**
     * 특정 사용자의 모든 공유 관계 삭제
     * 사용자 삭제 시 호출
     * 
     * @param userId 사용자 ID
     * @return 삭제된 공유 관계 개수
     */
    long deleteByUserId(String userId);
}