package com.studentmanagement.studentmanagementserver.domain.teacher;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import com.studentmanagement.studentmanagementserver.service.TemporaryPasswordGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherInviteService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;

    public TeacherInviteService(UserRepository userRepository,
                                TeacherRepository teacherRepository,
                                PasswordEncoder passwordEncoder,
                                TemporaryPasswordGenerator temporaryPasswordGenerator) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
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

        String tempPassword = temporaryPasswordGenerator.generate(username);

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
