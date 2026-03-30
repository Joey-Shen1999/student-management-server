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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskCenterInfoDllApiTest {

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
    void teacherCreateInfoAndList_success() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_create", "Info Teacher");
        Student studentA = createStudentAccount("info_student_a", "A", "One", "A1");
        Student studentB = createStudentAccount("info_student_b", "B", "Two", "B2");
        assignTeacherStudent(teacher, studentA, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, studentB, TeacherStudentStatus.ACTIVE);

        MvcResult createResult = mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInfoPayload(
                                "Volunteer signup notice",
                                "Please submit before Friday",
                                "VOLUNTEER",
                                Arrays.asList("Volunteer", "Grade12"),
                                Arrays.asList(studentA.getId(), studentB.getId())
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("INFO"))
                .andExpect(jsonPath("$.title").value("Volunteer signup notice"))
                .andExpect(jsonPath("$.category").value("VOLUNTEER"))
                .andExpect(jsonPath("$.tags.length()").value(2))
                .andExpect(jsonPath("$.targetStudentCount").value(2))
                .andReturn();
        long infoId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(get("/api/teacher/tasks")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .param("type", "INFO")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", hasItem((int) infoId)));
    }

    @Test
    void createInfoDeduplicatesStudentIdsAndCountsTargets() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_dedupe", "Info Dedupe Teacher");
        Student studentA = createStudentAccount("info_dedupe_student_a", "Ded", "A", "DA");
        Student studentB = createStudentAccount("info_dedupe_student_b", "Ded", "B", "DB");
        assignTeacherStudent(teacher, studentA, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, studentB, TeacherStudentStatus.ACTIVE);

        MvcResult createResult = mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInfoPayload(
                                "Duplicate targets",
                                "Only unique students should be targeted",
                                "ACTIVITY",
                                Arrays.asList("Tag1"),
                                Arrays.asList(studentA.getId(), studentA.getId(), studentB.getId())
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetStudentCount").value(2))
                .andReturn();
        long infoId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(get("/api/student/tasks")
                        .header("Authorization", bearerFor(studentA.getUser()))
                        .param("type", "INFO")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", hasItem((int) infoId)));
        mockMvc.perform(get("/api/student/tasks")
                        .header("Authorization", bearerFor(studentB.getUser()))
                        .param("type", "INFO")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", hasItem((int) infoId)));
    }

    @Test
    void createInfoWithoutStudentIds_returns400() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_missing_ids", "Missing Ids Teacher");

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("title", "Missing targets");
        payload.put("content", "No students selected");
        payload.put("category", "ACTIVITY");
        payload.put("tags", Arrays.asList("Notice"));

        mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("studentIds is required"));
    }

    @Test
    void createInfoWithInvalidStudentIds_returns400() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_invalid_ids", "Invalid Ids Teacher");

        mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInfoPayload(
                                "Invalid targets",
                                "contains invalid id",
                                "ACTIVITY",
                                Arrays.asList("Notice"),
                                Arrays.asList(0L, -1L)
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("studentIds must contain positive integers"));
    }

    @Test
    void createInfoForUnassignableStudent_returns400() throws Exception {
        Teacher teacherA = createTeacherAccount("info_teacher_scope_a", "Scope A");
        Teacher teacherB = createTeacherAccount("info_teacher_scope_b", "Scope B");
        Student student = createStudentAccount("info_scope_student", "Scope", "Student", "Scope");
        assignTeacherStudent(teacherB, student, TeacherStudentStatus.ACTIVE);

        mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacherA.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInfoPayload(
                                "Out of scope",
                                "teacher cannot target this student",
                                "ACTIVITY",
                                Arrays.asList("Scope"),
                                Arrays.asList(student.getId())
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STUDENT_NOT_ASSIGNABLE"));
    }

    @Test
    void createInfoForArchivedStudent_returns400() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_archived", "Archived Teacher");
        Student student = createStudentAccount("info_archived_student", "Archived", "Student", "Archived");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        student.getUser().updateStatus(UserAccountStatus.ARCHIVED, teacher.getUser().getId());
        userRepository.save(student.getUser());

        mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInfoPayload(
                                "Archived target",
                                "archived students are invalid targets",
                                "ACTIVITY",
                                Arrays.asList("Archive"),
                                Arrays.asList(student.getId())
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STUDENT_ARCHIVED"));
    }

    @Test
    void studentListInfoAndMarkRead_success() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_read", "Info Read Teacher");
        Student student = createStudentAccount("info_student_read", "Read", "Student", "Reader");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        long infoId = createInfoAsTeacher(
                teacher.getUser(),
                "Open day signup",
                "Please signup this week",
                "ACTIVITY",
                Arrays.asList("OpenDay"),
                Arrays.asList(student.getId())
        );

        mockMvc.perform(get("/api/student/tasks")
                        .header("Authorization", bearerFor(student.getUser()))
                        .param("type", "INFO")
                        .param("unreadOnly", "true")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", hasItem((int) infoId)))
                .andExpect(jsonPath("$.items[0].read").value(false));

        mockMvc.perform(patch("/api/student/tasks/{infoId}/read", infoId)
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(infoId))
                .andExpect(jsonPath("$.read").value(true))
                .andExpect(jsonPath("$.readAt").isNotEmpty());

        mockMvc.perform(get("/api/student/tasks")
                        .header("Authorization", bearerFor(student.getUser()))
                        .param("type", "INFO")
                        .param("unreadOnly", "true")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", not(hasItem((int) infoId))));
    }

    @Test
    void nonTargetStudentCannotSeeOrMarkInfoRead() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_target_only", "Target Teacher");
        Student targetStudent = createStudentAccount("info_target_student", "Target", "Student", "Target");
        Student otherStudent = createStudentAccount("info_other_student", "Other", "Student", "Other");
        assignTeacherStudent(teacher, targetStudent, TeacherStudentStatus.ACTIVE);

        long infoId = createInfoAsTeacher(
                teacher.getUser(),
                "Target only",
                "Only one student should receive this",
                "ACTIVITY",
                Arrays.asList("Targeted"),
                Arrays.asList(targetStudent.getId())
        );

        mockMvc.perform(get("/api/student/tasks")
                        .header("Authorization", bearerFor(otherStudent.getUser()))
                        .param("type", "INFO")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", not(hasItem((int) infoId))));

        mockMvc.perform(patch("/api/student/tasks/{infoId}/read", infoId)
                        .header("Authorization", bearerFor(otherStudent.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void infoFilters_categoryTagKeyword_work() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_filter", "Info Filter Teacher");
        Student student = createStudentAccount("info_student_filter", "Filter", "Student", "Filter");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        createInfoAsTeacher(
                teacher.getUser(),
                "Activity notice",
                "University open day arrangement",
                "ACTIVITY",
                Arrays.asList("OpenDay", "Grade12"),
                Arrays.asList(student.getId())
        );
        createInfoAsTeacher(
                teacher.getUser(),
                "Volunteer reminder",
                "Submit volunteer form",
                "VOLUNTEER",
                Arrays.asList("Volunteer"),
                Arrays.asList(student.getId())
        );

        mockMvc.perform(get("/api/teacher/tasks")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .param("type", "INFO")
                        .param("category", "VOLUNTEER")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].category").value("VOLUNTEER"));

        mockMvc.perform(get("/api/teacher/tasks")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .param("type", "INFO")
                        .param("tag", "OpenDay")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Activity notice"));

        mockMvc.perform(get("/api/teacher/tasks")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .param("type", "INFO")
                        .param("keyword", "volunteer")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Volunteer reminder"));
    }

    @Test
    void teacherCreateDllTemplateInstantiateAndList_success() throws Exception {
        Teacher teacher = createTeacherAccount("dll_teacher_create", "DLL Teacher");
        Student student = createStudentAccount("dll_student_create", "DLL", "Student", "DLL");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        MvcResult templateResult = mockMvc.perform(post("/api/teacher/tasks/dll-templates")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"OUAC package template\"," +
                                "\"description\":\"Application package\"," +
                                "\"payloadSchema\":\"{\\\"fields\\\":[\\\"deadline\\\"]}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("OUAC package template"))
                .andReturn();
        long templateId = objectMapper.readTree(templateResult.getResponse().getContentAsString()).path("id").asLong();

        MvcResult instantiateResult = mockMvc.perform(post("/api/teacher/tasks/dll-templates/{templateId}/instantiate", templateId)
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedStudentId\":" + student.getId() + "," +
                                "\"title\":\"OUAC package task\"," +
                                "\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateId").value(templateId))
                .andExpect(jsonPath("$.assignedStudentId").value(student.getId()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andReturn();
        long dllTaskId = objectMapper.readTree(instantiateResult.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(get("/api/teacher/tasks")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .param("type", "DLL")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", hasItem((int) dllTaskId)));
    }

    @Test
    void teacherCannotInstantiateOtherTeacherTemplate_returns403() throws Exception {
        Teacher teacherA = createTeacherAccount("dll_teacher_a", "DLL Teacher A");
        Teacher teacherB = createTeacherAccount("dll_teacher_b", "DLL Teacher B");
        Student student = createStudentAccount("dll_student_other_template", "DLL", "Target", "Target");
        assignTeacherStudent(teacherA, student, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacherB, student, TeacherStudentStatus.ACTIVE);

        MvcResult templateResult = mockMvc.perform(post("/api/teacher/tasks/dll-templates")
                        .header("Authorization", bearerFor(teacherA.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Template A\",\"description\":\"A\",\"payloadSchema\":\"{}\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long templateId = objectMapper.readTree(templateResult.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(post("/api/teacher/tasks/dll-templates/{templateId}/instantiate", templateId)
                        .header("Authorization", bearerFor(teacherB.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedStudentId\":" + student.getId() + "}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminCanListInfoAndDllTasks_globally() throws Exception {
        User admin = createAdmin("info_dll_admin");
        Teacher teacher = createTeacherAccount("info_dll_teacher", "Info DLL Teacher");
        Student student = createStudentAccount("info_dll_student", "Global", "Student", "Global");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        long infoId = createInfoAsTeacher(
                teacher.getUser(),
                "Global info",
                "Admin should see this",
                "ACTIVITY",
                Arrays.asList("Global"),
                Arrays.asList(student.getId())
        );

        MvcResult templateResult = mockMvc.perform(post("/api/teacher/tasks/dll-templates")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Global DLL template\",\"description\":\"d\",\"payloadSchema\":\"{}\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long templateId = objectMapper.readTree(templateResult.getResponse().getContentAsString()).path("id").asLong();
        long dllTaskId = createDllTaskAsTeacher(teacher.getUser(), templateId, student.getId());

        mockMvc.perform(get("/api/teacher/tasks")
                        .header("Authorization", bearerFor(admin))
                        .param("type", "INFO")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", hasItem((int) infoId)));

        mockMvc.perform(get("/api/teacher/tasks")
                        .header("Authorization", bearerFor(admin))
                        .param("type", "DLL")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", hasItem((int) dllTaskId)));
    }

    private long createInfoAsTeacher(User teacherUser,
                                     String title,
                                     String content,
                                     String category,
                                     List<String> tags,
                                     List<Long> studentIds) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInfoPayload(title, content, category, tags, studentIds)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("id").asLong();
    }

    private String createInfoPayload(String title,
                                     String content,
                                     String category,
                                     List<String> tags,
                                     List<Long> studentIds) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("title", title);
        payload.put("content", content);
        payload.put("category", category);
        payload.put("tags", tags);
        payload.put("studentIds", studentIds);
        return objectMapper.writeValueAsString(payload);
    }

    private long createDllTaskAsTeacher(User teacherUser, long templateId, long studentId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teacher/tasks/dll-templates/{templateId}/instantiate", templateId)
                        .header("Authorization", bearerFor(teacherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedStudentId\":" + studentId + "}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
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
        teacherStudentRepository.save(new TeacherStudent(teacher, student, status, "task-center-info-dll-test-assignment"));
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
