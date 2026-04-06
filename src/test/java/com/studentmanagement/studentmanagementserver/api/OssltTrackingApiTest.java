package com.studentmanagement.studentmanagementserver.api;

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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OssltTrackingApiTest {

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
    void teacherGetOssltModule_defaultState_success() throws Exception {
        Teacher teacher = createTeacherAccount("osslt_teacher_default", "OSSLT Teacher Default");
        Student student = createStudentAccount("osslt_student_default", "OSSLT", "Default", "Default");
        assignTeacherStudent(teacher, student);

        mockMvc.perform(get("/api/teacher/students/{studentId}/osslt-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.latestOssltResult").value("UNKNOWN"))
                .andExpect(jsonPath("$.latestOssltDate").value(nullValue()))
                .andExpect(jsonPath("$.hasOsslc").value(nullValue()))
                .andExpect(jsonPath("$.ossltTrackingManualStatus").value(nullValue()))
                .andExpect(jsonPath("$.ossltTrackingStatus").value("WAITING_UPDATE"));
    }

    @Test
    void teacherUpdateOssltModule_manualOverrideAndClear_success() throws Exception {
        Teacher teacher = createTeacherAccount("osslt_teacher_manual", "OSSLT Teacher Manual");
        Student student = createStudentAccount("osslt_student_manual", "OSSLT", "Manual", "Manual");
        assignTeacherStudent(teacher, student);

        Map<String, Object> updateWithManual = new LinkedHashMap<String, Object>();
        updateWithManual.put("latestOssltResult", "PASS");
        updateWithManual.put("latestOssltDate", "2026-03-20");
        updateWithManual.put("ossltTrackingManualStatus", "NEEDS_TRACKING");
        updateWithManual.put("teacherNote", "Need counselor confirmation");

        mockMvc.perform(put("/api/teacher/students/{studentId}/osslt-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateWithManual)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestOssltResult").value("PASS"))
                .andExpect(jsonPath("$.latestOssltDate").value("2026-03-20"))
                .andExpect(jsonPath("$.ossltTrackingManualStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.ossltTrackingStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.teacherNote").doesNotExist());

        Map<String, Object> clearManual = new LinkedHashMap<String, Object>();
        clearManual.put("ossltTrackingManualStatus", "");

        mockMvc.perform(put("/api/teacher/students/{studentId}/osslt-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clearManual)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestOssltResult").value("PASS"))
                .andExpect(jsonPath("$.ossltTrackingManualStatus").value(nullValue()))
                .andExpect(jsonPath("$.ossltTrackingStatus").value("PASSED"));
    }

    @Test
    void teacherUpdateOssltModule_autoDeriveStatus_success() throws Exception {
        Teacher teacher = createTeacherAccount("osslt_teacher_derive", "OSSLT Teacher Derive");
        Student student = createStudentAccount("osslt_student_derive", "OSSLT", "Derive", "Derive");
        assignTeacherStudent(teacher, student);

        mockMvc.perform(put("/api/teacher/students/{studentId}/osslt-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latestOssltResult\":\"PASS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ossltTrackingStatus").value("PASSED"));

        mockMvc.perform(put("/api/teacher/students/{studentId}/osslt-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latestOssltResult\":\"FAIL\",\"hasOsslc\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasOsslc").value(false))
                .andExpect(jsonPath("$.ossltTrackingStatus").value("NEEDS_TRACKING"));

        mockMvc.perform(put("/api/teacher/students/{studentId}/osslt-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latestOssltResult\":\"FAIL\",\"hasOsslc\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ossltTrackingStatus").value("WAITING_UPDATE"));

        mockMvc.perform(put("/api/teacher/students/{studentId}/osslt-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latestOssltResult\":\"UNKNOWN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ossltTrackingStatus").value("WAITING_UPDATE"));
    }

    @Test
    void ossltEndpoints_permissions_studentDenied_adminAllowed() throws Exception {
        Teacher teacher = createTeacherAccount("osslt_teacher_permission", "OSSLT Teacher Permission");
        Student targetStudent = createStudentAccount("osslt_student_target", "OSSLT", "Target", "Target");
        assignTeacherStudent(teacher, targetStudent);

        Student studentOperator = createStudentAccount("osslt_student_operator", "OSSLT", "Operator", "Operator");
        User admin = createAdminAccount("osslt_admin_permission");

        mockMvc.perform(get("/api/teacher/students/{studentId}/osslt-module", targetStudent.getId())
                        .header("Authorization", bearerFor(studentOperator.getUser())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/teacher/students/{studentId}/osslt-module", targetStudent.getId())
                        .header("Authorization", bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(targetStudent.getId()));
    }

    @Test
    void studentGetAndUpdateOssltModule_success_withoutTeacherNoteField() throws Exception {
        Teacher teacher = createTeacherAccount("osslt_teacher_student_view", "OSSLT Teacher Student View");
        Student student = createStudentAccount("osslt_student_self", "OSSLT", "Self", "Self");
        assignTeacherStudent(teacher, student);

        mockMvc.perform(get("/api/student/osslt-module")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.latestOssltResult").value("UNKNOWN"))
                .andExpect(jsonPath("$.hasOsslc").value(nullValue()))
                .andExpect(jsonPath("$.ossltTrackingStatus").value("WAITING_UPDATE"))
                .andExpect(jsonPath("$.teacherNote").doesNotExist());

        mockMvc.perform(put("/api/student/osslt-module")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latestOssltResult\":\"FAIL\",\"hasOsslc\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.latestOssltResult").value("FAIL"))
                .andExpect(jsonPath("$.hasOsslc").value(false))
                .andExpect(jsonPath("$.ossltTrackingStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.teacherNote").doesNotExist());

        mockMvc.perform(get("/api/teacher/students/{studentId}/osslt-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasOsslc").value(false))
                .andExpect(jsonPath("$.ossltTrackingStatus").value("NEEDS_TRACKING"));
    }

    @Test
    void studentUpdateOssltModule_forbiddenFields_returns400() throws Exception {
        Student student = createStudentAccount("osslt_student_forbidden_fields", "OSSLT", "Forbidden", "Fields");

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("latestOssltResult", "PASS");
        payload.put("hasOsslc", true);
        payload.put("ossltTrackingManualStatus", "PASSED");
        payload.put("teacherNote", "legacy field should be ignored");

        mockMvc.perform(put("/api/student/osslt-module")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.details", hasItem("ossltTrackingManualStatus is not allowed for student APIs")));
    }

    @Test
    void studentUpdateOssltModule_legacyTeacherNoteIgnored_success() throws Exception {
        Student student = createStudentAccount("osslt_student_legacy_note", "OSSLT", "Legacy", "Note");

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("latestOssltResult", "PASS");
        payload.put("hasOsslc", true);
        payload.put("teacherNote", "legacy payload field");

        mockMvc.perform(put("/api/student/osslt-module")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.latestOssltResult").value("PASS"))
                .andExpect(jsonPath("$.hasOsslc").value(true))
                .andExpect(jsonPath("$.ossltTrackingStatus").value("PASSED"))
                .andExpect(jsonPath("$.teacherNote").doesNotExist());
    }

    @Test
    void studentUpdateOssltModule_invalidInput_returns400() throws Exception {
        Student student = createStudentAccount("osslt_student_invalid_input", "OSSLT", "Invalid", "Input");

        mockMvc.perform(put("/api/student/osslt-module")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latestOssltResult\":\"UNKNOWN\",\"hasOsslc\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.details", hasItem("latestOssltResult must be PASS or FAIL")))
                .andExpect(jsonPath("$.details", hasItem("hasOsslc must be true or false")));
    }

    @Test
    void studentAndTeacherEndpointRoleIsolation_success() throws Exception {
        Teacher teacher = createTeacherAccount("osslt_teacher_role_iso", "OSSLT Teacher Role Iso");
        Student student = createStudentAccount("osslt_student_role_iso", "OSSLT", "Role", "Iso");
        assignTeacherStudent(teacher, student);

        mockMvc.perform(get("/api/student/osslt-module")
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/student/osslt-module")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latestOssltResult\":\"PASS\",\"hasOsslc\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherUpdateOssltModule_invalidEnumAndDate_returns400() throws Exception {
        Teacher teacher = createTeacherAccount("osslt_teacher_validation", "OSSLT Teacher Validation");
        Student student = createStudentAccount("osslt_student_validation", "OSSLT", "Validation", "Validation");
        assignTeacherStudent(teacher, student);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("latestOssltResult", "WRONG");
        payload.put("latestOssltDate", "2026/04/06");
        payload.put("ossltTrackingManualStatus", "INVALID");

        mockMvc.perform(put("/api/teacher/students/{studentId}/osslt-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.details", hasItem("latestOssltResult invalid")))
                .andExpect(jsonPath("$.details", hasItem("latestOssltDate must be yyyy-mm-dd")))
                .andExpect(jsonPath("$.details", hasItem("ossltTrackingManualStatus invalid")));
    }

    @Test
    void teacherGetOssltSummary_batch_success() throws Exception {
        Teacher teacher = createTeacherAccount("osslt_teacher_summary", "OSSLT Teacher Summary");
        Student studentA = createStudentAccount("osslt_student_summary_a", "OSSLT", "SummaryA", "SummaryA");
        Student studentB = createStudentAccount("osslt_student_summary_b", "OSSLT", "SummaryB", "SummaryB");
        assignTeacherStudent(teacher, studentA);
        assignTeacherStudent(teacher, studentB);

        mockMvc.perform(put("/api/teacher/students/{studentId}/osslt-module", studentA.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latestOssltResult\":\"PASS\",\"hasOsslc\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/teacher/students/{studentId}/osslt-module", studentB.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latestOssltResult\":\"FAIL\",\"hasOsslc\":false,\"ossltTrackingManualStatus\":\"NEEDS_TRACKING\"}"))
                .andExpect(status().isOk());

        String query = studentA.getId() + "," + studentB.getId();
        mockMvc.perform(get("/api/teacher/students/osslt-summary")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .param("studentIds", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].studentId").value(studentA.getId()))
                .andExpect(jsonPath("$[0].hasOsslc").value(true))
                .andExpect(jsonPath("$[0].ossltTrackingStatus").value("PASSED"))
                .andExpect(jsonPath("$[1].studentId").value(studentB.getId()))
                .andExpect(jsonPath("$[1].hasOsslc").value(false))
                .andExpect(jsonPath("$[1].ossltTrackingManualStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$[1].ossltTrackingStatus").value("NEEDS_TRACKING"));
    }

    private Teacher createTeacherAccount(String username, String displayName) {
        User user = userRepository.save(new User(username, passwordEncoder.encode("Teacher!234"), UserRole.TEACHER));
        return teacherRepository.save(new Teacher(user, displayName));
    }

    private Student createStudentAccount(String username, String firstName, String lastName, String nickName) {
        User user = userRepository.save(new User(username, passwordEncoder.encode("Student!234"), UserRole.STUDENT));
        return studentRepository.save(new Student(user, firstName, lastName, nickName));
    }

    private User createAdminAccount(String username) {
        return userRepository.save(new User(username, passwordEncoder.encode("Admin!234"), UserRole.ADMIN));
    }

    private void assignTeacherStudent(Teacher teacher, Student student) {
        teacherStudentRepository.save(new TeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE, "osslt-tracking-api-test-assignment"));
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
