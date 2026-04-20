package com.studentmanagement.studentmanagementserver.web;

import com.studentmanagement.studentmanagementserver.service.AccountArchivedException;
import com.studentmanagement.studentmanagementserver.service.ApiRequestException;
import com.studentmanagement.studentmanagementserver.service.MustChangePasswordRequiredException;
import com.studentmanagement.studentmanagementserver.service.PasswordPolicyViolationException;
import com.studentmanagement.studentmanagementserver.service.ProfileVersionConflictException;
import com.studentmanagement.studentmanagementserver.service.StudentInviteException;
import com.studentmanagement.studentmanagementserver.service.TeacherBindingRequiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(AccountArchivedException.class)
    public ResponseEntity<ApiError> handleAccountArchived(AccountArchivedException e) {
        ApiError body = new ApiError(
                HttpStatus.FORBIDDEN.value(),
                e.getMessage(),
                e.getCode(),
                Collections.<String>emptyList()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(ApiRequestException.class)
    public ResponseEntity<ApiError> handleApiRequest(ApiRequestException e) {
        HttpStatus status = e.getStatus() == null ? HttpStatus.BAD_REQUEST : e.getStatus();
        ApiError body = new ApiError(
                status.value(),
                e.getMessage(),
                e.getCode(),
                e.getDetails()
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ProfileVersionConflictException.class)
    public ResponseEntity<ApiError> handleProfileVersionConflict(ProfileVersionConflictException e) {
        ApiError body = new ApiError(
                HttpStatus.CONFLICT.value(),
                "Profile version conflict.",
                "PROFILE_VERSION_CONFLICT",
                Collections.<String>emptyList()
        );
        body.setCurrentVersion(e.getCurrentVersion());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(StudentInviteException.class)
    public ResponseEntity<ApiError> handleStudentInvite(StudentInviteException e) {
        HttpStatus status = e.getStatus() == null ? HttpStatus.BAD_REQUEST : e.getStatus();
        ApiError body = new ApiError(
                status.value(),
                e.getMessage(),
                e.getCode(),
                Collections.<String>emptyList()
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(TeacherBindingRequiredException.class)
    public ResponseEntity<ApiError> handleTeacherBindingRequired(TeacherBindingRequiredException e) {
        ApiError body = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                e.getCode(),
                Collections.<String>emptyList()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        Throwable root = e.getMostSpecificCause();
        String rawMessage = root != null && root.getMessage() != null ? root.getMessage() : e.getMessage();
        String normalizedRawMessage = rawMessage == null ? "" : rawMessage;
        String normalizedLower = normalizedRawMessage.toLowerCase(Locale.ROOT);

        boolean duplicate = normalizedLower.contains("duplicate key")
                || normalizedLower.contains("unique constraint");
        HttpStatus status = duplicate ? HttpStatus.CONFLICT : HttpStatus.UNPROCESSABLE_ENTITY;
        String code = duplicate ? "RESOURCE_CONFLICT" : "DATA_INTEGRITY_VIOLATION";
        String message = duplicate
                ? "Resource conflict."
                : "Request data violates database constraints.";
        if (normalizedRawMessage.contains("uk_student_school_record_unique_school_per_student")) {
            message = "Duplicate school record for this student.";
        }

        return ResponseEntity.status(status).body(new ApiError(
                status.value(),
                message,
                code,
                buildDataIntegrityDetails(normalizedRawMessage)
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMalformedBody(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        HttpStatus.BAD_REQUEST.value(),
                        "Malformed request body.",
                        "BAD_REQUEST",
                        Collections.<String>emptyList()
                ));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiError> handleMissingRequestPart(MissingServletRequestPartException e) {
        String partName = e.getRequestPartName();
        String message = (partName == null || partName.trim().isEmpty())
                ? "Missing required multipart field."
                : "Missing required multipart field: " + partName;

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        HttpStatus.BAD_REQUEST.value(),
                        message,
                        "BAD_REQUEST",
                        Collections.<String>emptyList()
                ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(fileTooLargeError());
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiError> handleMultipartError(MultipartException e) {
        if (isFileTooLargeError(e)) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(fileTooLargeError());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        HttpStatus.BAD_REQUEST.value(),
                        "Invalid multipart request.",
                        "BAD_REQUEST",
                        Collections.<String>emptyList()
                ));
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
        String code = status == HttpStatus.UNAUTHORIZED ? "UNAUTHENTICATED" : status.name();

        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), message, code, Collections.<String>emptyList()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception e) {
        ResponseStatusException unwrapped = unwrapResponseStatus(e);
        if (unwrapped != null) {
            return handleResponseStatus(unwrapped);
        }

        log.error("Unhandled server exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Server error",
                        "INTERNAL_SERVER_ERROR",
                        Collections.<String>emptyList()
                ));
    }

    private ResponseStatusException unwrapResponseStatus(Throwable error) {
        Throwable current = error;
        int depth = 0;
        while (current != null && depth < 8) {
            if (current instanceof ResponseStatusException) {
                return (ResponseStatusException) current;
            }
            current = current.getCause();
            depth++;
        }
        return null;
    }

    private ApiError fileTooLargeError() {
        return new ApiError(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "Max upload size is 50MB",
                "FILE_TOO_LARGE",
                Collections.<String>emptyList()
        );
    }

    private List<String> buildDataIntegrityDetails(String rawMessage) {
        String compact = rawMessage == null ? null : rawMessage.replace('\n', ' ').replace('\r', ' ').trim();
        if (compact == null || compact.isEmpty()) {
            return Collections.<String>emptyList();
        }
        if (compact.length() > 500) {
            compact = compact.substring(0, 500);
        }
        return Collections.singletonList(compact);
    }

    private boolean isFileTooLargeError(Throwable throwable) {
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < 8) {
            if (current instanceof MaxUploadSizeExceededException) {
                return true;
            }
            current = current.getCause();
            depth++;
        }
        return false;
    }

    public static class ApiError {
        private int status;
        private String message;
        private String code;
        private List<String> details;
        private Long currentVersion;

        public ApiError() {
            this.details = Collections.emptyList();
        }

        public ApiError(int status, String message, String code, List<String> details) {
            this.status = status;
            this.message = message;
            this.code = code;
            this.details = details == null ? Collections.<String>emptyList() : details;
            this.currentVersion = null;
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

        public Long getCurrentVersion() {
            return currentVersion;
        }

        public void setCurrentVersion(Long currentVersion) {
            this.currentVersion = currentVersion;
        }
    }
}
