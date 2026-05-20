package com.studentmanagement.studentmanagementserver.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherStudent;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherStudentRepository;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExtracurricularTrackingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TeacherStudentRepository teacherStudentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void teacherPutAndStudentGetExtracurricularTracking_success() throws Exception {
        Teacher teacher = createTeacherAccount("extra_teacher_rw", "Extracurricular RW Teacher");
        Student student = createStudentAccount("extra_student_rw", "Extra", "Rw", "ERW");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        Map<String, Object> payload = buildTrackingPayload(
                "admission related activities",
                Arrays.asList(
                        buildCompetition(
                                "Canadian Math Competition",
                                "CEMC",
                                "Participant",
                                "NATIONAL",
                                "Distinction",
                                "Mathematics",
                                "2026-05-01"
                        ),
                        buildTimedActivity(
                                "SUMMER_CAMP",
                                "Engineering Summer Camp",
                                "University of Waterloo",
                                "Student",
                                "INTERNATIONAL",
                                "",
                                "2026-07-01",
                                "2026-07-21"
                        )
                )
        );

        mockMvc.perform(put("/api/teacher/students/{studentId}/extracurricular-tracking", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.note").value("admission related activities"))
                .andExpect(jsonPath("$.totalActivities").value(2))
                .andExpect(jsonPath("$.competitionCount").value(1))
                .andExpect(jsonPath("$.awardCount").value(1))
                .andExpect(jsonPath("$.activities.length()").value(2))
                .andExpect(jsonPath("$.activities[0].activityType").value("COMPETITION"))
                .andExpect(jsonPath("$.activities[0].activityDate").value("2026-05-01"))
                .andExpect(jsonPath("$.activities[0].competitionCategory").value("Mathematics"))
                .andExpect(jsonPath("$.activities[1].activityType").value("SUMMER_CAMP"))
                .andExpect(jsonPath("$.activities[1].startDate").value("2026-07-01"))
                .andExpect(jsonPath("$.activities[1].endDate").value("2026-07-21"))
                .andExpect(jsonPath("$.updatedByTeacherId").value(teacher.getId()))
                .andExpect(jsonPath("$.updatedByTeacherName").value("Extracurricular RW Teacher"))
                .andExpect(jsonPath("$.records.length()").value(1))
                .andExpect(jsonPath("$.records[0].title").value("Extracurricular Activities"))
                .andExpect(jsonPath("$.records[0].totalActivities").value(2))
                .andExpect(jsonPath("$.records[0].competitionCount").value(1))
                .andExpect(jsonPath("$.records[0].awardCount").value(1));

        mockMvc.perform(get("/api/student/extracurricular-tracking")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.totalActivities").value(2))
                .andExpect(jsonPath("$.competitionCount").value(1))
                .andExpect(jsonPath("$.awardCount").value(1))
                .andExpect(jsonPath("$.activities.length()").value(2))
                .andExpect(jsonPath("$.activities[0].activityName").value("Canadian Math Competition"))
                .andExpect(jsonPath("$.records[0].activities.length()").value(2));
    }

    @Test
    void studentPutExtracurricularTracking_success() throws Exception {
        Student student = createStudentAccount("extra_student_put_self", "Extra", "Self", "ESE");

        mockMvc.perform(put("/api/student/extracurricular-tracking")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildTrackingPayload(
                                "",
                                Arrays.asList(
                                        buildTimedActivity(
                                                "PUBLIC_EVENT",
                                                "Open House Speech",
                                                "School",
                                                "Speaker",
                                                "SCHOOL",
                                                "Presented",
                                                "2026-06-01",
                                                "2026-06-01"
                                        )
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.totalActivities").value(1))
                .andExpect(jsonPath("$.awardCount").value(1))
                .andExpect(jsonPath("$.activities[0].activityType").value("PUBLIC_EVENT"))
                .andExpect(jsonPath("$.records.length()").value(1));
    }

    @Test
    void extracurricularTrackingValidation_missingCompetitionDate_returns400() throws Exception {
        Teacher teacher = createTeacherAccount("extra_teacher_val_comp", "Extracurricular Competition Date Teacher");
        Student student = createStudentAccount("extra_student_val_comp", "Extra", "Comp", "ECO");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        Map<String, Object> competition = buildCompetition(
                "Missing Date Competition",
                "Organizer",
                "Participant",
                "CITY",
                "",
                "Math",
                "2026-05-01"
        );
        competition.remove("activityDate");

        mockMvc.perform(put("/api/teacher/students/{studentId}/extracurricular-tracking", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildTrackingPayload("note", Arrays.asList(competition)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("activities[0].activityDate is required for competition records"));
    }

    @Test
    void extracurricularTrackingValidation_endDateBeforeStartDate_returns400() throws Exception {
        Teacher teacher = createTeacherAccount("extra_teacher_val_date", "Extracurricular Date Teacher");
        Student student = createStudentAccount("extra_student_val_date", "Extra", "Date", "EDA");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        mockMvc.perform(put("/api/teacher/students/{studentId}/extracurricular-tracking", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildTrackingPayload(
                                "note",
                                Arrays.asList(
                                        buildTimedActivity(
                                                "SUMMER_CAMP",
                                                "Bad Date Camp",
                                                "Camp",
                                                "Student",
                                                "PROVINCE",
                                                "",
                                                "2026-08-10",
                                                "2026-08-01"
                                        )
                                )
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("activities[0].endDate must be on or after startDate"));
    }

    @Test
    void extracurricularTrackingPermissions_teacherAdminAndStudentRules_work() throws Exception {
        Teacher teacherA = createTeacherAccount("extra_teacher_scope_a", "Extracurricular Scope A");
        Teacher teacherB = createTeacherAccount("extra_teacher_scope_b", "Extracurricular Scope B");
        User admin = createAdmin("extra_admin_scope");
        Student student = createStudentAccount("extra_student_scope", "Extra", "Scope", "ESC");
        assignTeacherStudent(teacherB, student, TeacherStudentStatus.ACTIVE);

        Map<String, Object> payload = buildTrackingPayload(
                "scope note",
                Arrays.asList(
                        buildCompetition(
                                "Scope Competition",
                                "Organizer",
                                "Participant",
                                "CITY",
                                "Finalist",
                                "Debate",
                                "2026-05-02"
                        )
                )
        );

        mockMvc.perform(get("/api/teacher/students/{studentId}/extracurricular-tracking", student.getId())
                        .header("Authorization", bearerFor(teacherA.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()));

        mockMvc.perform(put("/api/teacher/students/{studentId}/extracurricular-tracking", student.getId())
                        .header("Authorization", bearerFor(teacherA.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()));

        mockMvc.perform(put("/api/teacher/students/{studentId}/extracurricular-tracking", student.getId())
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()));

        mockMvc.perform(put("/api/teacher/students/{studentId}/extracurricular-tracking", student.getId())
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/student/extracurricular-tracking")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.activities.length()").value(1));
    }

    @Test
    void studentGetExtracurricularTracking_whenNoData_returnsEmptyPayload() throws Exception {
        Student student = createStudentAccount("extra_student_empty", "Extra", "Empty", "EEM");

        MvcResult result = mockMvc.perform(get("/api/student/extracurricular-tracking")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.totalActivities").value(0))
                .andExpect(jsonPath("$.competitionCount").value(0))
                .andExpect(jsonPath("$.awardCount").value(0))
                .andExpect(jsonPath("$.activities.length()").value(0))
                .andExpect(jsonPath("$.records.length()").value(0))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(body.path("createdAt").isNull());
        assertTrue(body.path("updatedAt").isNull());
        assertTrue(body.path("updatedByTeacherId").isNull());
        assertTrue(body.path("updatedByTeacherName").isNull());
    }

    @Test
    void batchSummary_returnsActivityStatsAndUpdatedAt() throws Exception {
        Teacher teacher = createTeacherAccount("extra_teacher_batch", "Extracurricular Batch Teacher");
        Student studentA = createStudentAccount("extra_batch_student_a", "Extra", "BatchA", "EBA");
        Student studentB = createStudentAccount("extra_batch_student_b", "Extra", "BatchB", "EBB");
        Student studentC = createStudentAccount("extra_batch_student_c", "Extra", "BatchC", "EBC");
        assignTeacherStudent(teacher, studentA, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, studentB, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, studentC, TeacherStudentStatus.ACTIVE);

        mockMvc.perform(put("/api/teacher/students/{studentId}/extracurricular-tracking", studentA.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildTrackingPayload(
                                "student a",
                                Arrays.asList(
                                        buildCompetition(
                                                "A Competition",
                                                "Organizer",
                                                "Participant",
                                                "CITY",
                                                "",
                                                "Math",
                                                "2026-05-03"
                                        )
                                )
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/teacher/students/{studentId}/extracurricular-tracking", studentB.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildTrackingPayload(
                                "student b",
                                Arrays.asList(
                                        buildCompetition(
                                                "B Competition",
                                                "Organizer",
                                                "Winner",
                                                "NATIONAL",
                                                "Gold",
                                                "Science",
                                                "2026-05-04"
                                        ),
                                        buildTimedActivity(
                                                "RESEARCH",
                                                "Research Project",
                                                "Lab",
                                                "Assistant",
                                                "SCHOOL",
                                                "",
                                                "2026-05-05",
                                                "2026-06-05"
                                        )
                                )
                        ))))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(post("/api/teacher/students/extracurricular-tracking/batch-summary")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBatchSummaryPayload(
                                Arrays.asList(studentA.getId(), studentB.getId(), studentC.getId())
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(3, items.size());

        JsonNode rowA = findByStudentId(items, studentA.getId());
        JsonNode rowB = findByStudentId(items, studentB.getId());
        JsonNode rowC = findByStudentId(items, studentC.getId());
        assertNotNull(rowA);
        assertNotNull(rowB);
        assertNotNull(rowC);

        assertEquals(1, rowA.path("totalActivities").asInt());
        assertEquals(1, rowA.path("competitionCount").asInt());
        assertEquals(0, rowA.path("awardCount").asInt());
        assertTrue(!rowA.path("updatedAt").isNull());

        assertEquals(2, rowB.path("totalActivities").asInt());
        assertEquals(1, rowB.path("competitionCount").asInt());
        assertEquals(1, rowB.path("awardCount").asInt());
        assertTrue(!rowB.path("updatedAt").isNull());

        assertEquals(0, rowC.path("totalActivities").asInt());
        assertEquals(0, rowC.path("competitionCount").asInt());
        assertEquals(0, rowC.path("awardCount").asInt());
        assertTrue(rowC.path("updatedAt").isNull());
    }

    @Test
    void batchSummaryValidationErrors_return400() throws Exception {
        Teacher teacher = createTeacherAccount("extra_teacher_batch_validate", "Extracurricular Batch Validate Teacher");

        mockMvc.perform(post("/api/teacher/students/extracurricular-tracking/batch-summary")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("studentIds is required"));

        mockMvc.perform(post("/api/teacher/students/extracurricular-tracking/batch-summary")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBatchSummaryPayload(Arrays.asList(0L)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("studentIds must contain positive integers"));

        List<Long> tooManyIds = new ArrayList<Long>();
        for (long i = 1; i <= 101; i++) {
            tooManyIds.add(i);
        }
        mockMvc.perform(post("/api/teacher/students/extracurricular-tracking/batch-summary")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBatchSummaryPayload(tooManyIds))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("studentIds size must be <= 100"));
    }

    private Map<String, Object> buildTrackingPayload(String note,
                                                     List<Map<String, Object>> activities) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("note", note);
        payload.put("activities", activities);
        return payload;
    }

    private Map<String, Object> buildCompetition(String activityName,
                                                 String organization,
                                                 String role,
                                                 String activityLevel,
                                                 String awardOrResult,
                                                 String competitionCategory,
                                                 String activityDate) {
        Map<String, Object> activity = buildBaseActivity(
                "COMPETITION",
                activityName,
                organization,
                role,
                activityLevel,
                awardOrResult
        );
        activity.put("competitionCategory", competitionCategory);
        activity.put("activityDate", activityDate);
        return activity;
    }

    private Map<String, Object> buildTimedActivity(String activityType,
                                                   String activityName,
                                                   String organization,
                                                   String role,
                                                   String activityLevel,
                                                   String awardOrResult,
                                                   String startDate,
                                                   String endDate) {
        Map<String, Object> activity = buildBaseActivity(
                activityType,
                activityName,
                organization,
                role,
                activityLevel,
                awardOrResult
        );
        activity.put("startDate", startDate);
        activity.put("endDate", endDate);
        return activity;
    }

    private Map<String, Object> buildBaseActivity(String activityType,
                                                  String activityName,
                                                  String organization,
                                                  String role,
                                                  String activityLevel,
                                                  String awardOrResult) {
        Map<String, Object> activity = new LinkedHashMap<String, Object>();
        activity.put("activityType", activityType);
        activity.put("activityName", activityName);
        activity.put("organization", organization);
        activity.put("role", role);
        activity.put("activityLevel", activityLevel);
        activity.put("awardOrResult", awardOrResult);
        activity.put("description", "description");
        activity.put("admissionRelevance", "admission relevance");
        activity.put("proofContact", "proof@example.com");
        activity.put("proofUrl", "https://example.com/proof");
        return activity;
    }

    private Map<String, Object> buildBatchSummaryPayload(List<Long> studentIds) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("studentIds", studentIds);
        return payload;
    }

    private JsonNode findByStudentId(JsonNode items, Long studentId) {
        if (items == null || !items.isArray() || studentId == null) {
            return null;
        }
        for (JsonNode item : items) {
            if (item.path("studentId").asLong() == studentId.longValue()) {
                return item;
            }
        }
        return null;
    }

    private User createAdmin(String username) {
        return userRepository.save(new User(username, passwordEncoder.encode("Admin!234"), UserRole.ADMIN));
    }

    private Teacher createTeacherAccount(String username, String displayName) {
        User user = userRepository.save(new User(username, passwordEncoder.encode("Teacher!234"), UserRole.TEACHER));
        Teacher teacher = teacherRepository.save(new Teacher(user, displayName));
        assertTrue(teacher.getId() > 0);
        return teacher;
    }

    private Student createStudentAccount(String username, String firstName, String lastName, String nickName) {
        User user = userRepository.save(new User(username, passwordEncoder.encode("Student!234"), UserRole.STUDENT));
        Student student = studentRepository.save(new Student(user, firstName, lastName, nickName));
        assertTrue(student.getId() > 0);
        return student;
    }

    private void assignTeacherStudent(Teacher teacher, Student student, TeacherStudentStatus status) {
        teacherStudentRepository.save(new TeacherStudent(teacher, student, status, "extracurricular-tracking-test-assignment"));
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
