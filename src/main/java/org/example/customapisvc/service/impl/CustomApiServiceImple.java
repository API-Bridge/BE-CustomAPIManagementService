package org.example.customapisvc.service.impl;

import org.example.customapisvc.dto.request.CustomApiCreateRequestDto;
import org.example.customapisvc.dto.response.CustomApiResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CustomApiServiceImple {

    //사용자의 모든 커스텀 API 조회
    List<CustomApiResponseDto> getCustomApisByUserId(String userId);

    //커스텀 API 단건 조회
    CustomApiResponseDto getCustomApiById(String customApiId);

    //커스텀 API 이름으로 검색
    List<CustomApiResponseDto> searchCustomApisByName(String userId, String name);


    //커스텀 API 삭제 (Soft Delete)
    void deleteCustomApi(String customApiId, String userId);
}