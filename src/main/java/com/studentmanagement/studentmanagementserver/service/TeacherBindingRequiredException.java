package com.studentmanagement.studentmanagementserver.service;

public class TeacherBindingRequiredException extends RuntimeException {

    private final String code;

    public TeacherBindingRequiredException() {
        super("Teacher binding is required for this account.");
        this.code = "TEACHER_BINDING_REQUIRED";
    }

    public String getCode() {
        return code;
    }
}
