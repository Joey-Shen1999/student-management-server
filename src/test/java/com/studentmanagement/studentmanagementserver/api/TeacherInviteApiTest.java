package com.studentmanagement.studentmanagementserver.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherInviteAuditLog;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.TeacherInviteAuditLogRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherInviteApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TeacherInviteAuditLogRepository teacherInviteAuditLogRepository;

    @Autowired
    private PasswordPolicyValidator passwordPolicyValidator;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthSessionService authSessionService;

    @Test
    void createInvite_success_returnsPolicyCompliantTempPassword() throws Exception {
        User admin = createAdmin("invite_admin_001");
        String username = "invite_user_001";
        String body = "{"
                + "\"username\":\"" + username + "\""
                + "}";

        MvcResult result = mockMvc.perform(post("/api/teacher/invites")
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.tempPassword").isString())
                .andReturn();

        JsonNode resp = objectMapper.readTree(result.getResponse().getContentAsString());
        String tempPassword = resp.get("tempPassword").asText();

        assertTrue(passwordPolicyValidator.validate(username, tempPassword).isEmpty());

        User invited = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("invited user not found"));
        assertEquals(UserRole.TEACHER, invited.getRole());
        assertTrue(invited.isMustChangePassword());
        assertTrue(passwordEncoder.matches(tempPassword, invited.getPasswordHash()));
        assertTrue(teacherRepository.findByUser_Id(invited.getId()).isPresent());

        List<TeacherInviteAuditLog> logs = teacherInviteAuditLogRepository.findAll();
        assertFalse(logs.isEmpty());
        TeacherInviteAuditLog latest = logs.get(logs.size() - 1);
        assertEquals(admin.getId(), latest.getOperatorUserId());
        assertEquals(invited.getId(), latest.getTargetUserId());
    }

    @Test
    void createInvite_duplicateUsername_returnsConflictErrorPayload() throws Exception {
        User admin = createAdmin("invite_admin_dup");
        String username = "invite_dup_user";
        userRepository.save(new User(username, passwordEncoder.encode("Valid!9A"), UserRole.TEACHER));

        String body = "{"
                + "\"username\":\"" + username + "\""
                + "}";

        mockMvc.perform(post("/api/teacher/invites")
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already exists: " + username))
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void createInvite_nonAdmin_returnsForbidden() throws Exception {
        User teacher = userRepository.save(new User("invite_teacher_non_admin", passwordEncoder.encode("Valid!9A"), UserRole.TEACHER));

        mockMvc.perform(post("/api/teacher/invites")
                        .header("Authorization", bearerFor(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"invite_user_non_admin\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Forbidden: admin role required."))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void createInvite_unauthenticated_returnsUNAUTHENTICATED() throws Exception {
        mockMvc.perform(post("/api/teacher/invites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"invite_user_unauth\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthenticated."))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void createInvite_mustChangePasswordAdmin_isBlocked() throws Exception {
        User admin = createAdmin("invite_admin_must_change");
        admin.setMustChangePassword(true);
        userRepository.save(admin);

        mockMvc.perform(post("/api/teacher/invites")
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"invite_user_blocked\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Password change required before accessing this resource."))
                .andExpect(jsonPath("$.code").value("MUST_CHANGE_PASSWORD_REQUIRED"))
                .andExpect(jsonPath("$.details").isArray());
    }

    private User createAdmin(String username) {
        return userRepository.save(new User(username, passwordEncoder.encode("Admin!234"), UserRole.ADMIN));
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
