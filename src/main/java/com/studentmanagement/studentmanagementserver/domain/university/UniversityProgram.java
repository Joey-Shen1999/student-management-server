package com.studentmanagement.studentmanagementserver.domain.university;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(
        name = "university_programs",
        indexes = {
                @Index(name = "idx_university_programs_university_active", columnList = "university_id,active"),
                @Index(name = "idx_university_programs_name", columnList = "program_name")
        }
)
public class UniversityProgram extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @Column(name = "program_name", nullable = false, length = 180)
    private String programName;

    @Column(name = "faculty_name", length = 180)
    private String facultyName;

    @Column(name = "degree_type", length = 40)
    private String degreeType;

    @Column(nullable = false)
    private boolean active = true;

    protected UniversityProgram() {
    }

    public UniversityProgram(University university, String programName, String facultyName, String degreeType) {
        this.university = university;
        this.programName = programName;
        this.facultyName = facultyName;
        this.degreeType = degreeType;
        this.active = true;
    }

    public University getUniversity() {
        return university;
    }

    public void setUniversity(University university) {
        this.university = university;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public String getDegreeType() {
        return degreeType;
    }

    public void setDegreeType(String degreeType) {
        this.degreeType = degreeType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
