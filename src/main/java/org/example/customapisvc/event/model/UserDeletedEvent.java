package org.example.customapisvc.event.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 사용자 삭제 이벤트
 * 
 * 사용자가 시스템에서 삭제되었을 때 발행되는 이벤트입니다.
 * 다른 마이크로서비스들이 이 이벤트를 구독하여 
 * 사용자 삭제에 따른 정리 작업을 수행할 수 있습니다.
 * 
 * 이벤트 구독 서비스 예시:
 * - 파일 서비스: 사용자 파일 삭제
 * - 알림 서비스: 사용자 관련 알림 정리
 * - 분석 서비스: 사용자 삭제 통계 업데이트
 * - 권한 서비스: 사용자 권한 정리
 * - 결제 서비스: 구독 및 결제 정보 정리
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDeletedEvent extends BaseEvent {
    
    /**
     * 삭제된 사용자의 고유 식별자
     */
    private String userId;
    
    /**
     * Auth0에서 제공하는 사용자 식별자
     */
    private String auth0Id;
    
    /**
     * 사용자 이메일 주소
     */
    private String userEmail;
    
    /**
     * 사용자 삭제 시간
     */
    private LocalDateTime deletedAt;
    
    /**
     * 삭제 사유 (선택적)
     */
    private String deletionReason;
    


    @Override
    public Object getPayload() {
        return this;
    }
}