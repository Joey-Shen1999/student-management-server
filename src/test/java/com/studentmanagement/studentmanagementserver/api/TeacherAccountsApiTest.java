package com.studentmanagement.studentmanagementserver.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherPasswordResetAuditLog;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.TeacherPasswordResetAuditLogRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherAccountsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TeacherPasswordResetAuditLogRepository teacherPasswordResetAuditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listTeacherAccounts_success() throws Exception {
        User admin = createAdmin("list_admin_user");
        createTeacherAccount("list_teacher_user", "List Teacher");

        mockMvc.perform(get("/api/teacher/accounts")
                        .header("X-User-Id", admin.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].username", hasItem("list_teacher_user")));
    }

    @Test
    void resetPassword_success_returnsTempPassword() throws Exception {
        User admin = createAdmin("reset_admin_user");
        Teacher teacher = createTeacherAccount("reset_teacher_user", "Reset Teacher");

        mockMvc.perform(post("/api/teacher/accounts/{teacherId}/reset-password", teacher.getId())
                        .header("X-User-Id", admin.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherId").value(teacher.getId()))
                .andExpect(jsonPath("$.username").value("reset_teacher_user"))
                .andExpect(jsonPath("$.message").value("Password reset successfully"))
                .andExpect(jsonPath("$.tempPassword").isString());
    }

    @Test
    void resetPassword_teacherNotFound_returns404() throws Exception {
        User admin = createAdmin("nf_admin_user");

        mockMvc.perform(post("/api/teacher/accounts/{teacherId}/reset-password", 999999L)
                        .header("X-User-Id", admin.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Teacher account not found: 999999"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void teacherWithoutPermission_returns403() throws Exception {
        User teacherUser = createTeacherUser("forbidden_teacher_user");

        mockMvc.perform(get("/api/teacher/accounts")
                        .header("X-User-Id", teacherUser.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Forbidden: admin role required."))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/teacher/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthenticated."))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void resetPassword_updatesMustChangePasswordAndWritesAuditLog() throws Exception {
        User admin = createAdmin("audit_admin_user");
        Teacher teacher = createTeacherAccount("audit_teacher_user", "Audit Teacher");
        User targetUser = teacher.getUser();
        targetUser.setMustChangePassword(false);
        userRepository.save(targetUser);

        MvcResult result = mockMvc.perform(post("/api/teacher/accounts/{teacherId}/reset-password", teacher.getId())
                        .header("X-User-Id", admin.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String tempPassword = json.get("tempPassword").asText();
        assertEquals(8, tempPassword.length());

        User updatedUser = userRepository.findById(targetUser.getId())
                .orElseThrow(() -> new RuntimeException("Target user not found"));
        assertTrue(updatedUser.isMustChangePassword());
        assertTrue(passwordEncoder.matches(tempPassword, updatedUser.getPasswordHash()));

        List<TeacherPasswordResetAuditLog> logs = teacherPasswordResetAuditLogRepository.findAll();
        assertFalse(logs.isEmpty());
        TeacherPasswordResetAuditLog latest = logs.get(logs.size() - 1);
        assertEquals(admin.getId(), latest.getOperatorUserId());
        assertEquals(updatedUser.getId(), latest.getTargetUserId());
        assertEquals(teacher.getId(), latest.getTeacherId());
    }

    private User createAdmin(String username) {
        return userRepository.save(new User(username, passwordEncoder.encode("Admin!234"), UserRole.ADMIN));
    }

    private User createTeacherUser(String username) {
        return userRepository.save(new User(username, passwordEncoder.encode("Teacher!234"), UserRole.TEACHER));
    }

    private Teacher createTeacherAccount(String username, String displayName) {
        User user = createTeacherUser(username);
        return teacherRepository.save(new Teacher(user, displayName));
    }
}
