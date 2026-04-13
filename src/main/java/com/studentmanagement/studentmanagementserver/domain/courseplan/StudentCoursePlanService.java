package com.studentmanagement.studentmanagementserver.domain.courseplan;

import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentCoursePlanRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherStudentRepository;
import com.studentmanagement.studentmanagementserver.service.ApiRequestException;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import com.studentmanagement.studentmanagementserver.service.ManagementAccessService;
import com.studentmanagement.studentmanagementserver.service.TeacherBindingRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class StudentCoursePlanService {

    private static final List<Integer> DEFAULT_GRADE_LEVELS = Arrays.asList(9, 10, 11, 12);
    private static final List<Integer> ALLOWED_GRADE_LEVELS = Arrays.asList(9, 10, 11, 12, 13);
    private static final int MAX_COURSE_ID_LENGTH = 128;
    private static final int MAX_COURSE_CODE_LENGTH = 64;

    private final AuthSessionService authSessionService;
    private final ManagementAccessService managementAccessService;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherStudentRepository teacherStudentRepository;
    private final StudentCoursePlanRepository studentCoursePlanRepository;

    public StudentCoursePlanService(AuthSessionService authSessionService,
                                    ManagementAccessService managementAccessService,
                                    StudentRepository studentRepository,
                                    TeacherRepository teacherRepository,
                                    TeacherStudentRepository teacherStudentRepository,
                                    StudentCoursePlanRepository studentCoursePlanRepository) {
        this.authSessionService = authSessionService;
        this.managementAccessService = managementAccessService;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.teacherStudentRepository = teacherStudentRepository;
        this.studentCoursePlanRepository = studentCoursePlanRepository;
    }

    @Transactional(readOnly = true)
    public StudentCoursePlanDto getCurrentStudentCoursePlan(HttpServletRequest request) {
        CurrentStudentContext context = requireCurrentStudentContext(request);
        return buildCoursePlanDto(loadCoursePlan(context.student.getId()));
    }

    @Transactional
    public StudentCoursePlanDto updateCurrentStudentCoursePlan(StudentCoursePlanDto requestBody,
                                                               HttpServletRequest request) {
        CurrentStudentContext context = requireCurrentStudentContext(request);
        return saveCoursePlan(context.student, requestBody);
    }

    @Transactional(readOnly = true)
    public StudentCoursePlanDto getTeacherStudentCoursePlan(Long studentId, HttpServletRequest request) {
        Student student = requireTeacherAccessibleStudent(studentId, request);
        return buildCoursePlanDto(loadCoursePlan(student.getId()));
    }

    @Transactional
    public StudentCoursePlanDto updateTeacherStudentCoursePlan(Long studentId,
                                                               StudentCoursePlanDto requestBody,
                                                               HttpServletRequest request) {
        TeacherStudentContext context = requireTeacherAccessibleStudentContext(studentId, request);
        return saveCoursePlan(context.student, requestBody);
    }

    private StudentCoursePlanDto saveCoursePlan(Student student, StudentCoursePlanDto requestBody) {
        NormalizedCoursePlan normalized = normalizeRequest(requestBody);
        StudentCoursePlan plan = studentCoursePlanRepository.findByStudent_Id(student.getId())
                .orElseGet(() -> new StudentCoursePlan(student));

        if (plan.getId() != null) {
            plan.replaceGrades(Collections.<StudentCoursePlanGrade>emptyList());
            studentCoursePlanRepository.saveAndFlush(plan);
        }

        plan.overwrite(normalized.currentGradeLevel, normalized.grade13Enabled);
        List<StudentCoursePlanGrade> gradeEntities = new ArrayList<StudentCoursePlanGrade>(normalized.grades.size());
        for (NormalizedGrade normalizedGrade : normalized.grades) {
            StudentCoursePlanGrade grade = new StudentCoursePlanGrade(
                    normalizedGrade.gradeLevel,
                    normalizedGrade.yearStructure
            );
            grade.replaceCourses(buildCourseEntities(normalizedGrade.courses));
            gradeEntities.add(grade);
        }
        plan.replaceGrades(gradeEntities);

        StudentCoursePlan saved = studentCoursePlanRepository.save(plan);
        return buildCoursePlanDto(saved);
    }

    private StudentCoursePlan loadCoursePlan(Long studentId) {
        StudentCoursePlan plan = studentCoursePlanRepository.findByStudent_Id(studentId).orElse(null);
        initializePlan(plan);
        return plan;
    }

    private void initializePlan(StudentCoursePlan plan) {
        if (plan == null || plan.getGrades() == null) {
            return;
        }
        plan.getGrades().size();
        for (StudentCoursePlanGrade grade : plan.getGrades()) {
            if (grade == null || grade.getCourses() == null) {
                continue;
            }
            grade.getCourses().size();
        }
    }

    private List<StudentCoursePlanCourse> buildCourseEntities(List<NormalizedCourse> normalizedCourses) {
        List<StudentCoursePlanCourse> courseEntities = new ArrayList<StudentCoursePlanCourse>(normalizedCourses.size());
        for (NormalizedCourse normalizedCourse : normalizedCourses) {
            courseEntities.add(new StudentCoursePlanCourse(
                    normalizedCourse.id,
                    normalizedCourse.courseCode,
                    normalizedCourse.status,
                    normalizedCourse.mark,
                    normalizedCourse.semester,
                    normalizedCourse.sortOrder
            ));
        }
        return courseEntities;
    }

    private StudentCoursePlanDto buildCoursePlanDto(StudentCoursePlan plan) {
        if (plan == null) {
            return defaultScaffold();
        }

        Map<Integer, StudentCoursePlanGrade> gradesByLevel = new LinkedHashMap<Integer, StudentCoursePlanGrade>();
        if (plan.getGrades() != null) {
            for (StudentCoursePlanGrade grade : plan.getGrades()) {
                if (grade == null || grade.getGradeLevel() == null) {
                    continue;
                }
                gradesByLevel.put(grade.getGradeLevel(), grade);
            }
        }

        boolean grade13Enabled = plan.isGrade13Enabled()
                || Integer.valueOf(13).equals(plan.getCurrentGradeLevel())
                || gradesByLevel.containsKey(13);
        List<StudentCoursePlanGradeDto> gradeDtos = new ArrayList<StudentCoursePlanGradeDto>();
        for (Integer gradeLevel : DEFAULT_GRADE_LEVELS) {
            gradeDtos.add(buildGradeDto(gradeLevel, gradesByLevel.get(gradeLevel)));
        }
        if (grade13Enabled) {
            gradeDtos.add(buildGradeDto(13, gradesByLevel.get(13)));
        }

        return new StudentCoursePlanDto(plan.getCurrentGradeLevel(), grade13Enabled, gradeDtos);
    }

    private StudentCoursePlanGradeDto buildGradeDto(Integer gradeLevel, StudentCoursePlanGrade grade) {
        CoursePlanYearStructure yearStructure = grade == null || grade.getYearStructure() == null
                ? CoursePlanYearStructure.FULL_YEAR
                : grade.getYearStructure();
        List<StudentCoursePlanCourseDto> courseDtos = new ArrayList<StudentCoursePlanCourseDto>();
        if (grade != null && grade.getCourses() != null) {
            List<StudentCoursePlanCourse> sortedCourses = new ArrayList<StudentCoursePlanCourse>(grade.getCourses());
            Collections.sort(sortedCourses, new Comparator<StudentCoursePlanCourse>() {
                @Override
                public int compare(StudentCoursePlanCourse left, StudentCoursePlanCourse right) {
                    int sortOrderCompare = Integer.valueOf(left.getSortOrder()).compareTo(right.getSortOrder());
                    if (sortOrderCompare != 0) {
                        return sortOrderCompare;
                    }
                    String leftId = left.getClientCourseId() == null ? "" : left.getClientCourseId();
                    String rightId = right.getClientCourseId() == null ? "" : right.getClientCourseId();
                    return leftId.compareTo(rightId);
                }
            });
            for (StudentCoursePlanCourse course : sortedCourses) {
                courseDtos.add(new StudentCoursePlanCourseDto(
                        course.getClientCourseId(),
                        course.getCourseCode(),
                        course.getStatus() == null ? null : course.getStatus().name(),
                        course.getMark(),
                        course.getSemester() == null ? null : course.getSemester().name(),
                        course.getSortOrder()
                ));
            }
        }
        return new StudentCoursePlanGradeDto(gradeLevel, yearStructure.name(), courseDtos);
    }

    private StudentCoursePlanDto defaultScaffold() {
        List<StudentCoursePlanGradeDto> grades = new ArrayList<StudentCoursePlanGradeDto>(DEFAULT_GRADE_LEVELS.size());
        for (Integer gradeLevel : DEFAULT_GRADE_LEVELS) {
            grades.add(new StudentCoursePlanGradeDto(
                    gradeLevel,
                    CoursePlanYearStructure.FULL_YEAR.name(),
                    Collections.<StudentCoursePlanCourseDto>emptyList()
            ));
        }
        return new StudentCoursePlanDto(null, false, grades);
    }

    private NormalizedCoursePlan normalizeRequest(StudentCoursePlanDto requestBody) {
        List<String> details = new ArrayList<String>();
        if (requestBody == null) {
            details.add("request body is required");
            throw validationFailed(details);
        }

        Integer currentGradeLevel = normalizeGradeLevel(requestBody.getCurrentGradeLevel(), "currentGradeLevel", true, details);
        List<StudentCoursePlanGradeDto> rawGrades = requestBody.getGrades() == null
                ? Collections.<StudentCoursePlanGradeDto>emptyList()
                : requestBody.getGrades();

        Map<Integer, NormalizedGrade> gradesByLevel = new LinkedHashMap<Integer, NormalizedGrade>();
        Set<String> courseIds = new LinkedHashSet<String>();
        boolean grade13Enabled = requestBody.isGrade13Enabled() || Integer.valueOf(13).equals(currentGradeLevel);

        for (int gradeIndex = 0; gradeIndex < rawGrades.size(); gradeIndex++) {
            StudentCoursePlanGradeDto rawGrade = rawGrades.get(gradeIndex);
            String gradeField = "grades[" + gradeIndex + "]";
            if (rawGrade == null) {
                details.add(gradeField + " is required");
                continue;
            }

            Integer gradeLevel = normalizeGradeLevel(rawGrade.getGradeLevel(), gradeField + ".gradeLevel", false, details);
            if (Integer.valueOf(13).equals(gradeLevel)) {
                grade13Enabled = true;
            }
            if (gradeLevel != null && gradesByLevel.containsKey(gradeLevel)) {
                details.add(gradeField + ".gradeLevel duplicates grade " + gradeLevel);
            }

            CoursePlanYearStructure yearStructure = parseYearStructure(
                    rawGrade.getYearStructure(),
                    gradeField + ".yearStructure",
                    details
            );
            List<StudentCoursePlanCourseDto> rawCourses = rawGrade.getCourses() == null
                    ? Collections.<StudentCoursePlanCourseDto>emptyList()
                    : rawGrade.getCourses();
            List<NormalizedCourse> courses = normalizeCourses(rawCourses, yearStructure, gradeField + ".courses", courseIds, details);

            if (gradeLevel != null && yearStructure != null && !gradesByLevel.containsKey(gradeLevel)) {
                gradesByLevel.put(gradeLevel, new NormalizedGrade(gradeLevel, yearStructure, courses));
            }
        }

        for (Integer defaultGradeLevel : DEFAULT_GRADE_LEVELS) {
            if (!gradesByLevel.containsKey(defaultGradeLevel)) {
                gradesByLevel.put(defaultGradeLevel, new NormalizedGrade(
                        defaultGradeLevel,
                        CoursePlanYearStructure.FULL_YEAR,
                        Collections.<NormalizedCourse>emptyList()
                ));
            }
        }
        if (grade13Enabled && !gradesByLevel.containsKey(13)) {
            gradesByLevel.put(13, new NormalizedGrade(
                    13,
                    CoursePlanYearStructure.FULL_YEAR,
                    Collections.<NormalizedCourse>emptyList()
            ));
        }

        if (!details.isEmpty()) {
            throw validationFailed(details);
        }

        List<NormalizedGrade> normalizedGrades = new ArrayList<NormalizedGrade>(gradesByLevel.values());
        Collections.sort(normalizedGrades, new Comparator<NormalizedGrade>() {
            @Override
            public int compare(NormalizedGrade left, NormalizedGrade right) {
                return left.gradeLevel.compareTo(right.gradeLevel);
            }
        });
        return new NormalizedCoursePlan(currentGradeLevel, grade13Enabled, normalizedGrades);
    }

    private List<NormalizedCourse> normalizeCourses(List<StudentCoursePlanCourseDto> rawCourses,
                                                    CoursePlanYearStructure yearStructure,
                                                    String coursesField,
                                                    Set<String> courseIds,
                                                    List<String> details) {
        List<NormalizedCourse> courses = new ArrayList<NormalizedCourse>(rawCourses.size());
        for (int courseIndex = 0; courseIndex < rawCourses.size(); courseIndex++) {
            StudentCoursePlanCourseDto rawCourse = rawCourses.get(courseIndex);
            String courseField = coursesField + "[" + courseIndex + "]";
            if (rawCourse == null) {
                details.add(courseField + " is required");
                continue;
            }

            String courseId = normalizeCourseId(rawCourse.getId(), courseField + ".id", details);
            if (courseId != null && !courseIds.add(courseId)) {
                details.add(courseField + ".id duplicates another course id");
            }
            String courseCode = normalizeCourseCode(rawCourse.getCourseCode(), courseField + ".courseCode", details);
            CoursePlanCourseStatus status = parseCourseStatus(rawCourse.getStatus(), courseField + ".status", details);
            Integer mark = normalizeMark(rawCourse.getMark(), status, courseField + ".mark", details);
            CoursePlanSemester semester = normalizeSemester(rawCourse.getSemester(), yearStructure, courseField + ".semester", details);
            int sortOrder = rawCourse.getSortOrder() == null ? courseIndex : rawCourse.getSortOrder().intValue();

            courses.add(new NormalizedCourse(courseId, courseCode, status, mark, semester, sortOrder));
        }
        Collections.sort(courses, new Comparator<NormalizedCourse>() {
            @Override
            public int compare(NormalizedCourse left, NormalizedCourse right) {
                int sortOrderCompare = left.sortOrder.compareTo(right.sortOrder);
                if (sortOrderCompare != 0) {
                    return sortOrderCompare;
                }
                String leftId = left.id == null ? "" : left.id;
                String rightId = right.id == null ? "" : right.id;
                return leftId.compareTo(rightId);
            }
        });
        return courses;
    }

    private Integer normalizeGradeLevel(Integer gradeLevel,
                                        String fieldPath,
                                        boolean allowNull,
                                        List<String> details) {
        if (gradeLevel == null) {
            if (!allowNull) {
                details.add(fieldPath + " is required");
            }
            return null;
        }
        if (!ALLOWED_GRADE_LEVELS.contains(gradeLevel)) {
            details.add(fieldPath + " must be one of 9, 10, 11, 12, 13");
            return null;
        }
        return gradeLevel;
    }

    private CoursePlanYearStructure parseYearStructure(String rawYearStructure,
                                                       String fieldPath,
                                                       List<String> details) {
        String normalized = trimToNull(rawYearStructure);
        if (normalized == null) {
            details.add(fieldPath + " is required");
            return null;
        }
        try {
            return CoursePlanYearStructure.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            details.add(fieldPath + " must be SEMESTER or FULL_YEAR");
            return null;
        }
    }

    private CoursePlanCourseStatus parseCourseStatus(String rawStatus,
                                                     String fieldPath,
                                                     List<String> details) {
        String normalized = trimToNull(rawStatus);
        if (normalized == null) {
            details.add(fieldPath + " is required");
            return null;
        }
        try {
            return CoursePlanCourseStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            details.add(fieldPath + " must be COMPLETED, IN_PROGRESS, or PLANNED");
            return null;
        }
    }

    private Integer normalizeMark(Integer mark,
                                  CoursePlanCourseStatus status,
                                  String fieldPath,
                                  List<String> details) {
        if (status == CoursePlanCourseStatus.PLANNED) {
            if (mark != null) {
                details.add(fieldPath + " must be null when status is PLANNED");
            }
            return null;
        }
        if (mark == null) {
            return null;
        }
        if (mark.intValue() < 0 || mark.intValue() > 100) {
            details.add(fieldPath + " must be between 0 and 100");
            return null;
        }
        return mark;
    }

    private CoursePlanSemester normalizeSemester(String rawSemester,
                                                 CoursePlanYearStructure yearStructure,
                                                 String fieldPath,
                                                 List<String> details) {
        String normalized = trimToNull(rawSemester);
        if (yearStructure == CoursePlanYearStructure.FULL_YEAR) {
            if (normalized != null) {
                details.add(fieldPath + " must be null when yearStructure is FULL_YEAR");
            }
            return null;
        }
        if (normalized == null) {
            return null;
        }
        try {
            return CoursePlanSemester.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            details.add(fieldPath + " must be S1, S2, or null");
            return null;
        }
    }

    private String normalizeCourseId(String rawId, String fieldPath, List<String> details) {
        String normalized = trimToNull(rawId);
        if (normalized == null) {
            details.add(fieldPath + " is required");
            return null;
        }
        if (normalized.length() > MAX_COURSE_ID_LENGTH) {
            details.add(fieldPath + " must be at most " + MAX_COURSE_ID_LENGTH + " characters");
            return null;
        }
        return normalized;
    }

    private String normalizeCourseCode(String rawCourseCode, String fieldPath, List<String> details) {
        if (rawCourseCode == null) {
            return null;
        }
        String trimmed = rawCourseCode.trim();
        if (trimmed.length() > MAX_COURSE_CODE_LENGTH) {
            details.add(fieldPath + " must be at most " + MAX_COURSE_CODE_LENGTH + " characters");
            return null;
        }
        return trimmed;
    }

    private CurrentStudentContext requireCurrentStudentContext(HttpServletRequest request) {
        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: student role required.");
        }
        Student student = studentRepository.findByUser_Id(operator.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found."));
        return new CurrentStudentContext(student, operator);
    }

    private Student requireTeacherAccessibleStudent(Long studentId, HttpServletRequest request) {
        return requireTeacherAccessibleStudentContext(studentId, request).student;
    }

    private TeacherStudentContext requireTeacherAccessibleStudentContext(Long studentId, HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        Long normalizedStudentId = requirePositiveId(studentId, "studentId");
        Student student = studentRepository.findById(normalizedStudentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + normalizedStudentId));
        ensureTeacherCanAccessStudent(operator, normalizedStudentId);
        return new TeacherStudentContext(student, operator);
    }

    private void ensureTeacherCanAccessStudent(User operator, Long studentId) {
        if (operator.getRole() == UserRole.ADMIN) {
            return;
        }
        if (operator.getRole() != UserRole.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }

        Teacher teacher = teacherRepository.findByUser_Id(operator.getId())
                .orElseThrow(TeacherBindingRequiredException::new);
        boolean assigned = teacherStudentRepository.existsByTeacher_IdAndStudent_IdAndStatus(
                teacher.getId(),
                studentId,
                TeacherStudentStatus.ACTIVE
        );
        if (!assigned) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: student not assigned to current teacher.");
        }
    }

    private Long requirePositiveId(Long id, String fieldName) {
        if (id == null || id.longValue() <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return id;
    }

    private ApiRequestException validationFailed(List<String> details) {
        return new ApiRequestException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Validation failed.", details);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static class CurrentStudentContext {
        private final Student student;
        @SuppressWarnings("unused")
        private final User operator;

        private CurrentStudentContext(Student student, User operator) {
            this.student = student;
            this.operator = operator;
        }
    }

    private static class TeacherStudentContext {
        private final Student student;
        @SuppressWarnings("unused")
        private final User operator;

        private TeacherStudentContext(Student student, User operator) {
            this.student = student;
            this.operator = operator;
        }
    }

    private static class NormalizedCoursePlan {
        private final Integer currentGradeLevel;
        private final boolean grade13Enabled;
        private final List<NormalizedGrade> grades;

        private NormalizedCoursePlan(Integer currentGradeLevel,
                                     boolean grade13Enabled,
                                     List<NormalizedGrade> grades) {
            this.currentGradeLevel = currentGradeLevel;
            this.grade13Enabled = grade13Enabled;
            this.grades = grades;
        }
    }

    private static class NormalizedGrade {
        private final Integer gradeLevel;
        private final CoursePlanYearStructure yearStructure;
        private final List<NormalizedCourse> courses;

        private NormalizedGrade(Integer gradeLevel,
                                CoursePlanYearStructure yearStructure,
                                List<NormalizedCourse> courses) {
            this.gradeLevel = gradeLevel;
            this.yearStructure = yearStructure;
            this.courses = courses;
        }
    }

    private static class NormalizedCourse {
        private final String id;
        private final String courseCode;
        private final CoursePlanCourseStatus status;
        private final Integer mark;
        private final CoursePlanSemester semester;
        private final Integer sortOrder;

        private NormalizedCourse(String id,
                                 String courseCode,
                                 CoursePlanCourseStatus status,
                                 Integer mark,
                                 CoursePlanSemester semester,
                                 Integer sortOrder) {
            this.id = id;
            this.courseCode = courseCode;
            this.status = status;
            this.mark = mark;
            this.semester = semester;
            this.sortOrder = sortOrder;
        }
    }
}
