package com.studentmanagement.studentmanagementserver.domain.student;

import com.studentmanagement.studentmanagementserver.domain.enums.SchoolType;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentCourseRecordRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentProfileRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class StudentProfileService {

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private final AuthSessionService authSessionService;
    private final StudentRepository studentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentCourseRecordRepository studentCourseRecordRepository;

    public StudentProfileService(AuthSessionService authSessionService,
                                 StudentRepository studentRepository,
                                 StudentProfileRepository studentProfileRepository,
                                 StudentCourseRecordRepository studentCourseRecordRepository) {
        this.authSessionService = authSessionService;
        this.studentRepository = studentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentCourseRecordRepository = studentCourseRecordRepository;
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
        List<StudentCourseRecord> courses = studentCourseRecordRepository.findByStudent_IdOrderByIdAsc(student.getId());
        return toDto(student, profile, courses);
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

        studentCourseRecordRepository.deleteByStudent_Id(student.getId());
        List<StudentCourseRecord> savedCourses = new ArrayList<StudentCourseRecord>();
        for (NormalizedCourse course : normalized.otherCourses) {
            savedCourses.add(new StudentCourseRecord(
                    student,
                    course.schoolType,
                    course.schoolName,
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

        return toDto(student, profile, savedCourses);
    }

    private void applyProfile(StudentProfile profile, NormalizedProfile normalized, Long operatorUserId) {
        profile.setGender(normalized.gender);
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

    private StudentProfileDto toDto(Student student, StudentProfile profile, List<StudentCourseRecord> courses) {
        StudentProfileDto dto = new StudentProfileDto();

        dto.setLegalFirstName(student.getFirstName());
        dto.setLegalLastName(student.getLastName());
        dto.setPreferredName(student.getNickName());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setNickName(student.getNickName());
        dto.setAp(Boolean.FALSE);

        if (profile != null) {
            dto.setGender(profile.getGender());
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

        List<StudentProfileDto.CourseDto> courseDtos = new ArrayList<StudentProfileDto.CourseDto>();
        if (courses != null) {
            for (StudentCourseRecord course : courses) {
                StudentProfileDto.CourseDto courseDto = new StudentProfileDto.CourseDto();
                courseDto.setSchoolType(course.getSchoolType() == null ? null : course.getSchoolType().name());
                courseDto.setSchoolName(course.getSchoolName());
                courseDto.setCourseCode(course.getCourseCode());
                courseDto.setMark(course.getMark());
                courseDto.setGradeLevel(course.getGradeLevel());
                courseDto.setStartTime(formatDate(course.getStartTime()));
                courseDto.setEndTime(formatDate(course.getEndTime()));
                courseDtos.add(courseDto);
            }
        }
        dto.setOtherCourses(courseDtos);

        return dto;
    }

    private String formatDate(LocalDate value) {
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
        String gender = trimToNull(requestBody.getGender());
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

        List<NormalizedCourse> courses = new ArrayList<NormalizedCourse>();
        List<StudentProfileDto.CourseDto> incomingCourses = requestBody.getOtherCourses();
        for (int i = 0; i < incomingCourses.size(); i++) {
            StudentProfileDto.CourseDto incomingCourse = incomingCourses.get(i);
            String pathPrefix = "otherCourses[" + i + "]";
            if (incomingCourse == null) {
                throw new IllegalArgumentException(pathPrefix + " is required");
            }

            SchoolType schoolType = parseSchoolType(incomingCourse.getSchoolType(), pathPrefix + ".schoolType");
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

            courses.add(new NormalizedCourse(
                    schoolType,
                    trimToNull(incomingCourse.getSchoolName()),
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
                gender,
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
                courses
        );
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

    private static class NormalizedProfile {
        private final String legalFirstName;
        private final String legalLastName;
        private final String preferredName;
        private final String gender;
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
        private final List<NormalizedCourse> otherCourses;

        private NormalizedProfile(String legalFirstName,
                                  String legalLastName,
                                  String preferredName,
                                  String gender,
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
                                  List<NormalizedCourse> otherCourses) {
            this.legalFirstName = legalFirstName;
            this.legalLastName = legalLastName;
            this.preferredName = preferredName;
            this.gender = gender;
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

    private static class NormalizedCourse {
        private final SchoolType schoolType;
        private final String schoolName;
        private final String courseCode;
        private final Integer mark;
        private final Integer gradeLevel;
        private final LocalDate startTime;
        private final LocalDate endTime;

        private NormalizedCourse(SchoolType schoolType,
                                 String schoolName,
                                 String courseCode,
                                 Integer mark,
                                 Integer gradeLevel,
                                 LocalDate startTime,
                                 LocalDate endTime) {
            this.schoolType = schoolType;
            this.schoolName = schoolName;
            this.courseCode = courseCode;
            this.mark = mark;
            this.gradeLevel = gradeLevel;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
}
