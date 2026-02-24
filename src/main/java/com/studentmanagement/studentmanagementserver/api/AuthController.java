package com.studentmanagement.studentmanagementserver.api;

import com.studentmanagement.studentmanagementserver.api.dto.LoginRequest;
import com.studentmanagement.studentmanagementserver.api.dto.LoginResponse;
import com.studentmanagement.studentmanagementserver.api.dto.RegisterRequest;
import com.studentmanagement.studentmanagementserver.api.dto.RegisterResponse;
import com.studentmanagement.studentmanagementserver.api.dto.SetPasswordRequest;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.service.AuthService;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthSessionService authSessionService;

    public AuthController(AuthService authService, AuthSessionService authSessionService) {
        this.authService = authService;
        this.authSessionService = authSessionService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        LoginResponse resp = authService.login(req.getUsername(), req.getPassword());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest req) {
        RegisterResponse resp = authService.register(req);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/set-password")
    public ResponseEntity<Map<String, Object>> setPassword(@RequestBody SetPasswordRequest req, HttpServletRequest request) {
        User currentUser = authSessionService.requireAuthenticatedUser(request);
        if (req.getUserId() != null && !req.getUserId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot set password for another user.");
        }
        if (!currentUser.isMustChangePassword()) {
            throw new IllegalArgumentException("set-password is only available when password change is required.");
        }

        authService.setPassword(currentUser.getId(), req.getNewPassword());

        Map<String, Object> response = new HashMap<String, Object>();
        response.put("success", true);
        response.put("message", "Password set successfully.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        authSessionService.revokeCurrentSession(request);

        Map<String, Object> response = new HashMap<String, Object>();
        response.put("success", true);
        response.put("message", "Logged out.");
        return ResponseEntity.ok(response);
    }
}
