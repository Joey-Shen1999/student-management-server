package com.studentmanagement.studentmanagementserver.domain.graduation;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.university.University;
import com.studentmanagement.studentmanagementserver.domain.university.UniversityProgram;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(
        name = "graduation_applications",
        indexes = {
                @Index(name = "idx_graduation_applications_student_sort", columnList = "student_id,sort_order"),
                @Index(name = "idx_graduation_applications_university", columnList = "university_id"),
                @Index(name = "idx_graduation_applications_program", columnList = "program_id"),
                @Index(name = "idx_graduation_applications_status", columnList = "status")
        }
)
public class GraduationApplication extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private UniversityProgram program;

    @Column(name = "source_aspiration_id")
    private Long sourceAspirationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private GraduationApplicationStatus status = GraduationApplicationStatus.PREPARING;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected GraduationApplication() {
    }

    public GraduationApplication(Student student,
                                 University university,
                                 UniversityProgram program,
                                 int sortOrder) {
        this.student = student;
        this.university = university;
        this.program = program;
        this.sortOrder = sortOrder;
    }

    public Student getStudent() {
        return student;
    }

    public University getUniversity() {
        return university;
    }

    public void setUniversity(University university) {
        this.university = university;
    }

    public UniversityProgram getProgram() {
        return program;
    }

    public void setProgram(UniversityProgram program) {
        this.program = program;
    }

    public Long getSourceAspirationId() {
        return sourceAspirationId;
    }

    public void setSourceAspirationId(Long sourceAspirationId) {
        this.sourceAspirationId = sourceAspirationId;
    }

    public GraduationApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(GraduationApplicationStatus status) {
        this.status = status == null ? GraduationApplicationStatus.PREPARING : status;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
