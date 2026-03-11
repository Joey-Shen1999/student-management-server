package com.studentmanagement.studentmanagementserver.domain.task;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(
        name = "info_tasks",
        indexes = {
                @Index(name = "idx_info_tasks_publisher", columnList = "published_by_teacher_id"),
                @Index(name = "idx_info_tasks_category", columnList = "category")
        }
)
public class InfoTask extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 4000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InfoTaskCategory category;

    @Column(name = "tags_text", nullable = false, length = 1000)
    private String tagsText;

    @Column(name = "target_student_count", nullable = false)
    private int targetStudentCount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by_teacher_id", nullable = false)
    private Teacher publishedByTeacher;

    protected InfoTask() {
    }

    public InfoTask(String title,
                    String content,
                    InfoTaskCategory category,
                    String tagsText,
                    int targetStudentCount,
                    Teacher publishedByTeacher) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.tagsText = tagsText;
        this.targetStudentCount = targetStudentCount;
        this.publishedByTeacher = publishedByTeacher;
    }

    @PrePersist
    void ensureDefaults() {
        if (this.tagsText == null) {
            this.tagsText = "";
        }
        if (this.targetStudentCount < 0) {
            this.targetStudentCount = 0;
        }
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public InfoTaskCategory getCategory() {
        return category;
    }

    public String getTagsText() {
        return tagsText;
    }

    public int getTargetStudentCount() {
        return targetStudentCount;
    }

    public Teacher getPublishedByTeacher() {
        return publishedByTeacher;
    }
}
