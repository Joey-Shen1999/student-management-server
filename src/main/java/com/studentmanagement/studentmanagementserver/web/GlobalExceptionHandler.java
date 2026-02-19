package com.studentmanagement.studentmanagementserver.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException e) {

        String msg = (e.getMessage() == null || e.getMessage().trim().isEmpty())
                ? "Bad request"
                : e.getMessage();

        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (msg.toLowerCase().contains("already exists")) {
            status = HttpStatus.CONFLICT; // 409
        }

        return ResponseEntity.status(status).body(new ApiError(status.value(), msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(500, "Server error"));
    }

    public static class ApiError {
        private int status;
        private String message;

        public ApiError() {}

        public ApiError(int status, String message) {
            this.status = status;
            this.message = message;
        }

        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
