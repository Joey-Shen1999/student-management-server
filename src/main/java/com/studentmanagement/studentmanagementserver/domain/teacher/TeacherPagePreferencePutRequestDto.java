package com.studentmanagement.studentmanagementserver.domain.teacher;

import java.util.List;

public class TeacherPagePreferencePutRequestDto {

    private String version;
    private List<String> visibleColumnKeys;
    private List<String> orderedColumnKeys;
    private List<String> pinnedColumnKeys;
    public List<String> getPinnedColumnKeys() { return pinnedColumnKeys; }
    public void setPinnedColumnKeys(List<String> value) { pinnedColumnKeys = value; }

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
