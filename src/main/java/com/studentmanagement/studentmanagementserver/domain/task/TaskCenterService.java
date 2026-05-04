package com.studentmanagement.studentmanagementserver.domain.task;

import com.studentmanagement.studentmanagementserver.domain.enums.SchoolType;
import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserAccountStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.notification.EmailService;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.student.StudentProfile;
import com.studentmanagement.studentmanagementserver.domain.student.StudentSchoolRecord;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherStudent;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.GoalTaskCompletionRepository;
import com.studentmanagement.studentmanagementserver.repo.GoalTaskRepository;
import com.studentmanagement.studentmanagementserver.repo.InfoTaskRecipientRepository;
import com.studentmanagement.studentmanagementserver.repo.InfoTaskRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentProfileRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentSchoolRecordRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherStudentRepository;
import com.studentmanagement.studentmanagementserver.service.ApiRequestException;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import com.studentmanagement.studentmanagementserver.service.TeacherBindingRequiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class TaskCenterService {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterService.class);

    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final int STUDENT_DEFAULT_PAGE = 1;
    private static final int STUDENT_DEFAULT_SIZE = 20;
    private static final int TEACHER_DEFAULT_PAGE = 1;
    private static final int TEACHER_DEFAULT_SIZE = 100;
    private static final int TITLE_MAX_LENGTH = 200;
    private static final int DESCRIPTION_MAX_LENGTH = 2000;
    private static final int PROGRESS_NOTE_MAX_LENGTH = 2000;
    private static final int MAX_COMPLETION_OCCURRENCES = 120;
    private static final int TASK_GROUP_ID_MAX_LENGTH = 64;
    private static final String STUDENT_NOT_ASSIGNABLE_CODE = "STUDENT_NOT_ASSIGNABLE";
    private static final String STUDENT_ARCHIVED_CODE = "STUDENT_ARCHIVED";
    private static final String TRACKING_ASSIGNMENT_NOTICE_TITLE = "学习进度通知";
    private static final String TRACKING_ASSIGNMENT_NOTICE_TAGS = "通知,学习记录";
    private static final String TRACKING_ASSIGNMENT_EMAIL_SUBJECT = "学习进度通知";

    private final AuthSessionService authSessionService;
    private final GoalTaskRepository goalTaskRepository;
    private final GoalTaskCompletionRepository goalTaskCompletionRepository;
    private final InfoTaskRepository infoTaskRepository;
    private final InfoTaskRecipientRepository infoTaskRecipientRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherStudentRepository teacherStudentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentSchoolRecordRepository studentSchoolRecordRepository;
    private final EmailService emailService;
    private final boolean taskTrackingEmailRemindersEnabled;

    public TaskCenterService(AuthSessionService authSessionService,
                             GoalTaskRepository goalTaskRepository,
                             GoalTaskCompletionRepository goalTaskCompletionRepository,
                             InfoTaskRepository infoTaskRepository,
                             InfoTaskRecipientRepository infoTaskRecipientRepository,
                             StudentRepository studentRepository,
                             TeacherRepository teacherRepository,
                             TeacherStudentRepository teacherStudentRepository,
                             StudentProfileRepository studentProfileRepository,
                             StudentSchoolRecordRepository studentSchoolRecordRepository,
                             EmailService emailService,
                             @Value("${app.task-tracking.email-reminders.enabled:true}")
                             boolean taskTrackingEmailRemindersEnabled) {
        this.authSessionService = authSessionService;
        this.goalTaskRepository = goalTaskRepository;
        this.goalTaskCompletionRepository = goalTaskCompletionRepository;
        this.infoTaskRepository = infoTaskRepository;
        this.infoTaskRecipientRepository = infoTaskRecipientRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.teacherStudentRepository = teacherStudentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentSchoolRecordRepository = studentSchoolRecordRepository;
        this.emailService = emailService;
        this.taskTrackingEmailRemindersEnabled = taskTrackingEmailRemindersEnabled;
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
        GoalCycleConfig cycleConfig = buildCycleConfig(requestBody, dueAt);

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

        String taskGroupId = generateTaskGroupId();
        GoalTask goalTask = new GoalTask(title, description, dueAt, student, teacher, taskGroupId);
        applyCycleConfig(goalTask, cycleConfig);
        goalTask.activate(resolveEnrollmentStartDate(dueAt));
        GoalTask saved = goalTaskRepository.save(goalTask);
        sendTrackingAssignmentReminder(
                teacher,
                Collections.singletonList(student),
                title,
                description,
                dueAt,
                buildCycleSummary(requestBody),
                taskGroupId
        );
        return toGoalTaskDto(saved);
    }

    @Transactional
    public GoalGroupResponseDto createGoalGroup(GoalGroupUpsertRequestDto requestBody, HttpServletRequest request) {
        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.TEACHER && operator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }
        if (requestBody == null) {
            throw badRequest("request body is required");
        }

        String title = requireNonBlank(requestBody.getTitle(), "title", TITLE_MAX_LENGTH);
        String description = requireNonBlank(requestBody.getDescription(), "description", DESCRIPTION_MAX_LENGTH);
        LocalDate dueAt = parseDueAt(requestBody.getDueAt());
        GoalCycleConfig cycleConfig = buildCycleConfig(requestBody, dueAt);
        List<Long> studentIds = normalizeStudentIds(requestBody.getStudentIds());

        String taskGroupId = normalizeTaskGroupIdForCreate(requestBody.getTaskGroupId());
        if (goalTaskRepository.existsByTaskGroupId(taskGroupId)) {
            throw new ApiRequestException(HttpStatus.CONFLICT, "RESOURCE_CONFLICT", "taskGroupId already exists");
        }

        Teacher teacher;
        if (operator.getRole() == UserRole.TEACHER) {
            teacher = requireTeacherByUser(operator);
        } else {
            teacher = resolveTeacherForWrite(operator);
        }

        List<Student> students = resolveStudentsForGoalWrite(operator, teacher, studentIds);
        List<GoalTask> toCreate = new ArrayList<GoalTask>(students.size());
        for (Student student : students) {
            GoalTask goalTask = new GoalTask(title, description, dueAt, student, teacher, taskGroupId);
            applyCycleConfig(goalTask, cycleConfig);
            goalTask.activate(resolveEnrollmentStartDate(dueAt));
            toCreate.add(goalTask);
        }
        if (!toCreate.isEmpty()) {
            goalTaskRepository.saveAll(toCreate);
        }

        List<GoalTask> savedGoals = goalTaskRepository.findByTaskGroupIdOrderByIdAsc(taskGroupId);
        sendTrackingAssignmentReminder(
                teacher,
                students,
                title,
                description,
                dueAt,
                buildCycleSummary(requestBody),
                taskGroupId
        );
        return toGoalGroupResponse(taskGroupId, savedGoals);
    }

    @Transactional
    public GoalGroupResponseDto overwriteGoalGroup(String taskGroupIdRaw,
                                                   GoalGroupUpsertRequestDto requestBody,
                                                   HttpServletRequest request) {
        String taskGroupId = requireTaskGroupId(taskGroupIdRaw, "taskGroupId");
        if (requestBody == null) {
            throw badRequest("request body is required");
        }

        String bodyTaskGroupId = normalizeOptionalTaskGroupId(requestBody.getTaskGroupId());
        if (bodyTaskGroupId != null && !taskGroupId.equals(bodyTaskGroupId)) {
            throw badRequest("taskGroupId in path and body must match");
        }

        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.TEACHER && operator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }

        String title = requireNonBlank(requestBody.getTitle(), "title", TITLE_MAX_LENGTH);
        String description = requireNonBlank(requestBody.getDescription(), "description", DESCRIPTION_MAX_LENGTH);
        LocalDate dueAt = parseDueAt(requestBody.getDueAt());
        GoalCycleConfig cycleConfig = buildCycleConfig(requestBody, dueAt);
        List<Long> studentIds = normalizeStudentIds(requestBody.getStudentIds());

        List<GoalTask> existingGoals = goalTaskRepository.findByTaskGroupIdOrderByIdAsc(taskGroupId);
        if (existingGoals.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task group not found.");
        }

        Teacher ownerTeacher = existingGoals.get(0).getAssignedByTeacher();
        Long ownerTeacherId = ownerTeacher == null ? null : ownerTeacher.getId();
        if (ownerTeacherId == null) {
            throw badRequest("taskGroup owner teacher is invalid");
        }
        for (GoalTask goal : existingGoals) {
            Teacher currentTeacher = goal.getAssignedByTeacher();
            Long currentTeacherId = currentTeacher == null ? null : currentTeacher.getId();
            if (!ownerTeacherId.equals(currentTeacherId)) {
                throw badRequest("taskGroup has inconsistent owner teacher");
            }
        }

        if (operator.getRole() == UserRole.TEACHER) {
            Teacher operatorTeacher = requireTeacherByUser(operator);
            if (!ownerTeacherId.equals(operatorTeacher.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: task group is not assigned by current teacher.");
            }
            ownerTeacher = operatorTeacher;
        }

        List<Student> targetStudents = resolveStudentsForGoalWrite(operator, ownerTeacher, studentIds);
        Map<Long, GoalTask> existingByStudentId = new LinkedHashMap<Long, GoalTask>();
        for (GoalTask goal : existingGoals) {
            Student assignedStudent = goal.getAssignedStudent();
            Long assignedStudentId = assignedStudent == null ? null : assignedStudent.getId();
            if (assignedStudentId == null) {
                continue;
            }
            existingByStudentId.put(assignedStudentId, goal);
        }

        List<GoalTask> toSave = new ArrayList<GoalTask>();
        List<Student> newlyAddedStudents = new ArrayList<Student>();
        LocalDate today = LocalDate.now();
        for (Student student : targetStudents) {
            Long studentId = student.getId();
            GoalTask existing = existingByStudentId.remove(studentId);
            if (existing == null) {
                GoalTask goalTask = new GoalTask(title, description, dueAt, student, ownerTeacher, taskGroupId);
                applyCycleConfig(goalTask, cycleConfig);
                goalTask.activate(resolveEnrollmentStartDate(dueAt));
                toSave.add(goalTask);
                newlyAddedStudents.add(student);
                continue;
            }
            existing.updateGoal(title, description, dueAt, student);
            applyCycleConfig(existing, cycleConfig);
            existing.activate(resolveEnrollmentStartDate(dueAt));
            toSave.add(existing);
        }

        for (GoalTask removed : existingByStudentId.values()) {
            removed.updateGoal(title, description, dueAt, removed.getAssignedStudent());
            applyCycleConfig(removed, cycleConfig);
            removed.deactivate(today);
            toSave.add(removed);
        }

        if (!toSave.isEmpty()) {
            goalTaskRepository.saveAll(toSave);
        }

        List<GoalTask> savedGoals = goalTaskRepository.findByTaskGroupIdOrderByIdAsc(taskGroupId);
        sendTrackingAssignmentReminder(
                ownerTeacher,
                newlyAddedStudents,
                title,
                description,
                dueAt,
                buildCycleSummary(requestBody),
                taskGroupId
        );
        return toGoalGroupResponse(taskGroupId, savedGoals);
    }

    @Transactional(readOnly = true)
    public GoalGroupStudentStatusResponseDto getGoalGroupStudentStatuses(String taskGroupIdRaw,
                                                                         HttpServletRequest request) {
        String taskGroupId = requireTaskGroupId(taskGroupIdRaw, "taskGroupId");

        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.TEACHER && operator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }

        Teacher teacher = null;
        if (operator.getRole() == UserRole.TEACHER) {
            teacher = requireTeacherByUser(operator);
        }

        List<GoalTask> goals = goalTaskRepository.findByTaskGroupIdOrderByIdAsc(taskGroupId);
        if (goals.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task group not found.");
        }
        if (teacher != null) {
            for (GoalTask goal : goals) {
                Teacher assignedByTeacher = goal.getAssignedByTeacher();
                Long assignedByTeacherId = assignedByTeacher == null ? null : assignedByTeacher.getId();
                if (!teacher.getId().equals(assignedByTeacherId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: task group is not assigned by current teacher.");
                }
            }
        }

        List<GoalTask> sortedGoals = new ArrayList<GoalTask>(goals);
        Collections.sort(sortedGoals, new Comparator<GoalTask>() {
            @Override
            public int compare(GoalTask left, GoalTask right) {
                int statusCompare = Integer.compare(
                        goalGroupStatusRank(left.getStatus()),
                        goalGroupStatusRank(right.getStatus())
                );
                if (statusCompare != 0) {
                    return statusCompare;
                }
                int nameCompare = buildStudentDisplayName(left.getAssignedStudent())
                        .compareToIgnoreCase(buildStudentDisplayName(right.getAssignedStudent()));
                if (nameCompare != 0) {
                    return nameCompare;
                }
                Long leftId = left.getAssignedStudent() == null ? 0L : left.getAssignedStudent().getId();
                Long rightId = right.getAssignedStudent() == null ? 0L : right.getAssignedStudent().getId();
                return leftId.compareTo(rightId);
            }
        });

        List<Long> studentIds = new ArrayList<Long>(sortedGoals.size());
        for (GoalTask goal : sortedGoals) {
            Student student = goal.getAssignedStudent();
            if (student != null && student.getId() != null) {
                studentIds.add(student.getId());
            }
        }
        Map<Long, StudentProfile> profileByStudentId = findProfilesByStudentIds(studentIds);

        List<Long> goalIds = new ArrayList<Long>(sortedGoals.size());
        for (GoalTask goal : sortedGoals) {
            if (goal.getId() != null) {
                goalIds.add(goal.getId());
            }
        }
        Map<Long, Map<String, GoalTaskCompletionEntry>> completionByGoalId = findCompletionsByGoalId(goalIds);
        Map<String, GoalTaskCompletionColumnDto> columnsByKey = new LinkedHashMap<String, GoalTaskCompletionColumnDto>();

        long completedCount = 0L;
        List<GoalGroupStudentStatusDto> students = new ArrayList<GoalGroupStudentStatusDto>(sortedGoals.size());
        for (GoalTask goal : sortedGoals) {
            Student student = goal.getAssignedStudent();
            Long studentId = student == null ? null : student.getId();
            StudentProfile profile = studentId == null ? null : profileByStudentId.get(studentId);
            List<OccurrenceSpec> occurrenceSpecs = buildOccurrenceSpecs(goal);
            Map<String, GoalTaskCompletionEntry> completionByKey =
                    goal.getId() == null ? Collections.<String, GoalTaskCompletionEntry>emptyMap() : completionByGoalId.get(goal.getId());
            if (completionByKey == null) {
                completionByKey = Collections.emptyMap();
            }
            List<GoalTaskCompletionDto> completions = new ArrayList<GoalTaskCompletionDto>(occurrenceSpecs.size());
            int completedOccurrences = 0;
            boolean useLegacyWholeTaskStatus = completionByKey.isEmpty() && goal.getStatus() == GoalTaskStatus.COMPLETED;
            for (OccurrenceSpec occurrenceSpec : occurrenceSpecs) {
                if (!columnsByKey.containsKey(occurrenceSpec.key)) {
                    columnsByKey.put(occurrenceSpec.key, toCompletionColumnDto(occurrenceSpec));
                }
                GoalTaskCompletionEntry completionEntry = completionByKey.get(occurrenceSpec.key);
                boolean occurrenceCompleted =
                        completionEntry == null ? useLegacyWholeTaskStatus : completionEntry.isCompleted();
                if (occurrenceCompleted) {
                    completedOccurrences += 1;
                }
                completions.add(toCompletionDto(
                        completionEntry,
                        occurrenceSpec,
                        useLegacyWholeTaskStatus,
                        goal.getCompletedAt() == null ? null : goal.getCompletedAt().toString(),
                        goal.getUpdatedAt() == null ? null : goal.getUpdatedAt().toString(),
                        goal.getProgressNote() == null ? "" : goal.getProgressNote()
                ));
            }
            boolean completed = !occurrenceSpecs.isEmpty() && completedOccurrences == occurrenceSpecs.size();
            if (completed) {
                completedCount += 1L;
            }

            students.add(new GoalGroupStudentStatusDto(
                    goal.getId(),
                    goal.getTaskGroupId(),
                    studentId,
                    student == null ? null : buildStudentDisplayName(student),
                    student == null || student.getUser() == null ? null : trimToNull(student.getUser().getUsername()),
                    profile == null ? null : trimToNull(profile.getEmail()),
                    resolveGoalStatusFromCompletionCounts(completedOccurrences, occurrenceSpecs.size(), goal.getStatus()),
                    completed,
                    goal.isActive(),
                    goal.getEnrollmentStartAt() == null ? null : goal.getEnrollmentStartAt().toString(),
                    goal.getEnrollmentEndAt() == null ? null : goal.getEnrollmentEndAt().toString(),
                    completedOccurrences,
                    occurrenceSpecs.size(),
                    goal.getCompletedAt() == null ? null : goal.getCompletedAt().toString(),
                    goal.getUpdatedAt() == null ? null : goal.getUpdatedAt().toString(),
                    goal.getProgressNote() == null ? "" : goal.getProgressNote(),
                    completions
            ));
        }

        GoalTask representative = sortedGoals.get(0);
        long totalAssigned = students.size();
        return new GoalGroupStudentStatusResponseDto(
                taskGroupId,
                representative.getTitle(),
                representative.getDescription(),
                representative.getDueAt() == null ? null : representative.getDueAt().toString(),
                valueOrDefault(representative.getCycleType(), "ONE_TIME"),
                representative.getCycleFrequency(),
                representative.getCycleInterval(),
                representative.getCycleUnit(),
                representative.getCycleLabel(),
                representative.getCycleEndAt() == null ? null : representative.getCycleEndAt().toString(),
                representative.isCycleNoEnd(),
                totalAssigned,
                completedCount,
                totalAssigned - completedCount,
                new ArrayList<GoalTaskCompletionColumnDto>(columnsByKey.values()),
                students
        );
    }

    @Transactional
    public void deleteGoalGroup(String taskGroupIdRaw, HttpServletRequest request) {
        String taskGroupId = requireTaskGroupId(taskGroupIdRaw, "taskGroupId");

        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.TEACHER && operator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }

        Teacher teacher = null;
        if (operator.getRole() == UserRole.TEACHER) {
            teacher = requireTeacherByUser(operator);
        }

        List<GoalTask> goals = goalTaskRepository.findByTaskGroupIdOrderByIdAsc(taskGroupId);
        if (goals.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task group not found.");
        }

        if (teacher != null) {
            for (GoalTask goal : goals) {
                Teacher assignedByTeacher = goal.getAssignedByTeacher();
                Long assignedByTeacherId = assignedByTeacher == null ? null : assignedByTeacher.getId();
                if (!teacher.getId().equals(assignedByTeacherId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: task group is not assigned by current teacher.");
                }
            }
        }

        goalTaskRepository.deleteByTaskGroupId(taskGroupId);
    }

    @Transactional
    public GoalTaskDto updateGoal(Long goalId, UpdateGoalRequestDto requestBody, HttpServletRequest request) {
        Long normalizedGoalId = requirePositiveId(goalId, "goalId");
        if (requestBody == null) {
            throw badRequest("request body is required");
        }

        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.TEACHER && operator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }

        Long studentId = requirePositiveId(requestBody.getStudentId(), "studentId");
        String title = requireNonBlank(requestBody.getTitle(), "title", TITLE_MAX_LENGTH);
        String description = requireNonBlank(requestBody.getDescription(), "description", DESCRIPTION_MAX_LENGTH);
        LocalDate dueAt = parseDueAt(requestBody.getDueAt());

        GoalTask task = goalTaskRepository.findByIdWithRelations(normalizedGoalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found."));

        Teacher teacher = null;
        if (operator.getRole() == UserRole.TEACHER) {
            teacher = requireTeacherByUser(operator);
            if (!task.getAssignedByTeacher().getId().equals(teacher.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: task is not assigned by current teacher.");
            }
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentId));
        if (operator.getRole() == UserRole.TEACHER) {
            ensureStudentAssignableForTeacher(teacher, student);
        } else {
            ensureStudentNotArchived(student);
        }
        if (goalTaskRepository.existsByTaskGroupIdAndAssignedStudent_IdAndIdNot(
                task.getTaskGroupId(),
                student.getId(),
                task.getId()
        )) {
            throw badRequest("student already exists in taskGroup");
        }

        task.updateGoal(title, description, dueAt, student);
        GoalTask saved = goalTaskRepository.save(task);
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

    @Transactional
    public GoalTaskCompletionDto updateTeacherGoalCompletion(Long goalId,
                                                             UpdateGoalCompletionRequestDto requestBody,
                                                             HttpServletRequest request) {
        Long normalizedGoalId = requirePositiveId(goalId, "goalId");
        if (requestBody == null) {
            throw badRequest("request body is required");
        }
        String occurrenceKey = requireNonBlank(requestBody.getOccurrenceKey(), "occurrenceKey", 40);
        boolean completed = requestBody.getCompleted() != null && requestBody.getCompleted().booleanValue();
        String progressNote = normalizeProgressNote(requestBody.getProgressNote());

        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.TEACHER && operator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }

        Teacher teacher = null;
        if (operator.getRole() == UserRole.TEACHER) {
            teacher = requireTeacherByUser(operator);
        }

        GoalTask task = goalTaskRepository.findByIdWithRelations(normalizedGoalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found."));
        if (teacher != null && !task.getAssignedByTeacher().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: task is not assigned by current teacher.");
        }

        OccurrenceSpec occurrenceSpec = findOccurrenceSpec(task, occurrenceKey);
        GoalTaskCompletionEntry completionEntry = goalTaskCompletionRepository
                .findByGoalTask_IdAndOccurrenceKey(task.getId(), occurrenceKey)
                .orElseGet(() -> new GoalTaskCompletionEntry(
                        task,
                        occurrenceSpec.key,
                        occurrenceSpec.label,
                        occurrenceSpec.startAt,
                        occurrenceSpec.endAt
                ));
        completionEntry.syncOccurrence(occurrenceSpec.label, occurrenceSpec.startAt, occurrenceSpec.endAt);
        completionEntry.updateCompleted(completed, progressNote, teacher);
        GoalTaskCompletionEntry savedCompletion = goalTaskCompletionRepository.save(completionEntry);
        refreshGoalStatusFromCompletions(task);
        goalTaskRepository.save(task);
        return toCompletionDto(savedCompletion, occurrenceSpec);
    }

    @Transactional(readOnly = true)
    public List<AssignableStudentDto> listAssignableStudents(String countryRaw,
                                                             String provinceRaw,
                                                             String cityRaw,
                                                             String schoolBoardRaw,
                                                             String graduationSeasonRaw,
                                                             String keywordRaw,
                                                             HttpServletRequest request) {
        String country = normalizeKeyword(countryRaw);
        String province = normalizeKeyword(provinceRaw);
        String city = normalizeKeyword(cityRaw);
        String schoolBoard = normalizeKeyword(schoolBoardRaw);
        String graduationSeason = normalizeKeyword(graduationSeasonRaw);
        String keyword = normalizeKeyword(keywordRaw);

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
            String studentName = buildStudentDisplayName(student);
            String username = student.getUser() == null ? null : trimToNull(student.getUser().getUsername());
            String email = profile == null ? null : trimToNull(profile.getEmail());
            String phone = profile == null ? null : trimToNull(profile.getPhone());
            String graduation = formatGraduation(primarySchool == null ? null : primarySchool.getEndTime());
            String schoolName = primarySchool == null ? null : trimToNull(primarySchool.getSchoolName());
            String canadaIdentity = profile == null ? null : trimToNull(profile.getStatusInCanada());
            String teacherNote = profile == null ? null : trimToNull(profile.getTeacherNote());
            String schoolBoardValue = primarySchool == null ? null : trimToNull(primarySchool.getSchoolBoard());

            if (!matchesSelectorFilters(
                    country,
                    province,
                    city,
                    schoolBoard,
                    graduationSeason,
                    keyword,
                    studentId,
                    studentName,
                    username,
                    email,
                    phone,
                    graduation,
                    schoolName,
                    canadaIdentity,
                    schoolBoardValue,
                    summaryCountry,
                    summaryProvince,
                    summaryCity,
                    teacherNote
            )) {
                continue;
            }

            result.add(new AssignableStudentDto(
                    studentId,
                    studentName,
                    username,
                    email,
                    phone,
                    graduation,
                    schoolName,
                    canadaIdentity,
                    summarizeGender(profile),
                    profile == null ? null : trimToNull(profile.getCitizenship()),
                    profile == null ? null : trimToNull(profile.getFirstLanguage()),
                    schoolBoardValue,
                    summaryCountry,
                    summaryProvince,
                    summaryCity,
                    teacherNote,
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

    private GoalGroupResponseDto toGoalGroupResponse(String taskGroupId, List<GoalTask> goals) {
        List<GoalTaskDto> items = new ArrayList<GoalTaskDto>(goals.size());
        for (GoalTask task : goals) {
            if (task == null || !task.isActive()) {
                continue;
            }
            items.add(toGoalTaskDto(task));
        }
        return new GoalGroupResponseDto(taskGroupId, items, items.size());
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
                task.getTaskGroupId(),
                valueOrDefault(task.getCycleType(), "ONE_TIME"),
                task.getCycleFrequency(),
                task.getCycleInterval(),
                task.getCycleUnit(),
                task.getCycleLabel(),
                task.getCycleEndAt() == null ? null : task.getCycleEndAt().toString(),
                task.isCycleNoEnd(),
                task.getEnrollmentStartAt() == null ? null : task.getEnrollmentStartAt().toString(),
                task.getEnrollmentEndAt() == null ? null : task.getEnrollmentEndAt().toString(),
                task.isActive(),
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

    private Map<Long, Map<String, GoalTaskCompletionEntry>> findCompletionsByGoalId(List<Long> goalIds) {
        Map<Long, Map<String, GoalTaskCompletionEntry>> result = new HashMap<Long, Map<String, GoalTaskCompletionEntry>>();
        if (goalIds == null || goalIds.isEmpty()) {
            return result;
        }
        List<GoalTaskCompletionEntry> entries = goalTaskCompletionRepository.findByGoalTask_IdIn(goalIds);
        for (GoalTaskCompletionEntry entry : entries) {
            if (entry == null || entry.getGoalTask() == null || entry.getGoalTask().getId() == null) {
                continue;
            }
            Long goalId = entry.getGoalTask().getId();
            Map<String, GoalTaskCompletionEntry> byKey = result.get(goalId);
            if (byKey == null) {
                byKey = new HashMap<String, GoalTaskCompletionEntry>();
                result.put(goalId, byKey);
            }
            byKey.put(entry.getOccurrenceKey(), entry);
        }
        return result;
    }

    private GoalTaskCompletionColumnDto toCompletionColumnDto(OccurrenceSpec occurrenceSpec) {
        return new GoalTaskCompletionColumnDto(
                occurrenceSpec.key,
                occurrenceSpec.label,
                occurrenceSpec.startAt == null ? null : occurrenceSpec.startAt.toString(),
                occurrenceSpec.endAt == null ? null : occurrenceSpec.endAt.toString()
        );
    }

    private GoalTaskCompletionDto toCompletionDto(GoalTaskCompletionEntry entry, OccurrenceSpec occurrenceSpec) {
        return toCompletionDto(entry, occurrenceSpec, false, null, null, "");
    }

    private GoalTaskCompletionDto toCompletionDto(GoalTaskCompletionEntry entry,
                                                  OccurrenceSpec occurrenceSpec,
                                                  boolean fallbackCompleted,
                                                  String fallbackCompletedAt,
                                                  String fallbackUpdatedAt,
                                                  String fallbackProgressNote) {
        return new GoalTaskCompletionDto(
                entry == null ? null : entry.getId(),
                occurrenceSpec.key,
                occurrenceSpec.label,
                occurrenceSpec.startAt == null ? null : occurrenceSpec.startAt.toString(),
                occurrenceSpec.endAt == null ? null : occurrenceSpec.endAt.toString(),
                entry == null ? fallbackCompleted : entry.isCompleted(),
                entry == null || entry.getCompletedAt() == null ? fallbackCompletedAt : entry.getCompletedAt().toString(),
                entry == null || entry.getUpdatedAt() == null ? fallbackUpdatedAt : entry.getUpdatedAt().toString(),
                entry == null || entry.getProgressNote() == null ? valueOrDefault(fallbackProgressNote, "") : entry.getProgressNote()
        );
    }

    private GoalTaskStatus resolveGoalStatusFromCompletionCounts(int completedOccurrences,
                                                                 int totalOccurrences,
                                                                 GoalTaskStatus fallback) {
        if (totalOccurrences <= 0) {
            return fallback == null ? GoalTaskStatus.NOT_STARTED : fallback;
        }
        if (completedOccurrences <= 0) {
            return GoalTaskStatus.NOT_STARTED;
        }
        if (completedOccurrences >= totalOccurrences) {
            return GoalTaskStatus.COMPLETED;
        }
        return GoalTaskStatus.IN_PROGRESS;
    }

    private void refreshGoalStatusFromCompletions(GoalTask task) {
        List<OccurrenceSpec> occurrenceSpecs = buildOccurrenceSpecs(task);
        if (occurrenceSpecs.isEmpty()) {
            task.updateStatus(GoalTaskStatus.NOT_STARTED, task.getProgressNote(), false);
            return;
        }
        Map<String, GoalTaskCompletionEntry> completionByKey = new HashMap<String, GoalTaskCompletionEntry>();
        List<GoalTaskCompletionEntry> entries = goalTaskCompletionRepository.findByGoalTask_IdIn(
                Collections.singletonList(task.getId())
        );
        for (GoalTaskCompletionEntry entry : entries) {
            completionByKey.put(entry.getOccurrenceKey(), entry);
        }
        int completedOccurrences = 0;
        for (OccurrenceSpec occurrenceSpec : occurrenceSpecs) {
            GoalTaskCompletionEntry entry = completionByKey.get(occurrenceSpec.key);
            if (entry != null && entry.isCompleted()) {
                completedOccurrences += 1;
            }
        }
        GoalTaskStatus nextStatus = resolveGoalStatusFromCompletionCounts(
                completedOccurrences,
                occurrenceSpecs.size(),
                task.getStatus()
        );
        task.updateStatus(nextStatus, task.getProgressNote(), false);
    }

    private OccurrenceSpec findOccurrenceSpec(GoalTask task, String occurrenceKey) {
        List<OccurrenceSpec> occurrenceSpecs = buildOccurrenceSpecs(task);
        for (OccurrenceSpec occurrenceSpec : occurrenceSpecs) {
            if (occurrenceSpec.key.equals(occurrenceKey)) {
                return occurrenceSpec;
            }
        }
        throw badRequest("occurrenceKey is not available for this task");
    }

    private List<OccurrenceSpec> buildOccurrenceSpecs(GoalTask task) {
        LocalDate start = firstNonNull(task.getEnrollmentStartAt(), task.getDueAt(), today());
        LocalDate enrollmentEnd = task.getEnrollmentEndAt();
        LocalDate cycleEnd = task.isCycleNoEnd() ? null : task.getCycleEndAt();
        LocalDate end = cycleEnd == null ? today() : cycleEnd;
        if (enrollmentEnd != null && enrollmentEnd.isBefore(end)) {
            end = enrollmentEnd;
        }
        if (end.isBefore(start)) {
            end = start;
        }

        String cycleType = valueOrDefault(task.getCycleType(), "ONE_TIME").toUpperCase(Locale.ROOT);
        List<OccurrenceSpec> result = new ArrayList<OccurrenceSpec>();
        if (!"ROUTINE".equals(cycleType)) {
            result.add(new OccurrenceSpec("once", "Single", start, start));
            return result;
        }

        int stepDays = resolveCycleStepDays(task);
        LocalDate cursor = start;
        int index = 1;
        while (!cursor.isAfter(end) && result.size() < MAX_COMPLETION_OCCURRENCES) {
            LocalDate occurrenceEnd = cursor.plusDays(stepDays - 1L);
            if (occurrenceEnd.isAfter(end)) {
                occurrenceEnd = end;
            }
            String key = cursor.toString();
            String label = buildOccurrenceLabel(task, cursor, index);
            result.add(new OccurrenceSpec(key, label, cursor, occurrenceEnd));
            cursor = cursor.plusDays(stepDays);
            index += 1;
        }
        if (result.isEmpty()) {
            result.add(new OccurrenceSpec(start.toString(), buildOccurrenceLabel(task, start, 1), start, start));
        }
        return result;
    }

    private int resolveCycleStepDays(GoalTask task) {
        String frequency = valueOrDefault(task.getCycleFrequency(), "DAILY").toUpperCase(Locale.ROOT);
        if ("WEEKLY".equals(frequency)) {
            return 7;
        }
        if ("CUSTOM".equals(frequency)) {
            int interval = task.getCycleInterval() == null || task.getCycleInterval().intValue() <= 0
                    ? 1
                    : task.getCycleInterval().intValue();
            String unit = valueOrDefault(task.getCycleUnit(), "DAYS").toUpperCase(Locale.ROOT);
            return "WEEKS".equals(unit) ? interval * 7 : interval;
        }
        return 1;
    }

    private String buildOccurrenceLabel(GoalTask task, LocalDate start, int index) {
        String label = trimToNull(task.getCycleLabel());
        if (label != null) {
            return label + " #" + index;
        }
        return start.toString();
    }

    private LocalDate firstNonNull(LocalDate first, LocalDate second, LocalDate third) {
        if (first != null) return first;
        if (second != null) return second;
        return third;
    }

    private LocalDate today() {
        return LocalDate.now();
    }

    private void sendTrackingAssignmentReminder(Teacher teacher,
                                                List<Student> students,
                                                String trackingTitle,
                                                String trackingDescription,
                                                LocalDate startDate,
                                                String cycleSummary,
                                                String taskGroupId) {
        if (teacher == null || students == null || students.isEmpty()) {
            return;
        }

        String title = trimToNull(trackingTitle);
        if (title == null) {
            title = "学习进度记录";
        }

        String teacherName = buildTeacherDisplayName(teacher);
        String content = buildTrackingAssignmentDetails(
                title,
                trackingDescription,
                teacherName,
                cycleSummary,
                startDate
        );
        InfoTask infoTask = new InfoTask(
                TRACKING_ASSIGNMENT_NOTICE_TITLE,
                content,
                InfoTaskCategory.ACTIVITY,
                TRACKING_ASSIGNMENT_NOTICE_TAGS,
                students.size(),
                teacher,
                null,
                trimToNull(taskGroupId)
        );
        infoTask = infoTaskRepository.save(infoTask);

        List<InfoTaskRecipient> recipients = new ArrayList<InfoTaskRecipient>(students.size());
        for (Student student : students) {
            if (student != null && student.getId() != null) {
                recipients.add(new InfoTaskRecipient(infoTask, student));
            }
        }
        if (!recipients.isEmpty()) {
            infoTaskRecipientRepository.saveAll(recipients);
        }

        if (!taskTrackingEmailRemindersEnabled) {
            return;
        }

        List<String> emails = collectTrackingReminderEmails(students);
        if (emails.isEmpty()) {
            return;
        }

        try {
            emailService.sendTextEmail(
                    emails,
                    TRACKING_ASSIGNMENT_EMAIL_SUBJECT,
                    buildTrackingAssignmentEmailBody(
                            title,
                            trackingDescription,
                            teacherName,
                            cycleSummary,
                            startDate
                    )
            );
        } catch (RuntimeException ex) {
            log.warn("Failed to send tracking assignment reminder email for taskGroupId={}", taskGroupId, ex);
        }
    }

    private String buildTrackingAssignmentEmailBody(String title,
                                                    String description,
                                                    String teacherName,
                                                    String cycleSummary,
                                                    LocalDate startDate) {
        return buildTrackingAssignmentDetails(title, description, teacherName, cycleSummary, startDate);
    }

    private String buildTrackingAssignmentDetails(String title,
                                                  String description,
                                                  String teacherName,
                                                  String cycleSummary,
                                                  LocalDate startDate) {
        StringBuilder body = new StringBuilder();
        body.append("你收到一个新的任务。").append("\n\n");
        body.append("任务名称：").append(valueOrUnset(title)).append("\n");
        body.append("任务内容：").append("\n");
        body.append(valueOrUnset(description)).append("\n\n");
        body.append("发布老师：").append(valueOrUnset(teacherName)).append("\n");
        body.append("周期：").append(valueOrUnset(cycleSummary)).append("\n");
        body.append("开始时间：").append(formatDateOrUnset(startDate));
        return body.toString();
    }

    private String buildCycleSummary(CreateGoalRequestDto requestBody) {
        if (requestBody == null) {
            return "一次性任务";
        }
        return buildCycleSummary(
                requestBody.getCycleType(),
                requestBody.getCycleFrequency(),
                requestBody.getCycleInterval(),
                requestBody.getCycleUnit(),
                requestBody.getCycleLabel()
        );
    }

    private String buildCycleSummary(GoalGroupUpsertRequestDto requestBody) {
        if (requestBody == null) {
            return "一次性任务";
        }
        return buildCycleSummary(
                requestBody.getCycleType(),
                requestBody.getCycleFrequency(),
                requestBody.getCycleInterval(),
                requestBody.getCycleUnit(),
                requestBody.getCycleLabel()
        );
    }

    private GoalCycleConfig buildCycleConfig(CreateGoalRequestDto requestBody, LocalDate startDate) {
        if (requestBody == null) {
            return buildCycleConfig(null, null, null, null, null, null, null, startDate);
        }
        return buildCycleConfig(
                requestBody.getCycleType(),
                requestBody.getCycleFrequency(),
                requestBody.getCycleInterval(),
                requestBody.getCycleUnit(),
                requestBody.getCycleLabel(),
                requestBody.getCycleEndAt(),
                requestBody.getCycleNoEnd(),
                startDate
        );
    }

    private GoalCycleConfig buildCycleConfig(GoalGroupUpsertRequestDto requestBody, LocalDate startDate) {
        if (requestBody == null) {
            return buildCycleConfig(null, null, null, null, null, null, null, startDate);
        }
        return buildCycleConfig(
                requestBody.getCycleType(),
                requestBody.getCycleFrequency(),
                requestBody.getCycleInterval(),
                requestBody.getCycleUnit(),
                requestBody.getCycleLabel(),
                requestBody.getCycleEndAt(),
                requestBody.getCycleNoEnd(),
                startDate
        );
    }

    private GoalCycleConfig buildCycleConfig(String cycleTypeRaw,
                                             String cycleFrequencyRaw,
                                             Integer cycleIntervalRaw,
                                             String cycleUnitRaw,
                                             String cycleLabelRaw,
                                             String cycleEndAtRaw,
                                             Boolean cycleNoEndRaw,
                                             LocalDate startDate) {
        String cycleType = trimToNull(cycleTypeRaw);
        cycleType = cycleType == null ? "ONE_TIME" : cycleType.toUpperCase(Locale.ROOT);
        if (!"ROUTINE".equals(cycleType)) {
            return new GoalCycleConfig("ONE_TIME", null, null, null, trimToNull(cycleLabelRaw), null, true);
        }

        String frequency = trimToNull(cycleFrequencyRaw);
        frequency = frequency == null ? "DAILY" : frequency.toUpperCase(Locale.ROOT);
        if (!"DAILY".equals(frequency) && !"WEEKLY".equals(frequency) && !"CUSTOM".equals(frequency)) {
            frequency = "DAILY";
        }

        Integer cycleInterval = null;
        String cycleUnit = null;
        if ("CUSTOM".equals(frequency)) {
            int normalizedInterval = cycleIntervalRaw == null || cycleIntervalRaw.intValue() <= 0
                    ? 1
                    : Math.min(cycleIntervalRaw.intValue(), 99);
            cycleInterval = Integer.valueOf(normalizedInterval);
            cycleUnit = "WEEKS".equalsIgnoreCase(trimToNull(cycleUnitRaw)) ? "WEEKS" : "DAYS";
        }

        boolean cycleNoEnd = cycleNoEndRaw == null || cycleNoEndRaw.booleanValue();
        LocalDate cycleEndAt = cycleNoEnd ? null : parseOptionalDate(cycleEndAtRaw, "cycleEndAt");
        if (!cycleNoEnd && cycleEndAt == null) {
            throw badRequest("cycleEndAt is required when cycleNoEnd is false");
        }
        if (cycleEndAt != null && startDate != null && cycleEndAt.isBefore(startDate)) {
            throw badRequest("cycleEndAt must be on or after dueAt");
        }

        return new GoalCycleConfig(
                cycleType,
                frequency,
                cycleInterval,
                cycleUnit,
                trimToNull(cycleLabelRaw),
                cycleEndAt,
                cycleNoEnd
        );
    }

    private void applyCycleConfig(GoalTask task, GoalCycleConfig cycleConfig) {
        task.updateCycle(
                cycleConfig.cycleType,
                cycleConfig.cycleFrequency,
                cycleConfig.cycleInterval,
                cycleConfig.cycleUnit,
                cycleConfig.cycleLabel,
                cycleConfig.cycleEndAt,
                cycleConfig.cycleNoEnd
        );
    }

    private LocalDate resolveEnrollmentStartDate(LocalDate taskStartDate) {
        LocalDate now = LocalDate.now();
        if (taskStartDate == null || taskStartDate.isBefore(now)) {
            return now;
        }
        return taskStartDate;
    }

    private String buildCycleSummary(String cycleTypeRaw,
                                     String cycleFrequencyRaw,
                                     Integer cycleInterval,
                                     String cycleUnitRaw,
                                     String cycleLabelRaw) {
        String label = trimToNull(cycleLabelRaw);
        String cycleType = trimToNull(cycleTypeRaw);
        String cadence = null;
        if (cycleType != null && "ROUTINE".equals(cycleType.toUpperCase(Locale.ROOT))) {
            cadence = resolveRoutineCadence(cycleFrequencyRaw, cycleInterval, cycleUnitRaw);
        } else if (cycleType != null && "ONE_TIME".equals(cycleType.toUpperCase(Locale.ROOT))) {
            cadence = "一次性任务";
        }

        if (label != null && cadence != null) {
            return label + "（" + cadence + "）";
        }
        if (label != null) {
            return label;
        }
        return cadence == null ? "一次性任务" : cadence;
    }

    private String resolveRoutineCadence(String cycleFrequencyRaw,
                                         Integer cycleInterval,
                                         String cycleUnitRaw) {
        String frequency = trimToNull(cycleFrequencyRaw);
        if (frequency == null) {
            return "周期任务";
        }
        String normalizedFrequency = frequency.toUpperCase(Locale.ROOT);
        if ("DAILY".equals(normalizedFrequency)) {
            return "每日";
        }
        if ("WEEKLY".equals(normalizedFrequency)) {
            return "每周";
        }
        if ("CUSTOM".equals(normalizedFrequency)) {
            int interval = cycleInterval == null || cycleInterval.intValue() <= 0
                    ? 1
                    : cycleInterval.intValue();
            String unit = trimToNull(cycleUnitRaw);
            String normalizedUnit = unit == null ? "DAYS" : unit.toUpperCase(Locale.ROOT);
            String unitText = "WEEKS".equals(normalizedUnit) ? "周" : "天";
            return "每 " + interval + " " + unitText;
        }
        return "周期任务";
    }

    private String valueOrUnset(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? "未设置" : normalized;
    }

    private String valueOrDefault(String value, String defaultValue) {
        String normalized = trimToNull(value);
        return normalized == null ? defaultValue : normalized;
    }

    private String formatDateOrUnset(LocalDate value) {
        return value == null ? "未设置" : value.toString();
    }

    private List<String> collectTrackingReminderEmails(List<Student> students) {
        if (students == null || students.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> studentIds = new ArrayList<Long>(students.size());
        for (Student student : students) {
            if (student != null && student.getId() != null) {
                studentIds.add(student.getId());
            }
        }
        Map<Long, StudentProfile> profileByStudentId = findProfilesByStudentIds(studentIds);
        LinkedHashSet<String> emails = new LinkedHashSet<String>();
        for (Student student : students) {
            Long studentId = student == null ? null : student.getId();
            StudentProfile profile = studentId == null ? null : profileByStudentId.get(studentId);
            String email = profile == null ? null : trimToNull(profile.getEmail());
            if (email != null && email.contains("@")) {
                emails.add(email);
            }
        }
        return new ArrayList<String>(emails);
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
        TeacherStudent relation = (teacherId == null || studentId == null)
                ? null
                : teacherStudentRepository.findTopByTeacher_IdAndStudent_IdOrderByIdDesc(teacherId, studentId).orElse(null);
        if (relation == null) {
            throw new ApiRequestException(
                    HttpStatus.BAD_REQUEST,
                    STUDENT_NOT_ASSIGNABLE_CODE,
                    "studentId is not assignable to current teacher"
            );
        }
        if (isStudentArchived(student)) {
            throw studentArchivedException();
        }
        if (relation.getStatus() != TeacherStudentStatus.ACTIVE) {
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

    private int goalGroupStatusRank(GoalTaskStatus status) {
        if (status == GoalTaskStatus.NOT_STARTED) {
            return 0;
        }
        if (status == GoalTaskStatus.IN_PROGRESS) {
            return 1;
        }
        if (status == GoalTaskStatus.COMPLETED) {
            return 2;
        }
        return 3;
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

    private boolean matchesSelectorFilters(String country,
                                           String province,
                                           String city,
                                           String schoolBoard,
                                           String graduationSeason,
                                           String keyword,
                                           Long studentId,
                                           String studentName,
                                           String username,
                                           String email,
                                           String phone,
                                           String graduation,
                                           String schoolName,
                                           String canadaIdentity,
                                           String schoolBoardValue,
                                           String summaryCountry,
                                           String summaryProvince,
                                           String summaryCity,
                                           String teacherNote) {
        if (!containsNormalized(summaryCountry, country)) {
            return false;
        }
        if (!containsNormalized(summaryProvince, province)) {
            return false;
        }
        if (!containsNormalized(summaryCity, city)) {
            return false;
        }
        if (!containsNormalized(schoolBoardValue, schoolBoard)) {
            return false;
        }
        if (!containsNormalized(graduation, graduationSeason)) {
            return false;
        }
        if (keyword == null) {
            return true;
        }

        String studentIdText = studentId == null ? null : String.valueOf(studentId);
        return containsNormalized(studentIdText, keyword)
                || containsNormalized(studentName, keyword)
                || containsNormalized(username, keyword)
                || containsNormalized(email, keyword)
                || containsNormalized(phone, keyword)
                || containsNormalized(graduation, keyword)
                || containsNormalized(schoolName, keyword)
                || containsNormalized(canadaIdentity, keyword)
                || containsNormalized(schoolBoardValue, keyword)
                || containsNormalized(summaryCountry, keyword)
                || containsNormalized(summaryProvince, keyword)
                || containsNormalized(summaryCity, keyword)
                || containsNormalized(teacherNote, keyword);
    }

    private boolean containsNormalized(String source, String filter) {
        if (filter == null) {
            return true;
        }
        String normalizedSource = trimToNull(source);
        if (normalizedSource == null) {
            return false;
        }
        return normalizedSource.toLowerCase(Locale.ROOT).contains(filter);
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

    private List<Long> normalizeStudentIds(List<Long> rawStudentIds) {
        if (rawStudentIds == null || rawStudentIds.isEmpty()) {
            throw badRequest("studentIds is required");
        }

        LinkedHashSet<Long> deduplicated = new LinkedHashSet<Long>();
        for (Long studentId : rawStudentIds) {
            if (studentId == null || studentId.longValue() <= 0L) {
                throw badRequest("studentIds must contain positive integers");
            }
            deduplicated.add(studentId);
        }
        if (deduplicated.isEmpty()) {
            throw badRequest("studentIds is required");
        }
        return new ArrayList<Long>(deduplicated);
    }

    private List<Student> resolveStudentsForGoalWrite(User operator, Teacher teacher, List<Long> studentIds) {
        List<Student> sourceStudents = studentRepository.findByIdInWithUser(studentIds);
        Map<Long, Student> studentById = new HashMap<Long, Student>(sourceStudents.size());
        for (Student sourceStudent : sourceStudents) {
            if (sourceStudent == null || sourceStudent.getId() == null) {
                continue;
            }
            studentById.put(sourceStudent.getId(), sourceStudent);
        }

        List<Student> orderedStudents = new ArrayList<Student>(studentIds.size());
        for (Long studentId : studentIds) {
            Student student = studentById.get(studentId);
            if (student == null) {
                throw badRequest("studentId is invalid: " + studentId);
            }
            if (operator.getRole() == UserRole.TEACHER) {
                ensureStudentAssignableForTeacher(teacher, student);
            } else {
                ensureStudentNotArchived(student);
            }
            orderedStudents.add(student);
        }
        return orderedStudents;
    }

    private String normalizeTaskGroupIdForCreate(String rawTaskGroupId) {
        String normalized = normalizeOptionalTaskGroupId(rawTaskGroupId);
        if (normalized != null) {
            return normalized;
        }
        return generateTaskGroupId();
    }

    private String normalizeOptionalTaskGroupId(String rawTaskGroupId) {
        String normalized = trimToNull(rawTaskGroupId);
        if (normalized == null) {
            return null;
        }
        return requireTaskGroupId(normalized, "taskGroupId");
    }

    private String requireTaskGroupId(String rawTaskGroupId, String fieldName) {
        String normalized = trimToNull(rawTaskGroupId);
        if (normalized == null) {
            throw badRequest(fieldName + " is required");
        }
        if (normalized.length() > TASK_GROUP_ID_MAX_LENGTH) {
            throw badRequest(fieldName + " too long");
        }
        return normalized;
    }

    private String generateTaskGroupId() {
        return UUID.randomUUID().toString();
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

    private LocalDate parseOptionalDate(String rawValue, String fieldName) {
        String normalized = trimToNull(rawValue);
        if (normalized == null) {
            return null;
        }
        if (!ISO_DATE_PATTERN.matcher(normalized).matches()) {
            throw badRequest(fieldName + " must be yyyy-mm-dd");
        }
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ex) {
            throw badRequest(fieldName + " must be yyyy-mm-dd");
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

    private static final class OccurrenceSpec {
        private final String key;
        private final String label;
        private final LocalDate startAt;
        private final LocalDate endAt;

        private OccurrenceSpec(String key, String label, LocalDate startAt, LocalDate endAt) {
            this.key = key;
            this.label = label;
            this.startAt = startAt;
            this.endAt = endAt;
        }
    }

    private static final class GoalCycleConfig {
        private final String cycleType;
        private final String cycleFrequency;
        private final Integer cycleInterval;
        private final String cycleUnit;
        private final String cycleLabel;
        private final LocalDate cycleEndAt;
        private final boolean cycleNoEnd;

        private GoalCycleConfig(String cycleType,
                                String cycleFrequency,
                                Integer cycleInterval,
                                String cycleUnit,
                                String cycleLabel,
                                LocalDate cycleEndAt,
                                boolean cycleNoEnd) {
            this.cycleType = cycleType;
            this.cycleFrequency = cycleFrequency;
            this.cycleInterval = cycleInterval;
            this.cycleUnit = cycleUnit;
            this.cycleLabel = cycleLabel;
            this.cycleEndAt = cycleEndAt;
            this.cycleNoEnd = cycleNoEnd;
        }
    }
}
