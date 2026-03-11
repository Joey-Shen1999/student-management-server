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

import static org.hamcrest.Matchers.hasItem;
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
                        .content("{\"title\":\"义工活动报名通知\"," +
                                "\"content\":\"请在周五前完成报名。\"," +
                                "\"category\":\"VOLUNTEER\"," +
                                "\"tags\":[\"Volunteer\",\"Grade12\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("INFO"))
                .andExpect(jsonPath("$.title").value("义工活动报名通知"))
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
    void studentListInfoAndMarkRead_success() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_read", "Info Read Teacher");
        Student student = createStudentAccount("info_student_read", "Read", "Student", "Reader");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        long infoId = createInfoAsTeacher(teacher.getUser(), "开放日报名", "本周报名", "ACTIVITY", "[\"OpenDay\"]");

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
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void infoFilters_categoryTagKeyword_work() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_filter", "Info Filter Teacher");
        Student student = createStudentAccount("info_student_filter", "Filter", "Student", "Filter");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        createInfoAsTeacher(teacher.getUser(), "活动通知", "大学开放日安排", "ACTIVITY", "[\"OpenDay\",\"Grade12\"]");
        createInfoAsTeacher(teacher.getUser(), "义工提醒", "提交义工表单", "VOLUNTEER", "[\"Volunteer\"]");

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
                .andExpect(jsonPath("$.items[0].title").value("活动通知"));

        mockMvc.perform(get("/api/teacher/tasks")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .param("type", "INFO")
                        .param("keyword", "义工")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("义工提醒"));
    }

    @Test
    void studentCannotMarkUnassignedInfoRead_returns404() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_own", "Info Own Teacher");
        Student owner = createStudentAccount("info_student_owner", "Owner", "One", "Owner");
        Student other = createStudentAccount("info_student_other", "Other", "Two", "Other");
        assignTeacherStudent(teacher, owner, TeacherStudentStatus.ACTIVE);
        long infoId = createInfoAsTeacher(teacher.getUser(), "仅目标学生可见", "测试", "ACTIVITY", "[]");

        mockMvc.perform(patch("/api/student/tasks/{infoId}/read", infoId)
                        .header("Authorization", bearerFor(other.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void teacherCreateDllTemplateInstantiateAndList_success() throws Exception {
        Teacher teacher = createTeacherAccount("dll_teacher_create", "DLL Teacher");
        Student student = createStudentAccount("dll_student_create", "DLL", "Student", "DLL");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        MvcResult templateResult = mockMvc.perform(post("/api/teacher/tasks/dll-templates")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"OUAC 材料包模板\"," +
                                "\"description\":\"申请材料打包\"," +
                                "\"payloadSchema\":\"{\\\"fields\\\":[\\\"deadline\\\"]}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("OUAC 材料包模板"))
                .andReturn();
        long templateId = objectMapper.readTree(templateResult.getResponse().getContentAsString()).path("id").asLong();

        MvcResult instantiateResult = mockMvc.perform(post("/api/teacher/tasks/dll-templates/{templateId}/instantiate", templateId)
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedStudentId\":" + student.getId() + "," +
                                "\"title\":\"OUAC 材料任务\"," +
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
                        .content("{\"name\":\"模板A\",\"description\":\"A\",\"payloadSchema\":\"{}\"}"))
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
        long infoId = createInfoAsTeacher(teacher.getUser(), "全局信息", "管理员应可见", "ACTIVITY", "[]");

        MvcResult templateResult = mockMvc.perform(post("/api/teacher/tasks/dll-templates")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"全局DLL模板\",\"description\":\"d\",\"payloadSchema\":\"{}\"}"))
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
                                     String tagsJsonArray) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"," +
                                "\"content\":\"" + content + "\"," +
                                "\"category\":\"" + category + "\"," +
                                "\"tags\":" + tagsJsonArray + "}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("id").asLong();
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
