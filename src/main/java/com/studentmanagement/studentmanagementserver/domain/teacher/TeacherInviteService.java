package com.studentmanagement.studentmanagementserver.domain.teacher;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
public class TeacherInviteService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;

    public TeacherInviteService(UserRepository userRepository,
                                TeacherRepository teacherRepository,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Create a TEACHER user + teacher profile and return a one-time temp password.
     * Transactional: either both User + Teacher are created, or none.
     */
    @Transactional
    public CreateTeacherInviteResponse createTeacher(String usernameRaw, String nameRaw) {
        String username = safeTrim(usernameRaw);
        String name = safeTrim(nameRaw);

        if (isBlankCompat(username)) {
            throw new IllegalArgumentException("username is required");
        }
        if (username.length() > 80) {
            throw new IllegalArgumentException("username too long (max 80)");
        }
        if (isBlankCompat(name)) {
            throw new IllegalArgumentException("teacher name is required");
        }
        if (name.length() > 120) {
            throw new IllegalArgumentException("teacher name too long (max 120)");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            // 让前端能直接显示清晰提示
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        String tempPassword = generateTempPassword8();

        User user = new User(username, passwordEncoder.encode(tempPassword), UserRole.TEACHER);
        user.setMustChangePassword(true);
        userRepository.save(user);

        Teacher teacher = new Teacher(user, name);
        teacherRepository.save(teacher);

        return new CreateTeacherInviteResponse(username, tempPassword);
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    // Java 8 compatible blank check
    private boolean isBlankCompat(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String generateTempPassword8() {
        // avoid ambiguous chars: 0/O, 1/I/l
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Simple DTO for response: username + tempPassword
     * JSON keys will be: username, tempPassword
     */
    public static class CreateTeacherInviteResponse {
        private final String username;
        private final String tempPassword;

        public CreateTeacherInviteResponse(String username, String tempPassword) {
            this.username = username;
            this.tempPassword = tempPassword;
        }

        public String getUsername() { return username; }
        public String getTempPassword() { return tempPassword; }
    }
}