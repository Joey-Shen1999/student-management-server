package com.studentmanagement.studentmanagementserver.service;

import org.springframework.http.HttpStatus;

public class StudentInviteException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public StudentInviteException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static StudentInviteException notFound() {
        return new StudentInviteException(HttpStatus.BAD_REQUEST, "INVITE_NOT_FOUND", "Invite token not found.");
    }

    public static StudentInviteException expired() {
        return new StudentInviteException(HttpStatus.BAD_REQUEST, "INVITE_EXPIRED", "Invite token has expired.");
    }

    public static StudentInviteException used() {
        return new StudentInviteException(HttpStatus.BAD_REQUEST, "INVITE_USED", "Invite token has already been used.");
    }

    public static StudentInviteException invalid() {
        return new StudentInviteException(HttpStatus.BAD_REQUEST, "INVITE_INVALID", "Invite token is invalid.");
    }

    public static StudentInviteException roleMismatch() {
        return new StudentInviteException(
                HttpStatus.BAD_REQUEST,
                "INVITE_ROLE_MISMATCH",
                "Invite token can only be used for STUDENT registration."
        );
    }
}
