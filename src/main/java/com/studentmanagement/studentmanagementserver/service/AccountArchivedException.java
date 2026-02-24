package com.studentmanagement.studentmanagementserver.service;

public class AccountArchivedException extends RuntimeException {

    private final String code;

    public AccountArchivedException() {
        super("This account has been archived. Please contact an admin to enable it.");
        this.code = "ACCOUNT_ARCHIVED";
    }

    public String getCode() {
        return code;
    }
}
