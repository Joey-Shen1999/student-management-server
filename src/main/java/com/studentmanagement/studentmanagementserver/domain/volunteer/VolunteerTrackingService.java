package com.studentmanagement.studentmanagementserver.domain.volunteer;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentVolunteerTrackingRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentVolunteerTrackingTaskRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.service.ApiRequestException;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import com.studentmanagement.studentmanagementserver.service.ManagementAccessService;
import com.studentmanagement.studentmanagementserver.service.TeacherBindingRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class VolunteerTrackingService {

    private static final int NOTE_MAX_LENGTH = 2000;
    private static final int TASK_NAME_MAX_LENGTH = 200;
    private static final int DESCRIPTION_MAX_LENGTH = 2000;
    private static final int VERIFIER_CONTACT_MAX_LENGTH = 255;
    private static final int BATCH_SUMMARY_MAX_STUDENT_IDS = 100;
    private static final BigDecimal HOURS_EQUALITY_TOLERANCE = new BigDecimal("0.01");
    private static final BigDecimal VOLUNTEER_COMPLETED_THRESHOLD = new BigDecimal("40");
    private static final String DEFAULT_TRACKING_TITLE = "Volunteer Tracking";

    private final AuthSessionService authSessionService;
    private final ManagementAccessService managementAccessService;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentVolunteerTrackingRepository studentVolunteerTrackingRepository;
    private final StudentVolunteerTrackingTaskRepository studentVolunteerTrackingTaskRepository;

    public VolunteerTrackingService(AuthSessionService authSessionService,
                                    ManagementAccessService managementAccessService,
                                    StudentRepository studentRepository,
                                    TeacherRepository teacherRepository,
                                    StudentVolunteerTrackingRepository studentVolunteerTrackingRepository,
                                    StudentVolunteerTrackingTaskRepository studentVolunteerTrackingTaskRepository) {
        this.authSessionService = authSessionService;
        this.managementAccessService = managementAccessService;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.studentVolunteerTrackingRepository = studentVolunteerTrackingRepository;
        this.studentVolunteerTrackingTaskRepository = studentVolunteerTrackingTaskRepository;
    }

    @Transactional(readOnly = true)
    public VolunteerTrackingDto getTeacherStudentVolunteerTracking(Long studentId, HttpServletRequest request) {
        TeacherStudentContext context = requireTeacherAccessibleStudentContext(studentId, request);
        return buildVolunteerTrackingDto(context.student);
    }

    @Transactional
    public VolunteerTrackingDto upsertTeacherStudentVolunteerTracking(Long studentId,
                                                                      VolunteerTrackingUpsertRequestDto requestBody,
                                                                      HttpServletRequest request) {
        if (requestBody == null) {
            throw badRequest("request body is required");
        }

        TeacherStudentContext context = requireTeacherAccessibleStudentContext(studentId, request);
        NormalizedVolunteerTracking normalized = normalizeRequestBody(requestBody);
        Teacher updatedByTeacher = resolveTeacherForWrite(context.operator);
        return upsertVolunteerTracking(context.student, normalized, updatedByTeacher);
    }

    @Transactional
    public VolunteerTrackingDto upsertCurrentStudentVolunteerTracking(VolunteerTrackingUpsertRequestDto requestBody,
                                                                      HttpServletRequest request) {
        if (requestBody == null) {
            throw badRequest("request body is required");
        }
        CurrentStudentContext context = requireCurrentStudentContext(request);
        NormalizedVolunteerTracking normalized = normalizeRequestBody(requestBody);
        return upsertVolunteerTracking(context.student, normalized, null);
    }

    private VolunteerTrackingDto upsertVolunteerTracking(Student student,
                                                         NormalizedVolunteerTracking normalized,
                                                         Teacher updatedByTeacher) {
        StudentVolunteerTracking tracking = studentVolunteerTrackingRepository.findByStudent_Id(student.getId())
                .orElse(null);
        if (tracking == null) {
            tracking = new StudentVolunteerTracking(
                    student,
                    normalized.totalHours,
                    normalized.note,
                    updatedByTeacher
            );
        } else {
            tracking.overwrite(normalized.totalHours, normalized.note, updatedByTeacher);
        }
        tracking = studentVolunteerTrackingRepository.save(tracking);

        studentVolunteerTrackingTaskRepository.deleteByTracking_Id(tracking.getId());
        if (!normalized.tasks.isEmpty()) {
            List<StudentVolunteerTrackingTask> toSave =
                    new ArrayList<StudentVolunteerTrackingTask>(normalized.tasks.size());
            for (NormalizedVolunteerTask task : normalized.tasks) {
                toSave.add(new StudentVolunteerTrackingTask(
                        tracking,
                        task.taskName,
                        task.description,
                        task.durationHours,
                        task.startDate,
                        task.endDate,
                        task.verifierContact
                ));
            }
            studentVolunteerTrackingTaskRepository.saveAll(toSave);
        }

        return buildVolunteerTrackingDto(student);
    }

    @Transactional(readOnly = true)
    public VolunteerTrackingDto getCurrentStudentVolunteerTracking(HttpServletRequest request) {
        CurrentStudentContext context = requireCurrentStudentContext(request);
        return buildVolunteerTrackingDto(context.student);
    }

    @Transactional(readOnly = true)
    public List<VolunteerTrackingBatchSummaryItemDto> getVolunteerTrackingBatchSummary(
            VolunteerTrackingBatchSummaryRequestDto requestBody,
            HttpServletRequest request) {
        if (requestBody == null) {
            throw badRequest("request body is required");
        }

        List<Long> studentIds = normalizeBatchStudentIds(requestBody.getStudentIds());
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        List<Student> students = resolveStudentsForBatchRead(operator, studentIds);
        Map<Long, StudentVolunteerTracking> trackingByStudentId = findTrackingByStudentIds(studentIds);

        List<VolunteerTrackingBatchSummaryItemDto> result =
                new ArrayList<VolunteerTrackingBatchSummaryItemDto>(students.size());
        for (Student student : students) {
            if (student == null || student.getId() == null) {
                continue;
            }
            StudentVolunteerTracking tracking = trackingByStudentId.get(student.getId());
            BigDecimal totalHours = tracking == null ? BigDecimal.ZERO : tracking.getTotalHours();
            if (totalHours == null) {
                totalHours = BigDecimal.ZERO;
            }
            result.add(new VolunteerTrackingBatchSummaryItemDto(
                    student.getId(),
                    totalHours,
                    totalHours.compareTo(VOLUNTEER_COMPLETED_THRESHOLD) >= 0,
                    tracking == null || tracking.getUpdatedAt() == null ? null : tracking.getUpdatedAt().toString()
            ));
        }
        return result;
    }

    private VolunteerTrackingDto buildVolunteerTrackingDto(Student student) {
        Long studentId = student == null ? null : student.getId();
        if (studentId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found.");
        }

        StudentVolunteerTracking tracking = studentVolunteerTrackingRepository.findByStudent_Id(studentId).orElse(null);
        if (tracking == null || tracking.getId() == null) {
            return new VolunteerTrackingDto(
                    studentId,
                    BigDecimal.ZERO,
                    null,
                    Collections.<VolunteerTrackingTaskDto>emptyList(),
                    null,
                    null,
                    null,
                    null,
                    Collections.<VolunteerTrackingDto.VolunteerTrackingRecordDto>emptyList()
            );
        }

        List<StudentVolunteerTrackingTask> tasks =
                studentVolunteerTrackingTaskRepository.findByTracking_IdOrderByIdAsc(tracking.getId());
        String createdAt = tracking.getCreatedAt() == null ? null : tracking.getCreatedAt().toString();
        String updatedAt = tracking.getUpdatedAt() == null ? null : tracking.getUpdatedAt().toString();
        Long updatedByTeacherId = tracking.getUpdatedByTeacher() == null ? null : tracking.getUpdatedByTeacher().getId();
        String updatedByTeacherName = tracking.getUpdatedByTeacher() == null ? null : tracking.getUpdatedByTeacher().getName();
        List<VolunteerTrackingTaskDto> taskDtos = toTaskDtos(tasks);
        VolunteerTrackingDto.VolunteerTrackingRecordDto record = new VolunteerTrackingDto.VolunteerTrackingRecordDto(
                tracking.getId(),
                DEFAULT_TRACKING_TITLE,
                tracking.getNote(),
                tracking.getTotalHours(),
                taskDtos,
                createdAt,
                updatedAt,
                updatedByTeacherId,
                updatedByTeacherName
        );
        return new VolunteerTrackingDto(
                studentId,
                tracking.getTotalHours(),
                tracking.getNote(),
                taskDtos,
                createdAt,
                updatedAt,
                updatedByTeacherId,
                updatedByTeacherName,
                Collections.singletonList(record)
        );
    }

    private List<VolunteerTrackingTaskDto> toTaskDtos(List<StudentVolunteerTrackingTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }
        List<VolunteerTrackingTaskDto> items = new ArrayList<VolunteerTrackingTaskDto>(tasks.size());
        for (StudentVolunteerTrackingTask task : tasks) {
            if (task == null) {
                continue;
            }
            items.add(new VolunteerTrackingTaskDto(
                    task.getTaskName(),
                    task.getDescription(),
                    task.getDurationHours(),
                    task.getStartDate(),
                    task.getEndDate(),
                    task.getVerifierContact()
            ));
        }
        return items;
    }

    private NormalizedVolunteerTracking normalizeRequestBody(VolunteerTrackingUpsertRequestDto requestBody) {
        List<VolunteerTrackingTaskUpsertDto> tasks = requestBody.getTasks();
        if (tasks == null || tasks.isEmpty()) {
            throw badRequest("tasks must contain at least one item");
        }

        BigDecimal requestedTotalHours = requestBody.getTotalHours();
        if (requestedTotalHours == null) {
            throw badRequest("totalHours is required");
        }

        List<NormalizedVolunteerTask> normalizedTasks = new ArrayList<NormalizedVolunteerTask>(tasks.size());
        BigDecimal computedTotal = BigDecimal.ZERO;
        for (int i = 0; i < tasks.size(); i++) {
            VolunteerTrackingTaskUpsertDto rawTask = tasks.get(i);
            String pathPrefix = "tasks[" + i + "]";
            if (rawTask == null) {
                throw badRequest(pathPrefix + " is required");
            }

            String taskName = requireNonBlank(rawTask.getTaskName(), pathPrefix + ".taskName", TASK_NAME_MAX_LENGTH);
            String description = requireNonBlank(
                    rawTask.getDescription(),
                    pathPrefix + ".description",
                    DESCRIPTION_MAX_LENGTH
            );
            String verifierContact = requireNonBlank(
                    rawTask.getVerifierContact(),
                    pathPrefix + ".verifierContact",
                    VERIFIER_CONTACT_MAX_LENGTH
            );

            BigDecimal durationHours = rawTask.getDurationHours();
            if (durationHours == null) {
                throw badRequest(pathPrefix + ".durationHours is required");
            }
            if (durationHours.compareTo(BigDecimal.ZERO) <= 0) {
                throw badRequest(pathPrefix + ".durationHours must be greater than 0");
            }

            LocalDate startDate = rawTask.getStartDate();
            if (startDate == null) {
                throw badRequest(pathPrefix + ".startDate is required");
            }
            LocalDate endDate = rawTask.getEndDate();
            if (endDate == null) {
                throw badRequest(pathPrefix + ".endDate is required");
            }
            if (endDate.isBefore(startDate)) {
                throw badRequest(pathPrefix + ".endDate must be on or after startDate");
            }

            normalizedTasks.add(new NormalizedVolunteerTask(
                    taskName,
                    description,
                    durationHours,
                    startDate,
                    endDate,
                    verifierContact
            ));
            computedTotal = computedTotal.add(durationHours);
        }

        if (requestedTotalHours.subtract(computedTotal).abs().compareTo(HOURS_EQUALITY_TOLERANCE) > 0) {
            throw badRequest("totalHours must equal sum(tasks.durationHours)");
        }

        String note = normalizeOptionalText(requestBody.getNote(), "note", NOTE_MAX_LENGTH);
        return new NormalizedVolunteerTracking(computedTotal, note, normalizedTasks);
    }

    private CurrentStudentContext requireCurrentStudentContext(HttpServletRequest request) {
        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: student role required.");
        }
        Student student = studentRepository.findByUser_Id(operator.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found."));
        return new CurrentStudentContext(student);
    }

    private TeacherStudentContext requireTeacherAccessibleStudentContext(Long studentId, HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        Long normalizedStudentId = requirePositiveId(studentId, "studentId");
        Student student = studentRepository.findById(normalizedStudentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + normalizedStudentId));
        ensureTeacherCanAccessStudent(operator, normalizedStudentId);
        return new TeacherStudentContext(student, operator);
    }

    private List<Long> normalizeBatchStudentIds(List<Long> rawStudentIds) {
        if (rawStudentIds == null || rawStudentIds.isEmpty()) {
            throw badRequest("studentIds is required");
        }
        if (rawStudentIds.size() > BATCH_SUMMARY_MAX_STUDENT_IDS) {
            throw badRequest("studentIds size must be <= " + BATCH_SUMMARY_MAX_STUDENT_IDS);
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

    private List<Student> resolveStudentsForBatchRead(User operator, List<Long> studentIds) {
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
            orderedStudents.add(student);
        }
        ensureTeacherCanAccessStudents(operator, studentIds);
        return orderedStudents;
    }

    private Map<Long, StudentVolunteerTracking> findTrackingByStudentIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<StudentVolunteerTracking> trackings = studentVolunteerTrackingRepository.findByStudent_IdIn(studentIds);
        if (trackings.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, StudentVolunteerTracking> trackingByStudentId =
                new HashMap<Long, StudentVolunteerTracking>(trackings.size());
        for (StudentVolunteerTracking tracking : trackings) {
            if (tracking == null || tracking.getStudent() == null || tracking.getStudent().getId() == null) {
                continue;
            }
            trackingByStudentId.put(tracking.getStudent().getId(), tracking);
        }
        return trackingByStudentId;
    }

    private void ensureTeacherCanAccessStudent(User operator, Long studentId) {
        if (operator.getRole() == UserRole.ADMIN || operator.getRole() == UserRole.TEACHER) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
    }

    private void ensureTeacherCanAccessStudents(User operator, List<Long> studentIds) {
        if (operator.getRole() == UserRole.ADMIN || operator.getRole() == UserRole.TEACHER) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
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

    private String normalizeOptionalText(String value, String fieldName, int maxLength) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw badRequest(fieldName + " too long");
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

    private static class CurrentStudentContext {
        private final Student student;

        private CurrentStudentContext(Student student) {
            this.student = student;
        }
    }

    private static class TeacherStudentContext {
        private final Student student;
        private final User operator;

        private TeacherStudentContext(Student student, User operator) {
            this.student = student;
            this.operator = operator;
        }
    }

    private static class NormalizedVolunteerTracking {
        private final BigDecimal totalHours;
        private final String note;
        private final List<NormalizedVolunteerTask> tasks;

        private NormalizedVolunteerTracking(BigDecimal totalHours, String note, List<NormalizedVolunteerTask> tasks) {
            this.totalHours = totalHours;
            this.note = note;
            this.tasks = tasks;
        }
    }

    private static class NormalizedVolunteerTask {
        private final String taskName;
        private final String description;
        private final BigDecimal durationHours;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final String verifierContact;

        private NormalizedVolunteerTask(String taskName,
                                        String description,
                                        BigDecimal durationHours,
                                        LocalDate startDate,
                                        LocalDate endDate,
                                        String verifierContact) {
            this.taskName = taskName;
            this.description = description;
            this.durationHours = durationHours;
            this.startDate = startDate;
            this.endDate = endDate;
            this.verifierContact = verifierContact;
        }
    }
}
