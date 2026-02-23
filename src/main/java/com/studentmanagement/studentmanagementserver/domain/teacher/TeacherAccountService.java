package com.studentmanagement.studentmanagementserver.domain.teacher;

import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.TeacherPasswordResetAuditLogRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import com.studentmanagement.studentmanagementserver.service.TemporaryPasswordGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class TeacherAccountService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final TeacherPasswordResetAuditLogRepository teacherPasswordResetAuditLogRepository;

    public TeacherAccountService(TeacherRepository teacherRepository,
                                 UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 TemporaryPasswordGenerator temporaryPasswordGenerator,
                                 TeacherPasswordResetAuditLogRepository teacherPasswordResetAuditLogRepository) {
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
        this.teacherPasswordResetAuditLogRepository = teacherPasswordResetAuditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<TeacherAccountItem> listTeacherAccounts() {
        List<Teacher> teachers = teacherRepository.findAllWithUser();
        List<TeacherAccountItem> result = new ArrayList<TeacherAccountItem>(teachers.size());
        for (Teacher teacher : teachers) {
            result.add(new TeacherAccountItem(
                    teacher.getId(),
                    teacher.getUser().getUsername(),
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

        teacherPasswordResetAuditLogRepository.save(new TeacherPasswordResetAuditLog(operator, teacher));

        return new ResetTeacherPasswordResponse(
                teacher.getId(),
                targetUser.getUsername(),
                tempPassword,
                "Password reset successfully"
        );
    }

    public static class TeacherAccountItem {
        private Long teacherId;
        private String username;
        private String displayName;
        private String firstName;
        private String lastName;
        private String email;

        public TeacherAccountItem(Long teacherId,
                                  String username,
                                  String displayName,
                                  String firstName,
                                  String lastName,
                                  String email) {
            this.teacherId = teacherId;
            this.username = username;
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
}
