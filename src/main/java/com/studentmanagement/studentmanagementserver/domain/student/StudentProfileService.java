package com.studentmanagement.studentmanagementserver.domain.student;

import com.studentmanagement.studentmanagementserver.domain.enums.SchoolType;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentCourseRecordRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentProfileRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentSchoolRecordRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentSchoolTranscriptRepository;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class StudentProfileService {

    private static final Logger log = LoggerFactory.getLogger(StudentProfileService.class);
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private final AuthSessionService authSessionService;
    private final StudentRepository studentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentSchoolRecordRepository studentSchoolRecordRepository;
    private final StudentSchoolTranscriptRepository studentSchoolTranscriptRepository;
    private final StudentCourseRecordRepository studentCourseRecordRepository;
    private final StudentSchoolTranscriptStorageService transcriptStorageService;

    public StudentProfileService(AuthSessionService authSessionService,
                                 StudentRepository studentRepository,
                                 StudentProfileRepository studentProfileRepository,
                                 StudentSchoolRecordRepository studentSchoolRecordRepository,
                                 StudentSchoolTranscriptRepository studentSchoolTranscriptRepository,
                                 StudentCourseRecordRepository studentCourseRecordRepository,
                                 StudentSchoolTranscriptStorageService transcriptStorageService) {
        this.authSessionService = authSessionService;
        this.studentRepository = studentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentSchoolRecordRepository = studentSchoolRecordRepository;
        this.studentSchoolTranscriptRepository = studentSchoolTranscriptRepository;
        this.studentCourseRecordRepository = studentCourseRecordRepository;
        this.transcriptStorageService = transcriptStorageService;
    }

    @Transactional(readOnly = true)
    public StudentProfileDto getCurrentStudentProfile(HttpServletRequest request) {
        Student student = requireCurrentStudent(request);
        return getProfileForStudent(student);
    }

    @Transactional(readOnly = true)
    public StudentProfileDto getProfileByStudentId(Long studentId) {
        Student student = requireStudentById(studentId);
        return getProfileForStudent(student);
    }

    @Transactional
    public StudentProfileDto saveCurrentStudentProfile(StudentProfileDto requestBody, HttpServletRequest request) {
        Student student = requireCurrentStudent(request);
        return saveProfileForStudent(student, requestBody, student.getUser().getId(), resolveTraceId(request));
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
        return saveProfileForStudent(student, requestBody, operatorUserId, traceId);
    }

    @Transactional
    public StudentSchoolTranscriptDto uploadCurrentStudentSchoolTranscript(Long schoolRecordId,
                                                                           MultipartFile file,
                                                                           HttpServletRequest request) {
        Student student = requireCurrentStudent(request);
        return uploadSchoolTranscriptForStudent(
                student,
                schoolRecordId,
                file,
                student.getUser().getId(),
                resolveTraceId(request)
        );
    }

    @Transactional
    public StudentSchoolTranscriptDto uploadStudentSchoolTranscriptByStudentId(Long studentId,
                                                                               Long schoolRecordId,
                                                                               MultipartFile file) {
        return uploadStudentSchoolTranscriptByStudentId(studentId, schoolRecordId, file, null, "N/A");
    }

    @Transactional
    public StudentSchoolTranscriptDto uploadStudentSchoolTranscriptByStudentId(Long studentId,
                                                                               Long schoolRecordId,
                                                                               MultipartFile file,
                                                                               Long uploadedBy,
                                                                               String traceId) {
        Student student = requireStudentById(studentId);
        Long operatorUserId = uploadedBy == null ? student.getUser().getId() : uploadedBy;
        return uploadSchoolTranscriptForStudent(student, schoolRecordId, file, operatorUserId, traceId);
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

    private StudentProfileDto getProfileForStudent(Student student) {
        StudentProfile profile = studentProfileRepository.findByStudent_Id(student.getId()).orElse(null);
        List<StudentSchoolRecord> schools = studentSchoolRecordRepository.findByStudent_IdOrderByIdAsc(student.getId());
        List<StudentSchoolTranscript> transcripts = findTranscriptsBySchoolRecords(schools);
        List<StudentCourseRecord> courses = studentCourseRecordRepository.findByStudent_IdOrderByIdAsc(student.getId());
        return toDto(student, profile, schools, transcripts, courses);
    }

    private StudentProfileDto saveProfileForStudent(Student student,
                                                    StudentProfileDto requestBody,
                                                    Long operatorUserId,
                                                    String traceId) {
        NormalizedProfile normalized = normalizeAndValidate(requestBody);

        StudentProfile profile = studentProfileRepository.findByStudent_Id(student.getId())
                .orElseGet(() -> new StudentProfile(student));
        applyProfile(profile, normalized, operatorUserId);
        profile = studentProfileRepository.save(profile);

        student.updateProfileNames(
                normalized.legalFirstName,
                normalized.legalLastName,
                normalized.preferredName
        );
        studentRepository.save(student);

        List<StudentSchoolRecord> existingSchoolRecords = studentSchoolRecordRepository.findByStudent_IdOrderByIdAsc(student.getId());
        List<StudentSchoolTranscript> existingTranscripts = findTranscriptsBySchoolRecords(existingSchoolRecords);
        Map<String, List<StudentSchoolTranscript>> transcriptsBySchoolKey =
                mapTranscriptsBySchoolKey(existingSchoolRecords, existingTranscripts);

        Set<String> incomingSchoolKeys = new HashSet<String>();
        for (NormalizedSchool school : normalized.schools) {
            incomingSchoolKeys.add(buildSchoolKey(
                    school.schoolType,
                    school.schoolName,
                    school.startTime,
                    school.endTime
            ));
        }
        for (Map.Entry<String, List<StudentSchoolTranscript>> entry : transcriptsBySchoolKey.entrySet()) {
            if (incomingSchoolKeys.contains(entry.getKey())) {
                continue;
            }
            for (StudentSchoolTranscript transcript : entry.getValue()) {
                deleteTranscriptStorageOrThrow(transcript, operatorUserId, traceId, "school_removed");
            }
        }

        studentSchoolRecordRepository.deleteByStudent_Id(student.getId());
        List<StudentSchoolRecord> savedSchools = new ArrayList<StudentSchoolRecord>();
        for (NormalizedSchool school : normalized.schools) {
            StudentSchoolRecord schoolRecord = new StudentSchoolRecord(
                    student,
                    school.schoolType,
                    school.schoolName,
                    school.streetAddress,
                    school.city,
                    school.state,
                    school.country,
                    school.postal,
                    school.startTime,
                    school.endTime
            );
            savedSchools.add(schoolRecord);
        }
        if (!savedSchools.isEmpty()) {
            savedSchools = studentSchoolRecordRepository.saveAll(savedSchools);
        }

        List<StudentSchoolTranscript> savedTranscripts = new ArrayList<StudentSchoolTranscript>();
        for (int i = 0; i < savedSchools.size(); i++) {
            StudentSchoolRecord savedSchool = savedSchools.get(i);
            NormalizedSchool normalizedSchool = normalized.schools.get(i);
            String schoolKey = buildSchoolKey(
                    normalizedSchool.schoolType,
                    normalizedSchool.schoolName,
                    normalizedSchool.startTime,
                    normalizedSchool.endTime
            );
            List<StudentSchoolTranscript> legacyTranscripts = transcriptsBySchoolKey.remove(schoolKey);
            if (legacyTranscripts == null) {
                legacyTranscripts = Collections.emptyList();
            }
            List<StudentSchoolTranscript> syncedTranscripts = syncSchoolTranscripts(
                    savedSchool,
                    normalizedSchool,
                    legacyTranscripts,
                    operatorUserId,
                    traceId
            );
            applyLegacyTranscriptFields(savedSchool, syncedTranscripts);
            savedTranscripts.addAll(syncedTranscripts);
        }
        if (!savedSchools.isEmpty()) {
            savedSchools = studentSchoolRecordRepository.saveAll(savedSchools);
        }
        for (Map.Entry<String, List<StudentSchoolTranscript>> left : transcriptsBySchoolKey.entrySet()) {
            for (StudentSchoolTranscript transcript : left.getValue()) {
                deleteTranscriptStorageOrThrow(transcript, operatorUserId, traceId, "key_not_reused");
            }
        }

        for (StudentSchoolRecord savedSchool : savedSchools) {
            if (savedSchool.getTranscriptStorageKey() == null) {
                savedSchool.setTranscriptOriginalFilename(null);
                savedSchool.setTranscriptContentType(null);
                savedSchool.setTranscriptSizeBytes(null);
                savedSchool.setTranscriptUploadedAt(null);
            }
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

        return toDto(student, profile, savedSchools, savedTranscripts, savedCourses);
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
        profile.setOenNumber(normalized.oenNumber);
        profile.setIb(normalized.ib);
        profile.setAp(normalized.ap);
        profile.setIdentityFileNote(normalized.identityFileNote);
        profile.setStreetAddress(normalized.address.streetAddress);
        profile.setStreetAddressLine2(normalized.address.streetAddressLine2);
        profile.setCity(normalized.address.city);
        profile.setState(normalized.address.state);
        profile.setCountry(normalized.address.country);
        profile.setPostal(normalized.address.postal);
        profile.setUpdatedBy(operatorUserId);
    }

    private StudentSchoolTranscriptDto uploadSchoolTranscriptForStudent(Student student,
                                                                        Long schoolRecordId,
                                                                        MultipartFile file,
                                                                        Long uploadedBy,
                                                                        String traceId) {
        if (schoolRecordId == null || schoolRecordId.longValue() <= 0L) {
            throw new IllegalArgumentException("schoolRecordId must be positive");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("transcript file is required");
        }

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

        return toTranscriptDto(school, transcripts);
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
                                                       List<StudentSchoolTranscript> transcripts) {
        StudentSchoolTranscriptDto dto = new StudentSchoolTranscriptDto();
        dto.setSchoolRecordId(school.getId());
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

    private StudentProfileDto toDto(Student student,
                                    StudentProfile profile,
                                    List<StudentSchoolRecord> schools,
                                    List<StudentSchoolTranscript> transcripts,
                                    List<StudentCourseRecord> courses) {
        StudentProfileDto dto = new StudentProfileDto();

        dto.setLegalFirstName(student.getFirstName());
        dto.setLegalLastName(student.getLastName());
        dto.setPreferredName(student.getNickName());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setNickName(student.getNickName());
        dto.setAp(Boolean.FALSE);

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
            dto.setOenNumber(profile.getOenNumber());
            dto.setIb(profile.getIb());
            dto.setAp(profile.isAp());
            dto.setIdentityFileNote(profile.getIdentityFileNote());

            StudentProfileDto.AddressDto address = new StudentProfileDto.AddressDto();
            address.setStreetAddress(profile.getStreetAddress());
            address.setStreetAddressLine2(profile.getStreetAddressLine2());
            address.setCity(profile.getCity());
            address.setState(profile.getState());
            address.setCountry(profile.getCountry());
            address.setPostal(profile.getPostal());
            dto.setAddress(address);
        }

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

    private Map<String, List<StudentSchoolTranscript>> mapTranscriptsBySchoolKey(List<StudentSchoolRecord> schools,
                                                                                  List<StudentSchoolTranscript> transcripts) {
        Map<Long, String> schoolKeyById = new HashMap<Long, String>();
        for (StudentSchoolRecord school : schools) {
            schoolKeyById.put(
                    school.getId(),
                    buildSchoolKey(school.getSchoolType(), school.getSchoolName(), school.getStartTime(), school.getEndTime())
            );
        }

        Map<String, List<StudentSchoolTranscript>> byKey = new LinkedHashMap<String, List<StudentSchoolTranscript>>();
        for (StudentSchoolTranscript transcript : transcripts) {
            String key = schoolKeyById.get(transcript.getSchoolRecord().getId());
            if (key == null) {
                continue;
            }
            List<StudentSchoolTranscript> list = byKey.get(key);
            if (list == null) {
                list = new ArrayList<StudentSchoolTranscript>();
                byKey.put(key, list);
            }
            list.add(transcript);
        }

        for (StudentSchoolRecord school : schools) {
            String key = schoolKeyById.get(school.getId());
            if (key == null || byKey.containsKey(key)) {
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
            byKey.put(key, list);
        }
        return byKey;
    }

    private List<StudentSchoolTranscript> syncSchoolTranscripts(StudentSchoolRecord school,
                                                                NormalizedSchool normalizedSchool,
                                                                List<StudentSchoolTranscript> legacyTranscripts,
                                                                Long operatorUserId,
                                                                String traceId) {
        List<StudentSchoolTranscript> finalState = new ArrayList<StudentSchoolTranscript>();
        if (normalizedSchool.transcripts == null) {
            for (StudentSchoolTranscript legacy : legacyTranscripts) {
                finalState.add(copyTranscriptForSchool(school, legacy));
            }
            if (finalState.isEmpty()) {
                return Collections.emptyList();
            }
            List<StudentSchoolTranscript> persisted =
                    sortTranscriptsLatestFirst(studentSchoolTranscriptRepository.saveAll(finalState));
            for (StudentSchoolTranscript transcript : persisted) {
                log.info(
                        "Transcript retained by PUT sync. traceId={}, userId={}, schoolRecordId={}, transcriptId={}",
                        safeTraceId(traceId),
                        operatorUserId,
                        school.getId(),
                        transcript.getId()
                );
            }
            return persisted;
        }

        Map<Long, StudentSchoolTranscript> legacyById = new HashMap<Long, StudentSchoolTranscript>();
        for (StudentSchoolTranscript legacy : legacyTranscripts) {
            legacyById.put(legacy.getId(), legacy);
        }

        Set<Long> keptIds = new HashSet<Long>();
        for (NormalizedTranscript normalizedTranscript : normalizedSchool.transcripts) {
            if (normalizedTranscript.id != null) {
                StudentSchoolTranscript existing = legacyById.get(normalizedTranscript.id);
                if (existing != null) {
                    keptIds.add(existing.getId());
                    finalState.add(copyTranscriptForSchool(school, existing, normalizedTranscript, operatorUserId));
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
            if (keptIds.contains(legacy.getId())) {
                continue;
            }
            deleteTranscriptStorageOrThrow(legacy, operatorUserId, traceId, "put_sync_removed");
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

    private StudentSchoolTranscript copyTranscriptForSchool(StudentSchoolRecord school, StudentSchoolTranscript source) {
        return new StudentSchoolTranscript(
                school,
                source.getStorageKey(),
                source.getOriginalFilename(),
                source.getMimeType(),
                source.getSizeBytes(),
                source.getUploadedAt(),
                source.getUploadedBy()
        );
    }

    private StudentSchoolTranscript copyTranscriptForSchool(StudentSchoolRecord school,
                                                            StudentSchoolTranscript source,
                                                            NormalizedTranscript override,
                                                            Long operatorUserId) {
        String fileName = firstNonBlank(override.fileName, source.getOriginalFilename());
        if (fileName == null) {
            fileName = "transcript.bin";
        }
        String contentType = firstNonBlank(override.contentType, source.getMimeType());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        Long size = override.sizeBytes == null ? source.getSizeBytes() : override.sizeBytes;
        if (size == null) {
            size = Long.valueOf(0L);
        }
        LocalDateTime uploadedAt = override.uploadedAt == null ? source.getUploadedAt() : override.uploadedAt;
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
        Long uploadedBy = override.uploadedBy == null ? source.getUploadedBy() : override.uploadedBy;
        if (uploadedBy == null) {
            uploadedBy = operatorUserId;
        }
        return new StudentSchoolTranscript(
                school,
                source.getStorageKey(),
                fileName,
                contentType,
                size,
                uploadedAt,
                uploadedBy
        );
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
        String oenNumber = trimToNull(requestBody.getOenNumber());
        String ib = trimToNull(requestBody.getIb());
        String identityFileNote = trimToNull(requestBody.getIdentityFileNote());

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

        List<NormalizedSchool> schools = new ArrayList<NormalizedSchool>();
        List<StudentProfileDto.SchoolDto> incomingSchools = chooseIncomingSchools(requestBody);
        for (int i = 0; i < incomingSchools.size(); i++) {
            StudentProfileDto.SchoolDto incomingSchool = incomingSchools.get(i);
            String pathPrefix = "schools[" + i + "]";
            if (incomingSchool == null) {
                throw new IllegalArgumentException(pathPrefix + " is required");
            }

            SchoolType schoolType = parseSchoolType(incomingSchool.getSchoolType(), pathPrefix + ".schoolType");
            String schoolName = trimToNull(incomingSchool.getSchoolName());
            if (schoolName == null) {
                throw new IllegalArgumentException(pathPrefix + ".schoolName is required");
            }

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
                    schoolType,
                    schoolName,
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
                oenNumber,
                ib,
                apRaw.booleanValue(),
                identityFileNote,
                address,
                schools,
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
        return new ArrayList<StudentProfileDto.SchoolDto>();
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

    private static class NormalizedGender {
        private final String gender;
        private final String genderOther;

        private NormalizedGender(String gender, String genderOther) {
            this.gender = gender;
            this.genderOther = genderOther;
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
        private final String oenNumber;
        private final String ib;
        private final boolean ap;
        private final String identityFileNote;
        private final NormalizedAddress address;
        private final List<NormalizedSchool> schools;
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
                                  String oenNumber,
                                  String ib,
                                  boolean ap,
                                  String identityFileNote,
                                  NormalizedAddress address,
                                  List<NormalizedSchool> schools,
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
            this.oenNumber = oenNumber;
            this.ib = ib;
            this.ap = ap;
            this.identityFileNote = identityFileNote;
            this.address = address;
            this.schools = schools;
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
        private final SchoolType schoolType;
        private final String schoolName;
        private final String streetAddress;
        private final String city;
        private final String state;
        private final String country;
        private final String postal;
        private final LocalDate startTime;
        private final LocalDate endTime;
        private final List<NormalizedTranscript> transcripts;

        private NormalizedSchool(SchoolType schoolType,
                                 String schoolName,
                                 String streetAddress,
                                 String city,
                                 String state,
                                 String country,
                                 String postal,
                                 LocalDate startTime,
                                 LocalDate endTime,
                                 List<NormalizedTranscript> transcripts) {
            this.schoolType = schoolType;
            this.schoolName = schoolName;
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

