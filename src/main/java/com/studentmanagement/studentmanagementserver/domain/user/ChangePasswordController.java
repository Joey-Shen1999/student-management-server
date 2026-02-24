package com.studentmanagement.studentmanagementserver.domain.user;

import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import com.studentmanagement.studentmanagementserver.service.PasswordPolicyValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class ChangePasswordController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final AuthSessionService authSessionService;

    public ChangePasswordController(UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    PasswordPolicyValidator passwordPolicyValidator,
                                    AuthSessionService authSessionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.authSessionService = authSessionService;
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest req, HttpServletRequest request) {
        String oldPassword = req.getOldPassword();
        String newPassword = req.getNewPassword();

        if (isBlank(oldPassword)) {
            throw new IllegalArgumentException("oldPassword is required");
        }
        if (isBlank(newPassword)) {
            throw new IllegalArgumentException("newPassword is required");
        }
        if (newPassword.equals(oldPassword)) {
            throw new IllegalArgumentException("newPassword must be different from oldPassword");
        }

        User user = authSessionService.requireAuthenticatedUser(request);

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("oldPassword incorrect");
        }

        passwordPolicyValidator.validateOrThrow(user.getUsername(), newPassword);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);

        return ResponseEntity.ok(new ChangePasswordResponse(true));
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
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
        private boolean ok;

        public ChangePasswordResponse(boolean ok) {
            this.ok = ok;
        }

        public boolean isOk() {
            return ok;
        }

        public void setOk(boolean ok) {
            this.ok = ok;
        }
    }
}
