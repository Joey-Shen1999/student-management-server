package com.studentmanagement.studentmanagementserver.domain.ielts;

import java.util.List;

public class StudentIeltsModuleStateDto {
    private Long studentId;
    private Integer graduationYear;
    private boolean hasTakenIeltsAcademic;
    private String preparationIntent;
    private List<StudentIeltsRecordDto> records;
    private StudentIeltsLanguageRiskDto languageRisk;
    private String updatedAt;

    public StudentIeltsModuleStateDto(Long studentId,
                                      Integer graduationYear,
                                      boolean hasTakenIeltsAcademic,
                                      String preparationIntent,
                                      List<StudentIeltsRecordDto> records,
                                      StudentIeltsLanguageRiskDto languageRisk,
                                      String updatedAt) {
        this.studentId = studentId;
        this.graduationYear = graduationYear;
        this.hasTakenIeltsAcademic = hasTakenIeltsAcademic;
        this.preparationIntent = preparationIntent;
        this.records = records;
        this.languageRisk = languageRisk;
        this.updatedAt = updatedAt;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public boolean isHasTakenIeltsAcademic() {
        return hasTakenIeltsAcademic;
    }

    public String getPreparationIntent() {
        return preparationIntent;
    }

    public List<StudentIeltsRecordDto> getRecords() {
        return records;
    }

    public StudentIeltsLanguageRiskDto getLanguageRisk() {
        return languageRisk;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
