package com.studentmanagement.studentmanagementserver.api;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentDocumentsApiTest {

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
    void studentDocuments_dedicatedPageUploadListViewDelete_flowWorks() throws Exception {
        Student student = createStudentAccount("student_documents_flow_student", "Amy", "Chen", "Amy");
        String bearer = bearerFor(student.getUser());

        byte[] passportBytes = "passport pdf bytes".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile passport = new MockMultipartFile(
                "file",
                "passport.pdf",
                "application/pdf",
                passportBytes
        );

        MvcResult identityUploadResult = mockMvc.perform(multipart("/api/student/documents")
                        .file(passport)
                        .param("documentCategory", "Identity Document")
                        .param("identityDocumentType", "Passport")
                        .param("title", "Passport Copy")
                        .param("notes", "bio page")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentCategory").value("Identity Document"))
                .andExpect(jsonPath("$.identityDocumentType").value("Passport"))
                .andExpect(jsonPath("$.title").value("Passport Copy"))
                .andExpect(jsonPath("$.uploadedByRole").value("STUDENT"))
                .andExpect(jsonPath("$.uploadedByName").value("Amy"))
                .andReturn();
        long identityDocumentId = objectMapper.readTree(identityUploadResult.getResponse().getContentAsString())
                .path("id")
                .asLong();

        byte[] reportCardBytes = "report card pdf bytes".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile reportCard = new MockMultipartFile(
                "file",
                "report-card.pdf",
                "application/pdf",
                reportCardBytes
        );
        mockMvc.perform(multipart("/api/student/documents")
                        .file(reportCard)
                        .param("documentCategory", "Academic Record")
                        .param("academicRecordType", "Report Card")
                        .param("reportYear", "2026")
                        .param("reportMonth", "June")
                        .param("title", "June 2026 Report Card")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentCategory").value("Academic Record"))
                .andExpect(jsonPath("$.academicRecordType").value("Report Card"))
                .andExpect(jsonPath("$.reportYear").value(2026))
                .andExpect(jsonPath("$.reportMonth").value("June"));

        mockMvc.perform(get("/api/student/documents")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].title").isString());

        mockMvc.perform(get("/api/student/documents/{documentId}/file", identityDocumentId)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes(passportBytes));

        mockMvc.perform(delete("/api/student/documents/{documentId}", identityDocumentId)
                        .header("Authorization", bearer))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/student/documents")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void studentDocuments_profileTranscriptUpload_createsSharedAcademicDocument() throws Exception {
        Student student = createStudentAccount("student_documents_profile_transcript", "Amy", "Chen", "Amy");
        String bearer = bearerFor(student.getUser());

        MvcResult saveResult = mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(buildProfilePayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schools[0].schoolRecordId").isNumber())
                .andReturn();
        long schoolRecordId = objectMapper.readTree(saveResult.getResponse().getContentAsString())
                .path("schools")
                .path(0)
                .path("schoolRecordId")
                .asLong();

        MockMultipartFile transcript = new MockMultipartFile(
                "file",
                "grade11-report-card.pdf",
                "application/pdf",
                "grade11 report card".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/student/profile/schools/{schoolRecordId}/transcript", schoolRecordId)
                        .file(transcript)
                        .param("academicRecordType", "Report Card")
                        .param("reportYear", "2025")
                        .param("reportMonth", "December")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.academicRecordType").value("Report Card"))
                .andExpect(jsonPath("$.reportYear").value(2025))
                .andExpect(jsonPath("$.reportMonth").value("December"));

        mockMvc.perform(get("/api/student/documents")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentCategory").value("Academic Record"))
                .andExpect(jsonPath("$[0].academicRecordType").value("Report Card"))
                .andExpect(jsonPath("$[0].reportYear").value(2025))
                .andExpect(jsonPath("$[0].reportMonth").value("December"));
    }

    @Test
    void teacherDocuments_assignedTeacher_canListAndEmptyArray() throws Exception {
        Teacher teacher = createTeacherAccount("student_documents_teacher_assigned", "Teacher Assigned");
        Student student = createStudentAccount("student_documents_teacher_student", "Amy", "Chen", "Amy");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        String teacherBearer = bearerFor(teacher.getUser());

        mockMvc.perform(get("/api/teacher/students/{studentId}/documents", student.getId())
                        .header("Authorization", teacherBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        MockMultipartFile passport = new MockMultipartFile(
                "file",
                "teacher-view-passport.pdf",
                "application/pdf",
                "teacher view passport".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/student/documents")
                        .file(passport)
                        .param("documentCategory", "Identity Document")
                        .param("identityDocumentType", "Passport")
                        .param("title", "Passport Copy")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/teacher/students/{studentId}/documents", student.getId())
                        .header("Authorization", teacherBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].documentCategory").value("Identity Document"))
                .andExpect(jsonPath("$[0].identityDocumentType").value("Passport"))
                .andExpect(jsonPath("$[0].fileName").value("teacher-view-passport.pdf"));
    }

    @Test
    void teacherDocuments_unassignedTeacher_forbidden403() throws Exception {
        Teacher teacher = createTeacherAccount("student_documents_teacher_unassigned", "Teacher Unassigned");
        Student student = createStudentAccount("student_documents_unassigned_student", "Amy", "Chen", "Amy");

        mockMvc.perform(get("/api/teacher/students/{studentId}/documents", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void teacherDocuments_admin_canListAnyStudent() throws Exception {
        User admin = createAdmin("student_documents_admin");
        Student student = createStudentAccount("student_documents_admin_student", "Amy", "Chen", "Amy");

        mockMvc.perform(get("/api/teacher/students/{studentId}/documents", student.getId())
                        .header("Authorization", bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void teacherDocuments_assignedTeacher_canUploadViewAndDelete() throws Exception {
        Teacher teacher = createTeacherAccount("student_documents_teacher_ops", "Teacher Ops");
        Student student = createStudentAccount("student_documents_teacher_ops_student", "Amy", "Chen", "Amy");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        String teacherBearer = bearerFor(teacher.getUser());

        byte[] transcriptBytes = "teacher uploaded transcript".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile transcript = new MockMultipartFile(
                "file",
                "teacher-uploaded-transcript.pdf",
                "application/pdf",
                transcriptBytes
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/teacher/students/{studentId}/documents", student.getId())
                        .file(transcript)
                        .param("documentCategory", "Academic Record")
                        .param("academicRecordType", "Transcript")
                        .param("reportYear", "2026")
                        .param("reportMonth", "fall")
                        .param("title", "Teacher Uploaded Transcript")
                        .param("notes", "uploaded by teacher")
                        .header("Authorization", teacherBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentCategory").value("Academic Record"))
                .andExpect(jsonPath("$.academicRecordType").value("Transcript"))
                .andExpect(jsonPath("$.reportYear").value(2026))
                .andExpect(jsonPath("$.reportMonth").value("fall"))
                .andExpect(jsonPath("$.title").value("Teacher Uploaded Transcript"))
                .andExpect(jsonPath("$.uploadedByRole").value("TEACHER"))
                .andExpect(jsonPath("$.uploadedByName").value("Teacher Ops"))
                .andReturn();

        long documentId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("id")
                .asLong();

        mockMvc.perform(get("/api/teacher/students/{studentId}/documents/{documentId}/file",
                        student.getId(),
                        documentId)
                        .header("Authorization", teacherBearer))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes(transcriptBytes));

        mockMvc.perform(delete("/api/teacher/students/{studentId}/documents/{documentId}",
                        student.getId(),
                        documentId)
                        .header("Authorization", teacherBearer))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/teacher/students/{studentId}/documents", student.getId())
                        .header("Authorization", teacherBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void studentDocuments_historyShowsTeacherUploadAndDeleteActionsToStudent() throws Exception {
        Teacher teacher = createTeacherAccount("student_documents_history_teacher", "History Teacher");
        Student student = createStudentAccount("student_documents_history_student", "Amy", "Chen", "Amy");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        String teacherBearer = bearerFor(teacher.getUser());
        String studentBearer = bearerFor(student.getUser());

        MockMultipartFile transcript = new MockMultipartFile(
                "file",
                "teacher-history-transcript.pdf",
                "application/pdf",
                "history transcript".getBytes(StandardCharsets.UTF_8)
        );
        MvcResult uploadResult = mockMvc.perform(multipart("/api/teacher/students/{studentId}/documents", student.getId())
                        .file(transcript)
                        .param("documentCategory", "Academic Record")
                        .param("academicRecordType", "Transcript")
                        .param("reportYear", "2026")
                        .param("reportMonth", "fall")
                        .param("title", "Teacher History Transcript")
                        .header("Authorization", teacherBearer))
                .andExpect(status().isOk())
                .andReturn();
        long documentId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("id")
                .asLong();

        mockMvc.perform(get("/api/student/documents/history")
                        .header("Authorization", studentBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].action").value("UPLOAD"))
                .andExpect(jsonPath("$.items[0].documentId").value(documentId))
                .andExpect(jsonPath("$.items[0].actorRole").value("TEACHER"))
                .andExpect(jsonPath("$.items[0].actorName").value("History Teacher"))
                .andExpect(jsonPath("$.items[0].fileName").value("teacher-history-transcript.pdf"));

        mockMvc.perform(get("/api/student/documents")
                        .header("Authorization", studentBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(documentId))
                .andExpect(jsonPath("$[0].reportYear").value(2026))
                .andExpect(jsonPath("$[0].reportMonth").value("fall"))
                .andExpect(jsonPath("$[0].uploadedByRole").value("TEACHER"))
                .andExpect(jsonPath("$[0].uploadedByName").value("History Teacher"));

        mockMvc.perform(delete("/api/teacher/students/{studentId}/documents/{documentId}",
                        student.getId(),
                        documentId)
                        .header("Authorization", teacherBearer))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/student/documents/history")
                        .header("Authorization", studentBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].action").value("DELETE"))
                .andExpect(jsonPath("$.items[0].documentId").value(documentId))
                .andExpect(jsonPath("$.items[0].actorRole").value("TEACHER"))
                .andExpect(jsonPath("$.items[0].actorName").value("History Teacher"))
                .andExpect(jsonPath("$.items[1].action").value("UPLOAD"));
    }

    @Test
    void teacherDocuments_historyUsesTeacherAssignmentPermission() throws Exception {
        Teacher assignedTeacher = createTeacherAccount("student_documents_history_assigned", "Assigned History Teacher");
        Teacher unassignedTeacher = createTeacherAccount("student_documents_history_unassigned", "Unassigned History Teacher");
        Student student = createStudentAccount("student_documents_history_target", "Amy", "Chen", "Amy");
        assignTeacherStudent(assignedTeacher, student, TeacherStudentStatus.ACTIVE);

        MockMultipartFile passport = new MockMultipartFile(
                "file",
                "history-passport.pdf",
                "application/pdf",
                "history passport".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/student/documents")
                        .file(passport)
                        .param("documentCategory", "Identity Document")
                        .param("identityDocumentType", "Passport")
                        .param("title", "Passport")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/teacher/students/{studentId}/documents/history", student.getId())
                        .header("Authorization", bearerFor(assignedTeacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].action").value("UPLOAD"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/documents/history", student.getId())
                        .header("Authorization", bearerFor(unassignedTeacher.getUser())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void teacherDocuments_unassignedTeacher_forbiddenForUploadViewAndDelete() throws Exception {
        Teacher teacher = createTeacherAccount("student_documents_teacher_forbidden_ops", "Teacher Forbidden");
        Student student = createStudentAccount("student_documents_teacher_forbidden_student", "Amy", "Chen", "Amy");
        String teacherBearer = bearerFor(teacher.getUser());

        byte[] passportBytes = "student passport".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile studentPassport = new MockMultipartFile(
                "file",
                "student-passport.pdf",
                "application/pdf",
                passportBytes
        );
        MvcResult studentUploadResult = mockMvc.perform(multipart("/api/student/documents")
                        .file(studentPassport)
                        .param("documentCategory", "Identity Document")
                        .param("identityDocumentType", "Passport")
                        .param("title", "Passport Copy")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andReturn();
        long documentId = objectMapper.readTree(studentUploadResult.getResponse().getContentAsString())
                .path("id")
                .asLong();

        MockMultipartFile teacherUpload = new MockMultipartFile(
                "file",
                "forbidden.pdf",
                "application/pdf",
                "forbidden upload".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/teacher/students/{studentId}/documents", student.getId())
                        .file(teacherUpload)
                        .param("documentCategory", "Other")
                        .param("title", "Forbidden")
                        .header("Authorization", teacherBearer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/documents/{documentId}/file",
                        student.getId(),
                        documentId)
                        .header("Authorization", teacherBearer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(delete("/api/teacher/students/{studentId}/documents/{documentId}",
                        student.getId(),
                        documentId)
                        .header("Authorization", teacherBearer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private Student createStudentAccount(String username, String firstName, String lastName, String nickName) {
        User user = userRepository.save(new User(username, passwordEncoder.encode("Student!234"), UserRole.STUDENT));
        return studentRepository.save(new Student(user, firstName, lastName, nickName));
    }

    private User createAdmin(String username) {
        return userRepository.save(new User(username, passwordEncoder.encode("Admin!234"), UserRole.ADMIN));
    }

    private Teacher createTeacherAccount(String username, String displayName) {
        User user = userRepository.save(new User(username, passwordEncoder.encode("Teacher!234"), UserRole.TEACHER));
        return teacherRepository.save(new Teacher(user, displayName));
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
        payload.put("studentRegion", "Ontario");
        payload.put("oenNumber", "123456789");
        payload.put("ib", "IB DP");
        payload.put("ap", Boolean.TRUE);
        payload.put("schools", Arrays.asList(
                buildSchool("MAIN", "A High School", "2023-09-01", null)
        ));
        payload.put("otherCourses", Arrays.asList());
        return payload;
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
}
