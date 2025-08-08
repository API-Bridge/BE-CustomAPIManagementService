package org.example.customapisvc.domain.Entity;

import org.example.customapisvc.domain.enums.ParameterSourceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "parameter_mapping", indexes = {
    @Index(name = "idx_step_id", columnList = "step_id"),
    @Index(name = "idx_source_type", columnList = "source_type")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParameterMapping {

    @Id
    @Column(name = "mapping_id", length = 36, nullable = false)
    private String mappingId;

    @Column(name = "step_id", length = 36, nullable = false)
    private String stepId;

    @Column(name = "target_param_name", nullable = false)
    private String targetParamName;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 50, nullable = false)
    private ParameterSourceType sourceType;

    @Column(name = "source_step_alias")
    private String sourceStepAlias;

    @Column(name = "source_data_path")
    private String sourceDataPath;

    @Column(name = "static_value", columnDefinition = "TEXT")
    private String staticValue;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", insertable = false, updatable = false, 
                foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private WorkflowStep workflowStep;
}