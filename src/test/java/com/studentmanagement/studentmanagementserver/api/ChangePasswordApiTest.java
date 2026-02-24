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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChangePasswordApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthSessionService authSessionService;

    @Test
    void changePassword_success_updatesPasswordAndMustChangeFlag() throws Exception {
        User user = userRepository.save(new User("cp_success_user", passwordEncoder.encode("OldPass!1"), UserRole.TEACHER));
        user.setMustChangePassword(true);
        userRepository.save(user);

        String body = "{"
                + "\"oldPassword\":\"OldPass!1\","
                + "\"newPassword\":\"NewPass!2\""
                + "}";

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", bearerFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        User updated = userRepository.findByUsername("cp_success_user")
                .orElseThrow(() -> new RuntimeException("user not found"));

        assertFalse(updated.isMustChangePassword());
        assertTrue(passwordEncoder.matches("NewPass!2", updated.getPasswordHash()));
    }

    @Test
    void changePassword_oldPasswordIncorrect_returnsReadableError() throws Exception {
        User user = userRepository.save(new User("cp_fail_user", passwordEncoder.encode("OldPass!1"), UserRole.TEACHER));

        String body = "{"
                + "\"oldPassword\":\"WrongPass!1\","
                + "\"newPassword\":\"NewPass!2\""
                + "}";

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", bearerFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("oldPassword incorrect"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void changePassword_unauthenticated_returnsUNAUTHENTICATED() throws Exception {
        String body = "{"
                + "\"oldPassword\":\"OldPass!1\","
                + "\"newPassword\":\"NewPass!2\""
                + "}";

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthenticated."))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.details").isArray());
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
