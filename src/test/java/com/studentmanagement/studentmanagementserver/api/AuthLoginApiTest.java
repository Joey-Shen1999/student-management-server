package com.studentmanagement.studentmanagementserver.api;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.student.StudentProfile;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentProfileRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthLoginApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void login_studentWithoutProfile_returnsRequiresProfileCompletionTrue() throws Exception {
        createStudentUser("login_stu_no_profile", "Student!234");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"login_stu_no_profile\",\"password\":\"Student!234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andExpect(jsonPath("$.requiresProfileCompletion").value(true));
    }

    @Test
    void login_studentWithProfile_returnsRequiresProfileCompletionFalse() throws Exception {
        User user = createStudentUser("login_stu_with_profile", "Student!234");
        Student student = studentRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new RuntimeException("student not found"));
        studentProfileRepository.save(new StudentProfile(student));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"login_stu_with_profile\",\"password\":\"Student!234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.requiresProfileCompletion").value(false));
    }

    @Test
    void login_studentMustChangePassword_stillReturnsMustChangePasswordAndRequiresProfileCompletion() throws Exception {
        User user = createStudentUser("login_stu_must_change", "Student!234");
        user.setMustChangePassword(true);
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"login_stu_must_change\",\"password\":\"Student!234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andExpect(jsonPath("$.requiresProfileCompletion").value(true));
    }

    @Test
    void login_teacher_returnsRequiresProfileCompletionFalse() throws Exception {
        createTeacherUser("login_teacher", "Teacher!234", UserRole.TEACHER);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"login_teacher\",\"password\":\"Teacher!234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.requiresProfileCompletion").value(false));
    }

    @Test
    void login_admin_returnsRequiresProfileCompletionFalse() throws Exception {
        createTeacherUser("login_admin", "Admin!234", UserRole.ADMIN);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"login_admin\",\"password\":\"Admin!234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.requiresProfileCompletion").value(false));
    }

    private User createStudentUser(String username, String rawPassword) {
        User user = userRepository.save(new User(username, passwordEncoder.encode(rawPassword), UserRole.STUDENT));
        studentRepository.save(new Student(user, "Amy", "Chen", "Amy"));
        return user;
    }

    private User createTeacherUser(String username, String rawPassword, UserRole role) {
        User user = userRepository.save(new User(username, passwordEncoder.encode(rawPassword), role));
        teacherRepository.save(new Teacher(user, "Teacher " + username));
        return user;
    }
}
