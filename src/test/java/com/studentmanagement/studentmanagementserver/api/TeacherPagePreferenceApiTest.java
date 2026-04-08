package com.studentmanagement.studentmanagementserver.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherPagePreference;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.TeacherPagePreferenceRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherPagePreferenceApiTest {

    private static final String GOAL_PAGE_KEY = "goal-management.create-goal.student-selector-columns";
    private static final String STUDENT_PAGE_KEY = "student-management.list-columns";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TeacherPagePreferenceRepository teacherPagePreferenceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void put_withOrderedColumnKeys_returns200AndPersistsNormalizedPayload() throws Exception {
        Teacher teacher = createTeacherAccount("pref_put_ordered_teacher", "Pref Put Ordered");

        mockMvc.perform(put("/api/teacher/preferences/{pageKey}", GOAL_PAGE_KEY)
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\" v2 \"," +
                                "\"visibleColumnKeys\":[\" name \",\"email\",\"email\",\"\",\"status\"]," +
                                "\"orderedColumnKeys\":[\"name\",\" schoolBoard \",\"schoolBoard\",\"city\",\"\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageKey").value(GOAL_PAGE_KEY))
                .andExpect(jsonPath("$.version").value("v2"))
                .andExpect(jsonPath("$.visibleColumnKeys.length()").value(3))
                .andExpect(jsonPath("$.visibleColumnKeys[0]").value("name"))
                .andExpect(jsonPath("$.visibleColumnKeys[1]").value("email"))
                .andExpect(jsonPath("$.visibleColumnKeys[2]").value("status"))
                .andExpect(jsonPath("$.orderedColumnKeys.length()").value(3))
                .andExpect(jsonPath("$.orderedColumnKeys[0]").value("name"))
                .andExpect(jsonPath("$.orderedColumnKeys[1]").value("schoolBoard"))
                .andExpect(jsonPath("$.orderedColumnKeys[2]").value("city"))
                .andExpect(jsonPath("$.updatedAt").isString());

        TeacherPagePreference saved = teacherPagePreferenceRepository
                .findByTeacher_IdAndPageKey(teacher.getId(), GOAL_PAGE_KEY)
                .orElseThrow(() -> new RuntimeException("preference not saved"));
        assertNotNull(saved.getVisibleColumnKeysJson());
        assertNotNull(saved.getOrderedColumnKeysJson());
    }

    @Test
    void get_afterPut_readsBackSameOrderedColumnKeys() throws Exception {
        Teacher teacher = createTeacherAccount("pref_get_roundtrip_teacher", "Pref Get Roundtrip");
        String bearer = bearerFor(teacher.getUser());

        mockMvc.perform(put("/api/teacher/preferences/{pageKey}", GOAL_PAGE_KEY)
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"v2\"," +
                                "\"visibleColumnKeys\":[\"name\",\"email\",\"status\"]," +
                                "\"orderedColumnKeys\":[\"name\",\"schoolBoard\",\"city\",\"email\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/teacher/preferences/{pageKey}", GOAL_PAGE_KEY)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageKey").value(GOAL_PAGE_KEY))
                .andExpect(jsonPath("$.orderedColumnKeys.length()").value(4))
                .andExpect(jsonPath("$.orderedColumnKeys[0]").value("name"))
                .andExpect(jsonPath("$.orderedColumnKeys[1]").value("schoolBoard"))
                .andExpect(jsonPath("$.orderedColumnKeys[2]").value("city"))
                .andExpect(jsonPath("$.orderedColumnKeys[3]").value("email"));
    }

    @Test
    void get_legacyDataWithVisibleColumnKeysOnly_doesNotFail() throws Exception {
        Teacher teacher = createTeacherAccount("pref_get_legacy_teacher", "Pref Legacy");
        TeacherPagePreference legacy = new TeacherPagePreference(teacher, GOAL_PAGE_KEY);
        legacy.setVersion("v1");
        legacy.setVisibleColumnKeysJson("[\"name\",\"email\"]");
        legacy.setOrderedColumnKeysJson(null);
        teacherPagePreferenceRepository.save(legacy);

        MvcResult result = mockMvc.perform(get("/api/teacher/preferences/{pageKey}", GOAL_PAGE_KEY)
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageKey").value(GOAL_PAGE_KEY))
                .andExpect(jsonPath("$.visibleColumnKeys.length()").value(2))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode orderedNode = body.get("orderedColumnKeys");
        assertTrue(orderedNode == null || orderedNode.isNull() || orderedNode.isArray());
    }

    @Test
    void put_withoutOrderedColumnKeys_oldClientRequest_stillSucceeds() throws Exception {
        Teacher teacher = createTeacherAccount("pref_put_legacy_client_teacher", "Pref Legacy Client");
        String bearer = bearerFor(teacher.getUser());

        mockMvc.perform(put("/api/teacher/preferences/{pageKey}", GOAL_PAGE_KEY)
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"v2\"," +
                                "\"visibleColumnKeys\":[\"name\",\"email\"]," +
                                "\"orderedColumnKeys\":[\"name\",\"city\",\"email\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/teacher/preferences/{pageKey}", GOAL_PAGE_KEY)
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"v2\"," +
                                "\"visibleColumnKeys\":[\"name\",\"status\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibleColumnKeys.length()").value(2))
                .andExpect(jsonPath("$.orderedColumnKeys.length()").value(3));

        mockMvc.perform(get("/api/teacher/preferences/{pageKey}", GOAL_PAGE_KEY)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibleColumnKeys.length()").value(2))
                .andExpect(jsonPath("$.visibleColumnKeys[0]").value("name"))
                .andExpect(jsonPath("$.visibleColumnKeys[1]").value("status"))
                .andExpect(jsonPath("$.orderedColumnKeys.length()").value(3))
                .andExpect(jsonPath("$.orderedColumnKeys[0]").value("name"))
                .andExpect(jsonPath("$.orderedColumnKeys[1]").value("city"))
                .andExpect(jsonPath("$.orderedColumnKeys[2]").value("email"));
    }

    @Test
    void preference_isolatedByTeacherAndPageKey() throws Exception {
        Teacher teacherA = createTeacherAccount("pref_isolation_teacher_a", "Pref Isolation A");
        Teacher teacherB = createTeacherAccount("pref_isolation_teacher_b", "Pref Isolation B");
        String bearerA = bearerFor(teacherA.getUser());
        String bearerB = bearerFor(teacherB.getUser());

        mockMvc.perform(put("/api/teacher/preferences/{pageKey}", GOAL_PAGE_KEY)
                        .header("Authorization", bearerA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"v2\"," +
                                "\"visibleColumnKeys\":[\"name\",\"email\"]," +
                                "\"orderedColumnKeys\":[\"name\",\"schoolBoard\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/teacher/preferences/{pageKey}", STUDENT_PAGE_KEY)
                        .header("Authorization", bearerA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"v5\"," +
                                "\"visibleColumnKeys\":[\"email\",\"phone\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/teacher/preferences/{pageKey}", GOAL_PAGE_KEY)
                        .header("Authorization", bearerB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"v2\"," +
                                "\"visibleColumnKeys\":[\"status\"]," +
                                "\"orderedColumnKeys\":[\"status\",\"name\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/teacher/preferences/{pageKey}", GOAL_PAGE_KEY)
                        .header("Authorization", bearerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibleColumnKeys[0]").value("name"))
                .andExpect(jsonPath("$.orderedColumnKeys[0]").value("name"));

        mockMvc.perform(get("/api/teacher/preferences/{pageKey}", STUDENT_PAGE_KEY)
                        .header("Authorization", bearerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("v5"))
                .andExpect(jsonPath("$.visibleColumnKeys[0]").value("email"))
                .andExpect(jsonPath("$.visibleColumnKeys[1]").value("phone"));

        mockMvc.perform(get("/api/teacher/preferences/{pageKey}", GOAL_PAGE_KEY)
                        .header("Authorization", bearerB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibleColumnKeys.length()").value(1))
                .andExpect(jsonPath("$.visibleColumnKeys[0]").value("status"))
                .andExpect(jsonPath("$.orderedColumnKeys[0]").value("status"))
                .andExpect(jsonPath("$.orderedColumnKeys[1]").value("name"));

        assertEquals(2, teacherPagePreferenceRepository.findByTeacher_IdAndPageKey(teacherA.getId(), GOAL_PAGE_KEY)
                .map(item -> 1)
                .orElse(0)
                + teacherPagePreferenceRepository.findByTeacher_IdAndPageKey(teacherA.getId(), STUDENT_PAGE_KEY)
                .map(item -> 1)
                .orElse(0));
        assertEquals(1, teacherPagePreferenceRepository.findByTeacher_IdAndPageKey(teacherB.getId(), GOAL_PAGE_KEY)
                .map(item -> 1)
                .orElse(0));
    }

    private Teacher createTeacherAccount(String username, String displayName) {
        User user = userRepository.save(new User(username, passwordEncoder.encode("Teacher!234"), UserRole.TEACHER));
        return teacherRepository.save(new Teacher(user, displayName));
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
