package org.example.customapisvc.domain.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "workflow_step", indexes = {
    @Index(name = "idx_custom_api_id", columnList = "custom_api_id"),
    @Index(name = "idx_external_api_id", columnList = "external_api_id"),
    @Index(name = "idx_execution_group", columnList = "execution_group")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStep {

    @Id
    @Column(name = "step_id", length = 36, nullable = false)
    private String stepId;

    @Column(name = "custom_api_id", length = 36, nullable = false)
    private String customApiId;

    @Column(name = "external_api_id", length = 36, nullable = false)
    private String externalApiId;

    @Column(name = "execution_group", nullable = false)
    private Integer executionGroup;

    @Column(name = "alias", nullable = false)
    private String alias;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_api_id", insertable = false, updatable = false, 
                foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private CustomApi customApi;

    @OneToMany(mappedBy = "workflowStep", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ParameterMapping> parameterMappings;
}