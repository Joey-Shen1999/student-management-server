package com.studentmanagement.studentmanagementserver.domain.student;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.user.User;

import javax.persistence.*;

@Entity
@Table(name = "students")
public class Student extends BaseEntity {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(nullable = false, length = 80)
    private String lastName;

    @Column(length = 80)
    private String nickName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    protected Student() {}

    public Student(User user, String firstName, String lastName, String nickName) {
        this(user, firstName, lastName, nickName, null);
    }

    public Student(User user, String firstName, String lastName, String nickName, Teacher teacher) {
        this.user = user;
        this.firstName = firstName;
        this.lastName = lastName;
        this.nickName = nickName;
        this.teacher = teacher;
    }

    public User getUser() { return user; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getNickName() { return nickName; }
    public Teacher getTeacher() { return teacher; }

    public void updateProfileNames(String firstName, String lastName, String nickName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.nickName = nickName;
    }
}
