package com.studentmanagement.studentmanagementserver.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.SchoolType;
import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.student.StudentSchoolRecord;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherStudent;
import com.studentmanagement.studentmanagementserver.domain.university.University;
import com.studentmanagement.studentmanagementserver.domain.university.UniversityProgram;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentSchoolRecordRepository;
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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private StudentSchoolRecordRepository studentSchoolRecordRepository;

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
    void deletingUniversityApplicationsRemovesAllProgramsInOneRequest() throws Exception {
        Teacher teacher = createTeacherAccount("graduation_teacher_bulk_delete", "Bulk Delete Teacher");
        Student student = createStudentAccount("graduation_student_bulk_delete", "Bulk", "Delete", "Student");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        University target = universityRepository.save(new University(
                "Bulk Delete University", "Ontario", "Toronto", "Canada", null));
        University retained = universityRepository.save(new University(
                "Retained University", "Ontario", "Ottawa", "Canada", null));
        UniversityProgram first = universityProgramRepository.save(new UniversityProgram(target, "Arts", null, null));
        UniversityProgram second = universityProgramRepository.save(new UniversityProgram(target, "Science", null, null));
        UniversityProgram third = universityProgramRepository.save(new UniversityProgram(retained, "Commerce", null, null));
        String bearer = bearerFor(teacher.getUser());

        mockMvc.perform(put("/api/students/{studentId}/graduation-applications/confirm", student.getId())
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmPayload(Arrays.asList(
                                applicationPayload(target.getId(), first.getId(), "PREPARING"),
                                applicationPayload(target.getId(), second.getId(), "READY_TO_SUBMIT"),
                                applicationPayload(retained.getId(), third.getId(), "SUBMITTED")
                        )))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/students/{studentId}/graduation-applications/universities/{universityId}",
                        student.getId(), target.getId())
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/students/{studentId}/graduation-applications", student.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(delete("/api/students/{studentId}/graduation-applications/universities/{universityId}",
                        student.getId(), target.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/students/{studentId}/graduation-applications/universities/{universityId}",
                        student.getId(), target.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/students/{studentId}/graduation-applications", student.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].universityId").value(retained.getId()))
                .andExpect(jsonPath("$[0].sortOrder").value(1));

        mockMvc.perform(get("/api/students/{studentId}/graduation-applications/history", student.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.items[0].operation").value("DELETE_APPLICATION"))
                .andExpect(jsonPath("$.items[1].operation").value("DELETE_APPLICATION"));
    }

    @Test
    void customProgramCanBeReusedInAnApplicationAndIsNotDuplicated() throws Exception {
        Teacher teacher = createTeacherAccount("graduation_teacher_custom_program", "Custom Program Teacher");
        Student student = createStudentAccount("graduation_student_custom_program", "Custom", "Program", "Student");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        University university = universityRepository.save(new University(
                "Custom Program University", "Ontario", "Toronto", "Canada", null));
        String endpoint = "/api/universities/" + university.getId() + "/programs";

        MvcResult created = mockMvc.perform(post(endpoint)
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programName\":\"  Emerging Media Studies  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programName").value("Emerging Media Studies"))
                .andReturn();
        Long programId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post(endpoint)
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programName\":\"emerging media studies\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(programId));

        mockMvc.perform(put("/api/students/{studentId}/graduation-applications/confirm", student.getId())
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmPayload(university.getId(), programId, "PREPARING"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].programName").value("Emerging Media Studies"));
    }

    @Test
    void customUniversityAndProgramCanBeUsedInAnApplication() throws Exception {
        Teacher teacher = createTeacherAccount("graduation_teacher_custom_university", "Custom University Teacher");
        Student student = createStudentAccount("graduation_student_custom_university", "Custom", "University", "Student");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        String bearer = bearerFor(teacher.getUser());

        MvcResult created = mockMvc.perform(post("/api/universities")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  Hong Kong Example University  \",\"country\":\" Hong Kong \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hong Kong Example University"))
                .andExpect(jsonPath("$.country").value("Hong Kong"))
                .andReturn();
        Long universityId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/universities")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"hong kong example university\",\"country\":\"Hong Kong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(universityId));

        MvcResult createdProgram = mockMvc.perform(post("/api/universities/{universityId}/programs", universityId)
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programName\":\"Emerging Media Studies\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Long programId = objectMapper.readTree(createdProgram.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/students/{studentId}/graduation-applications/confirm", student.getId())
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmPayload(universityId, programId, "PREPARING"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].universityName").value("Hong Kong Example University"))
                .andExpect(jsonPath("$[0].programName").value("Emerging Media Studies"));
    }

    @Test
    void customUniversityRequiresValidNameAndCountry() throws Exception {
        Teacher teacher = createTeacherAccount("graduation_teacher_invalid_university", "Invalid University Teacher");
        String bearer = bearerFor(teacher.getUser());

        mockMvc.perform(post("/api/universities")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Other\",\"country\":\"Hong Kong\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/universities")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Example University\",\"country\":\"\"}"))
                .andExpect(status().isBadRequest());

        universityRepository.save(new University("Example University", null, null, "Canada", null));
        mockMvc.perform(post("/api/universities")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Example University\",\"country\":\"Hong Kong\"}"))
                .andExpect(status().isConflict());
    }

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
    void teacherCanListApplicationStudentsByUniversity() throws Exception {
        Teacher teacher = createTeacherAccount("graduation_teacher_university_overview", "Graduation Overview Teacher");
        Student alice = createStudentAccount("graduation_student_university_alice", "Alice", "Anderson", "Alice Nick");
        Student bob = createStudentAccount("graduation_student_university_bob", "Bob", "Brown", "");
        assignTeacherStudent(teacher, alice, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, bob, TeacherStudentStatus.ACTIVE);
        University university = universityRepository.save(new University(
                "Graduation Overview University",
                "Ontario",
                "Toronto",
                "Canada",
                null
        ));
        University otherUniversity = universityRepository.save(new University(
                "Graduation Other University",
                "Ontario",
                "Waterloo",
                "Canada",
                null
        ));
        UniversityProgram computerScience = universityProgramRepository.save(new UniversityProgram(
                university,
                "Computer Science",
                "Faculty of Arts and Science",
                "BSc"
        ));
        UniversityProgram lifeSciences = universityProgramRepository.save(new UniversityProgram(
                university,
                "Life Sciences",
                "Faculty of Arts and Science",
                "BSc"
        ));
        UniversityProgram otherProgram = universityProgramRepository.save(new UniversityProgram(
                otherUniversity,
                "Engineering",
                "Faculty of Engineering",
                "BEng"
        ));
        String teacherBearer = bearerFor(teacher.getUser());

        mockMvc.perform(put("/api/students/{studentId}/graduation-applications/confirm", alice.getId())
                        .header("Authorization", teacherBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmPayload(Arrays.asList(
                                applicationPayload(university.getId(), computerScience.getId(), "SUBMITTED"),
                                applicationPayload(university.getId(), lifeSciences.getId(), "OFFER_RECEIVED")
                        )))))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/students/{studentId}/graduation-applications/confirm", bob.getId())
                        .header("Authorization", teacherBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmPayload(Arrays.asList(
                                applicationPayload(university.getId(), computerScience.getId(), "NOT_ADMITTED"),
                                applicationPayload(otherUniversity.getId(), otherProgram.getId(), "SUBMITTED")
                        )))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/graduation-applications/universities/{universityId}/students", university.getId())
                        .header("Authorization", teacherBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(alice.getId()))
                .andExpect(jsonPath("$[0].studentName").value("Alice Anderson"))
                .andExpect(jsonPath("$[0].applications.length()").value(2))
                .andExpect(jsonPath("$[0].applications[0].programName").value("Computer Science"))
                .andExpect(jsonPath("$[0].applications[1].programName").value("Life Sciences"))
                .andExpect(jsonPath("$[1].studentId").value(bob.getId()))
                .andExpect(jsonPath("$[1].studentName").value("Bob Brown"))
                .andExpect(jsonPath("$[1].applications.length()").value(1))
                .andExpect(jsonPath("$[1].applications[0].universityName").value("Graduation Overview University"))
                .andExpect(jsonPath("$[1].applications[0].status").value("NOT_ADMITTED"));

        mockMvc.perform(get("/api/graduation-applications/universities/summary")
                        .header("Authorization", teacherBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].universityId").value(university.getId()))
                .andExpect(jsonPath("$[0].universityName").value("Graduation Overview University"))
                .andExpect(jsonPath("$[0].studentCount").value(2))
                .andExpect(jsonPath("$[0].applicationCount").value(3));
    }

    @Test
    void interviewListIncludesOnlyMarkedApplicationsInRequestedGraduationYear() throws Exception {
        Teacher teacher = createTeacherAccount("graduation_teacher_interview_list", "Interview List Teacher");
        Student matching = createStudentAccount("graduation_student_interview_matching", "Interview", "Matching", "");
        Student otherYear = createStudentAccount("graduation_student_interview_other_year", "Interview", "OtherYear", "");
        assignTeacherStudent(teacher, matching, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, otherYear, TeacherStudentStatus.ACTIVE);
        studentSchoolRecordRepository.save(new StudentSchoolRecord(matching, SchoolType.MAIN,
                "Matching School", LocalDate.of(2032, 9, 1), LocalDate.of(2035, 6, 30)));
        studentSchoolRecordRepository.save(new StudentSchoolRecord(otherYear, SchoolType.MAIN,
                "Other School", LocalDate.of(2033, 9, 1), LocalDate.of(2036, 6, 30)));
        University markedUniversity = universityRepository.save(new University(
                "Interview Marked University", "Ontario", "Toronto", "Canada", null));
        University unmarkedUniversity = universityRepository.save(new University(
                "Interview Unmarked University", "Ontario", "Ottawa", "Canada", null));
        UniversityProgram markedProgram = universityProgramRepository.save(new UniversityProgram(
                markedUniversity, "Arts", null, null));
        UniversityProgram unmarkedProgram = universityProgramRepository.save(new UniversityProgram(
                unmarkedUniversity, "Science", null, null));
        String teacherBearer = bearerFor(teacher.getUser());

        mockMvc.perform(put("/api/students/{studentId}/graduation-applications/confirm", matching.getId())
                        .header("Authorization", teacherBearer).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmPayload(Arrays.asList(
                                applicationPayload(markedUniversity.getId(), markedProgram.getId(), "SUBMITTED"),
                                applicationPayload(unmarkedUniversity.getId(), unmarkedProgram.getId(), "PREPARING")
                        )))))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/students/{studentId}/graduation-applications/confirm", otherYear.getId())
                        .header("Authorization", teacherBearer).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmPayload(
                                markedUniversity.getId(), markedProgram.getId(), "SUBMITTED"))))
                .andExpect(status().isOk());
        for (Student student : Arrays.asList(matching, otherYear)) {
            mockMvc.perform(put("/api/students/{studentId}/graduation-applications/universities/{universityId}/portal",
                            student.getId(), markedUniversity.getId())
                            .header("Authorization", teacherBearer).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"videoInterviewRequired\":true}"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(put("/api/students/{studentId}/graduation-applications/universities/{universityId}/portal",
                        matching.getId(), unmarkedUniversity.getId())
                        .header("Authorization", teacherBearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewRequired\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/graduation-applications/video-interviews").param("year", "2035")
                        .header("Authorization", teacherBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentId").value(matching.getId()))
                .andExpect(jsonPath("$[0].studentName").value("Interview Matching"))
                .andExpect(jsonPath("$[0].universityId").value(markedUniversity.getId()))
                .andExpect(jsonPath("$[0].graduationYear").value(2035));
        mockMvc.perform(get("/api/graduation-applications/video-interviews").param("year", "2035")
                        .header("Authorization", bearerFor(matching.getUser())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/graduation-applications/video-interviews").param("year", "1800")
                        .header("Authorization", teacherBearer))
                .andExpect(status().isBadRequest());
    }

    @Test
    void interviewCoursesAreStudentLevelAndLessonsPersistIndependently() throws Exception {
        Teacher teacher = createTeacherAccount("interview_course_teacher", "Interview Course Teacher");
        Student matching = createStudentAccount("interview_course_matching", "Course", "Matching", "");
        Student otherYear = createStudentAccount("interview_course_other_year", "Course", "Other", "");
        studentSchoolRecordRepository.save(new StudentSchoolRecord(matching, SchoolType.MAIN,
                "Interview Course School", LocalDate.of(2038, 9, 1), LocalDate.of(2041, 6, 30)));
        studentSchoolRecordRepository.save(new StudentSchoolRecord(otherYear, SchoolType.MAIN,
                "Interview Course School", LocalDate.of(2039, 9, 1), LocalDate.of(2042, 6, 30)));
        String bearer = bearerFor(teacher.getUser());
        String base = "/api/students/" + matching.getId() + "/graduation-applications/interview-course";

        mockMvc.perform(get(base).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.lessons.length()").value(0));
        mockMvc.perform(put(base + "/enabled").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.lessons.length()").value(4))
                .andExpect(jsonPath("$.lessons[0].lessonDate").doesNotExist());
        mockMvc.perform(get("/api/graduation-applications/interview-courses/enabled-students")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(matching.getId()));
        mockMvc.perform(get("/api/graduation-applications/interview-courses/enabled-students")
                        .header("Authorization", bearerFor(matching.getUser())))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(base + "/enabled").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessons.length()").value(4));

        MvcResult added = mockMvc.perform(post(base + "/lessons").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessons.length()").value(5))
                .andExpect(jsonPath("$.lessons[4].lessonNumber").value(5))
                .andReturn();
        Map<String, Object> result = objectMapper.readValue(added.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lessons = (List<Map<String, Object>>) result.get("lessons");
        Number lessonId = (Number) lessons.get(0).get("id");
        mockMvc.perform(put(base + "/lessons/{lessonId}", lessonId.longValue())
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lessonDate\":\"2041-04-16\",\"lessonTime\":\"15:30\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessons[0].lessonDate").value("2041-04-16"))
                .andExpect(jsonPath("$.lessons[0].lessonTime").value("15:30:00"));

        MvcResult requirementAdded = mockMvc.perform(post(base + "/requirements")
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"universityName\":\"  University of Toronto  \",\"programName\":\"Computer Science\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements.length()").value(1))
                .andExpect(jsonPath("$.requirements[0].universityName").value("University of Toronto"))
                .andReturn();
        Map<String, Object> requirementResult = objectMapper.readValue(
                requirementAdded.getResponse().getContentAsString(), new TypeReference<Map<String, Object>>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> requirements = (List<Map<String, Object>>) requirementResult.get("requirements");
        Number requirementId = (Number) requirements.get(0).get("id");
        mockMvc.perform(put(base + "/requirements/{id}", requirementId.longValue())
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"universityName\":\"McMaster University\",\"programName\":\"Engineering I\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements[0].programName").value("Engineering I"));
        mockMvc.perform(post(base + "/requirements").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"universityName\":\" \" ,\"programName\":\"Engineering I\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/graduation-applications/interview-courses").param("year", "2041")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentId").value(matching.getId()))
                .andExpect(jsonPath("$[0].studentName").value("Course Matching"))
                .andExpect(jsonPath("$[0].lessons.length()").value(5))
                .andExpect(jsonPath("$[0].requirements[0].universityName").value("McMaster University"));
        mockMvc.perform(get("/api/graduation-applications/interview-courses").param("year", "2042")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/graduation-applications/interview-courses").param("year", "1800")
                        .header("Authorization", bearer))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/students/{studentId}/graduation-applications/interview-course/enabled",
                        otherYear.getId())
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/students/{studentId}/graduation-applications/interview-course/lessons/{lessonId}",
                        otherYear.getId(), lessonId.longValue())
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lessonDate\":\"2042-04-16\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/students/{studentId}/graduation-applications/interview-course/requirements/{id}",
                        otherYear.getId(), requirementId.longValue())
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"universityName\":\"Other\",\"programName\":\"Other\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/graduation-applications/interview-courses").param("year", "2041")
                        .header("Authorization", bearerFor(matching.getUser())))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(base + "/lessons/{lessonId}", lessonId.longValue())
                        .header("Authorization", bearerFor(matching.getUser()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(base + "/enabled").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/graduation-applications/interview-courses").param("year", "2041")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(put(base + "/enabled").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessons.length()").value(5))
                .andExpect(jsonPath("$.lessons[0].lessonDate").value("2041-04-16"))
                .andExpect(jsonPath("$.requirements.length()").value(1));
        mockMvc.perform(delete(base + "/requirements/{id}", requirementId.longValue())
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements.length()").value(0));
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
        updatePayload.put("status", "OFFER_ACCEPTED");
        mockMvc.perform(put("/api/graduation-applications/{applicationId}", applicationId)
                        .header("Authorization", teacherBearer)
                        .header("X-Trace-Id", "graduation-audit-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programName").value("Life Sciences"))
                .andExpect(jsonPath("$.status").value("OFFER_ACCEPTED"));

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

    @Test
    void teacherCanReadAndUpdateUniversityPortalCredential() throws Exception {
        Teacher teacher = createTeacherAccount("graduation_teacher_portal", "Graduation Portal Teacher");
        Student student = createStudentAccount("graduation_student_portal", "Grad", "Portal", "Credential");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        studentSchoolRecordRepository.save(new StudentSchoolRecord(
                student,
                SchoolType.MAIN,
                "Portal Test High School",
                LocalDate.of(2024, 9, 1),
                LocalDate.of(2027, 6, 30)
        ));
        University university = universityRepository.save(new University(
                "Graduation Portal University",
                "Ontario",
                "Toronto",
                "Canada",
                null
        ));
        String teacherBearer = bearerFor(teacher.getUser());

        mockMvc.perform(get(
                                "/api/students/{studentId}/graduation-applications/universities/{universityId}/portal",
                                student.getId(),
                                university.getId()
                        )
                        .header("Authorization", teacherBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.universityId").value(university.getId()))
                .andExpect(jsonPath("$.schoolAccount").value(""))
                .andExpect(jsonPath("$.schoolEmail").value("gradportalvip2027@outlook.com"))
                .andExpect(jsonPath("$.schoolPassword").value("ZAQ!2wsxcde3"))
                .andExpect(jsonPath("$.studentVisible").value(false))
                .andExpect(jsonPath("$.interviewRequired").value(false))
                .andExpect(jsonPath("$.videoInterviewRequired").value(false))
                .andExpect(jsonPath("$.languageScoreRequired").value(false));

        mockMvc.perform(get(
                                "/api/students/{studentId}/graduation-applications/universities/{universityId}/portal",
                                student.getId(),
                                university.getId()
                        )
                .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentVisible").value(false))
                .andExpect(jsonPath("$.interviewRequired").value(false))
                .andExpect(jsonPath("$.languageScoreRequired").value(false))
                .andExpect(jsonPath("$.schoolAccount").value(""))
                .andExpect(jsonPath("$.schoolEmail").value(""))
                .andExpect(jsonPath("$.schoolPassword").value(""));

        Map<String, Object> updatePayload = new LinkedHashMap<String, Object>();
        updatePayload.put("schoolAccount", "portal-user-123");
        updatePayload.put("schoolEmail", "custom.portal@outlook.com");
        updatePayload.put("schoolPassword", "Changed!234");
        updatePayload.put("studentVisible", true);
        updatePayload.put("interviewRequired", true);
        updatePayload.put("videoInterviewRequired", true);
        updatePayload.put("languageScoreRequired", true);
        mockMvc.perform(put(
                                "/api/students/{studentId}/graduation-applications/universities/{universityId}/portal",
                                student.getId(),
                                university.getId()
                        )
                        .header("Authorization", teacherBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schoolAccount").value("portal-user-123"))
                .andExpect(jsonPath("$.schoolEmail").value("custom.portal@outlook.com"))
                .andExpect(jsonPath("$.schoolPassword").value("Changed!234"))
                .andExpect(jsonPath("$.studentVisible").value(true))
                .andExpect(jsonPath("$.interviewRequired").value(true))
                .andExpect(jsonPath("$.videoInterviewRequired").value(true))
                .andExpect(jsonPath("$.languageScoreRequired").value(true));

        mockMvc.perform(get(
                                "/api/students/{studentId}/graduation-applications/universities/{universityId}/portal",
                                student.getId(),
                                university.getId()
                        )
                .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentVisible").value(true))
                .andExpect(jsonPath("$.interviewRequired").value(true))
                .andExpect(jsonPath("$.videoInterviewRequired").value(true))
                .andExpect(jsonPath("$.languageScoreRequired").value(true))
                .andExpect(jsonPath("$.schoolAccount").value("portal-user-123"))
                .andExpect(jsonPath("$.schoolEmail").value("custom.portal@outlook.com"))
                .andExpect(jsonPath("$.schoolPassword").value("Changed!234"));

        mockMvc.perform(get("/api/students/{studentId}/graduation-applications/history", student.getId())
                        .header("Authorization", teacherBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].operation").value("UPDATE_PORTAL_CREDENTIAL"))
                .andExpect(jsonPath("$.items[0].changedFields[0].path").value("schoolAccount"))
                .andExpect(jsonPath("$.items[0].changedFields[2].path").value("schoolPassword"))
                .andExpect(jsonPath("$.items[0].changedFields[2].before").value("已设置"))
                .andExpect(jsonPath("$.items[0].changedFields[2].after").value("已设置"));
    }

    @Test
    void teacherCanUpdateSharedApplicationAccountAndSyncPortalCredentials() throws Exception {
        Teacher teacher = createTeacherAccount("graduation_teacher_account", "Graduation Account Teacher");
        Student student = createStudentAccount("graduation_student_account", "Grad", "Account", "Credential");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        studentSchoolRecordRepository.save(new StudentSchoolRecord(
                student,
                SchoolType.MAIN,
                "Account Test High School",
                LocalDate.of(2024, 9, 1),
                LocalDate.of(2027, 6, 30)
        ));
        University university = universityRepository.save(new University(
                "Graduation Account University",
                "Ontario",
                "Toronto",
                "Canada",
                null
        ));
        String teacherBearer = bearerFor(teacher.getUser());

        mockMvc.perform(get("/api/students/{studentId}/graduation-applications/account", student.getId())
                        .header("Authorization", teacherBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationEmail").value("gradaccountvip2027@outlook.com"))
                .andExpect(jsonPath("$.applicationPassword").value("ZAQ!2wsxcde3"));

        Map<String, Object> portalPayload = new LinkedHashMap<String, Object>();
        portalPayload.put("schoolAccount", "school-login");
        portalPayload.put("schoolEmail", "old.application@outlook.com");
        portalPayload.put("schoolPassword", "Old!234");
        mockMvc.perform(put(
                                "/api/students/{studentId}/graduation-applications/universities/{universityId}/portal",
                                student.getId(),
                                university.getId()
                        )
                        .header("Authorization", teacherBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(portalPayload)))
                .andExpect(status().isOk());

        Map<String, Object> accountPayload = new LinkedHashMap<String, Object>();
        accountPayload.put("applicationEmail", "shared.application@outlook.com");
        accountPayload.put("applicationPassword", "Shared!234");
        mockMvc.perform(put("/api/students/{studentId}/graduation-applications/account", student.getId())
                        .header("Authorization", teacherBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationEmail").value("shared.application@outlook.com"))
                .andExpect(jsonPath("$.applicationPassword").value("Shared!234"));

        mockMvc.perform(get(
                                "/api/students/{studentId}/graduation-applications/universities/{universityId}/portal",
                                student.getId(),
                                university.getId()
                        )
                        .header("Authorization", teacherBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schoolAccount").value("school-login"))
                .andExpect(jsonPath("$.schoolEmail").value("shared.application@outlook.com"))
                .andExpect(jsonPath("$.schoolPassword").value("Shared!234"));

        mockMvc.perform(get("/api/students/{studentId}/graduation-applications/history", student.getId())
                        .header("Authorization", teacherBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].operation").value("UPDATE_APPLICATION_ACCOUNT_CREDENTIAL"))
                .andExpect(jsonPath("$.items[0].changedFields[0].path").value("applicationEmail"))
                .andExpect(jsonPath("$.items[0].changedFields[1].path").value("applicationPassword"));
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

    private Map<String, Object> confirmPayload(List<Map<String, Object>> applications) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("applications", applications);
        return payload;
    }

    private Map<String, Object> applicationPayload(Long universityId, Long programId, String status) {
        Map<String, Object> application = new LinkedHashMap<String, Object>();
        application.put("universityId", universityId);
        application.put("programId", programId);
        application.put("status", status);
        return application;
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
