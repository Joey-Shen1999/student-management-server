package com.studentmanagement.studentmanagementserver.domain.ielts;

import com.studentmanagement.studentmanagementserver.domain.enums.SchoolType;
import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.student.StudentProfile;
import com.studentmanagement.studentmanagementserver.domain.student.StudentSchoolRecord;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentIeltsModuleRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentIeltsRecordRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentProfileRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentSchoolRecordRepository;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class IeltsTrackingService {

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final int MAX_RECORD_ID_LENGTH = 64;
    private static final int CANADA_STUDY_YEARS_THRESHOLD = 4;
    private static final String LANGUAGE_RISK_FLAG_RISK = "RISK";
    private static final String LANGUAGE_RISK_FLAG_LOW_RISK = "LOW_RISK";
    private static final String PROFILE_COMPLETENESS_COMPLETE = "COMPLETE";
    private static final String PROFILE_COMPLETENESS_INCOMPLETE = "INCOMPLETE";

    private final AuthSessionService authSessionService;
    private final ManagementAccessService managementAccessService;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherStudentRepository teacherStudentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentSchoolRecordRepository studentSchoolRecordRepository;
    private final StudentIeltsModuleRepository studentIeltsModuleRepository;
    private final StudentIeltsRecordRepository studentIeltsRecordRepository;

    public IeltsTrackingService(AuthSessionService authSessionService,
                                ManagementAccessService managementAccessService,
                                StudentRepository studentRepository,
                                TeacherRepository teacherRepository,
                                TeacherStudentRepository teacherStudentRepository,
                                StudentProfileRepository studentProfileRepository,
                                StudentSchoolRecordRepository studentSchoolRecordRepository,
                                StudentIeltsModuleRepository studentIeltsModuleRepository,
                                StudentIeltsRecordRepository studentIeltsRecordRepository) {
        this.authSessionService = authSessionService;
        this.managementAccessService = managementAccessService;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.teacherStudentRepository = teacherStudentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentSchoolRecordRepository = studentSchoolRecordRepository;
        this.studentIeltsModuleRepository = studentIeltsModuleRepository;
        this.studentIeltsRecordRepository = studentIeltsRecordRepository;
    }

    @Transactional(readOnly = true)
    public StudentIeltsModuleStateDto getCurrentStudentModule(HttpServletRequest request) {
        Student student = requireCurrentStudent(request);
        StudentIeltsModule module = studentIeltsModuleRepository.findByStudent_Id(student.getId()).orElse(null);
        return buildModuleState(student, module);
    }

    @Transactional
    public StudentIeltsModuleStateDto updateCurrentStudentRecords(StudentIeltsRecordsUpdateRequestDto requestBody,
                                                                  HttpServletRequest request) {
        Student student = requireCurrentStudent(request);
        RecordsUpdate normalized = normalizeRecordsUpdateRequest(requestBody);
        return saveModuleState(student, normalized.hasTakenIeltsAcademic, normalized.preparationIntent, normalized.records, true);
    }

    @Transactional
    public StudentIeltsModuleStateDto updateCurrentStudentPreparationIntent(
            StudentIeltsPreparationIntentUpdateRequestDto requestBody,
            HttpServletRequest request) {
        Student student = requireCurrentStudent(request);
        PreparationIntentUpdate normalized = normalizePreparationIntentUpdateRequest(requestBody);
        return saveModuleState(
                student,
                normalized.hasTakenIeltsAcademic,
                normalized.preparationIntent,
                Collections.<NormalizedRecord>emptyList(),
                !normalized.hasTakenIeltsAcademic
        );
    }

    @Transactional(readOnly = true)
    public StudentIeltsModuleStateDto getTeacherStudentModule(Long studentId, HttpServletRequest request) {
        Student student = requireTeacherAccessibleStudent(studentId, request);
        StudentIeltsModule module = studentIeltsModuleRepository.findByStudent_Id(student.getId()).orElse(null);
        return buildModuleState(student, module);
    }

    @Transactional
    public StudentIeltsModuleStateDto updateTeacherStudentModule(Long studentId,
                                                                 TeacherIeltsModuleUpdateRequestDto requestBody,
                                                                 HttpServletRequest request) {
        Student student = requireTeacherAccessibleStudent(studentId, request);
        TeacherUpdate normalized = normalizeTeacherUpdateRequest(requestBody);
        return saveModuleState(
                student,
                normalized.hasTakenIeltsAcademic,
                normalized.preparationIntent,
                normalized.records,
                normalized.overwriteRecords
        );
    }

    @Transactional(readOnly = true)
    public StudentIeltsSummaryDto getTeacherStudentSummary(Long studentId, HttpServletRequest request) {
        Student student = requireTeacherAccessibleStudent(studentId, request);
        StudentIeltsModule module = studentIeltsModuleRepository.findByStudent_Id(student.getId()).orElse(null);
        StudentIeltsModuleStateDto moduleState = buildModuleState(student, module);

        List<StudentIeltsRecordDto> records = moduleState.getRecords() == null
                ? Collections.<StudentIeltsRecordDto>emptyList()
                : moduleState.getRecords();

        String latestTestDate = null;
        Double bestOverallBand = null;
        for (StudentIeltsRecordDto record : records) {
            if (record == null) {
                continue;
            }
            if (latestTestDate == null && record.getTestDate() != null) {
                latestTestDate = record.getTestDate();
            }
            Double overall = calculateOverallBand(record);
            if (overall == null) {
                continue;
            }
            if (bestOverallBand == null || overall.doubleValue() > bestOverallBand.doubleValue()) {
                bestOverallBand = overall;
            }
        }

        StudentIeltsLanguageRiskDto languageRisk = moduleState.getLanguageRisk();
        return new StudentIeltsSummaryDto(
                moduleState.getStudentId(),
                languageRisk != null && languageRisk.isShouldShowIeltsModule(),
                moduleState.isHasTakenIeltsAcademic(),
                moduleState.getPreparationIntent(),
                records.size(),
                latestTestDate,
                bestOverallBand,
                moduleState.getUpdatedAt()
        );
    }

    private StudentIeltsModuleStateDto saveModuleState(Student student,
                                                       boolean hasTakenIeltsAcademic,
                                                       IeltsPreparationIntent preparationIntent,
                                                       List<NormalizedRecord> records,
                                                       boolean overwriteRecords) {
        StudentIeltsModule module = studentIeltsModuleRepository.findByStudent_Id(student.getId())
                .orElseGet(() -> new StudentIeltsModule(student));

        module.updateState(hasTakenIeltsAcademic, preparationIntent);
        module = studentIeltsModuleRepository.save(module);

        if (overwriteRecords) {
            if (!hasTakenIeltsAcademic || records == null || records.isEmpty()) {
                studentIeltsRecordRepository.deleteByIeltsModule_Id(module.getId());
            } else {
                overwriteIeltsRecords(module, records);
            }
        }

        return buildModuleState(student, module);
    }

    private void overwriteIeltsRecords(StudentIeltsModule module, List<NormalizedRecord> normalizedRecords) {
        List<StudentIeltsRecord> existingRecords = studentIeltsRecordRepository.findByIeltsModule_Id(module.getId());
        Map<String, StudentIeltsRecord> existingByRecordId = new HashMap<String, StudentIeltsRecord>(existingRecords.size());
        for (StudentIeltsRecord existing : existingRecords) {
            if (existing == null || existing.getRecordId() == null) {
                continue;
            }
            existingByRecordId.put(existing.getRecordId(), existing);
        }

        List<StudentIeltsRecord> recordsToSave = new ArrayList<StudentIeltsRecord>(normalizedRecords.size());
        for (NormalizedRecord normalizedRecord : normalizedRecords) {
            StudentIeltsRecord existing = existingByRecordId.remove(normalizedRecord.recordId);
            if (existing == null) {
                existing = new StudentIeltsRecord(
                        module,
                        normalizedRecord.recordId,
                        normalizedRecord.testDate,
                        normalizedRecord.listening,
                        normalizedRecord.reading,
                        normalizedRecord.writing,
                        normalizedRecord.speaking
                );
            } else {
                existing.overwrite(
                        normalizedRecord.testDate,
                        normalizedRecord.listening,
                        normalizedRecord.reading,
                        normalizedRecord.writing,
                        normalizedRecord.speaking
                );
            }
            recordsToSave.add(existing);
        }

        if (!recordsToSave.isEmpty()) {
            studentIeltsRecordRepository.saveAll(recordsToSave);
        }
        if (!existingByRecordId.isEmpty()) {
            studentIeltsRecordRepository.deleteAll(existingByRecordId.values());
        }
    }

    private StudentIeltsModuleStateDto buildModuleState(Student student, StudentIeltsModule module) {
        List<StudentIeltsRecord> records = module == null
                ? Collections.<StudentIeltsRecord>emptyList()
                : studentIeltsRecordRepository.findByIeltsModule_IdOrderByTestDateDescIdDesc(module.getId());

        List<StudentIeltsRecordDto> recordDtos = new ArrayList<StudentIeltsRecordDto>(records.size());
        for (StudentIeltsRecord record : records) {
            recordDtos.add(new StudentIeltsRecordDto(
                    record.getRecordId(),
                    record.getTestDate() == null ? null : record.getTestDate().toString(),
                    record.getListening(),
                    record.getReading(),
                    record.getWriting(),
                    record.getSpeaking()
            ));
        }

        StudentProfile profile = studentProfileRepository.findByStudent_Id(student.getId()).orElse(null);
        List<StudentSchoolRecord> schoolRecords = studentSchoolRecordRepository.findByStudent_IdOrderByIdAsc(student.getId());

        StudentSchoolRecord primarySchool = findPrimarySchool(schoolRecords);
        Integer graduationYear = primarySchool == null || primarySchool.getEndTime() == null
                ? null
                : primarySchool.getEndTime().getYear();
        StudentIeltsLanguageRiskDto languageRisk = buildLanguageRisk(profile, schoolRecords, graduationYear);

        String updatedAt = resolveUpdatedAt(module, records);
        return new StudentIeltsModuleStateDto(
                student.getId(),
                graduationYear,
                module != null && module.isHasTakenIeltsAcademic(),
                module == null || module.getPreparationIntent() == null
                        ? IeltsPreparationIntent.UNSET.name()
                        : module.getPreparationIntent().name(),
                recordDtos,
                languageRisk,
                updatedAt
        );
    }

    private StudentIeltsLanguageRiskDto buildLanguageRisk(StudentProfile profile,
                                                          List<StudentSchoolRecord> schoolRecords,
                                                          Integer graduationYear) {
        String firstLanguage = trimToNull(profile == null ? null : profile.getFirstLanguage());
        String citizenship = trimToNull(profile == null ? null : profile.getCitizenship());

        int canadaStudyYears = estimateCanadaStudyYears(schoolRecords);
        boolean hasCanadianHighSchoolExperience = hasCanadianHighSchoolExperience(schoolRecords);
        boolean profileComplete = firstLanguage != null && citizenship != null && graduationYear != null;

        List<String> riskReasonCodes = new ArrayList<String>();
        if (firstLanguage == null) {
            riskReasonCodes.add("FIRST_LANGUAGE_MISSING");
        } else if (!isEnglishLanguage(firstLanguage)) {
            riskReasonCodes.add("NON_ENGLISH_PRIMARY_LANGUAGE");
        }
        if (citizenship == null) {
            riskReasonCodes.add("CITIZENSHIP_MISSING");
        } else if (!containsCanada(citizenship)) {
            riskReasonCodes.add("NON_CANADIAN_CITIZENSHIP");
        }
        if (canadaStudyYears < CANADA_STUDY_YEARS_THRESHOLD) {
            riskReasonCodes.add("LOW_CANADA_STUDY_YEARS");
        }
        if (!hasCanadianHighSchoolExperience) {
            riskReasonCodes.add("NO_CANADIAN_HIGH_SCHOOL_EXPERIENCE");
        }
        if (!profileComplete) {
            riskReasonCodes.add("PROFILE_INCOMPLETE");
        }

        boolean shouldShowIeltsModule = !riskReasonCodes.isEmpty();
        String languageRiskFlag = shouldShowIeltsModule ? LANGUAGE_RISK_FLAG_RISK : LANGUAGE_RISK_FLAG_LOW_RISK;
        String profileCompleteness = profileComplete ? PROFILE_COMPLETENESS_COMPLETE : PROFILE_COMPLETENESS_INCOMPLETE;

        return new StudentIeltsLanguageRiskDto(
                shouldShowIeltsModule,
                languageRiskFlag,
                firstLanguage,
                citizenship,
                canadaStudyYears,
                hasCanadianHighSchoolExperience,
                profileCompleteness,
                riskReasonCodes
        );
    }

    private int estimateCanadaStudyYears(List<StudentSchoolRecord> schoolRecords) {
        if (schoolRecords == null || schoolRecords.isEmpty()) {
            return 0;
        }

        LocalDate earliestStart = null;
        LocalDate latestEnd = null;
        for (StudentSchoolRecord schoolRecord : schoolRecords) {
            if (!isCanadianSchool(schoolRecord)) {
                continue;
            }

            LocalDate start = schoolRecord.getStartTime() == null ? schoolRecord.getEndTime() : schoolRecord.getStartTime();
            LocalDate end = schoolRecord.getEndTime() == null ? schoolRecord.getStartTime() : schoolRecord.getEndTime();
            if (start == null || end == null) {
                continue;
            }

            if (earliestStart == null || start.isBefore(earliestStart)) {
                earliestStart = start;
            }
            if (latestEnd == null || end.isAfter(latestEnd)) {
                latestEnd = end;
            }
        }

        if (earliestStart == null || latestEnd == null || latestEnd.isBefore(earliestStart)) {
            return 0;
        }

        long months = ChronoUnit.MONTHS.between(
                earliestStart.withDayOfMonth(1),
                latestEnd.withDayOfMonth(1)
        ) + 1L;
        if (months <= 0L) {
            return 0;
        }
        return (int) ((months + 11L) / 12L);
    }

    private boolean hasCanadianHighSchoolExperience(List<StudentSchoolRecord> schoolRecords) {
        if (schoolRecords == null || schoolRecords.isEmpty()) {
            return false;
        }
        for (StudentSchoolRecord schoolRecord : schoolRecords) {
            if (schoolRecord == null) {
                continue;
            }
            if (schoolRecord.getSchoolType() != SchoolType.MAIN) {
                continue;
            }
            if (isCanadianSchool(schoolRecord)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCanadianSchool(StudentSchoolRecord schoolRecord) {
        if (schoolRecord == null) {
            return false;
        }
        String country = trimToNull(schoolRecord.getCountry());
        return containsCanada(country);
    }

    private boolean containsCanada(String value) {
        String normalized = trimToNull(value);
        return normalized != null && normalized.toLowerCase(Locale.ROOT).contains("canada");
    }

    private boolean isEnglishLanguage(String firstLanguage) {
        String normalized = trimToNull(firstLanguage);
        return normalized != null && normalized.toLowerCase(Locale.ROOT).contains("english");
    }

    private String resolveUpdatedAt(StudentIeltsModule module, List<StudentIeltsRecord> records) {
        LocalDateTime latest = module == null ? null : module.getUpdatedAt();
        if (records != null) {
            for (StudentIeltsRecord record : records) {
                LocalDateTime recordUpdatedAt = record == null ? null : record.getUpdatedAt();
                if (recordUpdatedAt == null) {
                    continue;
                }
                if (latest == null || recordUpdatedAt.isAfter(latest)) {
                    latest = recordUpdatedAt;
                }
            }
        }
        if (latest == null) {
            return null;
        }
        return latest.atOffset(ZoneOffset.UTC).toString();
    }

    private StudentSchoolRecord findPrimarySchool(List<StudentSchoolRecord> schoolRecords) {
        if (schoolRecords == null || schoolRecords.isEmpty()) {
            return null;
        }

        StudentSchoolRecord primary = null;
        for (StudentSchoolRecord schoolRecord : schoolRecords) {
            if (shouldReplacePrimarySchool(primary, schoolRecord)) {
                primary = schoolRecord;
            }
        }
        return primary;
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

    private Double calculateOverallBand(StudentIeltsRecordDto record) {
        if (record == null
                || record.getListening() == null
                || record.getReading() == null
                || record.getWriting() == null
                || record.getSpeaking() == null) {
            return null;
        }
        double average = (record.getListening() + record.getReading() + record.getWriting() + record.getSpeaking()) / 4.0d;
        return Math.round(average * 10.0d) / 10.0d;
    }

    private RecordsUpdate normalizeRecordsUpdateRequest(StudentIeltsRecordsUpdateRequestDto requestBody) {
        if (requestBody == null) {
            throw validationFailed(Collections.singletonList("request body is required"));
        }

        List<String> details = new ArrayList<String>();
        Boolean hasTaken = requestBody.getHasTakenIeltsAcademic();
        if (hasTaken == null) {
            details.add("hasTakenIeltsAcademic is required");
        }

        List<NormalizedRecord> records = normalizeRecords(requestBody.getRecords(), "records", details);
        if (Boolean.FALSE.equals(hasTaken) && !records.isEmpty()) {
            details.add("records must be empty when hasTakenIeltsAcademic is false");
        }
        if (!details.isEmpty()) {
            throw validationFailed(details);
        }

        return new RecordsUpdate(hasTaken.booleanValue(), IeltsPreparationIntent.UNSET, records);
    }

    private PreparationIntentUpdate normalizePreparationIntentUpdateRequest(
            StudentIeltsPreparationIntentUpdateRequestDto requestBody) {
        if (requestBody == null) {
            throw validationFailed(Collections.singletonList("request body is required"));
        }

        List<String> details = new ArrayList<String>();
        Boolean hasTaken = requestBody.getHasTakenIeltsAcademic();
        if (hasTaken == null) {
            details.add("hasTakenIeltsAcademic is required");
        }
        IeltsPreparationIntent preparationIntent =
                parsePreparationIntent(requestBody.getPreparationIntent(), "preparationIntent", true, details);

        if (Boolean.TRUE.equals(hasTaken) && preparationIntent != IeltsPreparationIntent.UNSET) {
            details.add("preparationIntent must be UNSET when hasTakenIeltsAcademic is true");
        }
        if (!details.isEmpty()) {
            throw validationFailed(details);
        }

        IeltsPreparationIntent normalizedIntent = hasTaken.booleanValue()
                ? IeltsPreparationIntent.UNSET
                : preparationIntent;
        return new PreparationIntentUpdate(hasTaken.booleanValue(), normalizedIntent);
    }

    private TeacherUpdate normalizeTeacherUpdateRequest(TeacherIeltsModuleUpdateRequestDto requestBody) {
        if (requestBody == null) {
            throw validationFailed(Collections.singletonList("request body is required"));
        }

        List<String> details = new ArrayList<String>();
        Boolean hasTaken = requestBody.getHasTakenIeltsAcademic();
        if (hasTaken == null) {
            details.add("hasTakenIeltsAcademic is required");
        }

        if (hasTaken == null) {
            throw validationFailed(details);
        }

        if (hasTaken.booleanValue()) {
            IeltsPreparationIntent providedIntent =
                    parsePreparationIntent(requestBody.getPreparationIntent(), "preparationIntent", false, details);
            if (providedIntent != null && providedIntent != IeltsPreparationIntent.UNSET) {
                details.add("preparationIntent must be UNSET when hasTakenIeltsAcademic is true");
            }

            List<NormalizedRecord> records = requestBody.getRecords() == null
                    ? Collections.<NormalizedRecord>emptyList()
                    : normalizeRecords(requestBody.getRecords(), "records", details);
            if (!details.isEmpty()) {
                throw validationFailed(details);
            }
            return new TeacherUpdate(
                    true,
                    IeltsPreparationIntent.UNSET,
                    records,
                    requestBody.getRecords() != null
            );
        }

        List<NormalizedRecord> records = normalizeRecords(requestBody.getRecords(), "records", details);
        if (!records.isEmpty()) {
            details.add("records must be empty when hasTakenIeltsAcademic is false");
        }
        IeltsPreparationIntent intent =
                parsePreparationIntent(requestBody.getPreparationIntent(), "preparationIntent", true, details);
        if (!details.isEmpty()) {
            throw validationFailed(details);
        }
        return new TeacherUpdate(false, intent, Collections.<NormalizedRecord>emptyList(), true);
    }

    private List<NormalizedRecord> normalizeRecords(List<StudentIeltsRecordDto> rawRecords,
                                                    String fieldPath,
                                                    List<String> details) {
        if (rawRecords == null || rawRecords.isEmpty()) {
            return Collections.emptyList();
        }

        List<NormalizedRecord> normalizedRecords = new ArrayList<NormalizedRecord>(rawRecords.size());
        LinkedHashSet<String> deduplicatedRecordIds = new LinkedHashSet<String>();
        for (int i = 0; i < rawRecords.size(); i++) {
            String itemPath = fieldPath + "[" + i + "]";
            StudentIeltsRecordDto rawRecord = rawRecords.get(i);
            if (rawRecord == null) {
                details.add(itemPath + " is required");
                continue;
            }

            String recordId = trimToNull(rawRecord.getRecordId());
            if (recordId == null) {
                details.add(itemPath + ".recordId is required");
            } else if (recordId.length() > MAX_RECORD_ID_LENGTH) {
                details.add(itemPath + ".recordId too long");
            } else if (!deduplicatedRecordIds.add(recordId)) {
                details.add(itemPath + ".recordId duplicated");
            }

            LocalDate testDate = parseDate(rawRecord.getTestDate(), itemPath + ".testDate", details);
            Double listening = normalizeBand(rawRecord.getListening(), itemPath + ".listening", details);
            Double reading = normalizeBand(rawRecord.getReading(), itemPath + ".reading", details);
            Double writing = normalizeBand(rawRecord.getWriting(), itemPath + ".writing", details);
            Double speaking = normalizeBand(rawRecord.getSpeaking(), itemPath + ".speaking", details);

            if (recordId == null || testDate == null || listening == null || reading == null || writing == null || speaking == null) {
                continue;
            }

            normalizedRecords.add(new NormalizedRecord(
                    recordId,
                    testDate,
                    listening.doubleValue(),
                    reading.doubleValue(),
                    writing.doubleValue(),
                    speaking.doubleValue()
            ));
        }
        return normalizedRecords;
    }

    private IeltsPreparationIntent parsePreparationIntent(String rawPreparationIntent,
                                                          String fieldPath,
                                                          boolean required,
                                                          List<String> details) {
        String normalized = trimToNull(rawPreparationIntent);
        if (normalized == null) {
            if (required) {
                details.add(fieldPath + " is required");
            }
            return null;
        }
        try {
            return IeltsPreparationIntent.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            details.add(fieldPath + " invalid");
            return null;
        }
    }

    private LocalDate parseDate(String rawDate, String fieldPath, List<String> details) {
        String normalized = trimToNull(rawDate);
        if (normalized == null) {
            details.add(fieldPath + " is required");
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

    private Double normalizeBand(Double rawBand, String fieldPath, List<String> details) {
        if (rawBand == null) {
            details.add(fieldPath + " is required");
            return null;
        }
        double value = rawBand.doubleValue();
        if (value < 0.0d || value > 9.0d) {
            details.add(fieldPath + " must be between 0.0 and 9.0");
            return null;
        }
        if (!isHalfStep(value)) {
            details.add(fieldPath + " must use 0.5 steps");
            return null;
        }
        return value;
    }

    private boolean isHalfStep(double value) {
        double scaled = value * 2.0d;
        return Math.abs(scaled - Math.rint(scaled)) < 0.000001d;
    }

    private Student requireCurrentStudent(HttpServletRequest request) {
        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: student role required.");
        }
        return studentRepository.findByUser_Id(operator.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found."));
    }

    private Student requireTeacherAccessibleStudent(Long studentId, HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        Long normalizedStudentId = requirePositiveId(studentId, "studentId");
        Student student = studentRepository.findById(normalizedStudentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + normalizedStudentId));
        ensureTeacherCanAccessStudent(operator, normalizedStudentId);
        return student;
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

    private static class RecordsUpdate {
        private final boolean hasTakenIeltsAcademic;
        private final IeltsPreparationIntent preparationIntent;
        private final List<NormalizedRecord> records;

        private RecordsUpdate(boolean hasTakenIeltsAcademic,
                              IeltsPreparationIntent preparationIntent,
                              List<NormalizedRecord> records) {
            this.hasTakenIeltsAcademic = hasTakenIeltsAcademic;
            this.preparationIntent = preparationIntent;
            this.records = records;
        }
    }

    private static class PreparationIntentUpdate {
        private final boolean hasTakenIeltsAcademic;
        private final IeltsPreparationIntent preparationIntent;

        private PreparationIntentUpdate(boolean hasTakenIeltsAcademic,
                                        IeltsPreparationIntent preparationIntent) {
            this.hasTakenIeltsAcademic = hasTakenIeltsAcademic;
            this.preparationIntent = preparationIntent;
        }
    }

    private static class TeacherUpdate {
        private final boolean hasTakenIeltsAcademic;
        private final IeltsPreparationIntent preparationIntent;
        private final List<NormalizedRecord> records;
        private final boolean overwriteRecords;

        private TeacherUpdate(boolean hasTakenIeltsAcademic,
                              IeltsPreparationIntent preparationIntent,
                              List<NormalizedRecord> records,
                              boolean overwriteRecords) {
            this.hasTakenIeltsAcademic = hasTakenIeltsAcademic;
            this.preparationIntent = preparationIntent;
            this.records = records;
            this.overwriteRecords = overwriteRecords;
        }
    }

    private static class NormalizedRecord {
        private final String recordId;
        private final LocalDate testDate;
        private final double listening;
        private final double reading;
        private final double writing;
        private final double speaking;

        private NormalizedRecord(String recordId,
                                 LocalDate testDate,
                                 double listening,
                                 double reading,
                                 double writing,
                                 double speaking) {
            this.recordId = recordId;
            this.testDate = testDate;
            this.listening = listening;
            this.reading = reading;
            this.writing = writing;
            this.speaking = speaking;
        }
    }
}
