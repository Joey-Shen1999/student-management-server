package com.studentmanagement.studentmanagementserver.domain.teacher;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import com.studentmanagement.studentmanagementserver.service.PasswordPolicyValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TeacherInviteService {

    private static final String UPPERCASE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE_CHARS = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGIT_CHARS = "23456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*()-_=+[]{}:;,.?";
    private static final String ALL_CHARS = UPPERCASE_CHARS + LOWERCASE_CHARS + DIGIT_CHARS + SPECIAL_CHARS;

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final SecureRandom random = new SecureRandom();

    public TeacherInviteService(UserRepository userRepository,
                                TeacherRepository teacherRepository,
                                PasswordEncoder passwordEncoder,
                                PasswordPolicyValidator passwordPolicyValidator) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

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
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        String tempPassword = generateTempPassword(username);

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

    private boolean isBlankCompat(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String generateTempPassword(String username) {
        for (int attempt = 0; attempt < 100; attempt++) {
            String candidate = buildRandomPassword();
            if (passwordPolicyValidator.validate(username, candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to generate a policy-compliant temporary password.");
    }

    private String buildRandomPassword() {
        List<Character> chars = new ArrayList<Character>();
        chars.add(randomChar(UPPERCASE_CHARS));
        chars.add(randomChar(LOWERCASE_CHARS));
        chars.add(randomChar(DIGIT_CHARS));
        chars.add(randomChar(SPECIAL_CHARS));

        while (chars.size() < 8) {
            chars.add(randomChar(ALL_CHARS));
        }

        Collections.shuffle(chars, random);

        StringBuilder sb = new StringBuilder(chars.size());
        for (Character c : chars) {
            sb.append(c.charValue());
        }
        return sb.toString();
    }

    private char randomChar(String source) {
        return source.charAt(random.nextInt(source.length()));
    }

    public static class CreateTeacherInviteResponse {
        private final String username;
        private final String tempPassword;

        public CreateTeacherInviteResponse(String username, String tempPassword) {
            this.username = username;
            this.tempPassword = tempPassword;
        }

        public String getUsername() {
            return username;
        }

        public String getTempPassword() {
            return tempPassword;
        }
    }
}
