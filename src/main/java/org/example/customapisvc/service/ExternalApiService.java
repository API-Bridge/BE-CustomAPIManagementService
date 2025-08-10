package org.example.customapisvc.service;

import org.example.customapisvc.dto.request.ExternalApiRequestDto;
import org.example.customapisvc.dto.response.ExternalApiResponseDto;

/**
 * 외부API서비스와 통신하는 서비스 인터페이스
 */
public interface ExternalApiService {
    
    /**
     * 외부API서비스에서 도메인과 키워드에 해당하는 외부 API 리스트를 조회
     * 
     * @param request 도메인과 키워드 정보
     * @return 외부 API 리스트
     */
    ExternalApiResponseDto getExternalApiList(ExternalApiRequestDto request);
}