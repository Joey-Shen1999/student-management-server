package com.studentmanagement.studentmanagementserver.domain.user;

import com.studentmanagement.studentmanagementserver.service.AuthService;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class ChangePasswordController {

    private final AuthService authService;
    private final AuthSessionService authSessionService;

    public ChangePasswordController(AuthService authService,
                                    AuthSessionService authSessionService) {
        this.authService = authService;
        this.authSessionService = authSessionService;
    }

    @PostMapping("/change-password")
    public ResponseEntity<ChangePasswordResponse> changePassword(@RequestBody(required = false) ChangePasswordRequest req,
                                                                 HttpServletRequest request) {
        User user = authSessionService.requireAuthenticatedUser(request);
        String oldPassword = req == null ? null : req.getOldPassword();
        String newPassword = req == null ? null : req.getNewPassword();

        authService.changePassword(user, oldPassword, newPassword);
        return ResponseEntity.ok(new ChangePasswordResponse(true, "Password changed successfully."));
    }

    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;

        public String getOldPassword() {
            return oldPassword;
        }

        public void setOldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    public static class ChangePasswordResponse {
        private boolean success;
        private String message;

        public ChangePasswordResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
