package com.studentmanagement.studentmanagementserver.domain.osslt;

import com.studentmanagement.studentmanagementserver.domain.enums.SchoolType;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.student.StudentSchoolRecord;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentOssltModuleRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentSchoolRecordRepository;
import com.studentmanagement.studentmanagementserver.service.ApiRequestException;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import com.studentmanagement.studentmanagementserver.service.ManagementAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class OssltTrackingService {

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final int MAX_OSSLC_COURSE_LOCATION_LENGTH = 255;

    private final AuthSessionService authSessionService;
    private final ManagementAccessService managementAccessService;
    private final StudentRepository studentRepository;
    private final StudentSchoolRecordRepository studentSchoolRecordRepository;
    private final StudentOssltModuleRepository studentOssltModuleRepository;

    public OssltTrackingService(AuthSessionService authSessionService,
                                ManagementAccessService managementAccessService,
                                StudentRepository studentRepository,
                                StudentSchoolRecordRepository studentSchoolRecordRepository,
                                StudentOssltModuleRepository studentOssltModuleRepository) {
        this.authSessionService = authSessionService;
        this.managementAccessService = managementAccessService;
        this.studentRepository = studentRepository;
        this.studentSchoolRecordRepository = studentSchoolRecordRepository;
        this.studentOssltModuleRepository = studentOssltModuleRepository;
    }

    @Transactional(readOnly = true)
    public TeacherStudentOssltModuleStateDto getCurrentStudentModule(HttpServletRequest request) {
        CurrentStudentContext context = requireCurrentStudentContext(request);
        Student student = context.student;
        StudentOssltModule module = studentOssltModuleRepository.findByStudent_Id(student.getId()).orElse(null);
        Integer graduationYear = resolveGraduationYearForStudent(student.getId());
        return buildModuleState(student.getId(), graduationYear, module);
    }

    @Transactional
    public TeacherStudentOssltModuleStateDto updateCurrentStudentModule(StudentOssltModuleUpdateRequestDto requestBody,
                                                                        HttpServletRequest request) {
        CurrentStudentContext context = requireCurrentStudentContext(request);
        Student student = context.student;
        StudentNormalizedUpdate normalized = normalizeStudentUpdateRequest(requestBody);

        StudentOssltModule module = studentOssltModuleRepository.findByStudent_Id(student.getId())
                .orElseGet(() -> new StudentOssltModule(student));

        if (normalized.overwriteLatestOssltResult) {
            module.updateLatestOssltResult(normalized.latestOssltResult);
        }
        if (normalized.overwriteHasOsslc) {
            module.updateHasOsslc(normalized.hasOsslc);
        }

        OssltTrackingStatus finalStatus = deriveFinalTrackingStatus(
                module.getLatestOssltResult(),
                module.getHasOsslc(),
                module.getOssltTrackingManualStatus()
        );
        module.updateOssltTrackingStatus(finalStatus);
        module.markOssltUpdatedAt(LocalDateTime.now());
        module = studentOssltModuleRepository.save(module);

        Integer graduationYear = resolveGraduationYearForStudent(student.getId());
        return buildModuleState(student.getId(), graduationYear, module);
    }

    @Transactional(readOnly = true)
    public TeacherStudentOssltModuleStateDto getTeacherStudentModule(Long studentId, HttpServletRequest request) {
        TeacherStudentContext context = requireTeacherAccessibleStudentContext(studentId, request);
        Student student = context.student;
        StudentOssltModule module = studentOssltModuleRepository.findByStudent_Id(student.getId()).orElse(null);
        Integer graduationYear = resolveGraduationYearForStudent(student.getId());
        return buildModuleState(student.getId(), graduationYear, module);
    }

    @Transactional
    public TeacherStudentOssltModuleStateDto updateTeacherStudentModule(Long studentId,
                                                                        TeacherOssltModuleUpdateRequestDto requestBody,
                                                                        HttpServletRequest request) {
        TeacherStudentContext context = requireTeacherAccessibleStudentContext(studentId, request);
        Student student = context.student;
        NormalizedUpdate normalized = normalizeUpdateRequest(requestBody);

        StudentOssltModule module = studentOssltModuleRepository.findByStudent_Id(student.getId())
                .orElseGet(() -> new StudentOssltModule(student));

        if (normalized.overwriteLatestOssltResult) {
            module.updateLatestOssltResult(normalized.latestOssltResult);
        }
        if (normalized.overwriteLatestOssltDate) {
            module.updateLatestOssltDate(normalized.latestOssltDate);
        }
        if (normalized.overwriteHasOsslc) {
            module.updateHasOsslc(normalized.hasOsslc);
        }
        if (normalized.overwriteOssltTrackingManualStatus) {
            module.updateOssltTrackingManualStatus(normalized.ossltTrackingManualStatus);
        }
        if (normalized.overwriteOsslcCourseStatus) {
            module.updateOsslcCourseStatus(normalized.osslcCourseStatus);
        }
        if (normalized.overwriteOsslcCourseLocation) {
            module.updateOsslcCourseLocation(normalized.osslcCourseLocation);
        }

        validateTeacherOsslcRules(
                normalized.overwriteOssltTrackingManualStatus
                        ? normalized.ossltTrackingManualStatus
                        : module.getOssltTrackingManualStatus(),
                normalized.overwriteOsslcCourseStatus
                        ? normalized.osslcCourseStatus
                        : module.getOsslcCourseStatus(),
                normalized.overwriteOsslcCourseLocation
                        ? normalized.osslcCourseLocation
                        : module.getOsslcCourseLocation()
        );

        OssltTrackingStatus finalStatus = deriveFinalTrackingStatus(
                module.getLatestOssltResult(),
                module.getHasOsslc(),
                module.getOssltTrackingManualStatus()
        );
        module.updateOssltTrackingStatus(finalStatus);
        module.markOssltUpdatedAt(LocalDateTime.now());
        module = studentOssltModuleRepository.save(module);

        Integer graduationYear = resolveGraduationYearForStudent(student.getId());
        return buildModuleState(student.getId(), graduationYear, module);
    }

    @Transactional(readOnly = true)
    public List<TeacherStudentOssltSummaryDto> getTeacherStudentsSummary(String studentIdsQuery,
                                                                         HttpServletRequest request) {
        List<Long> studentIds = parseStudentIds(studentIdsQuery);
        if (studentIds.isEmpty()) {
            return Collections.emptyList();
        }

        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        ensureOperatorCanAccessStudents(operator, studentIds);

        Map<Long, Integer> graduationYearByStudentId = buildGraduationYearMap(studentIds);
        List<StudentOssltModule> modules = studentOssltModuleRepository.findByStudent_IdIn(studentIds);
        Map<Long, StudentOssltModule> moduleByStudentId = new HashMap<Long, StudentOssltModule>(modules.size());
        for (StudentOssltModule module : modules) {
            if (module == null || module.getStudent() == null || module.getStudent().getId() == null) {
                continue;
            }
            moduleByStudentId.put(module.getStudent().getId(), module);
        }

        List<TeacherStudentOssltSummaryDto> result = new ArrayList<TeacherStudentOssltSummaryDto>(studentIds.size());
        for (Long studentId : studentIds) {
            StudentOssltModule module = moduleByStudentId.get(studentId);
            OssltLatestResult latestResult = resolveLatestResult(module == null ? null : module.getLatestOssltResult());
            Boolean hasOsslc = module == null ? null : module.getHasOsslc();
            OssltTrackingManualStatus manualStatus = module == null ? null : module.getOssltTrackingManualStatus();
            OssltTrackingStatus trackingStatus = deriveFinalTrackingStatus(latestResult, hasOsslc, manualStatus);
            result.add(new TeacherStudentOssltSummaryDto(
                    studentId,
                    graduationYearByStudentId.get(studentId),
                    latestResult.name(),
                    module == null || module.getLatestOssltDate() == null ? null : module.getLatestOssltDate().toString(),
                    hasOsslc,
                    manualStatus == null ? null : manualStatus.name(),
                    trackingStatus.name(),
                    resolveUpdatedAt(module)
            ));
        }
        return result;
    }

    private TeacherStudentOssltModuleStateDto buildModuleState(Long studentId,
                                                               Integer graduationYear,
                                                               StudentOssltModule module) {
        OssltLatestResult latestResult = resolveLatestResult(module == null ? null : module.getLatestOssltResult());
        Boolean hasOsslc = module == null ? null : module.getHasOsslc();
        OsslcCourseStatus osslcCourseStatus = module == null ? null : module.getOsslcCourseStatus();
        String osslcCourseLocation = module == null ? null : module.getOsslcCourseLocation();
        OssltTrackingManualStatus manualStatus = module == null ? null : module.getOssltTrackingManualStatus();
        OssltTrackingStatus finalTrackingStatus = deriveFinalTrackingStatus(latestResult, hasOsslc, manualStatus);
        return new TeacherStudentOssltModuleStateDto(
                studentId,
                graduationYear,
                latestResult.name(),
                module == null || module.getLatestOssltDate() == null ? null : module.getLatestOssltDate().toString(),
                hasOsslc,
                osslcCourseStatus == null ? null : osslcCourseStatus.name(),
                osslcCourseLocation,
                manualStatus == null ? null : manualStatus.name(),
                finalTrackingStatus.name(),
                resolveUpdatedAt(module)
        );
    }

    private String resolveUpdatedAt(StudentOssltModule module) {
        if (module == null || module.getOssltUpdatedAt() == null) {
            return null;
        }
        return module.getOssltUpdatedAt().atOffset(ZoneOffset.UTC).toString();
    }

    private NormalizedUpdate normalizeUpdateRequest(TeacherOssltModuleUpdateRequestDto requestBody) {
        if (requestBody == null) {
            throw validationFailed(Collections.singletonList("request body is required"));
        }

        List<String> details = new ArrayList<String>();

        OssltLatestResult latestOssltResult = null;
        if (requestBody.isLatestOssltResultPresent()) {
            latestOssltResult = parseLatestOssltResult(requestBody.getLatestOssltResult(), "latestOssltResult", details);
        }

        LocalDate latestOssltDate = null;
        if (requestBody.isLatestOssltDatePresent()) {
            latestOssltDate = parseOptionalDate(requestBody.getLatestOssltDate(), "latestOssltDate", details);
        }

        Boolean hasOsslc = null;
        if (requestBody.isHasOsslcPresent()) {
            hasOsslc = requestBody.getHasOsslc();
        }

        OssltTrackingManualStatus ossltTrackingManualStatus = null;
        if (requestBody.isOssltTrackingManualStatusPresent()) {
            ossltTrackingManualStatus = parseManualStatus(
                    requestBody.getOssltTrackingManualStatus(),
                    "ossltTrackingManualStatus",
                    details
            );
        }

        OsslcCourseStatus osslcCourseStatus = null;
        if (requestBody.isOsslcCourseStatusPresent()) {
            osslcCourseStatus = parseOsslcCourseStatus(
                    requestBody.getOsslcCourseStatus(),
                    "osslcCourseStatus",
                    details
            );
        }

        String osslcCourseLocation = null;
        if (requestBody.isOsslcCourseLocationPresent()) {
            osslcCourseLocation = parseOsslcCourseLocation(
                    requestBody.getOsslcCourseLocation(),
                    "osslcCourseLocation",
                    details
            );
        }

        if (!details.isEmpty()) {
            throw validationFailed(details);
        }

        return new NormalizedUpdate(
                requestBody.isLatestOssltResultPresent(),
                latestOssltResult,
                requestBody.isLatestOssltDatePresent(),
                latestOssltDate,
                requestBody.isHasOsslcPresent(),
                hasOsslc,
                requestBody.isOssltTrackingManualStatusPresent(),
                ossltTrackingManualStatus,
                requestBody.isOsslcCourseStatusPresent(),
                osslcCourseStatus,
                requestBody.isOsslcCourseLocationPresent(),
                osslcCourseLocation
        );
    }

    private StudentNormalizedUpdate normalizeStudentUpdateRequest(StudentOssltModuleUpdateRequestDto requestBody) {
        if (requestBody == null) {
            throw validationFailed(Collections.singletonList("request body is required"));
        }

        List<String> details = new ArrayList<String>();
        if (requestBody.isOssltTrackingManualStatusPresent()) {
            details.add("ossltTrackingManualStatus is not allowed for student APIs");
        }
        if (requestBody.isOsslcCourseStatusPresent()) {
            details.add("osslcCourseStatus is not allowed for student APIs");
        }
        if (requestBody.isOsslcCourseLocationPresent()) {
            details.add("osslcCourseLocation is not allowed for student APIs");
        }

        boolean overwriteLatestOssltResult = requestBody.isLatestOssltResultPresent();
        OssltLatestResult latestOssltResult = null;
        if (overwriteLatestOssltResult) {
            latestOssltResult = parseStudentLatestOssltResult(
                    requestBody.getLatestOssltResult(),
                    "latestOssltResult",
                    details
            );
        }

        boolean overwriteHasOsslc = requestBody.isHasOsslcPresent();
        Boolean hasOsslc = null;
        if (overwriteHasOsslc) {
            hasOsslc = requestBody.getHasOsslc();
            if (hasOsslc == null) {
                details.add("hasOsslc must be true or false");
            }
        }

        if (!overwriteLatestOssltResult && !overwriteHasOsslc) {
            details.add("at least one of latestOssltResult or hasOsslc is required");
        }

        if (!details.isEmpty()) {
            throw validationFailed(details);
        }
        return new StudentNormalizedUpdate(overwriteLatestOssltResult, latestOssltResult, overwriteHasOsslc, hasOsslc);
    }

    private OssltLatestResult parseLatestOssltResult(String rawValue, String fieldPath, List<String> details) {
        String normalized = trimToNull(rawValue);
        if (normalized == null) {
            return OssltLatestResult.UNKNOWN;
        }
        try {
            return OssltLatestResult.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            details.add(fieldPath + " invalid");
            return null;
        }
    }

    private OssltLatestResult parseStudentLatestOssltResult(String rawValue, String fieldPath, List<String> details) {
        String normalized = trimToNull(rawValue);
        if (normalized == null) {
            details.add(fieldPath + " must be PASS or FAIL");
            return null;
        }
        if ("PASS".equalsIgnoreCase(normalized)) {
            return OssltLatestResult.PASS;
        }
        if ("FAIL".equalsIgnoreCase(normalized)) {
            return OssltLatestResult.FAIL;
        }
        details.add(fieldPath + " must be PASS or FAIL");
        return null;
    }

    private OssltTrackingManualStatus parseManualStatus(String rawValue, String fieldPath, List<String> details) {
        String normalized = trimToNull(rawValue);
        if (normalized == null) {
            return null;
        }
        try {
            return OssltTrackingManualStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            details.add(fieldPath + " invalid");
            return null;
        }
    }

    private OsslcCourseStatus parseOsslcCourseStatus(String rawValue, String fieldPath, List<String> details) {
        String normalized = trimToNull(rawValue);
        if (normalized == null) {
            return null;
        }
        try {
            return OsslcCourseStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            details.add(fieldPath + " invalid");
            return null;
        }
    }

    private String parseOsslcCourseLocation(String rawValue, String fieldPath, List<String> details) {
        String normalized = trimToNull(rawValue);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() > MAX_OSSLC_COURSE_LOCATION_LENGTH) {
            details.add(fieldPath + " too long");
            return null;
        }
        return normalized;
    }

    private void validateTeacherOsslcRules(OssltTrackingManualStatus manualStatus,
                                           OsslcCourseStatus osslcCourseStatus,
                                           String osslcCourseLocation) {
        List<String> details = new ArrayList<String>();
        if (manualStatus == OssltTrackingManualStatus.NEEDS_TRACKING && osslcCourseStatus == null) {
            details.add("osslcCourseStatus is required when ossltTrackingManualStatus is NEEDS_TRACKING");
        }

        String normalizedLocation = trimToNull(osslcCourseLocation);
        if (osslcCourseStatus == OsslcCourseStatus.IN_PROGRESS && normalizedLocation == null) {
            details.add("osslcCourseLocation is required when osslcCourseStatus is IN_PROGRESS");
        }
        if (normalizedLocation != null && normalizedLocation.length() > MAX_OSSLC_COURSE_LOCATION_LENGTH) {
            details.add("osslcCourseLocation too long");
        }

        if (!details.isEmpty()) {
            throw validationFailed(details);
        }
    }

    private LocalDate parseOptionalDate(String rawDate, String fieldPath, List<String> details) {
        String normalized = trimToNull(rawDate);
        if (normalized == null) {
            return null;
        }
        if (!DATE_PATTERN.matcher(normalized).matches()) {
            details.add(fieldPath + " must be yyyy-mm-dd");
            return null;
        }
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ex) {
            details.add(fieldPath + " must be yyyy-mm-dd");
            return null;
        }
    }

    private OssltLatestResult resolveLatestResult(OssltLatestResult latestResult) {
        return latestResult == null ? OssltLatestResult.UNKNOWN : latestResult;
    }

    private OssltTrackingStatus deriveAutoTrackingStatus(OssltLatestResult latestResult, Boolean hasOsslc) {
        if (hasOsslc != null) {
            return hasOsslc.booleanValue()
                    ? OssltTrackingStatus.PASSED
                    : OssltTrackingStatus.NEEDS_TRACKING;
        }
        OssltLatestResult resolvedResult = resolveLatestResult(latestResult);
        if (resolvedResult == OssltLatestResult.PASS) {
            return OssltTrackingStatus.PASSED;
        }
        if (resolvedResult == OssltLatestResult.FAIL) {
            return OssltTrackingStatus.NEEDS_TRACKING;
        }
        return OssltTrackingStatus.WAITING_UPDATE;
    }

    private OssltTrackingStatus deriveFinalTrackingStatus(OssltLatestResult latestResult,
                                                          Boolean hasOsslc,
                                                          OssltTrackingManualStatus manualStatus) {
        if (manualStatus != null) {
            return OssltTrackingStatus.valueOf(manualStatus.name());
        }
        return deriveAutoTrackingStatus(latestResult, hasOsslc);
    }

    private Integer resolveGraduationYearForStudent(Long studentId) {
        List<StudentSchoolRecord> schoolRecords = studentSchoolRecordRepository.findByStudent_IdOrderByIdAsc(studentId);
        return resolveGraduationYear(schoolRecords);
    }

    private Integer resolveGraduationYear(List<StudentSchoolRecord> schoolRecords) {
        StudentSchoolRecord primarySchool = null;
        if (schoolRecords != null) {
            for (StudentSchoolRecord schoolRecord : schoolRecords) {
                if (schoolRecord == null || schoolRecord.getSchoolType() != SchoolType.MAIN) {
                    continue;
                }
                primarySchool = schoolRecord;
                break;
            }
        }
        if (primarySchool == null || primarySchool.getEndTime() == null) {
            return null;
        }
        return Integer.valueOf(primarySchool.getEndTime().getYear());
    }

    private Map<Long, Integer> buildGraduationYearMap(List<Long> studentIds) {
        Map<Long, List<StudentSchoolRecord>> schoolRecordsByStudentId = new HashMap<Long, List<StudentSchoolRecord>>();
        List<StudentSchoolRecord> schoolRecords = studentSchoolRecordRepository
                .findByStudent_IdInOrderByStudent_IdAscIdAsc(studentIds);
        for (StudentSchoolRecord schoolRecord : schoolRecords) {
            if (schoolRecord == null || schoolRecord.getStudent() == null || schoolRecord.getStudent().getId() == null) {
                continue;
            }
            Long studentId = schoolRecord.getStudent().getId();
            List<StudentSchoolRecord> records = schoolRecordsByStudentId.get(studentId);
            if (records == null) {
                records = new ArrayList<StudentSchoolRecord>();
                schoolRecordsByStudentId.put(studentId, records);
            }
            records.add(schoolRecord);
        }

        Map<Long, Integer> graduationYearByStudentId = new HashMap<Long, Integer>();
        for (Long studentId : studentIds) {
            graduationYearByStudentId.put(studentId, resolveGraduationYear(schoolRecordsByStudentId.get(studentId)));
        }
        return graduationYearByStudentId;
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
        return new TeacherStudentContext(student);
    }

    private void ensureOperatorCanAccessStudents(User operator, List<Long> studentIds) {
        Map<Long, Student> studentsById = loadStudentsByIds(studentIds);
        if (operator.getRole() == UserRole.ADMIN || operator.getRole() == UserRole.TEACHER) {
            return;
        }
        for (Long studentId : studentIds) {
            if (!studentsById.containsKey(studentId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentId);
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
    }

    private Map<Long, Student> loadStudentsByIds(List<Long> studentIds) {
        List<Student> students = studentRepository.findByIdInWithUser(studentIds);
        Map<Long, Student> studentsById = new HashMap<Long, Student>(students.size());
        for (Student student : students) {
            if (student == null || student.getId() == null) {
                continue;
            }
            studentsById.put(student.getId(), student);
        }
        for (Long studentId : studentIds) {
            if (!studentsById.containsKey(studentId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentId);
            }
        }
        return studentsById;
    }

    private void ensureTeacherCanAccessStudent(User operator, Long studentId) {
        if (operator.getRole() == UserRole.ADMIN || operator.getRole() == UserRole.TEACHER) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
    }

    private List<Long> parseStudentIds(String studentIdsQuery) {
        String normalized = trimToNull(studentIdsQuery);
        if (normalized == null) {
            return Collections.emptyList();
        }

        String[] rawParts = normalized.split(",");
        LinkedHashSet<Long> deduplicated = new LinkedHashSet<Long>();
        List<String> details = new ArrayList<String>();

        for (int i = 0; i < rawParts.length; i++) {
            String rawPart = trimToNull(rawParts[i]);
            if (rawPart == null) {
                details.add("studentIds[" + i + "] is required");
                continue;
            }
            try {
                long value = Long.parseLong(rawPart);
                if (value <= 0L) {
                    details.add("studentIds[" + i + "] must be positive");
                    continue;
                }
                deduplicated.add(Long.valueOf(value));
            } catch (NumberFormatException ex) {
                details.add("studentIds[" + i + "] invalid");
            }
        }

        if (!details.isEmpty()) {
            throw validationFailed(details);
        }
        return new ArrayList<Long>(deduplicated);
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

        private CurrentStudentContext(Student student) {
            this.student = student;
        }
    }

    private static class TeacherStudentContext {
        private final Student student;

        private TeacherStudentContext(Student student) {
            this.student = student;
        }
    }

    private static class NormalizedUpdate {
        private final boolean overwriteLatestOssltResult;
        private final OssltLatestResult latestOssltResult;
        private final boolean overwriteLatestOssltDate;
        private final LocalDate latestOssltDate;
        private final boolean overwriteHasOsslc;
        private final Boolean hasOsslc;
        private final boolean overwriteOssltTrackingManualStatus;
        private final OssltTrackingManualStatus ossltTrackingManualStatus;
        private final boolean overwriteOsslcCourseStatus;
        private final OsslcCourseStatus osslcCourseStatus;
        private final boolean overwriteOsslcCourseLocation;
        private final String osslcCourseLocation;

        private NormalizedUpdate(boolean overwriteLatestOssltResult,
                                 OssltLatestResult latestOssltResult,
                                 boolean overwriteLatestOssltDate,
                                 LocalDate latestOssltDate,
                                 boolean overwriteHasOsslc,
                                 Boolean hasOsslc,
                                 boolean overwriteOssltTrackingManualStatus,
                                 OssltTrackingManualStatus ossltTrackingManualStatus,
                                 boolean overwriteOsslcCourseStatus,
                                 OsslcCourseStatus osslcCourseStatus,
                                 boolean overwriteOsslcCourseLocation,
                                 String osslcCourseLocation) {
            this.overwriteLatestOssltResult = overwriteLatestOssltResult;
            this.latestOssltResult = latestOssltResult;
            this.overwriteLatestOssltDate = overwriteLatestOssltDate;
            this.latestOssltDate = latestOssltDate;
            this.overwriteHasOsslc = overwriteHasOsslc;
            this.hasOsslc = hasOsslc;
            this.overwriteOssltTrackingManualStatus = overwriteOssltTrackingManualStatus;
            this.ossltTrackingManualStatus = ossltTrackingManualStatus;
            this.overwriteOsslcCourseStatus = overwriteOsslcCourseStatus;
            this.osslcCourseStatus = osslcCourseStatus;
            this.overwriteOsslcCourseLocation = overwriteOsslcCourseLocation;
            this.osslcCourseLocation = osslcCourseLocation;
        }
    }

    private static class StudentNormalizedUpdate {
        private final boolean overwriteLatestOssltResult;
        private final OssltLatestResult latestOssltResult;
        private final boolean overwriteHasOsslc;
        private final Boolean hasOsslc;

        private StudentNormalizedUpdate(boolean overwriteLatestOssltResult,
                                        OssltLatestResult latestOssltResult,
                                        boolean overwriteHasOsslc,
                                        Boolean hasOsslc) {
            this.overwriteLatestOssltResult = overwriteLatestOssltResult;
            this.latestOssltResult = latestOssltResult;
            this.overwriteHasOsslc = overwriteHasOsslc;
            this.hasOsslc = hasOsslc;
        }
    }
}
