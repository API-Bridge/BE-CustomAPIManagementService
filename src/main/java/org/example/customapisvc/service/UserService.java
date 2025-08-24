package org.example.customapisvc.service;

import org.example.customapisvc.dto.response.user.UserInfoResponseDto;

/**
 * User 서비스와 통신하는 서비스 인터페이스
 */
public interface UserService {
    
    /**
     * User 서비스에서 사용자 정보를 조회
     * 
     * @param userId 사용자 ID
     * @return 사용자 정보 (플랜 정보 포함)
     */
    UserInfoResponseDto getUserInfo(String userId);
}