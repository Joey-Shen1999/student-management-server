package com.studentmanagement.studentmanagementserver.domain.graduation;

public class InterviewCourseRequirementDto {
    private final Long id;
    private final String universityName;
    private final String programName;

    public InterviewCourseRequirementDto(Long id, String universityName, String programName) {
        this.id = id;
        this.universityName = universityName;
        this.programName = programName;
    }

    public Long getId() { return id; }
    public String getUniversityName() { return universityName; }
    public String getProgramName() { return programName; }
}
