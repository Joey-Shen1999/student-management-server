package com.studentmanagement.studentmanagementserver.domain.graduation;

import java.util.ArrayList;
import java.util.List;

public class GraduationApplicationUniversityStudentDto {
    private Long studentId;
    private String studentName;
    private String username;
    private List<GraduationApplicationDto> applications = new ArrayList<GraduationApplicationDto>();

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<GraduationApplicationDto> getApplications() {
        return applications;
    }

    public void setApplications(List<GraduationApplicationDto> applications) {
        this.applications = applications == null
                ? new ArrayList<GraduationApplicationDto>()
                : applications;
    }
}
