package com.studentmanagement.studentmanagementserver.api;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthChangePasswordApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthSessionService authSessionService;

    @Test
    void changePassword_student_success_updatesHashAndAllowsNewLogin() throws Exception {
        String oldPassword = "Old#Password1";
        String newPassword = "New#Password1";
        User studentUser = createStudentUser("change_pwd_student", oldPassword);

        String body = "{"
                + "\"oldPassword\":\"" + oldPassword + "\","
                + "\"newPassword\":\"" + newPassword + "\""
                + "}";

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", bearerFor(studentUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully."));

        User refreshed = userRepository.findById(studentUser.getId())
                .orElseThrow(() -> new RuntimeException("user not found"));
        assertTrue(passwordEncoder.matches(newPassword, refreshed.getPasswordHash()));
        assertFalse(passwordEncoder.matches(oldPassword, refreshed.getPasswordHash()));
        assertFalse(refreshed.isMustChangePassword());
        assertNotNull(refreshed.getPasswordUpdatedAt());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"change_pwd_student\",\"password\":\"" + oldPassword + "\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"change_pwd_student\",\"password\":\"" + newPassword + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    void changePassword_teacher_success_returns200() throws Exception {
        User teacherUser = createTeacherUser("change_pwd_teacher", "Old#Password1");

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", bearerFor(teacherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"Old#Password1\",\"newPassword\":\"New#Password1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void changePassword_invalidOldPassword_returnsINVALID_OLD_PASSWORD() throws Exception {
        User user = createStudentUser("change_pwd_bad_old", "Old#Password1");

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", bearerFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"Wrong#Password1\",\"newPassword\":\"New#Password1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OLD_PASSWORD"))
                .andExpect(jsonPath("$.message").value("oldPassword incorrect"));
    }

    @Test
    void changePassword_sameAsOldPassword_returnsSAME_AS_OLD_PASSWORD() throws Exception {
        User user = createStudentUser("change_pwd_same", "Old#Password1");

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", bearerFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"Old#Password1\",\"newPassword\":\"Old#Password1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SAME_AS_OLD_PASSWORD"))
                .andExpect(jsonPath("$.message").value("newPassword must be different from oldPassword"));
    }

    @Test
    void changePassword_weakPassword_returnsWEAK_PASSWORD() throws Exception {
        User user = createStudentUser("change_pwd_weak", "Old#Password1");

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", bearerFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"Old#Password1\",\"newPassword\":\"abc12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void changePassword_missingField_returnsVALIDATION_ERROR() throws Exception {
        User user = createStudentUser("change_pwd_missing", "Old#Password1");

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", bearerFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"New#Password1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("oldPassword is required"));
    }

    @Test
    void changePassword_unauthenticated_returnsUNAUTHENTICATED() throws Exception {
        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"Old#Password1\",\"newPassword\":\"New#Password1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("Unauthenticated."));
    }

    private User createStudentUser(String username, String rawPassword) {
        User user = userRepository.save(new User(username, passwordEncoder.encode(rawPassword), UserRole.STUDENT));
        studentRepository.save(new Student(user, "Amy", "Chen", "Amy"));
        return user;
    }

    private User createTeacherUser(String username, String rawPassword) {
        User user = userRepository.save(new User(username, passwordEncoder.encode(rawPassword), UserRole.TEACHER));
        teacherRepository.save(new Teacher(user, "Teacher " + username));
        return user;
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
