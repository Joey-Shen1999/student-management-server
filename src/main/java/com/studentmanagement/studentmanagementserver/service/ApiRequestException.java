package com.studentmanagement.studentmanagementserver.service;

import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ApiRequestException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<String> details;

    public ApiRequestException(HttpStatus status, String code, String message) {
        this(status, code, message, Collections.<String>emptyList());
    }

    public ApiRequestException(HttpStatus status, String code, String message, List<String> details) {
        super(message);
        this.status = status == null ? HttpStatus.BAD_REQUEST : status;
        this.code = code == null || code.trim().isEmpty() ? "BAD_REQUEST" : code;
        this.details = details == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(details));
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<String> getDetails() {
        return details;
    }
}
