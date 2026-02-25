package com.studentmanagement.studentmanagementserver.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.student.StudentCourseRecord;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentCourseRecordRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentProfileRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentProfileApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private StudentCourseRecordRepository studentCourseRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getProfile_withStudentToken_returns200AndDefaultStructure() throws Exception {
        Student student = createStudentAccount("profile_get_student", "Amy", "Chen", "Amy");

        mockMvc.perform(get("/api/student/profile")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalFirstName").value("Amy"))
                .andExpect(jsonPath("$.legalLastName").value("Chen"))
                .andExpect(jsonPath("$.preferredName").value("Amy"))
                .andExpect(jsonPath("$.firstName").value("Amy"))
                .andExpect(jsonPath("$.lastName").value("Chen"))
                .andExpect(jsonPath("$.nickName").value("Amy"))
                .andExpect(jsonPath("$.ap").value(false))
                .andExpect(jsonPath("$.address").isMap())
                .andExpect(jsonPath("$.schools").isArray())
                .andExpect(jsonPath("$.schools").isEmpty())
                .andExpect(jsonPath("$.otherCourses").isArray())
                .andExpect(jsonPath("$.otherCourses").isEmpty())
                .andExpect(jsonPath("$.schoolRecords").isArray())
                .andExpect(jsonPath("$.externalCourses").isArray());
    }

    @Test
    void putProfile_withValidBody_returns200AndPersists() throws Exception {
        Student student = createStudentAccount("profile_put_student", "Legacy", "Name", "Nick");

        Map<String, Object> payload = buildProfilePayload(
                "  Amy ",
                " Chen  ",
                "  A. Chen ",
                true,
                Arrays.asList(
                        buildSchool("MAIN", "A High School", "2023-09-01", null),
                        buildSchool("OTHER", "B High School", "2021-09-01", "2023-06-30")
                ),
                Arrays.asList(
                        buildCourse("Summer School C", "MHF4U", 93, 12, "2025-07-02", "2025-08-20")
                )
        );

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalFirstName").value("Amy"))
                .andExpect(jsonPath("$.legalLastName").value("Chen"))
                .andExpect(jsonPath("$.preferredName").value("A. Chen"))
                .andExpect(jsonPath("$.address.streetAddress").value("123 Main St"))
                .andExpect(jsonPath("$.schools[0].schoolType").value("MAIN"))
                .andExpect(jsonPath("$.schools[0].schoolName").value("A High School"))
                .andExpect(jsonPath("$.otherCourses[0].courseCode").value("MHF4U"))
                .andExpect(jsonPath("$.ap").value(true));

        Student updatedStudent = studentRepository.findById(student.getId())
                .orElseThrow(() -> new RuntimeException("student not found after update"));
        assertEquals("Amy", updatedStudent.getFirstName());
        assertEquals("Chen", updatedStudent.getLastName());
        assertEquals("A. Chen", updatedStudent.getNickName());
        assertTrue(studentProfileRepository.findByStudent_Id(student.getId()).isPresent());

        mockMvc.perform(get("/api/student/profile")
                        .header("Authorization", bearerFor(updatedStudent.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("amy@example.com"))
                .andExpect(jsonPath("$.birthday").value("2008-06-01"))
                .andExpect(jsonPath("$.firstBoardingDate").value("2024-09-01"))
                .andExpect(jsonPath("$.schools[1].schoolType").value("OTHER"))
                .andExpect(jsonPath("$.otherCourses[0].mark").value(93))
                .andExpect(jsonPath("$.otherCourses[0].gradeLevel").value(12))
                .andExpect(jsonPath("$.externalCourses[0].courseCode").value("MHF4U"))
                .andExpect(jsonPath("$.schoolRecords[0].schoolType").value("MAIN"));
    }

    @Test
    void putProfile_invalidDate_returns400() throws Exception {
        Student student = createStudentAccount("profile_invalid_date_student", "Amy", "Chen", "Amy");

        Map<String, Object> payload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                new ArrayList<Map<String, Object>>(),
                new ArrayList<Map<String, Object>>()
        );
        payload.put("birthday", "2008/06/01");

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("birthday must be yyyy-mm-dd"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void putProfile_aliasLists_schoolRecordsAndExternalCourses_supported() throws Exception {
        Student student = createStudentAccount("profile_alias_lists_student", "Amy", "Chen", "Amy");

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("legalFirstName", "Amy");
        payload.put("legalLastName", "Chen");
        payload.put("preferredName", "Amy");
        payload.put("birthday", "2008-06-01");
        payload.put("firstBoardingDate", "2024-09-01");
        payload.put("ap", Boolean.FALSE);
        payload.put("address", buildAddress());
        payload.put("schoolRecords", Arrays.asList(
                buildSchool("MAIN", "A High School", "2023-09-01", null)
        ));
        payload.put("externalCourses", Arrays.asList(
                buildCourse("Summer School C", "MHF4U", 95, 12, "2025-07-02", "2025-08-20")
        ));

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schools[0].schoolName").value("A High School"))
                .andExpect(jsonPath("$.otherCourses[0].courseCode").value("MHF4U"))
                .andExpect(jsonPath("$.schoolRecords[0].schoolName").value("A High School"))
                .andExpect(jsonPath("$.externalCourses[0].courseCode").value("MHF4U"));
    }

    @Test
    void getProfile_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/student/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthenticated."))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getProfile_withTeacherToken_returns403() throws Exception {
        User teacher = userRepository.save(
                new User("profile_teacher_forbidden", passwordEncoder.encode("Teacher!234"), UserRole.TEACHER)
        );

        mockMvc.perform(get("/api/student/profile")
                        .header("Authorization", bearerFor(teacher)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Forbidden: student role required."))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void putProfile_saveWithTwoThenOneCourse_removesOldExtraCourse() throws Exception {
        Student student = createStudentAccount("profile_replace_courses_student", "Amy", "Chen", "Amy");
        String bearer = bearerFor(student.getUser());

        Map<String, Object> firstPayload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                Arrays.asList(
                        buildSchool("MAIN", "A High School", "2023-09-01", null)
                ),
                Arrays.asList(
                        buildCourse("ABC Private School", "MHF4U", 93, 12, "2025-02-01", "2025-06-30"),
                        buildCourse("Night School", "ENG4U", 90, 12, "2025-02-01", "2025-06-30")
                )
        );

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(firstPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherCourses[0].courseCode").value("MHF4U"))
                .andExpect(jsonPath("$.otherCourses[1].courseCode").value("ENG4U"));

        Map<String, Object> secondPayload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                Arrays.asList(
                        buildSchool("MAIN", "A High School", "2023-09-01", null)
                ),
                Arrays.asList(
                        buildCourse("ABC Private School", "MHF4U", 95, 12, "2025-02-01", "2025-06-30")
                )
        );

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(secondPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherCourses").isArray())
                .andExpect(jsonPath("$.otherCourses[0].courseCode").value("MHF4U"));

        List<StudentCourseRecord> records = studentCourseRecordRepository.findByStudent_IdOrderByIdAsc(student.getId());
        assertEquals(1, records.size());
        assertEquals("MHF4U", records.get(0).getCourseCode());
        assertEquals(Integer.valueOf(95), records.get(0).getMark());
    }

    private Student createStudentAccount(String username, String firstName, String lastName, String nickName) {
        User user = userRepository.save(new User(username, passwordEncoder.encode("Student!234"), UserRole.STUDENT));
        return studentRepository.save(new Student(user, firstName, lastName, nickName));
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }

    private String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private Map<String, Object> buildProfilePayload(String legalFirstName,
                                                    String legalLastName,
                                                    String preferredName,
                                                    Boolean ap,
                                                    List<Map<String, Object>> schools,
                                                    List<Map<String, Object>> courses) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("legalFirstName", legalFirstName);
        payload.put("legalLastName", legalLastName);
        payload.put("preferredName", preferredName);
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
        payload.put("ap", ap);
        payload.put("identityFileNote", "Passport on file");
        payload.put("address", buildAddress());
        payload.put("schools", schools);
        payload.put("otherCourses", courses);
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
