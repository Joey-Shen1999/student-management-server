package com.studentmanagement.studentmanagementserver.web;

import com.studentmanagement.studentmanagementserver.service.MustChangePasswordRequiredException;
import com.studentmanagement.studentmanagementserver.service.PasswordPolicyViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PasswordPolicyViolationException.class)
    public ResponseEntity<ApiError> handlePasswordPolicyViolation(PasswordPolicyViolationException e) {
        ApiError body = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                e.getCode(),
                e.getDetails()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MustChangePasswordRequiredException.class)
    public ResponseEntity<ApiError> handleMustChangePassword(MustChangePasswordRequiredException e) {
        ApiError body = new ApiError(
                HttpStatus.FORBIDDEN.value(),
                e.getMessage(),
                e.getCode(),
                Collections.<String>emptyList()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException e) {
        String msg = (e.getMessage() == null || e.getMessage().trim().isEmpty())
                ? "Bad request"
                : e.getMessage();

        HttpStatus status = HttpStatus.BAD_REQUEST;
        String code = "BAD_REQUEST";
        if (msg.toLowerCase().contains("already exists")) {
            status = HttpStatus.CONFLICT;
            code = "RESOURCE_CONFLICT";
        }

        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), msg, code, Collections.<String>emptyList()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatus().value());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }

        String message = (e.getReason() == null || e.getReason().trim().isEmpty())
                ? status.getReasonPhrase()
                : e.getReason();

        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), message, status.name(), Collections.<String>emptyList()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Server error",
                        "INTERNAL_SERVER_ERROR",
                        Collections.<String>emptyList()
                ));
    }

    public static class ApiError {
        private int status;
        private String message;
        private String code;
        private List<String> details;

        public ApiError() {
            this.details = Collections.emptyList();
        }

        public ApiError(int status, String message, String code, List<String> details) {
            this.status = status;
            this.message = message;
            this.code = code;
            this.details = details == null ? Collections.<String>emptyList() : details;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public List<String> getDetails() {
            return details;
        }

        public void setDetails(List<String> details) {
            this.details = details == null ? Collections.<String>emptyList() : details;
        }
    }
}
