package com.studentmanagement.studentmanagementserver.domain.extracurricular;

import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentExtracurricularActivityRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentExtracurricularTrackingRepository;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ExtracurricularTrackingService {

    private static final int NOTE_MAX_LENGTH = 2000;
    private static final int ACTIVITY_NAME_MAX_LENGTH = 200;
    private static final int ORGANIZATION_MAX_LENGTH = 200;
    private static final int ROLE_MAX_LENGTH = 120;
    private static final int LEVEL_MAX_LENGTH = 40;
    private static final int AWARD_MAX_LENGTH = 255;
    private static final int COMPETITION_CATEGORY_MAX_LENGTH = 120;
    private static final int DESCRIPTION_MAX_LENGTH = 2000;
    private static final int ADMISSION_RELEVANCE_MAX_LENGTH = 2000;
    private static final int PROOF_CONTACT_MAX_LENGTH = 255;
    private static final int PROOF_URL_MAX_LENGTH = 500;
    private static final int BATCH_SUMMARY_MAX_STUDENT_IDS = 100;
    private static final String DEFAULT_TRACKING_TITLE = "Extracurricular Activities";
    private static final String TYPE_COMPETITION = "COMPETITION";
    private static final Set<String> ACTIVITY_TYPES = new HashSet<String>(Arrays.asList(
            "COMPETITION",
            "PUBLIC_EVENT",
            "SUMMER_CAMP",
            "CLUB",
            "RESEARCH",
            "INTERNSHIP",
            "CERTIFICATE",
            "OTHER"
    ));
    private static final Set<String> ACTIVITY_LEVELS = new HashSet<String>(Arrays.asList(
            "SCHOOL",
            "CITY",
            "PROVINCE",
            "NATIONAL",
            "INTERNATIONAL",
            "OTHER"
    ));

    private final AuthSessionService authSessionService;
    private final ManagementAccessService managementAccessService;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherStudentRepository teacherStudentRepository;
    private final StudentExtracurricularTrackingRepository trackingRepository;
    private final StudentExtracurricularActivityRepository activityRepository;

    public ExtracurricularTrackingService(AuthSessionService authSessionService,
                                          ManagementAccessService managementAccessService,
                                          StudentRepository studentRepository,
                                          TeacherRepository teacherRepository,
                                          TeacherStudentRepository teacherStudentRepository,
                                          StudentExtracurricularTrackingRepository trackingRepository,
                                          StudentExtracurricularActivityRepository activityRepository) {
        this.authSessionService = authSessionService;
        this.managementAccessService = managementAccessService;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.teacherStudentRepository = teacherStudentRepository;
        this.trackingRepository = trackingRepository;
        this.activityRepository = activityRepository;
    }

    @Transactional(readOnly = true)
    public ExtracurricularTrackingDto getTeacherStudentExtracurricularTracking(Long studentId,
                                                                               HttpServletRequest request) {
        TeacherStudentContext context = requireTeacherAccessibleStudentContext(studentId, request);
        return buildTrackingDto(context.student);
    }

    @Transactional
    public ExtracurricularTrackingDto upsertTeacherStudentExtracurricularTracking(
            Long studentId,
            ExtracurricularTrackingUpsertRequestDto requestBody,
            HttpServletRequest request) {
        if (requestBody == null) {
            throw badRequest("request body is required");
        }
        TeacherStudentContext context = requireTeacherAccessibleStudentContext(studentId, request);
        NormalizedTracking normalized = normalizeRequestBody(requestBody);
        Teacher updatedByTeacher = resolveTeacherForWrite(context.operator);
        return upsertTracking(context.student, normalized, updatedByTeacher);
    }

    @Transactional
    public ExtracurricularTrackingDto upsertCurrentStudentExtracurricularTracking(
            ExtracurricularTrackingUpsertRequestDto requestBody,
            HttpServletRequest request) {
        if (requestBody == null) {
            throw badRequest("request body is required");
        }
        CurrentStudentContext context = requireCurrentStudentContext(request);
        NormalizedTracking normalized = normalizeRequestBody(requestBody);
        return upsertTracking(context.student, normalized, null);
    }

    @Transactional(readOnly = true)
    public ExtracurricularTrackingDto getCurrentStudentExtracurricularTracking(HttpServletRequest request) {
        CurrentStudentContext context = requireCurrentStudentContext(request);
        return buildTrackingDto(context.student);
    }

    @Transactional(readOnly = true)
    public List<ExtracurricularTrackingBatchSummaryItemDto> getExtracurricularTrackingBatchSummary(
            ExtracurricularTrackingBatchSummaryRequestDto requestBody,
            HttpServletRequest request) {
        if (requestBody == null) {
            throw badRequest("request body is required");
        }
        List<Long> studentIds = normalizeBatchStudentIds(requestBody.getStudentIds());
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        List<Student> students = resolveStudentsForBatchRead(operator, studentIds);
        Map<Long, StudentExtracurricularTracking> trackingByStudentId = findTrackingByStudentIds(studentIds);
        Map<Long, List<StudentExtracurricularActivity>> activitiesByTrackingId =
                findActivitiesByTrackingId(trackingByStudentId.values());

        List<ExtracurricularTrackingBatchSummaryItemDto> result =
                new ArrayList<ExtracurricularTrackingBatchSummaryItemDto>(students.size());
        for (Student student : students) {
            StudentExtracurricularTracking tracking = trackingByStudentId.get(student.getId());
            List<StudentExtracurricularActivity> activities = tracking == null
                    ? Collections.<StudentExtracurricularActivity>emptyList()
                    : activitiesByTrackingId.get(tracking.getId());
            ActivityStats stats = computeStats(activities);
            result.add(new ExtracurricularTrackingBatchSummaryItemDto(
                    student.getId(),
                    stats.totalActivities,
                    stats.competitionCount,
                    stats.awardCount,
                    tracking == null || tracking.getUpdatedAt() == null ? null : tracking.getUpdatedAt().toString()
            ));
        }
        return result;
    }

    private ExtracurricularTrackingDto upsertTracking(Student student,
                                                      NormalizedTracking normalized,
                                                      Teacher updatedByTeacher) {
        StudentExtracurricularTracking tracking = trackingRepository.findByStudent_Id(student.getId()).orElse(null);
        if (tracking == null) {
            tracking = new StudentExtracurricularTracking(student, normalized.note, updatedByTeacher);
        } else {
            tracking.overwrite(normalized.note, updatedByTeacher);
        }
        tracking = trackingRepository.save(tracking);

        activityRepository.deleteByTracking_Id(tracking.getId());
        if (!normalized.activities.isEmpty()) {
            List<StudentExtracurricularActivity> toSave =
                    new ArrayList<StudentExtracurricularActivity>(normalized.activities.size());
            for (NormalizedActivity activity : normalized.activities) {
                toSave.add(new StudentExtracurricularActivity(
                        tracking,
                        activity.activityType,
                        activity.activityName,
                        activity.organization,
                        activity.role,
                        activity.activityLevel,
                        activity.awardOrResult,
                        activity.competitionCategory,
                        activity.activityDate,
                        activity.startDate,
                        activity.endDate,
                        activity.description,
                        activity.admissionRelevance,
                        activity.proofContact,
                        activity.proofUrl
                ));
            }
            activityRepository.saveAll(toSave);
        }
        return buildTrackingDto(student);
    }

    private ExtracurricularTrackingDto buildTrackingDto(Student student) {
        Long studentId = student == null ? null : student.getId();
        if (studentId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found.");
        }

        StudentExtracurricularTracking tracking = trackingRepository.findByStudent_Id(studentId).orElse(null);
        if (tracking == null || tracking.getId() == null) {
            return new ExtracurricularTrackingDto(
                    studentId,
                    null,
                    0,
                    0,
                    0,
                    Collections.<ExtracurricularActivityDto>emptyList(),
                    null,
                    null,
                    null,
                    null,
                    Collections.<ExtracurricularTrackingDto.ExtracurricularTrackingRecordDto>emptyList()
            );
        }

        List<StudentExtracurricularActivity> activities =
                activityRepository.findByTracking_IdOrderByIdAsc(tracking.getId());
        List<ExtracurricularActivityDto> activityDtos = toActivityDtos(activities);
        ActivityStats stats = computeStats(activities);
        String createdAt = tracking.getCreatedAt() == null ? null : tracking.getCreatedAt().toString();
        String updatedAt = tracking.getUpdatedAt() == null ? null : tracking.getUpdatedAt().toString();
        Long updatedByTeacherId = tracking.getUpdatedByTeacher() == null ? null : tracking.getUpdatedByTeacher().getId();
        String updatedByTeacherName = tracking.getUpdatedByTeacher() == null ? null : tracking.getUpdatedByTeacher().getName();

        ExtracurricularTrackingDto.ExtracurricularTrackingRecordDto record =
                new ExtracurricularTrackingDto.ExtracurricularTrackingRecordDto(
                        tracking.getId(),
                        DEFAULT_TRACKING_TITLE,
                        tracking.getNote(),
                        stats.totalActivities,
                        stats.competitionCount,
                        stats.awardCount,
                        activityDtos,
                        createdAt,
                        updatedAt,
                        updatedByTeacherId,
                        updatedByTeacherName
                );
        return new ExtracurricularTrackingDto(
                studentId,
                tracking.getNote(),
                stats.totalActivities,
                stats.competitionCount,
                stats.awardCount,
                activityDtos,
                createdAt,
                updatedAt,
                updatedByTeacherId,
                updatedByTeacherName,
                Collections.singletonList(record)
        );
    }

    private List<ExtracurricularActivityDto> toActivityDtos(List<StudentExtracurricularActivity> activities) {
        if (activities == null || activities.isEmpty()) {
            return Collections.emptyList();
        }
        List<ExtracurricularActivityDto> items =
                new ArrayList<ExtracurricularActivityDto>(activities.size());
        for (StudentExtracurricularActivity activity : activities) {
            if (activity == null) {
                continue;
            }
            items.add(new ExtracurricularActivityDto(
                    activity.getActivityType(),
                    activity.getActivityName(),
                    activity.getOrganization(),
                    activity.getRole(),
                    activity.getActivityLevel(),
                    activity.getAwardOrResult(),
                    activity.getCompetitionCategory(),
                    activity.getActivityDate(),
                    activity.getStartDate(),
                    activity.getEndDate(),
                    activity.getDescription(),
                    activity.getAdmissionRelevance(),
                    activity.getProofContact(),
                    activity.getProofUrl()
            ));
        }
        return items;
    }

    private NormalizedTracking normalizeRequestBody(ExtracurricularTrackingUpsertRequestDto requestBody) {
        List<ExtracurricularActivityUpsertDto> activities = requestBody.getActivities();
        if (activities == null || activities.isEmpty()) {
            throw badRequest("activities must contain at least one item");
        }

        List<NormalizedActivity> normalizedActivities =
                new ArrayList<NormalizedActivity>(activities.size());
        for (int i = 0; i < activities.size(); i++) {
            ExtracurricularActivityUpsertDto rawActivity = activities.get(i);
            String pathPrefix = "activities[" + i + "]";
            if (rawActivity == null) {
                throw badRequest(pathPrefix + " is required");
            }

            String activityType = normalizeActivityType(rawActivity.getActivityType(), pathPrefix + ".activityType");
            String activityName = requireNonBlank(rawActivity.getActivityName(), pathPrefix + ".activityName", ACTIVITY_NAME_MAX_LENGTH);
            String organization = normalizeOptionalText(rawActivity.getOrganization(), pathPrefix + ".organization", ORGANIZATION_MAX_LENGTH);
            String role = normalizeOptionalText(rawActivity.getRole(), pathPrefix + ".role", ROLE_MAX_LENGTH);
            String activityLevel = normalizeActivityLevel(rawActivity.getActivityLevel(), pathPrefix + ".activityLevel");
            String awardOrResult = normalizeOptionalText(rawActivity.getAwardOrResult(), pathPrefix + ".awardOrResult", AWARD_MAX_LENGTH);
            String competitionCategory = normalizeOptionalText(
                    rawActivity.getCompetitionCategory(),
                    pathPrefix + ".competitionCategory",
                    COMPETITION_CATEGORY_MAX_LENGTH
            );
            String description = normalizeOptionalText(rawActivity.getDescription(), pathPrefix + ".description", DESCRIPTION_MAX_LENGTH);
            String admissionRelevance = normalizeOptionalText(
                    rawActivity.getAdmissionRelevance(),
                    pathPrefix + ".admissionRelevance",
                    ADMISSION_RELEVANCE_MAX_LENGTH
            );
            String proofContact = normalizeOptionalText(rawActivity.getProofContact(), pathPrefix + ".proofContact", PROOF_CONTACT_MAX_LENGTH);
            String proofUrl = normalizeOptionalText(rawActivity.getProofUrl(), pathPrefix + ".proofUrl", PROOF_URL_MAX_LENGTH);

            LocalDate activityDate = rawActivity.getActivityDate();
            LocalDate startDate = rawActivity.getStartDate();
            LocalDate endDate = rawActivity.getEndDate();
            if (TYPE_COMPETITION.equals(activityType)) {
                if (activityDate == null) {
                    throw badRequest(pathPrefix + ".activityDate is required for competition records");
                }
                startDate = null;
                endDate = null;
            } else {
                if (startDate == null) {
                    throw badRequest(pathPrefix + ".startDate is required");
                }
                if (endDate == null) {
                    throw badRequest(pathPrefix + ".endDate is required");
                }
                if (endDate.isBefore(startDate)) {
                    throw badRequest(pathPrefix + ".endDate must be on or after startDate");
                }
                activityDate = null;
            }

            normalizedActivities.add(new NormalizedActivity(
                    activityType,
                    activityName,
                    organization,
                    role,
                    activityLevel,
                    awardOrResult,
                    competitionCategory,
                    activityDate,
                    startDate,
                    endDate,
                    description,
                    admissionRelevance,
                    proofContact,
                    proofUrl
            ));
        }

        String note = normalizeOptionalText(requestBody.getNote(), "note", NOTE_MAX_LENGTH);
        return new NormalizedTracking(note, normalizedActivities);
    }

    private ActivityStats computeStats(List<StudentExtracurricularActivity> activities) {
        if (activities == null || activities.isEmpty()) {
            return new ActivityStats(0, 0, 0);
        }
        int totalActivities = 0;
        int competitionCount = 0;
        int awardCount = 0;
        for (StudentExtracurricularActivity activity : activities) {
            if (activity == null) {
                continue;
            }
            totalActivities += 1;
            if (TYPE_COMPETITION.equalsIgnoreCase(trimToNull(activity.getActivityType()))) {
                competitionCount += 1;
            }
            if (trimToNull(activity.getAwardOrResult()) != null) {
                awardCount += 1;
            }
        }
        return new ActivityStats(totalActivities, competitionCount, awardCount);
    }

    private Map<Long, StudentExtracurricularTracking> findTrackingByStudentIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<StudentExtracurricularTracking> trackings = trackingRepository.findByStudent_IdIn(studentIds);
        if (trackings.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, StudentExtracurricularTracking> trackingByStudentId =
                new HashMap<Long, StudentExtracurricularTracking>(trackings.size());
        for (StudentExtracurricularTracking tracking : trackings) {
            if (tracking == null || tracking.getStudent() == null || tracking.getStudent().getId() == null) {
                continue;
            }
            trackingByStudentId.put(tracking.getStudent().getId(), tracking);
        }
        return trackingByStudentId;
    }

    private Map<Long, List<StudentExtracurricularActivity>> findActivitiesByTrackingId(
            Collection<StudentExtracurricularTracking> trackings) {
        if (trackings == null || trackings.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> trackingIds = new ArrayList<Long>(trackings.size());
        for (StudentExtracurricularTracking tracking : trackings) {
            if (tracking != null && tracking.getId() != null) {
                trackingIds.add(tracking.getId());
            }
        }
        if (trackingIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<StudentExtracurricularActivity> activities = activityRepository.findByTracking_IdIn(trackingIds);
        Map<Long, List<StudentExtracurricularActivity>> byTrackingId =
                new LinkedHashMap<Long, List<StudentExtracurricularActivity>>();
        for (StudentExtracurricularActivity activity : activities) {
            if (activity == null || activity.getTracking() == null || activity.getTracking().getId() == null) {
                continue;
            }
            Long trackingId = activity.getTracking().getId();
            List<StudentExtracurricularActivity> list = byTrackingId.get(trackingId);
            if (list == null) {
                list = new ArrayList<StudentExtracurricularActivity>();
                byTrackingId.put(trackingId, list);
            }
            list.add(activity);
        }
        return byTrackingId;
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

    private void ensureTeacherCanAccessStudents(User operator, List<Long> studentIds) {
        if (operator.getRole() == UserRole.ADMIN) {
            return;
        }
        if (operator.getRole() != UserRole.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }
        Teacher teacher = teacherRepository.findByUser_Id(operator.getId())
                .orElseThrow(TeacherBindingRequiredException::new);
        for (Long studentId : studentIds) {
            boolean assigned = teacherStudentRepository.existsByTeacher_IdAndStudent_IdAndStatus(
                    teacher.getId(),
                    studentId,
                    TeacherStudentStatus.ACTIVE
            );
            if (!assigned) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Forbidden: student not assigned to current teacher."
                );
            }
        }
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

    private String normalizeActivityType(String value, String fieldName) {
        String normalized = requireNonBlank(value, fieldName, 40).toUpperCase();
        if (!ACTIVITY_TYPES.contains(normalized)) {
            throw badRequest(fieldName + " invalid");
        }
        return normalized;
    }

    private String normalizeActivityLevel(String value, String fieldName) {
        String normalized = normalizeOptionalText(value, fieldName, LEVEL_MAX_LENGTH);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase();
        if (!ACTIVITY_LEVELS.contains(normalized)) {
            throw badRequest(fieldName + " invalid");
        }
        return normalized;
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

    private static class ActivityStats {
        private final int totalActivities;
        private final int competitionCount;
        private final int awardCount;

        private ActivityStats(int totalActivities, int competitionCount, int awardCount) {
            this.totalActivities = totalActivities;
            this.competitionCount = competitionCount;
            this.awardCount = awardCount;
        }
    }

    private static class NormalizedTracking {
        private final String note;
        private final List<NormalizedActivity> activities;

        private NormalizedTracking(String note, List<NormalizedActivity> activities) {
            this.note = note;
            this.activities = activities;
        }
    }

    private static class NormalizedActivity {
        private final String activityType;
        private final String activityName;
        private final String organization;
        private final String role;
        private final String activityLevel;
        private final String awardOrResult;
        private final String competitionCategory;
        private final LocalDate activityDate;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final String description;
        private final String admissionRelevance;
        private final String proofContact;
        private final String proofUrl;

        private NormalizedActivity(String activityType,
                                   String activityName,
                                   String organization,
                                   String role,
                                   String activityLevel,
                                   String awardOrResult,
                                   String competitionCategory,
                                   LocalDate activityDate,
                                   LocalDate startDate,
                                   LocalDate endDate,
                                   String description,
                                   String admissionRelevance,
                                   String proofContact,
                                   String proofUrl) {
            this.activityType = activityType;
            this.activityName = activityName;
            this.organization = organization;
            this.role = role;
            this.activityLevel = activityLevel;
            this.awardOrResult = awardOrResult;
            this.competitionCategory = competitionCategory;
            this.activityDate = activityDate;
            this.startDate = startDate;
            this.endDate = endDate;
            this.description = description;
            this.admissionRelevance = admissionRelevance;
            this.proofContact = proofContact;
            this.proofUrl = proofUrl;
        }
    }
}
