package com.studentmanagement.studentmanagementserver.domain.task;

import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.GoalTaskRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
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

    private final AuthSessionService authSessionService;
    private final GoalTaskRepository goalTaskRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherStudentRepository teacherStudentRepository;

    public TaskCenterService(AuthSessionService authSessionService,
                             GoalTaskRepository goalTaskRepository,
                             StudentRepository studentRepository,
                             TeacherRepository teacherRepository,
                             TeacherStudentRepository teacherStudentRepository) {
        this.authSessionService = authSessionService;
        this.goalTaskRepository = goalTaskRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.teacherStudentRepository = teacherStudentRepository;
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
        Teacher teacher = resolveTeacherForWrite(operator);
        if (operator.getRole() == UserRole.TEACHER) {
            ensureTeacherCanAssignStudent(teacher, student.getId());
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

        List<Student> students;
        if (operator.getRole() == UserRole.ADMIN) {
            students = studentRepository.findAllWithUser();
        } else {
            Teacher teacher = requireTeacherByUser(operator);
            students = teacherStudentRepository.findDistinctStudentsByTeacherIdAndStatusWithUser(
                    teacher.getId(),
                    TeacherStudentStatus.ACTIVE
            );
        }

        if (students.isEmpty()) {
            return Collections.emptyList();
        }

        List<Student> sortedStudents = new ArrayList<Student>(students);
        sortedStudents.sort(Comparator.comparing(Student::getId));

        Map<Long, AssignableStudentDto> deduplicated = new LinkedHashMap<Long, AssignableStudentDto>();
        for (Student student : sortedStudents) {
            Long studentId = student.getId();
            if (studentId == null || deduplicated.containsKey(studentId)) {
                continue;
            }
            deduplicated.put(studentId, new AssignableStudentDto(
                    studentId,
                    buildStudentDisplayName(student),
                    student.getUser() == null ? null : trimToNull(student.getUser().getUsername())
            ));
        }
        return new ArrayList<AssignableStudentDto>(deduplicated.values());
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

    private void ensureTeacherCanAssignStudent(Teacher teacher, Long studentId) {
        boolean hasActiveRelation = teacherStudentRepository.existsByTeacher_IdAndStudent_IdAndStatus(
                teacher.getId(),
                studentId,
                TeacherStudentStatus.ACTIVE
        );
        if (!hasActiveRelation) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Forbidden: student is not actively assigned to this teacher."
            );
        }
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
