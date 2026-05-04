package com.studentmanagement.studentmanagementserver.domain.task;

public class CreateGoalRequestDto {
    private Long studentId;
    private String title;
    private String description;
    private String dueAt;
    private String cycleType;
    private String cycleFrequency;
    private Integer cycleInterval;
    private String cycleUnit;
    private String cycleLabel;
    private String cycleEndAt;
    private Boolean cycleNoEnd;

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDueAt() {
        return dueAt;
    }

    public void setDueAt(String dueAt) {
        this.dueAt = dueAt;
    }

    public String getCycleType() {
        return cycleType;
    }

    public void setCycleType(String cycleType) {
        this.cycleType = cycleType;
    }

    public String getCycleFrequency() {
        return cycleFrequency;
    }

    public void setCycleFrequency(String cycleFrequency) {
        this.cycleFrequency = cycleFrequency;
    }

    public Integer getCycleInterval() {
        return cycleInterval;
    }

    public void setCycleInterval(Integer cycleInterval) {
        this.cycleInterval = cycleInterval;
    }

    public String getCycleUnit() {
        return cycleUnit;
    }

    public void setCycleUnit(String cycleUnit) {
        this.cycleUnit = cycleUnit;
    }

    public String getCycleLabel() {
        return cycleLabel;
    }

    public void setCycleLabel(String cycleLabel) {
        this.cycleLabel = cycleLabel;
    }

    public String getCycleEndAt() {
        return cycleEndAt;
    }

    public void setCycleEndAt(String cycleEndAt) {
        this.cycleEndAt = cycleEndAt;
    }

    public Boolean getCycleNoEnd() {
        return cycleNoEnd;
    }

    public void setCycleNoEnd(Boolean cycleNoEnd) {
        this.cycleNoEnd = cycleNoEnd;
    }
}
