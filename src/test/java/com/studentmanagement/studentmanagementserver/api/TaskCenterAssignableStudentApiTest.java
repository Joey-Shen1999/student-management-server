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
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskCenterAssignableStudentApiTest {

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
    void createGoal_archivedAssignment_returnsStudentArchived() throws Exception {
        Teacher teacher = createTeacherAccount("assignable_teacher_archived_relation", "Archived Relation Teacher");
        Student archivedStudent = createStudentAccount("assignable_student_archived_relation", "Archived", "Relation", "AR");
        assignTeacherStudent(teacher, archivedStudent, TeacherStudentStatus.ARCHIVED);

        mockMvc.perform(post("/api/teacher/tasks/goals")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + archivedStudent.getId() + "," +
                                "\"title\":\"Goal title\"," +
                                "\"description\":\"Goal description\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STUDENT_ARCHIVED"));
    }

    @Test
    void createGoal_archivedStudentAccount_returnsStudentArchived() throws Exception {
        Teacher teacher = createTeacherAccount("assignable_teacher_archived_account", "Archived Account Teacher");
        Student archivedStudent = createStudentAccount("assignable_student_archived_account", "Archived", "Account", "AA");
        assignTeacherStudent(teacher, archivedStudent, TeacherStudentStatus.ACTIVE);
        archivedStudent.getUser().updateStatus(UserAccountStatus.ARCHIVED, teacher.getUser().getId());
        userRepository.save(archivedStudent.getUser());

        mockMvc.perform(post("/api/teacher/tasks/goals")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + archivedStudent.getId() + "," +
                                "\"title\":\"Goal title\"," +
                                "\"description\":\"Goal description\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STUDENT_ARCHIVED"));
    }

    @Test
    void listAssignableStudents_returnsStatusSelectableAndSummaryFields() throws Exception {
        Teacher teacher = createTeacherAccount("assignable_teacher_summary", "Summary Teacher");
        Student activeStudent = createStudentAccount("assignable_student_summary_active", "Active", "Student", "AS");
        Student archivedStudent = createStudentAccount("assignable_student_summary_archived", "Archived", "Student", "XS");
        assignTeacherStudent(teacher, activeStudent, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, archivedStudent, TeacherStudentStatus.ARCHIVED);

        StudentProfile profile = new StudentProfile(activeStudent);
        profile.setEmail("active.student@example.com");
        profile.setPhone("+1-647-000-0000");
        profile.setStatusInCanada("Study Permit");
        profile.setGender("female");
        profile.setCitizenship("Chinese");
        profile.setFirstLanguage("Mandarin");
        profile.setTeacherNote("prefers STEM");
        profile.setCountry("Canada");
        profile.setState("Ontario");
        profile.setCity("Toronto");
        studentProfileRepository.save(profile);

        studentSchoolRecordRepository.save(new StudentSchoolRecord(
                activeStudent,
                SchoolType.MAIN,
                "Example High School",
                "TDSB",
                "123 Main St",
                "Toronto",
                "Ontario",
                "Canada",
                "M1M1M1",
                LocalDate.of(2023, 9, 1),
                LocalDate.of(2027, 6, 30)
        ));

        MvcResult result = mockMvc.perform(get("/api/teacher/tasks/assignable-students")
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString());

        JsonNode activeRow = findByStudentId(items, activeStudent.getId());
        JsonNode archivedRow = findByStudentId(items, archivedStudent.getId());
        assertNotNull(activeRow);
        assertNotNull(archivedRow);

        assertEquals("ACTIVE", activeRow.path("status").asText());
        assertTrue(activeRow.path("selectable").asBoolean());
        assertEquals("active.student@example.com", activeRow.path("email").asText());
        assertEquals("+1-647-000-0000", activeRow.path("phone").asText());
        assertEquals("2027-06", activeRow.path("graduation").asText());
        assertEquals("Example High School", activeRow.path("schoolName").asText());
        assertEquals("Study Permit", activeRow.path("canadaIdentity").asText());
        assertEquals("Female", activeRow.path("gender").asText());
        assertEquals("Chinese", activeRow.path("nationality").asText());
        assertEquals("Mandarin", activeRow.path("firstLanguage").asText());
        assertEquals("TDSB", activeRow.path("schoolBoard").asText());
        assertEquals("Canada", activeRow.path("country").asText());
        assertEquals("Ontario", activeRow.path("province").asText());
        assertEquals("Toronto", activeRow.path("city").asText());
        assertEquals("prefers STEM", activeRow.path("teacherNote").asText());

        assertEquals("ARCHIVED", archivedRow.path("status").asText());
        assertEquals(false, archivedRow.path("selectable").asBoolean());
    }

    @Test
    void listAssignableStudents_teacherSeesOnlyOwnAssignments() throws Exception {
        Teacher teacherA = createTeacherAccount("assignable_teacher_scope_a", "Teacher A");
        Teacher teacherB = createTeacherAccount("assignable_teacher_scope_b", "Teacher B");
        Student studentA = createStudentAccount("assignable_scope_student_a", "Scope", "A", "A");
        Student studentB = createStudentAccount("assignable_scope_student_b", "Scope", "B", "B");
        assignTeacherStudent(teacherA, studentA, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacherB, studentB, TeacherStudentStatus.ACTIVE);

        MvcResult result = mockMvc.perform(get("/api/teacher/tasks/assignable-students")
                        .header("Authorization", bearerFor(teacherA.getUser())))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString());

        assertNotNull(findByStudentId(items, studentA.getId()));
        assertNull(findByStudentId(items, studentB.getId()));
    }

    private JsonNode findByStudentId(JsonNode items, Long studentId) {
        if (items == null || !items.isArray() || studentId == null) {
            return null;
        }
        for (JsonNode item : items) {
            if (item.path("studentId").asLong() == studentId.longValue()) {
                return item;
            }
        }
        return null;
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
        teacherStudentRepository.save(new TeacherStudent(teacher, student, status, "assignable-student-test-assignment"));
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
