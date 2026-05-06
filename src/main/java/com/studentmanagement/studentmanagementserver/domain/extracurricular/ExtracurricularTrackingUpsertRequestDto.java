package com.studentmanagement.studentmanagementserver.domain.extracurricular;

import java.util.List;

public class ExtracurricularTrackingUpsertRequestDto {
    private String note;
    private List<ExtracurricularActivityUpsertDto> activities;

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<ExtracurricularActivityUpsertDto> getActivities() {
        return activities;
    }

    public void setActivities(List<ExtracurricularActivityUpsertDto> activities) {
        this.activities = activities;
    }
}
