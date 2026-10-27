package com.studentmanagement.studentmanagementserver.domain.graduation;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "interview_course_lessons", uniqueConstraints = @UniqueConstraint(
        name = "uk_interview_course_lesson_number", columnNames = {"plan_id", "lesson_number"}))
public class InterviewCourseLesson extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private InterviewCoursePlan plan;

    @Column(name = "lesson_number", nullable = false)
    private int lessonNumber;

    @Column(name = "lesson_date")
    private LocalDate lessonDate;

    @Column(name = "lesson_time")
    private LocalTime lessonTime;

    @Column(name = "confirmed", nullable = false)
    private boolean confirmed;

    protected InterviewCourseLesson() {}

    public InterviewCourseLesson(InterviewCoursePlan plan, int lessonNumber) {
        this.plan = plan;
        this.lessonNumber = lessonNumber;
    }

    public InterviewCoursePlan getPlan() { return plan; }
    public int getLessonNumber() { return lessonNumber; }
    public LocalDate getLessonDate() { return lessonDate; }
    public LocalTime getLessonTime() { return lessonTime; }
    public boolean isConfirmed() { return confirmed; }
    public void schedule(LocalDate lessonDate, LocalTime lessonTime) {
        if (!Objects.equals(this.lessonDate, lessonDate) || !Objects.equals(this.lessonTime, lessonTime)
                || lessonDate == null || lessonTime == null) {
            this.confirmed = false;
        }
        this.lessonDate = lessonDate;
        this.lessonTime = lessonTime;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed && lessonDate != null && lessonTime != null;
    }
}
