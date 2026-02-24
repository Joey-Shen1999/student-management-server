package com.studentmanagement.studentmanagementserver.domain.teacher;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.TeacherPasswordResetAuditLogRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import com.studentmanagement.studentmanagementserver.repo.UserSessionRepository;
import com.studentmanagement.studentmanagementserver.service.TemporaryPasswordGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class TeacherAccountService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final TeacherPasswordResetAuditLogRepository teacherPasswordResetAuditLogRepository;
    private final UserSessionRepository userSessionRepository;

    public TeacherAccountService(TeacherRepository teacherRepository,
                                 UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 TemporaryPasswordGenerator temporaryPasswordGenerator,
                                 TeacherPasswordResetAuditLogRepository teacherPasswordResetAuditLogRepository,
                                 UserSessionRepository userSessionRepository) {
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
        this.teacherPasswordResetAuditLogRepository = teacherPasswordResetAuditLogRepository;
        this.userSessionRepository = userSessionRepository;
    }

    @Transactional(readOnly = true)
    public List<TeacherAccountItem> listTeacherAccounts() {
        List<Teacher> teachers = teacherRepository.findAllWithUser();
        List<TeacherAccountItem> result = new ArrayList<TeacherAccountItem>(teachers.size());
        for (Teacher teacher : teachers) {
            result.add(new TeacherAccountItem(
                    teacher.getId(),
                    teacher.getUser().getUsername(),
                    teacher.getUser().getRole(),
                    teacher.getName(),
                    null,
                    null,
                    null
            ));
        }
        return result;
    }

    @Transactional
    public ResetTeacherPasswordResponse resetTeacherPassword(Long teacherId, User operator) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Teacher account not found: " + teacherId
                ));

        User targetUser = teacher.getUser();
        String tempPassword = temporaryPasswordGenerator.generate(targetUser.getUsername());

        targetUser.setPasswordHash(passwordEncoder.encode(tempPassword));
        targetUser.setMustChangePassword(true);
        userRepository.save(targetUser);
        userSessionRepository.revokeAllActiveSessions(targetUser.getId(), LocalDateTime.now());

        teacherPasswordResetAuditLogRepository.save(new TeacherPasswordResetAuditLog(operator, teacher));

        return new ResetTeacherPasswordResponse(
                teacher.getId(),
                targetUser.getUsername(),
                tempPassword,
                "Password reset successfully"
        );
    }

    @Transactional
    public UpdateTeacherRoleResponse updateTeacherRole(Long teacherId, String roleRaw) {
        UserRole targetRole = parseTeacherManagementRole(roleRaw);

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Teacher account not found: " + teacherId
                ));

        User targetUser = teacher.getUser();
        targetUser.setRole(targetRole);
        userRepository.save(targetUser);

        return new UpdateTeacherRoleResponse(
                teacher.getId(),
                targetUser.getUsername(),
                targetRole,
                "Role updated successfully."
        );
    }

    private UserRole parseTeacherManagementRole(String roleRaw) {
        if (roleRaw == null || roleRaw.trim().isEmpty()) {
            throw new IllegalArgumentException("role is required");
        }

        UserRole parsed;
        try {
            parsed = UserRole.valueOf(roleRaw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("role must be ADMIN or TEACHER");
        }

        if (parsed != UserRole.ADMIN && parsed != UserRole.TEACHER) {
            throw new IllegalArgumentException("role must be ADMIN or TEACHER");
        }
        return parsed;
    }

    public static class TeacherAccountItem {
        private Long teacherId;
        private String username;
        private UserRole role;
        private String displayName;
        private String firstName;
        private String lastName;
        private String email;

        public TeacherAccountItem(Long teacherId,
                                  String username,
                                  UserRole role,
                                  String displayName,
                                  String firstName,
                                  String lastName,
                                  String email) {
            this.teacherId = teacherId;
            this.username = username;
            this.role = role;
            this.displayName = displayName;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }

        public Long getTeacherId() {
            return teacherId;
        }

        public String getUsername() {
            return username;
        }

        public UserRole getRole() {
            return role;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getEmail() {
            return email;
        }
    }

    public static class ResetTeacherPasswordResponse {
        private Long teacherId;
        private String username;
        private String tempPassword;
        private String message;

        public ResetTeacherPasswordResponse(Long teacherId, String username, String tempPassword, String message) {
            this.teacherId = teacherId;
            this.username = username;
            this.tempPassword = tempPassword;
            this.message = message;
        }

        public Long getTeacherId() {
            return teacherId;
        }

        public String getUsername() {
            return username;
        }

        public String getTempPassword() {
            return tempPassword;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class UpdateTeacherRoleResponse {
        private Long teacherId;
        private String username;
        private UserRole role;
        private String message;

        public UpdateTeacherRoleResponse(Long teacherId, String username, UserRole role, String message) {
            this.teacherId = teacherId;
            this.username = username;
            this.role = role;
            this.message = message;
        }

        public Long getTeacherId() {
            return teacherId;
        }

        public String getUsername() {
            return username;
        }

        public UserRole getRole() {
            return role;
        }

        public String getMessage() {
            return message;
        }
    }
}
