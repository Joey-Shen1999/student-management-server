package com.studentmanagement.studentmanagementserver.api;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
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

    @Test
    void changePassword_success_updatesPasswordAndMustChangeFlag() throws Exception {
        String username = "cp_success_user";
        User user = userRepository.save(new User(username, passwordEncoder.encode("OldPass!1"), UserRole.TEACHER));
        user.setMustChangePassword(true);
        userRepository.save(user);

        String body = "{"
                + "\"username\":\"" + username + "\","
                + "\"oldPassword\":\"OldPass!1\","
                + "\"newPassword\":\"NewPass!2\""
                + "}";

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        User updated = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("user not found"));

        assertFalse(updated.isMustChangePassword());
        assertTrue(passwordEncoder.matches("NewPass!2", updated.getPasswordHash()));
    }

    @Test
    void changePassword_oldPasswordIncorrect_returnsReadableError() throws Exception {
        String username = "cp_fail_user";
        userRepository.save(new User(username, passwordEncoder.encode("OldPass!1"), UserRole.TEACHER));

        String body = "{"
                + "\"username\":\"" + username + "\","
                + "\"oldPassword\":\"WrongPass!1\","
                + "\"newPassword\":\"NewPass!2\""
                + "}";

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("oldPassword incorrect"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.details").isArray());
    }
}
