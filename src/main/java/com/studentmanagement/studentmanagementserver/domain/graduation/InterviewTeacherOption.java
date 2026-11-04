package com.studentmanagement.studentmanagementserver.domain.graduation;

public class InterviewTeacherOption {
    private final Long teacherId;
    private final String name;
    private final String username;
    public InterviewTeacherOption(Long teacherId, String name, String username) {
        this.teacherId = teacherId; this.name = name; this.username = username;
    }
    public Long getTeacherId() { return teacherId; }
    public String getName() { return name; }
    public String getUsername() { return username; }
}
