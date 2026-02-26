package com.studentmanagement.studentmanagementserver.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OntarioCourseProviderReferenceApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void search_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/reference/ontario-course-providers/search")
                        .param("q", "bayview night"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthenticated."))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void search_byNightSchoolName_returnsBayviewSecondaryNightSchool() throws Exception {
        User user = userRepository.save(new User("provider_ref_student", passwordEncoder.encode("Student!234"), UserRole.STUDENT));

        MvcResult result = mockMvc.perform(get("/api/reference/ontario-course-providers/search")
                        .header("Authorization", bearerFor(user))
                        .param("q", "bayview secondary night")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode bayviewNight = findByName(payload, "Bayview Secondary Night School");
        assertNotNull(bayviewNight, "Bayview Secondary Night School should be returned for query");
        assertEquals("York Region DSB", bayviewNight.get("boardName").asText());
        assertEquals("Continuing Education", bayviewNight.get("schoolSpecialConditions").asText());
        assertEquals("Ontario", bayviewNight.get("state").asText());
        assertEquals("Canada", bayviewNight.get("country").asText());
        assertAllOntario(payload);
    }

    @Test
    void search_withoutQuery_returnsLimitedList() throws Exception {
        User user = userRepository.save(new User("provider_ref_teacher", passwordEncoder.encode("Teacher!234"), UserRole.TEACHER));

        MvcResult result = mockMvc.perform(get("/api/reference/ontario-course-providers/search")
                        .header("Authorization", bearerFor(user))
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(payload.isArray());
        assertEquals(5, payload.size());
        assertAllOntario(payload);
    }

    private JsonNode findByName(JsonNode listNode, String schoolName) {
        if (listNode == null || !listNode.isArray()) {
            return null;
        }
        for (JsonNode item : listNode) {
            JsonNode nameNode = item.get("name");
            if (nameNode != null && schoolName.equals(nameNode.asText())) {
                return item;
            }
        }
        return null;
    }

    private void assertAllOntario(JsonNode listNode) {
        if (listNode == null || !listNode.isArray()) {
            return;
        }
        for (JsonNode item : listNode) {
            assertEquals("Ontario", item.path("state").asText(), "state should always be Ontario");
            assertEquals("Canada", item.path("country").asText(), "country should always be Canada");
        }
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
