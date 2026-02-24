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
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import com.studentmanagement.studentmanagementserver.service.PasswordPolicyValidator;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    private PasswordPolicyValidator passwordPolicyValidator;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthSessionService authSessionService;

    @Test
    void listTeacherAccounts_success() throws Exception {
        User admin = createAdmin("list_admin_user");
        createTeacherAccount("list_teacher_user", "List Teacher");

        mockMvc.perform(get("/api/teacher/accounts")
                        .header("Authorization", bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].username", hasItem("list_teacher_user")));
    }

    @Test
    void resetPassword_success_returnsTempPassword() throws Exception {
        User admin = createAdmin("reset_admin_user");
        Teacher teacher = createTeacherAccount("reset_teacher_user", "Reset Teacher");

        mockMvc.perform(post("/api/teacher/accounts/{teacherId}/reset-password", teacher.getId())
                        .header("Authorization", bearerFor(admin))
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
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Teacher account not found: 999999"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void patchRole_success_updatesRole() throws Exception {
        User admin = createAdmin("role_admin_user");
        Teacher teacher = createTeacherAccount("role_target_teacher", "Role Target");

        mockMvc.perform(patch("/api/teacher/accounts/{teacherId}/role", teacher.getId())
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherId").value(teacher.getId()))
                .andExpect(jsonPath("$.username").value("role_target_teacher"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.message").value("Role updated successfully."));

        User updatedUser = userRepository.findById(teacher.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Target user not found"));
        assertEquals(UserRole.ADMIN, updatedUser.getRole());
    }

    @Test
    void patchRole_unauthenticated_returns401() throws Exception {
        Teacher teacher = createTeacherAccount("role_unauth_target", "Role Unauth Target");

        mockMvc.perform(patch("/api/teacher/accounts/{teacherId}/role", teacher.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthenticated."))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void patchRole_nonAdmin_returns403() throws Exception {
        User teacherOperator = createTeacherUser("role_non_admin_operator");
        Teacher teacher = createTeacherAccount("role_non_admin_target", "Role Non Admin Target");

        mockMvc.perform(patch("/api/teacher/accounts/{teacherId}/role", teacher.getId())
                        .header("Authorization", bearerFor(teacherOperator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Forbidden: admin role required."))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void patchRole_teacherNotFound_returns404() throws Exception {
        User admin = createAdmin("role_nf_admin");

        mockMvc.perform(patch("/api/teacher/accounts/{teacherId}/role", 999999L)
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Teacher account not found: 999999"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void patchRole_mustChangePasswordAdmin_returns403() throws Exception {
        User admin = createAdmin("role_must_change_admin");
        admin.setMustChangePassword(true);
        userRepository.save(admin);
        Teacher teacher = createTeacherAccount("role_must_change_target", "Role Must Change Target");

        mockMvc.perform(patch("/api/teacher/accounts/{teacherId}/role", teacher.getId())
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Password change required before accessing this resource."))
                .andExpect(jsonPath("$.code").value("MUST_CHANGE_PASSWORD_REQUIRED"));
    }

    @Test
    void patchRole_invalidRole_returns400() throws Exception {
        User admin = createAdmin("role_invalid_admin");
        Teacher teacher = createTeacherAccount("role_invalid_target", "Role Invalid Target");

        mockMvc.perform(patch("/api/teacher/accounts/{teacherId}/role", teacher.getId())
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"STUDENT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("role must be ADMIN or TEACHER"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void patchRole_missingRole_returns400() throws Exception {
        User admin = createAdmin("role_missing_admin");
        Teacher teacher = createTeacherAccount("role_missing_target", "Role Missing Target");

        mockMvc.perform(patch("/api/teacher/accounts/{teacherId}/role", teacher.getId())
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("role is required"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void teacherWithoutPermission_returns403() throws Exception {
        User teacherUser = createTeacherUser("forbidden_teacher_user");

        mockMvc.perform(get("/api/teacher/accounts")
                        .header("Authorization", bearerFor(teacherUser)))
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
    void mustChangePasswordAdmin_isBlockedFromTeacherManagementApis() throws Exception {
        User admin = createAdmin("must_change_admin_user");
        admin.setMustChangePassword(true);
        userRepository.save(admin);
        Teacher teacher = createTeacherAccount("must_change_target_user", "Must Change Target");

        mockMvc.perform(get("/api/teacher/accounts")
                        .header("Authorization", bearerFor(admin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Password change required before accessing this resource."))
                .andExpect(jsonPath("$.code").value("MUST_CHANGE_PASSWORD_REQUIRED"))
                .andExpect(jsonPath("$.details").isArray());

        mockMvc.perform(post("/api/teacher/accounts/{teacherId}/reset-password", teacher.getId())
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Password change required before accessing this resource."))
                .andExpect(jsonPath("$.code").value("MUST_CHANGE_PASSWORD_REQUIRED"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void resetPassword_updatesMustChangePasswordAndWritesAuditLog() throws Exception {
        User admin = createAdmin("audit_admin_user");
        Teacher teacher = createTeacherAccount("audit_teacher_user", "Audit Teacher");
        User targetUser = teacher.getUser();
        String targetBearer = bearerFor(targetUser);
        targetUser.setMustChangePassword(false);
        userRepository.save(targetUser);

        MvcResult result = mockMvc.perform(post("/api/teacher/accounts/{teacherId}/reset-password", teacher.getId())
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String tempPassword = json.get("tempPassword").asText();
        assertEquals(8, tempPassword.length());
        assertTrue(passwordPolicyValidator.validate(updatedUserName(teacher), tempPassword).isEmpty());

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

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", targetBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"Teacher!234\",\"newPassword\":\"NewPass!2\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthenticated."))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
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

    private String updatedUserName(Teacher teacher) {
        return teacher.getUser().getUsername();
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
