package com.studentmanagement.studentmanagementserver.domain.graduation;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.student.Student;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
        name = "graduation_application_account_credentials",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_grad_app_account_credentials_student",
                columnNames = {"student_id"}
        ),
        indexes = {
                @Index(name = "idx_grad_app_account_credentials_student", columnList = "student_id")
        }
)
public class GraduationApplicationAccountCredential extends BaseEntity {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "application_email", length = 200)
    private String applicationEmail;

    @Column(name = "application_password", length = 200)
    private String applicationPassword;

    protected GraduationApplicationAccountCredential() {
    }

    public GraduationApplicationAccountCredential(Student student) {
        this.student = student;
    }

    public Student getStudent() {
        return student;
    }

    public String getApplicationEmail() {
        return applicationEmail;
    }

    public String getApplicationPassword() {
        return applicationPassword;
    }

    public void updateAccountInfo(String applicationEmail, String applicationPassword) {
        this.applicationEmail = applicationEmail;
        this.applicationPassword = applicationPassword;
    }
}
