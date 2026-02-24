package com.studentmanagement.studentmanagementserver.api;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthPasswordPolicyApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthSessionService authSessionService;

    @Test
    void register_returnsStructuredPasswordPolicyError() throws Exception {
        String body = "{"
                + "\"username\":\"student001\","
                + "\"password\":\"abc12345\","
                + "\"role\":\"STUDENT\","
                + "\"firstName\":\"Joey\","
                + "\"lastName\":\"Shen\""
                + "}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password does not meet policy requirements."))
                .andExpect(jsonPath("$.code").value("PASSWORD_POLICY_VIOLATION"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void setPassword_returnsStructuredPasswordPolicyError() throws Exception {
        User user = userRepository.save(new User("teacherApi", passwordEncoder.encode("Valid!9A"), UserRole.TEACHER));
        user.setMustChangePassword(true);
        userRepository.save(user);

        String body = "{"
                + "\"userId\":" + user.getId() + ","
                + "\"newPassword\":\"teacherApi!1A\""
                + "}";

        mockMvc.perform(post("/api/auth/set-password")
                        .header("Authorization", bearerFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password does not meet policy requirements."))
                .andExpect(jsonPath("$.code").value("PASSWORD_POLICY_VIOLATION"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void setPassword_userIdMismatch_returnsForbiddenErrorPayload() throws Exception {
        User self = userRepository.save(new User("setpwd_self_api", passwordEncoder.encode("Valid!9A"), UserRole.TEACHER));
        self.setMustChangePassword(true);
        userRepository.save(self);

        User other = userRepository.save(new User("setpwd_other_api", passwordEncoder.encode("Valid!9A"), UserRole.TEACHER));

        String body = "{"
                + "\"userId\":" + other.getId() + ","
                + "\"newPassword\":\"Valid!9A\""
                + "}";

        mockMvc.perform(post("/api/auth/set-password")
                        .header("Authorization", bearerFor(self))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Cannot set password for another user."))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void setPassword_unauthenticated_returnsUnauthorizedErrorPayload() throws Exception {
        String body = "{"
                + "\"newPassword\":\"Valid!9A\""
                + "}";

        mockMvc.perform(post("/api/auth/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthenticated."))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void register_duplicateUsername_returnsConflictErrorPayload() throws Exception {
        String username = "dup_user_api";
        userRepository.save(new User(username, passwordEncoder.encode("Valid!9A"), UserRole.TEACHER));

        String body = "{"
                + "\"username\":\"" + username + "\","
                + "\"password\":\"Valid!9A\","
                + "\"role\":\"TEACHER\","
                + "\"displayName\":\"Teacher Dup\""
                + "}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already exists"))
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"))
                .andExpect(jsonPath("$.details").isArray());
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
