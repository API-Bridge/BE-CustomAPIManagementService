package org.example.customapisvc.domain.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * API 공유(가져오기) 관계 엔티티
 * 어떤 사용자가 어떤 원본 API를 자신의 목록으로 가져갔는지를 관리합니다.
 * 
 * Association Table 패턴을 사용하여 CustomApi 간의 복잡한 자기참조 관계를 단순화하고,
 * 데이터 무결성과 확장성을 보장합니다.
 */
@Entity
@Table(name = "api_share", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_origin_api_user", columnNames = {"origin_api_id", "user_id"})
       },
       indexes = {
           @Index(name = "idx_share_user_id", columnList = "user_id"),
           @Index(name = "idx_share_origin_api", columnList = "origin_api_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "originApi") // 순환 참조 방지
public class ApiShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "share_id")
    private Long shareId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_api_id", nullable = false)
    private CustomApi originApi;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "alias")
    private String alias;

    @Column(name = "shared_at", nullable = false)
    private LocalDateTime sharedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @PrePersist
    protected void onCreate() {
        if (sharedAt == null) {
            sharedAt = LocalDateTime.now();
        }
    }

    // 비즈니스 메서드
    public String getOriginApiId() {
        return originApi != null ? originApi.getCustomApiId() : null;
    }

    // 편의 생성자
    public ApiShare(CustomApi originApi, String userId) {
        this.originApi = originApi;
        this.userId = userId;
        this.sharedAt = LocalDateTime.now();
    }

    public ApiShare(CustomApi originApi, String userId, String alias) {
        this.originApi = originApi;
        this.userId = userId;
        this.alias = alias;
        this.sharedAt = LocalDateTime.now();
    }
}