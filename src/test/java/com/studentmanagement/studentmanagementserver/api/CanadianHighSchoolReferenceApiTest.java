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
class CanadianHighSchoolReferenceApiTest {

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
        mockMvc.perform(get("/api/reference/canadian-high-schools/search")
                        .param("q", "Unionville"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthenticated."))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void search_withTypoQuery_returnsUnionvilleWithAddress() throws Exception {
        User user = userRepository.save(new User("school_ref_student", passwordEncoder.encode("Student!234"), UserRole.STUDENT));

        MvcResult result = mockMvc.perform(get("/api/reference/canadian-high-schools/search")
                        .header("Authorization", bearerFor(user))
                        .param("q", "unionvile")
                        .param("limit", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode unionville = findSchoolByName(payload, "Unionville High School");
        assertNotNull(unionville, "Unionville High School should be returned for fuzzy query");
        assertEquals("201 Town Centre Blvd", unionville.get("streetAddress").asText());
        assertEquals("Unionville", unionville.get("city").asText());
        assertEquals("Ontario", unionville.get("state").asText());
        assertEquals("Canada", unionville.get("country").asText());
        assertEquals("L3R 8G5", unionville.get("postal").asText());
        assertAllOntario(payload);
    }

    @Test
    void search_withAcronymQuery_returnsRichmondGreenSecondarySchool() throws Exception {
        User user = userRepository.save(new User("school_ref_rgss", passwordEncoder.encode("Student!234"), UserRole.STUDENT));

        MvcResult result = mockMvc.perform(get("/api/reference/canadian-high-schools/search")
                        .header("Authorization", bearerFor(user))
                        .param("q", "RGSS")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode rgss = findSchoolByName(payload, "Richmond Green Secondary School");
        assertNotNull(rgss, "RGSS query should return Richmond Green Secondary School");
        assertEquals("1 William F. Bell Parkway", rgss.get("streetAddress").asText());
        assertEquals("Richmond Hill", rgss.get("city").asText());
        assertEquals("Ontario", rgss.get("state").asText());
        assertEquals("Canada", rgss.get("country").asText());
        assertEquals("L4S 2T9", rgss.get("postal").asText());
        assertAllOntario(payload);
    }

    @Test
    void search_withoutQuery_returnsLimitedList() throws Exception {
        User user = userRepository.save(new User("school_ref_teacher", passwordEncoder.encode("Teacher!234"), UserRole.TEACHER));

        MvcResult result = mockMvc.perform(get("/api/reference/canadian-high-schools/search")
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

    private JsonNode findSchoolByName(JsonNode listNode, String schoolName) {
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
