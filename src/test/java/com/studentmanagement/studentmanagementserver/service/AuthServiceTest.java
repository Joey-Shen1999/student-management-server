package com.studentmanagement.studentmanagementserver.service;

import com.studentmanagement.studentmanagementserver.api.dto.LoginResponse;
import com.studentmanagement.studentmanagementserver.domain.enums.UserAccountStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired TeacherRepository teacherRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void login_student_success_updatesLastLoginAt() {
        User u = userRepository.save(new User("stu1", encoder.encode("123456"), UserRole.STUDENT));
        Student s = studentRepository.save(new Student(u, "Joey", "Shen", "Goblin"));

        LoginResponse resp = authService.login("stu1", "123456");

        User updated = userRepository.findById(u.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        assertNotNull(updated.getLastLoginAt());

        assertEquals(u.getId(), resp.getUserId());
        assertEquals(UserRole.STUDENT, resp.getRole());
        assertEquals(s.getId(), resp.getStudentId());
        assertNull(resp.getTeacherId());
        assertNotNull(resp.getAccessToken());
        assertEquals("Bearer", resp.getTokenType());
        assertNotNull(resp.getTokenExpiresAt());
    }

    @Test
    void login_teacher_success() {
        User u = userRepository.save(new User("t1", encoder.encode("pw"), UserRole.TEACHER));
        Teacher t = teacherRepository.save(new Teacher(u, "Mr. Smith"));

        LoginResponse resp = authService.login("t1", "pw");

        assertEquals(UserRole.TEACHER, resp.getRole());
        assertEquals(t.getId(), resp.getTeacherId());
        assertNull(resp.getStudentId());
        assertNotNull(resp.getAccessToken());
        assertEquals("Bearer", resp.getTokenType());
        assertNotNull(resp.getTokenExpiresAt());
    }

    @Test
    void login_wrongPassword_throws() {
        userRepository.save(new User("u1", encoder.encode("right"), UserRole.STUDENT));
        assertThrows(IllegalArgumentException.class, () -> authService.login("u1", "wrong"));
    }

    @Test
    void login_archivedAccount_throws() {
        User u = userRepository.save(new User("archived_user", encoder.encode("pw"), UserRole.TEACHER));
        teacherRepository.save(new Teacher(u, "Archived Teacher"));
        u.updateStatus(UserAccountStatus.ARCHIVED, null);
        userRepository.save(u);

        AccountArchivedException ex =
                assertThrows(AccountArchivedException.class, () -> authService.login("archived_user", "pw"));
        assertEquals("ACCOUNT_ARCHIVED", ex.getCode());
    }

    @Test
    void login_adminWithTeacherBinding_returnsTeacherId() {
        User admin = userRepository.save(new User("admin_login_teacher", encoder.encode("Admin!234"), UserRole.ADMIN));
        Teacher teacher = teacherRepository.save(new Teacher(admin, "Admin Teacher"));

        LoginResponse resp = authService.login("admin_login_teacher", "Admin!234");

        assertEquals(UserRole.ADMIN, resp.getRole());
        assertEquals(teacher.getId(), resp.getTeacherId());
    }
}
