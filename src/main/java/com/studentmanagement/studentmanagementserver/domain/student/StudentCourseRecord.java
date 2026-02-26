package com.studentmanagement.studentmanagementserver.domain.student;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.enums.SchoolType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(
        name = "student_course_record",
        indexes = @Index(name = "idx_student_course_record_student_id", columnList = "student_id")
)
public class StudentCourseRecord extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "school_type", length = 10)
    private SchoolType schoolType;

    @Column(name = "school_name", length = 200)
    private String schoolName;

    @Column(name = "course_code", length = 80)
    private String courseCode;

    @Column(name = "street_address", length = 255)
    private String streetAddress;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "state", length = 120)
    private String state;

    @Column(name = "country", length = 120)
    private String country;

    @Column(name = "postal", length = 30)
    private String postal;

    private Integer mark;

    @Column(name = "grade_level")
    private Integer gradeLevel;

    @Column(name = "start_time")
    private LocalDate startTime;

    @Column(name = "end_time")
    private LocalDate endTime;

    protected StudentCourseRecord() {
    }

    public StudentCourseRecord(Student student,
                               String schoolName,
                               String courseCode,
                               Integer mark,
                               Integer gradeLevel,
                               LocalDate startTime,
                               LocalDate endTime) {
        this(student, null, schoolName, null, null, null, null, null, courseCode, mark, gradeLevel, startTime, endTime);
    }

    public StudentCourseRecord(Student student,
                               SchoolType schoolType,
                               String schoolName,
                               String streetAddress,
                               String city,
                               String state,
                               String country,
                               String postal,
                               String courseCode,
                               Integer mark,
                               Integer gradeLevel,
                               LocalDate startTime,
                               LocalDate endTime) {
        this.student = student;
        this.schoolType = schoolType;
        this.schoolName = schoolName;
        this.streetAddress = streetAddress;
        this.city = city;
        this.state = state;
        this.country = country;
        this.postal = postal;
        this.courseCode = courseCode;
        this.mark = mark;
        this.gradeLevel = gradeLevel;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Student getStudent() {
        return student;
    }

    public SchoolType getSchoolType() {
        return schoolType;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getCountry() {
        return country;
    }

    public String getPostal() {
        return postal;
    }

    public Integer getMark() {
        return mark;
    }

    public Integer getGradeLevel() {
        return gradeLevel;
    }

    public LocalDate getStartTime() {
        return startTime;
    }

    public LocalDate getEndTime() {
        return endTime;
    }
}
