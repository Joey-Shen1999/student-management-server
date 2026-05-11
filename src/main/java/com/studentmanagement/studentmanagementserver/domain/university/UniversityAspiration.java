package com.studentmanagement.studentmanagementserver.domain.university;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.student.Student;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(
        name = "university_aspirations",
        indexes = {
                @Index(name = "idx_university_aspirations_student_sort", columnList = "student_id,sort_order"),
                @Index(name = "idx_university_aspirations_university", columnList = "university_id"),
                @Index(name = "idx_university_aspirations_program", columnList = "program_id")
        }
)
public class UniversityAspiration extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private UniversityProgram program;

    @Column(length = 5000)
    private String notes;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected UniversityAspiration() {
    }

    public UniversityAspiration(Student student, University university, UniversityProgram program, int sortOrder) {
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
