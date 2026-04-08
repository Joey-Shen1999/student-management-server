package com.studentmanagement.studentmanagementserver.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserAccountStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherStudent;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.domain.volunteer.StudentVolunteerTracking;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentVolunteerTrackingRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentVolunteerTrackingTaskRepository;
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

import java.math.BigDecimal;
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
class VolunteerTrackingApiTest {

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
    private StudentVolunteerTrackingRepository studentVolunteerTrackingRepository;

    @Autowired
    private StudentVolunteerTrackingTaskRepository studentVolunteerTrackingTaskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void teacherPutAndStudentGetVolunteerTracking_success() throws Exception {
        Teacher teacher = createTeacherAccount("vol_teacher_rw", "Volunteer RW Teacher");
        Student student = createStudentAccount("vol_student_rw", "Vol", "Rw", "VRW");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        Map<String, Object> payload = buildVolunteerPayload(
                new BigDecimal("4.00"),
                "on campus and community",
                Arrays.asList(
                        buildTask("Library", "Shelving books", new BigDecimal("2.50"), "2026-04-01", "2026-04-01", "lib@example.com"),
                        buildTask("Community", "Neighborhood event", new BigDecimal("1.50"), "2026-04-02", "2026-04-02", "com@example.com")
                )
        );

        mockMvc.perform(put("/api/teacher/students/{studentId}/volunteer-tracking", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.totalHours").value(4.0))
                .andExpect(jsonPath("$.tasks.length()").value(2))
                .andExpect(jsonPath("$.updatedByTeacherId").value(teacher.getId()));

        mockMvc.perform(get("/api/student/volunteer-tracking")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.totalHours").value(4.0))
                .andExpect(jsonPath("$.tasks.length()").value(2))
                .andExpect(jsonPath("$.tasks[0].taskName").value("Library"));
    }

    @Test
    void teacherPutVolunteerTracking_overwritesTaskRows() throws Exception {
        Teacher teacher = createTeacherAccount("vol_teacher_overwrite", "Volunteer Overwrite Teacher");
        Student student = createStudentAccount("vol_student_overwrite", "Vol", "Overwrite", "VOW");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        mockMvc.perform(put("/api/teacher/students/{studentId}/volunteer-tracking", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildVolunteerPayload(
                                new BigDecimal("3.00"),
                                "v1",
                                Arrays.asList(
                                        buildTask("TaskA", "A", new BigDecimal("1.00"), "2026-04-01", "2026-04-01", "a"),
                                        buildTask("TaskB", "B", new BigDecimal("2.00"), "2026-04-02", "2026-04-02", "b")
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks.length()").value(2));

        Long trackingId = studentVolunteerTrackingRepository.findByStudent_Id(student.getId())
                .orElseThrow(() -> new IllegalStateException("tracking missing"))
                .getId();
        assertEquals(2, studentVolunteerTrackingTaskRepository.findByTracking_IdOrderByIdAsc(trackingId).size());

        mockMvc.perform(put("/api/teacher/students/{studentId}/volunteer-tracking", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildVolunteerPayload(
                                new BigDecimal("3.50"),
                                "v2",
                                Arrays.asList(
                                        buildTask("TaskC", "C", new BigDecimal("3.50"), "2026-04-03", "2026-04-03", "c")
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks.length()").value(1))
                .andExpect(jsonPath("$.tasks[0].taskName").value("TaskC"))
                .andExpect(jsonPath("$.totalHours").value(3.5));

        assertEquals(1, studentVolunteerTrackingTaskRepository.findByTracking_IdOrderByIdAsc(trackingId).size());
        assertEquals(
                "TaskC",
                studentVolunteerTrackingTaskRepository.findByTracking_IdOrderByIdAsc(trackingId).get(0).getTaskName()
        );
    }

    @Test
    void volunteerTrackingValidation_missingTaskField_returns400() throws Exception {
        Teacher teacher = createTeacherAccount("vol_teacher_val_missing", "Volunteer Missing Teacher");
        Student student = createStudentAccount("vol_student_val_missing", "Vol", "Missing", "VMI");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        Map<String, Object> task = buildTask("TaskA", "Desc", new BigDecimal("1.00"), "2026-04-01", "2026-04-01", "c");
        task.remove("taskName");

        mockMvc.perform(put("/api/teacher/students/{studentId}/volunteer-tracking", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildVolunteerPayload(
                                new BigDecimal("1.00"),
                                "note",
                                Arrays.asList(task)
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("tasks[0].taskName is required"));
    }

    @Test
    void volunteerTrackingValidation_negativeDuration_returns400() throws Exception {
        Teacher teacher = createTeacherAccount("vol_teacher_val_neg", "Volunteer Neg Teacher");
        Student student = createStudentAccount("vol_student_val_neg", "Vol", "Neg", "VNE");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        mockMvc.perform(put("/api/teacher/students/{studentId}/volunteer-tracking", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildVolunteerPayload(
                                new BigDecimal("1.00"),
                                "note",
                                Arrays.asList(
                                        buildTask("TaskA", "Desc", new BigDecimal("-1.00"), "2026-04-01", "2026-04-01", "c")
                                )
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("tasks[0].durationHours must be greater than 0"));
    }

    @Test
    void volunteerTrackingValidation_endDateBeforeStartDate_returns400() throws Exception {
        Teacher teacher = createTeacherAccount("vol_teacher_val_date", "Volunteer Date Teacher");
        Student student = createStudentAccount("vol_student_val_date", "Vol", "Date", "VDA");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        mockMvc.perform(put("/api/teacher/students/{studentId}/volunteer-tracking", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildVolunteerPayload(
                                new BigDecimal("1.00"),
                                "note",
                                Arrays.asList(
                                        buildTask("TaskA", "Desc", new BigDecimal("1.00"), "2026-04-02", "2026-04-01", "c")
                                )
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("tasks[0].endDate must be on or after startDate"));
    }

    @Test
    void volunteerTrackingValidation_totalHoursMismatch_returns400() throws Exception {
        Teacher teacher = createTeacherAccount("vol_teacher_val_total", "Volunteer Total Teacher");
        Student student = createStudentAccount("vol_student_val_total", "Vol", "Total", "VTO");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        mockMvc.perform(put("/api/teacher/students/{studentId}/volunteer-tracking", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildVolunteerPayload(
                                new BigDecimal("5.00"),
                                "note",
                                Arrays.asList(
                                        buildTask("TaskA", "Desc", new BigDecimal("1.00"), "2026-04-01", "2026-04-01", "c"),
                                        buildTask("TaskB", "Desc", new BigDecimal("2.00"), "2026-04-02", "2026-04-02", "d")
                                )
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("totalHours must equal sum(tasks.durationHours)"));
    }

    @Test
    void volunteerTrackingPermissions_teacherScopeAdminAndStudentRules_work() throws Exception {
        Teacher teacherA = createTeacherAccount("vol_teacher_scope_a", "Volunteer Scope A");
        Teacher teacherB = createTeacherAccount("vol_teacher_scope_b", "Volunteer Scope B");
        User admin = createAdmin("vol_admin_scope");
        Student student = createStudentAccount("vol_student_scope", "Vol", "Scope", "VSC");
        assignTeacherStudent(teacherB, student, TeacherStudentStatus.ACTIVE);

        Map<String, Object> payload = buildVolunteerPayload(
                new BigDecimal("1.00"),
                "scope note",
                Arrays.asList(
                        buildTask("TaskA", "Desc", new BigDecimal("1.00"), "2026-04-01", "2026-04-01", "contact")
                )
        );

        mockMvc.perform(get("/api/teacher/students/{studentId}/volunteer-tracking", student.getId())
                        .header("Authorization", bearerFor(teacherA.getUser())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(put("/api/teacher/students/{studentId}/volunteer-tracking", student.getId())
                        .header("Authorization", bearerFor(teacherA.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(put("/api/teacher/students/{studentId}/volunteer-tracking", student.getId())
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()));

        mockMvc.perform(put("/api/teacher/students/{studentId}/volunteer-tracking", student.getId())
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/student/volunteer-tracking")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.tasks.length()").value(1));
    }

    @Test
    void studentGetVolunteerTracking_whenNoData_returnsEmptyPayload() throws Exception {
        Student student = createStudentAccount("vol_student_empty", "Vol", "Empty", "VEM");

        MvcResult result = mockMvc.perform(get("/api/student/volunteer-tracking")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.totalHours").value(0))
                .andExpect(jsonPath("$.tasks.length()").value(0))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(body.path("updatedAt").isNull());
        assertTrue(body.path("updatedByTeacherId").isNull());
    }

    @Test
    void batchSummary_returnsHoursCompletionAndUpdatedAt() throws Exception {
        Teacher teacher = createTeacherAccount("vol_teacher_batch", "Volunteer Batch Teacher");
        Student studentA = createStudentAccount("vol_batch_student_a", "Batch", "A", "BA");
        Student studentB = createStudentAccount("vol_batch_student_b", "Batch", "B", "BB");
        Student studentC = createStudentAccount("vol_batch_student_c", "Batch", "C", "BC");
        assignTeacherStudent(teacher, studentA, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, studentB, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, studentC, TeacherStudentStatus.ACTIVE);

        studentVolunteerTrackingRepository.save(new StudentVolunteerTracking(
                studentA,
                new BigDecimal("39.90"),
                "below threshold",
                teacher
        ));
        studentVolunteerTrackingRepository.save(new StudentVolunteerTracking(
                studentB,
                new BigDecimal("40.00"),
                "at threshold",
                teacher
        ));

        MvcResult result = mockMvc.perform(post("/api/teacher/students/volunteer-tracking/batch-summary")
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

        assertEquals(39.9, rowA.path("totalVolunteerHours").asDouble(), 0.0001d);
        assertEquals(false, rowA.path("volunteerCompleted").asBoolean());
        assertTrue(!rowA.path("updatedAt").isNull());

        assertEquals(40.0, rowB.path("totalVolunteerHours").asDouble(), 0.0001d);
        assertTrue(rowB.path("volunteerCompleted").asBoolean());
        assertTrue(!rowB.path("updatedAt").isNull());

        assertEquals(0.0, rowC.path("totalVolunteerHours").asDouble(), 0.0001d);
        assertEquals(false, rowC.path("volunteerCompleted").asBoolean());
        assertTrue(rowC.path("updatedAt").isNull());
    }

    @Test
    void batchSummary_permissionChecks_work() throws Exception {
        Teacher teacherA = createTeacherAccount("vol_teacher_batch_scope_a", "Volunteer Batch Scope A");
        Teacher teacherB = createTeacherAccount("vol_teacher_batch_scope_b", "Volunteer Batch Scope B");
        User admin = createAdmin("vol_admin_batch");
        Student student = createStudentAccount("vol_batch_scope_student", "Batch", "Scope", "BS");
        assignTeacherStudent(teacherB, student, TeacherStudentStatus.ACTIVE);

        mockMvc.perform(post("/api/teacher/students/volunteer-tracking/batch-summary")
                        .header("Authorization", bearerFor(teacherA.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBatchSummaryPayload(Arrays.asList(student.getId())))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/teacher/students/volunteer-tracking/batch-summary")
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBatchSummaryPayload(Arrays.asList(student.getId())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(student.getId()));

        mockMvc.perform(post("/api/teacher/students/volunteer-tracking/batch-summary")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBatchSummaryPayload(Arrays.asList(student.getId())))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void batchSummary_validationErrors_return400() throws Exception {
        Teacher teacher = createTeacherAccount("vol_teacher_batch_validate", "Volunteer Batch Validate Teacher");

        mockMvc.perform(post("/api/teacher/students/volunteer-tracking/batch-summary")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("studentIds is required"));

        mockMvc.perform(post("/api/teacher/students/volunteer-tracking/batch-summary")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBatchSummaryPayload(Arrays.asList(0L)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("studentIds must contain positive integers"));

        List<Long> tooManyIds = new ArrayList<Long>();
        for (long i = 1; i <= 101; i++) {
            tooManyIds.add(i);
        }
        mockMvc.perform(post("/api/teacher/students/volunteer-tracking/batch-summary")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBatchSummaryPayload(tooManyIds))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("studentIds size must be <= 100"));
    }

    private Map<String, Object> buildVolunteerPayload(BigDecimal totalHours,
                                                      String note,
                                                      List<Map<String, Object>> tasks) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("totalHours", totalHours);
        payload.put("note", note);
        payload.put("tasks", tasks);
        return payload;
    }

    private Map<String, Object> buildTask(String taskName,
                                          String description,
                                          BigDecimal durationHours,
                                          String startDate,
                                          String endDate,
                                          String verifierContact) {
        Map<String, Object> task = new LinkedHashMap<String, Object>();
        task.put("taskName", taskName);
        task.put("description", description);
        task.put("durationHours", durationHours);
        task.put("startDate", startDate);
        task.put("endDate", endDate);
        task.put("verifierContact", verifierContact);
        return task;
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
        teacherStudentRepository.save(new TeacherStudent(teacher, student, status, "volunteer-tracking-test-assignment"));
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
