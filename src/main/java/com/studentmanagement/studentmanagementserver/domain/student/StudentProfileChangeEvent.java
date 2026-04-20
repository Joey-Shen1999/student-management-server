package com.studentmanagement.studentmanagementserver.domain.student;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_profile_change_events",
        indexes = {
                @Index(name = "idx_student_profile_change_events_student_id", columnList = "student_id"),
                @Index(name = "idx_student_profile_change_events_changed_at", columnList = "changed_at"),
                @Index(name = "idx_student_profile_change_events_to_version", columnList = "to_version"),
                @Index(name = "idx_student_profile_change_events_actor_user_id", columnList = "actor_user_id")
        }
)
public class StudentProfileChangeEvent extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "from_version", nullable = false)
    private Long fromVersion;

    @Column(name = "to_version", nullable = false)
    private Long toVersion;

    @Column(name = "change_source", nullable = false, length = 40)
    private String changeSource;

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

    public Long getFromVersion() {
        return fromVersion;
    }

    public void setFromVersion(Long fromVersion) {
        this.fromVersion = fromVersion;
    }

    public Long getToVersion() {
        return toVersion;
    }

    public void setToVersion(Long toVersion) {
        this.toVersion = toVersion;
    }

    public String getChangeSource() {
        return changeSource;
    }

    public void setChangeSource(String changeSource) {
        this.changeSource = changeSource;
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
