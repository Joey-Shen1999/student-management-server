package com.studentmanagement.studentmanagementserver.domain.graduation;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "graduation_application_change_events",
        indexes = {
                @Index(name = "idx_grad_app_change_events_student_id", columnList = "student_id"),
                @Index(name = "idx_grad_app_change_events_application_id", columnList = "application_id"),
                @Index(name = "idx_grad_app_change_events_changed_at", columnList = "changed_at"),
                @Index(name = "idx_grad_app_change_events_actor_user_id", columnList = "actor_user_id"),
                @Index(name = "idx_grad_app_change_events_operation", columnList = "operation")
        }
)
public class GraduationApplicationChangeEvent extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "operation", nullable = false, length = 60)
    private String operation;

    @Column(name = "changed_fields_json", columnDefinition = "TEXT")
    private String changedFieldsJson;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_role", length = 40)
    private String actorRole;

    @Column(name = "actor_name", length = 120)
    private String actorName;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "request_id", length = 120)
    private String requestId;

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getChangedFieldsJson() {
        return changedFieldsJson;
    }

    public void setChangedFieldsJson(String changedFieldsJson) {
        this.changedFieldsJson = changedFieldsJson;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
