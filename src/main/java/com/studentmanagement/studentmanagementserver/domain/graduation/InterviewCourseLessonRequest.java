package com.studentmanagement.studentmanagementserver.domain.graduation;

import java.time.LocalDate;
import java.time.LocalTime;

public class InterviewCourseLessonRequest {
    private LocalDate lessonDate;
    private LocalTime lessonTime;
    private Boolean confirmed;
    public LocalDate getLessonDate() { return lessonDate; }
    public void setLessonDate(LocalDate lessonDate) { this.lessonDate = lessonDate; }
    public LocalTime getLessonTime() { return lessonTime; }
    public void setLessonTime(LocalTime lessonTime) { this.lessonTime = lessonTime; }
    public Boolean getConfirmed() { return confirmed; }
    public void setConfirmed(Boolean confirmed) { this.confirmed = confirmed; }
}
