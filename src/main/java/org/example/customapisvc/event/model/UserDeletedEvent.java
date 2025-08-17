package org.example.customapisvc.event.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 사용자 삭제 이벤트
 * 다른 마이크로서비스에서 사용자가 삭제되었을 때 발행되는 이벤트
 * 이 이벤트를 수신하여 해당 사용자의 모든 커스텀 API를 삭제 처리
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserDeletedEvent extends BaseEvent {

    /** 삭제된 사용자의 ID */
    private String userId;
    
    /** 사용자 삭제 사유 (선택사항) */
    private String deletionReason;


    /**
     * 사용자 삭제 이벤트 생성자 (삭제 사유 포함)
     * 
     * @param userId 삭제된 사용자 ID
     * @param deletionReason 삭제 사유
     */
    public UserDeletedEvent(String userId, String deletionReason) {
        super("USER_DELETED");
        this.userId = userId;
        this.deletionReason = deletionReason;
    }
}