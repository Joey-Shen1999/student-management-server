package com.studentmanagement.studentmanagementserver.service;

import com.studentmanagement.studentmanagementserver.api.dto.LoginResponse;
import com.studentmanagement.studentmanagementserver.api.dto.RegisterRequest;
import com.studentmanagement.studentmanagementserver.api.dto.RegisterResponse;
import com.studentmanagement.studentmanagementserver.domain.enums.UserAccountStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.student.StudentInvite;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherStudent;
import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherStudentRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final AuthSessionService authSessionService;
    private final StudentInviteService studentInviteService;
    private final TeacherStudentRepository teacherStudentRepository;

    public AuthService(UserRepository userRepository,
                       StudentRepository studentRepository,
                       TeacherRepository teacherRepository,
                       TeacherStudentRepository teacherStudentRepository,
                       PasswordEncoder passwordEncoder,
                       PasswordPolicyValidator passwordPolicyValidator,
                       AuthSessionService authSessionService,
                       StudentInviteService studentInviteService) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.teacherStudentRepository = teacherStudentRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.authSessionService = authSessionService;
        this.studentInviteService = studentInviteService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        String password = req.getPassword();

        if (username.isEmpty() || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Username and password are required");
        }
        passwordPolicyValidator.validateOrThrow(username, password);

        UserRole role = req.getRole();
        if (role == null) {
            throw new IllegalArgumentException("Role is required (STUDENT or TEACHER)");
        }
        String inviteToken = req.getInviteToken() == null ? null : req.getInviteToken().trim();
        if (inviteToken != null && inviteToken.isEmpty()) {
            inviteToken = null;
        }
        if (inviteToken != null && role != UserRole.STUDENT) {
            throw StudentInviteException.roleMismatch();
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        StudentInvite studentInvite = inviteToken == null ? null : studentInviteService.lockPendingInviteForRegistration(inviteToken);
        Teacher invitedTeacher = studentInvite == null ? null : studentInvite.getTeacher();

        User user = new User(username, passwordEncoder.encode(password), role);
        user = userRepository.save(user);

        Long studentId = null;
        Long teacherId = null;

        if (role == UserRole.STUDENT) {
            String firstName = req.getFirstName() == null ? "" : req.getFirstName().trim();
            String lastName = req.getLastName() == null ? "" : req.getLastName().trim();
            String preferredName = req.getPreferredName() == null ? null : req.getPreferredName().trim();

            if (firstName.isEmpty() || lastName.isEmpty()) {
                throw new IllegalArgumentException("First name and last name are required for students");
            }

            Student student = new Student(user, firstName, lastName, preferredName, invitedTeacher);
            student = studentRepository.save(student);
            if (invitedTeacher != null) {
                boolean hasActiveRelation = teacherStudentRepository.existsByTeacher_IdAndStudent_IdAndStatus(
                        invitedTeacher.getId(),
                        student.getId(),
                        TeacherStudentStatus.ACTIVE
                );
                if (!hasActiveRelation) {
                    teacherStudentRepository.save(new TeacherStudent(
                            invitedTeacher,
                            student,
                            TeacherStudentStatus.ACTIVE,
                            "Created by student invite registration"
                    ));
                }
            }
            studentId = student.getId();
            if (studentInvite != null) {
                studentInviteService.markInviteUsed(studentInvite, user.getId());
            }
        } else if (role == UserRole.TEACHER) {
            String displayName = req.getDisplayName() == null ? "" : req.getDisplayName().trim();
            if (displayName.isEmpty()) {
                displayName = username;
            }

            Teacher teacher = new Teacher(user, displayName);
            teacher = teacherRepository.save(teacher);
            teacherId = teacher.getId();
        } else {
            throw new IllegalArgumentException("Unsupported role: " + role);
        }

        return new RegisterResponse(user.getId(), role, studentId, teacherId);
    }

    @Transactional
    public LoginResponse login(String username, String rawPassword) {
        String u = username == null ? "" : username.trim();
        String p = rawPassword == null ? "" : rawPassword;

        if (u.isEmpty() || p.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        User user = userRepository.findByUsername(u)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(p, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        if (user.getStatus() == UserAccountStatus.ARCHIVED) {
            throw new AccountArchivedException();
        }

        Long studentId = null;
        Long teacherId = null;

        if (user.getRole() == UserRole.STUDENT) {
            studentId = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new IllegalStateException("Student profile missing"))
                    .getId();
        } else if (user.getRole() == UserRole.TEACHER || user.getRole() == UserRole.ADMIN) {
            teacherId = teacherRepository.findByUser_Id(user.getId())
                    .orElseThrow(TeacherBindingRequiredException::new)
                    .getId();
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);

        return new LoginResponse(
                user.getId(),
                user.getRole(),
                studentId,
                teacherId,
                user.isMustChangePassword(),
                issuedSession.getAccessToken(),
                issuedSession.getTokenType(),
                issuedSession.getExpiresAt()
        );
    }

    @Transactional
    public void setPassword(Long userId, String newPassword) {
        if (userId == null || newPassword == null || newPassword.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing fields.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        passwordPolicyValidator.validateOrThrow(user.getUsername(), newPassword);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }
}
