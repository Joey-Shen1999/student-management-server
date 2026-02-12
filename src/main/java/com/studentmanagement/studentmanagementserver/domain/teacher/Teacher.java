package com.studentmanagement.studentmanagementserver.domain.teacher;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.user.User;

import javax.persistence.*;

@Entity
@Table(name = "teachers")
public class Teacher extends BaseEntity {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 120)
    private String name;

    protected Teacher() {}

    public Teacher(User user, String name) {
        this.user = user;
        this.name = name;
    }

    public User getUser() { return user; }
    public String getName() { return name; }
}
