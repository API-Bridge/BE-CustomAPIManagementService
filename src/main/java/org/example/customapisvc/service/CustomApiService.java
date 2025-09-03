package org.example.customapisvc.service;

import org.example.customapisvc.dto.request.InitiateCreationRequestDto;
import org.example.customapisvc.dto.response.CustomApiResponseDto;
import org.example.customapisvc.dto.response.CustomApiDetailResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CustomApiService {

    //사용자의 모든 커스텀 API 조회
    List<CustomApiResponseDto> getCustomApisByUserId(String userId);

    //커스텀 API 단건 조회
    CustomApiResponseDto getCustomApiById(String customApiId);
    
    //커스텀 API 상세 조회 (외부 마이크로서비스용)
    CustomApiDetailResponseDto getCustomApiDetailById(String customApiId);

    //커스텀 API 이름으로 검색 (공유된 API만)
    List<CustomApiResponseDto> searchCustomApisByName(String name);


    //커스텀 API 삭제 (Soft Delete) - 이벤트 발행 포함
    void deleteCustomApi(String customApiId, String userId);

    /**
     * 특정 사용자의 모든 커스텀 API 삭제 (Soft Delete)
     * 사용자 삭제 이벤트 수신 시 호출되어 해당 사용자의 모든 커스텀 API를 삭제 처리
     * 
     * @param userId 삭제할 사용자 ID
     * @return 삭제 처리된 커스텀 API 개수
     */
    int deleteAllCustomApisByUserId(String userId);

    /**
     * 특정 외부 API를 사용하는 모든 커스텀 API 비활성화
     * 외부 API 삭제 이벤트 수신 시 호출되어 해당 외부 API를 사용하는 모든 커스텀 API를 비활성화 처리
     * 
     * @param externalApiId 비활성화할 외부 API ID
     * @return 비활성화 처리된 커스텀 API 개수
     */
    int deactivateCustomApisByExternalApiId(String externalApiId);

    /**
     * 커스텀 API를 다른 사용자와 공유하거나 공유를 취소합니다.
     * @param customApiId 공유할 커스텀 API의 ID
     * @param userId 요청한 사용자 ID (소유권 확인용)
     * @param share true면 공유, false면 공유 취소
     */
    void shareCustomApi(String customApiId, String userId, boolean share);

    /**
     * 모든 사용자에게 공유된 커스텀 API 목록을 조회합니다.
     * @return 공유된 API 목록
     */
    List<CustomApiResponseDto> getSharedApis();

    /**
     * 공유된 커스텀 API를 현재 사용자의 대시보드로 가져옵니다. (Import)
     * @param originApiId 가져올 원본 API의 ID
     * @param importerUserId API를 가져오는 사용자 ID
     * @return 가져오기 후 생성된 새로운 API 정보
     */
    CustomApiResponseDto importSharedApi(String originApiId, String importerUserId);

    /**
     * 사용자 플랜 다운그레이드 시 커스텀 API 활성화/비활성화 처리
     * 새로운 플랜의 제한 개수에 맞춰 가장 오래된 API부터 활성화하고 나머지는 비활성화
     * 
     * @param userId 플랜이 변경된 사용자 ID
     * @param newPlan 새로운 구독 플랜 (FREE, PRO)
     */
    void handlePlanDowngrade(String userId, String newPlan);

    /**
     * 사용자 플랜 업그레이드 시 커스텀 API 재활성화 처리
     * 새로운 플랜의 제한 개수에 맞춰 비활성화된 API들을 다시 활성화
     * 
     * @param userId 플랜이 변경된 사용자 ID
     * @param newPlan 새로운 구독 플랜 (FREE, PRO)
     */
    void handlePlanUpgrade(String userId, String newPlan);
}