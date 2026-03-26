package com.studentmanagement.studentmanagementserver.domain.task;

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
import com.studentmanagement.studentmanagementserver.repo.GoalTaskRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentProfileRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentSchoolRecordRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherStudentRepository;
import com.studentmanagement.studentmanagementserver.service.ApiRequestException;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import com.studentmanagement.studentmanagementserver.service.TeacherBindingRequiredException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class TaskCenterService {

    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final int STUDENT_DEFAULT_PAGE = 1;
    private static final int STUDENT_DEFAULT_SIZE = 20;
    private static final int TEACHER_DEFAULT_PAGE = 1;
    private static final int TEACHER_DEFAULT_SIZE = 100;
    private static final int TITLE_MAX_LENGTH = 200;
    private static final int DESCRIPTION_MAX_LENGTH = 2000;
    private static final int PROGRESS_NOTE_MAX_LENGTH = 2000;
    private static final String STUDENT_NOT_ASSIGNABLE_CODE = "STUDENT_NOT_ASSIGNABLE";
    private static final String STUDENT_ARCHIVED_CODE = "STUDENT_ARCHIVED";

    private final AuthSessionService authSessionService;
    private final GoalTaskRepository goalTaskRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherStudentRepository teacherStudentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentSchoolRecordRepository studentSchoolRecordRepository;

    public TaskCenterService(AuthSessionService authSessionService,
                             GoalTaskRepository goalTaskRepository,
                             StudentRepository studentRepository,
                             TeacherRepository teacherRepository,
                             TeacherStudentRepository teacherStudentRepository,
                             StudentProfileRepository studentProfileRepository,
                             StudentSchoolRecordRepository studentSchoolRecordRepository) {
        this.authSessionService = authSessionService;
        this.goalTaskRepository = goalTaskRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.teacherStudentRepository = teacherStudentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentSchoolRecordRepository = studentSchoolRecordRepository;
    }

    @Transactional(readOnly = true)
    public GoalListResponseDto listMyGoals(String typeRaw,
                                           String statusRaw,
                                           String keywordRaw,
                                           String pageRaw,
                                           String sizeRaw,
                                           HttpServletRequest request) {
        requireGoalType(typeRaw);
        GoalTaskStatus status = parseStatusFilter(statusRaw);
        String keyword = normalizeKeyword(keywordRaw);
        int page = parsePositiveIntOrDefault(pageRaw, STUDENT_DEFAULT_PAGE, "page");
        int size = parsePositiveIntOrDefault(sizeRaw, STUDENT_DEFAULT_SIZE, "size");

        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: student role required.");
        }
        Student student = studentRepository.findByUser_Id(operator.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found."));

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "updatedAt", "id"));
        Page<GoalTask> result = goalTaskRepository.findStudentGoals(student.getId(), status, keyword, pageable);
        return toGoalListResponse(result, page, size);
    }

    @Transactional
    public GoalTaskDto updateMyGoalStatus(Long taskId,
                                          UpdateGoalStatusRequestDto requestBody,
                                          HttpServletRequest request) {
        Long normalizedTaskId = requirePositiveId(taskId, "taskId");
        GoalTaskStatus nextStatus = parseRequiredStatus(requestBody == null ? null : requestBody.getStatus());
        String progressNote = normalizeProgressNote(requestBody == null ? null : requestBody.getProgressNote());

        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: student role required.");
        }
        Student student = studentRepository.findByUser_Id(operator.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found."));

        GoalTask task = goalTaskRepository.findByIdWithRelations(normalizedTaskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found."));
        if (!task.getAssignedStudent().getId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: task does not belong to current student.");
        }
        if (!isStudentTransitionAllowed(task.getStatus(), nextStatus)) {
            throw badRequest("status transition not allowed");
        }

        boolean overwriteProgressNote = requestBody != null && requestBody.getProgressNote() != null;
        task.updateStatus(nextStatus, progressNote, overwriteProgressNote);
        GoalTask saved = goalTaskRepository.save(task);
        return toGoalTaskDto(saved);
    }

    @Transactional(readOnly = true)
    public GoalListResponseDto listTeacherGoals(String typeRaw,
                                                String studentIdRaw,
                                                String statusRaw,
                                                String keywordRaw,
                                                String pageRaw,
                                                String sizeRaw,
                                                HttpServletRequest request) {
        requireGoalType(typeRaw);
        Long studentId = parseOptionalPositiveLong(studentIdRaw, "studentId");
        GoalTaskStatus status = parseStatusFilter(statusRaw);
        String keyword = normalizeKeyword(keywordRaw);
        int page = parsePositiveIntOrDefault(pageRaw, TEACHER_DEFAULT_PAGE, "page");
        int size = parsePositiveIntOrDefault(sizeRaw, TEACHER_DEFAULT_SIZE, "size");

        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.TEACHER && operator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }

        Long teacherScopeId = null;
        if (operator.getRole() == UserRole.TEACHER) {
            teacherScopeId = requireTeacherByUser(operator).getId();
        }

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "updatedAt", "id"));
        Page<GoalTask> result = goalTaskRepository.findTeacherGoals(
                teacherScopeId,
                studentId,
                status,
                keyword,
                pageable
        );
        return toGoalListResponse(result, page, size);
    }

    @Transactional
    public GoalTaskDto createGoal(CreateGoalRequestDto requestBody, HttpServletRequest request) {
        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.TEACHER && operator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }
        if (requestBody == null) {
            throw badRequest("request body is required");
        }

        Long studentId = requirePositiveId(requestBody.getStudentId(), "studentId");
        String title = requireNonBlank(requestBody.getTitle(), "title", TITLE_MAX_LENGTH);
        String description = requireNonBlank(requestBody.getDescription(), "description", DESCRIPTION_MAX_LENGTH);
        LocalDate dueAt = parseDueAt(requestBody.getDueAt());

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentId));
        Teacher teacher;
        if (operator.getRole() == UserRole.TEACHER) {
            teacher = requireTeacherByUser(operator);
            ensureStudentAssignableForTeacher(teacher, student);
        } else {
            teacher = resolveTeacherForWrite(operator);
            ensureStudentNotArchived(student);
        }

        GoalTask goalTask = new GoalTask(title, description, dueAt, student, teacher);
        GoalTask saved = goalTaskRepository.save(goalTask);
        return toGoalTaskDto(saved);
    }

    @Transactional
    public GoalTaskDto updateTeacherGoalStatus(Long taskId,
                                               UpdateGoalStatusRequestDto requestBody,
                                               HttpServletRequest request) {
        Long normalizedTaskId = requirePositiveId(taskId, "taskId");
        GoalTaskStatus nextStatus = parseRequiredStatus(requestBody == null ? null : requestBody.getStatus());
        String progressNote = normalizeProgressNote(requestBody == null ? null : requestBody.getProgressNote());

        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.TEACHER && operator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }

        Teacher teacher = null;
        if (operator.getRole() == UserRole.TEACHER) {
            teacher = requireTeacherByUser(operator);
        }

        GoalTask task = goalTaskRepository.findByIdWithRelations(normalizedTaskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found."));
        if (teacher != null && !task.getAssignedByTeacher().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: task is not assigned by current teacher.");
        }

        boolean overwriteProgressNote = requestBody != null && requestBody.getProgressNote() != null;
        task.updateStatus(nextStatus, progressNote, overwriteProgressNote);
        GoalTask saved = goalTaskRepository.save(task);
        return toGoalTaskDto(saved);
    }

    @Transactional(readOnly = true)
    public List<AssignableStudentDto> listAssignableStudents(HttpServletRequest request) {
        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.TEACHER && operator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }

        Map<Long, AssignableStudentStatus> statusByStudentId = new LinkedHashMap<Long, AssignableStudentStatus>();
        List<Student> students;
        if (operator.getRole() == UserRole.TEACHER) {
            Teacher teacher = requireTeacherByUser(operator);
            students = resolveTeacherAssignableStudentsWithStatus(teacher, statusByStudentId);
        } else {
            students = studentRepository.findAllWithUser();
            for (Student student : students) {
                Long studentId = student == null ? null : student.getId();
                if (studentId == null || statusByStudentId.containsKey(studentId)) {
                    continue;
                }
                statusByStudentId.put(studentId, resolveAssignableStatusForAdmin(student));
            }
        }

        if (students.isEmpty()) {
            return Collections.emptyList();
        }

        List<Student> sortedStudents = new ArrayList<Student>(students);
        sortedStudents.sort(Comparator.comparing(Student::getId));

        List<Long> studentIds = new ArrayList<Long>(sortedStudents.size());
        for (Student student : sortedStudents) {
            if (student != null && student.getId() != null) {
                studentIds.add(student.getId());
            }
        }

        Map<Long, StudentProfile> profileByStudentId = findProfilesByStudentIds(studentIds);
        Map<Long, StudentSchoolRecord> schoolByStudentId = findPrimarySchoolByStudentIds(studentIds);

        List<AssignableStudentDto> result = new ArrayList<AssignableStudentDto>(sortedStudents.size());
        for (Student student : sortedStudents) {
            Long studentId = student.getId();
            if (studentId == null) {
                continue;
            }

            AssignableStudentStatus status = statusByStudentId.get(studentId);
            if (status == null) {
                status = resolveAssignableStatusForAdmin(student);
            }
            boolean selectable = status == AssignableStudentStatus.ACTIVE;

            StudentProfile profile = profileByStudentId.get(studentId);
            StudentSchoolRecord primarySchool = schoolByStudentId.get(studentId);
            String summaryCountry = firstNonBlank(
                    primarySchool == null ? null : primarySchool.getCountry(),
                    profile == null ? null : profile.getCountry()
            );
            String summaryProvince = firstNonBlank(
                    primarySchool == null ? null : primarySchool.getState(),
                    profile == null ? null : profile.getState()
            );
            String summaryCity = firstNonBlank(
                    primarySchool == null ? null : primarySchool.getCity(),
                    profile == null ? null : profile.getCity()
            );

            result.add(new AssignableStudentDto(
                    studentId,
                    buildStudentDisplayName(student),
                    student.getUser() == null ? null : trimToNull(student.getUser().getUsername()),
                    profile == null ? null : trimToNull(profile.getEmail()),
                    profile == null ? null : trimToNull(profile.getPhone()),
                    formatGraduation(primarySchool == null ? null : primarySchool.getEndTime()),
                    primarySchool == null ? null : trimToNull(primarySchool.getSchoolName()),
                    profile == null ? null : trimToNull(profile.getStatusInCanada()),
                    summarizeGender(profile),
                    profile == null ? null : trimToNull(profile.getCitizenship()),
                    profile == null ? null : trimToNull(profile.getFirstLanguage()),
                    primarySchool == null ? null : trimToNull(primarySchool.getSchoolBoard()),
                    summaryCountry,
                    summaryProvince,
                    summaryCity,
                    profile == null ? null : trimToNull(profile.getTeacherNote()),
                    status,
                    selectable
            ));
        }
        return result;
    }

    private GoalListResponseDto toGoalListResponse(Page<GoalTask> result, int page, int size) {
        List<GoalTaskDto> items = new ArrayList<GoalTaskDto>(result.getContent().size());
        for (GoalTask task : result.getContent()) {
            items.add(toGoalTaskDto(task));
        }
        return new GoalListResponseDto(items, result.getTotalElements(), page, size);
    }

    private GoalTaskDto toGoalTaskDto(GoalTask task) {
        Student student = task.getAssignedStudent();
        Teacher teacher = task.getAssignedByTeacher();
        return new GoalTaskDto(
                task.getId(),
                "GOAL",
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getDueAt() == null ? null : task.getDueAt().toString(),
                student.getId(),
                buildStudentDisplayName(student),
                teacher.getId(),
                buildTeacherDisplayName(teacher),
                task.getCreatedAt() == null ? null : task.getCreatedAt().toString(),
                task.getUpdatedAt() == null ? null : task.getUpdatedAt().toString(),
                task.getCompletedAt() == null ? null : task.getCompletedAt().toString(),
                task.getProgressNote() == null ? "" : task.getProgressNote()
        );
    }

    private String buildStudentDisplayName(Student student) {
        String nickname = trimToNull(student.getNickName());
        if (nickname != null) {
            return nickname;
        }

        String firstName = trimToNull(student.getFirstName());
        String lastName = trimToNull(student.getLastName());
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        if (firstName != null) {
            return firstName;
        }
        if (lastName != null) {
            return lastName;
        }
        if (student.getUser() != null) {
            String username = trimToNull(student.getUser().getUsername());
            if (username != null) {
                return username;
            }
        }
        return "Student #" + student.getId();
    }

    private String buildTeacherDisplayName(Teacher teacher) {
        String teacherName = trimToNull(teacher.getName());
        if (teacherName != null) {
            return teacherName;
        }
        if (teacher.getUser() != null) {
            String username = trimToNull(teacher.getUser().getUsername());
            if (username != null) {
                return username;
            }
        }
        return "Teacher #" + teacher.getId();
    }

    private void ensureStudentAssignableForTeacher(Teacher teacher, Student student) {
        Long teacherId = teacher == null ? null : teacher.getId();
        Long studentId = student == null ? null : student.getId();
        boolean assigned = teacherId != null
                && studentId != null
                && teacherStudentRepository.existsByTeacher_IdAndStudent_Id(teacherId, studentId);
        if (!assigned) {
            throw new ApiRequestException(
                    HttpStatus.BAD_REQUEST,
                    STUDENT_NOT_ASSIGNABLE_CODE,
                    "studentId is not assignable to current teacher"
            );
        }
        if (isStudentArchived(student)) {
            throw studentArchivedException();
        }
        boolean active = teacherStudentRepository.existsByTeacher_IdAndStudent_IdAndStatus(
                teacherId,
                studentId,
                TeacherStudentStatus.ACTIVE
        );
        if (!active) {
            throw studentArchivedException();
        }
    }

    private void ensureStudentNotArchived(Student student) {
        if (isStudentArchived(student)) {
            throw studentArchivedException();
        }
    }

    private ApiRequestException studentArchivedException() {
        return new ApiRequestException(
                HttpStatus.BAD_REQUEST,
                STUDENT_ARCHIVED_CODE,
                "student is archived and cannot be assigned"
        );
    }

    private boolean isStudentArchived(Student student) {
        if (student == null || student.getUser() == null) {
            return true;
        }
        UserAccountStatus status = student.getUser().getStatus();
        return status == UserAccountStatus.ARCHIVED;
    }

    private List<Student> resolveTeacherAssignableStudentsWithStatus(Teacher teacher,
                                                                     Map<Long, AssignableStudentStatus> statusByStudentId) {
        List<TeacherStudent> relations = teacherStudentRepository.findByTeacherIdWithStudentAndUser(teacher.getId());
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Student> studentsById = new LinkedHashMap<Long, Student>();
        Map<Long, Boolean> hasActiveAssignmentByStudentId = new HashMap<Long, Boolean>();
        for (TeacherStudent relation : relations) {
            Student student = relation.getStudent();
            Long studentId = student == null ? null : student.getId();
            if (studentId == null) {
                continue;
            }
            if (!studentsById.containsKey(studentId)) {
                studentsById.put(studentId, student);
            }
            if (relation.getStatus() == TeacherStudentStatus.ACTIVE) {
                hasActiveAssignmentByStudentId.put(studentId, Boolean.TRUE);
            }
        }

        for (Map.Entry<Long, Student> entry : studentsById.entrySet()) {
            Long studentId = entry.getKey();
            Student student = entry.getValue();
            boolean hasActiveAssignment = Boolean.TRUE.equals(hasActiveAssignmentByStudentId.get(studentId));
            statusByStudentId.put(studentId, resolveAssignableStatus(student, hasActiveAssignment));
        }

        return new ArrayList<Student>(studentsById.values());
    }

    private AssignableStudentStatus resolveAssignableStatus(Student student, boolean hasActiveAssignment) {
        if (isStudentArchived(student)) {
            return AssignableStudentStatus.ARCHIVED;
        }
        return hasActiveAssignment ? AssignableStudentStatus.ACTIVE : AssignableStudentStatus.ARCHIVED;
    }

    private AssignableStudentStatus resolveAssignableStatusForAdmin(Student student) {
        if (isStudentArchived(student)) {
            return AssignableStudentStatus.ARCHIVED;
        }
        return AssignableStudentStatus.ACTIVE;
    }

    private Map<Long, StudentProfile> findProfilesByStudentIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<StudentProfile> profiles = studentProfileRepository.findByStudentIdsWithStudent(studentIds);
        if (profiles.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, StudentProfile> profileByStudentId = new HashMap<Long, StudentProfile>();
        for (StudentProfile profile : profiles) {
            if (profile == null || profile.getStudent() == null || profile.getStudent().getId() == null) {
                continue;
            }
            profileByStudentId.put(profile.getStudent().getId(), profile);
        }
        return profileByStudentId;
    }

    private Map<Long, StudentSchoolRecord> findPrimarySchoolByStudentIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<StudentSchoolRecord> schools =
                studentSchoolRecordRepository.findByStudent_IdInOrderByStudent_IdAscIdAsc(studentIds);
        if (schools.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, StudentSchoolRecord> schoolByStudentId = new HashMap<Long, StudentSchoolRecord>();
        for (StudentSchoolRecord school : schools) {
            if (school == null || school.getStudent() == null || school.getStudent().getId() == null) {
                continue;
            }
            Long studentId = school.getStudent().getId();
            StudentSchoolRecord current = schoolByStudentId.get(studentId);
            if (shouldReplacePrimarySchool(current, school)) {
                schoolByStudentId.put(studentId, school);
            }
        }
        return schoolByStudentId;
    }

    private boolean shouldReplacePrimarySchool(StudentSchoolRecord current, StudentSchoolRecord candidate) {
        if (candidate == null) {
            return false;
        }
        if (current == null) {
            return true;
        }

        int currentTypeRank = schoolTypeRank(current.getSchoolType());
        int candidateTypeRank = schoolTypeRank(candidate.getSchoolType());
        if (candidateTypeRank != currentTypeRank) {
            return candidateTypeRank < currentTypeRank;
        }

        int endTimeCompare = compareDateDescNullLast(candidate.getEndTime(), current.getEndTime());
        if (endTimeCompare != 0) {
            return endTimeCompare < 0;
        }

        int startTimeCompare = compareDateDescNullLast(candidate.getStartTime(), current.getStartTime());
        if (startTimeCompare != 0) {
            return startTimeCompare < 0;
        }

        Long currentId = current.getId() == null ? 0L : current.getId();
        Long candidateId = candidate.getId() == null ? 0L : candidate.getId();
        return candidateId > currentId;
    }

    private int schoolTypeRank(SchoolType schoolType) {
        if (schoolType == SchoolType.MAIN) {
            return 0;
        }
        if (schoolType == SchoolType.OTHER) {
            return 1;
        }
        return 2;
    }

    private int compareDateDescNullLast(LocalDate left, LocalDate right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return right.compareTo(left);
    }

    private String formatGraduation(LocalDate graduationDate) {
        if (graduationDate == null) {
            return null;
        }
        int month = graduationDate.getMonthValue();
        String monthText = month < 10 ? "0" + month : String.valueOf(month);
        return graduationDate.getYear() + "-" + monthText;
    }

    private String summarizeGender(StudentProfile profile) {
        if (profile == null) {
            return null;
        }
        String gender = trimToNull(profile.getGender());
        String genderOther = trimToNull(profile.getGenderOther());
        if (gender == null && genderOther == null) {
            return null;
        }
        if (gender == null) {
            return "Other";
        }

        String normalized = gender.toLowerCase(Locale.ROOT);
        if ("male".equals(normalized)) {
            return "Male";
        }
        if ("female".equals(normalized)) {
            return "Female";
        }
        if ("other".equals(normalized) || normalized.startsWith("other")) {
            return "Other";
        }
        return gender;
    }

    private String firstNonBlank(String first, String second) {
        String firstValue = trimToNull(first);
        if (firstValue != null) {
            return firstValue;
        }
        return trimToNull(second);
    }

    private Teacher resolveTeacherForWrite(User operator) {
        Teacher teacher = teacherRepository.findByUser_Id(operator.getId()).orElse(null);
        if (teacher != null) {
            return teacher;
        }
        if (operator.getRole() == UserRole.TEACHER) {
            throw new TeacherBindingRequiredException();
        }
        String fallbackName = trimToNull(operator.getUsername());
        if (fallbackName == null) {
            fallbackName = "Admin #" + operator.getId();
        }
        return teacherRepository.save(new Teacher(operator, fallbackName));
    }

    private Teacher requireTeacherByUser(User operator) {
        return teacherRepository.findByUser_Id(operator.getId())
                .orElseThrow(TeacherBindingRequiredException::new);
    }

    private void requireGoalType(String typeRaw) {
        String type = trimToNull(typeRaw);
        if (type == null || !"GOAL".equalsIgnoreCase(type)) {
            throw badRequest("type must be GOAL");
        }
    }

    private GoalTaskStatus parseStatusFilter(String statusRaw) {
        String normalized = trimToNull(statusRaw);
        if (normalized == null || "ALL".equalsIgnoreCase(normalized)) {
            return null;
        }
        return parseStatusValue(normalized);
    }

    private GoalTaskStatus parseRequiredStatus(String statusRaw) {
        String normalized = trimToNull(statusRaw);
        if (normalized == null) {
            throw badRequest("status is required");
        }
        return parseStatusValue(normalized);
    }

    private GoalTaskStatus parseStatusValue(String statusRaw) {
        try {
            return GoalTaskStatus.valueOf(statusRaw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw badRequest("status invalid");
        }
    }

    private boolean isStudentTransitionAllowed(GoalTaskStatus current, GoalTaskStatus next) {
        if (current == next) {
            return true;
        }
        if (current == GoalTaskStatus.NOT_STARTED) {
            return next == GoalTaskStatus.IN_PROGRESS;
        }
        if (current == GoalTaskStatus.IN_PROGRESS) {
            return next == GoalTaskStatus.COMPLETED;
        }
        return current == GoalTaskStatus.COMPLETED && next == GoalTaskStatus.IN_PROGRESS;
    }

    private int parsePositiveIntOrDefault(String raw, int defaultValue, String fieldName) {
        String normalized = trimToNull(raw);
        if (normalized == null) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(normalized);
            if (value <= 0) {
                throw badRequest(fieldName + " must be a positive integer");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw badRequest(fieldName + " must be a positive integer");
        }
    }

    private Long parseOptionalPositiveLong(String raw, String fieldName) {
        String normalized = trimToNull(raw);
        if (normalized == null) {
            return null;
        }
        try {
            long value = Long.parseLong(normalized);
            if (value <= 0L) {
                throw badRequest(fieldName + " must be a positive integer");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw badRequest(fieldName + " must be a positive integer");
        }
    }

    private Long requirePositiveId(Long id, String fieldName) {
        if (id == null || id.longValue() <= 0L) {
            throw badRequest(fieldName + " must be positive");
        }
        return id;
    }

    private String requireNonBlank(String value, String fieldName, int maxLength) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw badRequest(fieldName + " is required");
        }
        if (normalized.length() > maxLength) {
            throw badRequest(fieldName + " too long");
        }
        return normalized;
    }

    private LocalDate parseDueAt(String dueAtRaw) {
        String normalized = trimToNull(dueAtRaw);
        if (normalized == null) {
            return null;
        }
        if (!ISO_DATE_PATTERN.matcher(normalized).matches()) {
            throw badRequest("dueAt must be yyyy-mm-dd");
        }
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ex) {
            throw badRequest("dueAt must be yyyy-mm-dd");
        }
    }

    private String normalizeKeyword(String keywordRaw) {
        String normalized = trimToNull(keywordRaw);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeProgressNote(String progressNoteRaw) {
        if (progressNoteRaw == null) {
            return null;
        }
        String normalized = progressNoteRaw.trim();
        if (normalized.length() > PROGRESS_NOTE_MAX_LENGTH) {
            throw badRequest("progressNote too long");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ApiRequestException badRequest(String message) {
        return new ApiRequestException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }
}
