package com.studentmanagement.studentmanagementserver.api;

import com.studentmanagement.studentmanagementserver.api.dto.LoginRequest;
import com.studentmanagement.studentmanagementserver.api.dto.LoginResponse;
import com.studentmanagement.studentmanagementserver.api.dto.RegisterRequest;
import com.studentmanagement.studentmanagementserver.api.dto.RegisterResponse;
import com.studentmanagement.studentmanagementserver.api.dto.SetPasswordRequest;
import com.studentmanagement.studentmanagementserver.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 登录
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        LoginResponse resp = authService.login(req.getUsername(), req.getPassword());
        return ResponseEntity.ok(resp);
    }

    // 注册
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest req) {
        RegisterResponse resp = authService.register(req);
        return ResponseEntity.ok(resp);
    }

    // ✅ 首次登录设置新密码（不需要旧密码）
    @PostMapping("/set-password")
    public ResponseEntity<Map<String, Object>> setPassword(@RequestBody SetPasswordRequest req) {

        authService.setPassword(req.getUserId(), req.getNewPassword());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Password set successfully.");

        return ResponseEntity.ok(response);
    }
}