package com.studentmanagement.studentmanagementserver.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.SchoolType;
import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserAccountStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.student.StudentProfile;
import com.studentmanagement.studentmanagementserver.domain.student.StudentSchoolRecord;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherStudent;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.domain.volunteer.StudentVolunteerTracking;
import com.studentmanagement.studentmanagementserver.repo.StudentProfileRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentSchoolRecordRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentVolunteerTrackingRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherStudentRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentAccountsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private StudentSchoolRecordRepository studentSchoolRecordRepository;

    @Autowired
    private StudentVolunteerTrackingRepository studentVolunteerTrackingRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TeacherStudentRepository teacherStudentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordPolicyValidator passwordPolicyValidator;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listStudentAccounts_teacherCanAccessAndSeeStatus() throws Exception {
        User teacherOperator = createTeacherUser("student_list_teacher");
        Student assigned = createStudentAccount("student_list_target", "List", "Target", "LT", UserAccountStatus.ACTIVE);
        createStudentAccount("student_list_unassigned", "List", "Unassigned", "LU", UserAccountStatus.ACTIVE);
        assignTeacherStudent(teacherOperator, assigned);

        MvcResult result = mockMvc.perform(get("/api/teacher/student-accounts")
                        .header("Authorization", bearerFor(teacherOperator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        JsonNode assignedRow = findByUsername(data, "student_list_target");
        assertNotNull(assignedRow);
        assertEquals("ACTIVE", assignedRow.path("status").asText());
        assertNull(findByUsername(data, "student_list_unassigned"));
    }

    @Test
    void listStudentAccounts_adminCanAccess() throws Exception {
        User admin = createAdmin("student_list_admin");
        createStudentAccount("student_list_admin_target", "Admin", "Target", "AT", UserAccountStatus.ARCHIVED);

        mockMvc.perform(get("/api/teacher/student-accounts")
                        .header("Authorization", bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].status", hasItem("ARCHIVED")));
    }

    @Test
    void listStudentAccounts_returnsUnifiedLocationFieldsAndVolunteerSummary() throws Exception {
        User teacherOperator = createTeacherUser("student_list_unified_teacher");
        Student student = createStudentAccount("student_list_unified_target", "Unified", "Target", "UT", UserAccountStatus.ACTIVE);
        assignTeacherStudent(teacherOperator, student);

        StudentProfile profile = new StudentProfile(student);
        profile.setEmail("unified.target@example.com");
        profile.setPhone("+1-437-000-0001");
        profile.setStatusInCanada("Study Permit");
        profile.setTeacherNote("student-note");
        profile.setCountry("Canada");
        profile.setState("Ontario");
        profile.setCity("Toronto");
        profile.setServiceItems(Arrays.asList("A: 面试辅导", "SAT全科班"));
        studentProfileRepository.save(profile);

        studentSchoolRecordRepository.save(new StudentSchoolRecord(
                student,
                SchoolType.MAIN,
                "Unified High School",
                "TDSB",
                "123 Street",
                "Toronto",
                "Ontario",
                "Canada",
                "M1M1M1",
                LocalDate.of(2024, 9, 1),
                LocalDate.of(2027, 6, 30)
        ));

        studentVolunteerTrackingRepository.save(new StudentVolunteerTracking(
                student,
                new BigDecimal("40.10"),
                "hours summary",
                null
        ));

        MvcResult result = mockMvc.perform(get("/api/teacher/student-accounts")
                        .header("Authorization", bearerFor(teacherOperator)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        JsonNode row = findByUsername(data, "student_list_unified_target");
        assertNotNull(row);
        assertEquals("UT", row.path("studentName").asText());
        assertEquals("unified.target@example.com", row.path("email").asText());
        assertEquals("+1-437-000-0001", row.path("phone").asText());
        assertEquals("2027-06", row.path("graduation").asText());
        assertEquals("Unified High School", row.path("schoolName").asText());
        assertEquals("Study Permit", row.path("canadaIdentity").asText());
        assertEquals("TDSB", row.path("schoolBoard").asText());
        assertEquals("Canada", row.path("country").asText());
        assertEquals("Ontario", row.path("province").asText());
        assertEquals("Toronto", row.path("city").asText());
        assertEquals(2, row.path("serviceItems").size());
        assertTrue(!row.path("serviceItems").path(0).asText().startsWith("A:"));
        assertTrue(row.path("serviceItems").path(0).asText().length() > 0);
        assertTrue(row.path("serviceItems").path(1).asText().contains("SAT"));
        assertEquals("student-note", row.path("teacherNote").asText());
        assertEquals("ACTIVE", row.path("status").asText());
        assertTrue(row.path("selectable").asBoolean());
        assertEquals(40.1, row.path("totalVolunteerHours").asDouble(), 0.0001d);
        assertTrue(row.path("volunteerCompleted").asBoolean());
    }

    @Test
    void listStudentAccounts_volunteerCompletedThreshold_works() throws Exception {
        User teacherOperator = createTeacherUser("student_list_threshold_teacher");
        Student s399 = createStudentAccount("student_list_threshold_399", "T", "399", "T399", UserAccountStatus.ACTIVE);
        Student s400 = createStudentAccount("student_list_threshold_400", "T", "400", "T400", UserAccountStatus.ACTIVE);
        Student s401 = createStudentAccount("student_list_threshold_401", "T", "401", "T401", UserAccountStatus.ACTIVE);
        assignTeacherStudent(teacherOperator, s399);
        assignTeacherStudent(teacherOperator, s400);
        assignTeacherStudent(teacherOperator, s401);

        studentVolunteerTrackingRepository.save(new StudentVolunteerTracking(
                s399,
                new BigDecimal("39.90"),
                "below",
                null
        ));
        studentVolunteerTrackingRepository.save(new StudentVolunteerTracking(
                s400,
                new BigDecimal("40.00"),
                "equal",
                null
        ));
        studentVolunteerTrackingRepository.save(new StudentVolunteerTracking(
                s401,
                new BigDecimal("40.10"),
                "above",
                null
        ));

        MvcResult result = mockMvc.perform(get("/api/teacher/student-accounts")
                        .header("Authorization", bearerFor(teacherOperator)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");

        JsonNode row399 = findByUsername(data, "student_list_threshold_399");
        JsonNode row400 = findByUsername(data, "student_list_threshold_400");
        JsonNode row401 = findByUsername(data, "student_list_threshold_401");
        assertNotNull(row399);
        assertNotNull(row400);
        assertNotNull(row401);

        assertEquals(39.9, row399.path("totalVolunteerHours").asDouble(), 0.0001d);
        assertEquals(false, row399.path("volunteerCompleted").asBoolean());
        assertEquals(40.0, row400.path("totalVolunteerHours").asDouble(), 0.0001d);
        assertTrue(row400.path("volunteerCompleted").asBoolean());
        assertEquals(40.1, row401.path("totalVolunteerHours").asDouble(), 0.0001d);
        assertTrue(row401.path("volunteerCompleted").asBoolean());
    }

    @Test
    void listStudentAccounts_studentForbidden() throws Exception {
        User studentOperator = createStudentUserOnly("student_list_forbidden");

        mockMvc.perform(get("/api/teacher/student-accounts")
                        .header("Authorization", bearerFor(studentOperator)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Forbidden: teacher/admin role required."))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void listStudentAccounts_unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/teacher/student-accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthenticated."))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void resetStudentPassword_successByTeacher() throws Exception {
        User teacherOperator = createTeacherUser("student_reset_teacher");
        Student student = createStudentAccount("student_reset_target", "Reset", "Student", "RS", UserAccountStatus.ACTIVE);
        assignTeacherStudent(teacherOperator, student);
        User targetUser = student.getUser();
        targetUser.setMustChangePassword(false);
        userRepository.save(targetUser);
        String staleBearer = bearerFor(targetUser);

        MvcResult result = mockMvc.perform(post("/api/teacher/student-accounts/{studentId}/reset-password", student.getId())
                        .header("Authorization", bearerFor(teacherOperator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.username").value("student_reset_target"))
                .andExpect(jsonPath("$.message").value("Password reset successfully"))
                .andExpect(jsonPath("$.tempPassword").isString())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String tempPassword = json.get("tempPassword").asText();
        assertEquals(8, tempPassword.length());
        assertTrue(passwordPolicyValidator.validate(targetUser.getUsername(), tempPassword).isEmpty());

        User updatedUser = userRepository.findById(targetUser.getId())
                .orElseThrow(() -> new RuntimeException("Student user not found"));
        assertTrue(updatedUser.isMustChangePassword());
        assertTrue(passwordEncoder.matches(tempPassword, updatedUser.getPasswordHash()));

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", staleBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"Student!234\",\"newPassword\":\"NewPass!2\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void resetStudentPassword_notFoundReturns404() throws Exception {
        User teacherOperator = createTeacherUser("student_reset_nf_teacher");

        mockMvc.perform(post("/api/teacher/student-accounts/{studentId}/reset-password", 999999L)
                        .header("Authorization", bearerFor(teacherOperator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Student account not found: 999999"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void resetStudentPassword_unassignedTeacherForbidden() throws Exception {
        User teacherOperator = createTeacherUser("student_reset_unassigned_teacher");
        Student student = createStudentAccount("student_reset_unassigned_target", "Reset", "Unassigned", "RU", UserAccountStatus.ACTIVE);

        mockMvc.perform(post("/api/teacher/student-accounts/{studentId}/reset-password", student.getId())
                        .header("Authorization", bearerFor(teacherOperator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Forbidden: student not assigned to current teacher."))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void patchStatus_successByTeacher() throws Exception {
        User teacherOperator = createTeacherUser("student_status_teacher");
        Student student = createStudentAccount("student_status_target", "Status", "Student", "SS", UserAccountStatus.ACTIVE);
        assignTeacherStudent(teacherOperator, student);

        mockMvc.perform(patch("/api/teacher/student-accounts/{studentId}/status", student.getId())
                        .header("Authorization", bearerFor(teacherOperator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.username").value("student_status_target"))
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        User archivedUser = userRepository.findById(student.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Archived student user not found"));
        assertEquals(UserAccountStatus.ARCHIVED, archivedUser.getStatus());

        mockMvc.perform(get("/api/teacher/student-accounts")
                        .header("Authorization", bearerFor(teacherOperator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.username=='student_status_target')].status", hasItem("ARCHIVED")));
    }

    @Test
    void patchStatus_invalidStatusReturns400() throws Exception {
        User admin = createAdmin("student_status_invalid_admin");
        Student student = createStudentAccount("student_status_invalid_target", "Invalid", "Status", "IS", UserAccountStatus.ACTIVE);

        mockMvc.perform(patch("/api/teacher/student-accounts/{studentId}/status", student.getId())
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid account status. Expected ACTIVE or ARCHIVED."))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void patchStatus_unassignedTeacherForbidden() throws Exception {
        User teacherOperator = createTeacherUser("student_status_unassigned_teacher");
        Student student = createStudentAccount("student_status_unassigned_target", "Status", "Unassigned", "SU", UserAccountStatus.ACTIVE);

        mockMvc.perform(patch("/api/teacher/student-accounts/{studentId}/status", student.getId())
                        .header("Authorization", bearerFor(teacherOperator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Forbidden: student not assigned to current teacher."))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void patchStatus_studentForbidden() throws Exception {
        User studentOperator = createStudentUserOnly("student_status_forbidden");
        Student target = createStudentAccount("student_status_forbidden_target", "Forbidden", "Status", "FS", UserAccountStatus.ACTIVE);

        mockMvc.perform(patch("/api/teacher/student-accounts/{studentId}/status", target.getId())
                        .header("Authorization", bearerFor(studentOperator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void patchStatus_notFoundReturns404() throws Exception {
        User admin = createAdmin("student_status_nf_admin");

        mockMvc.perform(patch("/api/teacher/student-accounts/{studentId}/status", 999999L)
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Student account not found: 999999"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void archiveAndEnable_studentLoginFlow_matchesAcceptance() throws Exception {
        User teacherOperator = createTeacherUser("student_flow_teacher");
        Student student = createStudentAccount("student_flow_target", "Flow", "Student", "FlowNick", UserAccountStatus.ACTIVE);
        assignTeacherStudent(teacherOperator, student);

        mockMvc.perform(patch("/api/teacher/student-accounts/{studentId}/status", student.getId())
                        .header("Authorization", bearerFor(teacherOperator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student_flow_target\",\"password\":\"Student!234\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_ARCHIVED"))
                .andExpect(jsonPath("$.message")
                        .value("This account has been archived. Please contact an admin to enable it."));

        mockMvc.perform(patch("/api/teacher/student-accounts/{studentId}/status", student.getId())
                        .header("Authorization", bearerFor(teacherOperator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student_flow_target\",\"password\":\"Student!234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.accessToken").isString());
    }

    private User createAdmin(String username) {
        return userRepository.save(new User(username, passwordEncoder.encode("Admin!234"), UserRole.ADMIN));
    }

    private User createTeacherUser(String username) {
        User user = userRepository.save(new User(username, passwordEncoder.encode("Teacher!234"), UserRole.TEACHER));
        teacherRepository.save(new Teacher(user, username + " Teacher"));
        return user;
    }

    private void assignTeacherStudent(User teacherUser, Student student) {
        Teacher teacher = teacherRepository.findByUser_Id(teacherUser.getId())
                .orElseThrow(() -> new RuntimeException("Teacher record not found"));
        teacherStudentRepository.save(new TeacherStudent(
                teacher,
                student,
                TeacherStudentStatus.ACTIVE,
                "student-accounts-api-test-assignment"
        ));
    }

    private User createStudentUserOnly(String username) {
        return userRepository.save(new User(username, passwordEncoder.encode("Student!234"), UserRole.STUDENT));
    }

    private Student createStudentAccount(String username,
                                         String firstName,
                                         String lastName,
                                         String nickName,
                                         UserAccountStatus status) {
        User user = createStudentUserOnly(username);
        user.updateStatus(status, null);
        user = userRepository.save(user);
        return studentRepository.save(new Student(user, firstName, lastName, nickName));
    }

    private JsonNode findByUsername(JsonNode items, String username) {
        if (items == null || !items.isArray() || username == null) {
            return null;
        }
        for (JsonNode item : items) {
            if (username.equals(item.path("username").asText())) {
                return item;
            }
        }
        return null;
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
