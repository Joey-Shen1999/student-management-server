package com.studentmanagement.studentmanagementserver.domain.graduation;

import java.util.List;

public class GraduationApplicationConfirmRequest {
    private List<GraduationApplicationRequest> applications;

    public List<GraduationApplicationRequest> getApplications() {
        return applications;
    }

    public void setApplications(List<GraduationApplicationRequest> applications) {
        this.applications = applications;
    }
}
