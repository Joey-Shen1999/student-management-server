package com.studentmanagement.studentmanagementserver.domain.graduation;

import com.studentmanagement.studentmanagementserver.domain.enums.SchoolType;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.student.StudentSchoolRecord;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.InterviewCourseLessonRepository;
import com.studentmanagement.studentmanagementserver.repo.InterviewCoursePlanRepository;
import com.studentmanagement.studentmanagementserver.repo.InterviewCourseRequirementRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentSchoolRecordRepository;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import com.studentmanagement.studentmanagementserver.service.MustChangePasswordRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InterviewCourseService {
    private static final int INITIAL_LESSON_COUNT = 4;

    private final InterviewCoursePlanRepository planRepository;
    private final InterviewCourseLessonRepository lessonRepository;
    private final InterviewCourseRequirementRepository requirementRepository;
    private final StudentRepository studentRepository;
    private final StudentSchoolRecordRepository schoolRepository;
    private final AuthSessionService authSessionService;

    public InterviewCourseService(InterviewCoursePlanRepository planRepository,
                                  InterviewCourseLessonRepository lessonRepository,
                                  InterviewCourseRequirementRepository requirementRepository,
                                  StudentRepository studentRepository,
                                  StudentSchoolRecordRepository schoolRepository,
                                  AuthSessionService authSessionService) {
        this.planRepository = planRepository;
        this.lessonRepository = lessonRepository;
        this.requirementRepository = requirementRepository;
        this.studentRepository = studentRepository;
        this.schoolRepository = schoolRepository;
        this.authSessionService = authSessionService;
    }

    @Transactional(readOnly = true)
    public InterviewCoursePlanDto get(Long studentId, HttpServletRequest request) {
        requireTeacherOrAdmin(request);
        Student student = requireStudent(studentId);
        InterviewCoursePlan plan = planRepository.findByStudent_Id(studentId).orElse(null);
        return toDto(student, plan, graduationYear(studentId),
                plan == null ? Collections.emptyList() : lessonRepository.findByPlan_IdOrderByLessonNumberAsc(plan.getId()),
                plan == null ? Collections.emptyList() : requirementRepository.findByPlan_IdOrderByIdAsc(plan.getId()));
    }

    @Transactional(readOnly = true)
    public InterviewCoursePlanDto getMyInterviewCourse(HttpServletRequest request) {
        User user = authSessionService.requireAuthenticatedUser(request);
        if (user.isMustChangePassword()) throw new MustChangePasswordRequiredException();
        if (user.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student required");
        }
        Student student = studentRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        InterviewCoursePlan plan = planRepository.findByStudent_Id(student.getId()).orElse(null);
        if (plan == null || !plan.isEnabled()) {
            return toDto(student, null, graduationYear(student.getId()),
                    Collections.emptyList(), Collections.emptyList());
        }
        return currentPlan(student, plan);
    }

    @Transactional
    public InterviewCoursePlanDto setEnabled(Long studentId, InterviewCourseEnabledRequest body,
                                             HttpServletRequest request) {
        requireTeacherOrAdmin(request);
        if (body == null || body.getEnabled() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "enabled is required");
        }
        Student student = requireStudent(studentId);
        InterviewCoursePlan plan = planRepository.findByStudent_Id(studentId)
                .orElseGet(() -> planRepository.save(new InterviewCoursePlan(student)));
        plan.setEnabled(body.getEnabled());
        planRepository.save(plan);
        List<InterviewCourseLesson> lessons = lessonRepository.findByPlan_IdOrderByLessonNumberAsc(plan.getId());
        if (body.getEnabled() && lessons.isEmpty()) {
            for (int number = 1; number <= INITIAL_LESSON_COUNT; number++) {
                lessons.add(lessonRepository.save(new InterviewCourseLesson(plan, number)));
            }
        }
        return toDto(student, plan, graduationYear(studentId), lessons,
                requirementRepository.findByPlan_IdOrderByIdAsc(plan.getId()));
    }

    @Transactional
    public InterviewCoursePlanDto addLesson(Long studentId, HttpServletRequest request) {
        requireTeacherOrAdmin(request);
        Student student = requireStudent(studentId);
        InterviewCoursePlan plan = requireEnabledPlan(studentId);
        List<InterviewCourseLesson> lessons = lessonRepository.findByPlan_IdOrderByLessonNumberAsc(plan.getId());
        int nextNumber = lessons.stream().mapToInt(InterviewCourseLesson::getLessonNumber).max().orElse(0) + 1;
        lessons.add(lessonRepository.save(new InterviewCourseLesson(plan, nextNumber)));
        return toDto(student, plan, graduationYear(studentId), lessons,
                requirementRepository.findByPlan_IdOrderByIdAsc(plan.getId()));
    }

    @Transactional
    public InterviewCoursePlanDto updateLesson(Long studentId, Long lessonId, InterviewCourseLessonRequest body,
                                               HttpServletRequest request) {
        requireTeacherOrAdmin(request);
        Student student = requireStudent(studentId);
        InterviewCoursePlan plan = requireEnabledPlan(studentId);
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lesson is required");
        }
        InterviewCourseLesson lesson = lessonRepository.findByIdAndPlan_Id(lessonId, plan.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        if (Boolean.TRUE.equals(body.getConfirmed())
                && (body.getLessonDate() == null || body.getLessonTime() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A date and time are required to confirm a lesson");
        }
        lesson.schedule(body.getLessonDate(), body.getLessonTime());
        if (body.getConfirmed() != null) lesson.setConfirmed(body.getConfirmed());
        lessonRepository.save(lesson);
        return toDto(student, plan, graduationYear(studentId),
                lessonRepository.findByPlan_IdOrderByLessonNumberAsc(plan.getId()),
                requirementRepository.findByPlan_IdOrderByIdAsc(plan.getId()));
    }

    @Transactional
    public InterviewCoursePlanDto addRequirement(Long studentId, InterviewCourseRequirementRequest body,
                                                 HttpServletRequest request) {
        requireTeacherOrAdmin(request);
        Student student = requireStudent(studentId);
        InterviewCoursePlan plan = requireEnabledPlan(studentId);
        String university = requiredName(body == null ? null : body.getUniversityName(), "universityName");
        String program = requiredName(body == null ? null : body.getProgramName(), "programName");
        requirementRepository.save(new InterviewCourseRequirement(plan, university, program));
        return currentPlan(student, plan);
    }

    @Transactional
    public InterviewCoursePlanDto updateRequirement(Long studentId, Long requirementId,
                                                    InterviewCourseRequirementRequest body,
                                                    HttpServletRequest request) {
        requireTeacherOrAdmin(request);
        Student student = requireStudent(studentId);
        InterviewCoursePlan plan = requireEnabledPlan(studentId);
        InterviewCourseRequirement requirement = requirementRepository.findByIdAndPlan_Id(requirementId, plan.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requirement not found"));
        String university = requiredName(body == null ? null : body.getUniversityName(), "universityName");
        String program = requiredName(body == null ? null : body.getProgramName(), "programName");
        requirement.change(university, program);
        requirementRepository.save(requirement);
        return currentPlan(student, plan);
    }

    @Transactional
    public InterviewCoursePlanDto deleteRequirement(Long studentId, Long requirementId,
                                                    HttpServletRequest request) {
        requireTeacherOrAdmin(request);
        Student student = requireStudent(studentId);
        InterviewCoursePlan plan = requireEnabledPlan(studentId);
        InterviewCourseRequirement requirement = requirementRepository.findByIdAndPlan_Id(requirementId, plan.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requirement not found"));
        requirementRepository.delete(requirement);
        return currentPlan(student, plan);
    }

    private String requiredName(String value, String field) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty() || trimmed.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be 1-200 characters");
        }
        return trimmed;
    }

    private InterviewCoursePlanDto currentPlan(Student student, InterviewCoursePlan plan) {
        return toDto(student, plan, graduationYear(student.getId()),
                lessonRepository.findByPlan_IdOrderByLessonNumberAsc(plan.getId()),
                requirementRepository.findByPlan_IdOrderByIdAsc(plan.getId()));
    }

    @Transactional(readOnly = true)
    public List<Long> listEnabledStudentIds(HttpServletRequest request) {
        requireTeacherOrAdmin(request);
        return planRepository.findEnabledForActiveStudents().stream()
                .map(plan -> plan.getStudent().getId())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InterviewCoursePlanDto> list(Integer year, HttpServletRequest request) {
        requireTeacherOrAdmin(request);
        int targetYear = year == null ? currentGraduationYear(LocalDate.now(ZoneId.of("America/Toronto"))) : year;
        if (targetYear < 1900 || targetYear > 2100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid graduation year");
        }
        List<InterviewCoursePlan> plans = planRepository.findEnabledForActiveStudents();
        if (plans.isEmpty()) return Collections.emptyList();

        List<Long> studentIds = plans.stream().map(plan -> plan.getStudent().getId()).collect(Collectors.toList());
        Map<Long, StudentSchoolRecord> primarySchools = primarySchools(studentIds);
        List<Long> planIds = plans.stream().map(InterviewCoursePlan::getId).collect(Collectors.toList());
        Map<Long, List<InterviewCourseLesson>> lessonsByPlan = new HashMap<>();
        for (InterviewCourseLesson lesson : lessonRepository.findByPlan_IdInOrderByPlan_IdAscLessonNumberAsc(planIds)) {
            lessonsByPlan.computeIfAbsent(lesson.getPlan().getId(), ignored -> new ArrayList<>()).add(lesson);
        }
        Map<Long, List<InterviewCourseRequirement>> requirementsByPlan = new HashMap<>();
        for (InterviewCourseRequirement requirement : requirementRepository.findByPlan_IdInOrderByPlan_IdAscIdAsc(planIds)) {
            requirementsByPlan.computeIfAbsent(requirement.getPlan().getId(), ignored -> new ArrayList<>()).add(requirement);
        }

        List<InterviewCoursePlanDto> result = new ArrayList<>();
        for (InterviewCoursePlan plan : plans) {
            Student student = plan.getStudent();
            StudentSchoolRecord school = primarySchools.get(student.getId());
            if (school == null || school.getEndTime() == null || school.getEndTime().getYear() != targetYear) continue;
            result.add(toDto(student, plan, targetYear,
                    lessonsByPlan.getOrDefault(plan.getId(), Collections.emptyList()),
                    requirementsByPlan.getOrDefault(plan.getId(), Collections.emptyList())));
        }
        return result;
    }

    static int currentGraduationYear(LocalDate date) {
        // The September academic year ends with graduation in the following calendar year.
        return date.getYear() + (date.getMonthValue() >= 9 ? 1 : 0);
    }

    private InterviewCoursePlan requireEnabledPlan(Long studentId) {
        InterviewCoursePlan plan = planRepository.findByStudent_Id(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Interview course is not enabled"));
        if (!plan.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Interview course is not enabled");
        }
        return plan;
    }

    private Student requireStudent(Long studentId) {
        if (studentId == null || studentId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid student");
        }
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    }

    private void requireTeacherOrAdmin(HttpServletRequest request) {
        User user = authSessionService.requireAuthenticatedUser(request);
        if (user.isMustChangePassword()) throw new MustChangePasswordRequiredException();
        if (user.getRole() != UserRole.TEACHER && user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher or admin required");
        }
    }

    private Integer graduationYear(Long studentId) {
        StudentSchoolRecord school = primarySchools(Collections.singletonList(studentId)).get(studentId);
        return school == null || school.getEndTime() == null ? null : school.getEndTime().getYear();
    }

    private Map<Long, StudentSchoolRecord> primarySchools(List<Long> studentIds) {
        Map<Long, StudentSchoolRecord> result = new HashMap<>();
        for (StudentSchoolRecord school : schoolRepository.findByStudent_IdInOrderByStudent_IdAscIdAsc(studentIds)) {
            Long studentId = school.getStudent().getId();
            StudentSchoolRecord current = result.get(studentId);
            if (current == null || schoolRank(school) < schoolRank(current)
                    || (schoolRank(school) == schoolRank(current) && newer(school, current))) {
                result.put(studentId, school);
            }
        }
        return result;
    }

    private int schoolRank(StudentSchoolRecord school) {
        return school.getSchoolType() == SchoolType.MAIN ? 0 : school.getSchoolType() == SchoolType.OTHER ? 1 : 2;
    }

    private boolean newer(StudentSchoolRecord candidate, StudentSchoolRecord current) {
        Comparator<LocalDate> dates = Comparator.nullsFirst(Comparator.naturalOrder());
        int endCompare = dates.compare(candidate.getEndTime(), current.getEndTime());
        if (endCompare != 0) return endCompare > 0;
        int startCompare = dates.compare(candidate.getStartTime(), current.getStartTime());
        if (startCompare != 0) return startCompare > 0;
        return candidate.getId() > current.getId();
    }

    private InterviewCoursePlanDto toDto(Student student, InterviewCoursePlan plan, Integer year,
                                         List<InterviewCourseLesson> lessons,
                                         List<InterviewCourseRequirement> requirements) {
        String name = ((student.getFirstName() == null ? "" : student.getFirstName()) + " " +
                (student.getLastName() == null ? "" : student.getLastName())).trim();
        if (name.isEmpty()) name = student.getNickName() == null ? "" : student.getNickName();
        if (name.isEmpty()) name = student.getUser().getUsername();
        List<InterviewCourseLessonDto> lessonDtos = new ArrayList<>();
        for (InterviewCourseLesson lesson : lessons) {
            lessonDtos.add(new InterviewCourseLessonDto(lesson.getId(), lesson.getLessonNumber(),
                    lesson.getLessonDate(), lesson.getLessonTime(), lesson.isConfirmed()));
        }
        List<InterviewCourseRequirementDto> requirementDtos = new ArrayList<>();
        for (InterviewCourseRequirement requirement : requirements) {
            requirementDtos.add(new InterviewCourseRequirementDto(requirement.getId(),
                    requirement.getUniversityName(), requirement.getProgramName()));
        }
        return new InterviewCoursePlanDto(student.getId(), name, student.getUser().getUsername(), year,
                plan != null && plan.isEnabled(), lessonDtos, requirementDtos);
    }
}
