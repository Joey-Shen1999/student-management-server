package com.studentmanagement.studentmanagementserver.domain.teacher;

import java.util.List;

public class TeacherPagePreferencePutRequestDto {

    private String version;
    private List<String> visibleColumnKeys;
    private List<String> orderedColumnKeys;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getVisibleColumnKeys() {
        return visibleColumnKeys;
    }

    public void setVisibleColumnKeys(List<String> visibleColumnKeys) {
        this.visibleColumnKeys = visibleColumnKeys;
    }

    public List<String> getOrderedColumnKeys() {
        return orderedColumnKeys;
    }

    public void setOrderedColumnKeys(List<String> orderedColumnKeys) {
        this.orderedColumnKeys = orderedColumnKeys;
    }
}
