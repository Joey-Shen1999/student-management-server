package com.studentmanagement.studentmanagementserver.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private PasswordPolicyValidator passwordPolicyValidator;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createInvite_success_returnsPolicyCompliantTempPassword() throws Exception {
        String username = "invite_user_001";
        String body = "{"
                + "\"username\":\"" + username + "\""
                + "}";

        MvcResult result = mockMvc.perform(post("/api/teacher/invites")
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
    }

    @Test
    void createInvite_duplicateUsername_returnsConflictErrorPayload() throws Exception {
        String username = "invite_dup_user";
        userRepository.save(new User(username, passwordEncoder.encode("Valid!9A"), UserRole.TEACHER));

        String body = "{"
                + "\"username\":\"" + username + "\""
                + "}";

        mockMvc.perform(post("/api/teacher/invites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already exists: " + username))
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"))
                .andExpect(jsonPath("$.details").isArray());
    }
}
