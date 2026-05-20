package com.studentmanagement.studentmanagementserver.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.SchoolType;
import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.student.StudentProfile;
import com.studentmanagement.studentmanagementserver.domain.student.StudentSchoolRecord;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherStudent;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentProfileRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentSchoolRecordRepository;
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

import java.time.LocalDate;
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
class IeltsTrackingApiTest {

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
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private StudentSchoolRecordRepository studentSchoolRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void studentGetIeltsModule_defaultState_success() throws Exception {
        Student student = createStudentAccount("ielts_student_default", "IELTS", "Default", "Default");

        mockMvc.perform(get("/api/student/ielts-module")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.languageScoreType").value("IELTS"))
                .andExpect(jsonPath("$.hasTakenIeltsAcademic").value(false))
                .andExpect(jsonPath("$.preparationIntent").value("UNSET"))
                .andExpect(jsonPath("$.languageTrackingManualStatus").value(nullValue()))
                .andExpect(jsonPath("$.languageCourseStatus").value(nullValue()))
                .andExpect(jsonPath("$.trackingStatus").value("YELLOW_NEEDS_PREPARATION"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.summary.languageScoreType").value("IELTS"))
                .andExpect(jsonPath("$.summary.trackingStatus").value("YELLOW_NEEDS_PREPARATION"))
                .andExpect(jsonPath("$.summary.languageTrackingStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.records.length()").value(0))
                .andExpect(jsonPath("$.languageRisk.shouldShowIeltsModule").value(true))
                .andExpect(jsonPath("$.languageRisk.languageRiskFlag").value("RISK"));
    }

    @Test
    void studentUpdateRecords_andFetch_success() throws Exception {
        Student student = createStudentAccount("ielts_student_records", "IELTS", "Records", "Records");
        saveProfileAndSchool(student, "Chinese", "China (Mainland)", LocalDate.of(2023, 9, 1), LocalDate.of(2027, 6, 30), "Canada");

        mockMvc.perform(put("/api/student/ielts-module/records")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordsPayload(true, 6.5d, 6.5d, 6.0d, 6.0d)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.graduationYear").value(2027))
                .andExpect(jsonPath("$.languageScoreType").value("IELTS"))
                .andExpect(jsonPath("$.hasTakenIeltsAcademic").value(true))
                .andExpect(jsonPath("$.preparationIntent").value("UNSET"))
                .andExpect(jsonPath("$.records.length()").value(1))
                .andExpect(jsonPath("$.records[0].recordId").value("r-1"))
                .andExpect(jsonPath("$.records[0].listening").value(6.5));

        mockMvc.perform(get("/api/student/ielts-module")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("IELTS"))
                .andExpect(jsonPath("$.hasTakenIeltsAcademic").value(true))
                .andExpect(jsonPath("$.records.length()").value(1))
                .andExpect(jsonPath("$.records[0].recordId").value("r-1"));
    }

    @Test
    void studentUpdatePreparationIntent_success() throws Exception {
        Student student = createStudentAccount("ielts_student_intent", "IELTS", "Intent", "Intent");

        mockMvc.perform(put("/api/student/ielts-module/preparation-intent")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(preparationIntentPayload(false, "PREPARING")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasTakenIeltsAcademic").value(false))
                .andExpect(jsonPath("$.preparationIntent").value("PREPARING"))
                .andExpect(jsonPath("$.records.length()").value(0));
    }

    @Test
    void studentUpdateRecords_withManualStatusField_returns403FieldForbidden() throws Exception {
        Student student = createStudentAccount("ielts_student_records_forbidden", "IELTS", "Forbidden", "Forbidden");

        Map<String, Object> record = new LinkedHashMap<String, Object>();
        record.put("recordId", "r-1");
        record.put("testDate", "2025-10-12");
        record.put("listening", 6.5d);
        record.put("reading", 6.5d);
        record.put("writing", 6.0d);
        record.put("speaking", 6.0d);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", true);
        payload.put("languageTrackingManualStatus", "TEACHER_REVIEW_APPROVED");
        payload.put("records", Arrays.asList(record));

        mockMvc.perform(put("/api/student/ielts-module/records")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FIELD_FORBIDDEN"));
    }

    @Test
    void studentUpdateRecords_withCanonicalManualStatusField_returns403FieldForbidden() throws Exception {
        Student student = createStudentAccount("ielts_student_records_forbidden_new_key", "IELTS", "Forbidden", "NewKey");

        Map<String, Object> record = new LinkedHashMap<String, Object>();
        record.put("recordId", "r-1");
        record.put("testDate", "2025-10-12");
        record.put("listening", 6.5d);
        record.put("reading", 6.5d);
        record.put("writing", 6.0d);
        record.put("speaking", 6.0d);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", true);
        payload.put("languageScoreTrackingManualStatus", "TEACHER_REVIEW_APPROVED");
        payload.put("records", Arrays.asList(record));

        mockMvc.perform(put("/api/student/ielts-module/records")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FIELD_FORBIDDEN"));
    }

    @Test
    void studentUpdatePreparationIntent_withManualStatusField_returns403FieldForbidden() throws Exception {
        Student student = createStudentAccount("ielts_student_intent_forbidden", "IELTS", "IntentF", "IntentF");

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", false);
        payload.put("preparationIntent", "PREPARING");
        payload.put("languageTrackingManualStatus", "TEACHER_REVIEW_APPROVED");

        mockMvc.perform(put("/api/student/ielts-module/preparation-intent")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FIELD_FORBIDDEN"));
    }

    @Test
    void studentUpdateRecords_withLanguageCourseStatusField_returns403FieldForbidden() throws Exception {
        Student student = createStudentAccount("ielts_student_records_course_status_forbidden", "IELTS", "Course", "Forbidden");

        Map<String, Object> record = new LinkedHashMap<String, Object>();
        record.put("recordId", "r-1");
        record.put("testDate", "2025-10-12");
        record.put("listening", 6.5d);
        record.put("reading", 6.5d);
        record.put("writing", 6.0d);
        record.put("speaking", 6.0d);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", true);
        payload.put("languageCourseStatus", "ENROLLED_GLOBAL_IELTS");
        payload.put("records", Arrays.asList(record));

        mockMvc.perform(put("/api/student/ielts-module/records")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FIELD_FORBIDDEN"));
    }

    @Test
    void studentUpdateRecords_invalidBandStep_returns400() throws Exception {
        Student student = createStudentAccount("ielts_student_bad_band", "IELTS", "Band", "Band");

        mockMvc.perform(put("/api/student/ielts-module/records")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordsPayload(true, 6.3d, 6.5d, 6.0d, 6.0d)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.details", hasItem("records[0].listening must use 0.5 steps")));
    }

    @Test
    void teacherAssignedCanReadAndUpdate_success() throws Exception {
        Teacher teacher = createTeacherAccount("ielts_teacher_assigned", "IELTS Teacher Assigned");
        Student student = createStudentAccount("ielts_student_assigned", "IELTS", "Assigned", "Assigned");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teacherModulePayload(false, "NOT_PREPARING")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.languageScoreType").value("IELTS"))
                .andExpect(jsonPath("$.preparationIntent").value("NOT_PREPARING"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.languageScoreType").value("IELTS"))
                .andExpect(jsonPath("$.preparationIntent").value("NOT_PREPARING"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/ielts-summary", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.languageScoreType").value("IELTS"))
                .andExpect(jsonPath("$.recordCount").value(0))
                .andExpect(jsonPath("$.preparationIntent").value("NOT_PREPARING"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.summary.languageScoreType").value("IELTS"))
                .andExpect(jsonPath("$.summary.languageTrackingStatus").value("NEEDS_TRACKING"));
    }

    @Test
    void teacherUpdateLanguageCourseStatus_thenGetEcho_success() throws Exception {
        Teacher teacher = createTeacherAccount("ielts_teacher_course_status", "IELTS Teacher Course Status");
        Student student = createStudentAccount("ielts_student_course_status", "IELTS", "Course", "Status");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", false);
        payload.put("preparationIntent", "NOT_PREPARING");
        payload.put("languageCourseStatus", "ENROLLED_GLOBAL_IELTS");
        payload.put("records", Arrays.asList());

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageCourseStatus").value("ENROLLED_GLOBAL_IELTS"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageCourseStatus").value("ENROLLED_GLOBAL_IELTS"));

        mockMvc.perform(get("/api/student/ielts-module")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageCourseStatus").value("ENROLLED_GLOBAL_IELTS"));
    }

    @Test
    void teacherUpdateLanguageCourseStatus_withAlias_success() throws Exception {
        Teacher teacher = createTeacherAccount("ielts_teacher_course_status_alias", "IELTS Teacher Course Status Alias");
        Student student = createStudentAccount("ielts_student_course_status_alias", "IELTS", "Course", "Alias");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", false);
        payload.put("preparationIntent", "NOT_PREPARING");
        payload.put("languageCourseEnrollmentStatus", "EXAM_REGISTERED");
        payload.put("records", Arrays.asList());

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageCourseStatus").value("EXAM_REGISTERED"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageCourseStatus").value("EXAM_REGISTERED"));
    }

    @Test
    void teacherUpdateLanguageCourseStatus_invalidValue_returns400() throws Exception {
        Teacher teacher = createTeacherAccount("ielts_teacher_course_status_invalid", "IELTS Teacher Course Status Invalid");
        Student student = createStudentAccount("ielts_student_course_status_invalid", "IELTS", "Course", "Invalid");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", false);
        payload.put("preparationIntent", "NOT_PREPARING");
        payload.put("languageCourseStatus", "INVALID_STATUS");
        payload.put("records", Arrays.asList());

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.details", hasItem("languageCourseStatus invalid")));
    }

    @Test
    void teacherCanReadAnyStudentIeltsModule_returns200() throws Exception {
        Teacher teacherA = createTeacherAccount("ielts_teacher_scope_a", "IELTS Scope A");
        Teacher teacherB = createTeacherAccount("ielts_teacher_scope_b", "IELTS Scope B");
        Student student = createStudentAccount("ielts_scope_student", "IELTS", "Scope", "Scope");
        assignTeacherStudent(teacherB, student, TeacherStudentStatus.ACTIVE);

        mockMvc.perform(get("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacherA.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()));
    }

    @Test
    void teacherManualApprovedThenClear_restoresAutomaticStatus() throws Exception {
        Teacher teacher = createTeacherAccount("ielts_teacher_manual", "IELTS Teacher Manual");
        Student student = createStudentAccount("ielts_student_manual", "IELTS", "Manual", "Manual");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        saveProfileAndSchool(
                student,
                "Chinese",
                "China",
                LocalDate.of(2023, 9, 1),
                LocalDate.of(2027, 6, 30),
                "Canada"
        );

        Map<String, Object> strictRecord = new LinkedHashMap<String, Object>();
        strictRecord.put("recordId", "strict-1");
        strictRecord.put("testDate", "2025-10-12");
        strictRecord.put("listening", 7.0d);
        strictRecord.put("reading", 7.0d);
        strictRecord.put("writing", 7.0d);
        strictRecord.put("speaking", 7.0d);

        Map<String, Object> approvePayload = new LinkedHashMap<String, Object>();
        approvePayload.put("hasTakenIeltsAcademic", true);
        approvePayload.put("records", Arrays.asList(strictRecord));
        approvePayload.put("languageTrackingManualStatus", "TEACHER_REVIEW_APPROVED");

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approvePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.languageTrackingManualStatus").value("TEACHER_REVIEW_APPROVED"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("TEACHER_REVIEW_APPROVED"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/ielts-summary", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("TEACHER_REVIEW_APPROVED"))
                .andExpect(jsonPath("$.summary.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.summary.languageTrackingStatus").value("TEACHER_REVIEW_APPROVED"));

        Map<String, Object> clearPayload = new LinkedHashMap<String, Object>();
        clearPayload.put("hasTakenIeltsAcademic", true);
        clearPayload.put("languageTrackingManualStatus", null);

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clearPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.languageTrackingManualStatus").value(nullValue()))
                .andExpect(jsonPath("$.languageTrackingStatus").value("AUTO_PASS_ALL_SCHOOLS"));
    }

    @Test
    void teacherManualStatus_nonNullAlwaysOverridesFinalStatus() throws Exception {
        Teacher teacher = createTeacherAccount("ielts_teacher_manual_override", "IELTS Teacher Manual Override");
        Student student = createStudentAccount("ielts_student_manual_override", "IELTS", "ManualOv", "ManualOv");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        saveProfileAndSchool(
                student,
                "Chinese",
                "China",
                LocalDate.of(2023, 9, 1),
                LocalDate.of(2027, 6, 30),
                "Canada"
        );

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teacherRecordsPayloadWithManual("strict-manual", 7.0d, 7.0d, 7.0d, 7.0d, "NEEDS_TRACKING")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.languageTrackingManualStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("NEEDS_TRACKING"));
    }

    @Test
    void trackingStatusMapsToLanguageTrackingStatus_strictCommonYellow() throws Exception {
        Teacher teacher = createTeacherAccount("ielts_teacher_mapping", "IELTS Teacher Mapping");
        Student student = createStudentAccount("ielts_student_mapping", "IELTS", "Mapping", "Mapping");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        saveProfileAndSchool(
                student,
                "Chinese",
                "China",
                LocalDate.of(2023, 9, 1),
                LocalDate.of(2027, 6, 30),
                "Canada"
        );

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teacherRecordsPayloadWithManual("strict-r", 7.0d, 7.0d, 7.0d, 7.0d, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("AUTO_PASS_ALL_SCHOOLS"));

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teacherRecordsPayloadWithManual("common-r", 6.5d, 6.5d, 6.0d, 6.0d, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_COMMON_PASS_WITH_WARNING"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("AUTO_PASS_PARTIAL_SCHOOLS"));

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teacherRecordsPayloadWithManual("yellow-r", 5.5d, 5.5d, 5.5d, 5.5d, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingStatus").value("YELLOW_NEEDS_PREPARATION"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("NEEDS_TRACKING"));
    }

    @Test
    void studentUpdateClearsManualAndRecomputesLanguageTrackingStatus() throws Exception {
        Teacher teacher = createTeacherAccount("ielts_teacher_student_clear", "IELTS Teacher Student Clear");
        Student student = createStudentAccount("ielts_student_clear_manual", "IELTS", "Clear", "Clear");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        saveProfileAndSchool(
                student,
                "Chinese",
                "China",
                LocalDate.of(2023, 9, 1),
                LocalDate.of(2027, 6, 30),
                "Canada"
        );

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teacherRecordsPayloadWithManual(
                                "strict-before-clear",
                                7.0d,
                                7.0d,
                                7.0d,
                                7.0d,
                                "TEACHER_REVIEW_APPROVED"
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageTrackingManualStatus").value("TEACHER_REVIEW_APPROVED"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("TEACHER_REVIEW_APPROVED"));

        mockMvc.perform(put("/api/student/ielts-module/records")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordsPayload(true, 6.5d, 6.5d, 6.0d, 6.0d)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_COMMON_PASS_WITH_WARNING"))
                .andExpect(jsonPath("$.languageTrackingManualStatus").value(nullValue()))
                .andExpect(jsonPath("$.languageTrackingStatus").value("AUTO_PASS_PARTIAL_SCHOOLS"));
    }

    @Test
    void studentUpdateToeflRecords_persistsLanguageScoreTypeAndUsesToeflRecordsFirst() throws Exception {
        Teacher teacher = createTeacherAccount("ielts_teacher_toefl_priority", "IELTS Teacher TOEFL Priority");
        Student student = createStudentAccount("ielts_student_toefl_priority", "TOEFL", "Priority", "Priority");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        saveProfileAndSchool(
                student,
                "Chinese",
                "China",
                LocalDate.of(2023, 9, 1),
                LocalDate.of(2027, 6, 30),
                "Canada"
        );

        Map<String, Object> invalidFallbackRecord = createRecord("ignored-record", "2025-10-12", 7.0d, 7.0d, 7.0d, 7.0d);
        Map<String, Object> toeflRecord = createRecord("toefl-record", "2025-10-12", 5.0d, 5.0d, 5.0d, 5.0d);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", true);
        payload.put("test_type", "TOEFL");
        payload.put("records", Arrays.asList(invalidFallbackRecord));
        payload.put("toeflRecords", Arrays.asList(toeflRecord));

        mockMvc.perform(put("/api/student/ielts-module/records")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("TOEFL"))
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.records[0].recordId").value("toefl-record"));

        mockMvc.perform(get("/api/student/ielts-module")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("TOEFL"))
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.summary.languageScoreType").value("TOEFL"))
                .andExpect(jsonPath("$.summary.trackingStatus").value("GREEN_STRICT_PASS"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("TOEFL"))
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.summary.languageScoreType").value("TOEFL"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/ielts-summary", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("TOEFL"))
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.summary.languageScoreType").value("TOEFL"))
                .andExpect(jsonPath("$.summary.languageTrackingStatus").value("AUTO_PASS_ALL_SCHOOLS"));
    }

    @Test
    void teacherUpdateToeflRecords_thresholdsStrictCommonYellow() throws Exception {
        Teacher teacher = createTeacherAccount("ielts_teacher_toefl_thresholds", "IELTS Teacher TOEFL Thresholds");
        Student student = createStudentAccount("ielts_student_toefl_thresholds", "TOEFL", "Thresholds", "Thresholds");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        saveProfileAndSchool(
                student,
                "Chinese",
                "China",
                LocalDate.of(2023, 9, 1),
                LocalDate.of(2027, 6, 30),
                "Canada"
        );

        Map<String, Object> strictPayload = new LinkedHashMap<String, Object>();
        strictPayload.put("hasTakenIeltsAcademic", true);
        strictPayload.put("testType", "TOEFL");
        strictPayload.put("records", Arrays.asList(createRecord("toefl-strict", "2025-10-12", 5.0d, 5.0d, 5.0d, 5.0d)));

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(strictPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("TOEFL"))
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("AUTO_PASS_ALL_SCHOOLS"));

        Map<String, Object> commonPayload = new LinkedHashMap<String, Object>();
        commonPayload.put("hasTakenIeltsAcademic", true);
        commonPayload.put("languageScoreType", "TOEFL");
        commonPayload.put("toeflRecords", Arrays.asList(createRecord("toefl-common", "2025-10-12", 4.5d, 4.5d, 4.5d, 4.5d)));

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commonPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("TOEFL"))
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_COMMON_PASS_WITH_WARNING"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("AUTO_PASS_PARTIAL_SCHOOLS"));

        Map<String, Object> yellowPayload = new LinkedHashMap<String, Object>();
        yellowPayload.put("hasTakenIeltsAcademic", true);
        yellowPayload.put("languageScoreType", "TOEFL");
        yellowPayload.put("toeflRecords", Arrays.asList(createRecord("toefl-yellow", "2025-10-12", 4.0d, 4.0d, 4.0d, 4.0d)));

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(yellowPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("TOEFL"))
                .andExpect(jsonPath("$.trackingStatus").value("YELLOW_NEEDS_PREPARATION"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("NEEDS_TRACKING"));
    }

    @Test
    void teacherUpdateToeflRecords_outsideValidityWindow_resultsYellow() throws Exception {
        Teacher teacher = createTeacherAccount("ielts_teacher_toefl_window", "IELTS Teacher TOEFL Window");
        Student student = createStudentAccount("ielts_student_toefl_window", "TOEFL", "Window", "Window");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        saveProfileAndSchool(
                student,
                "Chinese",
                "China",
                LocalDate.of(2023, 9, 1),
                LocalDate.of(2027, 6, 30),
                "Canada"
        );

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", true);
        payload.put("languageScoreType", "TOEFL");
        payload.put("toeflRecords", Arrays.asList(createRecord("toefl-expired", "2025-05-30", 5.5d, 5.5d, 5.5d, 5.5d)));

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("TOEFL"))
                .andExpect(jsonPath("$.trackingStatus").value("YELLOW_NEEDS_PREPARATION"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("NEEDS_TRACKING"));
    }

    @Test
    void studentUpdateToeflRecords_invalidRange_returns400() throws Exception {
        Student student = createStudentAccount("ielts_student_toefl_invalid_range", "TOEFL", "Range", "Range");

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", true);
        payload.put("languageScoreType", "TOEFL");
        payload.put("toeflRecords", Arrays.asList(createRecord("bad-range", "2025-10-12", 0.5d, 4.5d, 4.5d, 4.5d)));

        mockMvc.perform(put("/api/student/ielts-module/records")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.details", hasItem("toeflRecords[0].listening must be between 1.0 and 6.0")));
    }

    @Test
    void studentUpdatePreparationIntent_withDuolingoType_persistsType() throws Exception {
        Student student = createStudentAccount("ielts_student_duo_intent", "DUO", "Intent", "Intent");

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", false);
        payload.put("languageScoreType", "DUOLINGO");
        payload.put("preparationIntent", "PREPARING");

        mockMvc.perform(put("/api/student/ielts-module/preparation-intent")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("DUOLINGO"))
                .andExpect(jsonPath("$.hasTakenIeltsAcademic").value(false))
                .andExpect(jsonPath("$.preparationIntent").value("PREPARING"));

        mockMvc.perform(get("/api/student/ielts-module")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("DUOLINGO"))
                .andExpect(jsonPath("$.summary.languageScoreType").value("DUOLINGO"));
    }

    @Test
    void studentUpdateDuolingoRecords_persistsLanguageScoreTypeAndUsesDuolingoRecordsFirst() throws Exception {
        Teacher teacher = createTeacherAccount("ielts_teacher_duolingo_priority", "IELTS Teacher DUOLINGO Priority");
        Student student = createStudentAccount("ielts_student_duolingo_priority", "DUOLINGO", "Priority", "Priority");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        saveProfileAndSchool(
                student,
                "Chinese",
                "China",
                LocalDate.of(2023, 9, 1),
                LocalDate.of(2027, 6, 30),
                "Canada"
        );

        Map<String, Object> fallbackRecord = createRecord("ignored-record", "2025-10-12", 7.0d, 7.0d, 7.0d, 7.0d);
        Map<String, Object> duolingoRecord = createRecord("duolingo-record", "2025-10-12", 130.0d, 130.0d, 130.0d, 130.0d);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", true);
        payload.put("test_type", "DUOLINGO");
        payload.put("records", Arrays.asList(fallbackRecord));
        payload.put("duolingoRecords", Arrays.asList(duolingoRecord));

        mockMvc.perform(put("/api/student/ielts-module/records")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("DUOLINGO"))
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.records[0].recordId").value("duolingo-record"));

        mockMvc.perform(get("/api/student/ielts-module")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("DUOLINGO"))
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.summary.languageScoreType").value("DUOLINGO"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("DUOLINGO"))
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.summary.languageScoreType").value("DUOLINGO"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/ielts-summary", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("DUOLINGO"))
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.summary.languageScoreType").value("DUOLINGO"))
                .andExpect(jsonPath("$.summary.languageTrackingStatus").value("AUTO_PASS_ALL_SCHOOLS"));
    }

    @Test
    void teacherUpdateDuolingoRecords_thresholdsStrictCommonYellow() throws Exception {
        Teacher teacher = createTeacherAccount("ielts_teacher_duolingo_thresholds", "IELTS Teacher DUOLINGO Thresholds");
        Student student = createStudentAccount("ielts_student_duolingo_thresholds", "DUOLINGO", "Thresholds", "Thresholds");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        saveProfileAndSchool(
                student,
                "Chinese",
                "China",
                LocalDate.of(2023, 9, 1),
                LocalDate.of(2027, 6, 30),
                "Canada"
        );

        Map<String, Object> strictPayload = new LinkedHashMap<String, Object>();
        strictPayload.put("hasTakenIeltsAcademic", true);
        strictPayload.put("testType", "DUOLINGO");
        strictPayload.put("records", Arrays.asList(createRecord("duolingo-strict", "2025-10-12", 130.0d, 130.0d, 130.0d, 130.0d)));

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(strictPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("DUOLINGO"))
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("AUTO_PASS_ALL_SCHOOLS"));

        Map<String, Object> commonPayload = new LinkedHashMap<String, Object>();
        commonPayload.put("hasTakenIeltsAcademic", true);
        commonPayload.put("languageScoreType", "DUOLINGO");
        commonPayload.put("duolingoRecords", Arrays.asList(createRecord("duolingo-common", "2025-10-12", 120.0d, 120.0d, 120.0d, 120.0d)));

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commonPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("DUOLINGO"))
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_COMMON_PASS_WITH_WARNING"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("AUTO_PASS_PARTIAL_SCHOOLS"));

        Map<String, Object> yellowPayload = new LinkedHashMap<String, Object>();
        yellowPayload.put("hasTakenIeltsAcademic", true);
        yellowPayload.put("languageScoreType", "DUOLINGO");
        yellowPayload.put("duolingoRecords", Arrays.asList(createRecord("duolingo-yellow", "2025-10-12", 110.0d, 110.0d, 110.0d, 110.0d)));

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(yellowPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("DUOLINGO"))
                .andExpect(jsonPath("$.trackingStatus").value("YELLOW_NEEDS_PREPARATION"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("NEEDS_TRACKING"));
    }

    @Test
    void teacherUpdateDuolingoRecords_outsideValidityWindow_resultsYellow() throws Exception {
        Teacher teacher = createTeacherAccount("ielts_teacher_duolingo_window", "IELTS Teacher DUOLINGO Window");
        Student student = createStudentAccount("ielts_student_duolingo_window", "DUOLINGO", "Window", "Window");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        saveProfileAndSchool(
                student,
                "Chinese",
                "China",
                LocalDate.of(2023, 9, 1),
                LocalDate.of(2027, 6, 30),
                "Canada"
        );

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", true);
        payload.put("languageScoreType", "DUOLINGO");
        payload.put("duolingoRecords", Arrays.asList(createRecord("duolingo-expired", "2025-05-30", 140.0d, 140.0d, 140.0d, 140.0d)));

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("DUOLINGO"))
                .andExpect(jsonPath("$.trackingStatus").value("YELLOW_NEEDS_PREPARATION"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("NEEDS_TRACKING"));
    }

    @Test
    void studentUpdateDuolingoRecords_invalidRange_returns400() throws Exception {
        Student student = createStudentAccount("ielts_student_duolingo_invalid_range", "DUOLINGO", "Range", "Range");

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", true);
        payload.put("languageScoreType", "DUOLINGO");
        payload.put("duolingoRecords", Arrays.asList(createRecord("duolingo-bad-range", "2025-10-12", 165.0d, 130.0d, 130.0d, 130.0d)));

        mockMvc.perform(put("/api/student/ielts-module/records")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.details", hasItem("duolingoRecords[0].listening must be between 10.0 and 160.0")));
    }

    @Test
    void studentUpdateDuolingoRecords_invalidStep_returns400() throws Exception {
        Student student = createStudentAccount("ielts_student_duolingo_invalid_step", "DUOLINGO", "Step", "Step");

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", true);
        payload.put("languageScoreType", "DUOLINGO");
        payload.put("duolingoRecords", Arrays.asList(createRecord("duolingo-bad-step", "2025-10-12", 121.0d, 130.0d, 130.0d, 130.0d)));

        mockMvc.perform(put("/api/student/ielts-module/records")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.details", hasItem("duolingoRecords[0].listening must use 5-point steps")));
    }

    @Test
    void studentUpdateDuolingoRecords_withManualStatusField_returns403FieldForbidden() throws Exception {
        Student student = createStudentAccount("ielts_student_duolingo_manual_forbidden", "DUOLINGO", "Manual", "Manual");

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", true);
        payload.put("languageScoreType", "DUOLINGO");
        payload.put("languageTrackingManualStatus", "TEACHER_REVIEW_APPROVED");
        payload.put("duolingoRecords", Arrays.asList(createRecord("duolingo-forbidden", "2025-10-12", 130.0d, 130.0d, 130.0d, 130.0d)));

        mockMvc.perform(put("/api/student/ielts-module/records")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FIELD_FORBIDDEN"));
    }

    @Test
    void readEndpoints_returnConsistentTrackingFields() throws Exception {
        Teacher teacher = createTeacherAccount("ielts_teacher_read_consistency", "IELTS Teacher Read Consistency");
        Student student = createStudentAccount("ielts_student_read_consistency", "Read", "Consistency", "Consistency");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        saveProfileAndSchool(
                student,
                "Chinese",
                "China",
                LocalDate.of(2023, 9, 1),
                LocalDate.of(2027, 6, 30),
                "Canada"
        );

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", true);
        payload.put("languageScoreType", "TOEFL");
        payload.put("toeflRecords", Arrays.asList(createRecord("toefl-manual", "2025-10-12", 5.0d, 5.0d, 5.0d, 5.0d)));
        payload.put("languageTrackingManualStatus", "NEEDS_TRACKING");

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("TOEFL"))
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.languageTrackingManualStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.summary.languageScoreType").value("TOEFL"))
                .andExpect(jsonPath("$.summary.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.summary.languageTrackingStatus").value("NEEDS_TRACKING"));

        mockMvc.perform(get("/api/student/ielts-module")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("TOEFL"))
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.languageTrackingManualStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.summary.languageScoreType").value("TOEFL"))
                .andExpect(jsonPath("$.summary.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.summary.languageTrackingStatus").value("NEEDS_TRACKING"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("TOEFL"))
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.languageTrackingManualStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.summary.languageScoreType").value("TOEFL"))
                .andExpect(jsonPath("$.summary.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.summary.languageTrackingStatus").value("NEEDS_TRACKING"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/ielts-summary", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreType").value("TOEFL"))
                .andExpect(jsonPath("$.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.summary.languageScoreType").value("TOEFL"))
                .andExpect(jsonPath("$.summary.trackingStatus").value("GREEN_STRICT_PASS"))
                .andExpect(jsonPath("$.summary.languageTrackingStatus").value("NEEDS_TRACKING"));
    }

    @Test
    void teacherUpdate_withLegacyManualStatusKey_writesCanonicalAndReturnsDualKeys() throws Exception {
        Teacher teacher = createTeacherAccount("ielts_teacher_legacy_manual_alias", "IELTS Teacher Legacy Manual Alias");
        Student student = createStudentAccount("ielts_student_legacy_manual_alias", "Legacy", "Manual", "Alias");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        saveProfileAndSchool(
                student,
                "Chinese",
                "China",
                LocalDate.of(2023, 9, 1),
                LocalDate.of(2027, 6, 30),
                "Canada"
        );

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", true);
        payload.put("records", Arrays.asList(createRecord("legacy-key-record", "2025-10-12", 5.0d, 5.0d, 5.0d, 5.0d)));
        payload.put("languageTrackingManualStatus", "NEEDS_TRACKING");

        mockMvc.perform(put("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreTrackingManualStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.languageTrackingManualStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.languageScoreTrackingStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.summary.languageScoreTrackingStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.summary.languageTrackingStatus").value("NEEDS_TRACKING"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/ielts-summary", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageScoreTrackingStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.languageTrackingStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.summary.languageScoreTrackingStatus").value("NEEDS_TRACKING"))
                .andExpect(jsonPath("$.summary.languageTrackingStatus").value("NEEDS_TRACKING"));
    }

    private String recordsPayload(boolean hasTaken, double listening, double reading, double writing, double speaking) throws Exception {
        Map<String, Object> record = new LinkedHashMap<String, Object>();
        record.put("recordId", "r-1");
        record.put("testDate", "2025-10-12");
        record.put("listening", listening);
        record.put("reading", reading);
        record.put("writing", writing);
        record.put("speaking", speaking);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", hasTaken);
        payload.put("records", Arrays.asList(record));
        return objectMapper.writeValueAsString(payload);
    }

    private String preparationIntentPayload(boolean hasTaken, String preparationIntent) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", hasTaken);
        payload.put("preparationIntent", preparationIntent);
        return objectMapper.writeValueAsString(payload);
    }

    private String teacherModulePayload(boolean hasTaken, String preparationIntent) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", hasTaken);
        payload.put("preparationIntent", preparationIntent);
        payload.put("records", Arrays.asList());
        return objectMapper.writeValueAsString(payload);
    }

    private String teacherRecordsPayloadWithManual(String recordId,
                                                   double listening,
                                                   double reading,
                                                   double writing,
                                                   double speaking,
                                                   String languageTrackingManualStatus) throws Exception {
        Map<String, Object> record = new LinkedHashMap<String, Object>();
        record.put("recordId", recordId);
        record.put("testDate", "2025-10-12");
        record.put("listening", listening);
        record.put("reading", reading);
        record.put("writing", writing);
        record.put("speaking", speaking);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hasTakenIeltsAcademic", true);
        payload.put("preparationIntent", "UNSET");
        payload.put("records", Arrays.asList(record));
        payload.put("languageTrackingManualStatus", languageTrackingManualStatus);
        return objectMapper.writeValueAsString(payload);
    }

    private Map<String, Object> createRecord(String recordId,
                                             String testDate,
                                             double listening,
                                             double reading,
                                             double writing,
                                             double speaking) {
        Map<String, Object> record = new LinkedHashMap<String, Object>();
        record.put("recordId", recordId);
        record.put("testDate", testDate);
        record.put("listening", listening);
        record.put("reading", reading);
        record.put("writing", writing);
        record.put("speaking", speaking);
        return record;
    }

    private void saveProfileAndSchool(Student student,
                                      String firstLanguage,
                                      String citizenship,
                                      LocalDate schoolStart,
                                      LocalDate schoolEnd,
                                      String schoolCountry) {
        StudentProfile profile = new StudentProfile(student);
        profile.setFirstLanguage(firstLanguage);
        profile.setCitizenship(citizenship);
        studentProfileRepository.save(profile);

        StudentSchoolRecord schoolRecord = new StudentSchoolRecord(
                student,
                SchoolType.MAIN,
                "Ontario Secondary School",
                schoolStart,
                schoolEnd
        );
        schoolRecord.setCountry(schoolCountry);
        studentSchoolRecordRepository.save(schoolRecord);
    }

    private Teacher createTeacherAccount(String username, String displayName) {
        User user = userRepository.save(new User(username, passwordEncoder.encode("Teacher!234"), UserRole.TEACHER));
        return teacherRepository.save(new Teacher(user, displayName));
    }

    private Student createStudentAccount(String username, String firstName, String lastName, String nickName) {
        User user = userRepository.save(new User(username, passwordEncoder.encode("Student!234"), UserRole.STUDENT));
        return studentRepository.save(new Student(user, firstName, lastName, nickName));
    }

    private void assignTeacherStudent(Teacher teacher, Student student, TeacherStudentStatus status) {
        teacherStudentRepository.save(new TeacherStudent(teacher, student, status, "ielts-tracking-api-test-assignment"));
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
