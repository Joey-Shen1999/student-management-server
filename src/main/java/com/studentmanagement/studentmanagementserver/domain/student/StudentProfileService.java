package com.studentmanagement.studentmanagementserver.domain.student;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.SchoolType;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentCourseRecordRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentIdentityFileRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentProfileChangeEventRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentProfileRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentProfileVersionRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentSchoolRecordRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentSchoolTranscriptRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import com.studentmanagement.studentmanagementserver.service.ProfileVersionConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

@Service
public class StudentProfileService {

    private static final Logger log = LoggerFactory.getLogger(StudentProfileService.class);
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern SCHOOL_BOARD_PATTERN = Pattern.compile("^[\\p{L}\\p{N} &()/.\\-]+$");
    private static final Pattern LOCAL_STUDENT_NUMBER_PATTERN = Pattern.compile("^\\d{9}$");
    private static final int MAX_SCHOOL_BOARD_LENGTH = 64;
    private static final int MAX_UNIQUE_SCHOOLS_PER_PROFILE = 100;
    private static final int MAX_TEACHER_NOTE_LENGTH = 5000;
    private static final long MAX_UPLOAD_SIZE_BYTES = 50L * 1024L * 1024L;
    private static final int DEFAULT_HISTORY_PAGE = 0;
    private static final int DEFAULT_HISTORY_SIZE = 20;
    private static final int MAX_HISTORY_SIZE = 100;
    private static final String CHANGE_SOURCE_MANUAL_SAVE = "manual_save";
    private static final String CHANGE_SOURCE_AUTO_SAVE = "auto_save";
    private static final String CHANGE_SOURCE_FILE_UPLOAD = "file_upload";
    private static final String CHANGE_SOURCE_VERSION_RESTORE = "version_restore";
    private static final String STUDENT_REGION_ONTARIO = "Ontario";
    private static final String STUDENT_REGION_BRITISH_COLUMBIA = "British Columbia";
    private static final String STUDENT_REGION_ALBERTA = "Alberta";
    private static final String STUDENT_REGION_SASKATCHEWAN = "Saskatchewan";
    private static final String STUDENT_REGION_MANITOBA = "Manitoba";
    private static final String STUDENT_REGION_QUEBEC = "Quebec";
    private static final String STUDENT_REGION_NEW_BRUNSWICK = "New Brunswick";
    private static final String STUDENT_REGION_NOVA_SCOTIA = "Nova Scotia";
    private static final String STUDENT_REGION_PRINCE_EDWARD_ISLAND = "Prince Edward Island";
    private static final String STUDENT_REGION_NEWFOUNDLAND_AND_LABRADOR = "Newfoundland and Labrador";
    private static final String STUDENT_REGION_YUKON = "Yukon";
    private static final String STUDENT_REGION_NORTHWEST_TERRITORIES = "Northwest Territories";
    private static final String STUDENT_REGION_NUNAVUT = "Nunavut";
    private static final String STUDENT_REGION_CHINA = "China";
    private static final String STUDENT_REGION_UNITED_STATES = "United States";
    private static final Set<String> SUPPORTED_STUDENT_REGIONS = buildSupportedStudentRegions();
    private static final Map<String, String> STUDENT_REGION_ALIASES = buildStudentRegionAliases();
    private static final Set<String> REPORT_CARD_MONTHS = buildReportCardMonths();
    private static final Set<String> HISTORY_SENSITIVE_FIELDS = buildHistorySensitiveFields();

    private final AuthSessionService authSessionService;
    private final StudentRepository studentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentSchoolRecordRepository studentSchoolRecordRepository;
    private final StudentSchoolTranscriptRepository studentSchoolTranscriptRepository;
    private final StudentIdentityFileRepository studentIdentityFileRepository;
    private final StudentCourseRecordRepository studentCourseRecordRepository;
    private final StudentProfileChangeEventRepository studentProfileChangeEventRepository;
    private final StudentProfileVersionRepository studentProfileVersionRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentSchoolTranscriptStorageService transcriptStorageService;
    private final StudentIdentityFileStorageService identityFileStorageService;
    private final StudentDocumentService studentDocumentService;
    private final ObjectMapper objectMapper;

    public StudentProfileService(AuthSessionService authSessionService,
                                 StudentRepository studentRepository,
                                 StudentProfileRepository studentProfileRepository,
                                 StudentSchoolRecordRepository studentSchoolRecordRepository,
                                 StudentSchoolTranscriptRepository studentSchoolTranscriptRepository,
                                 StudentIdentityFileRepository studentIdentityFileRepository,
                                 StudentCourseRecordRepository studentCourseRecordRepository,
                                 StudentProfileChangeEventRepository studentProfileChangeEventRepository,
                                 StudentProfileVersionRepository studentProfileVersionRepository,
                                 UserRepository userRepository,
                                 TeacherRepository teacherRepository,
                                 StudentSchoolTranscriptStorageService transcriptStorageService,
                                 StudentIdentityFileStorageService identityFileStorageService,
                                 StudentDocumentService studentDocumentService,
                                 ObjectMapper objectMapper) {
        this.authSessionService = authSessionService;
        this.studentRepository = studentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentSchoolRecordRepository = studentSchoolRecordRepository;
        this.studentSchoolTranscriptRepository = studentSchoolTranscriptRepository;
        this.studentIdentityFileRepository = studentIdentityFileRepository;
        this.studentCourseRecordRepository = studentCourseRecordRepository;
        this.studentProfileChangeEventRepository = studentProfileChangeEventRepository;
        this.studentProfileVersionRepository = studentProfileVersionRepository;
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.transcriptStorageService = transcriptStorageService;
        this.identityFileStorageService = identityFileStorageService;
        this.studentDocumentService = studentDocumentService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public StudentProfileDto getCurrentStudentProfile(HttpServletRequest request) {
        Student student = requireCurrentStudent(request);
        return getProfileForStudent(student, false);
    }

    @Transactional(readOnly = true)
    public StudentProfileDto getProfileByStudentId(Long studentId) {
        Student student = requireStudentById(studentId);
        return getProfileForStudent(student, false);
    }

    @Transactional(readOnly = true)
    public TeacherStudentProfileDto getProfileByStudentIdForTeacher(Long studentId) {
        Student student = requireStudentById(studentId);
        return toTeacherDto(getProfileForStudent(student, true));
    }

    @Transactional(readOnly = true)
    public StudentProfileHistoryListDto getCurrentStudentProfileHistory(Integer page,
                                                                        Integer size,
                                                                        HttpServletRequest request) {
        Student student = requireCurrentStudent(request);
        return getProfileHistoryByStudentId(student.getId(), page, size);
    }

    @Transactional(readOnly = true)
    public StudentProfileHistoryListDto getProfileHistoryByStudentId(Long studentId, Integer page, Integer size) {
        Student student = requireStudentById(studentId);
        int normalizedPage = normalizeHistoryPage(page);
        int normalizedSize = normalizeHistorySize(size);
        Pageable pageable = PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by(Sort.Order.desc("changedAt"), Sort.Order.desc("id"))
        );

        Page<StudentProfileChangeEvent> result = studentProfileChangeEventRepository.findByStudentId(student.getId(), pageable);
        StudentProfileHistoryListDto response = new StudentProfileHistoryListDto();
        response.setItems(toHistoryItems(result.getContent()));
        response.setTotal(result.getTotalElements());
        response.setPage(normalizedPage);
        response.setSize(normalizedSize);
        return response;
    }

    @Transactional
    public StudentProfileDto saveCurrentStudentProfile(StudentProfileDto requestBody,
                                                       HttpServletRequest request,
                                                       String ifMatch,
                                                       String changeSource) {
        Student student = requireCurrentStudent(request);
        Long expectedVersion = resolveExpectedVersion(requestBody, ifMatch);
        return saveProfileForStudent(
                student,
                requestBody,
                student.getUser().getId(),
                resolveTraceId(request),
                normalizeChangeSource(changeSource, CHANGE_SOURCE_MANUAL_SAVE),
                expectedVersion,
                false,
                null,
                false
        );
    }

    @Transactional
    public StudentProfileDto saveProfileByStudentId(Long studentId, StudentProfileDto requestBody, Long operatorUserId) {
        return saveProfileByStudentId(studentId, requestBody, operatorUserId, "N/A");
    }

    @Transactional
    public StudentProfileDto saveProfileByStudentId(Long studentId,
                                                    StudentProfileDto requestBody,
                                                    Long operatorUserId,
                                                    String traceId) {
        Student student = requireStudentById(studentId);
        return saveProfileForStudent(
                student,
                requestBody,
                operatorUserId,
                traceId,
                CHANGE_SOURCE_MANUAL_SAVE,
                null,
                false,
                null,
                false
        );
    }

    @Transactional
    public TeacherStudentProfileDto saveProfileByStudentIdForTeacher(Long studentId,
                                                                     TeacherStudentProfileDto requestBody,
                                                                     Long operatorUserId,
                                                                     String traceId) {
        return saveProfileByStudentIdForTeacher(
                studentId,
                requestBody,
                operatorUserId,
                traceId,
                null,
                CHANGE_SOURCE_MANUAL_SAVE
        );
    }

    @Transactional
    public TeacherStudentProfileDto saveProfileByStudentIdForTeacher(Long studentId,
                                                                     TeacherStudentProfileDto requestBody,
                                                                     Long operatorUserId,
                                                                     String traceId,
                                                                     String ifMatch,
                                                                     String changeSource) {
        Student student = requireStudentById(studentId);
        String teacherNoteToSave = null;
        boolean teacherNoteProvided = false;
        if (requestBody != null && requestBody.isTeacherNoteProvided()) {
            teacherNoteToSave = normalizeTeacherNote(requestBody.getTeacherNote());
            teacherNoteProvided = true;
        }
        Long expectedVersion = resolveExpectedVersion(requestBody, ifMatch);
        StudentProfileDto saved = saveProfileForStudent(
                student,
                requestBody,
                operatorUserId,
                traceId,
                normalizeChangeSource(changeSource, CHANGE_SOURCE_MANUAL_SAVE),
                expectedVersion,
                teacherNoteProvided,
                teacherNoteToSave,
                true
        );
        return toTeacherDto(saved);
    }

    @Transactional
    public StudentSchoolTranscriptDto uploadCurrentStudentSchoolTranscript(Long schoolRecordId,
                                                                           MultipartFile file,
                                                                           String academicRecordType,
                                                                           Integer reportYear,
                                                                           String reportMonth,
                                                                           HttpServletRequest request) {
        Student student = requireCurrentStudent(request);
        return uploadSchoolTranscriptForStudent(
                student,
                schoolRecordId,
                file,
                academicRecordType,
                reportYear,
                reportMonth,
                student.getUser().getId(),
                resolveTraceId(request),
                CHANGE_SOURCE_FILE_UPLOAD
        );
    }

    @Transactional
    public StudentSchoolTranscriptDto uploadStudentSchoolTranscriptByStudentId(Long studentId,
                                                                               Long schoolRecordId,
                                                                               MultipartFile file) {
        return uploadStudentSchoolTranscriptByStudentId(
                studentId,
                schoolRecordId,
                file,
                null,
                null,
                null,
                null,
                "N/A"
        );
    }

    @Transactional
    public StudentSchoolTranscriptDto uploadStudentSchoolTranscriptByStudentId(Long studentId,
                                                                               Long schoolRecordId,
                                                                               MultipartFile file,
                                                                               String academicRecordType,
                                                                               Integer reportYear,
                                                                               String reportMonth,
                                                                               Long uploadedBy,
                                                                               String traceId) {
        Student student = requireStudentById(studentId);
        Long operatorUserId = uploadedBy == null ? student.getUser().getId() : uploadedBy;
        return uploadSchoolTranscriptForStudent(
                student,
                schoolRecordId,
                file,
                academicRecordType,
                reportYear,
                reportMonth,
                operatorUserId,
                traceId,
                CHANGE_SOURCE_FILE_UPLOAD
        );
    }

    @Transactional(readOnly = true)
    public SchoolTranscriptDownload downloadCurrentStudentSchoolTranscript(Long schoolRecordId, HttpServletRequest request) {
        Student student = requireCurrentStudent(request);
        return downloadSchoolTranscriptForStudent(student, schoolRecordId);
    }

    @Transactional(readOnly = true)
    public SchoolTranscriptDownload downloadCurrentStudentSchoolTranscriptByTranscriptId(Long schoolRecordId,
                                                                                          Long transcriptId,
                                                                                          HttpServletRequest request) {
        Student student = requireCurrentStudent(request);
        return downloadSchoolTranscriptForStudentByTranscriptId(student, schoolRecordId, transcriptId);
    }

    @Transactional(readOnly = true)
    public SchoolTranscriptDownload downloadStudentSchoolTranscriptByStudentId(Long studentId, Long schoolRecordId) {
        Student student = requireStudentById(studentId);
        return downloadSchoolTranscriptForStudent(student, schoolRecordId);
    }

    @Transactional(readOnly = true)
    public SchoolTranscriptDownload downloadStudentSchoolTranscriptByStudentIdAndTranscriptId(Long studentId,
                                                                                               Long schoolRecordId,
                                                                                               Long transcriptId) {
        Student student = requireStudentById(studentId);
        return downloadSchoolTranscriptForStudentByTranscriptId(student, schoolRecordId, transcriptId);
    }

    @Transactional
    public StudentIdentityFileUploadDto uploadCurrentStudentIdentityFile(MultipartFile file,
                                                                         String identityDocumentType,
                                                                         HttpServletRequest request) {
        Student student = requireCurrentStudent(request);
        return uploadIdentityFileForStudent(
                student,
                file,
                identityDocumentType,
                student.getUser().getId(),
                resolveTraceId(request),
                CHANGE_SOURCE_FILE_UPLOAD
        );
    }

    @Transactional
    public StudentIdentityFileUploadDto uploadStudentIdentityFileByStudentId(Long studentId,
                                                                              MultipartFile file,
                                                                              String identityDocumentType,
                                                                              Long uploadedBy,
                                                                              String traceId) {
        Student student = requireStudentById(studentId);
        Long operatorUserId = uploadedBy == null ? student.getUser().getId() : uploadedBy;
        return uploadIdentityFileForStudent(
                student,
                file,
                identityDocumentType,
                operatorUserId,
                traceId,
                CHANGE_SOURCE_FILE_UPLOAD
        );
    }

    @Transactional(readOnly = true)
    public IdentityFileDownload downloadCurrentStudentIdentityFileByIdentityFileId(Long identityFileId,
                                                                                    HttpServletRequest request) {
        Student student = requireCurrentStudent(request);
        return downloadIdentityFileForStudentById(student, identityFileId);
    }

    @Transactional(readOnly = true)
    public IdentityFileDownload downloadStudentIdentityFileByStudentIdAndIdentityFileId(Long studentId,
                                                                                         Long identityFileId) {
        Student student = requireStudentById(studentId);
        return downloadIdentityFileForStudentById(student, identityFileId);
    }

    private Student requireCurrentStudent(HttpServletRequest request) {
        User user = authSessionService.requireAuthenticatedUser(request);
        if (user.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: student role required.");
        }
        return studentRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found."));
    }

    private Student requireStudentById(Long studentId) {
        if (studentId == null || studentId.longValue() <= 0L) {
            throw new IllegalArgumentException("studentId must be positive");
        }
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentId));
    }

    private StudentProfile requireProfileForStudent(Student student) {
        return studentProfileRepository.findByStudent_Id(student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found."));
    }

    private StudentSchoolRecord requireOwnedSchoolRecord(Student student, Long schoolRecordId) {
        if (schoolRecordId == null || schoolRecordId.longValue() <= 0L) {
            throw new IllegalArgumentException("schoolRecordId must be positive");
        }

        StudentSchoolRecord schoolRecord = studentSchoolRecordRepository.findById(schoolRecordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School record not found."));
        if (!schoolRecord.getStudent().getId().equals(student.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Forbidden: school record does not belong to current student."
            );
        }
        return schoolRecord;
    }

    private StudentProfileDto getProfileForStudent(Student student, boolean includeTeacherNote) {
        StudentProfile profile = studentProfileRepository.findByStudent_Id(student.getId()).orElse(null);
        List<StudentSchoolRecord> schools = studentSchoolRecordRepository.findByStudent_IdOrderByIdAsc(student.getId());
        List<StudentSchoolTranscript> transcripts = findTranscriptsBySchoolRecords(schools);
        List<StudentIdentityFile> identityFiles = profile == null
                ? Collections.<StudentIdentityFile>emptyList()
                : studentIdentityFileRepository.findByStudentProfile_IdOrderByUploadedAtDescIdDesc(profile.getId());
        List<StudentCourseRecord> courses = studentCourseRecordRepository.findByStudent_IdOrderByIdAsc(student.getId());
        return toDto(student, profile, schools, transcripts, identityFiles, courses, includeTeacherNote);
    }

    private StudentProfileDto saveProfileForStudent(Student student,
                                                    StudentProfileDto requestBody,
                                                    Long operatorUserId,
                                                    String traceId,
                                                    String changeSource,
                                                    Long expectedVersion,
                                                    boolean teacherNoteProvided,
                                                    String teacherNoteToSave,
                                                    boolean includeTeacherNoteInResponse) {
        NormalizedProfile normalized = normalizeAndValidate(requestBody);
        StudentProfileDto beforeSnapshot = getProfileForStudent(student, false);

        StudentProfile profile = studentProfileRepository.findByStudent_Id(student.getId())
                .orElseGet(() -> new StudentProfile(student));
        long currentVersion = safeProfileVersion(profile.getProfileVersion());
        ensureProfileVersionMatches(expectedVersion, currentVersion);
        applyProfile(profile, normalized, operatorUserId);
        if (teacherNoteProvided) {
            profile.setTeacherNote(teacherNoteToSave);
        }
        profile = studentProfileRepository.save(profile);

        List<StudentIdentityFile> existingIdentityFiles =
                studentIdentityFileRepository.findByStudentProfile_IdOrderByUploadedAtDescIdDesc(profile.getId());
        List<StudentIdentityFile> savedIdentityFiles = syncIdentityFiles(
                profile,
                normalized.identityFiles,
                existingIdentityFiles,
                operatorUserId,
                traceId
        );

        student.updateProfileNames(
                normalized.legalFirstName,
                normalized.legalLastName,
                normalized.preferredName
        );
        studentRepository.save(student);

        List<StudentSchoolRecord> existingSchoolRecords = studentSchoolRecordRepository.findByStudent_IdOrderByIdAsc(student.getId());
        List<StudentSchoolTranscript> existingTranscripts = findTranscriptsBySchoolRecords(existingSchoolRecords);
        List<StudentSchoolRecord> savedSchools = existingSchoolRecords;
        List<StudentSchoolTranscript> savedTranscripts = existingTranscripts;
        if (normalized.schools != null) {
            SchoolSyncResult schoolSyncResult = syncSchoolsForStudent(
                    student,
                    normalized.schools,
                    existingSchoolRecords,
                    existingTranscripts,
                    operatorUserId,
                    traceId
            );
            savedSchools = schoolSyncResult.schools;
            savedTranscripts = schoolSyncResult.transcripts;
        }

        studentCourseRecordRepository.deleteByStudent_Id(student.getId());
        List<StudentCourseRecord> savedCourses = new ArrayList<StudentCourseRecord>();
        for (NormalizedCourse course : normalized.otherCourses) {
            // Keep school_type populated for backward DB compatibility.
            savedCourses.add(new StudentCourseRecord(
                    student,
                    SchoolType.OTHER,
                    course.schoolName,
                    course.streetAddress,
                    course.city,
                    course.state,
                    course.country,
                    course.postal,
                    course.courseCode,
                    course.mark,
                    course.gradeLevel,
                    course.startTime,
                    course.endTime
            ));
        }
        if (!savedCourses.isEmpty()) {
            savedCourses = studentCourseRecordRepository.saveAll(savedCourses);
        }

        StudentProfileDto savedDto = toDto(
                student,
                profile,
                savedSchools,
                savedTranscripts,
                savedIdentityFiles,
                savedCourses,
                includeTeacherNoteInResponse
        );
        recordProfileHistoryIfChanged(
                student,
                profile,
                beforeSnapshot,
                savedDto,
                operatorUserId,
                traceId,
                changeSource
        );
        return savedDto;
    }

    private void applyProfile(StudentProfile profile, NormalizedProfile normalized, Long operatorUserId) {
        profile.setGender(normalized.gender);
        profile.setGenderOther(normalized.genderOther);
        profile.setBirthday(normalized.birthday);
        profile.setStatusInCanada(normalized.statusInCanada);
        profile.setPhone(normalized.phone);
        profile.setEmail(normalized.email);
        profile.setCitizenship(normalized.citizenship);
        profile.setFirstLanguage(normalized.firstLanguage);
        profile.setFirstBoardingDate(normalized.firstBoardingDate);
        profile.setStudentRegion(normalized.studentRegion);
        profile.setOenNumber(normalized.oenNumber);
        profile.setPenNumber(normalized.penNumber);
        profile.setIb(normalized.ib);
        profile.setAp(normalized.ap);
        profile.setStreetAddress(normalized.address.streetAddress);
        profile.setStreetAddressLine2(normalized.address.streetAddressLine2);
        profile.setCity(normalized.address.city);
        profile.setState(normalized.address.state);
        profile.setCountry(normalized.address.country);
        profile.setPostal(normalized.address.postal);
        if (normalized.serviceItems != null) {
            profile.setServiceItems(normalized.serviceItems);
        }
        profile.setUpdatedBy(operatorUserId);
    }

    private StudentSchoolTranscriptDto uploadSchoolTranscriptForStudent(Student student,
                                                                        Long schoolRecordId,
                                                                        MultipartFile file,
                                                                        String academicRecordType,
                                                                        Integer reportYear,
                                                                        String reportMonth,
                                                                        Long uploadedBy,
                                                                        String traceId,
                                                                        String changeSource) {
        if (schoolRecordId == null || schoolRecordId.longValue() <= 0L) {
            throw new IllegalArgumentException("schoolRecordId must be positive");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("transcript file is required");
        }
        assertUploadSizeWithinLimit(file);
        assertPdfUpload(file);

        NormalizedAcademicUploadMetadata academicUploadMetadata = normalizeAcademicUploadMetadata(
                academicRecordType,
                reportYear,
                reportMonth
        );

        StudentProfile profile = studentProfileRepository.findByStudent_Id(student.getId())
                .orElseGet(() -> {
                    StudentProfile created = new StudentProfile(student);
                    created.setUpdatedBy(uploadedBy);
                    created.setProfileVersion(0L);
                    return studentProfileRepository.save(created);
                });
        StudentProfileDto beforeSnapshot = getProfileForStudent(student, false);
        StudentSchoolRecord school = requireOwnedSchoolRecord(student, schoolRecordId);

        LocalDateTime now = LocalDateTime.now();
        StudentSchoolTranscriptStorageService.StoredTranscript stored =
                transcriptStorageService.store(student.getId(), school.getId(), file);

        StudentSchoolTranscript transcript = new StudentSchoolTranscript(
                school,
                stored.getStorageKey(),
                stored.getOriginalFilename(),
                stored.getContentType(),
                Long.valueOf(stored.getSizeBytes()),
                now,
                uploadedBy
        );
        transcript = studentSchoolTranscriptRepository.save(transcript);

        studentDocumentService.createLinkedAcademicDocument(
                student,
                school,
                transcript,
                file,
                academicUploadMetadata.academicRecordType,
                academicUploadMetadata.reportYear,
                academicUploadMetadata.reportMonth,
                uploadedBy
        );

        school.setTranscriptOriginalFilename(stored.getOriginalFilename());
        school.setTranscriptContentType(stored.getContentType());
        school.setTranscriptStorageKey(stored.getStorageKey());
        school.setTranscriptSizeBytes(Long.valueOf(stored.getSizeBytes()));
        school.setTranscriptUploadedAt(now);
        school = studentSchoolRecordRepository.save(school);

        List<StudentSchoolTranscript> transcripts =
                studentSchoolTranscriptRepository.findBySchoolRecord_IdOrderByUploadedAtDescIdDesc(school.getId());
        log.info(
                "Transcript appended. traceId={}, userId={}, schoolRecordId={}, transcriptId={}",
                safeTraceId(traceId),
                uploadedBy,
                school.getId(),
                transcript.getId()
        );
        StudentProfileDto afterSnapshot = getProfileForStudent(student, false);
        recordProfileHistoryIfChanged(
                student,
                profile,
                beforeSnapshot,
                afterSnapshot,
                uploadedBy,
                traceId,
                changeSource
        );

        return toTranscriptDto(
                school,
                transcripts,
                academicUploadMetadata.academicRecordType,
                academicUploadMetadata.reportYear,
                academicUploadMetadata.reportMonth
        );
    }

    private SchoolTranscriptDownload downloadSchoolTranscriptForStudent(Student student, Long schoolRecordId) {
        if (schoolRecordId == null || schoolRecordId.longValue() <= 0L) {
            throw new IllegalArgumentException("schoolRecordId must be positive");
        }

        StudentSchoolRecord school = requireOwnedSchoolRecord(student, schoolRecordId);
        List<StudentSchoolTranscript> transcripts =
                studentSchoolTranscriptRepository.findBySchoolRecord_IdOrderByUploadedAtDescIdDesc(school.getId());
        if (!transcripts.isEmpty()) {
            return toDownload(transcripts.get(0));
        }
        String legacyStorageKey = trimToNull(school.getTranscriptStorageKey());
        if (legacyStorageKey != null) {
            return legacySchoolDownload(school);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transcript file not found.");
    }

    private SchoolTranscriptDownload downloadSchoolTranscriptForStudentByTranscriptId(Student student,
                                                                                       Long schoolRecordId,
                                                                                       Long transcriptId) {
        if (schoolRecordId == null || schoolRecordId.longValue() <= 0L) {
            throw new IllegalArgumentException("schoolRecordId must be positive");
        }
        if (transcriptId == null || transcriptId.longValue() <= 0L) {
            throw new IllegalArgumentException("transcriptId must be positive");
        }

        StudentSchoolRecord school = requireOwnedSchoolRecord(student, schoolRecordId);
        StudentSchoolTranscript transcript = studentSchoolTranscriptRepository
                .findByIdAndSchoolRecord_Id(transcriptId, school.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transcript file not found."));
        return toDownload(transcript);
    }

    private StudentSchoolTranscriptDto toTranscriptDto(StudentSchoolRecord school,
                                                       List<StudentSchoolTranscript> transcripts,
                                                       String academicRecordType,
                                                       Integer reportYear,
                                                       String reportMonth) {
        StudentSchoolTranscriptDto dto = new StudentSchoolTranscriptDto();
        dto.setSchoolRecordId(school.getId());
        dto.setAcademicRecordType(academicRecordType);
        dto.setReportYear(reportYear);
        dto.setReportMonth(reportMonth);
        List<StudentSchoolTranscript> sorted = sortTranscriptsLatestFirst(transcripts);
        List<StudentSchoolTranscriptDto.TranscriptItemDto> transcriptItems =
                new ArrayList<StudentSchoolTranscriptDto.TranscriptItemDto>();
        for (StudentSchoolTranscript transcript : sorted) {
            transcriptItems.add(toTranscriptItemDto(transcript));
        }
        dto.setTranscripts(transcriptItems);

        if (!sorted.isEmpty()) {
            StudentSchoolTranscript latest = sorted.get(0);
            dto.setTranscriptFileName(latest.getOriginalFilename());
            dto.setTranscriptContentType(latest.getMimeType());
            dto.setTranscriptSizeBytes(latest.getSizeBytes());
            dto.setTranscriptUploadedAt(formatDateTime(latest.getUploadedAt()));
            dto.setHasTranscript(Boolean.TRUE);
        } else {
            dto.setTranscriptFileName(null);
            dto.setTranscriptContentType(null);
            dto.setTranscriptSizeBytes(null);
            dto.setTranscriptUploadedAt(null);
            dto.setHasTranscript(Boolean.FALSE);
        }
        return dto;
    }

    private StudentIdentityFileUploadDto uploadIdentityFileForStudent(Student student,
                                                                       MultipartFile file,
                                                                       String identityDocumentType,
                                                                       Long uploadedBy,
                                                                       String traceId,
                                                                       String changeSource) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("identity file is required");
        }
        assertUploadSizeWithinLimit(file);
        assertPdfUpload(file);

        StudentProfileDto beforeSnapshot = getProfileForStudent(student, false);
        StudentProfile profile = studentProfileRepository.findByStudent_Id(student.getId())
                .orElseGet(() -> {
                    StudentProfile created = new StudentProfile(student);
                    created.setUpdatedBy(uploadedBy);
                    created.setProfileVersion(0L);
                    return studentProfileRepository.save(created);
                });

        LocalDateTime now = LocalDateTime.now();
        StudentIdentityFileStorageService.StoredIdentityFile stored = identityFileStorageService.store(student.getId(), file);
        StudentIdentityFile identityFile = new StudentIdentityFile(
                profile,
                stored.getStorageKey(),
                stored.getOriginalFilename(),
                stored.getContentType(),
                Long.valueOf(stored.getSizeBytes()),
                now,
                uploadedBy
        );
        identityFile = studentIdentityFileRepository.save(identityFile);

        studentDocumentService.createLinkedIdentityDocument(
                student,
                identityFile,
                file,
                identityDocumentType,
                uploadedBy
        );

        List<StudentIdentityFile> identityFiles =
                studentIdentityFileRepository.findByStudentProfile_IdOrderByUploadedAtDescIdDesc(profile.getId());
        log.info(
                "Identity file appended. traceId={}, userId={}, profileId={}, identityFileId={}",
                safeTraceId(traceId),
                uploadedBy,
                profile.getId(),
                identityFile.getId()
        );
        StudentProfileDto afterSnapshot = getProfileForStudent(student, false);
        recordProfileHistoryIfChanged(
                student,
                profile,
                beforeSnapshot,
                afterSnapshot,
                uploadedBy,
                traceId,
                changeSource
        );

        return toIdentityFileUploadDto(identityFiles);
    }

    private IdentityFileDownload downloadIdentityFileForStudentById(Student student, Long identityFileId) {
        if (identityFileId == null || identityFileId.longValue() <= 0L) {
            throw new IllegalArgumentException("identityFileId must be positive");
        }

        StudentProfile profile = requireProfileForStudent(student);
        StudentIdentityFile identityFile = studentIdentityFileRepository
                .findByIdAndStudentProfile_Id(identityFileId, profile.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Identity file not found."));
        return toIdentityDownload(identityFile);
    }

    private StudentIdentityFileUploadDto toIdentityFileUploadDto(List<StudentIdentityFile> identityFiles) {
        StudentIdentityFileUploadDto dto = new StudentIdentityFileUploadDto();
        List<StudentIdentityFile> sorted = sortIdentityFilesLatestFirst(identityFiles);
        List<StudentIdentityFileUploadDto.IdentityFileItemDto> items =
                new ArrayList<StudentIdentityFileUploadDto.IdentityFileItemDto>();
        for (StudentIdentityFile identityFile : sorted) {
            items.add(toIdentityUploadItemDto(identityFile));
        }
        dto.setIdentityFiles(items);

        if (!sorted.isEmpty()) {
            StudentIdentityFile latest = sorted.get(0);
            dto.setIdentityFileId(latest.getId());
            dto.setIdentityFileName(latest.getOriginalFilename());
            dto.setIdentityFileContentType(latest.getMimeType());
            dto.setIdentityFileSizeBytes(latest.getSizeBytes());
            dto.setIdentityFileUploadedAt(formatDateTime(latest.getUploadedAt()));
            dto.setHasIdentityFile(Boolean.TRUE);
        } else {
            dto.setIdentityFileId(null);
            dto.setIdentityFileName(null);
            dto.setIdentityFileContentType(null);
            dto.setIdentityFileSizeBytes(null);
            dto.setIdentityFileUploadedAt(null);
            dto.setHasIdentityFile(Boolean.FALSE);
        }
        return dto;
    }

    private StudentProfileDto toDto(Student student,
                                    StudentProfile profile,
                                    List<StudentSchoolRecord> schools,
                                    List<StudentSchoolTranscript> transcripts,
                                    List<StudentIdentityFile> identityFiles,
                                    List<StudentCourseRecord> courses,
                                    boolean includeTeacherNote) {
        StudentProfileDto dto = includeTeacherNote ? new TeacherStudentProfileDto() : new StudentProfileDto();

        dto.setLegalFirstName(student.getFirstName());
        dto.setLegalLastName(student.getLastName());
        dto.setPreferredName(student.getNickName());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setNickName(student.getNickName());
        dto.setVersion(profile == null ? Long.valueOf(0L) : Long.valueOf(safeProfileVersion(profile.getProfileVersion())));
        dto.setAp(Boolean.FALSE);
        dto.setStudentRegion(STUDENT_REGION_ONTARIO);

        if (profile != null) {
            NormalizedGender normalizedGender = normalizeGenderFields(
                    profile.getGender(),
                    profile.getGenderOther(),
                    false
            );
            dto.setGender(normalizedGender.gender);
            dto.setGenderOther(normalizedGender.genderOther);
            dto.setBirthday(formatDate(profile.getBirthday()));
            dto.setPhone(profile.getPhone());
            dto.setEmail(profile.getEmail());
            dto.setStatusInCanada(profile.getStatusInCanada());
            dto.setCitizenship(profile.getCitizenship());
            dto.setFirstLanguage(profile.getFirstLanguage());
            dto.setFirstBoardingDate(formatDate(profile.getFirstBoardingDate()));
            StudentRegionSnapshot regionSnapshot = resolveStudentRegionSnapshotFromStored(
                    profile.getStudentRegion(),
                    profile.getOenNumber(),
                    profile.getPenNumber()
            );
            dto.setStudentRegion(regionSnapshot.studentRegion);
            dto.setOenNumber(regionSnapshot.oenNumber);
            dto.setPenNumber(regionSnapshot.penNumber);
            dto.setIb(profile.getIb());
            dto.setAp(profile.isAp());
            StudentProfileDto.AddressDto address = new StudentProfileDto.AddressDto();
            address.setStreetAddress(profile.getStreetAddress());
            address.setStreetAddressLine2(profile.getStreetAddressLine2());
            address.setCity(profile.getCity());
            address.setState(profile.getState());
            address.setCountry(profile.getCountry());
            address.setPostal(profile.getPostal());
            dto.setAddress(address);
        }
        List<String> serviceItems = StudentServiceItemNormalizer.normalizeStored(
                profile == null ? null : profile.getServiceItems()
        );
        dto.setServiceItems(serviceItems);
        dto.setServiceProjects(new ArrayList<String>(serviceItems));
        if (includeTeacherNote && dto instanceof TeacherStudentProfileDto) {
            String note = profile == null ? null : profile.getTeacherNote();
            ((TeacherStudentProfileDto) dto).setTeacherNote(note);
        }

        List<StudentProfileDto.IdentityFileDto> identityFileDtos = new ArrayList<StudentProfileDto.IdentityFileDto>();
        List<StudentIdentityFile> sortedIdentityFiles = sortIdentityFilesLatestFirst(identityFiles);
        for (StudentIdentityFile identityFile : sortedIdentityFiles) {
            identityFileDtos.add(toProfileIdentityFileDto(identityFile));
        }
        dto.setIdentityFiles(identityFileDtos);

        List<StudentProfileDto.SchoolDto> schoolDtos = new ArrayList<StudentProfileDto.SchoolDto>();
        Map<Long, List<StudentSchoolTranscript>> transcriptsBySchoolId = new HashMap<Long, List<StudentSchoolTranscript>>();
        if (transcripts != null) {
            for (StudentSchoolTranscript transcript : transcripts) {
                Long schoolId = transcript.getSchoolRecord().getId();
                List<StudentSchoolTranscript> list = transcriptsBySchoolId.get(schoolId);
                if (list == null) {
                    list = new ArrayList<StudentSchoolTranscript>();
                    transcriptsBySchoolId.put(schoolId, list);
                }
                list.add(transcript);
            }
        }
        if (schools != null) {
            for (StudentSchoolRecord school : schools) {
                StudentProfileDto.SchoolDto schoolDto = new StudentProfileDto.SchoolDto();
                schoolDto.setSchoolRecordId(school.getId());
                schoolDto.setSchoolType(school.getSchoolType() == null ? null : school.getSchoolType().name());
                schoolDto.setSchoolName(school.getSchoolName());
                String schoolBoard = trimToNull(school.getSchoolBoard());
                schoolDto.setSchoolBoard(schoolBoard);
                schoolDto.setBoardName(schoolBoard);
                StudentProfileDto.AddressDto schoolAddress = new StudentProfileDto.AddressDto();
                schoolAddress.setStreetAddress(school.getStreetAddress());
                schoolAddress.setCity(school.getCity());
                schoolAddress.setState(school.getState());
                schoolAddress.setCountry(school.getCountry());
                schoolAddress.setPostal(school.getPostal());
                schoolDto.setAddress(schoolAddress);
                schoolDto.setStreetAddress(school.getStreetAddress());
                schoolDto.setCity(school.getCity());
                schoolDto.setState(school.getState());
                schoolDto.setCountry(school.getCountry());
                schoolDto.setPostal(school.getPostal());
                schoolDto.setStartTime(formatDate(school.getStartTime()));
                schoolDto.setEndTime(formatDate(school.getEndTime()));
                List<StudentSchoolTranscript> schoolTranscripts = sortTranscriptsLatestFirst(
                        transcriptsBySchoolId.get(school.getId())
                );
                List<StudentProfileDto.TranscriptDto> transcriptDtos = new ArrayList<StudentProfileDto.TranscriptDto>();
                for (StudentSchoolTranscript transcript : schoolTranscripts) {
                    transcriptDtos.add(toProfileTranscriptDto(transcript));
                }
                schoolDto.setTranscripts(transcriptDtos);

                if (!schoolTranscripts.isEmpty()) {
                    StudentSchoolTranscript latest = schoolTranscripts.get(0);
                    schoolDto.setTranscriptFileName(latest.getOriginalFilename());
                    schoolDto.setTranscriptSizeBytes(latest.getSizeBytes());
                    schoolDto.setTranscriptUploadedAt(formatDateTime(latest.getUploadedAt()));
                    schoolDto.setHasTranscript(Boolean.TRUE);
                } else {
                    schoolDto.setTranscriptFileName(school.getTranscriptOriginalFilename());
                    schoolDto.setTranscriptSizeBytes(school.getTranscriptSizeBytes());
                    schoolDto.setTranscriptUploadedAt(formatDateTime(school.getTranscriptUploadedAt()));
                    schoolDto.setHasTranscript(Boolean.valueOf(trimToNull(school.getTranscriptStorageKey()) != null));
                }
                schoolDtos.add(schoolDto);
            }
        }
        dto.setSchools(schoolDtos);
        dto.setSchoolRecords(new ArrayList<StudentProfileDto.SchoolDto>(schoolDtos));

        List<StudentProfileDto.CourseDto> courseDtos = new ArrayList<StudentProfileDto.CourseDto>();
        if (courses != null) {
            for (StudentCourseRecord course : courses) {
                StudentProfileDto.CourseDto courseDto = new StudentProfileDto.CourseDto();
                courseDto.setSchoolName(course.getSchoolName());
                StudentProfileDto.AddressDto courseAddress = new StudentProfileDto.AddressDto();
                courseAddress.setStreetAddress(course.getStreetAddress());
                courseAddress.setCity(course.getCity());
                courseAddress.setState(course.getState());
                courseAddress.setCountry(course.getCountry());
                courseAddress.setPostal(course.getPostal());
                courseDto.setAddress(courseAddress);
                courseDto.setStreetAddress(course.getStreetAddress());
                courseDto.setCity(course.getCity());
                courseDto.setState(course.getState());
                courseDto.setCountry(course.getCountry());
                courseDto.setPostal(course.getPostal());
                courseDto.setCourseCode(course.getCourseCode());
                courseDto.setMark(course.getMark());
                courseDto.setGradeLevel(course.getGradeLevel());
                courseDto.setStartTime(formatDate(course.getStartTime()));
                courseDto.setEndTime(formatDate(course.getEndTime()));
                courseDtos.add(courseDto);
            }
        }
        dto.setOtherCourses(courseDtos);
        dto.setExternalCourses(new ArrayList<StudentProfileDto.CourseDto>(courseDtos));

        return dto;
    }

    private TeacherStudentProfileDto toTeacherDto(StudentProfileDto dto) {
        if (dto instanceof TeacherStudentProfileDto) {
            return (TeacherStudentProfileDto) dto;
        }
        throw new IllegalStateException("Teacher profile DTO expected.");
    }

    private List<StudentSchoolTranscript> findTranscriptsBySchoolRecords(List<StudentSchoolRecord> schools) {
        if (schools == null || schools.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> schoolRecordIds = new ArrayList<Long>();
        for (StudentSchoolRecord school : schools) {
            schoolRecordIds.add(school.getId());
        }
        return studentSchoolTranscriptRepository
                .findBySchoolRecord_IdInOrderBySchoolRecord_IdAscUploadedAtDescIdDesc(schoolRecordIds);
    }

    private Map<String, List<StudentSchoolRecord>> mapSchoolRecordsByKey(List<StudentSchoolRecord> schools) {
        Map<String, List<StudentSchoolRecord>> byKey = new LinkedHashMap<String, List<StudentSchoolRecord>>();
        if (schools == null) {
            return byKey;
        }
        for (StudentSchoolRecord school : schools) {
            String key = buildSchoolKey(school.getSchoolType(), school.getSchoolName(), school.getStartTime(), school.getEndTime());
            List<StudentSchoolRecord> records = byKey.get(key);
            if (records == null) {
                records = new ArrayList<StudentSchoolRecord>();
                byKey.put(key, records);
            }
            records.add(school);
        }
        return byKey;
    }

    private Map<Long, StudentSchoolRecord> mapSchoolRecordsById(List<StudentSchoolRecord> schools) {
        Map<Long, StudentSchoolRecord> byId = new LinkedHashMap<Long, StudentSchoolRecord>();
        if (schools == null) {
            return byId;
        }
        for (StudentSchoolRecord school : schools) {
            if (school.getId() == null) {
                continue;
            }
            byId.put(school.getId(), school);
        }
        return byId;
    }

    private StudentSchoolRecord popSchoolRecordByKey(Map<String, List<StudentSchoolRecord>> schoolsByKey, String key) {
        if (schoolsByKey == null || key == null) {
            return null;
        }
        List<StudentSchoolRecord> records = schoolsByKey.get(key);
        if (records == null || records.isEmpty()) {
            return null;
        }
        StudentSchoolRecord matched = records.remove(0);
        if (records.isEmpty()) {
            schoolsByKey.remove(key);
        }
        return matched;
    }

    private void removeSchoolRecordFromKeyMap(Map<String, List<StudentSchoolRecord>> schoolsByKey,
                                              StudentSchoolRecord school) {
        if (schoolsByKey == null || school == null) {
            return;
        }
        String key = buildSchoolKey(
                school.getSchoolType(),
                school.getSchoolName(),
                school.getStartTime(),
                school.getEndTime()
        );
        List<StudentSchoolRecord> records = schoolsByKey.get(key);
        if (records == null || records.isEmpty()) {
            return;
        }
        records.remove(school);
        if (records.isEmpty()) {
            schoolsByKey.remove(key);
        }
    }

    private String resolveSchoolBoardForSave(NormalizedSchool incomingSchool, StudentSchoolRecord existingSchoolRecord) {
        if (incomingSchool.schoolBoardProvided) {
            return incomingSchool.schoolBoard;
        }
        if (existingSchoolRecord == null) {
            return null;
        }
        return trimToNull(existingSchoolRecord.getSchoolBoard());
    }

    private SchoolSyncResult syncSchoolsForStudent(Student student,
                                                   List<NormalizedSchool> incomingSchools,
                                                   List<StudentSchoolRecord> existingSchoolRecords,
                                                   List<StudentSchoolTranscript> existingTranscripts,
                                                   Long operatorUserId,
                                                   String traceId) {
        Map<Long, StudentSchoolRecord> existingById = mapSchoolRecordsById(existingSchoolRecords);
        Map<String, List<StudentSchoolRecord>> existingByKey = mapSchoolRecordsByKey(existingSchoolRecords);
        Map<Long, List<StudentSchoolTranscript>> transcriptsBySchoolId =
                mapTranscriptsBySchoolId(existingSchoolRecords, existingTranscripts);

        List<StudentSchoolRecord> schoolsToSave = new ArrayList<StudentSchoolRecord>();
        List<SchoolSyncPlan> syncPlans = new ArrayList<SchoolSyncPlan>();
        for (int i = 0; i < incomingSchools.size(); i++) {
            NormalizedSchool incomingSchool = incomingSchools.get(i);
            String pathPrefix = "schools[" + i + "]";
            StudentSchoolRecord existingSchool = resolveExistingSchoolRecord(
                    pathPrefix,
                    incomingSchool,
                    existingById,
                    existingByKey
            );
            String resolvedSchoolBoard = resolveSchoolBoardForSave(incomingSchool, existingSchool);
            StudentSchoolRecord schoolToSave;
            if (existingSchool == null) {
                schoolToSave = new StudentSchoolRecord(
                        student,
                        incomingSchool.schoolType,
                        incomingSchool.schoolName,
                        resolvedSchoolBoard,
                        incomingSchool.streetAddress,
                        incomingSchool.city,
                        incomingSchool.state,
                        incomingSchool.country,
                        incomingSchool.postal,
                        incomingSchool.startTime,
                        incomingSchool.endTime
                );
            } else {
                schoolToSave = existingSchool;
                applySchoolRecordUpdate(schoolToSave, incomingSchool, resolvedSchoolBoard);
            }

            schoolsToSave.add(schoolToSave);
            List<StudentSchoolTranscript> legacyTranscripts = existingSchool == null
                    ? Collections.<StudentSchoolTranscript>emptyList()
                    : popTranscriptsBySchoolId(transcriptsBySchoolId, existingSchool.getId());
            syncPlans.add(new SchoolSyncPlan(schoolToSave, incomingSchool, legacyTranscripts));
        }

        List<StudentSchoolRecord> schoolsToDelete = new ArrayList<StudentSchoolRecord>(existingById.values());
        for (StudentSchoolRecord schoolToDelete : schoolsToDelete) {
            List<StudentSchoolTranscript> legacyTranscripts = popTranscriptsBySchoolId(transcriptsBySchoolId, schoolToDelete.getId());
            for (StudentSchoolTranscript transcript : legacyTranscripts) {
                deleteTranscriptStorageOrThrow(transcript, operatorUserId, traceId, "school_removed");
            }
        }
        for (Map.Entry<Long, List<StudentSchoolTranscript>> orphanEntry : transcriptsBySchoolId.entrySet()) {
            for (StudentSchoolTranscript transcript : orphanEntry.getValue()) {
                deleteTranscriptStorageOrThrow(transcript, operatorUserId, traceId, "school_orphaned");
            }
        }

        if (!schoolsToDelete.isEmpty()) {
            studentSchoolRecordRepository.deleteAll(schoolsToDelete);
            studentSchoolRecordRepository.flush();
        }

        if (!schoolsToSave.isEmpty()) {
            studentSchoolRecordRepository.saveAll(schoolsToSave);
            studentSchoolRecordRepository.flush();
        }

        List<StudentSchoolTranscript> savedTranscripts = new ArrayList<StudentSchoolTranscript>();
        for (SchoolSyncPlan syncPlan : syncPlans) {
            List<StudentSchoolTranscript> syncedTranscripts = syncSchoolTranscripts(
                    syncPlan.school,
                    syncPlan.normalizedSchool,
                    syncPlan.legacyTranscripts,
                    operatorUserId,
                    traceId
            );
            applyLegacyTranscriptFields(syncPlan.school, syncedTranscripts);
            savedTranscripts.addAll(syncedTranscripts);
        }

        if (!schoolsToSave.isEmpty()) {
            studentSchoolRecordRepository.saveAll(schoolsToSave);
            studentSchoolRecordRepository.flush();
        }
        return new SchoolSyncResult(schoolsToSave, savedTranscripts);
    }

    private StudentSchoolRecord resolveExistingSchoolRecord(String pathPrefix,
                                                            NormalizedSchool incomingSchool,
                                                            Map<Long, StudentSchoolRecord> existingById,
                                                            Map<String, List<StudentSchoolRecord>> existingByKey) {
        if (incomingSchool.schoolRecordId != null) {
            StudentSchoolRecord byId = existingById.remove(incomingSchool.schoolRecordId);
            if (byId == null) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        pathPrefix + ".schoolRecordId does not belong to current student."
                );
            }
            removeSchoolRecordFromKeyMap(existingByKey, byId);
            return byId;
        }

        String schoolKey = buildSchoolKey(
                incomingSchool.schoolType,
                incomingSchool.schoolName,
                incomingSchool.startTime,
                incomingSchool.endTime
        );
        StudentSchoolRecord byKey = popSchoolRecordByKey(existingByKey, schoolKey);
        if (byKey != null) {
            existingById.remove(byKey.getId());
        }
        return byKey;
    }

    private void applySchoolRecordUpdate(StudentSchoolRecord school,
                                         NormalizedSchool incomingSchool,
                                         String resolvedSchoolBoard) {
        school.setSchoolType(incomingSchool.schoolType);
        school.setSchoolName(incomingSchool.schoolName);
        school.setSchoolBoard(resolvedSchoolBoard);
        school.setStreetAddress(incomingSchool.streetAddress);
        school.setCity(incomingSchool.city);
        school.setState(incomingSchool.state);
        school.setCountry(incomingSchool.country);
        school.setPostal(incomingSchool.postal);
        school.setStartTime(incomingSchool.startTime);
        school.setEndTime(incomingSchool.endTime);
    }

    private Map<Long, List<StudentSchoolTranscript>> mapTranscriptsBySchoolId(List<StudentSchoolRecord> schools,
                                                                               List<StudentSchoolTranscript> transcripts) {
        Map<Long, List<StudentSchoolTranscript>> bySchoolId = new LinkedHashMap<Long, List<StudentSchoolTranscript>>();
        if (transcripts != null) {
            for (StudentSchoolTranscript transcript : transcripts) {
                if (transcript == null
                        || transcript.getSchoolRecord() == null
                        || transcript.getSchoolRecord().getId() == null) {
                    continue;
                }
                Long schoolId = transcript.getSchoolRecord().getId();
                List<StudentSchoolTranscript> list = bySchoolId.get(schoolId);
                if (list == null) {
                    list = new ArrayList<StudentSchoolTranscript>();
                    bySchoolId.put(schoolId, list);
                }
                list.add(transcript);
            }
        }

        if (schools == null) {
            return bySchoolId;
        }
        for (StudentSchoolRecord school : schools) {
            if (school == null || school.getId() == null || bySchoolId.containsKey(school.getId())) {
                continue;
            }
            String storageKey = trimToNull(school.getTranscriptStorageKey());
            if (storageKey == null) {
                continue;
            }
            String fileName = trimToNull(school.getTranscriptOriginalFilename());
            if (fileName == null) {
                fileName = "transcript.bin";
            }
            String mimeType = trimToNull(school.getTranscriptContentType());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            Long sizeBytes = school.getTranscriptSizeBytes();
            if (sizeBytes == null || sizeBytes.longValue() < 0L) {
                sizeBytes = Long.valueOf(0L);
            }
            LocalDateTime uploadedAt = school.getTranscriptUploadedAt();
            if (uploadedAt == null) {
                uploadedAt = school.getUpdatedAt() == null ? LocalDateTime.now() : school.getUpdatedAt();
            }
            Long uploadedBy = school.getStudent() == null || school.getStudent().getUser() == null
                    ? Long.valueOf(0L)
                    : school.getStudent().getUser().getId();
            List<StudentSchoolTranscript> list = new ArrayList<StudentSchoolTranscript>();
            list.add(new StudentSchoolTranscript(
                    school,
                    storageKey,
                    fileName,
                    mimeType,
                    sizeBytes,
                    uploadedAt,
                    uploadedBy
            ));
            bySchoolId.put(school.getId(), list);
        }
        return bySchoolId;
    }

    private List<StudentSchoolTranscript> popTranscriptsBySchoolId(Map<Long, List<StudentSchoolTranscript>> transcriptsBySchoolId,
                                                                   Long schoolId) {
        if (transcriptsBySchoolId == null || schoolId == null) {
            return Collections.emptyList();
        }
        List<StudentSchoolTranscript> transcripts = transcriptsBySchoolId.remove(schoolId);
        if (transcripts == null || transcripts.isEmpty()) {
            return Collections.emptyList();
        }
        return transcripts;
    }

    private List<StudentSchoolTranscript> syncSchoolTranscripts(StudentSchoolRecord school,
                                                                NormalizedSchool normalizedSchool,
                                                                List<StudentSchoolTranscript> legacyTranscripts,
                                                                Long operatorUserId,
                                                                String traceId) {
        if (normalizedSchool.transcripts == null) {
            List<StudentSchoolTranscript> retained = sortTranscriptsLatestFirst(legacyTranscripts);
            if (retained.isEmpty()) {
                return Collections.emptyList();
            }
            for (StudentSchoolTranscript transcript : retained) {
                log.info(
                        "Transcript retained by PUT sync. traceId={}, userId={}, schoolRecordId={}, transcriptId={}",
                        safeTraceId(traceId),
                        operatorUserId,
                        school.getId(),
                        transcript.getId()
                );
            }
            return retained;
        }

        List<StudentSchoolTranscript> finalState = new ArrayList<StudentSchoolTranscript>();
        Map<Long, StudentSchoolTranscript> legacyById = new HashMap<Long, StudentSchoolTranscript>();
        for (StudentSchoolTranscript legacy : legacyTranscripts) {
            if (legacy.getId() != null) {
                legacyById.put(legacy.getId(), legacy);
            }
        }

        Set<Long> keptIds = new HashSet<Long>();
        for (NormalizedTranscript normalizedTranscript : normalizedSchool.transcripts) {
            if (normalizedTranscript.id != null) {
                StudentSchoolTranscript existing = legacyById.get(normalizedTranscript.id);
                if (existing != null) {
                    keptIds.add(existing.getId());
                    applyTranscriptOverride(existing, normalizedTranscript, operatorUserId);
                    finalState.add(existing);
                    continue;
                }
            }

            if (normalizedTranscript.storageKey == null) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Transcript entry cannot be inserted without storageKey."
                );
            }
            finalState.add(createTranscriptFromRequest(school, normalizedTranscript, operatorUserId));
        }

        for (StudentSchoolTranscript legacy : legacyTranscripts) {
            if (legacy.getId() != null && keptIds.contains(legacy.getId())) {
                continue;
            }
            deleteTranscriptStorageOrThrow(legacy, operatorUserId, traceId, "put_sync_removed");
            if (legacy.getId() != null) {
                studentDocumentService.deleteDocumentsLinkedToSchoolTranscript(legacy.getId());
                studentSchoolTranscriptRepository.delete(legacy);
            }
        }

        if (finalState.isEmpty()) {
            return Collections.emptyList();
        }
        List<StudentSchoolTranscript> persisted =
                sortTranscriptsLatestFirst(studentSchoolTranscriptRepository.saveAll(finalState));
        for (StudentSchoolTranscript transcript : persisted) {
            log.info(
                    "Transcript upserted by PUT sync. traceId={}, userId={}, schoolRecordId={}, transcriptId={}",
                    safeTraceId(traceId),
                    operatorUserId,
                    school.getId(),
                    transcript.getId()
            );
        }
        return persisted;
    }

    private StudentSchoolTranscript createTranscriptFromRequest(StudentSchoolRecord school,
                                                                NormalizedTranscript normalizedTranscript,
                                                                Long operatorUserId) {
        String fileName = trimToNull(normalizedTranscript.fileName);
        if (fileName == null) {
            fileName = "transcript.bin";
        }
        String contentType = trimToNull(normalizedTranscript.contentType);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        Long size = normalizedTranscript.sizeBytes == null ? Long.valueOf(0L) : normalizedTranscript.sizeBytes;
        LocalDateTime uploadedAt = normalizedTranscript.uploadedAt == null
                ? LocalDateTime.now()
                : normalizedTranscript.uploadedAt;
        Long uploadedBy = normalizedTranscript.uploadedBy == null
                ? operatorUserId
                : normalizedTranscript.uploadedBy;
        return new StudentSchoolTranscript(
                school,
                normalizedTranscript.storageKey,
                fileName,
                contentType,
                size,
                uploadedAt,
                uploadedBy
        );
    }

    private void applyTranscriptOverride(StudentSchoolTranscript existing,
                                        NormalizedTranscript override,
                                        Long operatorUserId) {
        String fileName = firstNonBlank(override.fileName, existing.getOriginalFilename());
        if (fileName == null) {
            fileName = "transcript.bin";
        }
        String contentType = firstNonBlank(override.contentType, existing.getMimeType());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        Long size = override.sizeBytes == null ? existing.getSizeBytes() : override.sizeBytes;
        if (size == null) {
            size = Long.valueOf(0L);
        }
        LocalDateTime uploadedAt = override.uploadedAt == null ? existing.getUploadedAt() : override.uploadedAt;
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
        Long uploadedBy = override.uploadedBy == null ? existing.getUploadedBy() : override.uploadedBy;
        if (uploadedBy == null) {
            uploadedBy = operatorUserId;
        }
        existing.setOriginalFilename(fileName);
        existing.setMimeType(contentType);
        existing.setSizeBytes(size);
        existing.setUploadedAt(uploadedAt);
        existing.setUploadedBy(uploadedBy);
    }

    private void applyLegacyTranscriptFields(StudentSchoolRecord school, List<StudentSchoolTranscript> transcripts) {
        List<StudentSchoolTranscript> sorted = sortTranscriptsLatestFirst(transcripts);
        if (sorted.isEmpty()) {
            school.setTranscriptOriginalFilename(null);
            school.setTranscriptContentType(null);
            school.setTranscriptStorageKey(null);
            school.setTranscriptSizeBytes(null);
            school.setTranscriptUploadedAt(null);
            return;
        }
        StudentSchoolTranscript latest = sorted.get(0);
        school.setTranscriptOriginalFilename(latest.getOriginalFilename());
        school.setTranscriptContentType(latest.getMimeType());
        school.setTranscriptStorageKey(latest.getStorageKey());
        school.setTranscriptSizeBytes(latest.getSizeBytes());
        school.setTranscriptUploadedAt(latest.getUploadedAt());
    }

    private List<StudentSchoolTranscript> sortTranscriptsLatestFirst(List<StudentSchoolTranscript> transcripts) {
        if (transcripts == null || transcripts.isEmpty()) {
            return Collections.emptyList();
        }
        List<StudentSchoolTranscript> sorted = new ArrayList<StudentSchoolTranscript>(transcripts);
        Collections.sort(sorted, (left, right) -> {
            LocalDateTime leftAt = left.getUploadedAt();
            LocalDateTime rightAt = right.getUploadedAt();
            if (leftAt == null && rightAt == null) {
                Long leftId = left.getId() == null ? Long.valueOf(0L) : left.getId();
                Long rightId = right.getId() == null ? Long.valueOf(0L) : right.getId();
                return rightId.compareTo(leftId);
            }
            if (leftAt == null) {
                return 1;
            }
            if (rightAt == null) {
                return -1;
            }
            int byTime = rightAt.compareTo(leftAt);
            if (byTime != 0) {
                return byTime;
            }
            Long leftId = left.getId() == null ? Long.valueOf(0L) : left.getId();
            Long rightId = right.getId() == null ? Long.valueOf(0L) : right.getId();
            return rightId.compareTo(leftId);
        });
        return sorted;
    }

    private void deleteTranscriptStorageOrThrow(StudentSchoolTranscript transcript,
                                                Long operatorUserId,
                                                String traceId,
                                                String reason) {
        try {
            transcriptStorageService.deleteRequired(transcript.getStorageKey());
            log.info(
                    "Transcript storage deleted. traceId={}, userId={}, schoolRecordId={}, transcriptId={}, reason={}",
                    safeTraceId(traceId),
                    operatorUserId,
                    transcript.getSchoolRecord().getId(),
                    transcript.getId(),
                    reason
            );
        } catch (RuntimeException ex) {
            log.error(
                    "Transcript storage delete failed. traceId={}, userId={}, schoolRecordId={}, transcriptId={}, reason={}",
                    safeTraceId(traceId),
                    operatorUserId,
                    transcript.getSchoolRecord().getId(),
                    transcript.getId(),
                    reason,
                    ex
            );
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Failed to delete transcript file.");
        }
    }

    private List<StudentIdentityFile> syncIdentityFiles(StudentProfile profile,
                                                        List<NormalizedIdentityFile> normalizedIdentityFiles,
                                                        List<StudentIdentityFile> legacyIdentityFiles,
                                                        Long operatorUserId,
                                                        String traceId) {
        if (normalizedIdentityFiles == null) {
            List<StudentIdentityFile> retained = sortIdentityFilesLatestFirst(legacyIdentityFiles);
            if (retained.isEmpty()) {
                return Collections.emptyList();
            }
            for (StudentIdentityFile identityFile : retained) {
                log.info(
                        "Identity file retained by PUT sync. traceId={}, userId={}, profileId={}, identityFileId={}",
                        safeTraceId(traceId),
                        operatorUserId,
                        profile.getId(),
                        identityFile.getId()
                );
            }
            return retained;
        }

        List<StudentIdentityFile> finalState = new ArrayList<StudentIdentityFile>();
        Map<Long, StudentIdentityFile> legacyById = new HashMap<Long, StudentIdentityFile>();
        for (StudentIdentityFile legacy : legacyIdentityFiles) {
            legacyById.put(legacy.getId(), legacy);
        }

        Set<Long> keptIds = new HashSet<Long>();
        for (NormalizedIdentityFile normalizedIdentityFile : normalizedIdentityFiles) {
            if (normalizedIdentityFile.id != null) {
                StudentIdentityFile existing = legacyById.get(normalizedIdentityFile.id);
                if (existing != null) {
                    keptIds.add(existing.getId());
                    applyIdentityFileOverride(existing, normalizedIdentityFile, operatorUserId);
                    finalState.add(existing);
                    continue;
                }
            }

            if (normalizedIdentityFile.storageKey == null) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Identity file entry cannot be inserted without storageKey."
                );
            }
            finalState.add(createIdentityFileFromRequest(profile, normalizedIdentityFile, operatorUserId));
        }

        for (StudentIdentityFile legacy : legacyIdentityFiles) {
            if (keptIds.contains(legacy.getId())) {
                continue;
            }
            deleteIdentityFileStorageOrThrow(legacy, operatorUserId, traceId, "put_sync_removed");
            if (legacy.getId() != null) {
                studentDocumentService.deleteDocumentsLinkedToIdentityFile(legacy.getId());
            }
            studentIdentityFileRepository.delete(legacy);
        }

        if (finalState.isEmpty()) {
            return Collections.emptyList();
        }
        List<StudentIdentityFile> persisted =
                sortIdentityFilesLatestFirst(studentIdentityFileRepository.saveAll(finalState));
        for (StudentIdentityFile identityFile : persisted) {
            log.info(
                    "Identity file upserted by PUT sync. traceId={}, userId={}, profileId={}, identityFileId={}",
                    safeTraceId(traceId),
                    operatorUserId,
                    profile.getId(),
                    identityFile.getId()
            );
        }
        return persisted;
    }

    private StudentIdentityFile createIdentityFileFromRequest(StudentProfile profile,
                                                              NormalizedIdentityFile normalizedIdentityFile,
                                                              Long operatorUserId) {
        String fileName = trimToNull(normalizedIdentityFile.fileName);
        if (fileName == null) {
            fileName = "identity.bin";
        }
        String contentType = trimToNull(normalizedIdentityFile.contentType);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        Long size = normalizedIdentityFile.sizeBytes == null ? Long.valueOf(0L) : normalizedIdentityFile.sizeBytes;
        LocalDateTime uploadedAt = normalizedIdentityFile.uploadedAt == null
                ? LocalDateTime.now()
                : normalizedIdentityFile.uploadedAt;
        Long uploadedBy = normalizedIdentityFile.uploadedBy == null
                ? operatorUserId
                : normalizedIdentityFile.uploadedBy;
        return new StudentIdentityFile(
                profile,
                normalizedIdentityFile.storageKey,
                fileName,
                contentType,
                size,
                uploadedAt,
                uploadedBy
        );
    }

    private void applyIdentityFileOverride(StudentIdentityFile existing,
                                           NormalizedIdentityFile override,
                                           Long operatorUserId) {
        String fileName = firstNonBlank(override.fileName, existing.getOriginalFilename());
        if (fileName == null) {
            fileName = "identity.bin";
        }
        String contentType = firstNonBlank(override.contentType, existing.getMimeType());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        Long size = override.sizeBytes == null ? existing.getSizeBytes() : override.sizeBytes;
        if (size == null) {
            size = Long.valueOf(0L);
        }
        LocalDateTime uploadedAt = override.uploadedAt == null ? existing.getUploadedAt() : override.uploadedAt;
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
        Long uploadedBy = override.uploadedBy == null ? existing.getUploadedBy() : override.uploadedBy;
        if (uploadedBy == null) {
            uploadedBy = operatorUserId;
        }
        existing.setOriginalFilename(fileName);
        existing.setMimeType(contentType);
        existing.setSizeBytes(size);
        existing.setUploadedAt(uploadedAt);
        existing.setUploadedBy(uploadedBy);
    }

    private List<StudentIdentityFile> sortIdentityFilesLatestFirst(List<StudentIdentityFile> identityFiles) {
        if (identityFiles == null || identityFiles.isEmpty()) {
            return Collections.emptyList();
        }
        List<StudentIdentityFile> sorted = new ArrayList<StudentIdentityFile>(identityFiles);
        Collections.sort(sorted, (left, right) -> {
            LocalDateTime leftAt = left.getUploadedAt();
            LocalDateTime rightAt = right.getUploadedAt();
            if (leftAt == null && rightAt == null) {
                Long leftId = left.getId() == null ? Long.valueOf(0L) : left.getId();
                Long rightId = right.getId() == null ? Long.valueOf(0L) : right.getId();
                return rightId.compareTo(leftId);
            }
            if (leftAt == null) {
                return 1;
            }
            if (rightAt == null) {
                return -1;
            }
            int byTime = rightAt.compareTo(leftAt);
            if (byTime != 0) {
                return byTime;
            }
            Long leftId = left.getId() == null ? Long.valueOf(0L) : left.getId();
            Long rightId = right.getId() == null ? Long.valueOf(0L) : right.getId();
            return rightId.compareTo(leftId);
        });
        return sorted;
    }

    private void deleteIdentityFileStorageOrThrow(StudentIdentityFile identityFile,
                                                  Long operatorUserId,
                                                  String traceId,
                                                  String reason) {
        try {
            identityFileStorageService.deleteRequired(identityFile.getStorageKey());
            log.info(
                    "Identity file storage deleted. traceId={}, userId={}, profileId={}, identityFileId={}, reason={}",
                    safeTraceId(traceId),
                    operatorUserId,
                    identityFile.getStudentProfile().getId(),
                    identityFile.getId(),
                    reason
            );
        } catch (RuntimeException ex) {
            log.error(
                    "Identity file storage delete failed. traceId={}, userId={}, profileId={}, identityFileId={}, reason={}",
                    safeTraceId(traceId),
                    operatorUserId,
                    identityFile.getStudentProfile().getId(),
                    identityFile.getId(),
                    reason,
                    ex
            );
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Failed to delete identity file.");
        }
    }

    private StudentProfileDto.TranscriptDto toProfileTranscriptDto(StudentSchoolTranscript transcript) {
        StudentProfileDto.TranscriptDto dto = new StudentProfileDto.TranscriptDto();
        dto.setId(transcript.getId());
        dto.setStorageKey(transcript.getStorageKey());
        dto.setTranscriptFileName(transcript.getOriginalFilename());
        dto.setTranscriptContentType(transcript.getMimeType());
        dto.setTranscriptSizeBytes(transcript.getSizeBytes());
        dto.setTranscriptUploadedAt(formatDateTime(transcript.getUploadedAt()));
        dto.setUploadedBy(transcript.getUploadedBy());
        return dto;
    }

    private StudentProfileDto.IdentityFileDto toProfileIdentityFileDto(StudentIdentityFile identityFile) {
        StudentProfileDto.IdentityFileDto dto = new StudentProfileDto.IdentityFileDto();
        dto.setId(identityFile.getId());
        dto.setStorageKey(identityFile.getStorageKey());
        dto.setIdentityFileName(identityFile.getOriginalFilename());
        dto.setIdentityFileContentType(identityFile.getMimeType());
        dto.setIdentityFileSizeBytes(identityFile.getSizeBytes());
        dto.setIdentityFileUploadedAt(formatDateTime(identityFile.getUploadedAt()));
        dto.setUploadedBy(identityFile.getUploadedBy());
        return dto;
    }

    private StudentIdentityFileUploadDto.IdentityFileItemDto toIdentityUploadItemDto(StudentIdentityFile identityFile) {
        StudentIdentityFileUploadDto.IdentityFileItemDto dto = new StudentIdentityFileUploadDto.IdentityFileItemDto();
        dto.setId(identityFile.getId());
        dto.setStorageKey(identityFile.getStorageKey());
        dto.setIdentityFileName(identityFile.getOriginalFilename());
        dto.setIdentityFileContentType(identityFile.getMimeType());
        dto.setIdentityFileSizeBytes(identityFile.getSizeBytes());
        dto.setIdentityFileUploadedAt(formatDateTime(identityFile.getUploadedAt()));
        dto.setUploadedBy(identityFile.getUploadedBy());
        return dto;
    }

    private StudentSchoolTranscriptDto.TranscriptItemDto toTranscriptItemDto(StudentSchoolTranscript transcript) {
        StudentSchoolTranscriptDto.TranscriptItemDto dto = new StudentSchoolTranscriptDto.TranscriptItemDto();
        dto.setId(transcript.getId());
        dto.setTranscriptFileName(transcript.getOriginalFilename());
        dto.setTranscriptContentType(transcript.getMimeType());
        dto.setTranscriptSizeBytes(transcript.getSizeBytes());
        dto.setTranscriptUploadedAt(formatDateTime(transcript.getUploadedAt()));
        dto.setUploadedBy(transcript.getUploadedBy());
        return dto;
    }

    private SchoolTranscriptDownload toDownload(StudentSchoolTranscript transcript) {
        byte[] data = transcriptStorageService.readAllBytes(transcript.getStorageKey());
        String fileName = trimToNull(transcript.getOriginalFilename());
        if (fileName == null) {
            fileName = "transcript.bin";
        }
        String contentType = trimToNull(transcript.getMimeType());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return new SchoolTranscriptDownload(fileName, contentType, data);
    }

    private SchoolTranscriptDownload legacySchoolDownload(StudentSchoolRecord school) {
        byte[] data = transcriptStorageService.readAllBytes(school.getTranscriptStorageKey());
        String fileName = trimToNull(school.getTranscriptOriginalFilename());
        if (fileName == null) {
            fileName = "transcript.bin";
        }

        String contentType = trimToNull(school.getTranscriptContentType());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return new SchoolTranscriptDownload(fileName, contentType, data);
    }

    private IdentityFileDownload toIdentityDownload(StudentIdentityFile identityFile) {
        byte[] data = identityFileStorageService.readAllBytes(identityFile.getStorageKey());
        String fileName = trimToNull(identityFile.getOriginalFilename());
        if (fileName == null) {
            fileName = "identity.bin";
        }
        String contentType = trimToNull(identityFile.getMimeType());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return new IdentityFileDownload(fileName, contentType, data);
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request == null ? null : request.getHeader("X-Trace-Id");
        return safeTraceId(traceId);
    }

    private String safeTraceId(String traceId) {
        if (traceId == null || traceId.trim().isEmpty()) {
            return "N/A";
        }
        return traceId.trim();
    }

    private String formatDate(LocalDate value) {
        return value == null ? null : value.toString();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private String formatUtcDateTime(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return OffsetDateTime.of(value, ZoneOffset.UTC).toInstant().toString();
    }

    private int normalizeHistoryPage(Integer page) {
        if (page == null) {
            return DEFAULT_HISTORY_PAGE;
        }
        return Math.max(page.intValue(), 0);
    }

    private int normalizeHistorySize(Integer size) {
        if (size == null || size.intValue() <= 0) {
            return DEFAULT_HISTORY_SIZE;
        }
        return Math.min(size.intValue(), MAX_HISTORY_SIZE);
    }

    private Long resolveExpectedVersion(StudentProfileDto requestBody, String ifMatch) {
        Long bodyVersion = requestBody == null ? null : requestBody.getVersion();
        if (bodyVersion != null && bodyVersion.longValue() < 0L) {
            throw new IllegalArgumentException("version must be a non-negative integer");
        }

        Long headerVersion = parseIfMatchVersion(ifMatch);
        if (bodyVersion != null && headerVersion != null && !bodyVersion.equals(headerVersion)) {
            throw new IllegalArgumentException("version in payload does not match If-Match header");
        }
        return bodyVersion != null ? bodyVersion : headerVersion;
    }

    private Long parseIfMatchVersion(String ifMatch) {
        String normalized = trimToNull(ifMatch);
        if (normalized == null || "*".equals(normalized)) {
            return null;
        }
        if (normalized.startsWith("W/")) {
            normalized = trimToNull(normalized.substring(2));
        }
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() >= 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        try {
            long value = Long.parseLong(normalized);
            if (value < 0L) {
                throw new IllegalArgumentException("If-Match version must be non-negative");
            }
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("If-Match must contain a numeric profile version");
        }
    }

    private void ensureProfileVersionMatches(Long expectedVersion, long currentVersion) {
        if (expectedVersion == null) {
            return;
        }
        if (expectedVersion.longValue() != currentVersion) {
            throw new ProfileVersionConflictException(Long.valueOf(currentVersion));
        }
    }

    private long safeProfileVersion(Long value) {
        return value == null ? 0L : Math.max(0L, value.longValue());
    }

    private void recordProfileHistoryIfChanged(Student student,
                                               StudentProfile profile,
                                               StudentProfileDto beforeSnapshot,
                                               StudentProfileDto afterSnapshot,
                                               Long actorUserId,
                                               String traceId,
                                               String changeSource) {
        List<StudentProfileHistoryListDto.FieldChangeDto> changedFields = buildChangedFields(beforeSnapshot, afterSnapshot);
        if (changedFields.isEmpty()) {
            return;
        }

        long fromVersion = safeProfileVersion(profile.getProfileVersion());
        long toVersion = fromVersion + 1L;
        profile.setProfileVersion(Long.valueOf(toVersion));
        profile.setUpdatedBy(actorUserId);
        studentProfileRepository.save(profile);
        if (afterSnapshot != null) {
            afterSnapshot.setVersion(Long.valueOf(toVersion));
        }

        AuditActor actor = resolveAuditActor(actorUserId, student);
        LocalDateTime changedAt = LocalDateTime.now(ZoneOffset.UTC);
        String normalizedSource = normalizeChangeSource(changeSource, CHANGE_SOURCE_MANUAL_SAVE);

        StudentProfileChangeEvent event = new StudentProfileChangeEvent();
        event.setStudentId(student.getId());
        event.setFromVersion(Long.valueOf(fromVersion));
        event.setToVersion(Long.valueOf(toVersion));
        event.setChangeSource(normalizedSource);
        event.setActorUserId(actor.userId);
        event.setActorRole(actor.role);
        event.setActorName(actor.name);
        event.setChangedAt(changedAt);
        event.setRequestId(safeTraceId(traceId));
        event.setChangedFieldsJson(serializeChangedFields(changedFields));
        event = studentProfileChangeEventRepository.save(event);

        StudentProfileVersion version = new StudentProfileVersion();
        version.setStudentId(student.getId());
        version.setProfileVersion(Long.valueOf(toVersion));
        String snapshotJson = serializeProfileSnapshot(afterSnapshot);
        version.setProfileSnapshotJson(snapshotJson);
        String previousHash = loadPreviousSnapshotHash(student.getId());
        version.setPreviousHash(previousHash);
        version.setSnapshotHash(computeSnapshotHash(snapshotJson, previousHash));
        version.setChangedByUserId(actor.userId);
        version.setChangedByRole(actor.role);
        version.setChangedAt(changedAt);
        version.setChangeEventId(event.getId());
        version.setRequestId(safeTraceId(traceId));
        studentProfileVersionRepository.save(version);
    }

    private String loadPreviousSnapshotHash(Long studentId) {
        return studentProfileVersionRepository.findTopByStudentIdOrderByProfileVersionDescIdDesc(studentId)
                .map(StudentProfileVersion::getSnapshotHash)
                .orElse(null);
    }

    private String serializeChangedFields(List<StudentProfileHistoryListDto.FieldChangeDto> changedFields) {
        try {
            return objectMapper.writeValueAsString(changedFields);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize profile history changed fields", ex);
        }
    }

    private String serializeProfileSnapshot(StudentProfileDto snapshot) {
        Map<String, Object> payload = buildComparableSnapshot(snapshot);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize profile history snapshot", ex);
        }
    }

    private String computeSnapshotHash(String snapshotJson, String previousHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String data = (snapshotJson == null ? "" : snapshotJson) + "|" + (previousHash == null ? "" : previousHash);
            byte[] hashed = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                builder.append(String.format("%02x", Integer.valueOf(b & 0xff)));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }

    private String normalizeChangeSource(String raw, String fallback) {
        String candidate = trimToNull(raw);
        if (candidate == null) {
            return fallback;
        }
        String normalized = candidate.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (CHANGE_SOURCE_MANUAL_SAVE.equals(normalized)
                || CHANGE_SOURCE_AUTO_SAVE.equals(normalized)
                || CHANGE_SOURCE_FILE_UPLOAD.equals(normalized)
                || CHANGE_SOURCE_VERSION_RESTORE.equals(normalized)) {
            return normalized;
        }
        if ("manual".equals(normalized) || normalized.contains("manual")) {
            return CHANGE_SOURCE_MANUAL_SAVE;
        }
        if ("auto".equals(normalized) || normalized.contains("auto")) {
            return CHANGE_SOURCE_AUTO_SAVE;
        }
        if ("file".equals(normalized) || normalized.contains("upload")) {
            return CHANGE_SOURCE_FILE_UPLOAD;
        }
        if ("restore".equals(normalized) || normalized.contains("restore")) {
            return CHANGE_SOURCE_VERSION_RESTORE;
        }
        return fallback;
    }

    private List<StudentProfileHistoryListDto.ItemDto> toHistoryItems(List<StudentProfileChangeEvent> events) {
        List<StudentProfileHistoryListDto.ItemDto> items = new ArrayList<StudentProfileHistoryListDto.ItemDto>();
        if (events == null) {
            return items;
        }

        for (StudentProfileChangeEvent event : events) {
            StudentProfileHistoryListDto.ItemDto item = new StudentProfileHistoryListDto.ItemDto();
            item.setId(event.getId());
            item.setStudentId(event.getStudentId());
            item.setFromVersion(event.getFromVersion());
            item.setToVersion(event.getToVersion());
            item.setChangeSource(normalizeChangeSource(event.getChangeSource(), CHANGE_SOURCE_MANUAL_SAVE));
            item.setActorUserId(event.getActorUserId());
            item.setActorRole(trimToNull(event.getActorRole()));
            item.setActorName(trimToNull(event.getActorName()));
            item.setChangedAt(formatUtcDateTime(event.getChangedAt() == null ? event.getCreatedAt() : event.getChangedAt()));

            List<StudentProfileHistoryListDto.FieldChangeDto> rawChanges = parseChangedFieldsJson(event.getChangedFieldsJson());
            List<StudentProfileHistoryListDto.FieldChangeDto> normalizedChanges =
                    new ArrayList<StudentProfileHistoryListDto.FieldChangeDto>(rawChanges.size());
            for (StudentProfileHistoryListDto.FieldChangeDto rawChange : rawChanges) {
                String path = trimToNull(rawChange.getPath());
                String effectivePath = path == null ? "field" : path;
                if (shouldIgnoreHistoryPath(effectivePath)) {
                    continue;
                }
                String label = trimToNull(rawChange.getLabel());
                if (label == null) {
                    label = resolveHistoryFieldLabel(effectivePath);
                }
                normalizedChanges.add(new StudentProfileHistoryListDto.FieldChangeDto(
                        effectivePath,
                        label,
                        maskHistoryValueIfSensitive(effectivePath, rawChange.getBefore()),
                        maskHistoryValueIfSensitive(effectivePath, rawChange.getAfter())
                ));
            }
            item.setChangedFields(normalizedChanges);
            items.add(item);
        }
        return items;
    }

    private List<StudentProfileHistoryListDto.FieldChangeDto> parseChangedFieldsJson(String changedFieldsJson) {
        String raw = trimToNull(changedFieldsJson);
        if (raw == null) {
            return Collections.<StudentProfileHistoryListDto.FieldChangeDto>emptyList();
        }
        try {
            List<StudentProfileHistoryListDto.FieldChangeDto> parsed = objectMapper.readValue(
                    raw,
                    new TypeReference<List<StudentProfileHistoryListDto.FieldChangeDto>>() {
                    }
            );
            return parsed == null ? Collections.<StudentProfileHistoryListDto.FieldChangeDto>emptyList() : parsed;
        } catch (Exception ex) {
            log.warn("Failed to parse changed_fields_json, return empty list. raw={}", raw, ex);
            return Collections.<StudentProfileHistoryListDto.FieldChangeDto>emptyList();
        }
    }

    private List<StudentProfileHistoryListDto.FieldChangeDto> buildChangedFields(StudentProfileDto beforeSnapshot,
                                                                                  StudentProfileDto afterSnapshot) {
        Map<String, Object> before = flattenSnapshot(buildComparableSnapshot(beforeSnapshot));
        Map<String, Object> after = flattenSnapshot(buildComparableSnapshot(afterSnapshot));
        Set<String> allPaths = new TreeSet<String>();
        allPaths.addAll(before.keySet());
        allPaths.addAll(after.keySet());

        List<StudentProfileHistoryListDto.FieldChangeDto> changes = new ArrayList<StudentProfileHistoryListDto.FieldChangeDto>();
        for (String path : allPaths) {
            if (shouldIgnoreHistoryPath(path)) {
                continue;
            }
            Object beforeValue = before.get(path);
            Object afterValue = after.get(path);
            if (Objects.equals(beforeValue, afterValue)) {
                continue;
            }
            changes.add(new StudentProfileHistoryListDto.FieldChangeDto(
                    path,
                    resolveHistoryFieldLabel(path),
                    beforeValue,
                    afterValue
            ));
        }
        return changes;
    }

    private boolean shouldIgnoreHistoryPath(String path) {
        String normalizedPath = trimToNull(path);
        if (normalizedPath == null) {
            return true;
        }

        // Technical/derived fields that should not be shown in change history.
        return normalizedPath.endsWith(".schoolRecordId")
                || normalizedPath.endsWith(".hasTranscript")
                || normalizedPath.endsWith(".transcriptFileName")
                || normalizedPath.endsWith(".transcriptSizeBytes")
                || normalizedPath.endsWith(".transcriptUploadedAt")
                || normalizedPath.endsWith(".id")
                || normalizedPath.endsWith(".storageKey")
                || normalizedPath.endsWith(".uploadedBy");
    }

    private Object maskHistoryValueIfSensitive(String path, Object value) {
        if (!isSensitiveHistoryPath(path)) {
            return value;
        }
        String raw = value == null ? null : String.valueOf(value);
        String trimmed = trimToNull(raw);
        if (trimmed == null) {
            return null;
        }
        if (trimmed.length() <= 3) {
            return "***";
        }
        return trimmed.substring(0, 3) + "***";
    }

    private boolean isSensitiveHistoryPath(String path) {
        String normalizedPath = trimToNull(path);
        if (normalizedPath == null) {
            return false;
        }
        String leaf = normalizedPath.replaceAll("\\[\\d+\\]", "");
        int lastDot = leaf.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < leaf.length() - 1) {
            leaf = leaf.substring(lastDot + 1);
        }
        return HISTORY_SENSITIVE_FIELDS.contains(leaf.toLowerCase(Locale.ROOT));
    }

    private Map<String, Object> buildComparableSnapshot(StudentProfileDto snapshot) {
        if (snapshot == null) {
            return Collections.<String, Object>emptyMap();
        }
        Map<String, Object> payload = objectMapper.convertValue(snapshot, new TypeReference<Map<String, Object>>() {
        });
        payload.remove("firstName");
        payload.remove("lastName");
        payload.remove("nickName");
        payload.remove("serviceProjects");
        payload.remove("schoolRecords");
        payload.remove("externalCourses");
        payload.remove("version");
        payload.remove("teacherNote");
        return payload;
    }

    private Map<String, Object> flattenSnapshot(Map<String, Object> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return Collections.<String, Object>emptyMap();
        }
        Map<String, Object> flattened = new LinkedHashMap<String, Object>();
        JsonNode node = objectMapper.valueToTree(snapshot);
        flattenJsonNode("", node, flattened);
        return flattened;
    }

    private void flattenJsonNode(String path, JsonNode node, Map<String, Object> flattened) {
        if (node == null || node.isMissingNode()) {
            return;
        }
        if (node.isObject()) {
            if (!node.fieldNames().hasNext()) {
                if (trimToNull(path) != null) {
                    flattened.put(path, null);
                }
                return;
            }
            node.fields().forEachRemaining(entry -> {
                String childPath = trimToNull(path) == null ? entry.getKey() : path + "." + entry.getKey();
                flattenJsonNode(childPath, entry.getValue(), flattened);
            });
            return;
        }
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                String childPath = path + "[" + i + "]";
                flattenJsonNode(childPath, node.get(i), flattened);
            }
            return;
        }
        flattened.put(path, jsonNodeToSimpleValue(node));
    }

    private Object jsonNodeToSimpleValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return Boolean.valueOf(node.asBoolean());
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        return node.toString();
    }

    private String resolveHistoryFieldLabel(String path) {
        String normalizedPath = trimToNull(path);
        if (normalizedPath == null) {
            return "字段";
        }
        String leaf = normalizedPath.replaceAll("\\[\\d+\\]", "");
        int lastDot = leaf.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < leaf.length() - 1) {
            leaf = leaf.substring(lastDot + 1);
        }

        if ("legalFirstName".equals(leaf)) return "法定名字";
        if ("legalLastName".equals(leaf)) return "法定姓氏";
        if ("preferredName".equals(leaf)) return "常用名";
        if ("gender".equals(leaf)) return "性别";
        if ("genderOther".equals(leaf)) return "其他性别说明";
        if ("birthday".equals(leaf)) return "生日";
        if ("phone".equals(leaf)) return "联系电话";
        if ("email".equals(leaf)) return "邮箱";
        if ("statusInCanada".equals(leaf)) return "在加身份";
        if ("citizenship".equals(leaf)) return "国籍";
        if ("firstLanguage".equals(leaf)) return "第一语言";
        if ("firstBoardingDate".equals(leaf)) return "首次入境加拿大时间";
        if ("studentRegion".equals(leaf)) return "高中毕业地区";
        if ("oenNumber".equals(leaf)) return "OEN";
        if ("penNumber".equals(leaf)) return "PEN";
        if ("ib".equals(leaf)) return "IB";
        if ("ap".equals(leaf)) return "AP";
        if ("serviceItems".equals(leaf)) return "服务项目";
        if ("schools".equals(leaf)) return "高中学校";
        if ("schoolName".equals(leaf)) return "学校名称";
        if ("schoolBoard".equals(leaf)) return "所属教育局";
        if ("streetAddress".equals(leaf)) return "街道地址";
        if ("streetAddressLine2".equals(leaf)) return "地址第二行";
        if ("city".equals(leaf)) return "城市";
        if ("state".equals(leaf)) return "省/州";
        if ("country".equals(leaf)) return "国家";
        if ("postal".equals(leaf)) return "邮编";
        if ("startTime".equals(leaf)) return "开始日期";
        if ("endTime".equals(leaf)) return "结束日期";
        if ("transcriptFileName".equals(leaf)) return "成绩单文件名";
        if ("transcriptSizeBytes".equals(leaf)) return "成绩单文件大小";
        if ("transcriptUploadedAt".equals(leaf)) return "成绩单上传时间";
        if ("identityFiles".equals(leaf)) return "身份证明文件";
        if ("identityFileName".equals(leaf)) return "身份证明文件名";
        if ("identityFileSizeBytes".equals(leaf)) return "身份证明文件大小";
        if ("identityFileUploadedAt".equals(leaf)) return "身份证明上传时间";
        if ("otherCourses".equals(leaf)) return "校外课程";
        if ("courseCode".equals(leaf)) return "课程代码";
        if ("mark".equals(leaf)) return "分数";
        if ("gradeLevel".equals(leaf)) return "年级";
        return normalizedPath;
    }

    private AuditActor resolveAuditActor(Long actorUserId, Student targetStudent) {
        if (actorUserId == null) {
            return new AuditActor(null, "SYSTEM", "System");
        }
        User actor = userRepository.findById(actorUserId).orElse(null);
        if (actor == null) {
            return new AuditActor(actorUserId, "SYSTEM", "System");
        }
        String role = actor.getRole() == null ? "SYSTEM" : actor.getRole().name();
        String name = trimToNull(actor.getUsername());

        if (actor.getRole() == UserRole.STUDENT) {
            Student actorStudent = targetStudent != null && targetStudent.getUser() != null
                    && actorUserId.equals(targetStudent.getUser().getId())
                    ? targetStudent
                    : studentRepository.findByUser_Id(actorUserId).orElse(null);
            if (actorStudent != null) {
                String nick = trimToNull(actorStudent.getNickName());
                if (nick != null) {
                    name = nick;
                } else {
                    String first = trimToNull(actorStudent.getFirstName());
                    String last = trimToNull(actorStudent.getLastName());
                    name = trimToNull((first == null ? "" : first) + " " + (last == null ? "" : last));
                }
            }
        } else if (actor.getRole() == UserRole.TEACHER) {
            Teacher teacher = teacherRepository.findByUser_Id(actorUserId).orElse(null);
            if (teacher != null && trimToNull(teacher.getName()) != null) {
                name = teacher.getName().trim();
            }
        }

        if (trimToNull(name) == null) {
            name = "Unknown";
        }
        return new AuditActor(actorUserId, role, name);
    }

    private void assertUploadSizeWithinLimit(MultipartFile file) {
        if (file == null) {
            return;
        }
        if (file.getSize() > MAX_UPLOAD_SIZE_BYTES) {
            throw new MaxUploadSizeExceededException(MAX_UPLOAD_SIZE_BYTES);
        }
    }

    private void assertPdfUpload(MultipartFile file) {
        if (file == null) {
            return;
        }
        String fileName = trimToNull(file.getOriginalFilename());
        String contentType = trimToNull(file.getContentType());
        boolean byName = fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".pdf");
        boolean byType = contentType != null && contentType.equalsIgnoreCase("application/pdf");
        if (!byName && !byType) {
            throw new IllegalArgumentException("Only PDF files are supported.");
        }
    }

    private NormalizedAcademicUploadMetadata normalizeAcademicUploadMetadata(String academicRecordType,
                                                                             Integer reportYear,
                                                                             String reportMonth) {
        String normalizedType = trimToNull(academicRecordType);
        if (normalizedType == null) {
            normalizedType = StudentDocumentService.ACADEMIC_RECORD_TYPE_TRANSCRIPT;
        } else if (StudentDocumentService.ACADEMIC_RECORD_TYPE_REPORT_CARD.equalsIgnoreCase(normalizedType)) {
            normalizedType = StudentDocumentService.ACADEMIC_RECORD_TYPE_REPORT_CARD;
        } else if (StudentDocumentService.ACADEMIC_RECORD_TYPE_TRANSCRIPT.equalsIgnoreCase(normalizedType)) {
            normalizedType = StudentDocumentService.ACADEMIC_RECORD_TYPE_TRANSCRIPT;
        } else {
            throw new IllegalArgumentException("academicRecordType must be Transcript or Report Card");
        }

        if (StudentDocumentService.ACADEMIC_RECORD_TYPE_REPORT_CARD.equals(normalizedType)) {
            if (reportYear == null) {
                throw new IllegalArgumentException("reportYear is required when academicRecordType is Report Card");
            }
            int normalizedYear = reportYear.intValue();
            if (normalizedYear < 1900 || normalizedYear > 2200) {
                throw new IllegalArgumentException("reportYear must be between 1900 and 2200");
            }
            String normalizedMonth = trimToNull(reportMonth);
            if (normalizedMonth == null) {
                throw new IllegalArgumentException("reportMonth is required when academicRecordType is Report Card");
            }
            String canonicalMonth = null;
            for (String month : REPORT_CARD_MONTHS) {
                if (month.equalsIgnoreCase(normalizedMonth)) {
                    canonicalMonth = month;
                    break;
                }
            }
            if (canonicalMonth == null) {
                throw new IllegalArgumentException(
                        "reportMonth must be one of: " + String.join(", ", REPORT_CARD_MONTHS)
                );
            }
            return new NormalizedAcademicUploadMetadata(
                    normalizedType,
                    Integer.valueOf(normalizedYear),
                    canonicalMonth
            );
        }

        return new NormalizedAcademicUploadMetadata(normalizedType, null, null);
    }

    private String normalizeTeacherNote(String teacherNoteRaw) {
        if (teacherNoteRaw == null) {
            return null;
        }
        String normalized = teacherNoteRaw.trim();
        if (normalized.length() > MAX_TEACHER_NOTE_LENGTH) {
            throw new IllegalArgumentException("teacherNote must be at most " + MAX_TEACHER_NOTE_LENGTH + " characters");
        }
        return normalized;
    }

    private NormalizedProfile normalizeAndValidate(StudentProfileDto requestBody) {
        if (requestBody == null) {
            throw new IllegalArgumentException("profile payload is required");
        }

        String legalFirstName = firstNonBlank(requestBody.getLegalFirstName(), requestBody.getFirstName());
        if (legalFirstName == null) {
            throw new IllegalArgumentException("legalFirstName is required");
        }

        String legalLastName = firstNonBlank(requestBody.getLegalLastName(), requestBody.getLastName());
        if (legalLastName == null) {
            throw new IllegalArgumentException("legalLastName is required");
        }

        String preferredName = firstNonBlank(requestBody.getPreferredName(), requestBody.getNickName());
        NormalizedGender normalizedGender = normalizeGenderFields(
                requestBody.getGender(),
                requestBody.getGenderOther(),
                true
        );
        LocalDate birthday = parseDateOrNull(requestBody.getBirthday(), "birthday");
        String phone = trimToNull(requestBody.getPhone());
        String email = trimToNull(requestBody.getEmail());
        String statusInCanada = trimToNull(requestBody.getStatusInCanada());
        String citizenship = trimToNull(requestBody.getCitizenship());
        String firstLanguage = trimToNull(requestBody.getFirstLanguage());
        LocalDate firstBoardingDate = parseDateOrNull(requestBody.getFirstBoardingDate(), "firstBoardingDate");
        String rawOenNumber = trimToNull(requestBody.getOenNumber());
        String rawPenNumber = trimToNull(requestBody.getPenNumber());
        String studentRegion = resolveStudentRegionForPayload(
                requestBody.getStudentRegion(),
                rawOenNumber,
                rawPenNumber
        );
        StudentRegionSnapshot regionSnapshot = resolveStudentRegionSnapshotForPayload(
                studentRegion,
                rawOenNumber,
                rawPenNumber
        );
        String oenNumber = regionSnapshot.oenNumber;
        String penNumber = regionSnapshot.penNumber;
        String ib = trimToNull(requestBody.getIb());
        List<String> serviceItems = StudentServiceItemNormalizer.normalizeIncoming(
                requestBody.getServiceItems(),
                requestBody.getServiceProjects()
        );

        Boolean apRaw = requestBody.getAp();
        if (apRaw == null) {
            throw new IllegalArgumentException("ap must be boolean");
        }

        StudentProfileDto.AddressDto addressDto = requestBody.getAddress();
        NormalizedAddress address = new NormalizedAddress(
                trimToNull(addressDto.getStreetAddress()),
                trimToNull(addressDto.getStreetAddressLine2()),
                trimToNull(addressDto.getCity()),
                trimToNull(addressDto.getState()),
                trimToNull(addressDto.getCountry()),
                trimToNull(addressDto.getPostal())
        );

        List<NormalizedSchool> schools = null;
        List<StudentProfileDto.SchoolDto> incomingSchools = chooseIncomingSchools(requestBody);
        if (incomingSchools != null) {
            schools = new ArrayList<NormalizedSchool>();
            for (int i = 0; i < incomingSchools.size(); i++) {
                StudentProfileDto.SchoolDto incomingSchool = incomingSchools.get(i);
                String pathPrefix = "schools[" + i + "]";
                if (incomingSchool == null) {
                    throw new IllegalArgumentException(pathPrefix + " is required");
                }

                Long schoolRecordId = incomingSchool.getSchoolRecordId();
                if (schoolRecordId != null && schoolRecordId.longValue() <= 0L) {
                    throw new IllegalArgumentException(pathPrefix + ".schoolRecordId must be positive");
                }

                SchoolType schoolType = parseSchoolType(incomingSchool.getSchoolType(), pathPrefix + ".schoolType");
                String schoolName = trimToNull(incomingSchool.getSchoolName());
                if (schoolName == null) {
                    throw new IllegalArgumentException(pathPrefix + ".schoolName is required");
                }
                NormalizedSchoolBoard schoolBoard = normalizeSchoolBoard(incomingSchool, pathPrefix + ".schoolBoard");

                StudentProfileDto.AddressDto schoolAddressDto = incomingSchool.getAddress();
                String schoolStreetAddress = firstNonBlank(incomingSchool.getStreetAddress(), schoolAddressDto.getStreetAddress());
                String schoolCity = firstNonBlank(incomingSchool.getCity(), schoolAddressDto.getCity());
                String schoolState = firstNonBlank(incomingSchool.getState(), schoolAddressDto.getState());
                String schoolCountry = firstNonBlank(incomingSchool.getCountry(), schoolAddressDto.getCountry());
                String schoolPostal = firstNonBlank(incomingSchool.getPostal(), schoolAddressDto.getPostal());

                LocalDate startTime = parseDateOrNull(incomingSchool.getStartTime(), pathPrefix + ".startTime");
                LocalDate endTime = parseDateOrNull(incomingSchool.getEndTime(), pathPrefix + ".endTime");
                if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
                    throw new IllegalArgumentException(pathPrefix + ".startTime must be on or before endTime");
                }

                List<NormalizedTranscript> transcripts = null;
                if (incomingSchool.getTranscripts() != null) {
                    transcripts = new ArrayList<NormalizedTranscript>();
                    for (int t = 0; t < incomingSchool.getTranscripts().size(); t++) {
                        StudentProfileDto.TranscriptDto transcriptDto = incomingSchool.getTranscripts().get(t);
                        String transcriptPath = pathPrefix + ".transcripts[" + t + "]";
                        if (transcriptDto == null) {
                            throw new IllegalArgumentException(transcriptPath + " is required");
                        }

                        Long transcriptId = transcriptDto.getId();
                        if (transcriptId != null && transcriptId.longValue() <= 0L) {
                            throw new IllegalArgumentException(transcriptPath + ".id must be positive");
                        }

                        Long sizeBytes = transcriptDto.getTranscriptSizeBytes();
                        if (sizeBytes != null && sizeBytes.longValue() < 0L) {
                            throw new IllegalArgumentException(transcriptPath + ".transcriptSizeBytes must be >= 0");
                        }

                        Long uploadedBy = transcriptDto.getUploadedBy();
                        if (uploadedBy != null && uploadedBy.longValue() <= 0L) {
                            throw new IllegalArgumentException(transcriptPath + ".uploadedBy must be positive");
                        }

                        transcripts.add(new NormalizedTranscript(
                                transcriptId,
                                trimToNull(transcriptDto.getStorageKey()),
                                trimToNull(transcriptDto.getTranscriptFileName()),
                                trimToNull(transcriptDto.getTranscriptContentType()),
                                sizeBytes,
                                parseDateTimeOrNull(transcriptDto.getTranscriptUploadedAt(), transcriptPath + ".transcriptUploadedAt"),
                                uploadedBy
                        ));
                    }
                }

                schools.add(new NormalizedSchool(
                        schoolRecordId,
                        schoolType,
                        schoolName,
                        schoolBoard.value,
                        schoolBoard.provided,
                        schoolStreetAddress,
                        schoolCity,
                        schoolState,
                        schoolCountry,
                        schoolPostal,
                        startTime,
                        endTime,
                        transcripts
                ));
            }
            schools = deduplicateSchoolsByKey(schools);
            if (schools.size() > MAX_UNIQUE_SCHOOLS_PER_PROFILE) {
                throw new IllegalArgumentException(
                        "schools must contain at most " + MAX_UNIQUE_SCHOOLS_PER_PROFILE + " unique items"
                );
            }
        }

        List<NormalizedIdentityFile> identityFiles = null;
        if (requestBody.getIdentityFiles() != null) {
            identityFiles = new ArrayList<NormalizedIdentityFile>();
            for (int i = 0; i < requestBody.getIdentityFiles().size(); i++) {
                StudentProfileDto.IdentityFileDto identityFileDto = requestBody.getIdentityFiles().get(i);
                String pathPrefix = "identityFiles[" + i + "]";
                if (identityFileDto == null) {
                    throw new IllegalArgumentException(pathPrefix + " is required");
                }

                Long identityFileId = identityFileDto.getId();
                if (identityFileId != null && identityFileId.longValue() <= 0L) {
                    throw new IllegalArgumentException(pathPrefix + ".id must be positive");
                }

                Long sizeBytes = identityFileDto.getIdentityFileSizeBytes();
                if (sizeBytes != null && sizeBytes.longValue() < 0L) {
                    throw new IllegalArgumentException(pathPrefix + ".identityFileSizeBytes must be >= 0");
                }

                Long uploadedBy = identityFileDto.getUploadedBy();
                if (uploadedBy != null && uploadedBy.longValue() <= 0L) {
                    throw new IllegalArgumentException(pathPrefix + ".uploadedBy must be positive");
                }

                identityFiles.add(new NormalizedIdentityFile(
                        identityFileId,
                        trimToNull(identityFileDto.getStorageKey()),
                        trimToNull(identityFileDto.getIdentityFileName()),
                        trimToNull(identityFileDto.getIdentityFileContentType()),
                        sizeBytes,
                        parseDateTimeOrNull(identityFileDto.getIdentityFileUploadedAt(), pathPrefix + ".identityFileUploadedAt"),
                        uploadedBy
                ));
            }
        }

        List<NormalizedCourse> courses = new ArrayList<NormalizedCourse>();
        List<StudentProfileDto.CourseDto> incomingCourses = chooseIncomingCourses(requestBody);
        for (int i = 0; i < incomingCourses.size(); i++) {
            StudentProfileDto.CourseDto incomingCourse = incomingCourses.get(i);
            String pathPrefix = "otherCourses[" + i + "]";
            if (incomingCourse == null) {
                throw new IllegalArgumentException(pathPrefix + " is required");
            }

            Integer mark = incomingCourse.getMark();
            if (mark != null && (mark < 0 || mark > 100)) {
                throw new IllegalArgumentException(pathPrefix + ".mark must be between 0 and 100");
            }

            Integer gradeLevel = incomingCourse.getGradeLevel();
            if (gradeLevel != null && (gradeLevel < 1 || gradeLevel > 12)) {
                throw new IllegalArgumentException(pathPrefix + ".gradeLevel must be between 1 and 12");
            }

            LocalDate startTime = parseDateOrNull(incomingCourse.getStartTime(), pathPrefix + ".startTime");
            LocalDate endTime = parseDateOrNull(incomingCourse.getEndTime(), pathPrefix + ".endTime");
            if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
                throw new IllegalArgumentException(pathPrefix + ".startTime must be on or before endTime");
            }

            StudentProfileDto.AddressDto courseAddressDto = incomingCourse.getAddress();
            String courseStreetAddress = firstNonBlank(incomingCourse.getStreetAddress(), courseAddressDto.getStreetAddress());
            String courseCity = firstNonBlank(incomingCourse.getCity(), courseAddressDto.getCity());
            String courseState = firstNonBlank(incomingCourse.getState(), courseAddressDto.getState());
            String courseCountry = firstNonBlank(incomingCourse.getCountry(), courseAddressDto.getCountry());
            String coursePostal = firstNonBlank(incomingCourse.getPostal(), courseAddressDto.getPostal());

            courses.add(new NormalizedCourse(
                    trimToNull(incomingCourse.getSchoolName()),
                    courseStreetAddress,
                    courseCity,
                    courseState,
                    courseCountry,
                    coursePostal,
                    trimToNull(incomingCourse.getCourseCode()),
                    mark,
                    gradeLevel,
                    startTime,
                    endTime
            ));
        }

        return new NormalizedProfile(
                legalFirstName,
                legalLastName,
                preferredName,
                normalizedGender.gender,
                normalizedGender.genderOther,
                birthday,
                phone,
                email,
                statusInCanada,
                citizenship,
                firstLanguage,
                firstBoardingDate,
                studentRegion,
                oenNumber,
                penNumber,
                ib,
                serviceItems,
                apRaw.booleanValue(),
                address,
                schools,
                identityFiles,
                courses
        );
    }

    private List<StudentProfileDto.SchoolDto> chooseIncomingSchools(StudentProfileDto requestBody) {
        if (requestBody.getSchools() != null) {
            return requestBody.getSchools();
        }
        if (requestBody.getSchoolRecords() != null) {
            return requestBody.getSchoolRecords();
        }
        return null;
    }

    private List<NormalizedSchool> deduplicateSchoolsByKey(List<NormalizedSchool> schools) {
        if (schools == null || schools.isEmpty()) {
            return new ArrayList<NormalizedSchool>();
        }
        Map<String, NormalizedSchool> deduped = new LinkedHashMap<String, NormalizedSchool>();
        int duplicatesRemoved = 0;
        for (NormalizedSchool school : schools) {
            String key = buildSchoolKey(school.schoolType, school.schoolName, school.startTime, school.endTime);
            NormalizedSchool existing = deduped.get(key);
            if (existing == null) {
                deduped.put(key, school);
                continue;
            }
            deduped.put(key, mergeDuplicateSchool(existing, school));
            duplicatesRemoved++;
        }
        if (duplicatesRemoved > 0) {
            log.warn("Duplicate schools detected in profile payload. removedDuplicates={}", duplicatesRemoved);
        }
        return new ArrayList<NormalizedSchool>(deduped.values());
    }

    private NormalizedSchool mergeDuplicateSchool(NormalizedSchool preserved, NormalizedSchool duplicate) {
        Long schoolRecordId = mergeDuplicateSchoolRecordId(preserved, duplicate);
        String schoolBoard = preserved.schoolBoardProvided ? preserved.schoolBoard : duplicate.schoolBoard;
        boolean schoolBoardProvided = preserved.schoolBoardProvided || duplicate.schoolBoardProvided;
        return new NormalizedSchool(
                schoolRecordId,
                preserved.schoolType,
                preserved.schoolName,
                schoolBoard,
                schoolBoardProvided,
                firstNonBlank(preserved.streetAddress, duplicate.streetAddress),
                firstNonBlank(preserved.city, duplicate.city),
                firstNonBlank(preserved.state, duplicate.state),
                firstNonBlank(preserved.country, duplicate.country),
                firstNonBlank(preserved.postal, duplicate.postal),
                preserved.startTime,
                preserved.endTime,
                mergeTranscriptLists(preserved.transcripts, duplicate.transcripts)
        );
    }

    private Long mergeDuplicateSchoolRecordId(NormalizedSchool preserved, NormalizedSchool duplicate) {
        if (preserved.schoolRecordId != null
                && duplicate.schoolRecordId != null
                && !preserved.schoolRecordId.equals(duplicate.schoolRecordId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Duplicate schools conflict: multiple schoolRecordId values map to the same school key."
            );
        }
        return preserved.schoolRecordId != null ? preserved.schoolRecordId : duplicate.schoolRecordId;
    }

    private List<NormalizedTranscript> mergeTranscriptLists(List<NormalizedTranscript> first,
                                                            List<NormalizedTranscript> second) {
        if ((first == null || first.isEmpty()) && (second == null || second.isEmpty())) {
            return null;
        }
        List<NormalizedTranscript> merged = new ArrayList<NormalizedTranscript>();
        if (first != null && !first.isEmpty()) {
            merged.addAll(first);
        }
        if (second != null && !second.isEmpty()) {
            merged.addAll(second);
        }
        return merged;
    }

    private List<StudentProfileDto.CourseDto> chooseIncomingCourses(StudentProfileDto requestBody) {
        if (requestBody.getOtherCourses() != null) {
            return requestBody.getOtherCourses();
        }
        if (requestBody.getExternalCourses() != null) {
            return requestBody.getExternalCourses();
        }
        return new ArrayList<StudentProfileDto.CourseDto>();
    }

    private NormalizedSchoolBoard normalizeSchoolBoard(StudentProfileDto.SchoolDto incomingSchool, String fieldPath) {
        String preferredValue = firstNonBlank(
                incomingSchool.getSchoolBoard(),
                firstNonBlank(incomingSchool.getBoardName(), incomingSchool.getEducationBureau())
        );
        boolean provided = incomingSchool.getSchoolBoard() != null
                || incomingSchool.getBoardName() != null
                || incomingSchool.getEducationBureau() != null;
        if (!provided) {
            return new NormalizedSchoolBoard(null, false);
        }

        String normalized = trimToNull(preferredValue);
        if (normalized == null
                || normalized.length() > MAX_SCHOOL_BOARD_LENGTH
                || !SCHOOL_BOARD_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(fieldPath + " is invalid");
        }
        return new NormalizedSchoolBoard(normalized, true);
    }

    private NormalizedGender normalizeGenderFields(String genderRaw, String genderOtherRaw, boolean requireOtherDetail) {
        String gender = trimToNull(genderRaw);
        String genderOther = trimToNull(genderOtherRaw);

        if (gender == null && genderOther == null) {
            return new NormalizedGender(null, null);
        }

        if (gender != null) {
            String normalized = gender.toLowerCase(Locale.ROOT);
            if ("male".equals(normalized)) {
                return new NormalizedGender("Male", null);
            }
            if ("female".equals(normalized)) {
                return new NormalizedGender("Female", null);
            }
            if ("other".equals(normalized) || normalized.startsWith("other")) {
                String parsedLegacyOther = extractLegacyGenderOther(gender);
                if (genderOther == null) {
                    genderOther = parsedLegacyOther;
                }
                if (requireOtherDetail && genderOther == null) {
                    throw new IllegalArgumentException("genderOther is required when gender is Other");
                }
                return new NormalizedGender("Other", genderOther);
            }

            // Backward compatibility:
            // historical payloads may send custom text directly in gender.
            if (genderOther == null) {
                genderOther = gender;
            }
            if (requireOtherDetail && genderOther == null) {
                throw new IllegalArgumentException("genderOther is required when gender is Other");
            }
            return new NormalizedGender("Other", genderOther);
        }

        if (requireOtherDetail && genderOther == null) {
            throw new IllegalArgumentException("genderOther is required when gender is Other");
        }
        return new NormalizedGender("Other", genderOther);
    }

    private String extractLegacyGenderOther(String genderRaw) {
        String value = trimToNull(genderRaw);
        if (value == null) {
            return null;
        }

        String lower = value.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("other")) {
            return null;
        }
        if (value.length() <= "other".length()) {
            return null;
        }

        String suffix = value.substring("other".length()).trim();
        while (!suffix.isEmpty()) {
            char first = suffix.charAt(0);
            if (first == ':' || first == '-' || first == ',' || first == ';') {
                suffix = suffix.substring(1).trim();
                continue;
            }
            break;
        }
        return suffix.isEmpty() ? null : suffix;
    }

    private String buildSchoolKey(SchoolType schoolType, String schoolName, LocalDate startTime, LocalDate endTime) {
        String type = schoolType == null ? "" : schoolType.name();
        String normalizedName = trimToNull(schoolName);
        if (normalizedName == null) {
            normalizedName = "";
        } else {
            normalizedName = normalizedName.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        }
        return type + "|" + normalizedName + "|" + formatDate(startTime) + "|" + formatDate(endTime);
    }

    private SchoolType parseSchoolType(String schoolTypeRaw, String fieldPath) {
        String value = trimToNull(schoolTypeRaw);
        if (value == null) {
            throw new IllegalArgumentException(fieldPath + " must be MAIN or OTHER");
        }
        try {
            return SchoolType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(fieldPath + " must be MAIN or OTHER");
        }
    }

    private LocalDate parseDateOrNull(String raw, String fieldName) {
        String value = trimToNull(raw);
        if (value == null) {
            return null;
        }
        if (!DATE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " must be yyyy-mm-dd");
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(fieldName + " must be yyyy-mm-dd");
        }
    }

    private LocalDateTime parseDateTimeOrNull(String raw, String fieldName) {
        String value = trimToNull(raw);
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            // Try zone-aware formats like 2026-03-04T10:00:00Z.
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // Try instant format.
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(fieldName + " must be ISO datetime");
        }
    }

    private static Set<String> buildReportCardMonths() {
        Set<String> months = new LinkedHashSet<String>();
        months.add("January");
        months.add("February");
        months.add("March");
        months.add("April");
        months.add("May");
        months.add("June");
        months.add("July");
        months.add("August");
        months.add("September");
        months.add("October");
        months.add("November");
        months.add("December");
        return Collections.unmodifiableSet(months);
    }

    private static Set<String> buildHistorySensitiveFields() {
        Set<String> fields = new HashSet<String>();
        fields.add("oennumber");
        fields.add("pennumber");
        fields.add("passportnumber");
        fields.add("identitynumber");
        fields.add("idnumber");
        return Collections.unmodifiableSet(fields);
    }

    private static Set<String> buildSupportedStudentRegions() {
        Set<String> regions = new LinkedHashSet<String>();
        regions.add(STUDENT_REGION_ONTARIO);
        regions.add(STUDENT_REGION_BRITISH_COLUMBIA);
        regions.add(STUDENT_REGION_ALBERTA);
        regions.add(STUDENT_REGION_SASKATCHEWAN);
        regions.add(STUDENT_REGION_MANITOBA);
        regions.add(STUDENT_REGION_QUEBEC);
        regions.add(STUDENT_REGION_NEW_BRUNSWICK);
        regions.add(STUDENT_REGION_NOVA_SCOTIA);
        regions.add(STUDENT_REGION_PRINCE_EDWARD_ISLAND);
        regions.add(STUDENT_REGION_NEWFOUNDLAND_AND_LABRADOR);
        regions.add(STUDENT_REGION_YUKON);
        regions.add(STUDENT_REGION_NORTHWEST_TERRITORIES);
        regions.add(STUDENT_REGION_NUNAVUT);
        regions.add(STUDENT_REGION_CHINA);
        regions.add(STUDENT_REGION_UNITED_STATES);
        return Collections.unmodifiableSet(regions);
    }

    private static Map<String, String> buildStudentRegionAliases() {
        Map<String, String> aliases = new LinkedHashMap<String, String>();
        for (String region : SUPPORTED_STUDENT_REGIONS) {
            registerStudentRegionAlias(aliases, region, region);
        }

        registerStudentRegionAlias(aliases, "ON", STUDENT_REGION_ONTARIO);
        registerStudentRegionAlias(aliases, "CA-ON", STUDENT_REGION_ONTARIO);
        registerStudentRegionAlias(aliases, "安大略", STUDENT_REGION_ONTARIO);

        registerStudentRegionAlias(aliases, "BC", STUDENT_REGION_BRITISH_COLUMBIA);
        registerStudentRegionAlias(aliases, "CA-BC", STUDENT_REGION_BRITISH_COLUMBIA);
        registerStudentRegionAlias(aliases, "不列颠哥伦比亚", STUDENT_REGION_BRITISH_COLUMBIA);

        registerStudentRegionAlias(aliases, "AB", STUDENT_REGION_ALBERTA);
        registerStudentRegionAlias(aliases, "SK", STUDENT_REGION_SASKATCHEWAN);
        registerStudentRegionAlias(aliases, "MB", STUDENT_REGION_MANITOBA);
        registerStudentRegionAlias(aliases, "QC", STUDENT_REGION_QUEBEC);
        registerStudentRegionAlias(aliases, "NB", STUDENT_REGION_NEW_BRUNSWICK);
        registerStudentRegionAlias(aliases, "NS", STUDENT_REGION_NOVA_SCOTIA);
        registerStudentRegionAlias(aliases, "PEI", STUDENT_REGION_PRINCE_EDWARD_ISLAND);
        registerStudentRegionAlias(aliases, "NL", STUDENT_REGION_NEWFOUNDLAND_AND_LABRADOR);
        registerStudentRegionAlias(aliases, "YT", STUDENT_REGION_YUKON);
        registerStudentRegionAlias(aliases, "NT", STUDENT_REGION_NORTHWEST_TERRITORIES);
        registerStudentRegionAlias(aliases, "NU", STUDENT_REGION_NUNAVUT);

        registerStudentRegionAlias(aliases, "CN", STUDENT_REGION_CHINA);
        registerStudentRegionAlias(aliases, "PRC", STUDENT_REGION_CHINA);
        registerStudentRegionAlias(aliases, "中国", STUDENT_REGION_CHINA);

        registerStudentRegionAlias(aliases, "US", STUDENT_REGION_UNITED_STATES);
        registerStudentRegionAlias(aliases, "USA", STUDENT_REGION_UNITED_STATES);
        registerStudentRegionAlias(aliases, "United States of America", STUDENT_REGION_UNITED_STATES);
        registerStudentRegionAlias(aliases, "美国", STUDENT_REGION_UNITED_STATES);

        return Collections.unmodifiableMap(aliases);
    }

    private static void registerStudentRegionAlias(Map<String, String> aliases, String alias, String region) {
        String normalized = normalizeStudentRegionAliasKey(alias);
        if (normalized != null && !normalized.isEmpty()) {
            aliases.put(normalized, region);
        }
    }

    private static String normalizeStudentRegionAliasKey(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        normalized = normalized.replace('_', ' ');
        normalized = normalized.replace('-', ' ');
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String resolveStudentRegionAlias(String value) {
        String normalizedKey = normalizeStudentRegionAliasKey(value);
        if (normalizedKey == null) {
            return null;
        }
        return STUDENT_REGION_ALIASES.get(normalizedKey);
    }

    private String resolveStudentRegionForPayload(String rawRegion, String rawOenNumber, String rawPenNumber) {
        String explicitRegion = trimToNull(rawRegion);
        if (explicitRegion != null) {
            String resolved = resolveStudentRegionAlias(explicitRegion);
            if (resolved == null) {
                throw new IllegalArgumentException(
                        "studentRegion must be one of: " + String.join(", ", SUPPORTED_STUDENT_REGIONS)
                );
            }
            return resolved;
        }

        if (trimToNull(rawOenNumber) != null) {
            return STUDENT_REGION_ONTARIO;
        }
        if (trimToNull(rawPenNumber) != null) {
            return STUDENT_REGION_BRITISH_COLUMBIA;
        }
        return STUDENT_REGION_ONTARIO;
    }

    private StudentRegionSnapshot resolveStudentRegionSnapshotForPayload(String studentRegion,
                                                                         String rawOenNumber,
                                                                         String rawPenNumber) {
        String oenNumber = trimToNull(rawOenNumber);
        String penNumber = trimToNull(rawPenNumber);

        if (STUDENT_REGION_ONTARIO.equals(studentRegion)) {
            if (oenNumber != null && !LOCAL_STUDENT_NUMBER_PATTERN.matcher(oenNumber).matches()) {
                throw new IllegalArgumentException("oenNumber must be 9 digits when studentRegion is Ontario");
            }
            return new StudentRegionSnapshot(studentRegion, oenNumber, null);
        }

        if (STUDENT_REGION_BRITISH_COLUMBIA.equals(studentRegion)) {
            if (penNumber != null && !LOCAL_STUDENT_NUMBER_PATTERN.matcher(penNumber).matches()) {
                throw new IllegalArgumentException("penNumber must be 9 digits when studentRegion is British Columbia");
            }
            return new StudentRegionSnapshot(studentRegion, null, penNumber);
        }

        return new StudentRegionSnapshot(studentRegion, null, null);
    }

    private StudentRegionSnapshot resolveStudentRegionSnapshotFromStored(String rawRegion,
                                                                         String rawOenNumber,
                                                                         String rawPenNumber) {
        String studentRegion = resolveStudentRegionAlias(rawRegion);
        String oenNumber = trimToNull(rawOenNumber);
        String penNumber = trimToNull(rawPenNumber);

        if (studentRegion == null) {
            if (oenNumber != null) {
                studentRegion = STUDENT_REGION_ONTARIO;
            } else if (penNumber != null) {
                studentRegion = STUDENT_REGION_BRITISH_COLUMBIA;
            } else {
                studentRegion = STUDENT_REGION_ONTARIO;
            }
        }

        if (STUDENT_REGION_ONTARIO.equals(studentRegion)) {
            String normalizedOen = oenNumber;
            if (normalizedOen != null && !LOCAL_STUDENT_NUMBER_PATTERN.matcher(normalizedOen).matches()) {
                normalizedOen = null;
            }
            return new StudentRegionSnapshot(studentRegion, normalizedOen, null);
        }

        if (STUDENT_REGION_BRITISH_COLUMBIA.equals(studentRegion)) {
            String normalizedPen = penNumber;
            if (normalizedPen != null && !LOCAL_STUDENT_NUMBER_PATTERN.matcher(normalizedPen).matches()) {
                normalizedPen = null;
            }
            return new StudentRegionSnapshot(studentRegion, null, normalizedPen);
        }

        return new StudentRegionSnapshot(studentRegion, null, null);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String primary, String fallback) {
        String first = trimToNull(primary);
        if (first != null) {
            return first;
        }
        return trimToNull(fallback);
    }

    private static class StudentRegionSnapshot {
        private final String studentRegion;
        private final String oenNumber;
        private final String penNumber;

        private StudentRegionSnapshot(String studentRegion, String oenNumber, String penNumber) {
            this.studentRegion = studentRegion;
            this.oenNumber = oenNumber;
            this.penNumber = penNumber;
        }
    }

    private static class NormalizedAcademicUploadMetadata {
        private final String academicRecordType;
        private final Integer reportYear;
        private final String reportMonth;

        private NormalizedAcademicUploadMetadata(String academicRecordType, Integer reportYear, String reportMonth) {
            this.academicRecordType = academicRecordType;
            this.reportYear = reportYear;
            this.reportMonth = reportMonth;
        }
    }

    private static class NormalizedGender {
        private final String gender;
        private final String genderOther;

        private NormalizedGender(String gender, String genderOther) {
            this.gender = gender;
            this.genderOther = genderOther;
        }
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

    public static class SchoolTranscriptDownload {
        private final String fileName;
        private final String contentType;
        private final byte[] content;

        public SchoolTranscriptDownload(String fileName, String contentType, byte[] content) {
            this.fileName = fileName;
            this.contentType = contentType;
            this.content = content;
        }

        public String getFileName() {
            return fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public byte[] getContent() {
            return content;
        }
    }

    public static class IdentityFileDownload {
        private final String fileName;
        private final String contentType;
        private final byte[] content;

        public IdentityFileDownload(String fileName, String contentType, byte[] content) {
            this.fileName = fileName;
            this.contentType = contentType;
            this.content = content;
        }

        public String getFileName() {
            return fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public byte[] getContent() {
            return content;
        }
    }

    private static class SchoolSyncResult {
        private final List<StudentSchoolRecord> schools;
        private final List<StudentSchoolTranscript> transcripts;

        private SchoolSyncResult(List<StudentSchoolRecord> schools, List<StudentSchoolTranscript> transcripts) {
            this.schools = schools;
            this.transcripts = transcripts;
        }
    }

    private static class SchoolSyncPlan {
        private final StudentSchoolRecord school;
        private final NormalizedSchool normalizedSchool;
        private final List<StudentSchoolTranscript> legacyTranscripts;

        private SchoolSyncPlan(StudentSchoolRecord school,
                               NormalizedSchool normalizedSchool,
                               List<StudentSchoolTranscript> legacyTranscripts) {
            this.school = school;
            this.normalizedSchool = normalizedSchool;
            this.legacyTranscripts = legacyTranscripts;
        }
    }

    private static class NormalizedProfile {
        private final String legalFirstName;
        private final String legalLastName;
        private final String preferredName;
        private final String gender;
        private final String genderOther;
        private final LocalDate birthday;
        private final String phone;
        private final String email;
        private final String statusInCanada;
        private final String citizenship;
        private final String firstLanguage;
        private final LocalDate firstBoardingDate;
        private final String studentRegion;
        private final String oenNumber;
        private final String penNumber;
        private final String ib;
        private final List<String> serviceItems;
        private final boolean ap;
        private final NormalizedAddress address;
        private final List<NormalizedSchool> schools;
        private final List<NormalizedIdentityFile> identityFiles;
        private final List<NormalizedCourse> otherCourses;

        private NormalizedProfile(String legalFirstName,
                                  String legalLastName,
                                  String preferredName,
                                  String gender,
                                  String genderOther,
                                  LocalDate birthday,
                                  String phone,
                                  String email,
                                  String statusInCanada,
                                  String citizenship,
                                  String firstLanguage,
                                  LocalDate firstBoardingDate,
                                  String studentRegion,
                                  String oenNumber,
                                  String penNumber,
                                  String ib,
                                  List<String> serviceItems,
                                  boolean ap,
                                  NormalizedAddress address,
                                  List<NormalizedSchool> schools,
                                  List<NormalizedIdentityFile> identityFiles,
                                  List<NormalizedCourse> otherCourses) {
            this.legalFirstName = legalFirstName;
            this.legalLastName = legalLastName;
            this.preferredName = preferredName;
            this.gender = gender;
            this.genderOther = genderOther;
            this.birthday = birthday;
            this.phone = phone;
            this.email = email;
            this.statusInCanada = statusInCanada;
            this.citizenship = citizenship;
            this.firstLanguage = firstLanguage;
            this.firstBoardingDate = firstBoardingDate;
            this.studentRegion = studentRegion;
            this.oenNumber = oenNumber;
            this.penNumber = penNumber;
            this.ib = ib;
            this.serviceItems = serviceItems;
            this.ap = ap;
            this.address = address;
            this.schools = schools;
            this.identityFiles = identityFiles;
            this.otherCourses = otherCourses;
        }
    }

    private static class NormalizedAddress {
        private final String streetAddress;
        private final String streetAddressLine2;
        private final String city;
        private final String state;
        private final String country;
        private final String postal;

        private NormalizedAddress(String streetAddress,
                                  String streetAddressLine2,
                                  String city,
                                  String state,
                                  String country,
                                  String postal) {
            this.streetAddress = streetAddress;
            this.streetAddressLine2 = streetAddressLine2;
            this.city = city;
            this.state = state;
            this.country = country;
            this.postal = postal;
        }
    }

    private static class NormalizedSchool {
        private final Long schoolRecordId;
        private final SchoolType schoolType;
        private final String schoolName;
        private final String schoolBoard;
        private final boolean schoolBoardProvided;
        private final String streetAddress;
        private final String city;
        private final String state;
        private final String country;
        private final String postal;
        private final LocalDate startTime;
        private final LocalDate endTime;
        private final List<NormalizedTranscript> transcripts;

        private NormalizedSchool(Long schoolRecordId,
                                 SchoolType schoolType,
                                 String schoolName,
                                 String schoolBoard,
                                 boolean schoolBoardProvided,
                                 String streetAddress,
                                 String city,
                                 String state,
                                 String country,
                                 String postal,
                                 LocalDate startTime,
                                 LocalDate endTime,
                                 List<NormalizedTranscript> transcripts) {
            this.schoolRecordId = schoolRecordId;
            this.schoolType = schoolType;
            this.schoolName = schoolName;
            this.schoolBoard = schoolBoard;
            this.schoolBoardProvided = schoolBoardProvided;
            this.streetAddress = streetAddress;
            this.city = city;
            this.state = state;
            this.country = country;
            this.postal = postal;
            this.startTime = startTime;
            this.endTime = endTime;
            this.transcripts = transcripts;
        }
    }

    private static class NormalizedSchoolBoard {
        private final String value;
        private final boolean provided;

        private NormalizedSchoolBoard(String value, boolean provided) {
            this.value = value;
            this.provided = provided;
        }
    }

    private static class NormalizedTranscript {
        private final Long id;
        private final String storageKey;
        private final String fileName;
        private final String contentType;
        private final Long sizeBytes;
        private final LocalDateTime uploadedAt;
        private final Long uploadedBy;

        private NormalizedTranscript(Long id,
                                     String storageKey,
                                     String fileName,
                                     String contentType,
                                     Long sizeBytes,
                                     LocalDateTime uploadedAt,
                                     Long uploadedBy) {
            this.id = id;
            this.storageKey = storageKey;
            this.fileName = fileName;
            this.contentType = contentType;
            this.sizeBytes = sizeBytes;
            this.uploadedAt = uploadedAt;
            this.uploadedBy = uploadedBy;
        }
    }

    private static class NormalizedIdentityFile {
        private final Long id;
        private final String storageKey;
        private final String fileName;
        private final String contentType;
        private final Long sizeBytes;
        private final LocalDateTime uploadedAt;
        private final Long uploadedBy;

        private NormalizedIdentityFile(Long id,
                                       String storageKey,
                                       String fileName,
                                       String contentType,
                                       Long sizeBytes,
                                       LocalDateTime uploadedAt,
                                       Long uploadedBy) {
            this.id = id;
            this.storageKey = storageKey;
            this.fileName = fileName;
            this.contentType = contentType;
            this.sizeBytes = sizeBytes;
            this.uploadedAt = uploadedAt;
            this.uploadedBy = uploadedBy;
        }
    }

    private static class NormalizedCourse {
        private final String schoolName;
        private final String streetAddress;
        private final String city;
        private final String state;
        private final String country;
        private final String postal;
        private final String courseCode;
        private final Integer mark;
        private final Integer gradeLevel;
        private final LocalDate startTime;
        private final LocalDate endTime;

        private NormalizedCourse(String schoolName,
                                 String streetAddress,
                                 String city,
                                 String state,
                                 String country,
                                 String postal,
                                 String courseCode,
                                 Integer mark,
                                 Integer gradeLevel,
                                 LocalDate startTime,
                                 LocalDate endTime) {
            this.schoolName = schoolName;
            this.streetAddress = streetAddress;
            this.city = city;
            this.state = state;
            this.country = country;
            this.postal = postal;
            this.courseCode = courseCode;
            this.mark = mark;
            this.gradeLevel = gradeLevel;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
}

