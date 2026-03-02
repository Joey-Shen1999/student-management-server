package com.studentmanagement.studentmanagementserver.domain.student;

import com.studentmanagement.studentmanagementserver.domain.enums.SchoolType;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentCourseRecordRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentProfileRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentSchoolRecordRepository;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class StudentProfileService {

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private final AuthSessionService authSessionService;
    private final StudentRepository studentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentSchoolRecordRepository studentSchoolRecordRepository;
    private final StudentCourseRecordRepository studentCourseRecordRepository;
    private final StudentSchoolTranscriptStorageService transcriptStorageService;

    public StudentProfileService(AuthSessionService authSessionService,
                                 StudentRepository studentRepository,
                                 StudentProfileRepository studentProfileRepository,
                                 StudentSchoolRecordRepository studentSchoolRecordRepository,
                                 StudentCourseRecordRepository studentCourseRecordRepository,
                                 StudentSchoolTranscriptStorageService transcriptStorageService) {
        this.authSessionService = authSessionService;
        this.studentRepository = studentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentSchoolRecordRepository = studentSchoolRecordRepository;
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
        return saveProfileForStudent(student, requestBody, student.getUser().getId());
    }

    @Transactional
    public StudentProfileDto saveProfileByStudentId(Long studentId, StudentProfileDto requestBody, Long operatorUserId) {
        Student student = requireStudentById(studentId);
        return saveProfileForStudent(student, requestBody, operatorUserId);
    }

    @Transactional
    public StudentSchoolTranscriptDto uploadCurrentStudentSchoolTranscript(Long schoolRecordId,
                                                                           MultipartFile file,
                                                                           HttpServletRequest request) {
        Student student = requireCurrentStudent(request);
        return uploadSchoolTranscriptForStudent(student, schoolRecordId, file);
    }

    @Transactional
    public StudentSchoolTranscriptDto uploadStudentSchoolTranscriptByStudentId(Long studentId,
                                                                               Long schoolRecordId,
                                                                               MultipartFile file) {
        Student student = requireStudentById(studentId);
        return uploadSchoolTranscriptForStudent(student, schoolRecordId, file);
    }

    @Transactional(readOnly = true)
    public SchoolTranscriptDownload downloadCurrentStudentSchoolTranscript(Long schoolRecordId, HttpServletRequest request) {
        Student student = requireCurrentStudent(request);
        return downloadSchoolTranscriptForStudent(student, schoolRecordId);
    }

    @Transactional(readOnly = true)
    public SchoolTranscriptDownload downloadStudentSchoolTranscriptByStudentId(Long studentId, Long schoolRecordId) {
        Student student = requireStudentById(studentId);
        return downloadSchoolTranscriptForStudent(student, schoolRecordId);
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

    private StudentProfileDto getProfileForStudent(Student student) {
        StudentProfile profile = studentProfileRepository.findByStudent_Id(student.getId()).orElse(null);
        List<StudentSchoolRecord> schools = studentSchoolRecordRepository.findByStudent_IdOrderByIdAsc(student.getId());
        List<StudentCourseRecord> courses = studentCourseRecordRepository.findByStudent_IdOrderByIdAsc(student.getId());
        return toDto(student, profile, schools, courses);
    }

    private StudentProfileDto saveProfileForStudent(Student student, StudentProfileDto requestBody, Long operatorUserId) {
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
        Map<String, TranscriptBinding> transcriptBySchoolKey = new HashMap<String, TranscriptBinding>();
        for (StudentSchoolRecord existingSchool : existingSchoolRecords) {
            String storageKey = trimToNull(existingSchool.getTranscriptStorageKey());
            if (storageKey == null) {
                continue;
            }

            String key = buildSchoolKey(
                    existingSchool.getSchoolType(),
                    existingSchool.getSchoolName(),
                    existingSchool.getStartTime(),
                    existingSchool.getEndTime()
            );
            transcriptBySchoolKey.put(key, new TranscriptBinding(
                    existingSchool.getTranscriptOriginalFilename(),
                    existingSchool.getTranscriptContentType(),
                    existingSchool.getTranscriptStorageKey(),
                    existingSchool.getTranscriptSizeBytes(),
                    existingSchool.getTranscriptUploadedAt()
            ));
        }

        studentSchoolRecordRepository.deleteByStudent_Id(student.getId());
        List<StudentSchoolRecord> savedSchools = new ArrayList<StudentSchoolRecord>();
        Set<String> reusedTranscriptKeys = new HashSet<String>();
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

            String schoolKey = buildSchoolKey(
                    school.schoolType,
                    school.schoolName,
                    school.startTime,
                    school.endTime
            );
            TranscriptBinding transcript = transcriptBySchoolKey.get(schoolKey);
            if (transcript != null) {
                schoolRecord.setTranscriptOriginalFilename(transcript.originalFilename);
                schoolRecord.setTranscriptContentType(transcript.contentType);
                schoolRecord.setTranscriptStorageKey(transcript.storageKey);
                schoolRecord.setTranscriptSizeBytes(transcript.sizeBytes);
                schoolRecord.setTranscriptUploadedAt(transcript.uploadedAt);
                reusedTranscriptKeys.add(schoolKey);
            }

            savedSchools.add(schoolRecord);
        }
        if (!savedSchools.isEmpty()) {
            savedSchools = studentSchoolRecordRepository.saveAll(savedSchools);
        }

        for (Map.Entry<String, TranscriptBinding> entry : transcriptBySchoolKey.entrySet()) {
            if (reusedTranscriptKeys.contains(entry.getKey())) {
                continue;
            }
            transcriptStorageService.deleteIfExists(entry.getValue().storageKey);
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

        return toDto(student, profile, savedSchools, savedCourses);
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
                                                                        MultipartFile file) {
        if (schoolRecordId == null || schoolRecordId.longValue() <= 0L) {
            throw new IllegalArgumentException("schoolRecordId must be positive");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("transcript file is required");
        }

        StudentSchoolRecord school = studentSchoolRecordRepository.findByIdAndStudent_Id(schoolRecordId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School record not found."));

        String previousStorageKey = trimToNull(school.getTranscriptStorageKey());
        StudentSchoolTranscriptStorageService.StoredTranscript stored =
                transcriptStorageService.store(student.getId(), school.getId(), file);

        school.setTranscriptOriginalFilename(stored.getOriginalFilename());
        school.setTranscriptContentType(stored.getContentType());
        school.setTranscriptStorageKey(stored.getStorageKey());
        school.setTranscriptSizeBytes(Long.valueOf(stored.getSizeBytes()));
        school.setTranscriptUploadedAt(LocalDateTime.now());
        school = studentSchoolRecordRepository.save(school);

        if (previousStorageKey != null && !previousStorageKey.equals(stored.getStorageKey())) {
            transcriptStorageService.deleteIfExists(previousStorageKey);
        }

        return toTranscriptDto(school);
    }

    private SchoolTranscriptDownload downloadSchoolTranscriptForStudent(Student student, Long schoolRecordId) {
        if (schoolRecordId == null || schoolRecordId.longValue() <= 0L) {
            throw new IllegalArgumentException("schoolRecordId must be positive");
        }

        StudentSchoolRecord school = studentSchoolRecordRepository.findByIdAndStudent_Id(schoolRecordId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School record not found."));
        String storageKey = trimToNull(school.getTranscriptStorageKey());
        if (storageKey == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transcript file not found.");
        }

        byte[] data = transcriptStorageService.readAllBytes(storageKey);
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

    private StudentSchoolTranscriptDto toTranscriptDto(StudentSchoolRecord school) {
        StudentSchoolTranscriptDto dto = new StudentSchoolTranscriptDto();
        dto.setSchoolRecordId(school.getId());
        dto.setTranscriptFileName(school.getTranscriptOriginalFilename());
        dto.setTranscriptContentType(school.getTranscriptContentType());
        dto.setTranscriptSizeBytes(school.getTranscriptSizeBytes());
        dto.setTranscriptUploadedAt(formatDateTime(school.getTranscriptUploadedAt()));
        dto.setHasTranscript(Boolean.valueOf(trimToNull(school.getTranscriptStorageKey()) != null));
        return dto;
    }

    private StudentProfileDto toDto(Student student,
                                    StudentProfile profile,
                                    List<StudentSchoolRecord> schools,
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
                schoolDto.setTranscriptFileName(school.getTranscriptOriginalFilename());
                schoolDto.setTranscriptSizeBytes(school.getTranscriptSizeBytes());
                schoolDto.setTranscriptUploadedAt(formatDateTime(school.getTranscriptUploadedAt()));
                schoolDto.setHasTranscript(Boolean.valueOf(trimToNull(school.getTranscriptStorageKey()) != null));
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

            schools.add(new NormalizedSchool(
                    schoolType,
                    schoolName,
                    schoolStreetAddress,
                    schoolCity,
                    schoolState,
                    schoolCountry,
                    schoolPostal,
                    startTime,
                    endTime
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

    private static class TranscriptBinding {
        private final String originalFilename;
        private final String contentType;
        private final String storageKey;
        private final Long sizeBytes;
        private final LocalDateTime uploadedAt;

        private TranscriptBinding(String originalFilename,
                                  String contentType,
                                  String storageKey,
                                  Long sizeBytes,
                                  LocalDateTime uploadedAt) {
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.storageKey = storageKey;
            this.sizeBytes = sizeBytes;
            this.uploadedAt = uploadedAt;
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

        private NormalizedSchool(SchoolType schoolType,
                                 String schoolName,
                                 String streetAddress,
                                 String city,
                                 String state,
                                 String country,
                                 String postal,
                                 LocalDate startTime,
                                 LocalDate endTime) {
            this.schoolType = schoolType;
            this.schoolName = schoolName;
            this.streetAddress = streetAddress;
            this.city = city;
            this.state = state;
            this.country = country;
            this.postal = postal;
            this.startTime = startTime;
            this.endTime = endTime;
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

