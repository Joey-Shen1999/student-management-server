package com.studentmanagement.studentmanagementserver.service;

import com.studentmanagement.studentmanagementserver.api.dto.LoginResponse;
import com.studentmanagement.studentmanagementserver.api.dto.RegisterRequest;
import com.studentmanagement.studentmanagementserver.api.dto.RegisterResponse;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher; // ✅ 如果你的Teacher包名不同，改这里
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

        if (username.isEmpty() || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Username and password are required");
        }

        UserRole role = req.getRole();
        if (role == null) {
            throw new IllegalArgumentException("Role is required (STUDENT or TEACHER)");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        // 创建 User
        User user = new User(username, encoder.encode(password), role);
        user = userRepository.save(user);

        Long studentId = null;
        Long teacherId = null;

        if (role == UserRole.STUDENT) {
            // ✅ 学生：要求 first / last name
            String firstName = req.getFirstName() == null ? "" : req.getFirstName().trim();
            String lastName = req.getLastName() == null ? "" : req.getLastName().trim();
            String preferredName = req.getPreferredName() == null ? null : req.getPreferredName().trim();

            if (firstName.isEmpty() || lastName.isEmpty()) {
                throw new IllegalArgumentException("First name and last name are required for students");
            }

            Student student = new Student(user, firstName, lastName, preferredName);
            student = studentRepository.save(student);
            studentId = student.getId();
        } else if (role == UserRole.TEACHER) {
            // ✅ 老师：创建 Teacher profile（先最简）
            String displayName = req.getDisplayName() == null ? "" : req.getDisplayName().trim();
            if (displayName.isEmpty()) {
                // 你可以改成强制要求 displayName
                displayName = username;
            }

            // ⚠️ 下面构造函数可能需要你按 Teacher 实体调整（见备注）
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

        if (!encoder.matches(p, user.getPasswordHash())) {
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
        userRepository.save(user); // ✅ 显式保存更稳

        return new LoginResponse(user.getId(), user.getRole(), studentId, teacherId);
    }
}
