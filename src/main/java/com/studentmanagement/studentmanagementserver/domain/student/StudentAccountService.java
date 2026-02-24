package com.studentmanagement.studentmanagementserver.domain.student;

import com.studentmanagement.studentmanagementserver.domain.enums.UserAccountStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import com.studentmanagement.studentmanagementserver.repo.UserSessionRepository;
import com.studentmanagement.studentmanagementserver.service.TemporaryPasswordGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class StudentAccountService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final UserSessionRepository userSessionRepository;

    public StudentAccountService(StudentRepository studentRepository,
                                 UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 TemporaryPasswordGenerator temporaryPasswordGenerator,
                                 UserSessionRepository userSessionRepository) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
        this.userSessionRepository = userSessionRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentAccountItem> listStudentAccounts() {
        List<Student> students = studentRepository.findAllWithUser();
        List<StudentAccountItem> result = new ArrayList<StudentAccountItem>(students.size());
        for (Student student : students) {
            result.add(new StudentAccountItem(
                    student.getId(),
                    student.getUser().getUsername(),
                    student.getUser().getRole(),
                    student.getUser().getStatus(),
                    student.getFirstName(),
                    student.getLastName(),
                    student.getNickName()
            ));
        }
        return result;
    }

    @Transactional
    public ResetStudentPasswordResponse resetStudentPassword(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student account not found: " + studentId
                ));

        User targetUser = student.getUser();
        String tempPassword = temporaryPasswordGenerator.generate(targetUser.getUsername());
        targetUser.setPasswordHash(passwordEncoder.encode(tempPassword));
        targetUser.setMustChangePassword(true);
        userRepository.save(targetUser);
        userSessionRepository.revokeAllActiveSessions(targetUser.getId(), LocalDateTime.now());

        return new ResetStudentPasswordResponse(
                student.getId(),
                targetUser.getUsername(),
                tempPassword,
                "Password reset successfully"
        );
    }

    @Transactional
    public UpdateStudentStatusResponse updateStudentStatus(Long studentId, String statusRaw, User operator) {
        UserAccountStatus targetStatus = parseStudentAccountStatus(statusRaw);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student account not found: " + studentId
                ));

        User targetUser = student.getUser();
        targetUser.updateStatus(targetStatus, operator == null ? null : operator.getId());
        userRepository.save(targetUser);
        if (targetStatus == UserAccountStatus.ARCHIVED) {
            userSessionRepository.revokeAllActiveSessions(targetUser.getId(), LocalDateTime.now());
        }

        return new UpdateStudentStatusResponse(
                student.getId(),
                targetUser.getUsername(),
                targetStatus
        );
    }

    private UserAccountStatus parseStudentAccountStatus(String statusRaw) {
        if (statusRaw == null || statusRaw.trim().isEmpty()) {
            throw new IllegalArgumentException("status is required");
        }
        try {
            return UserAccountStatus.valueOf(statusRaw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid account status. Expected ACTIVE or ARCHIVED.");
        }
    }

    public static class StudentAccountItem {
        private Long studentId;
        private String username;
        private UserRole role;
        private UserAccountStatus status;
        private String firstName;
        private String lastName;
        private String nickName;

        public StudentAccountItem(Long studentId,
                                  String username,
                                  UserRole role,
                                  UserAccountStatus status,
                                  String firstName,
                                  String lastName,
                                  String nickName) {
            this.studentId = studentId;
            this.username = username;
            this.role = role;
            this.status = status;
            this.firstName = firstName;
            this.lastName = lastName;
            this.nickName = nickName;
        }

        public Long getStudentId() {
            return studentId;
        }

        public String getUsername() {
            return username;
        }

        public UserRole getRole() {
            return role;
        }

        public UserAccountStatus getStatus() {
            return status;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getNickName() {
            return nickName;
        }
    }

    public static class ResetStudentPasswordResponse {
        private Long studentId;
        private String username;
        private String tempPassword;
        private String message;

        public ResetStudentPasswordResponse(Long studentId, String username, String tempPassword, String message) {
            this.studentId = studentId;
            this.username = username;
            this.tempPassword = tempPassword;
            this.message = message;
        }

        public Long getStudentId() {
            return studentId;
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

    public static class UpdateStudentStatusResponse {
        private Long studentId;
        private String username;
        private UserAccountStatus status;

        public UpdateStudentStatusResponse(Long studentId, String username, UserAccountStatus status) {
            this.studentId = studentId;
            this.username = username;
            this.status = status;
        }

        public Long getStudentId() {
            return studentId;
        }

        public String getUsername() {
            return username;
        }

        public UserAccountStatus getStatus() {
            return status;
        }
    }
}
