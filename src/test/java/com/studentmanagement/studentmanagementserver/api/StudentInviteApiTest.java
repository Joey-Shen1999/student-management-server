package com.studentmanagement.studentmanagementserver.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.student.StudentInvite;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentInviteRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import com.studentmanagement.studentmanagementserver.service.TeacherBindingBackfillService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentInviteApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentInviteRepository studentInviteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TeacherBindingBackfillService teacherBindingBackfillService;

    @Test
    void generateInvite_teacher_success_andPreviewValid() throws Exception {
        Teacher teacher = createTeacherAccount("invite_teacher_01", "Invite Teacher 01");

        MvcResult createResult = mockMvc.perform(post("/api/teacher/student-invites")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inviteToken").isString())
                .andExpect(jsonPath("$.inviteUrl").isString())
                .andExpect(jsonPath("$.expiresAt").isString())
                .andReturn();

        String token = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("inviteToken").asText();
        assertTrue(token.length() >= 32);

        mockMvc.perform(get("/api/auth/student-invites/{inviteToken}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.teacherName").value("Invite Teacher 01"))
                .andExpect(jsonPath("$.expiresAt").isString());
    }

    @Test
    void generateInvite_adminWithTeacherId_success() throws Exception {
        User admin = createAdmin("invite_admin_01");
        Teacher teacher = createTeacherAccount("invite_teacher_02", "Invite Teacher 02");

        mockMvc.perform(post("/api/teacher/student-invites")
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teacherId\":" + teacher.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inviteToken").isString())
                .andExpect(jsonPath("$.expiresAt").isString());
    }

    @Test
    void generateInvite_adminWithoutTeacherId_defaultsToOwnTeacherId() throws Exception {
        User admin = createAdmin("invite_admin_02");
        Teacher adminTeacher = teacherRepository.save(new Teacher(admin, "Admin Bound Teacher"));

        MvcResult result = mockMvc.perform(post("/api/teacher/student-invites")
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("inviteToken").asText();
        StudentInvite invite = studentInviteRepository.findByInviteTokenWithTeacher(token)
                .orElseThrow(() -> new RuntimeException("Invite not found"));
        assertEquals(adminTeacher.getId(), invite.getTeacher().getId());
    }

    @Test
    void generateInvite_studentForbidden_andUnauthenticated401() throws Exception {
        User student = createStudentUserOnly("invite_student_forbidden");

        mockMvc.perform(post("/api/teacher/student-invites")
                        .header("Authorization", bearerFor(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/teacher/student-invites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void generateInvite_teacherIgnoresTeacherIdInBody_adminUsesBodyTeacherId() throws Exception {
        Teacher teacherCaller = createTeacherAccount("invite_teacher_caller", "Invite Teacher Caller");
        Teacher teacherFromBody = createTeacherAccount("invite_teacher_body", "Invite Teacher Body");

        MvcResult teacherCall = mockMvc.perform(post("/api/teacher/student-invites")
                        .header("Authorization", bearerFor(teacherCaller.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teacherId\":" + teacherFromBody.getId() + "}"))
                .andExpect(status().isOk())
                .andReturn();
        String teacherToken = objectMapper.readTree(teacherCall.getResponse().getContentAsString())
                .get("inviteToken").asText();
        StudentInvite teacherInvite = studentInviteRepository.findByInviteTokenWithTeacher(teacherToken)
                .orElseThrow(() -> new RuntimeException("Teacher invite not found"));
        assertEquals(teacherCaller.getId(), teacherInvite.getTeacher().getId());

        User admin = createAdmin("invite_admin_body_teacher");
        teacherRepository.save(new Teacher(admin, "Admin Teacher Self"));

        MvcResult adminCall = mockMvc.perform(post("/api/teacher/student-invites")
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teacherId\":" + teacherFromBody.getId() + "}"))
                .andExpect(status().isOk())
                .andReturn();
        String adminToken = objectMapper.readTree(adminCall.getResponse().getContentAsString())
                .get("inviteToken").asText();
        StudentInvite adminInvite = studentInviteRepository.findByInviteTokenWithTeacher(adminToken)
                .orElseThrow(() -> new RuntimeException("Admin invite not found"));
        assertEquals(teacherFromBody.getId(), adminInvite.getTeacher().getId());
    }

    @Test
    void generateInvite_adminWithoutTeacherBinding_returnsTeacherBindingRequired() throws Exception {
        User admin = createAdmin("invite_admin_no_binding");

        mockMvc.perform(post("/api/teacher/student-invites")
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TEACHER_BINDING_REQUIRED"));
    }

    @Test
    void generateInvite_adminWithoutTeacherBinding_canCreateAfterBackfill() throws Exception {
        User admin = createAdmin("invite_admin_backfill_invite");
        TeacherBindingBackfillService.BackfillResult result = teacherBindingBackfillService.backfillMissingTeacherBindings();
        assertTrue(result.getInserted() >= 1);

        MvcResult createResult = mockMvc.perform(post("/api/teacher/student-invites")
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("inviteToken").asText();
        StudentInvite invite = studentInviteRepository.findByInviteTokenWithTeacher(token)
                .orElseThrow(() -> new RuntimeException("Invite not found"));
        Teacher adminTeacher = teacherRepository.findByUser_Id(admin.getId())
                .orElseThrow(() -> new RuntimeException("Teacher binding not found"));
        assertEquals(adminTeacher.getId(), invite.getTeacher().getId());
    }

    @Test
    void registerWithInvite_consumesOnce_andBindsTeacher() throws Exception {
        Teacher teacher = createTeacherAccount("invite_teacher_bind", "Invite Teacher Bind");
        String token = createInviteTokenAs(teacher.getUser(), "{}");

        String registerBody = "{"
                + "\"username\":\"invited_student_01\","
                + "\"password\":\"Student!234\","
                + "\"role\":\"STUDENT\","
                + "\"firstName\":\"Invited\","
                + "\"lastName\":\"Student\","
                + "\"inviteToken\":\"" + token + "\""
                + "}";

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.studentId").isNumber())
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        Long studentId = registerJson.get("studentId").asLong();

        Student student = studentRepository.findByIdWithTeacher(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        assertNotNull(student.getTeacher());
        assertEquals(teacher.getId(), student.getTeacher().getId());

        mockMvc.perform(get("/api/auth/student-invites/{inviteToken}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.status").value("USED"));

        String secondBody = "{"
                + "\"username\":\"invited_student_02\","
                + "\"password\":\"Student!234\","
                + "\"role\":\"STUDENT\","
                + "\"firstName\":\"Another\","
                + "\"lastName\":\"Student\","
                + "\"inviteToken\":\"" + token + "\""
                + "}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVITE_USED"));
    }

    @Test
    void registerWithInvite_concurrentConsume_onlyOneSuccess() throws Exception {
        Teacher teacher = createTeacherAccount("invite_teacher_concurrent", "Invite Teacher Concurrent");
        final String token = createInviteTokenAs(teacher.getUser(), "{}");
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Integer> registerTask1 = createConcurrentRegisterTask("invite_concurrent_u1", token, ready, start);
        Callable<Integer> registerTask2 = createConcurrentRegisterTask("invite_concurrent_u2", token, ready, start);

        Future<Integer> f1 = executor.submit(registerTask1);
        Future<Integer> f2 = executor.submit(registerTask2);
        ready.await(3, TimeUnit.SECONDS);
        start.countDown();

        int s1 = f1.get(10, TimeUnit.SECONDS);
        int s2 = f2.get(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        int successCount = (s1 == 200 ? 1 : 0) + (s2 == 200 ? 1 : 0);
        int badRequestCount = (s1 == 400 ? 1 : 0) + (s2 == 400 ? 1 : 0);
        assertEquals(1, successCount);
        assertEquals(1, badRequestCount);
    }

    @Test
    void registerWithInvite_roleMismatch_returnsInviteRoleMismatch() throws Exception {
        Teacher teacher = createTeacherAccount("invite_teacher_role", "Invite Teacher Role");
        String token = createInviteTokenAs(teacher.getUser(), "{}");

        String body = "{"
                + "\"username\":\"invite_role_mismatch_teacher\","
                + "\"password\":\"Teacher!234\","
                + "\"role\":\"TEACHER\","
                + "\"displayName\":\"Role Mismatch\","
                + "\"inviteToken\":\"" + token + "\""
                + "}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVITE_ROLE_MISMATCH"));
    }

    @Test
    void registerWithInvite_invalidAndExpiredCodes() throws Exception {
        String notFoundBody = "{"
                + "\"username\":\"invite_not_found_student\","
                + "\"password\":\"Student!234\","
                + "\"role\":\"STUDENT\","
                + "\"firstName\":\"No\","
                + "\"lastName\":\"Token\","
                + "\"inviteToken\":\"not_exists_token\""
                + "}";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notFoundBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVITE_NOT_FOUND"));

        Teacher teacher = createTeacherAccount("invite_teacher_expired", "Invite Teacher Expired");
        String token = createInviteTokenAs(teacher.getUser(), "{\"expiresInHours\":1}");
        StudentInvite invite = studentInviteRepository.findByInviteTokenWithTeacher(token)
                .orElseThrow(() -> new RuntimeException("Invite not found after create"));
        ReflectionTestUtils.setField(invite, "expiresAt", LocalDateTime.now().minusMinutes(1));
        studentInviteRepository.save(invite);

        String expiredBody = "{"
                + "\"username\":\"invite_expired_student\","
                + "\"password\":\"Student!234\","
                + "\"role\":\"STUDENT\","
                + "\"firstName\":\"Expired\","
                + "\"lastName\":\"Token\","
                + "\"inviteToken\":\"" + token + "\""
                + "}";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expiredBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVITE_EXPIRED"));

        mockMvc.perform(get("/api/auth/student-invites/{inviteToken}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.status").value("EXPIRED"));
    }

    @Test
    void previewInvalidToken_returnsInvalidResponse() throws Exception {
        mockMvc.perform(get("/api/auth/student-invites/{inviteToken}", "missing_token_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.status").value("INVALID"));
    }

    @Test
    void adminLogin_returnsTeacherId_afterBackfill() throws Exception {
        User admin = createAdmin("invite_admin_login_backfill");
        TeacherBindingBackfillService.BackfillResult result = teacherBindingBackfillService.backfillMissingTeacherBindings();
        assertTrue(result.getInserted() >= 1);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"invite_admin_login_backfill\",\"password\":\"Admin!234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.teacherId").isNumber());
    }

    private User createAdmin(String username) {
        return userRepository.save(new User(username, passwordEncoder.encode("Admin!234"), UserRole.ADMIN));
    }

    private User createStudentUserOnly(String username) {
        return userRepository.save(new User(username, passwordEncoder.encode("Student!234"), UserRole.STUDENT));
    }

    private Teacher createTeacherAccount(String username, String displayName) {
        User user = userRepository.save(new User(username, passwordEncoder.encode("Teacher!234"), UserRole.TEACHER));
        return teacherRepository.save(new Teacher(user, displayName));
    }

    private String createInviteTokenAs(User operator, String requestBody) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teacher/student-invites")
                        .header("Authorization", bearerFor(operator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("inviteToken").asText();
    }

    private Callable<Integer> createConcurrentRegisterTask(final String username,
                                                           final String token,
                                                           final CountDownLatch ready,
                                                           final CountDownLatch start) {
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                ready.countDown();
                start.await(3, TimeUnit.SECONDS);
                String body = "{"
                        + "\"username\":\"" + username + "\","
                        + "\"password\":\"Student!234\","
                        + "\"role\":\"STUDENT\","
                        + "\"firstName\":\"Concurrent\","
                        + "\"lastName\":\"Student\","
                        + "\"inviteToken\":\"" + token + "\""
                        + "}";
                return mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            }
        };
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
