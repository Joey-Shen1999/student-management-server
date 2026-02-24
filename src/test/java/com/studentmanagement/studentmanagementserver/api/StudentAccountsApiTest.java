package com.studentmanagement.studentmanagementserver.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.UserAccountStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
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

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        createStudentAccount("student_list_target", "List", "Target", "LT", UserAccountStatus.ACTIVE);

        mockMvc.perform(get("/api/teacher/student-accounts")
                        .header("Authorization", bearerFor(teacherOperator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].username", hasItem("student_list_target")))
                .andExpect(jsonPath("$.data[*].status", hasItem("ACTIVE")));
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
    void patchStatus_successByTeacher() throws Exception {
        User teacherOperator = createTeacherUser("student_status_teacher");
        Student student = createStudentAccount("student_status_target", "Status", "Student", "SS", UserAccountStatus.ACTIVE);

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
        return userRepository.save(new User(username, passwordEncoder.encode("Teacher!234"), UserRole.TEACHER));
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

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
