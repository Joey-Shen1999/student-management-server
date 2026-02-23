package com.studentmanagement.studentmanagementserver.service;

public class MustChangePasswordRequiredException extends RuntimeException {

    private final String code;

    public MustChangePasswordRequiredException() {
        super("Password change required before accessing this resource.");
        this.code = "MUST_CHANGE_PASSWORD_REQUIRED";
    }

    public String getCode() {
        return code;
    }
}
