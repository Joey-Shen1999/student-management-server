package com.studentmanagement.studentmanagementserver.domain.user;

import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class ChangePasswordController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ChangePasswordController(UserRepository userRepository,
                                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest req) {

        String username = safeTrim(req.getUsername());
        String oldPassword = req.getOldPassword();
        String newPassword = req.getNewPassword();

        if (isBlank(username)) {
            throw new IllegalArgumentException("username is required");
        }
        if (isBlank(oldPassword)) {
            throw new IllegalArgumentException("oldPassword is required");
        }
        if (isBlank(newPassword)) {
            throw new IllegalArgumentException("newPassword is required");
        }
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("newPassword must be at least 8 characters");
        }
        if (newPassword.equals(oldPassword)) {
            throw new IllegalArgumentException("newPassword must be different from oldPassword");
        }

        Optional<User> opt = userRepository.findByUsername(username);
        if (!opt.isPresent()) {
            throw new IllegalArgumentException("user not found: " + username);
        }

        User user = opt.get();

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("oldPassword incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);

        return ResponseEntity.ok(new ChangePasswordResponse(true));
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static class ChangePasswordRequest {
        private String username;
        private String oldPassword;
        private String newPassword;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class ChangePasswordResponse {
        private boolean ok;

        public ChangePasswordResponse(boolean ok) { this.ok = ok; }

        public boolean isOk() { return ok; }
        public void setOk(boolean ok) { this.ok = ok; }
    }
}
