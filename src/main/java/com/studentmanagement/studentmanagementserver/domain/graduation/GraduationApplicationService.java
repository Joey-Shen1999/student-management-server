package com.studentmanagement.studentmanagementserver.domain.graduation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.university.University;
import com.studentmanagement.studentmanagementserver.domain.university.UniversityProgram;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.GraduationApplicationChangeEventRepository;
import com.studentmanagement.studentmanagementserver.repo.GraduationApplicationRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherStudentRepository;
import com.studentmanagement.studentmanagementserver.repo.UniversityProgramRepository;
import com.studentmanagement.studentmanagementserver.repo.UniversityRepository;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import com.studentmanagement.studentmanagementserver.service.MustChangePasswordRequiredException;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class GraduationApplicationService {

    private static final String OPERATION_CONFIRM_STAGE = "CONFIRM_STAGE";
    private static final String OPERATION_ENTER_GRADUATION_STAGE = "ENTER_GRADUATION_STAGE";
    private static final String OPERATION_CREATE_APPLICATION = "CREATE_APPLICATION";
    private static final String OPERATION_UPDATE_APPLICATION = "UPDATE_APPLICATION";
    private static final String OPERATION_DELETE_APPLICATION = "DELETE_APPLICATION";
    private static final String OPERATION_REORDER_APPLICATIONS = "REORDER_APPLICATIONS";

    private final GraduationApplicationRepository applicationRepository;
    private final GraduationApplicationChangeEventRepository changeEventRepository;
    private final StudentRepository studentRepository;
    private final UniversityRepository universityRepository;
    private final UniversityProgramRepository programRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherStudentRepository teacherStudentRepository;
    private final AuthSessionService authSessionService;
    private final ObjectMapper objectMapper;

    public GraduationApplicationService(GraduationApplicationRepository applicationRepository,
                                        GraduationApplicationChangeEventRepository changeEventRepository,
                                        StudentRepository studentRepository,
                                        UniversityRepository universityRepository,
                                        UniversityProgramRepository programRepository,
                                        TeacherRepository teacherRepository,
                                        TeacherStudentRepository teacherStudentRepository,
                                        AuthSessionService authSessionService,
                                        ObjectMapper objectMapper) {
        this.applicationRepository = applicationRepository;
        this.changeEventRepository = changeEventRepository;
        this.studentRepository = studentRepository;
        this.universityRepository = universityRepository;
        this.programRepository = programRepository;
        this.teacherRepository = teacherRepository;
        this.teacherStudentRepository = teacherStudentRepository;
        this.authSessionService = authSessionService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<GraduationApplicationDto> listByStudent(Long studentId, HttpServletRequest request) {
        Student student = requireStudent(studentId);
        requireStudentAccess(student.getId(), request, false);
        return toDtos(applicationRepository.findByStudent_IdOrderBySortOrderAscIdAsc(student.getId()));
    }

    @Transactional(readOnly = true)
    public GraduationApplicationHistoryListDto listHistory(Long studentId,
                                                           Integer page,
                                                           Integer size,
                                                           HttpServletRequest request) {
        Student student = requireStudent(studentId);
        requireStudentAccess(student.getId(), request, false);
        int normalizedPage = normalizeHistoryPage(page);
        int normalizedSize = normalizeHistorySize(size);
        Pageable pageable = PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by(Sort.Order.desc("changedAt"), Sort.Order.desc("id"))
        );
        Page<GraduationApplicationChangeEvent> result =
                changeEventRepository.findByStudentId(student.getId(), pageable);
        GraduationApplicationHistoryListDto response = new GraduationApplicationHistoryListDto();
        response.setItems(toHistoryItems(result.getContent()));
        response.setTotal(result.getTotalElements());
        response.setPage(normalizedPage);
        response.setSize(normalizedSize);
        return response;
    }

    @Transactional
    public List<GraduationApplicationDto> confirmStage(Long studentId,
                                                       GraduationApplicationConfirmRequest requestBody,
                                                       HttpServletRequest request) {
        Student student = requireStudent(studentId);
        User operator = requireStudentAccess(student.getId(), request, true);
        List<GraduationApplicationRequest> items = requestBody == null ? null : requestBody.getApplications();
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "applications is required");
        }

        List<Map<String, Object>> beforeSnapshot =
                snapshotApplications(applicationRepository.findByStudent_IdOrderBySortOrderAscIdAsc(student.getId()));
        boolean alreadyInGraduationStage = !beforeSnapshot.isEmpty();

        List<GraduationApplication> nextApplications = new ArrayList<GraduationApplication>(items.size());
        int sortOrder = 1;
        for (GraduationApplicationRequest item : items) {
            GraduationApplication application = buildApplication(student, item, sortOrder++);
            nextApplications.add(application);
        }

        applicationRepository.deleteByStudent_Id(student.getId());
        applicationRepository.flush();
        applicationRepository.saveAll(nextApplications);
        List<GraduationApplication> saved =
                applicationRepository.findByStudent_IdOrderBySortOrderAscIdAsc(student.getId());
        if (!alreadyInGraduationStage) {
            recordHistory(
                    student,
                    null,
                    OPERATION_ENTER_GRADUATION_STAGE,
                    buildSingleChange("graduationStage", "升学阶段", Boolean.FALSE, Boolean.TRUE),
                    operator,
                    resolveTraceId(request)
            );
        }
        recordHistory(
                student,
                null,
                OPERATION_CONFIRM_STAGE,
                buildSingleChange("applications", "正式大学申请清单", beforeSnapshot, snapshotApplications(saved)),
                operator,
                resolveTraceId(request)
        );
        return toDtos(saved);
    }

    @Transactional
    public GraduationApplicationDto create(Long studentId,
                                           GraduationApplicationRequest requestBody,
                                           HttpServletRequest request) {
        Student student = requireStudent(studentId);
        User operator = requireStudentAccess(student.getId(), request, true);
        Integer maxSortOrder = applicationRepository.findMaxSortOrderByStudentId(student.getId());
        int nextSortOrder = maxSortOrder == null ? 1 : maxSortOrder.intValue() + 1;
        GraduationApplication application = buildApplication(student, requestBody, nextSortOrder);
        GraduationApplication saved = applicationRepository.save(application);
        recordHistory(
                student,
                saved.getId(),
                OPERATION_CREATE_APPLICATION,
                buildSingleChange("application", "正式大学申请", null, snapshotApplication(saved)),
                operator,
                resolveTraceId(request)
        );
        return toDto(saved);
    }

    @Transactional
    public GraduationApplicationDto update(Long applicationId,
                                           GraduationApplicationRequest requestBody,
                                           HttpServletRequest request) {
        GraduationApplication application = requireApplication(applicationId);
        Student student = application.getStudent();
        Long studentId = student == null ? null : student.getId();
        User operator = requireStudentAccess(studentId, request, true);
        Map<String, Object> beforeSnapshot = snapshotApplication(application);

        University university = requireActiveUniversity(requestBody == null ? null : requestBody.getUniversityId());
        UniversityProgram program = requireActiveProgram(requestBody == null ? null : requestBody.getProgramId());
        ensureProgramBelongsToUniversity(program, university);

        application.setUniversity(university);
        application.setProgram(program);
        application.setStatus(normalizeStatus(requestBody == null ? null : requestBody.getStatus()));
        application.setSourceAspirationId(normalizePositiveId(requestBody == null ? null : requestBody.getSourceAspirationId()));
        GraduationApplication saved = applicationRepository.save(application);
        Map<String, Object> afterSnapshot = snapshotApplication(saved);
        recordHistory(
                student,
                saved.getId(),
                OPERATION_UPDATE_APPLICATION,
                buildApplicationChanges(beforeSnapshot, afterSnapshot),
                operator,
                resolveTraceId(request)
        );
        return toDto(saved);
    }

    @Transactional
    public void delete(Long applicationId, HttpServletRequest request) {
        GraduationApplication application = requireApplication(applicationId);
        Student student = application.getStudent();
        Long studentId = student == null ? null : student.getId();
        User operator = requireStudentAccess(studentId, request, true);
        Map<String, Object> beforeSnapshot = snapshotApplication(application);
        applicationRepository.delete(application);
        applicationRepository.flush();
        normalizeSortOrders(studentId);
        recordHistory(
                student,
                applicationId,
                OPERATION_DELETE_APPLICATION,
                buildSingleChange("application", "正式大学申请", beforeSnapshot, null),
                operator,
                resolveTraceId(request)
        );
    }

    @Transactional
    public List<GraduationApplicationDto> reorder(Long studentId,
                                                  List<GraduationApplicationReorderRequest> requestBody,
                                                  HttpServletRequest request) {
        Student student = requireStudent(studentId);
        User operator = requireStudentAccess(student.getId(), request, true);

        List<GraduationApplication> existing =
                applicationRepository.findByStudent_IdOrderBySortOrderAscIdAsc(student.getId());
        if (existing.isEmpty()) {
            return new ArrayList<GraduationApplicationDto>();
        }
        if (requestBody == null || requestBody.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reorder request is required");
        }

        Map<Long, GraduationApplication> existingById = new HashMap<Long, GraduationApplication>();
        for (GraduationApplication application : existing) {
            existingById.put(application.getId(), application);
        }
        List<Map<String, Object>> beforeOrder = snapshotApplicationOrder(existing);

        List<GraduationApplicationReorderRequest> sorted =
                new ArrayList<GraduationApplicationReorderRequest>(requestBody);
        Collections.sort(sorted, new Comparator<GraduationApplicationReorderRequest>() {
            @Override
            public int compare(GraduationApplicationReorderRequest left, GraduationApplicationReorderRequest right) {
                int leftOrder = left == null || left.getSortOrder() == null ? Integer.MAX_VALUE : left.getSortOrder();
                int rightOrder = right == null || right.getSortOrder() == null ? Integer.MAX_VALUE : right.getSortOrder();
                if (leftOrder != rightOrder) {
                    return leftOrder < rightOrder ? -1 : 1;
                }
                Long leftId = left == null ? null : left.getId();
                Long rightId = right == null ? null : right.getId();
                if (leftId == null && rightId == null) return 0;
                if (leftId == null) return 1;
                if (rightId == null) return -1;
                return leftId.compareTo(rightId);
            }
        });

        Set<Long> seenIds = new HashSet<Long>();
        int sortOrder = 1;
        for (GraduationApplicationReorderRequest item : sorted) {
            Long id = item == null ? null : item.getId();
            if (id == null || !existingById.containsKey(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All reordered applications must belong to the student.");
            }
            if (!seenIds.add(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate application id in reorder request: " + id);
            }
            existingById.get(id).setSortOrder(sortOrder++);
        }
        if (seenIds.size() != existing.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reorder request must include all applications for the student.");
        }

        applicationRepository.saveAll(existing);
        List<GraduationApplication> saved =
                applicationRepository.findByStudent_IdOrderBySortOrderAscIdAsc(student.getId());
        recordHistory(
                student,
                null,
                OPERATION_REORDER_APPLICATIONS,
                buildSingleChange("sortOrder", "正式大学申请排序", beforeOrder, snapshotApplicationOrder(saved)),
                operator,
                resolveTraceId(request)
        );
        return toDtos(saved);
    }

    private GraduationApplication buildApplication(Student student,
                                                   GraduationApplicationRequest requestBody,
                                                   int sortOrder) {
        University university = requireActiveUniversity(requestBody == null ? null : requestBody.getUniversityId());
        UniversityProgram program = requireActiveProgram(requestBody == null ? null : requestBody.getProgramId());
        ensureProgramBelongsToUniversity(program, university);

        GraduationApplication application = new GraduationApplication(student, university, program, sortOrder);
        application.setStatus(normalizeStatus(requestBody == null ? null : requestBody.getStatus()));
        application.setSourceAspirationId(normalizePositiveId(requestBody == null ? null : requestBody.getSourceAspirationId()));
        return application;
    }

    private void normalizeSortOrders(Long studentId) {
        if (studentId == null) {
            return;
        }
        List<GraduationApplication> applications =
                applicationRepository.findByStudent_IdOrderBySortOrderAscIdAsc(studentId);
        int sortOrder = 1;
        for (GraduationApplication application : applications) {
            application.setSortOrder(sortOrder++);
        }
        applicationRepository.saveAll(applications);
    }

    private Student requireStudent(Long studentId) {
        if (studentId == null || studentId.longValue() <= 0L) {
            throw new IllegalArgumentException("studentId must be positive");
        }
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentId));
    }

    private GraduationApplication requireApplication(Long applicationId) {
        if (applicationId == null || applicationId.longValue() <= 0L) {
            throw new IllegalArgumentException("applicationId must be positive");
        }
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Graduation application not found: " + applicationId));
    }

    private University requireActiveUniversity(Long universityId) {
        if (universityId == null || universityId.longValue() <= 0L) {
            throw new IllegalArgumentException("universityId is required");
        }
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "University not found: " + universityId));
        if (!university.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "University is not active.");
        }
        return university;
    }

    private UniversityProgram requireActiveProgram(Long programId) {
        if (programId == null || programId.longValue() <= 0L) {
            throw new IllegalArgumentException("programId is required");
        }
        UniversityProgram program = programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "University program not found: " + programId));
        if (!program.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "University program is not active.");
        }
        return program;
    }

    private void ensureProgramBelongsToUniversity(UniversityProgram program, University university) {
        Long programUniversityId = program.getUniversity() == null ? null : program.getUniversity().getId();
        if (programUniversityId == null || !programUniversityId.equals(university.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "programId does not belong to universityId.");
        }
    }

    private GraduationApplicationStatus normalizeStatus(GraduationApplicationStatus status) {
        return status == null ? GraduationApplicationStatus.PREPARING : status;
    }

    private Long normalizePositiveId(Long value) {
        return value == null || value.longValue() <= 0L ? null : value;
    }

    private void recordHistory(Student student,
                               Long applicationId,
                               String operation,
                               List<GraduationApplicationHistoryListDto.FieldChangeDto> changedFields,
                               User operator,
                               String traceId) {
        if (student == null || student.getId() == null) {
            return;
        }
        List<GraduationApplicationHistoryListDto.FieldChangeDto> safeChanges =
                changedFields == null
                        ? new ArrayList<GraduationApplicationHistoryListDto.FieldChangeDto>()
                        : changedFields;
        AuditActor actor = resolveAuditActor(operator, student);
        GraduationApplicationChangeEvent event = new GraduationApplicationChangeEvent();
        event.setStudentId(student.getId());
        event.setApplicationId(applicationId);
        event.setOperation(operation);
        event.setChangedFieldsJson(serializeChangedFields(safeChanges));
        event.setActorUserId(actor.userId);
        event.setActorRole(actor.role);
        event.setActorName(actor.name);
        event.setChangedAt(LocalDateTime.now(ZoneOffset.UTC));
        event.setRequestId(safeTraceId(traceId));
        changeEventRepository.save(event);
    }

    private List<GraduationApplicationHistoryListDto.FieldChangeDto> buildSingleChange(String path,
                                                                                       String label,
                                                                                       Object before,
                                                                                       Object after) {
        List<GraduationApplicationHistoryListDto.FieldChangeDto> changes =
                new ArrayList<GraduationApplicationHistoryListDto.FieldChangeDto>();
        changes.add(new GraduationApplicationHistoryListDto.FieldChangeDto(path, label, before, after));
        return changes;
    }

    private List<GraduationApplicationHistoryListDto.FieldChangeDto> buildApplicationChanges(Map<String, Object> before,
                                                                                            Map<String, Object> after) {
        List<GraduationApplicationHistoryListDto.FieldChangeDto> changes =
                new ArrayList<GraduationApplicationHistoryListDto.FieldChangeDto>();
        addChangeIfDifferent(changes, "university", "大学", before, after, "universityName");
        addChangeIfDifferent(changes, "program", "专业", before, after, "programName");
        addChangeIfDifferent(changes, "facultyName", "Faculty", before, after, "facultyName");
        addChangeIfDifferent(changes, "degreeType", "Degree", before, after, "degreeType");
        addChangeIfDifferent(changes, "status", "申请进度", before, after, "status");
        addChangeIfDifferent(changes, "sortOrder", "排序", before, after, "sortOrder");
        addChangeIfDifferent(changes, "sourceAspirationId", "来源大学目标", before, after, "sourceAspirationId");
        if (changes.isEmpty()) {
            changes.add(new GraduationApplicationHistoryListDto.FieldChangeDto(
                    "application",
                    "正式大学申请",
                    before,
                    after
            ));
        }
        return changes;
    }

    private void addChangeIfDifferent(List<GraduationApplicationHistoryListDto.FieldChangeDto> changes,
                                      String path,
                                      String label,
                                      Map<String, Object> before,
                                      Map<String, Object> after,
                                      String key) {
        Object beforeValue = before == null ? null : before.get(key);
        Object afterValue = after == null ? null : after.get(key);
        if (Objects.equals(beforeValue, afterValue)) {
            return;
        }
        changes.add(new GraduationApplicationHistoryListDto.FieldChangeDto(path, label, beforeValue, afterValue));
    }

    private Map<String, Object> snapshotApplication(GraduationApplication application) {
        if (application == null) {
            return null;
        }
        University university = application.getUniversity();
        UniversityProgram program = application.getProgram();
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("id", application.getId());
        snapshot.put("universityId", university == null ? null : university.getId());
        snapshot.put("universityName", university == null ? null : university.getName());
        snapshot.put("programId", program == null ? null : program.getId());
        snapshot.put("programName", program == null ? null : program.getProgramName());
        snapshot.put("facultyName", program == null ? null : program.getFacultyName());
        snapshot.put("degreeType", program == null ? null : program.getDegreeType());
        snapshot.put("status", application.getStatus() == null ? null : application.getStatus().name());
        snapshot.put("sortOrder", Integer.valueOf(application.getSortOrder()));
        snapshot.put("sourceAspirationId", application.getSourceAspirationId());
        return snapshot;
    }

    private List<Map<String, Object>> snapshotApplications(List<GraduationApplication> applications) {
        List<Map<String, Object>> snapshots = new ArrayList<Map<String, Object>>();
        if (applications == null) {
            return snapshots;
        }
        for (GraduationApplication application : applications) {
            snapshots.add(snapshotApplication(application));
        }
        return snapshots;
    }

    private List<Map<String, Object>> snapshotApplicationOrder(List<GraduationApplication> applications) {
        List<Map<String, Object>> snapshots = new ArrayList<Map<String, Object>>();
        if (applications == null) {
            return snapshots;
        }
        for (GraduationApplication application : applications) {
            Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
            snapshot.put("id", application.getId());
            snapshot.put("universityName", application.getUniversity() == null ? null : application.getUniversity().getName());
            snapshot.put("programName", application.getProgram() == null ? null : application.getProgram().getProgramName());
            snapshot.put("sortOrder", Integer.valueOf(application.getSortOrder()));
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    private String serializeChangedFields(List<GraduationApplicationHistoryListDto.FieldChangeDto> changedFields) {
        try {
            return objectMapper.writeValueAsString(changedFields);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize graduation application history", ex);
        }
    }

    private List<GraduationApplicationHistoryListDto.ItemDto> toHistoryItems(List<GraduationApplicationChangeEvent> events) {
        List<GraduationApplicationHistoryListDto.ItemDto> items =
                new ArrayList<GraduationApplicationHistoryListDto.ItemDto>();
        if (events == null) {
            return items;
        }
        for (GraduationApplicationChangeEvent event : events) {
            GraduationApplicationHistoryListDto.ItemDto item = new GraduationApplicationHistoryListDto.ItemDto();
            item.setId(event.getId());
            item.setStudentId(event.getStudentId());
            item.setApplicationId(event.getApplicationId());
            item.setOperation(event.getOperation());
            item.setActorUserId(event.getActorUserId());
            item.setActorRole(event.getActorRole());
            item.setActorName(event.getActorName());
            item.setChangedAt(formatUtcDateTime(event.getChangedAt() == null ? event.getCreatedAt() : event.getChangedAt()));
            item.setChangedFields(parseChangedFields(event.getChangedFieldsJson()));
            items.add(item);
        }
        return items;
    }

    private List<GraduationApplicationHistoryListDto.FieldChangeDto> parseChangedFields(String rawJson) {
        String raw = trimToNull(rawJson);
        if (raw == null) {
            return new ArrayList<GraduationApplicationHistoryListDto.FieldChangeDto>();
        }
        try {
            List<GraduationApplicationHistoryListDto.FieldChangeDto> parsed = objectMapper.readValue(
                    raw,
                    new TypeReference<List<GraduationApplicationHistoryListDto.FieldChangeDto>>() {
                    }
            );
            return parsed == null ? new ArrayList<GraduationApplicationHistoryListDto.FieldChangeDto>() : parsed;
        } catch (Exception ex) {
            return new ArrayList<GraduationApplicationHistoryListDto.FieldChangeDto>();
        }
    }

    private int normalizeHistoryPage(Integer page) {
        return page == null || page.intValue() < 0 ? 0 : page.intValue();
    }

    private int normalizeHistorySize(Integer size) {
        if (size == null || size.intValue() <= 0) {
            return 20;
        }
        return Math.min(100, size.intValue());
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request == null ? null : request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = request == null ? null : request.getHeader("X-Request-Id");
        }
        return safeTraceId(traceId);
    }

    private String safeTraceId(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return "N/A";
        }
        return text.length() <= 120 ? text : text.substring(0, 120);
    }

    private String formatUtcDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private AuditActor resolveAuditActor(User operator, Student targetStudent) {
        if (operator == null) {
            return new AuditActor(null, "SYSTEM", "System");
        }
        String role = operator.getRole() == null ? "SYSTEM" : operator.getRole().name();
        String name = trimToNull(operator.getUsername());
        if (operator.getRole() == UserRole.TEACHER) {
            Teacher teacher = teacherRepository.findByUser_Id(operator.getId()).orElse(null);
            if (teacher != null && trimToNull(teacher.getName()) != null) {
                name = teacher.getName().trim();
            }
        } else if (operator.getRole() == UserRole.STUDENT && targetStudent != null) {
            String nickName = trimToNull(targetStudent.getNickName());
            if (nickName != null) {
                name = nickName;
            } else {
                String firstName = trimToNull(targetStudent.getFirstName());
                String lastName = trimToNull(targetStudent.getLastName());
                name = trimToNull((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName));
            }
        }
        if (name == null) {
            name = "Unknown";
        }
        return new AuditActor(operator.getId(), role, name);
    }

    private User requireStudentAccess(Long studentId, HttpServletRequest request, boolean requireMutationRole) {
        if (studentId == null || studentId.longValue() <= 0L) {
            throw new IllegalArgumentException("studentId must be positive");
        }
        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.isMustChangePassword()) {
            throw new MustChangePasswordRequiredException();
        }

        if (operator.getRole() == UserRole.ADMIN) {
            return operator;
        }
        if (operator.getRole() == UserRole.STUDENT) {
            if (requireMutationRole) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: student cannot manage graduation applications.");
            }
            Student currentStudent = studentRepository.findByUser_Id(operator.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: student binding required."));
            if (studentId.equals(currentStudent.getId())) {
                return operator;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: student can only access own graduation applications.");
        }
        if (operator.getRole() == UserRole.TEACHER) {
            Teacher teacher = teacherRepository.findByUser_Id(operator.getId())
                    .orElseThrow(TeacherBindingRequiredException::new);
            boolean assigned = teacherStudentRepository.existsByTeacher_IdAndStudent_IdAndStatus(
                    teacher.getId(),
                    studentId,
                    TeacherStudentStatus.ACTIVE
            );
            if (assigned) {
                return operator;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: student not assigned to current teacher.");
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin/student role required.");
    }

    private List<GraduationApplicationDto> toDtos(List<GraduationApplication> applications) {
        List<GraduationApplicationDto> dtos = new ArrayList<GraduationApplicationDto>(applications.size());
        for (GraduationApplication application : applications) {
            dtos.add(toDto(application));
        }
        return dtos;
    }

    private GraduationApplicationDto toDto(GraduationApplication application) {
        University university = application.getUniversity();
        UniversityProgram program = application.getProgram();

        GraduationApplicationDto dto = new GraduationApplicationDto();
        dto.setId(application.getId());
        dto.setStudentId(application.getStudent() == null ? null : application.getStudent().getId());
        dto.setUniversityId(university == null ? null : university.getId());
        dto.setUniversityName(university == null ? null : university.getName());
        dto.setProgramId(program == null ? null : program.getId());
        dto.setProgramName(program == null ? null : program.getProgramName());
        dto.setFacultyName(program == null ? null : program.getFacultyName());
        dto.setDegreeType(program == null ? null : program.getDegreeType());
        dto.setStatus(application.getStatus());
        dto.setSortOrder(application.getSortOrder());
        dto.setSourceAspirationId(application.getSourceAspirationId());
        dto.setCreatedAt(application.getCreatedAt());
        dto.setUpdatedAt(application.getUpdatedAt());
        return dto;
    }

    private static class AuditActor {
        private final Long userId;
        private final String role;
        private final String name;

        private AuditActor(Long userId, String role, String name) {
            this.userId = userId;
            this.role = role;
            this.name = name;
        }
    }
}
