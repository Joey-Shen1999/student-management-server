package com.studentmanagement.studentmanagementserver.domain.teacher;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
        name = "teacher_page_preferences",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_teacher_page_preferences_teacher_page_key",
                        columnNames = {"teacher_id", "page_key"}
                )
        },
        indexes = {
                @Index(name = "idx_teacher_page_preferences_teacher", columnList = "teacher_id"),
                @Index(name = "idx_teacher_page_preferences_page_key", columnList = "page_key")
        }
)
public class TeacherPagePreference extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(name = "page_key", nullable = false, length = 160)
    private String pageKey;

    @Column(name = "version", length = 32)
    private String version;

    @Column(name = "visible_column_keys_json", nullable = false, columnDefinition = "text")
    private String visibleColumnKeysJson;

    @Column(name = "ordered_column_keys_json", columnDefinition = "text")
    private String orderedColumnKeysJson;

    protected TeacherPagePreference() {
    }

    public TeacherPagePreference(Teacher teacher, String pageKey) {
        this.teacher = teacher;
        this.pageKey = pageKey;
        this.visibleColumnKeysJson = "[]";
    }

    @PrePersist
    void ensureDefaults() {
        if (this.visibleColumnKeysJson == null) {
            this.visibleColumnKeysJson = "[]";
        }
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public String getPageKey() {
        return pageKey;
    }

    public void setPageKey(String pageKey) {
        this.pageKey = pageKey;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVisibleColumnKeysJson() {
        return visibleColumnKeysJson;
    }

    public void setVisibleColumnKeysJson(String visibleColumnKeysJson) {
        this.visibleColumnKeysJson = visibleColumnKeysJson;
    }

    public String getOrderedColumnKeysJson() {
        return orderedColumnKeysJson;
    }

    public void setOrderedColumnKeysJson(String orderedColumnKeysJson) {
        this.orderedColumnKeysJson = orderedColumnKeysJson;
    }
}
