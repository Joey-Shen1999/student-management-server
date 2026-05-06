package com.studentmanagement.studentmanagementserver.domain.extracurricular;

import java.util.ArrayList;
import java.util.List;

public class ExtracurricularTrackingDto {
    private Long studentId;
    private String note;
    private int totalActivities;
    private int competitionCount;
    private int awardCount;
    private List<ExtracurricularActivityDto> activities;
    private String createdAt;
    private String updatedAt;
    private Long updatedByTeacherId;
    private String updatedByTeacherName;
    private List<ExtracurricularTrackingRecordDto> records;

    public ExtracurricularTrackingDto(Long studentId,
                                      String note,
                                      int totalActivities,
                                      int competitionCount,
                                      int awardCount,
                                      List<ExtracurricularActivityDto> activities,
                                      String createdAt,
                                      String updatedAt,
                                      Long updatedByTeacherId,
                                      String updatedByTeacherName,
                                      List<ExtracurricularTrackingRecordDto> records) {
        this.studentId = studentId;
        this.note = note;
        this.totalActivities = totalActivities;
        this.competitionCount = competitionCount;
        this.awardCount = awardCount;
        this.activities = activities == null
                ? new ArrayList<ExtracurricularActivityDto>()
                : activities;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedByTeacherId = updatedByTeacherId;
        this.updatedByTeacherName = updatedByTeacherName;
        this.records = records == null
                ? new ArrayList<ExtracurricularTrackingRecordDto>()
                : records;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getNote() {
        return note;
    }

    public int getTotalActivities() {
        return totalActivities;
    }

    public int getCompetitionCount() {
        return competitionCount;
    }

    public int getAwardCount() {
        return awardCount;
    }

    public List<ExtracurricularActivityDto> getActivities() {
        return activities;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public Long getUpdatedByTeacherId() {
        return updatedByTeacherId;
    }

    public String getUpdatedByTeacherName() {
        return updatedByTeacherName;
    }

    public List<ExtracurricularTrackingRecordDto> getRecords() {
        return records;
    }

    public static class ExtracurricularTrackingRecordDto {
        private Long id;
        private String title;
        private String note;
        private int totalActivities;
        private int competitionCount;
        private int awardCount;
        private List<ExtracurricularActivityDto> activities;
        private String createdAt;
        private String updatedAt;
        private Long updatedByTeacherId;
        private String updatedByTeacherName;

        public ExtracurricularTrackingRecordDto(Long id,
                                                String title,
                                                String note,
                                                int totalActivities,
                                                int competitionCount,
                                                int awardCount,
                                                List<ExtracurricularActivityDto> activities,
                                                String createdAt,
                                                String updatedAt,
                                                Long updatedByTeacherId,
                                                String updatedByTeacherName) {
            this.id = id;
            this.title = title;
            this.note = note;
            this.totalActivities = totalActivities;
            this.competitionCount = competitionCount;
            this.awardCount = awardCount;
            this.activities = activities;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.updatedByTeacherId = updatedByTeacherId;
            this.updatedByTeacherName = updatedByTeacherName;
        }

        public Long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getNote() {
            return note;
        }

        public int getTotalActivities() {
            return totalActivities;
        }

        public int getCompetitionCount() {
            return competitionCount;
        }

        public int getAwardCount() {
            return awardCount;
        }

        public List<ExtracurricularActivityDto> getActivities() {
            return activities;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public Long getUpdatedByTeacherId() {
            return updatedByTeacherId;
        }

        public String getUpdatedByTeacherName() {
            return updatedByTeacherName;
        }
    }
}
