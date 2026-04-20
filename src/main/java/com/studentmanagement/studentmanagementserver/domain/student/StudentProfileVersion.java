package com.studentmanagement.studentmanagementserver.domain.student;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_profile_versions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_student_profile_versions_student_version",
                        columnNames = {"student_id", "profile_version"}
                )
        },
        indexes = {
                @Index(name = "idx_student_profile_versions_student_id", columnList = "student_id"),
                @Index(name = "idx_student_profile_versions_profile_version", columnList = "profile_version")
        }
)
public class StudentProfileVersion extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "profile_version", nullable = false)
    private Long profileVersion;

    @Column(name = "profile_snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String profileSnapshotJson;

    @Column(name = "snapshot_hash", length = 128)
    private String snapshotHash;

    @Column(name = "previous_hash", length = 128)
    private String previousHash;

    @Column(name = "changed_by_user_id")
    private Long changedByUserId;

    @Column(name = "changed_by_role", length = 40)
    private String changedByRole;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "change_event_id")
    private Long changeEventId;

    @Column(name = "request_id", length = 120)
    private String requestId;

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getProfileVersion() {
        return profileVersion;
    }

    public void setProfileVersion(Long profileVersion) {
        this.profileVersion = profileVersion;
    }

    public String getProfileSnapshotJson() {
        return profileSnapshotJson;
    }

    public void setProfileSnapshotJson(String profileSnapshotJson) {
        this.profileSnapshotJson = profileSnapshotJson;
    }

    public String getSnapshotHash() {
        return snapshotHash;
    }

    public void setSnapshotHash(String snapshotHash) {
        this.snapshotHash = snapshotHash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public Long getChangedByUserId() {
        return changedByUserId;
    }

    public void setChangedByUserId(Long changedByUserId) {
        this.changedByUserId = changedByUserId;
    }

    public String getChangedByRole() {
        return changedByRole;
    }

    public void setChangedByRole(String changedByRole) {
        this.changedByRole = changedByRole;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public Long getChangeEventId() {
        return changeEventId;
    }

    public void setChangeEventId(Long changeEventId) {
        this.changeEventId = changeEventId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
