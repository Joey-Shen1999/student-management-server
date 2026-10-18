package com.studentmanagement.studentmanagementserver.domain.graduation;

import java.time.LocalDate;
import java.time.LocalTime;

public class InterviewCourseLessonDto {
    private Long id;
    private int lessonNumber;
    private LocalDate lessonDate;
    private LocalTime lessonTime;

    public InterviewCourseLessonDto(Long id, int lessonNumber, LocalDate lessonDate, LocalTime lessonTime) {
        this.id = id;
        this.lessonNumber = lessonNumber;
        this.lessonDate = lessonDate;
        this.lessonTime = lessonTime;
    }

    public Long getId() { return id; }
    public int getLessonNumber() { return lessonNumber; }
    public LocalDate getLessonDate() { return lessonDate; }
    public LocalTime getLessonTime() { return lessonTime; }
}
