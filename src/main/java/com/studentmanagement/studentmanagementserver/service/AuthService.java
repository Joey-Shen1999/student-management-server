package com.studentmanagement.studentmanagementserver.service;

import com.studentmanagement.studentmanagementserver.api.dto.LoginResponse;
import com.studentmanagement.studentmanagementserver.api.dto.RegisterRequest;
import com.studentmanagement.studentmanagementserver.api.dto.RegisterResponse;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository,
                       StudentRepository studentRepository,
                       TeacherRepository teacherRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        String password = req.getPassword();

        if (username.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Username and password are required");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        UserRole role = req.getRole();

        // 创建 User
        User user = new User(username, encoder.encode(password), role);
        user = userRepository.save(user);

        Long studentId = null;
        Long teacherId = null;

        // 如果是学生，创建 Student Profile
        if (role == UserRole.STUDENT) {
            String firstName = req.getFirstName() == null ? "" : req.getFirstName().trim();
            String lastName = req.getLastName() == null ? "" : req.getLastName().trim();
            String nickName = req.getPreferredName() == null ? null : req.getPreferredName().trim();

            if (firstName.isEmpty() || lastName.isEmpty()) {
                throw new IllegalArgumentException("First name and last name are required for students");
            }

            Student student = new Student(user, firstName, lastName, nickName);
            student = studentRepository.save(student);
            studentId = student.getId();
        }

        return new RegisterResponse(user.getId(), role, studentId, teacherId);
    }

    @Transactional
    public LoginResponse login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!encoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        Long studentId = null;
        Long teacherId = null;

        if (user.getRole() == UserRole.STUDENT) {
            studentId = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new IllegalStateException("Student profile missing"))
                    .getId();
        } else if (user.getRole() == UserRole.TEACHER) {
            teacherId = teacherRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new IllegalStateException("Teacher profile missing"))
                    .getId();
        }

        user.setLastLoginAt(LocalDateTime.now());

        return new LoginResponse(user.getId(), user.getRole(), studentId, teacherId);
    }
}
