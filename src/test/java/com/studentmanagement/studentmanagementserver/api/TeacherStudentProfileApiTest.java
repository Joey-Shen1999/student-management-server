package com.studentmanagement.studentmanagementserver.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.student.StudentCourseRecord;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherStudent;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentCourseRecordRepository;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherStudentProfileApiTest {

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
    private StudentCourseRecordRepository studentCourseRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void teacherProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/teacher/students/{studentId}/profile", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void teacherProfile_studentRole_forbidden403() throws Exception {
        Student studentOperator = createStudentAccount("phase2_student_forbidden", "Stu", "Operator", "SO");

        mockMvc.perform(get("/api/teacher/students/{studentId}/profile", studentOperator.getId())
                        .header("Authorization", bearerFor(studentOperator.getUser())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", studentOperator.getId())
                        .header("Authorization", bearerFor(studentOperator.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(buildProfilePayload())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void teacherProfile_teacherAssignedActive_getAndPut200() throws Exception {
        Teacher teacher = createTeacherAccount("phase2_teacher_active", "Teacher Active");
        Student student = createStudentAccount("phase2_student_active", "Amy", "Chen", "Amy");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        mockMvc.perform(get("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalFirstName").value("Amy"))
                .andExpect(jsonPath("$.schools").isArray())
                .andExpect(jsonPath("$.otherCourses").isArray());

        Map<String, Object> payload = buildProfilePayload();
        payload.put("legalFirstName", "  TeacherEdited  ");
        payload.put("preferredName", " TE ");

        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalFirstName").value("TeacherEdited"))
                .andExpect(jsonPath("$.preferredName").value("TE"))
                .andExpect(jsonPath("$.otherCourses[0].courseCode").value("MHF4U"))
                .andExpect(jsonPath("$.externalCourses[0].courseCode").value("MHF4U"))
                .andExpect(jsonPath("$.schoolRecords[0].schoolType").value("MAIN"));
    }

    @Test
    void teacherProfile_teacherUnassigned_forbidden403() throws Exception {
        Teacher teacher = createTeacherAccount("phase2_teacher_unassigned", "Teacher Unassigned");
        Student student = createStudentAccount("phase2_student_unassigned", "Amy", "Chen", "Amy");

        mockMvc.perform(get("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Forbidden: student is not actively assigned to this teacher."))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(buildProfilePayload())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void teacherProfile_teacherArchivedRelation_forbidden403() throws Exception {
        Teacher teacher = createTeacherAccount("phase2_teacher_archived", "Teacher Archived");
        Student student = createStudentAccount("phase2_student_archived", "Amy", "Chen", "Amy");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ARCHIVED);

        mockMvc.perform(get("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(buildProfilePayload())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void teacherProfile_adminAnyStudent_getAndPut200() throws Exception {
        User admin = createAdmin("phase2_admin_any");
        Student student = createStudentAccount("phase2_student_admin_any", "Amy", "Chen", "Amy");

        mockMvc.perform(get("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalFirstName").value("Amy"));

        Map<String, Object> payload = buildProfilePayload();
        payload.put("legalFirstName", "AdminEdited");

        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalFirstName").value("AdminEdited"));
    }

    @Test
    void teacherProfile_studentIdInvalid_returns400() throws Exception {
        Teacher teacher = createTeacherAccount("phase2_teacher_bad_id", "Teacher BadId");

        mockMvc.perform(get("/api/teacher/students/{studentId}/profile", 0L)
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("studentId must be positive"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", -1L)
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(buildProfilePayload())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void teacherProfile_invalidDate_returns400() throws Exception {
        Teacher teacher = createTeacherAccount("phase2_teacher_invalid_date", "Teacher Invalid Date");
        Student student = createStudentAccount("phase2_student_invalid_date", "Amy", "Chen", "Amy");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        Map<String, Object> payload = buildProfilePayload();
        payload.put("birthday", "2008/06/01");

        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("birthday must be yyyy-mm-dd"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void teacherProfile_courseReplace_twoToOne_keepsOne() throws Exception {
        Teacher teacher = createTeacherAccount("phase2_teacher_replace", "Teacher Replace");
        Student student = createStudentAccount("phase2_student_replace", "Amy", "Chen", "Amy");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        Map<String, Object> firstPayload = buildProfilePayload();
        firstPayload.put("otherCourses", Arrays.asList(
                buildCourse("ABC Private School", "MHF4U", 93, 12, "2025-02-01", "2025-06-30"),
                buildCourse("Night School", "ENG4U", 90, 12, "2025-02-01", "2025-06-30")
        ));

        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(firstPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherCourses[1].courseCode").value("ENG4U"));

        Map<String, Object> secondPayload = buildProfilePayload();
        secondPayload.put("otherCourses", Arrays.asList(
                buildCourse("ABC Private School", "MHF4U", 95, 12, "2025-02-01", "2025-06-30")
        ));

        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(secondPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherCourses[0].courseCode").value("MHF4U"));

        List<StudentCourseRecord> records = studentCourseRecordRepository.findByStudent_IdOrderByIdAsc(student.getId());
        assertEquals(1, records.size());
    }

    private User createAdmin(String username) {
        return userRepository.save(new User(username, passwordEncoder.encode("Admin!234"), UserRole.ADMIN));
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
        teacherStudentRepository.save(new TeacherStudent(teacher, student, status, "test assignment"));
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }

    private String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private Map<String, Object> buildProfilePayload() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("legalFirstName", "Amy");
        payload.put("legalLastName", "Chen");
        payload.put("preferredName", "Amy");
        payload.put("gender", "Female");
        payload.put("birthday", "2008-06-01");
        payload.put("phone", "(647) 111-2222");
        payload.put("email", "amy@example.com");
        payload.put("statusInCanada", "PR");
        payload.put("citizenship", "Canada");
        payload.put("firstLanguage", "English");
        payload.put("firstBoardingDate", "2024-09-01");
        payload.put("oenNumber", "123456789");
        payload.put("ib", "IB DP");
        payload.put("ap", Boolean.TRUE);
        payload.put("identityFileNote", "Passport on file");
        payload.put("address", buildAddress());
        payload.put("schools", Arrays.asList(
                buildSchool("MAIN", "A High School", "2023-09-01", null),
                buildSchool("OTHER", "B High School", "2021-09-01", "2023-06-30")
        ));
        payload.put("otherCourses", Arrays.asList(
                buildCourse("ABC Private School", "MHF4U", 93, 12, "2025-02-01", "2025-06-30")
        ));
        return payload;
    }

    private Map<String, Object> buildAddress() {
        Map<String, Object> address = new LinkedHashMap<String, Object>();
        address.put("streetAddress", "123 Main St");
        address.put("streetAddressLine2", "Unit 5");
        address.put("city", "Toronto");
        address.put("state", "ON");
        address.put("country", "Canada");
        address.put("postal", "M1M1M1");
        return address;
    }

    private Map<String, Object> buildSchool(String schoolType,
                                            String schoolName,
                                            String startTime,
                                            String endTime) {
        Map<String, Object> school = new LinkedHashMap<String, Object>();
        school.put("schoolType", schoolType);
        school.put("schoolName", schoolName);
        school.put("startTime", startTime);
        school.put("endTime", endTime);
        return school;
    }

    private Map<String, Object> buildCourse(String schoolName,
                                            String courseCode,
                                            Integer mark,
                                            Integer gradeLevel,
                                            String startTime,
                                            String endTime) {
        Map<String, Object> course = new LinkedHashMap<String, Object>();
        course.put("schoolName", schoolName);
        course.put("courseCode", courseCode);
        course.put("mark", mark);
        course.put("gradeLevel", gradeLevel);
        course.put("startTime", startTime);
        course.put("endTime", endTime);
        return course;
    }
}
