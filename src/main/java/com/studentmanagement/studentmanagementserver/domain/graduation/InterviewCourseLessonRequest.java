package com.studentmanagement.studentmanagementserver.domain.graduation;

import java.time.LocalDate;
import java.time.LocalTime;

public class InterviewCourseLessonRequest {
    private LocalDate lessonDate;
    private LocalTime lessonTime;
    public LocalDate getLessonDate() { return lessonDate; }
    public void setLessonDate(LocalDate lessonDate) { this.lessonDate = lessonDate; }
    public LocalTime getLessonTime() { return lessonTime; }
    public void setLessonTime(LocalTime lessonTime) { this.lessonTime = lessonTime; }
}
