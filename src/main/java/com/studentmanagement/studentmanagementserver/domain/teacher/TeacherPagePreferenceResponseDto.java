package com.studentmanagement.studentmanagementserver.domain.teacher;

import java.util.List;

public class TeacherPagePreferenceResponseDto {

    private final String pageKey;
    private final String version;
    private final List<String> visibleColumnKeys;
    private final List<String> orderedColumnKeys;
    private final String updatedAt;

    public TeacherPagePreferenceResponseDto(String pageKey,
                                            String version,
                                            List<String> visibleColumnKeys,
                                            List<String> orderedColumnKeys,
                                            String updatedAt) {
        this.pageKey = pageKey;
        this.version = version;
        this.visibleColumnKeys = visibleColumnKeys;
        this.orderedColumnKeys = orderedColumnKeys;
        this.updatedAt = updatedAt;
    }

    public String getPageKey() {
        return pageKey;
    }

    public String getVersion() {
        return version;
    }

    public List<String> getVisibleColumnKeys() {
        return visibleColumnKeys;
    }

    public List<String> getOrderedColumnKeys() {
        return orderedColumnKeys;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
