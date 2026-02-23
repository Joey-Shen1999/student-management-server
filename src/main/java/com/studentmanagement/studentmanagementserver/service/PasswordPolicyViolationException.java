package com.studentmanagement.studentmanagementserver.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PasswordPolicyViolationException extends RuntimeException {

    private final String code;
    private final List<String> details;

    public PasswordPolicyViolationException(List<String> details) {
        super("Password does not meet policy requirements.");
        this.code = "PASSWORD_POLICY_VIOLATION";
        this.details = Collections.unmodifiableList(new ArrayList<String>(details));
    }

    public String getCode() {
        return code;
    }

    public List<String> getDetails() {
        return details;
    }
}
