package com.studentmanagement.studentmanagementserver.domain.graduation;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "interview_course_requirements")
public class InterviewCourseRequirement extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private InterviewCoursePlan plan;

    @Column(name = "university_name", nullable = false, length = 200)
    private String universityName;

    @Column(name = "program_name", nullable = false, length = 200)
    private String programName;

    protected InterviewCourseRequirement() {}

    public InterviewCourseRequirement(InterviewCoursePlan plan, String universityName, String programName) {
        this.plan = plan;
        change(universityName, programName);
    }

    public void change(String universityName, String programName) {
        this.universityName = universityName;
        this.programName = programName;
    }

    public String getUniversityName() { return universityName; }
    public String getProgramName() { return programName; }
    public InterviewCoursePlan getPlan() { return plan; }
}
