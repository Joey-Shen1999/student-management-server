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
import com.studentmanagement.studentmanagementserver.repo.StudentIeltsManualStatusAuditLogRepository;
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
    private static final int VALIDITY_ANCHOR_MONTH = 5;
    private static final int VALIDITY_ANCHOR_DAY = 31;
    private static final int VALIDITY_ROLLING_YEARS = 2;
    private static final double IELTS_STRICT_MIN_OVERALL = 7.0d;
    private static final double IELTS_STRICT_MIN_LISTENING = 6.5d;
    private static final double IELTS_STRICT_MIN_READING = 6.5d;
    private static final double IELTS_STRICT_MIN_WRITING = 6.5d;
    private static final double IELTS_STRICT_MIN_SPEAKING = 6.5d;
    private static final double IELTS_COMMON_MIN_OVERALL = 6.5d;
    private static final double IELTS_COMMON_MIN_LISTENING = 6.0d;
    private static final double IELTS_COMMON_MIN_READING = 6.0d;
    private static final double IELTS_COMMON_MIN_WRITING = 6.0d;
    private static final double IELTS_COMMON_MIN_SPEAKING = 6.0d;
    private static final double TOEFL_STRICT_MIN_OVERALL = 5.0d;
    private static final double TOEFL_STRICT_MIN_LISTENING = 4.5d;
    private static final double TOEFL_STRICT_MIN_READING = 4.5d;
    private static final double TOEFL_STRICT_MIN_WRITING = 4.5d;
    private static final double TOEFL_STRICT_MIN_SPEAKING = 4.5d;
    private static final double TOEFL_COMMON_MIN_OVERALL = 4.5d;
    private static final double TOEFL_COMMON_MIN_LISTENING = 4.0d;
    private static final double TOEFL_COMMON_MIN_READING = 4.0d;
    private static final double TOEFL_COMMON_MIN_WRITING = 4.0d;
    private static final double TOEFL_COMMON_MIN_SPEAKING = 4.0d;
    private static final double DUOLINGO_STRICT_MIN_OVERALL = 130.0d;
    private static final double DUOLINGO_STRICT_MIN_LISTENING = 120.0d;
    private static final double DUOLINGO_STRICT_MIN_READING = 120.0d;
    private static final double DUOLINGO_STRICT_MIN_WRITING = 120.0d;
    private static final double DUOLINGO_STRICT_MIN_SPEAKING = 120.0d;
    private static final double DUOLINGO_COMMON_MIN_OVERALL = 120.0d;
    private static final double DUOLINGO_COMMON_MIN_LISTENING = 110.0d;
    private static final double DUOLINGO_COMMON_MIN_READING = 110.0d;
    private static final double DUOLINGO_COMMON_MIN_WRITING = 110.0d;
    private static final double DUOLINGO_COMMON_MIN_SPEAKING = 110.0d;
    private static final double IELTS_MIN_SCORE = 0.0d;
    private static final double IELTS_MAX_SCORE = 9.0d;
    private static final double TOEFL_MIN_SCORE = 1.0d;
    private static final double TOEFL_MAX_SCORE = 6.0d;
    private static final double DUOLINGO_MIN_SCORE = 10.0d;
    private static final double DUOLINGO_MAX_SCORE = 160.0d;
    private static final double DUOLINGO_SCORE_STEP = 5.0d;
    private static final String LANGUAGE_RISK_FLAG_RISK = "RISK";
    private static final String LANGUAGE_RISK_FLAG_LOW_RISK = "LOW_RISK";
    private static final String PROFILE_COMPLETENESS_COMPLETE = "COMPLETE";
    private static final String PROFILE_COMPLETENESS_INCOMPLETE = "INCOMPLETE";
    private static final String MANUAL_STATUS_SOURCE_TEACHER_UPDATE = "TEACHER_UPDATE";
    private static final String MANUAL_STATUS_SOURCE_STUDENT_DATA_UPDATE = "STUDENT_DATA_UPDATE_CLEAR";

    private final AuthSessionService authSessionService;
    private final ManagementAccessService managementAccessService;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherStudentRepository teacherStudentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentSchoolRecordRepository studentSchoolRecordRepository;
    private final StudentIeltsModuleRepository studentIeltsModuleRepository;
    private final StudentIeltsManualStatusAuditLogRepository studentIeltsManualStatusAuditLogRepository;
    private final StudentIeltsRecordRepository studentIeltsRecordRepository;

    public IeltsTrackingService(AuthSessionService authSessionService,
                                ManagementAccessService managementAccessService,
                                StudentRepository studentRepository,
                                TeacherRepository teacherRepository,
                                TeacherStudentRepository teacherStudentRepository,
                                StudentProfileRepository studentProfileRepository,
                                StudentSchoolRecordRepository studentSchoolRecordRepository,
                                StudentIeltsModuleRepository studentIeltsModuleRepository,
                                StudentIeltsManualStatusAuditLogRepository studentIeltsManualStatusAuditLogRepository,
                                StudentIeltsRecordRepository studentIeltsRecordRepository) {
        this.authSessionService = authSessionService;
        this.managementAccessService = managementAccessService;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.teacherStudentRepository = teacherStudentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentSchoolRecordRepository = studentSchoolRecordRepository;
        this.studentIeltsModuleRepository = studentIeltsModuleRepository;
        this.studentIeltsManualStatusAuditLogRepository = studentIeltsManualStatusAuditLogRepository;
        this.studentIeltsRecordRepository = studentIeltsRecordRepository;
    }

    @Transactional(readOnly = true)
    public StudentIeltsModuleStateDto getCurrentStudentModule(HttpServletRequest request) {
        CurrentStudentContext context = requireCurrentStudentContext(request);
        Student student = context.student;
        StudentIeltsModule module = studentIeltsModuleRepository.findByStudent_Id(student.getId()).orElse(null);
        return buildModuleState(student, module);
    }

    @Transactional
    public StudentIeltsModuleStateDto updateCurrentStudentRecords(StudentIeltsRecordsUpdateRequestDto requestBody,
                                                                  HttpServletRequest request) {
        CurrentStudentContext context = requireCurrentStudentContext(request);
        Student student = context.student;
        StudentIeltsModule existingModule = studentIeltsModuleRepository.findByStudent_Id(student.getId()).orElse(null);
        LanguageScoreType existingLanguageScoreType =
                resolveLanguageScoreType(existingModule == null ? null : existingModule.getLanguageScoreType());
        RecordsUpdate normalized = normalizeRecordsUpdateRequest(requestBody, existingLanguageScoreType);
        return saveModuleState(
                student,
                normalized.hasTakenIeltsAcademic,
                normalized.preparationIntent,
                normalized.records,
                normalized.languageScoreType,
                true,
                null,
                true,
                context.operator,
                MANUAL_STATUS_SOURCE_STUDENT_DATA_UPDATE
        );
    }

    @Transactional
    public StudentIeltsModuleStateDto updateCurrentStudentPreparationIntent(
            StudentIeltsPreparationIntentUpdateRequestDto requestBody,
            HttpServletRequest request) {
        CurrentStudentContext context = requireCurrentStudentContext(request);
        Student student = context.student;
        StudentIeltsModule existingModule = studentIeltsModuleRepository.findByStudent_Id(student.getId()).orElse(null);
        LanguageScoreType existingLanguageScoreType =
                resolveLanguageScoreType(existingModule == null ? null : existingModule.getLanguageScoreType());
        PreparationIntentUpdate normalized = normalizePreparationIntentUpdateRequest(requestBody, existingLanguageScoreType);
        return saveModuleState(
                student,
                normalized.hasTakenIeltsAcademic,
                normalized.preparationIntent,
                Collections.<NormalizedRecord>emptyList(),
                normalized.languageScoreType,
                !normalized.hasTakenIeltsAcademic,
                null,
                true,
                context.operator,
                MANUAL_STATUS_SOURCE_STUDENT_DATA_UPDATE
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
        TeacherStudentContext context = requireTeacherAccessibleStudentContext(studentId, request);
        Student student = context.student;
        StudentIeltsModule existingModule = studentIeltsModuleRepository.findByStudent_Id(student.getId()).orElse(null);
        LanguageScoreType existingLanguageScoreType =
                resolveLanguageScoreType(existingModule == null ? null : existingModule.getLanguageScoreType());
        TeacherUpdate normalized = normalizeTeacherUpdateRequest(requestBody, existingLanguageScoreType);
        return saveModuleState(
                student,
                normalized.hasTakenIeltsAcademic,
                normalized.preparationIntent,
                normalized.records,
                normalized.languageScoreType,
                normalized.overwriteRecords,
                normalized.languageTrackingManualStatus,
                normalized.overwriteLanguageTrackingManualStatus,
                context.operator,
                MANUAL_STATUS_SOURCE_TEACHER_UPDATE
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
        LanguageScoreType summaryLanguageScoreType = resolveLanguageScoreType(moduleState.getLanguageScoreType());

        String latestTestDate = null;
        Double bestOverallBand = null;
        for (StudentIeltsRecordDto record : records) {
            if (record == null) {
                continue;
            }
            if (latestTestDate == null && record.getTestDate() != null) {
                latestTestDate = record.getTestDate();
            }
            Double overall = calculateOverallBand(record, summaryLanguageScoreType);
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
                moduleState.getLanguageScoreType(),
                languageRisk != null && languageRisk.isShouldShowIeltsModule(),
                moduleState.isHasTakenIeltsAcademic(),
                moduleState.getPreparationIntent(),
                records.size(),
                latestTestDate,
                bestOverallBand,
                moduleState.getUpdatedAt(),
                moduleState.getTrackingStatus(),
                moduleState.getLanguageTrackingStatus(),
                new IeltsSummarySnapshotDto(
                        moduleState.getLanguageScoreType(),
                        moduleState.getTrackingStatus(),
                        moduleState.getLanguageTrackingStatus()
                )
        );
    }

    private StudentIeltsModuleStateDto saveModuleState(Student student,
                                                       boolean hasTakenIeltsAcademic,
                                                       IeltsPreparationIntent preparationIntent,
                                                       List<NormalizedRecord> records,
                                                       LanguageScoreType languageScoreType,
                                                       boolean overwriteRecords,
                                                       LanguageTrackingManualStatus languageTrackingManualStatus,
                                                       boolean overwriteLanguageTrackingManualStatus,
                                                       User operator,
                                                       String manualStatusChangeSource) {
        StudentIeltsModule module = studentIeltsModuleRepository.findByStudent_Id(student.getId())
                .orElseGet(() -> new StudentIeltsModule(student));

        module.updateState(hasTakenIeltsAcademic, preparationIntent);
        module.updateLanguageScoreType(resolveLanguageScoreType(languageScoreType));
        if (overwriteLanguageTrackingManualStatus) {
            applyLanguageTrackingManualStatus(module, languageTrackingManualStatus, operator, manualStatusChangeSource);
        }
        module.syncLanguageTrackingCompatibilityFields();
        module = studentIeltsModuleRepository.save(module);

        if (overwriteRecords) {
            if (!hasTakenIeltsAcademic || records == null || records.isEmpty()) {
                studentIeltsRecordRepository.deleteByIeltsModule_Id(module.getId());
            } else {
                overwriteIeltsRecords(module, records);
            }
        }

        updateAndPersistDerivedStatuses(student, module);
        return buildModuleState(student, module);
    }

    private void updateAndPersistDerivedStatuses(Student student, StudentIeltsModule module) {
        if (student == null || module == null || module.getId() == null) {
            return;
        }

        List<StudentIeltsRecord> records =
                studentIeltsRecordRepository.findByIeltsModule_IdOrderByTestDateDescIdDesc(module.getId());
        List<StudentSchoolRecord> schoolRecords =
                studentSchoolRecordRepository.findByStudent_IdOrderByIdAsc(student.getId());
        Integer graduationYear = resolveGraduationYear(schoolRecords);
        LanguageScoreType languageScoreType = resolveLanguageScoreType(module.getLanguageScoreType());
        IeltsTrackingStatus trackingStatus = deriveIeltsTrackingStatus(
                module.isHasTakenIeltsAcademic(),
                languageScoreType,
                graduationYear,
                records
        );
        LanguageTrackingStatus languageTrackingStatus =
                deriveLanguageTrackingStatus(trackingStatus, module.getLanguageTrackingManualStatus());
        module.updateDerivedStatuses(trackingStatus, languageTrackingStatus);
        studentIeltsModuleRepository.save(module);
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

        Integer graduationYear = resolveGraduationYear(schoolRecords);
        StudentIeltsLanguageRiskDto languageRisk = buildLanguageRisk(profile, schoolRecords, graduationYear);

        boolean hasTakenIeltsAcademic = module != null && module.isHasTakenIeltsAcademic();
        LanguageScoreType languageScoreType =
                resolveLanguageScoreType(module == null ? null : module.getLanguageScoreType());
        LanguageTrackingManualStatus languageTrackingManualStatus =
                module == null ? null : module.getLanguageTrackingManualStatus();
        IeltsTrackingStatus trackingStatus =
                deriveIeltsTrackingStatus(hasTakenIeltsAcademic, languageScoreType, graduationYear, records);
        LanguageTrackingStatus languageTrackingStatus =
                deriveLanguageTrackingStatus(trackingStatus, languageTrackingManualStatus);

        String updatedAt = resolveUpdatedAt(module, records);
        return new StudentIeltsModuleStateDto(
                student.getId(),
                graduationYear,
                languageScoreType.name(),
                hasTakenIeltsAcademic,
                module == null || module.getPreparationIntent() == null
                        ? IeltsPreparationIntent.UNSET.name()
                        : module.getPreparationIntent().name(),
                languageTrackingManualStatus == null ? null : languageTrackingManualStatus.name(),
                trackingStatus.name(),
                languageTrackingStatus.name(),
                new IeltsSummarySnapshotDto(
                        languageScoreType.name(),
                        trackingStatus.name(),
                        languageTrackingStatus.name()
                ),
                recordDtos,
                languageRisk,
                updatedAt
        );
    }

    private Integer resolveGraduationYear(List<StudentSchoolRecord> schoolRecords) {
        StudentSchoolRecord primarySchool = findPrimarySchool(schoolRecords);
        if (primarySchool == null || primarySchool.getEndTime() == null) {
            return null;
        }
        return Integer.valueOf(primarySchool.getEndTime().getYear());
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

    private void applyLanguageTrackingManualStatus(StudentIeltsModule module,
                                                   LanguageTrackingManualStatus nextStatus,
                                                   User operator,
                                                   String changeSource) {
        if (module == null) {
            return;
        }

        LanguageTrackingManualStatus previousStatus = module.getLanguageTrackingManualStatus();
        if (previousStatus == nextStatus) {
            return;
        }

        LocalDateTime changedAt = LocalDateTime.now();
        Long operatorUserId = operator == null ? null : operator.getId();
        module.updateLanguageTrackingManualStatus(nextStatus, operatorUserId, changedAt);

        studentIeltsManualStatusAuditLogRepository.save(new StudentIeltsManualStatusAuditLog(
                module.getStudent().getId(),
                operator,
                previousStatus == null ? null : previousStatus.name(),
                nextStatus == null ? null : nextStatus.name(),
                trimToNull(changeSource) == null ? "UNKNOWN" : trimToNull(changeSource),
                changedAt
        ));
    }

    private LanguageTrackingStatus deriveLanguageTrackingStatus(IeltsTrackingStatus trackingStatus,
                                                                LanguageTrackingManualStatus manualStatus) {
        if (manualStatus != null) {
            return LanguageTrackingStatus.valueOf(manualStatus.name());
        }
        if (trackingStatus == IeltsTrackingStatus.GREEN_STRICT_PASS) {
            return LanguageTrackingStatus.AUTO_PASS_ALL_SCHOOLS;
        }
        if (trackingStatus == IeltsTrackingStatus.GREEN_COMMON_PASS_WITH_WARNING) {
            return LanguageTrackingStatus.AUTO_PASS_PARTIAL_SCHOOLS;
        }
        return LanguageTrackingStatus.NEEDS_TRACKING;
    }

    private IeltsTrackingStatus deriveIeltsTrackingStatus(boolean hasTakenIeltsAcademic,
                                                          LanguageScoreType languageScoreType,
                                                          Integer graduationYear,
                                                          List<StudentIeltsRecord> records) {
        if (!hasTakenIeltsAcademic) {
            return IeltsTrackingStatus.YELLOW_NEEDS_PREPARATION;
        }
        StudentIeltsRecord latestValidRecord = findLatestValidRecord(records, graduationYear);
        if (latestValidRecord == null) {
            return IeltsTrackingStatus.YELLOW_NEEDS_PREPARATION;
        }
        LanguageScoreType resolvedLanguageScoreType = resolveLanguageScoreType(languageScoreType);

        if (resolvedLanguageScoreType == LanguageScoreType.DUOLINGO) {
            if (matchesThreshold(
                    latestValidRecord,
                    resolvedLanguageScoreType,
                    DUOLINGO_STRICT_MIN_OVERALL,
                    DUOLINGO_STRICT_MIN_LISTENING,
                    DUOLINGO_STRICT_MIN_READING,
                    DUOLINGO_STRICT_MIN_WRITING,
                    DUOLINGO_STRICT_MIN_SPEAKING
            )) {
                return IeltsTrackingStatus.GREEN_STRICT_PASS;
            }
            if (matchesThreshold(
                    latestValidRecord,
                    resolvedLanguageScoreType,
                    DUOLINGO_COMMON_MIN_OVERALL,
                    DUOLINGO_COMMON_MIN_LISTENING,
                    DUOLINGO_COMMON_MIN_READING,
                    DUOLINGO_COMMON_MIN_WRITING,
                    DUOLINGO_COMMON_MIN_SPEAKING
            )) {
                return IeltsTrackingStatus.GREEN_COMMON_PASS_WITH_WARNING;
            }
            return IeltsTrackingStatus.YELLOW_NEEDS_PREPARATION;
        }

        if (resolvedLanguageScoreType == LanguageScoreType.TOEFL) {
            if (matchesThreshold(
                    latestValidRecord,
                    resolvedLanguageScoreType,
                    TOEFL_STRICT_MIN_OVERALL,
                    TOEFL_STRICT_MIN_LISTENING,
                    TOEFL_STRICT_MIN_READING,
                    TOEFL_STRICT_MIN_WRITING,
                    TOEFL_STRICT_MIN_SPEAKING
            )) {
                return IeltsTrackingStatus.GREEN_STRICT_PASS;
            }
            if (matchesThreshold(
                    latestValidRecord,
                    resolvedLanguageScoreType,
                    TOEFL_COMMON_MIN_OVERALL,
                    TOEFL_COMMON_MIN_LISTENING,
                    TOEFL_COMMON_MIN_READING,
                    TOEFL_COMMON_MIN_WRITING,
                    TOEFL_COMMON_MIN_SPEAKING
            )) {
                return IeltsTrackingStatus.GREEN_COMMON_PASS_WITH_WARNING;
            }
            return IeltsTrackingStatus.YELLOW_NEEDS_PREPARATION;
        }

        if (matchesThreshold(
                latestValidRecord,
                resolvedLanguageScoreType,
                IELTS_STRICT_MIN_OVERALL,
                IELTS_STRICT_MIN_LISTENING,
                IELTS_STRICT_MIN_READING,
                IELTS_STRICT_MIN_WRITING,
                IELTS_STRICT_MIN_SPEAKING
        )) {
            return IeltsTrackingStatus.GREEN_STRICT_PASS;
        }
        if (matchesThreshold(
                latestValidRecord,
                resolvedLanguageScoreType,
                IELTS_COMMON_MIN_OVERALL,
                IELTS_COMMON_MIN_LISTENING,
                IELTS_COMMON_MIN_READING,
                IELTS_COMMON_MIN_WRITING,
                IELTS_COMMON_MIN_SPEAKING
        )) {
            return IeltsTrackingStatus.GREEN_COMMON_PASS_WITH_WARNING;
        }
        return IeltsTrackingStatus.YELLOW_NEEDS_PREPARATION;
    }

    private StudentIeltsRecord findLatestValidRecord(List<StudentIeltsRecord> records, Integer graduationYear) {
        if (records == null || records.isEmpty()) {
            return null;
        }
        for (StudentIeltsRecord record : records) {
            if (record == null || record.getTestDate() == null) {
                continue;
            }
            if (isValidForTrackingWindow(record.getTestDate(), graduationYear)) {
                return record;
            }
        }
        return null;
    }

    private boolean isValidForTrackingWindow(LocalDate testDate, Integer graduationYear) {
        if (testDate == null) {
            return false;
        }

        Integer effectiveGraduationYear = resolveEffectiveGraduationYear(graduationYear);
        if (effectiveGraduationYear == null) {
            return false;
        }

        LocalDate anchorDate = LocalDate.of(
                effectiveGraduationYear.intValue(),
                VALIDITY_ANCHOR_MONTH,
                VALIDITY_ANCHOR_DAY
        );
        LocalDate cutoffDate = LocalDate.of(
                effectiveGraduationYear.intValue() - VALIDITY_ROLLING_YEARS,
                VALIDITY_ANCHOR_MONTH,
                VALIDITY_ANCHOR_DAY
        );
        return !testDate.isAfter(anchorDate) && !testDate.isBefore(cutoffDate);
    }

    private Integer resolveEffectiveGraduationYear(Integer graduationYear) {
        Integer normalized = normalizeGraduationYear(graduationYear);
        if (normalized != null) {
            return normalized;
        }
        return resolveCurrentCohortGraduationYear();
    }

    private Integer normalizeGraduationYear(Integer graduationYear) {
        if (graduationYear == null) {
            return null;
        }
        int year = graduationYear.intValue();
        if (year < 1900 || year > 2999) {
            return null;
        }
        return Integer.valueOf(year);
    }

    private Integer resolveCurrentCohortGraduationYear() {
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        int currentYear = todayUtc.getYear();
        boolean hasPassedAnchor = todayUtc.getMonthValue() > VALIDITY_ANCHOR_MONTH
                || (todayUtc.getMonthValue() == VALIDITY_ANCHOR_MONTH && todayUtc.getDayOfMonth() > VALIDITY_ANCHOR_DAY);
        int candidateYear = hasPassedAnchor ? currentYear + 1 : currentYear;
        if (candidateYear < 1900 || candidateYear > 2999) {
            return null;
        }
        return Integer.valueOf(candidateYear);
    }

    private boolean matchesThreshold(StudentIeltsRecord record,
                                     LanguageScoreType languageScoreType,
                                     double minimumOverall,
                                     double minimumListening,
                                     double minimumReading,
                                     double minimumWriting,
                                     double minimumSpeaking) {
        if (record == null) {
            return false;
        }

        Double overall = calculateOverallBandForTracking(record, languageScoreType);
        if (overall == null || overall.doubleValue() < minimumOverall) {
            return false;
        }

        return record.getListening() >= minimumListening
                && record.getReading() >= minimumReading
                && record.getWriting() >= minimumWriting
                && record.getSpeaking() >= minimumSpeaking;
    }

    private Double calculateOverallBandForTracking(StudentIeltsRecord record, LanguageScoreType languageScoreType) {
        if (record == null) {
            return null;
        }
        LanguageScoreType resolvedLanguageScoreType = resolveLanguageScoreType(languageScoreType);
        double average = (record.getListening() + record.getReading() + record.getWriting() + record.getSpeaking()) / 4.0d;
        if (resolvedLanguageScoreType == LanguageScoreType.DUOLINGO) {
            double roundedStep = roundToNearestStep(average, DUOLINGO_SCORE_STEP);
            return Math.round(roundedStep * 10.0d) / 10.0d;
        }
        if (resolvedLanguageScoreType == LanguageScoreType.TOEFL) {
            return Math.round(average * 10.0d) / 10.0d;
        }
        double roundedHalfStep = Math.round(average * 2.0d) / 2.0d;
        return Math.round(roundedHalfStep * 10.0d) / 10.0d;
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

    private Double calculateOverallBand(StudentIeltsRecordDto record, LanguageScoreType languageScoreType) {
        if (record == null
                || record.getListening() == null
                || record.getReading() == null
                || record.getWriting() == null
                || record.getSpeaking() == null) {
            return null;
        }
        LanguageScoreType resolvedLanguageScoreType = resolveLanguageScoreType(languageScoreType);
        double average = (record.getListening() + record.getReading() + record.getWriting() + record.getSpeaking()) / 4.0d;
        if (resolvedLanguageScoreType == LanguageScoreType.DUOLINGO) {
            double roundedStep = roundToNearestStep(average, DUOLINGO_SCORE_STEP);
            return Math.round(roundedStep * 10.0d) / 10.0d;
        }
        return Math.round(average * 10.0d) / 10.0d;
    }

    private RecordsUpdate normalizeRecordsUpdateRequest(StudentIeltsRecordsUpdateRequestDto requestBody,
                                                        LanguageScoreType existingLanguageScoreType) {
        if (requestBody == null) {
            throw validationFailed(Collections.singletonList("request body is required"));
        }
        if (requestBody.isLanguageScoreTrackingManualStatusPresent()) {
            throw fieldForbidden("languageScoreTrackingManualStatus");
        }

        List<String> details = new ArrayList<String>();
        Boolean hasTaken = requestBody.getHasTakenIeltsAcademic();
        if (hasTaken == null) {
            details.add("hasTakenIeltsAcademic is required");
        }

        LanguageScoreType languageScoreType = resolveRequestedLanguageScoreType(
                requestBody.getLanguageScoreType(),
                existingLanguageScoreType,
                requestBody.isToeflRecordsPresent(),
                requestBody.isDuolingoRecordsPresent(),
                "languageScoreType",
                details
        );
        List<StudentIeltsRecordDto> rawRecords = resolveRequestedRecords(
                languageScoreType,
                requestBody.getRecords(),
                requestBody.getToeflRecords(),
                requestBody.isToeflRecordsPresent(),
                requestBody.getDuolingoRecords(),
                requestBody.isDuolingoRecordsPresent()
        );
        List<NormalizedRecord> records = normalizeRecords(
                rawRecords,
                resolveRequestedRecordsFieldPath(
                        languageScoreType,
                        requestBody.isToeflRecordsPresent(),
                        requestBody.isDuolingoRecordsPresent()
                ),
                languageScoreType,
                details
        );
        if (Boolean.FALSE.equals(hasTaken) && !records.isEmpty()) {
            details.add("records must be empty when hasTakenIeltsAcademic is false");
        }
        if (!details.isEmpty()) {
            throw validationFailed(details);
        }

        return new RecordsUpdate(hasTaken.booleanValue(), IeltsPreparationIntent.UNSET, records, languageScoreType);
    }

    private PreparationIntentUpdate normalizePreparationIntentUpdateRequest(
            StudentIeltsPreparationIntentUpdateRequestDto requestBody,
            LanguageScoreType existingLanguageScoreType) {
        if (requestBody == null) {
            throw validationFailed(Collections.singletonList("request body is required"));
        }
        if (requestBody.isLanguageScoreTrackingManualStatusPresent()) {
            throw fieldForbidden("languageScoreTrackingManualStatus");
        }

        List<String> details = new ArrayList<String>();
        Boolean hasTaken = requestBody.getHasTakenIeltsAcademic();
        if (hasTaken == null) {
            details.add("hasTakenIeltsAcademic is required");
        }
        LanguageScoreType languageScoreType = resolveRequestedLanguageScoreType(
                requestBody.getLanguageScoreType(),
                existingLanguageScoreType,
                false,
                false,
                "languageScoreType",
                details
        );
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
        return new PreparationIntentUpdate(hasTaken.booleanValue(), normalizedIntent, languageScoreType);
    }

    private TeacherUpdate normalizeTeacherUpdateRequest(TeacherIeltsModuleUpdateRequestDto requestBody,
                                                        LanguageScoreType existingLanguageScoreType) {
        if (requestBody == null) {
            throw validationFailed(Collections.singletonList("request body is required"));
        }

        List<String> details = new ArrayList<String>();
        boolean overwriteLanguageTrackingManualStatus = requestBody.isLanguageScoreTrackingManualStatusPresent();
        LanguageTrackingManualStatus languageTrackingManualStatus = overwriteLanguageTrackingManualStatus
                ? parseLanguageTrackingManualStatus(
                requestBody.getLanguageScoreTrackingManualStatus(),
                "languageScoreTrackingManualStatus",
                details
        )
                : null;

        Boolean hasTaken = requestBody.getHasTakenIeltsAcademic();
        if (hasTaken == null) {
            details.add("hasTakenIeltsAcademic is required");
        }
        LanguageScoreType languageScoreType = resolveRequestedLanguageScoreType(
                requestBody.getLanguageScoreType(),
                existingLanguageScoreType,
                requestBody.isToeflRecordsPresent(),
                requestBody.isDuolingoRecordsPresent(),
                "languageScoreType",
                details
        );

        if (hasTaken == null) {
            throw validationFailed(details);
        }

        if (hasTaken.booleanValue()) {
            IeltsPreparationIntent providedIntent =
                    parsePreparationIntent(requestBody.getPreparationIntent(), "preparationIntent", false, details);
            if (providedIntent != null && providedIntent != IeltsPreparationIntent.UNSET) {
                details.add("preparationIntent must be UNSET when hasTakenIeltsAcademic is true");
            }

            List<StudentIeltsRecordDto> rawRecords = resolveRequestedRecords(
                    languageScoreType,
                    requestBody.getRecords(),
                    requestBody.getToeflRecords(),
                    requestBody.isToeflRecordsPresent(),
                    requestBody.getDuolingoRecords(),
                    requestBody.isDuolingoRecordsPresent()
            );
            List<NormalizedRecord> records = rawRecords == null
                    ? Collections.<NormalizedRecord>emptyList()
                    : normalizeRecords(
                    rawRecords,
                    resolveRequestedRecordsFieldPath(
                            languageScoreType,
                            requestBody.isToeflRecordsPresent(),
                            requestBody.isDuolingoRecordsPresent()
                    ),
                    languageScoreType,
                    details
            );
            if (!details.isEmpty()) {
                throw validationFailed(details);
            }
            return new TeacherUpdate(
                    true,
                    IeltsPreparationIntent.UNSET,
                    records,
                    rawRecords != null,
                    languageTrackingManualStatus,
                    overwriteLanguageTrackingManualStatus,
                    languageScoreType
            );
        }

        List<StudentIeltsRecordDto> rawRecords = resolveRequestedRecords(
                languageScoreType,
                requestBody.getRecords(),
                requestBody.getToeflRecords(),
                requestBody.isToeflRecordsPresent(),
                requestBody.getDuolingoRecords(),
                requestBody.isDuolingoRecordsPresent()
        );
        List<NormalizedRecord> records = normalizeRecords(
                rawRecords,
                resolveRequestedRecordsFieldPath(
                        languageScoreType,
                        requestBody.isToeflRecordsPresent(),
                        requestBody.isDuolingoRecordsPresent()
                ),
                languageScoreType,
                details
        );
        if (!records.isEmpty()) {
            details.add("records must be empty when hasTakenIeltsAcademic is false");
        }
        IeltsPreparationIntent intent =
                parsePreparationIntent(requestBody.getPreparationIntent(), "preparationIntent", true, details);
        if (!details.isEmpty()) {
            throw validationFailed(details);
        }
        return new TeacherUpdate(
                false,
                intent,
                Collections.<NormalizedRecord>emptyList(),
                true,
                languageTrackingManualStatus,
                overwriteLanguageTrackingManualStatus,
                languageScoreType
        );
    }

    private LanguageScoreType resolveRequestedLanguageScoreType(String rawLanguageScoreType,
                                                                LanguageScoreType fallbackLanguageScoreType,
                                                                boolean toeflRecordsPresent,
                                                                boolean duolingoRecordsPresent,
                                                                String fieldPath,
                                                                List<String> details) {
        String normalizedRaw = trimToNull(rawLanguageScoreType);
        if (normalizedRaw != null) {
            try {
                return LanguageScoreType.valueOf(normalizedRaw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                details.add(fieldPath + " invalid");
                return resolveLanguageScoreType(fallbackLanguageScoreType);
            }
        }
        if (duolingoRecordsPresent) {
            return LanguageScoreType.DUOLINGO;
        }
        if (toeflRecordsPresent) {
            return LanguageScoreType.TOEFL;
        }
        return resolveLanguageScoreType(fallbackLanguageScoreType);
    }

    private LanguageScoreType resolveLanguageScoreType(LanguageScoreType languageScoreType) {
        return languageScoreType == null ? LanguageScoreType.IELTS : languageScoreType;
    }

    private LanguageScoreType resolveLanguageScoreType(String languageScoreType) {
        String normalized = trimToNull(languageScoreType);
        if (normalized == null) {
            return LanguageScoreType.IELTS;
        }
        try {
            return LanguageScoreType.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return LanguageScoreType.IELTS;
        }
    }

    private List<StudentIeltsRecordDto> resolveRequestedRecords(LanguageScoreType languageScoreType,
                                                                List<StudentIeltsRecordDto> records,
                                                                List<StudentIeltsRecordDto> toeflRecords,
                                                                boolean toeflRecordsPresent,
                                                                List<StudentIeltsRecordDto> duolingoRecords,
                                                                boolean duolingoRecordsPresent) {
        LanguageScoreType resolvedLanguageScoreType = resolveLanguageScoreType(languageScoreType);
        if (resolvedLanguageScoreType == LanguageScoreType.TOEFL) {
            if (toeflRecordsPresent) {
                return toeflRecords;
            }
            return records;
        }
        if (resolvedLanguageScoreType == LanguageScoreType.DUOLINGO) {
            if (duolingoRecordsPresent) {
                return duolingoRecords;
            }
            return records;
        }
        return records;
    }

    private String resolveRequestedRecordsFieldPath(LanguageScoreType languageScoreType,
                                                    boolean toeflRecordsPresent,
                                                    boolean duolingoRecordsPresent) {
        LanguageScoreType resolvedLanguageScoreType = resolveLanguageScoreType(languageScoreType);
        if (resolvedLanguageScoreType == LanguageScoreType.TOEFL && toeflRecordsPresent) {
            return "toeflRecords";
        }
        if (resolvedLanguageScoreType == LanguageScoreType.DUOLINGO && duolingoRecordsPresent) {
            return "duolingoRecords";
        }
        return "records";
    }

    private List<NormalizedRecord> normalizeRecords(List<StudentIeltsRecordDto> rawRecords,
                                                    String fieldPath,
                                                    LanguageScoreType languageScoreType,
                                                    List<String> details) {
        if (rawRecords == null || rawRecords.isEmpty()) {
            return Collections.<NormalizedRecord>emptyList();
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
            Double listening = normalizeBand(rawRecord.getListening(), itemPath + ".listening", languageScoreType, details);
            Double reading = normalizeBand(rawRecord.getReading(), itemPath + ".reading", languageScoreType, details);
            Double writing = normalizeBand(rawRecord.getWriting(), itemPath + ".writing", languageScoreType, details);
            Double speaking = normalizeBand(rawRecord.getSpeaking(), itemPath + ".speaking", languageScoreType, details);

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

    private LanguageTrackingManualStatus parseLanguageTrackingManualStatus(String rawManualStatus,
                                                                           String fieldPath,
                                                                           List<String> details) {
        String normalized = trimToNull(rawManualStatus);
        if (normalized == null) {
            return null;
        }
        try {
            return LanguageTrackingManualStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
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

    private Double normalizeBand(Double rawBand,
                                 String fieldPath,
                                 LanguageScoreType languageScoreType,
                                 List<String> details) {
        if (rawBand == null) {
            details.add(fieldPath + " is required");
            return null;
        }
        double value = rawBand.doubleValue();
        LanguageScoreType resolvedLanguageScoreType = resolveLanguageScoreType(languageScoreType);
        double min;
        double max;
        if (resolvedLanguageScoreType == LanguageScoreType.TOEFL) {
            min = TOEFL_MIN_SCORE;
            max = TOEFL_MAX_SCORE;
        } else if (resolvedLanguageScoreType == LanguageScoreType.DUOLINGO) {
            min = DUOLINGO_MIN_SCORE;
            max = DUOLINGO_MAX_SCORE;
        } else {
            min = IELTS_MIN_SCORE;
            max = IELTS_MAX_SCORE;
        }
        if (value < min || value > max) {
            details.add(fieldPath + " must be between " + min + " and " + max);
            return null;
        }
        if (resolvedLanguageScoreType == LanguageScoreType.DUOLINGO && !isStep(value, DUOLINGO_SCORE_STEP)) {
            details.add(fieldPath + " must use 5-point steps");
            return null;
        }
        if (resolvedLanguageScoreType != LanguageScoreType.DUOLINGO && !isHalfStep(value)) {
            details.add(fieldPath + " must use 0.5 steps");
            return null;
        }
        return value;
    }

    private boolean isHalfStep(double value) {
        double scaled = value * 2.0d;
        return Math.abs(scaled - Math.rint(scaled)) < 0.000001d;
    }

    private boolean isStep(double value, double step) {
        double scaled = value / step;
        return Math.abs(scaled - Math.rint(scaled)) < 0.000001d;
    }

    private double roundToNearestStep(double value, double step) {
        return Math.round(value / step) * step;
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

    private ApiRequestException fieldForbidden(String fieldName) {
        return new ApiRequestException(
                HttpStatus.FORBIDDEN,
                "FIELD_FORBIDDEN",
                "Forbidden field in student request.",
                Collections.singletonList(fieldName + " is not allowed for student APIs")
        );
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
        private final User operator;

        private CurrentStudentContext(Student student, User operator) {
            this.student = student;
            this.operator = operator;
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

    private static class RecordsUpdate {
        private final boolean hasTakenIeltsAcademic;
        private final IeltsPreparationIntent preparationIntent;
        private final List<NormalizedRecord> records;
        private final LanguageScoreType languageScoreType;

        private RecordsUpdate(boolean hasTakenIeltsAcademic,
                              IeltsPreparationIntent preparationIntent,
                              List<NormalizedRecord> records,
                              LanguageScoreType languageScoreType) {
            this.hasTakenIeltsAcademic = hasTakenIeltsAcademic;
            this.preparationIntent = preparationIntent;
            this.records = records;
            this.languageScoreType = languageScoreType;
        }
    }

    private static class PreparationIntentUpdate {
        private final boolean hasTakenIeltsAcademic;
        private final IeltsPreparationIntent preparationIntent;
        private final LanguageScoreType languageScoreType;

        private PreparationIntentUpdate(boolean hasTakenIeltsAcademic,
                                        IeltsPreparationIntent preparationIntent,
                                        LanguageScoreType languageScoreType) {
            this.hasTakenIeltsAcademic = hasTakenIeltsAcademic;
            this.preparationIntent = preparationIntent;
            this.languageScoreType = languageScoreType;
        }
    }

    private static class TeacherUpdate {
        private final boolean hasTakenIeltsAcademic;
        private final IeltsPreparationIntent preparationIntent;
        private final List<NormalizedRecord> records;
        private final boolean overwriteRecords;
        private final LanguageTrackingManualStatus languageTrackingManualStatus;
        private final boolean overwriteLanguageTrackingManualStatus;
        private final LanguageScoreType languageScoreType;

        private TeacherUpdate(boolean hasTakenIeltsAcademic,
                              IeltsPreparationIntent preparationIntent,
                              List<NormalizedRecord> records,
                              boolean overwriteRecords,
                              LanguageTrackingManualStatus languageTrackingManualStatus,
                              boolean overwriteLanguageTrackingManualStatus,
                              LanguageScoreType languageScoreType) {
            this.hasTakenIeltsAcademic = hasTakenIeltsAcademic;
            this.preparationIntent = preparationIntent;
            this.records = records;
            this.overwriteRecords = overwriteRecords;
            this.languageTrackingManualStatus = languageTrackingManualStatus;
            this.overwriteLanguageTrackingManualStatus = overwriteLanguageTrackingManualStatus;
            this.languageScoreType = languageScoreType;
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
