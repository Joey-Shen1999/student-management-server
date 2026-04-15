package com.studentmanagement.studentmanagementserver.domain.volunteer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class VolunteerTrackingDto {
    private Long studentId;
    private BigDecimal totalHours;
    private String note;
    private List<VolunteerTrackingTaskDto> tasks;
    private String createdAt;
    private String updatedAt;
    private Long updatedByTeacherId;
    private String updatedByTeacherName;
    private List<VolunteerTrackingRecordDto> records;

    public VolunteerTrackingDto(Long studentId,
                                BigDecimal totalHours,
                                String note,
                                List<VolunteerTrackingTaskDto> tasks,
                                String createdAt,
                                String updatedAt,
                                Long updatedByTeacherId,
                                String updatedByTeacherName,
                                List<VolunteerTrackingRecordDto> records) {
        this.studentId = studentId;
        this.totalHours = totalHours;
        this.note = note;
        this.tasks = tasks;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedByTeacherId = updatedByTeacherId;
        this.updatedByTeacherName = updatedByTeacherName;
        this.records = records == null
                ? new ArrayList<VolunteerTrackingRecordDto>()
                : records;
    }

    public Long getStudentId() {
        return studentId;
    }

    public BigDecimal getTotalHours() {
        return totalHours;
    }

    public String getNote() {
        return note;
    }

    public List<VolunteerTrackingTaskDto> getTasks() {
        return tasks;
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

    public List<VolunteerTrackingRecordDto> getRecords() {
        return records;
    }

    public static class VolunteerTrackingRecordDto {
        private Long id;
        private String title;
        private String note;
        private BigDecimal totalHours;
        private List<VolunteerTrackingTaskDto> tasks;
        private String createdAt;
        private String updatedAt;
        private Long updatedByTeacherId;
        private String updatedByTeacherName;

        public VolunteerTrackingRecordDto(Long id,
                                          String title,
                                          String note,
                                          BigDecimal totalHours,
                                          List<VolunteerTrackingTaskDto> tasks,
                                          String createdAt,
                                          String updatedAt,
                                          Long updatedByTeacherId,
                                          String updatedByTeacherName) {
            this.id = id;
            this.title = title;
            this.note = note;
            this.totalHours = totalHours;
            this.tasks = tasks;
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

        public BigDecimal getTotalHours() {
            return totalHours;
        }

        public List<VolunteerTrackingTaskDto> getTasks() {
            return tasks;
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
