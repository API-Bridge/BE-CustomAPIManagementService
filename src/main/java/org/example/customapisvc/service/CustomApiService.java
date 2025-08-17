package org.example.customapisvc.service;

import org.example.customapisvc.dto.response.CustomApiResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CustomApiService {

    //사용자의 모든 커스텀 API 조회
    List<CustomApiResponseDto> getCustomApisByUserId(String userId);

    //커스텀 API 단건 조회
    CustomApiResponseDto getCustomApiById(String customApiId);

    //커스텀 API 이름으로 검색
    List<CustomApiResponseDto> searchCustomApisByName(String userId, String name);


    //커스텀 API 삭제 (Soft Delete)
    void deleteCustomApi(String customApiId, String userId);

    /**
     * 특정 사용자의 모든 커스텀 API 삭제 (Soft Delete)
     * 사용자 삭제 이벤트 수신 시 호출되어 해당 사용자의 모든 커스텀 API를 삭제 처리
     * 
     * @param userId 삭제할 사용자 ID
     * @return 삭제 처리된 커스텀 API 개수
     */
    int deleteAllCustomApisByUserId(String userId);
}