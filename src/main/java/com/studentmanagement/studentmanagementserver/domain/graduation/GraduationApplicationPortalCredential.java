package com.studentmanagement.studentmanagementserver.domain.graduation;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.university.University;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
        name = "graduation_application_portal_credentials",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_grad_app_portal_credentials_student_university",
                columnNames = {"student_id", "university_id"}
        ),
        indexes = {
                @Index(name = "idx_grad_app_portal_credentials_student", columnList = "student_id"),
                @Index(name = "idx_grad_app_portal_credentials_university", columnList = "university_id")
        }
)
public class GraduationApplicationPortalCredential extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @Column(name = "school_account", length = 160)
    private String schoolAccount;

    @Column(name = "school_email", length = 200)
    private String schoolEmail;

    @Column(name = "school_password", length = 200)
    private String schoolPassword;

    protected GraduationApplicationPortalCredential() {
    }

    public GraduationApplicationPortalCredential(Student student, University university) {
        this.student = student;
        this.university = university;
    }

    public Student getStudent() {
        return student;
    }

    public University getUniversity() {
        return university;
    }

    public String getSchoolAccount() {
        return schoolAccount;
    }

    public String getSchoolEmail() {
        return schoolEmail;
    }

    public String getSchoolPassword() {
        return schoolPassword;
    }

    public void updatePortalInfo(String schoolAccount, String schoolEmail, String schoolPassword) {
        this.schoolAccount = schoolAccount;
        this.schoolEmail = schoolEmail;
        this.schoolPassword = schoolPassword;
    }
}
