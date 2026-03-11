package com.studentmanagement.studentmanagementserver.domain.task;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(
        name = "dll_templates",
        indexes = {
                @Index(name = "idx_dll_template_creator", columnList = "created_by_teacher_id")
        }
)
public class DllTemplate extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "payload_schema", nullable = false, length = 8000)
    private String payloadSchema;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_teacher_id", nullable = false)
    private Teacher createdByTeacher;

    protected DllTemplate() {
    }

    public DllTemplate(String name, String description, String payloadSchema, Teacher createdByTeacher) {
        this.name = name;
        this.description = description;
        this.payloadSchema = payloadSchema;
        this.createdByTeacher = createdByTeacher;
    }

    @PrePersist
    void ensureDefaults() {
        if (this.description == null) {
            this.description = "";
        }
        if (this.payloadSchema == null) {
            this.payloadSchema = "{}";
        }
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPayloadSchema() {
        return payloadSchema;
    }

    public Teacher getCreatedByTeacher() {
        return createdByTeacher;
    }
}
