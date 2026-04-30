package com.studentmanagement.studentmanagementserver.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserAccountStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.notification.EmailService;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.student.StudentProfile;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherStudent;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.InfoVolunteerTaskItemRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentProfileRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherStudentRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.info-task.email-reminders.enabled=true")
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
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TeacherStudentRepository teacherStudentRepository;

    @Autowired
    private InfoVolunteerTaskItemRepository infoVolunteerTaskItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailService emailService;

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
                .andExpect(jsonPath("$.goalId").isEmpty())
                .andExpect(jsonPath("$.taskGroupId").isEmpty())
                .andExpect(jsonPath("$.recipientStudentIds.length()").value(2))
                .andExpect(jsonPath("$.recipientStudentIds", hasItem(studentA.getId().intValue())))
                .andExpect(jsonPath("$.recipientStudentIds", hasItem(studentB.getId().intValue())))
                .andExpect(jsonPath("$.targetStudentCount").value(2))
                .andReturn();
        long infoId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(get("/api/teacher/tasks")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .param("type", "INFO")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", hasItem((int) infoId)))
                .andExpect(jsonPath("$.items[0].recipientStudentIds", hasItem(studentA.getId().intValue())))
                .andExpect(jsonPath("$.items[0].recipientStudentIds", hasItem(studentB.getId().intValue())));
    }

    @Test
    void teacherCreateInfo_sendsEmailReminderToStudentProfileEmails() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_email_create", "Info Email Teacher");
        Student studentA = createStudentAccount("info_email_student_a", "Email", "A", "EA");
        Student studentB = createStudentAccount("info_email_student_b", "Email", "B", "EB");
        assignTeacherStudent(teacher, studentA, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, studentB, TeacherStudentStatus.ACTIVE);
        createStudentProfileWithEmail(studentA, "info.student.a@example.com");
        createStudentProfileWithEmail(studentB, "info.student.b@example.com");

        mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInfoPayload(
                                "College checklist",
                                "Please review the new checklist before Friday",
                                "ACTIVITY",
                                Arrays.asList("Checklist"),
                                Arrays.asList(studentA.getId(), studentB.getId())
                        )))
                .andExpect(status().isOk());

        verify(emailService).sendTextEmail(
                argThat((Collection<String> recipients) ->
                        recipients != null
                                && recipients.contains("info.student.a@example.com")
                                && recipients.contains("info.student.b@example.com")),
                argThat((String subject) ->
                        subject != null && subject.contains("College checklist")),
                argThat((String body) ->
                        body != null
                                && body.contains("College checklist")
                                && body.contains("Please review the new checklist")
                                && body.contains("Info Email Teacher"))
        );
    }

    @Test
    void createInfoWithTaskGroupId_sendsEmailOnlyToNewStudentsOnOverwrite() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_email_overwrite", "Info Email Overwrite");
        Student studentA = createStudentAccount("info_email_overwrite_student_a", "Email", "A", "EOA");
        Student studentB = createStudentAccount("info_email_overwrite_student_b", "Email", "B", "EOB");
        Student studentC = createStudentAccount("info_email_overwrite_student_c", "Email", "C", "EOC");
        assignTeacherStudent(teacher, studentA, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, studentB, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, studentC, TeacherStudentStatus.ACTIVE);
        createStudentProfileWithEmail(studentA, "info.overwrite.a@example.com");
        createStudentProfileWithEmail(studentB, "info.overwrite.b@example.com");
        createStudentProfileWithEmail(studentC, "info.overwrite.c@example.com");

        String taskGroupId = "tg-email-overwrite";
        mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInfoPayload(
                                "Task group email v1",
                                "first content",
                                "ACTIVITY",
                                Arrays.asList("Task", "Email"),
                                Arrays.asList(studentA.getId(), studentB.getId()),
                                taskGroupId,
                                null
                        )))
                .andExpect(status().isOk());

        clearInvocations(emailService);

        mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInfoPayload(
                                "Task group email v2",
                                "second content",
                                "ACTIVITY",
                                Arrays.asList("Task", "Email", "Updated"),
                                Arrays.asList(studentB.getId(), studentC.getId()),
                                taskGroupId,
                                null
                        )))
                .andExpect(status().isOk());

        verify(emailService).sendTextEmail(
                argThat((Collection<String> recipients) ->
                        recipients != null
                                && recipients.size() == 1
                                && recipients.contains("info.overwrite.c@example.com")
                                && !recipients.contains("info.overwrite.a@example.com")
                                && !recipients.contains("info.overwrite.b@example.com")),
                argThat((String subject) ->
                        subject != null && subject.contains("Task group email v2")),
                argThat((String body) ->
                        body != null
                                && body.contains("Task group email v2")
                                && body.contains("second content"))
        );
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
    void createInfoWithTaskGroupId_overwritesExistingInfoInsteadOfCreatingNew() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_task_group_upsert", "Info Task Group Upsert");
        Student studentA = createStudentAccount("info_task_group_student_a", "Task", "A", "TA");
        Student studentB = createStudentAccount("info_task_group_student_b", "Task", "B", "TB");
        Student studentC = createStudentAccount("info_task_group_student_c", "Task", "C", "TC");
        assignTeacherStudent(teacher, studentA, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, studentB, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, studentC, TeacherStudentStatus.ACTIVE);

        String taskGroupId = "tg-20260330-001";
        MvcResult firstCreateResult = mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInfoPayload(
                                "Task group update v1",
                                "first content",
                                "ACTIVITY",
                                Arrays.asList("Task", "Group"),
                                Arrays.asList(studentA.getId(), studentB.getId()),
                                taskGroupId,
                                null
                        )))
                .andExpect(status().isOk())
                .andReturn();
        long firstInfoId = objectMapper.readTree(firstCreateResult.getResponse().getContentAsString()).path("id").asLong();

        MvcResult secondCreateResult = mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInfoPayload(
                                "Task group update v2",
                                "second content",
                                "ACTIVITY",
                                Arrays.asList("Task", "Group", "Updated"),
                                Arrays.asList(studentB.getId(), studentC.getId()),
                                taskGroupId,
                                null
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Task group update v2"))
                .andExpect(jsonPath("$.taskGroupId").value(taskGroupId))
                .andExpect(jsonPath("$.recipientStudentIds.length()").value(2))
                .andExpect(jsonPath("$.recipientStudentIds", hasItem(studentB.getId().intValue())))
                .andExpect(jsonPath("$.recipientStudentIds", hasItem(studentC.getId().intValue())))
                .andExpect(jsonPath("$.recipientStudentIds", not(hasItem(studentA.getId().intValue()))))
                .andExpect(jsonPath("$.targetStudentCount").value(2))
                .andReturn();
        long secondInfoId = objectMapper.readTree(secondCreateResult.getResponse().getContentAsString()).path("id").asLong();
        assertEquals(firstInfoId, secondInfoId);

        MvcResult thirdCreateResult = mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInfoPayload(
                                "Task group update v2",
                                "second content",
                                "ACTIVITY",
                                Arrays.asList("Task", "Group", "Updated"),
                                Arrays.asList(studentB.getId(), studentC.getId()),
                                taskGroupId,
                                null
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstInfoId))
                .andExpect(jsonPath("$.recipientStudentIds.length()").value(2))
                .andReturn();
        long thirdInfoId = objectMapper.readTree(thirdCreateResult.getResponse().getContentAsString()).path("id").asLong();
        assertEquals(firstInfoId, thirdInfoId);

        mockMvc.perform(get("/api/teacher/tasks")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .param("type", "INFO")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(firstInfoId))
                .andExpect(jsonPath("$.items[0].title").value("Task group update v2"))
                .andExpect(jsonPath("$.items[0].taskGroupId").value(taskGroupId))
                .andExpect(jsonPath("$.items[0].recipientStudentIds", hasItem(studentB.getId().intValue())))
                .andExpect(jsonPath("$.items[0].recipientStudentIds", hasItem(studentC.getId().intValue())))
                .andExpect(jsonPath("$.items[0].recipientStudentIds", not(hasItem(studentA.getId().intValue()))));

        mockMvc.perform(get("/api/student/tasks")
                        .header("Authorization", bearerFor(studentA.getUser()))
                        .param("type", "INFO")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", not(hasItem((int) firstInfoId))));

        mockMvc.perform(get("/api/student/tasks")
                        .header("Authorization", bearerFor(studentB.getUser()))
                        .param("type", "INFO")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", hasItem((int) firstInfoId)))
                .andExpect(jsonPath("$.items[0].goalId").isEmpty())
                .andExpect(jsonPath("$.items[0].taskGroupId").isEmpty())
                .andExpect(jsonPath("$.items[0].recipientStudentIds.length()").value(0))
                .andExpect(jsonPath("$.items[0].targetStudentCount").value(0))
                .andExpect(jsonPath("$.items[0].volunteer").isEmpty());

        mockMvc.perform(get("/api/student/tasks")
                        .header("Authorization", bearerFor(studentC.getUser()))
                        .param("type", "INFO")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", hasItem((int) firstInfoId)));
    }

    @Test
    void createVolunteerInfoStructured_andQueryReturnsVolunteerPayload() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_vol_struct", "Volunteer Struct Teacher");
        Student student = createStudentAccount("info_vol_struct_student", "Vol", "Student", "VolStu");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        Map<String, Object> volunteer = buildVolunteerPayload(Arrays.asList(
                buildVolunteerTask("校园导览", "新生校园导览", new BigDecimal("2.50"), "2026-03-01", "2026-03-01", "123-456-7890"),
                buildVolunteerTask("社区活动", "社区募捐活动", new BigDecimal("1.50"), "2026-03-05", "2026-03-06", "contact@example.com")
        ));

        MvcResult createResult = mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInfoPayload(
                                "Volunteer structured info",
                                "legacy content remains",
                                "VOLUNTEER",
                                Arrays.asList("Volunteer"),
                                Arrays.asList(student.getId()),
                                null,
                                null,
                                volunteer
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("VOLUNTEER"))
                .andExpect(jsonPath("$.content").value("legacy content remains"))
                .andExpect(jsonPath("$.volunteer.totalHours").value(4.0))
                .andExpect(jsonPath("$.volunteer.tasks.length()").value(2))
                .andExpect(jsonPath("$.volunteer.tasks[0].taskName").value("校园导览"))
                .andExpect(jsonPath("$.volunteer.tasks[0].durationHours").value(2.5))
                .andReturn();
        long infoId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(get("/api/teacher/tasks")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .param("type", "INFO")
                        .param("category", "VOLUNTEER")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(infoId))
                .andExpect(jsonPath("$.items[0].volunteer.totalHours").value(4.0))
                .andExpect(jsonPath("$.items[0].volunteer.tasks.length()").value(2));

        mockMvc.perform(get("/api/student/tasks")
                        .header("Authorization", bearerFor(student.getUser()))
                        .param("type", "INFO")
                        .param("category", "VOLUNTEER")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(infoId))
                .andExpect(jsonPath("$.items[0].volunteer").isEmpty());
    }

    @Test
    void createVolunteerInfoWithEmptyTasks_returns400() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_vol_empty", "Volunteer Empty Teacher");
        Student student = createStudentAccount("info_vol_empty_student", "Vol", "Empty", "VolEmpty");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInfoPayload(
                                "Volunteer invalid",
                                "invalid payload",
                                "VOLUNTEER",
                                Arrays.asList("Volunteer"),
                                Arrays.asList(student.getId()),
                                null,
                                null,
                                buildVolunteerPayload(Arrays.<Map<String, Object>>asList())
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("volunteer.tasks must contain at least one item"));
    }

    @Test
    void createVolunteerInfoWithNegativeDuration_returns400() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_vol_negative", "Volunteer Negative Teacher");
        Student student = createStudentAccount("info_vol_negative_student", "Vol", "Negative", "VolNegative");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        Map<String, Object> volunteer = buildVolunteerPayload(Arrays.asList(
                buildVolunteerTask("Task A", "Desc A", new BigDecimal("-1.00"), "2026-03-01", "2026-03-01", "A-Contact")
        ));

        mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInfoPayload(
                                "Volunteer invalid duration",
                                "invalid payload",
                                "VOLUNTEER",
                                Arrays.asList("Volunteer"),
                                Arrays.asList(student.getId()),
                                null,
                                null,
                                volunteer
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("volunteer.tasks[0].durationHours must be greater than 0"));
    }

    @Test
    void createVolunteerInfoWithEndDateBeforeStartDate_returns400() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_vol_date", "Volunteer Date Teacher");
        Student student = createStudentAccount("info_vol_date_student", "Vol", "Date", "VolDate");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        Map<String, Object> volunteer = buildVolunteerPayload(Arrays.asList(
                buildVolunteerTask("Task A", "Desc A", new BigDecimal("1.00"), "2026-03-02", "2026-03-01", "A-Contact")
        ));

        mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInfoPayload(
                                "Volunteer invalid date",
                                "invalid payload",
                                "VOLUNTEER",
                                Arrays.asList("Volunteer"),
                                Arrays.asList(student.getId()),
                                null,
                                null,
                                volunteer
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("volunteer.tasks[0].endDate must be on or after startDate"));
    }

    @Test
    void createVolunteerInfoWithTaskGroupId_overwriteReplacesVolunteerDetails() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_vol_overwrite", "Volunteer Overwrite Teacher");
        Student student = createStudentAccount("info_vol_overwrite_student", "Vol", "Overwrite", "VolOverwrite");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        String taskGroupId = "vol-tg-20260408-001";
        Map<String, Object> volunteerV1 = buildVolunteerPayload(Arrays.asList(
                buildVolunteerTask("Task V1-A", "Desc V1-A", new BigDecimal("1.25"), "2026-03-01", "2026-03-01", "c1"),
                buildVolunteerTask("Task V1-B", "Desc V1-B", new BigDecimal("2.00"), "2026-03-02", "2026-03-02", "c2")
        ));
        MvcResult firstCreateResult = mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInfoPayload(
                                "Volunteer overwrite v1",
                                "content v1",
                                "VOLUNTEER",
                                Arrays.asList("Volunteer"),
                                Arrays.asList(student.getId()),
                                taskGroupId,
                                null,
                                volunteerV1
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.volunteer.tasks.length()").value(2))
                .andReturn();
        long infoId = objectMapper.readTree(firstCreateResult.getResponse().getContentAsString()).path("id").asLong();
        assertEquals(2, infoVolunteerTaskItemRepository.findByInfoTask_IdOrderByIdAsc(infoId).size());

        Map<String, Object> volunteerV2 = buildVolunteerPayload(Arrays.asList(
                buildVolunteerTask("Task V2-Only", "Desc V2", new BigDecimal("3.50"), "2026-03-03", "2026-03-03", "c3")
        ));
        MvcResult secondCreateResult = mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInfoPayload(
                                "Volunteer overwrite v2",
                                "content v2",
                                "VOLUNTEER",
                                Arrays.asList("Volunteer"),
                                Arrays.asList(student.getId()),
                                taskGroupId,
                                null,
                                volunteerV2
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(infoId))
                .andExpect(jsonPath("$.volunteer.totalHours").value(3.5))
                .andExpect(jsonPath("$.volunteer.tasks.length()").value(1))
                .andExpect(jsonPath("$.volunteer.tasks[0].taskName").value("Task V2-Only"))
                .andReturn();
        long overwrittenInfoId = objectMapper.readTree(secondCreateResult.getResponse().getContentAsString()).path("id").asLong();
        assertEquals(infoId, overwrittenInfoId);

        assertEquals(1, infoVolunteerTaskItemRepository.findByInfoTask_IdOrderByIdAsc(infoId).size());
        assertEquals(
                "Task V2-Only",
                infoVolunteerTaskItemRepository.findByInfoTask_IdOrderByIdAsc(infoId).get(0).getTaskName()
        );
    }

    @Test
    void createVolunteerInfoWithoutStructuredData_remainsBackwardCompatible() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_vol_legacy", "Volunteer Legacy Teacher");
        Student student = createStudentAccount("info_vol_legacy_student", "Vol", "Legacy", "VolLegacy");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        String legacyContent = "义工总时长：3 小时\n义工任务明细：\n任务名称：历史任务";
        long infoId = createInfoAsTeacher(
                teacher.getUser(),
                "Volunteer legacy content only",
                legacyContent,
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
                .andExpect(jsonPath("$.items[0].id").value(infoId))
                .andExpect(jsonPath("$.items[0].content").value(legacyContent))
                .andExpect(jsonPath("$.items[0].volunteer").isEmpty());

        mockMvc.perform(get("/api/student/tasks")
                        .header("Authorization", bearerFor(student.getUser()))
                        .param("type", "INFO")
                        .param("category", "VOLUNTEER")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(infoId))
                .andExpect(jsonPath("$.items[0].content").value(legacyContent))
                .andExpect(jsonPath("$.items[0].volunteer").isEmpty());
    }

    @Test
    void createInfoWithGoalId_overwritesExistingInfoInsteadOfCreatingNew() throws Exception {
        Teacher teacher = createTeacherAccount("info_teacher_goal_upsert", "Info Goal Upsert Teacher");
        Student studentA = createStudentAccount("info_goal_upsert_student_a", "Goal", "A", "GoalA");
        Student studentB = createStudentAccount("info_goal_upsert_student_b", "Goal", "B", "GoalB");
        Student studentC = createStudentAccount("info_goal_upsert_student_c", "Goal", "C", "GoalC");
        assignTeacherStudent(teacher, studentA, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, studentB, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, studentC, TeacherStudentStatus.ACTIVE);

        long goalId = 1001L;
        MvcResult firstCreateResult = mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInfoPayload(
                                "Task update v1",
                                "First content",
                                "ACTIVITY",
                                Arrays.asList("Task", "Update"),
                                Arrays.asList(studentA.getId(), studentB.getId()),
                                null,
                                goalId
                        )))
                .andExpect(status().isOk())
                .andReturn();
        long firstInfoId = objectMapper.readTree(firstCreateResult.getResponse().getContentAsString()).path("id").asLong();

        MvcResult secondCreateResult = mockMvc.perform(post("/api/teacher/tasks/infos")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInfoPayload(
                                "Task update v2",
                                "Second content",
                                "ACTIVITY",
                                Arrays.asList("Task", "Updated"),
                                Arrays.asList(studentB.getId(), studentC.getId()),
                                null,
                                goalId
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Task update v2"))
                .andExpect(jsonPath("$.goalId").value((int) goalId))
                .andExpect(jsonPath("$.recipientStudentIds.length()").value(2))
                .andExpect(jsonPath("$.recipientStudentIds", hasItem(studentB.getId().intValue())))
                .andExpect(jsonPath("$.recipientStudentIds", hasItem(studentC.getId().intValue())))
                .andExpect(jsonPath("$.recipientStudentIds", not(hasItem(studentA.getId().intValue()))))
                .andExpect(jsonPath("$.targetStudentCount").value(2))
                .andReturn();
        long secondInfoId = objectMapper.readTree(secondCreateResult.getResponse().getContentAsString()).path("id").asLong();
        assertEquals(firstInfoId, secondInfoId);

        mockMvc.perform(get("/api/teacher/tasks")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .param("type", "INFO")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(firstInfoId))
                .andExpect(jsonPath("$.items[0].title").value("Task update v2"))
                .andExpect(jsonPath("$.items[0].goalId").value((int) goalId))
                .andExpect(jsonPath("$.items[0].recipientStudentIds", hasItem(studentB.getId().intValue())))
                .andExpect(jsonPath("$.items[0].recipientStudentIds", hasItem(studentC.getId().intValue())))
                .andExpect(jsonPath("$.items[0].recipientStudentIds", not(hasItem(studentA.getId().intValue()))));

        mockMvc.perform(get("/api/student/tasks")
                        .header("Authorization", bearerFor(studentA.getUser()))
                        .param("type", "INFO")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", not(hasItem((int) firstInfoId))));

        mockMvc.perform(get("/api/student/tasks")
                        .header("Authorization", bearerFor(studentB.getUser()))
                        .param("type", "INFO")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", hasItem((int) firstInfoId)))
                .andExpect(jsonPath("$.items[0].goalId").isEmpty())
                .andExpect(jsonPath("$.items[0].taskGroupId").isEmpty())
                .andExpect(jsonPath("$.items[0].recipientStudentIds.length()").value(0))
                .andExpect(jsonPath("$.items[0].targetStudentCount").value(0))
                .andExpect(jsonPath("$.items[0].volunteer").isEmpty());

        mockMvc.perform(get("/api/student/tasks")
                        .header("Authorization", bearerFor(studentC.getUser()))
                        .param("type", "INFO")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", hasItem((int) firstInfoId)));
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
        return createInfoPayload(title, content, category, tags, studentIds, null, null, null);
    }

    private String createInfoPayload(String title,
                                     String content,
                                     String category,
                                     List<String> tags,
                                     List<Long> studentIds,
                                     Long goalId) throws Exception {
        return createInfoPayload(title, content, category, tags, studentIds, null, goalId, null);
    }

    private String createInfoPayload(String title,
                                     String content,
                                     String category,
                                     List<String> tags,
                                     List<Long> studentIds,
                                     String taskGroupId,
                                     Long goalId) throws Exception {
        return createInfoPayload(title, content, category, tags, studentIds, taskGroupId, goalId, null);
    }

    private String createInfoPayload(String title,
                                     String content,
                                     String category,
                                     List<String> tags,
                                     List<Long> studentIds,
                                     String taskGroupId,
                                     Long goalId,
                                     Map<String, Object> volunteer) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("title", title);
        payload.put("content", content);
        payload.put("category", category);
        payload.put("tags", tags);
        payload.put("studentIds", studentIds);
        if (taskGroupId != null) {
            payload.put("taskGroupId", taskGroupId);
        }
        if (goalId != null) {
            payload.put("goalId", goalId);
        }
        if (volunteer != null) {
            payload.put("volunteer", volunteer);
        }
        return objectMapper.writeValueAsString(payload);
    }

    private Map<String, Object> buildVolunteerPayload(List<Map<String, Object>> tasks) {
        Map<String, Object> volunteer = new LinkedHashMap<String, Object>();
        volunteer.put("tasks", tasks);
        return volunteer;
    }

    private Map<String, Object> buildVolunteerTask(String taskName,
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

    private void createStudentProfileWithEmail(Student student, String email) {
        StudentProfile profile = new StudentProfile(student);
        profile.setEmail(email);
        studentProfileRepository.save(profile);
    }

    private void assignTeacherStudent(Teacher teacher, Student student, TeacherStudentStatus status) {
        teacherStudentRepository.save(new TeacherStudent(teacher, student, status, "task-center-info-dll-test-assignment"));
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
