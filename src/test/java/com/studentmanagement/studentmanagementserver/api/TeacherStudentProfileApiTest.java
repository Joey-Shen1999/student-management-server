package com.studentmanagement.studentmanagementserver.api;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        payload.put("gender", "Other");
        payload.put("genderOther", "Non-binary");
        payload.put("teacherNote", "  Follow up transcript in April  ");
        payload.put("serviceProjects", Arrays.asList("A: 面试辅导", "B: 雅思A类全科班"));

        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalFirstName").value("TeacherEdited"))
                .andExpect(jsonPath("$.preferredName").value("TE"))
                .andExpect(jsonPath("$.gender").value("Other"))
                .andExpect(jsonPath("$.genderOther").value("Non-binary"))
                .andExpect(jsonPath("$.teacherNote").value("Follow up transcript in April"))
                .andExpect(jsonPath("$.serviceItems[0]").value("面试辅导"))
                .andExpect(jsonPath("$.serviceItems[1]").value("雅思A类全科班"))
                .andExpect(jsonPath("$.serviceProjects[0]").value("面试辅导"))
                .andExpect(jsonPath("$.serviceProjects[1]").value("雅思A类全科班"))
                .andExpect(jsonPath("$.otherCourses[0].courseCode").value("MHF4U"))
                .andExpect(jsonPath("$.externalCourses[0].courseCode").value("MHF4U"))
                .andExpect(jsonPath("$.schoolRecords[0].schoolType").value("MAIN"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherNote").value("Follow up transcript in April"))
                .andExpect(jsonPath("$.serviceItems[0]").value("面试辅导"))
                .andExpect(jsonPath("$.serviceProjects[0]").value("面试辅导"));
    }

    @Test
    void teacherProfile_serviceItemsQuickUpdate_omittingChildCollections_persistsAndPreservesRows() throws Exception {
        Teacher teacher = createTeacherAccount("phase2_teacher_service_quick", "Teacher Service Quick");
        Student student = createStudentAccount("phase2_student_service_quick", "Amy", "Chen", "Amy");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        String bearer = bearerFor(teacher.getUser());

        Map<String, Object> initialPayload = buildProfilePayload();
        initialPayload.put("serviceItems", Arrays.asList("面试辅导"));
        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(initialPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceItems[0]").value("面试辅导"))
                .andExpect(jsonPath("$.schools[0].schoolName").value("A High School"))
                .andExpect(jsonPath("$.otherCourses[0].courseCode").value("MHF4U"));

        MvcResult getResult = mockMvc.perform(get("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        Map<String, Object> quickPayload = objectMapper.readValue(
                getResult.getResponse().getContentAsString(),
                Map.class
        );
        quickPayload.remove("schools");
        quickPayload.remove("schoolRecords");
        quickPayload.remove("identityFiles");
        quickPayload.put("serviceItems", Arrays.asList("面试辅导", "一对一辅导"));
        quickPayload.put("serviceProjects", Arrays.asList("面试辅导", "一对一辅导"));

        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(quickPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceItems.length()").value(2))
                .andExpect(jsonPath("$.serviceItems[1]").value("一对一辅导"))
                .andExpect(jsonPath("$.serviceProjects[1]").value("一对一辅导"))
                .andExpect(jsonPath("$.schools[0].schoolName").value("A High School"))
                .andExpect(jsonPath("$.otherCourses[0].courseCode").value("MHF4U"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceItems.length()").value(2))
                .andExpect(jsonPath("$.serviceItems[1]").value("一对一辅导"))
                .andExpect(jsonPath("$.serviceProjects[1]").value("一对一辅导"))
                .andExpect(jsonPath("$.schools[0].schoolName").value("A High School"))
                .andExpect(jsonPath("$.otherCourses[0].courseCode").value("MHF4U"));
    }

    @Test
    void teacherProfile_schoolBoardAliasesAndPreserveWithoutField() throws Exception {
        Teacher teacher = createTeacherAccount("phase2_teacher_school_board", "Teacher SchoolBoard");
        Student student = createStudentAccount("phase2_student_school_board", "Amy", "Chen", "Amy");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        String bearer = bearerFor(teacher.getUser());

        Map<String, Object> firstPayload = buildProfilePayload();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> firstSchools = (List<Map<String, Object>>) firstPayload.get("schools");
        firstSchools.get(0).put("boardName", "TDSB");
        firstSchools.get(1).put("educationBureau", "私校");

        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(firstPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schools[0].schoolBoard").value("TDSB"))
                .andExpect(jsonPath("$.schools[0].boardName").value("TDSB"))
                .andExpect(jsonPath("$.schools[1].schoolBoard").value("私校"))
                .andExpect(jsonPath("$.schools[1].boardName").value("私校"));

        Map<String, Object> secondPayload = buildProfilePayload();
        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(secondPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schools[0].schoolBoard").value("TDSB"))
                .andExpect(jsonPath("$.schools[0].boardName").value("TDSB"))
                .andExpect(jsonPath("$.schools[1].schoolBoard").value("私校"))
                .andExpect(jsonPath("$.schools[1].boardName").value("私校"));
    }

    @Test
    void teacherProfile_teacherAssignedActive_canUploadAndDownloadSchoolTranscript() throws Exception {
        Teacher teacher = createTeacherAccount("phase2_teacher_transcript", "Teacher Transcript");
        Student student = createStudentAccount("phase2_student_transcript", "Amy", "Chen", "Amy");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        String bearer = bearerFor(teacher.getUser());

        MvcResult saveResult = mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(buildProfilePayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schools[0].schoolRecordId").isNumber())
                .andReturn();

        JsonNode saveJson = objectMapper.readTree(saveResult.getResponse().getContentAsString());
        long schoolRecordId = saveJson.path("schools").path(0).path("schoolRecordId").asLong();

        byte[] transcriptBytes = "teacher upload transcript".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile transcript = new MockMultipartFile(
                "transcript",
                "teacher-uploaded-transcript.pdf",
                "application/pdf",
                transcriptBytes
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/teacher/students/{studentId}/profile/schools/{schoolRecordId}/transcript",
                                student.getId(),
                                schoolRecordId)
                        .file(transcript)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schoolRecordId").value(schoolRecordId))
                .andExpect(jsonPath("$.hasTranscript").value(true))
                .andExpect(jsonPath("$.transcripts.length()").value(1))
                .andReturn();
        long transcriptId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("transcripts")
                .path(0)
                .path("id")
                .asLong();

        mockMvc.perform(get("/api/teacher/students/{studentId}/profile/schools/{schoolRecordId}/transcript",
                                student.getId(),
                                schoolRecordId)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes(transcriptBytes));

        mockMvc.perform(get("/api/teacher/students/{studentId}/profile/schools/{schoolRecordId}/transcripts/{transcriptId}",
                                student.getId(),
                                schoolRecordId,
                                transcriptId)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes(transcriptBytes));
    }

    @Test
    void teacherProfile_teacherAssignedActive_canUploadAndDownloadIdentityFile() throws Exception {
        Teacher teacher = createTeacherAccount("phase2_teacher_identity", "Teacher Identity");
        Student student = createStudentAccount("phase2_student_identity", "Amy", "Chen", "Amy");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        String bearer = bearerFor(teacher.getUser());

        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(buildProfilePayload())))
                .andExpect(status().isOk());

        byte[] identityBytes = "teacher upload identity".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile identityFile = new MockMultipartFile(
                "identity",
                "teacher-uploaded-identity.pdf",
                "application/pdf",
                identityBytes
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/teacher/students/{studentId}/profile/identity-files",
                                student.getId())
                        .file(identityFile)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasIdentityFile").value(true))
                .andExpect(jsonPath("$.identityFiles.length()").value(1))
                .andReturn();
        long identityFileId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("identityFiles")
                .path(0)
                .path("id")
                .asLong();

        mockMvc.perform(get("/api/teacher/students/{studentId}/profile/identity-files/{identityFileId}",
                                student.getId(),
                                identityFileId)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes(identityBytes));
    }

    @Test
    void teacherProfile_teacherUnassigned_getAndPut403() throws Exception {
        Teacher teacher = createTeacherAccount("phase2_teacher_unassigned", "Teacher Unassigned");
        Student student = createStudentAccount("phase2_student_unassigned", "Amy", "Chen", "Amy");

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
    void teacherProfile_teacherArchivedRelation_getAndPut403() throws Exception {
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

    @Test
    void teacherProfile_teacherNote_visibleToOtherAssignedTeacher() throws Exception {
        Teacher teacherA = createTeacherAccount("phase2_teacher_note_a", "Teacher Note A");
        Teacher teacherB = createTeacherAccount("phase2_teacher_note_b", "Teacher Note B");
        Student student = createStudentAccount("phase2_student_note_shared", "Amy", "Chen", "Amy");
        assignTeacherStudent(teacherA, student, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacherB, student, TeacherStudentStatus.ACTIVE);

        Map<String, Object> payload = buildProfilePayload();
        payload.put("teacherNote", "same note for all assigned teachers");

        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearerFor(teacherA.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherNote").value("same note for all assigned teachers"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearerFor(teacherB.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherNote").value("same note for all assigned teachers"));
    }

    @Test
    void teacherProfile_teacherNote_emptyString_clearsValue() throws Exception {
        Teacher teacher = createTeacherAccount("phase2_teacher_note_clear", "Teacher Note Clear");
        Student student = createStudentAccount("phase2_student_note_clear", "Amy", "Chen", "Amy");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        String bearer = bearerFor(teacher.getUser());

        Map<String, Object> firstPayload = buildProfilePayload();
        firstPayload.put("teacherNote", "will be cleared");
        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(firstPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherNote").value("will be cleared"));

        Map<String, Object> secondPayload = buildProfilePayload();
        secondPayload.put("teacherNote", "   ");
        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(secondPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherNote").value(""));

        mockMvc.perform(get("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherNote").value(""));
    }

    @Test
    void teacherProfile_teacherNote_tooLong_returns400() throws Exception {
        Teacher teacher = createTeacherAccount("phase2_teacher_note_long", "Teacher Note Long");
        Student student = createStudentAccount("phase2_student_note_long", "Amy", "Chen", "Amy");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        Map<String, Object> payload = buildProfilePayload();
        payload.put("teacherNote", repeatChar('x', 5001));

        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("teacherNote must be at most 5000 characters"));
    }

    @Test
    void teacherProfileHistory_assignedTeacher_canReadHistoryItems() throws Exception {
        Teacher teacher = createTeacherAccount("phase2_teacher_history", "Teacher History");
        Student student = createStudentAccount("phase2_student_history", "Amy", "Chen", "Amy");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        String bearer = bearerFor(teacher.getUser());

        Map<String, Object> payload = buildProfilePayload();
        payload.put("version", 0);

        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearer)
                        .header("X-Profile-Change-Source", "manual_save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        MvcResult historyResult = mockMvc.perform(get("/api/teacher/students/{studentId}/profile/history?size=20", student.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].studentId").value(student.getId()))
                .andExpect(jsonPath("$.items[0].fromVersion").value(0))
                .andExpect(jsonPath("$.items[0].toVersion").value(1))
                .andExpect(jsonPath("$.items[0].changeSource").value("manual_save"))
                .andExpect(jsonPath("$.items[0].actorRole").value("TEACHER"))
                .andExpect(jsonPath("$.items[0].changedFields").isArray())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andReturn();

        JsonNode changedFields = objectMapper.readTree(historyResult.getResponse().getContentAsString())
                .path("items")
                .path(0)
                .path("changedFields");
        assertHistoryFieldPlainValue(changedFields, "birthday", null, "2008-06-01");
        assertHistoryFieldPlainValue(changedFields, "phone", null, "(647) 111-2222");
        assertHistoryFieldPlainValue(changedFields, "oenNumber", null, "123456789");
        assertHistoryFieldPlainValue(changedFields, "address.streetAddress", null, "123 Main St");
        assertHistoryPathAbsent(changedFields, "schools[0].schoolRecordId");
        assertHistoryPathAbsent(changedFields, "schools[0].hasTranscript");
    }

    @Test
    void teacherProfileHistory_admin_canReadPlainSensitiveFields() throws Exception {
        User admin = createAdmin("phase2_admin_history_plain");
        Student student = createStudentAccount("phase2_admin_student_history", "Amy", "Chen", "Amy");
        String bearer = bearerFor(admin);

        Map<String, Object> payload = buildProfilePayload();
        payload.put("version", 0);
        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearer)
                        .header("X-Profile-Change-Source", "manual_save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        MvcResult historyResult = mockMvc.perform(get("/api/teacher/students/{studentId}/profile/history?size=20", student.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].studentId").value(student.getId()))
                .andExpect(jsonPath("$.total").value(1))
                .andReturn();

        JsonNode changedFields = objectMapper.readTree(historyResult.getResponse().getContentAsString())
                .path("items")
                .path(0)
                .path("changedFields");
        assertHistoryFieldPlainValue(changedFields, "birthday", null, "2008-06-01");
        assertHistoryFieldPlainValue(changedFields, "phone", null, "(647) 111-2222");
        assertHistoryFieldPlainValue(changedFields, "oenNumber", null, "123456789");
        assertHistoryFieldPlainValue(changedFields, "address.streetAddress", null, "123 Main St");
        assertHistoryPathAbsent(changedFields, "schools[0].schoolRecordId");
        assertHistoryPathAbsent(changedFields, "schools[0].hasTranscript");
    }

    @Test
    void teacherProfile_withStaleVersion_returns409ProfileVersionConflict() throws Exception {
        Teacher teacher = createTeacherAccount("phase2_teacher_version_conflict", "Teacher Version");
        Student student = createStudentAccount("phase2_student_version_conflict", "Amy", "Chen", "Amy");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        String bearer = bearerFor(teacher.getUser());

        Map<String, Object> firstPayload = buildProfilePayload();
        firstPayload.put("version", 0);
        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(firstPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        Map<String, Object> stalePayload = buildProfilePayload();
        stalePayload.put("version", 0);
        stalePayload.put("preferredName", "Conflict Name");
        mockMvc.perform(put("/api/teacher/students/{studentId}/profile", student.getId())
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(stalePayload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROFILE_VERSION_CONFLICT"))
                .andExpect(jsonPath("$.currentVersion").value(1));
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

    private void assertHistoryFieldPlainValue(JsonNode changedFields,
                                              String path,
                                              String expectedBefore,
                                              String expectedAfter) {
        for (JsonNode field : changedFields) {
            if (!path.equals(field.path("path").asText())) {
                continue;
            }

            JsonNode before = field.get("before");
            if (expectedBefore == null) {
                org.junit.jupiter.api.Assertions.assertTrue(
                        before != null && before.isNull(),
                        "before should be null for path=" + path
                );
            } else {
                assertEquals(expectedBefore, before == null ? null : before.asText(), "before mismatch for path=" + path);
            }

            JsonNode after = field.get("after");
            assertEquals(expectedAfter, after == null ? null : after.asText(), "after mismatch for path=" + path);
            return;
        }
        throw new AssertionError("Expected changed field path not found: " + path);
    }

    private void assertHistoryPathAbsent(JsonNode changedFields, String path) {
        for (JsonNode field : changedFields) {
            if (path.equals(field.path("path").asText())) {
                throw new AssertionError("Unexpected changed field path found: " + path);
            }
        }
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

    private String repeatChar(char ch, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(ch);
        }
        return builder.toString();
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
