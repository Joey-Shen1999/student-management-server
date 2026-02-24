package com.studentmanagement.studentmanagementserver.api;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.UserSession;
import com.studentmanagement.studentmanagementserver.repo.UserSessionRepository;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Comparator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSessionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Test
    void logout_revokesToken() throws Exception {
        User admin = userRepository.save(new User("logout_admin", passwordEncoder.encode("Admin!234"), UserRole.ADMIN));
        String bearer = bearerFor(admin);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out."));

        mockMvc.perform(get("/api/teacher/accounts")
                        .header("Authorization", bearer))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthenticated."))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void logout_revokedToken_returns401() throws Exception {
        User admin = userRepository.save(new User("logout_admin_revoke", passwordEncoder.encode("Admin!234"), UserRole.ADMIN));
        String bearer = bearerFor(admin);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthenticated."))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void logout_missingAuthorization_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthenticated."))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void logout_malformedAuthorization_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Basic abcdef")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthenticated."))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void logout_unknownToken_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer not_a_real_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthenticated."))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void logout_expiredToken_returns401() throws Exception {
        User admin = userRepository.save(new User("logout_admin_expired", passwordEncoder.encode("Admin!234"), UserRole.ADMIN));
        String bearer = bearerFor(admin);
        UserSession session = latestSessionOf(admin.getId());
        ReflectionTestUtils.setField(session, "expiresAt", LocalDateTime.now().minusMinutes(1));
        userSessionRepository.save(session);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthenticated."))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private UserSession latestSessionOf(Long userId) {
        return userSessionRepository.findAll().stream()
                .filter(s -> s.getUser().getId().equals(userId))
                .max(Comparator.comparing(UserSession::getId))
                .orElseThrow(() -> new RuntimeException("session not found for user: " + userId));
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
