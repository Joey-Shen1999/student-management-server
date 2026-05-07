package com.studentmanagement.studentmanagementserver.domain.serviceprogress;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "service_progress_record",
        indexes = {
                @Index(name = "idx_service_progress_student_id", columnList = "student_id"),
                @Index(name = "idx_service_progress_appointment_time", columnList = "appointment_time")
        }
)
public class ServiceProgressRecord extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "appointment_time", nullable = false)
    private LocalDateTime appointmentTime;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "advisor_id", nullable = false)
    private Teacher advisor;

    @Column(name = "follow_up_content", length = 5000)
    private String followUpContent;

    @Column(name = "next_plan", length = 5000)
    private String nextPlan;

    protected ServiceProgressRecord() {
    }

    public ServiceProgressRecord(Student student, LocalDateTime appointmentTime, Teacher advisor) {
        this.student = student;
        this.appointmentTime = appointmentTime;
        this.advisor = advisor;
    }

    public Student getStudent() {
        return student;
    }

    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalDateTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public Teacher getAdvisor() {
        return advisor;
    }

    public void setAdvisor(Teacher advisor) {
        this.advisor = advisor;
    }

    public String getFollowUpContent() {
        return followUpContent;
    }

    public void setFollowUpContent(String followUpContent) {
        this.followUpContent = followUpContent;
    }

    public String getNextPlan() {
        return nextPlan;
    }

    public void setNextPlan(String nextPlan) {
        this.nextPlan = nextPlan;
    }
}
