package com.studentmanagement.studentmanagementserver.domain.graduation;

public class InterviewCourseRequirementRequest {
    private String universityName;
    private String programName;

    public String getUniversityName() { return universityName; }
    public void setUniversityName(String universityName) { this.universityName = universityName; }
    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }
}
