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
                .andExpect(jsonPath("$.hasTakenIeltsAcademic").value(false))
                .andExpect(jsonPath("$.preparationIntent").value("UNSET"))
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
                .andExpect(jsonPath("$.hasTakenIeltsAcademic").value(true))
                .andExpect(jsonPath("$.preparationIntent").value("UNSET"))
                .andExpect(jsonPath("$.records.length()").value(1))
                .andExpect(jsonPath("$.records[0].recordId").value("r-1"))
                .andExpect(jsonPath("$.records[0].listening").value(6.5));

        mockMvc.perform(get("/api/student/ielts-module")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
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
                .andExpect(jsonPath("$.preparationIntent").value("NOT_PREPARING"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.preparationIntent").value("NOT_PREPARING"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/ielts-summary", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.recordCount").value(0))
                .andExpect(jsonPath("$.preparationIntent").value("NOT_PREPARING"));
    }

    @Test
    void teacherUnassignedCannotReadStudentIeltsModule_returns403() throws Exception {
        Teacher teacherA = createTeacherAccount("ielts_teacher_scope_a", "IELTS Scope A");
        Teacher teacherB = createTeacherAccount("ielts_teacher_scope_b", "IELTS Scope B");
        Student student = createStudentAccount("ielts_scope_student", "IELTS", "Scope", "Scope");
        assignTeacherStudent(teacherB, student, TeacherStudentStatus.ACTIVE);

        mockMvc.perform(get("/api/teacher/students/{studentId}/ielts-module", student.getId())
                        .header("Authorization", bearerFor(teacherA.getUser())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
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
