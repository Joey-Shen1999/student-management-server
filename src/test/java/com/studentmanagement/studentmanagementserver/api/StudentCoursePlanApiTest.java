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
import java.util.Collections;
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
class StudentCoursePlanApiTest {

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
    void studentFirstGet_returnsDefaultScaffold() throws Exception {
        Student student = createStudentAccount("course_plan_student_default", "Course", "Plan", "Default");

        mockMvc.perform(get("/api/student/course-plan")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentGradeLevel").value(nullValue()))
                .andExpect(jsonPath("$.grade13Enabled").value(false))
                .andExpect(jsonPath("$.grades.length()").value(4))
                .andExpect(jsonPath("$.grades[0].gradeLevel").value(9))
                .andExpect(jsonPath("$.grades[0].yearStructure").value("FULL_YEAR"))
                .andExpect(jsonPath("$.grades[0].courses.length()").value(0))
                .andExpect(jsonPath("$.grades[3].gradeLevel").value(12))
                .andExpect(jsonPath("$.grades[3].yearStructure").value("FULL_YEAR"));
    }

    @Test
    void studentPutThenGet_roundTripsNormalizedCoursePlan() throws Exception {
        Student student = createStudentAccount("course_plan_student_round_trip", "Course", "Plan", "RoundTrip");

        mockMvc.perform(put("/api/student/course-plan")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRoundTripPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentGradeLevel").value(11))
                .andExpect(jsonPath("$.grade13Enabled").value(false))
                .andExpect(jsonPath("$.grades.length()").value(4))
                .andExpect(jsonPath("$.grades[0].gradeLevel").value(9))
                .andExpect(jsonPath("$.grades[0].courses[0].id").value("g9-eng"))
                .andExpect(jsonPath("$.grades[0].courses[0].mark").value(91))
                .andExpect(jsonPath("$.grades[1].yearStructure").value("SEMESTER"))
                .andExpect(jsonPath("$.grades[1].courses[0].id").value("g10-s1-math"))
                .andExpect(jsonPath("$.grades[1].courses[0].semester").value("S1"))
                .andExpect(jsonPath("$.grades[1].courses[1].id").value("g10-s2-chem"))
                .andExpect(jsonPath("$.grades[1].courses[1].semester").value("S2"))
                .andExpect(jsonPath("$.grades[2].courses[0].courseCode").value("ENG3U"))
                .andExpect(jsonPath("$.grades[2].courses[0].status").value("PLANNED"))
                .andExpect(jsonPath("$.grades[2].courses[0].mark").value(nullValue()));

        mockMvc.perform(get("/api/student/course-plan")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentGradeLevel").value(11))
                .andExpect(jsonPath("$.grade13Enabled").value(false))
                .andExpect(jsonPath("$.grades.length()").value(4))
                .andExpect(jsonPath("$.grades[0].courses[0].id").value("g9-eng"))
                .andExpect(jsonPath("$.grades[1].courses[0].id").value("g10-s1-math"))
                .andExpect(jsonPath("$.grades[1].courses[0].sortOrder").value(0))
                .andExpect(jsonPath("$.grades[1].courses[1].id").value("g10-s2-chem"))
                .andExpect(jsonPath("$.grades[1].courses[1].sortOrder").value(1))
                .andExpect(jsonPath("$.grades[2].courses[0].courseCode").value("ENG3U"))
                .andExpect(jsonPath("$.grades[3].gradeLevel").value(12));
    }

    @Test
    void teacherCanReadWriteAnyStudentCoursePlan() throws Exception {
        Teacher assignedTeacher = createTeacherAccount("course_plan_teacher_assigned", "Course Plan Assigned");
        Teacher unassignedTeacher = createTeacherAccount("course_plan_teacher_unassigned", "Course Plan Unassigned");
        Student student = createStudentAccount("course_plan_student_teacher_access", "Teacher", "Access", "Student");
        assignTeacherStudent(assignedTeacher, student, TeacherStudentStatus.ACTIVE);

        mockMvc.perform(put("/api/teacher/students/{studentId}/course-plan", student.getId())
                        .header("Authorization", bearerFor(assignedTeacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTeacherPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentGradeLevel").value(10))
                .andExpect(jsonPath("$.grades[0].courses[0].id").value("teacher-g9-course"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/course-plan", student.getId())
                        .header("Authorization", bearerFor(assignedTeacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentGradeLevel").value(10))
                .andExpect(jsonPath("$.grades[0].courses[0].id").value("teacher-g9-course"));

        mockMvc.perform(get("/api/teacher/students/{studentId}/course-plan", student.getId())
                        .header("Authorization", bearerFor(unassignedTeacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentGradeLevel").value(10));

        mockMvc.perform(put("/api/teacher/students/{studentId}/course-plan", student.getId())
                        .header("Authorization", bearerFor(unassignedTeacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTeacherPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentGradeLevel").value(10));
    }

    @Test
    void plannedCourseWithMark_returns400() throws Exception {
        Student student = createStudentAccount("course_plan_student_planned_mark", "Planned", "Mark", "Invalid");

        mockMvc.perform(put("/api/student/course-plan")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPlannedMarkInvalidPayload())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.details", hasItem("grades[2].courses[0].mark must be null when status is PLANNED")));
    }

    @Test
    void fullYearWithSemester_returns400() throws Exception {
        Student student = createStudentAccount("course_plan_student_full_year_semester", "Full", "Year", "Semester");

        mockMvc.perform(put("/api/student/course-plan")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createFullYearSemesterInvalidPayload())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.details", hasItem("grades[0].courses[0].semester must be null when yearStructure is FULL_YEAR")));
    }

    @Test
    void removingGrade13AndCourses_updatesStoredState() throws Exception {
        Student student = createStudentAccount("course_plan_student_remove_grade13", "Grade", "Thirteen", "Remove");

        mockMvc.perform(put("/api/student/course-plan")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createGrade13EnabledByDataPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade13Enabled").value(true))
                .andExpect(jsonPath("$.grades.length()").value(5))
                .andExpect(jsonPath("$.grades[4].gradeLevel").value(13))
                .andExpect(jsonPath("$.grades[0].courses.length()").value(2));

        mockMvc.perform(put("/api/student/course-plan")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createGrade13RemovedPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade13Enabled").value(false))
                .andExpect(jsonPath("$.grades.length()").value(4))
                .andExpect(jsonPath("$.grades[0].courses.length()").value(1))
                .andExpect(jsonPath("$.grades[0].courses[0].id").value("g9-keep"));

        mockMvc.perform(get("/api/student/course-plan")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade13Enabled").value(false))
                .andExpect(jsonPath("$.grades.length()").value(4))
                .andExpect(jsonPath("$.grades[0].courses.length()").value(1))
                .andExpect(jsonPath("$.grades[0].courses[0].id").value("g9-keep"))
                .andExpect(jsonPath("$.grades[3].gradeLevel").value(12));
    }

    @Test
    void movingCoursesAcrossGradesAndSemesters_preservesOwnershipAndOrder() throws Exception {
        Student student = createStudentAccount("course_plan_student_drag_move", "Drag", "Move", "Student");

        mockMvc.perform(put("/api/student/course-plan")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createInitialMovePayload())))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/student/course-plan")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createMovedPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grades[0].courses.length()").value(0))
                .andExpect(jsonPath("$.grades[1].courses.length()").value(3))
                .andExpect(jsonPath("$.grades[1].courses[0].id").value("course-move"))
                .andExpect(jsonPath("$.grades[1].courses[0].semester").value("S2"))
                .andExpect(jsonPath("$.grades[1].courses[0].sortOrder").value(0))
                .andExpect(jsonPath("$.grades[1].courses[1].id").value("course-switch"))
                .andExpect(jsonPath("$.grades[1].courses[1].semester").value("S2"))
                .andExpect(jsonPath("$.grades[1].courses[1].sortOrder").value(1))
                .andExpect(jsonPath("$.grades[1].courses[2].id").value("course-stay"))
                .andExpect(jsonPath("$.grades[1].courses[2].semester").value("S1"))
                .andExpect(jsonPath("$.grades[1].courses[2].sortOrder").value(2));

        mockMvc.perform(get("/api/student/course-plan")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grades[0].courses.length()").value(0))
                .andExpect(jsonPath("$.grades[1].courses[0].id").value("course-move"))
                .andExpect(jsonPath("$.grades[1].courses[1].id").value("course-switch"))
                .andExpect(jsonPath("$.grades[1].courses[2].id").value("course-stay"));
    }

    private Map<String, Object> createRoundTripPayload() {
        return createPayload(
                11,
                false,
                Arrays.asList(
                        createGrade(9, "FULL_YEAR", Arrays.asList(
                                createCourse("g9-eng", "ENG1D", "COMPLETED", 91, null, 0)
                        )),
                        createGrade(10, "SEMESTER", Arrays.asList(
                                createCourse("g10-s2-chem", "SNC2D", "IN_PROGRESS", null, "S2", 1),
                                createCourse("g10-s1-math", "MPM2D", "COMPLETED", 88, "S1", 0)
                        )),
                        createGrade(11, "FULL_YEAR", Arrays.asList(
                                createCourse("g11-plan-eng", "  ENG3U  ", "PLANNED", null, null, 0)
                        )),
                        createGrade(12, "FULL_YEAR", Collections.<Map<String, Object>>emptyList())
                )
        );
    }

    private Map<String, Object> createTeacherPayload() {
        return createPayload(
                10,
                false,
                Arrays.asList(
                        createGrade(9, "FULL_YEAR", Arrays.asList(
                                createCourse("teacher-g9-course", "MTH1W", "COMPLETED", 84, null, 0)
                        )),
                        createGrade(10, "FULL_YEAR", Collections.<Map<String, Object>>emptyList()),
                        createGrade(11, "FULL_YEAR", Collections.<Map<String, Object>>emptyList()),
                        createGrade(12, "FULL_YEAR", Collections.<Map<String, Object>>emptyList())
                )
        );
    }

    private Map<String, Object> createPlannedMarkInvalidPayload() {
        return createPayload(
                11,
                false,
                Arrays.asList(
                        createGrade(9, "FULL_YEAR", Collections.<Map<String, Object>>emptyList()),
                        createGrade(10, "FULL_YEAR", Collections.<Map<String, Object>>emptyList()),
                        createGrade(11, "FULL_YEAR", Arrays.asList(
                                createCourse("g11-plan-invalid", "ENG3U", "PLANNED", 90, null, 0)
                        )),
                        createGrade(12, "FULL_YEAR", Collections.<Map<String, Object>>emptyList())
                )
        );
    }

    private Map<String, Object> createFullYearSemesterInvalidPayload() {
        return createPayload(
                9,
                false,
                Arrays.asList(
                        createGrade(9, "FULL_YEAR", Arrays.asList(
                                createCourse("g9-invalid-semester", "ENG1D", "COMPLETED", 90, "S1", 0)
                        )),
                        createGrade(10, "FULL_YEAR", Collections.<Map<String, Object>>emptyList()),
                        createGrade(11, "FULL_YEAR", Collections.<Map<String, Object>>emptyList()),
                        createGrade(12, "FULL_YEAR", Collections.<Map<String, Object>>emptyList())
                )
        );
    }

    private Map<String, Object> createGrade13EnabledByDataPayload() {
        return createPayload(
                11,
                false,
                Arrays.asList(
                        createGrade(9, "FULL_YEAR", Arrays.asList(
                                createCourse("g9-keep", "ENG1D", "COMPLETED", 90, null, 0),
                                createCourse("g9-delete", "MTH1W", "PLANNED", null, null, 1)
                        )),
                        createGrade(10, "FULL_YEAR", Collections.<Map<String, Object>>emptyList()),
                        createGrade(11, "FULL_YEAR", Collections.<Map<String, Object>>emptyList()),
                        createGrade(12, "FULL_YEAR", Collections.<Map<String, Object>>emptyList()),
                        createGrade(13, "FULL_YEAR", Arrays.asList(
                                createCourse("g13-course", "ENG4U", "PLANNED", null, null, 0)
                        ))
                )
        );
    }

    private Map<String, Object> createGrade13RemovedPayload() {
        return createPayload(
                11,
                false,
                Arrays.asList(
                        createGrade(9, "FULL_YEAR", Arrays.asList(
                                createCourse("g9-keep", "ENG1D", "COMPLETED", 90, null, 0)
                        )),
                        createGrade(10, "FULL_YEAR", Collections.<Map<String, Object>>emptyList()),
                        createGrade(11, "FULL_YEAR", Collections.<Map<String, Object>>emptyList()),
                        createGrade(12, "FULL_YEAR", Collections.<Map<String, Object>>emptyList())
                )
        );
    }

    private Map<String, Object> createInitialMovePayload() {
        return createPayload(
                10,
                false,
                Arrays.asList(
                        createGrade(9, "FULL_YEAR", Arrays.asList(
                                createCourse("course-move", "MPM1D", "COMPLETED", 92, null, 0)
                        )),
                        createGrade(10, "SEMESTER", Arrays.asList(
                                createCourse("course-switch", "SNC2D", "IN_PROGRESS", null, "S1", 0),
                                createCourse("course-stay", "CHV2O", "PLANNED", null, "S2", 1)
                        )),
                        createGrade(11, "FULL_YEAR", Collections.<Map<String, Object>>emptyList()),
                        createGrade(12, "FULL_YEAR", Collections.<Map<String, Object>>emptyList())
                )
        );
    }

    private Map<String, Object> createMovedPayload() {
        return createPayload(
                10,
                false,
                Arrays.asList(
                        createGrade(9, "FULL_YEAR", Collections.<Map<String, Object>>emptyList()),
                        createGrade(10, "SEMESTER", Arrays.asList(
                                createCourse("course-move", "MPM1D", "COMPLETED", 92, "S2", 0),
                                createCourse("course-switch", "SNC2D", "IN_PROGRESS", null, "S2", 1),
                                createCourse("course-stay", "CHV2O", "PLANNED", null, "S1", 2)
                        )),
                        createGrade(11, "FULL_YEAR", Collections.<Map<String, Object>>emptyList()),
                        createGrade(12, "FULL_YEAR", Collections.<Map<String, Object>>emptyList())
                )
        );
    }

    private Map<String, Object> createPayload(Integer currentGradeLevel,
                                              boolean grade13Enabled,
                                              java.util.List<Map<String, Object>> grades) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("currentGradeLevel", currentGradeLevel);
        payload.put("grade13Enabled", grade13Enabled);
        payload.put("grades", grades);
        return payload;
    }

    private Map<String, Object> createGrade(int gradeLevel,
                                            String yearStructure,
                                            java.util.List<Map<String, Object>> courses) {
        Map<String, Object> grade = new LinkedHashMap<String, Object>();
        grade.put("gradeLevel", gradeLevel);
        grade.put("yearStructure", yearStructure);
        grade.put("courses", courses);
        return grade;
    }

    private Map<String, Object> createCourse(String id,
                                             String courseCode,
                                             String status,
                                             Integer mark,
                                             String semester,
                                             int sortOrder) {
        Map<String, Object> course = new LinkedHashMap<String, Object>();
        course.put("id", id);
        course.put("courseCode", courseCode);
        course.put("status", status);
        course.put("mark", mark);
        course.put("semester", semester);
        course.put("sortOrder", sortOrder);
        return course;
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
        teacherStudentRepository.save(new TeacherStudent(teacher, student, status, "student-course-plan-api-test-assignment"));
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
