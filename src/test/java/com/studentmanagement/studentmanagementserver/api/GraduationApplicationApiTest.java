package com.studentmanagement.studentmanagementserver.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherStudent;
import com.studentmanagement.studentmanagementserver.domain.university.University;
import com.studentmanagement.studentmanagementserver.domain.university.UniversityProgram;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherStudentRepository;
import com.studentmanagement.studentmanagementserver.repo.UniversityProgramRepository;
import com.studentmanagement.studentmanagementserver.repo.UniversityRepository;
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

import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GraduationApplicationApiTest {

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
    private UniversityRepository universityRepository;

    @Autowired
    private UniversityProgramRepository universityProgramRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void teacherConfirmApplications_studentCanReadProgress() throws Exception {
        Teacher teacher = createTeacherAccount("graduation_teacher_confirm", "Graduation Teacher");
        Student student = createStudentAccount("graduation_student_confirm", "Grad", "Student", "Confirm");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        University university = universityRepository.save(new University(
                "Graduation Test University",
                "Ontario",
                "Toronto",
                "Canada",
                null
        ));
        UniversityProgram program = universityProgramRepository.save(new UniversityProgram(
                university,
                "Computer Science",
                "Faculty of Arts and Science",
                "BSc"
        ));

        mockMvc.perform(put("/api/students/{studentId}/graduation-applications/confirm", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmPayload(
                                university.getId(),
                                program.getId(),
                                "READY_TO_SUBMIT"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(student.getId()))
                .andExpect(jsonPath("$[0].universityName").value("Graduation Test University"))
                .andExpect(jsonPath("$[0].programName").value("Computer Science"))
                .andExpect(jsonPath("$[0].status").value("READY_TO_SUBMIT"))
                .andExpect(jsonPath("$[0].sortOrder").value(1));

        mockMvc.perform(get("/api/students/{studentId}/graduation-applications", student.getId())
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].universityName").value("Graduation Test University"))
                .andExpect(jsonPath("$[0].programName").value("Computer Science"))
                .andExpect(jsonPath("$[0].status").value("READY_TO_SUBMIT"));
    }

    @Test
    void confirmRejectsProgramFromDifferentUniversity() throws Exception {
        Teacher teacher = createTeacherAccount("graduation_teacher_mismatch", "Graduation Mismatch Teacher");
        Student student = createStudentAccount("graduation_student_mismatch", "Grad", "Student", "Mismatch");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        University universityA = universityRepository.save(new University("Graduation University A", "Ontario", "Toronto", "Canada", null));
        University universityB = universityRepository.save(new University("Graduation University B", "Ontario", "Waterloo", "Canada", null));
        UniversityProgram programB = universityProgramRepository.save(new UniversityProgram(universityB, "Engineering", null, null));

        mockMvc.perform(put("/api/students/{studentId}/graduation-applications/confirm", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmPayload(
                                universityA.getId(),
                                programB.getId(),
                                "PREPARING"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void graduationApplicationMutationsAreAudited() throws Exception {
        Teacher teacher = createTeacherAccount("graduation_teacher_audit", "Graduation Audit Teacher");
        Student student = createStudentAccount("graduation_student_audit", "Grad", "Student", "Audit");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        University university = universityRepository.save(new University(
                "Graduation Audit University",
                "Ontario",
                "Toronto",
                "Canada",
                null
        ));
        UniversityProgram program = universityProgramRepository.save(new UniversityProgram(
                university,
                "Computer Science",
                "Faculty of Arts and Science",
                "BSc"
        ));
        UniversityProgram secondProgram = universityProgramRepository.save(new UniversityProgram(
                university,
                "Life Sciences",
                "Faculty of Arts and Science",
                "BSc"
        ));
        String teacherBearer = bearerFor(teacher.getUser());

        MvcResult confirmResult = mockMvc.perform(put("/api/students/{studentId}/graduation-applications/confirm", student.getId())
                        .header("Authorization", teacherBearer)
                        .header("X-Trace-Id", "graduation-audit-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmPayload(
                                university.getId(),
                                program.getId(),
                                "READY_TO_SUBMIT"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        List<Map<String, Object>> confirmed = objectMapper.readValue(
                confirmResult.getResponse().getContentAsString(),
                new TypeReference<List<Map<String, Object>>>() {
                }
        );
        Long applicationId = Long.valueOf(String.valueOf(confirmed.get(0).get("id")));

        Map<String, Object> updatePayload = new LinkedHashMap<String, Object>();
        updatePayload.put("universityId", university.getId());
        updatePayload.put("programId", secondProgram.getId());
        updatePayload.put("status", "SUBMITTED");
        mockMvc.perform(put("/api/graduation-applications/{applicationId}", applicationId)
                        .header("Authorization", teacherBearer)
                        .header("X-Trace-Id", "graduation-audit-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programName").value("Life Sciences"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        mockMvc.perform(delete("/api/graduation-applications/{applicationId}", applicationId)
                        .header("Authorization", teacherBearer)
                        .header("X-Trace-Id", "graduation-audit-delete"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/students/{studentId}/graduation-applications/history", student.getId())
                        .header("Authorization", teacherBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.items[0].operation").value("DELETE_APPLICATION"))
                .andExpect(jsonPath("$.items[0].actorName").value("Graduation Audit Teacher"))
                .andExpect(jsonPath("$.items[0].changedFields[0].path").value("application"))
                .andExpect(jsonPath("$.items[1].operation").value("UPDATE_APPLICATION"))
                .andExpect(jsonPath("$.items[1].changedFields[0].path").value("program"))
                .andExpect(jsonPath("$.items[2].operation").value("CONFIRM_STAGE"))
                .andExpect(jsonPath("$.items[2].changedFields[0].path").value("applications"))
                .andExpect(jsonPath("$.items[3].operation").value("ENTER_GRADUATION_STAGE"))
                .andExpect(jsonPath("$.items[3].changedFields[0].path").value("graduationStage"))
                .andExpect(jsonPath("$.items[3].changedFields[0].before").value(false))
                .andExpect(jsonPath("$.items[3].changedFields[0].after").value(true));
    }

    private Map<String, Object> confirmPayload(Long universityId, Long programId, String status) {
        Map<String, Object> application = new LinkedHashMap<String, Object>();
        application.put("universityId", universityId);
        application.put("programId", programId);
        application.put("status", status);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("applications", Arrays.asList(application));
        return payload;
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
        teacherStudentRepository.save(new TeacherStudent(teacher, student, status, "graduation-application-api-test"));
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
