package org.example.customapisvc.service;

import org.example.customapisvc.dto.request.InitiateCreationRequestDto;
import org.example.customapisvc.dto.response.CustomApiResponseDto;

/**
 * AI를 활용한 커스텀 API 자동 생성 서비스 인터페이스
 */
public interface AiCustomApiGenerationService {
    
    /**
     * AI를 통해 커스텀 API를 자동 생성합니다.
     * 
     * @param request 커스텀 API 생성 개시 요청 정보
     * @return 생성된 커스텀 API 정보
     */
    CustomApiResponseDto generateCustomApiWithAi(InitiateCreationRequestDto request);
}