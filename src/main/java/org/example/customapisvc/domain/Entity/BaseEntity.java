package org.example.customapisvc.domain.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모든 엔티티의 공통 필드를 관리하는 기본 엔티티 클래스
 * 
 * 공통 필드:
 * - createdAt: 생성 일시 (자동 설정, 수정 불가)
 * - updatedAt: 수정 일시 (자동 갱신)
 * - deleted: 소프트 삭제 플래그
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;
    
    /**
     * 소프트 삭제 처리
     */
    public void softDelete() {
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 소프트 삭제 복구
     */
    public void restore() {
        this.deleted = false;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 삭제 여부 확인
     */
    public boolean isDeleted() {
        return this.deleted != null && this.deleted;
    }
}