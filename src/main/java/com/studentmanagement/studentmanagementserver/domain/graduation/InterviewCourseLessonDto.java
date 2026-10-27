package com.studentmanagement.studentmanagementserver.domain.graduation;

import java.time.LocalDate;
import java.time.LocalTime;

public class InterviewCourseLessonDto {
    private Long id;
    private int lessonNumber;
    private LocalDate lessonDate;
    private LocalTime lessonTime;
    private boolean confirmed;

    public InterviewCourseLessonDto(Long id, int lessonNumber, LocalDate lessonDate, LocalTime lessonTime, boolean confirmed) {
        this.id = id;
        this.lessonNumber = lessonNumber;
        this.lessonDate = lessonDate;
        this.lessonTime = lessonTime;
        this.confirmed = confirmed;
    }

    public Long getId() { return id; }
    public int getLessonNumber() { return lessonNumber; }
    public LocalDate getLessonDate() { return lessonDate; }
    public LocalTime getLessonTime() { return lessonTime; }
    public boolean isConfirmed() { return confirmed; }
}
