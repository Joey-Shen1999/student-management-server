package com.studentmanagement.studentmanagementserver.domain.student;

import com.studentmanagement.studentmanagementserver.domain.enums.SchoolType;
import com.studentmanagement.studentmanagementserver.domain.enums.UserAccountStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.domain.volunteer.StudentVolunteerTracking;
import com.studentmanagement.studentmanagementserver.repo.GraduationApplicationRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentProfileRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentSchoolRecordRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentVolunteerTrackingRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import com.studentmanagement.studentmanagementserver.repo.UserSessionRepository;
import com.studentmanagement.studentmanagementserver.service.TemporaryPasswordGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class StudentAccountService {

    private static final BigDecimal VOLUNTEER_COMPLETED_THRESHOLD = new BigDecimal("40");

    private final StudentRepository studentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentSchoolRecordRepository studentSchoolRecordRepository;
    private final StudentVolunteerTrackingRepository studentVolunteerTrackingRepository;
    private final GraduationApplicationRepository graduationApplicationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final UserSessionRepository userSessionRepository;

    public StudentAccountService(StudentRepository studentRepository,
                                 StudentProfileRepository studentProfileRepository,
                                 StudentSchoolRecordRepository studentSchoolRecordRepository,
                                 StudentVolunteerTrackingRepository studentVolunteerTrackingRepository,
                                 GraduationApplicationRepository graduationApplicationRepository,
                                 UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 TemporaryPasswordGenerator temporaryPasswordGenerator,
                                 UserSessionRepository userSessionRepository) {
        this.studentRepository = studentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentSchoolRecordRepository = studentSchoolRecordRepository;
        this.studentVolunteerTrackingRepository = studentVolunteerTrackingRepository;
        this.graduationApplicationRepository = graduationApplicationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
        this.userSessionRepository = userSessionRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentAccountItem> listStudentAccounts() {
        return buildStudentAccountItems(studentRepository.findAllWithUser());
    }

    @Transactional(readOnly = true)
    public List<StudentAccountItem> listStudentAccounts(User operator) {
        if (operator == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }
        if (operator.getRole() == UserRole.ADMIN || operator.getRole() == UserRole.TEACHER) {
            return buildStudentAccountItems(studentRepository.findAllWithUser());
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
    }

    private List<StudentAccountItem> buildStudentAccountItems(List<Student> students) {
        if (students.isEmpty()) {
            return Collections.emptyList();
        }

        List<Student> sortedStudents = new ArrayList<Student>(students);
        sortedStudents.sort(Comparator.comparing(Student::getId));
        List<Long> studentIds = new ArrayList<Long>(sortedStudents.size());
        for (Student student : sortedStudents) {
            if (student != null && student.getId() != null) {
                studentIds.add(student.getId());
            }
        }

        Map<Long, StudentProfile> profileByStudentId = findProfilesByStudentIds(studentIds);
        Map<Long, StudentSchoolRecord> primarySchoolByStudentId = findPrimarySchoolByStudentIds(studentIds);
        Map<Long, BigDecimal> volunteerHoursByStudentId = findVolunteerHoursByStudentIds(studentIds);
        Map<Long, Integer> graduationApplicationCountByStudentId = findGraduationApplicationCountsByStudentIds(studentIds);

        List<StudentAccountItem> result = new ArrayList<StudentAccountItem>(sortedStudents.size());
        for (Student student : sortedStudents) {
            if (student == null || student.getId() == null || student.getUser() == null) {
                continue;
            }
            Long studentId = student.getId();
            User user = student.getUser();
            StudentProfile profile = profileByStudentId.get(studentId);
            StudentSchoolRecord primarySchool = primarySchoolByStudentId.get(studentId);

            String country = firstNonBlank(
                    primarySchool == null ? null : primarySchool.getCountry(),
                    profile == null ? null : profile.getCountry()
            );
            String province = firstNonBlank(
                    primarySchool == null ? null : primarySchool.getState(),
                    profile == null ? null : profile.getState()
            );
            String city = firstNonBlank(
                    primarySchool == null ? null : primarySchool.getCity(),
                    profile == null ? null : profile.getCity()
            );
            BigDecimal totalVolunteerHours = volunteerHoursByStudentId.get(studentId);
            if (totalVolunteerHours == null) {
                totalVolunteerHours = BigDecimal.ZERO;
            }
            boolean volunteerCompleted = totalVolunteerHours.compareTo(VOLUNTEER_COMPLETED_THRESHOLD) >= 0;
            int graduationApplicationCount = graduationApplicationCountByStudentId.containsKey(studentId)
                    ? graduationApplicationCountByStudentId.get(studentId)
                    : 0;
            UserAccountStatus accountStatus = user.getStatus();
            boolean selectable = accountStatus == UserAccountStatus.ACTIVE;
            List<String> serviceItems = StudentServiceItemNormalizer.normalizeStored(
                    profile == null ? null : profile.getServiceItems()
            );

            result.add(new StudentAccountItem(
                    studentId,
                    buildStudentDisplayName(student),
                    user.getUsername(),
                    user.getRole(),
                    accountStatus,
                    student.getFirstName(),
                    student.getLastName(),
                    student.getNickName(),
                    profile == null ? null : trimToNull(profile.getEmail()),
                    profile == null ? null : trimToNull(profile.getPhone()),
                    formatGraduation(primarySchool == null ? null : primarySchool.getEndTime()),
                    primarySchool == null ? null : trimToNull(primarySchool.getSchoolName()),
                    profile == null ? null : trimToNull(profile.getStatusInCanada()),
                    primarySchool == null ? null : trimToNull(primarySchool.getSchoolBoard()),
                    country,
                    province,
                    city,
                    serviceItems,
                    profile == null ? null : trimToNull(profile.getTeacherNote()),
                    selectable,
                    totalVolunteerHours,
                    volunteerCompleted,
                    graduationApplicationCount > 0,
                    graduationApplicationCount
            ));
        }
        return result;
    }

    private String buildStudentDisplayName(Student student) {
        String nickname = trimToNull(student.getNickName());
        if (nickname != null) {
            return nickname;
        }

        String firstName = trimToNull(student.getFirstName());
        String lastName = trimToNull(student.getLastName());
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        if (firstName != null) {
            return firstName;
        }
        if (lastName != null) {
            return lastName;
        }
        if (student.getUser() != null) {
            String username = trimToNull(student.getUser().getUsername());
            if (username != null) {
                return username;
            }
        }
        return "Student #" + student.getId();
    }

    private Map<Long, StudentProfile> findProfilesByStudentIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<StudentProfile> profiles = studentProfileRepository.findByStudentIdsWithStudent(studentIds);
        if (profiles.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, StudentProfile> profileByStudentId = new HashMap<Long, StudentProfile>();
        for (StudentProfile profile : profiles) {
            if (profile == null || profile.getStudent() == null || profile.getStudent().getId() == null) {
                continue;
            }
            profileByStudentId.put(profile.getStudent().getId(), profile);
        }
        return profileByStudentId;
    }

    private Map<Long, StudentSchoolRecord> findPrimarySchoolByStudentIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<StudentSchoolRecord> schools =
                studentSchoolRecordRepository.findByStudent_IdInOrderByStudent_IdAscIdAsc(studentIds);
        if (schools.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, StudentSchoolRecord> schoolByStudentId = new HashMap<Long, StudentSchoolRecord>();
        for (StudentSchoolRecord school : schools) {
            if (school == null || school.getStudent() == null || school.getStudent().getId() == null) {
                continue;
            }
            Long studentId = school.getStudent().getId();
            StudentSchoolRecord current = schoolByStudentId.get(studentId);
            if (shouldReplacePrimarySchool(current, school)) {
                schoolByStudentId.put(studentId, school);
            }
        }
        return schoolByStudentId;
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

    private Map<Long, BigDecimal> findVolunteerHoursByStudentIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<StudentVolunteerTracking> trackings = studentVolunteerTrackingRepository.findByStudent_IdIn(studentIds);
        if (trackings.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, BigDecimal> result = new HashMap<Long, BigDecimal>();
        for (StudentVolunteerTracking tracking : trackings) {
            if (tracking == null || tracking.getStudent() == null || tracking.getStudent().getId() == null) {
                continue;
            }
            Long studentId = tracking.getStudent().getId();
            BigDecimal totalHours = tracking.getTotalHours();
            result.put(studentId, totalHours == null ? BigDecimal.ZERO : totalHours);
        }
        return result;
    }

    private Map<Long, Integer> findGraduationApplicationCountsByStudentIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<GraduationApplicationRepository.StudentApplicationCountView> rows =
                graduationApplicationRepository.countByStudentIds(studentIds);
        if (rows.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Integer> result = new HashMap<Long, Integer>();
        for (GraduationApplicationRepository.StudentApplicationCountView row : rows) {
            if (row == null || row.getStudentId() == null) {
                continue;
            }
            Long count = row.getApplicationCount();
            result.put(row.getStudentId(), count == null ? 0 : count.intValue());
        }
        return result;
    }

    private String firstNonBlank(String first, String second) {
        String firstValue = trimToNull(first);
        if (firstValue != null) {
            return firstValue;
        }
        return trimToNull(second);
    }

    private String formatGraduation(LocalDate graduationDate) {
        if (graduationDate == null) {
            return null;
        }
        int month = graduationDate.getMonthValue();
        String monthText = month < 10 ? "0" + month : String.valueOf(month);
        return graduationDate.getYear() + "-" + monthText;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Transactional
    public ResetStudentPasswordResponse resetStudentPassword(Long studentId, User operator) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student account not found: " + studentId
                ));
        ensureCanManageStudentAccount(operator, studentId);

        User targetUser = student.getUser();
        String tempPassword = temporaryPasswordGenerator.generate(targetUser.getUsername());
        targetUser.setPasswordHash(passwordEncoder.encode(tempPassword));
        targetUser.setMustChangePassword(true);
        userRepository.save(targetUser);
        userSessionRepository.revokeAllActiveSessions(targetUser.getId(), LocalDateTime.now());

        return new ResetStudentPasswordResponse(
                student.getId(),
                targetUser.getUsername(),
                tempPassword,
                "Password reset successfully"
        );
    }

    @Transactional
    public UpdateStudentStatusResponse updateStudentStatus(Long studentId, String statusRaw, User operator) {
        UserAccountStatus targetStatus = parseStudentAccountStatus(statusRaw);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student account not found: " + studentId
                ));
        ensureCanManageStudentAccount(operator, studentId);

        User targetUser = student.getUser();
        targetUser.updateStatus(targetStatus, operator == null ? null : operator.getId());
        userRepository.save(targetUser);
        if (targetStatus == UserAccountStatus.ARCHIVED) {
            userSessionRepository.revokeAllActiveSessions(targetUser.getId(), LocalDateTime.now());
        }

        return new UpdateStudentStatusResponse(
                student.getId(),
                targetUser.getUsername(),
                targetStatus
        );
    }

    private UserAccountStatus parseStudentAccountStatus(String statusRaw) {
        if (statusRaw == null || statusRaw.trim().isEmpty()) {
            throw new IllegalArgumentException("status is required");
        }
        try {
            return UserAccountStatus.valueOf(statusRaw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid account status. Expected ACTIVE or ARCHIVED.");
        }
    }

    private void ensureCanManageStudentAccount(User operator, Long studentId) {
        if (operator == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }
        if (operator.getRole() == UserRole.ADMIN || operator.getRole() == UserRole.TEACHER) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
    }

    public static class StudentAccountItem {
        private Long studentId;
        private String studentName;
        private String username;
        private UserRole role;
        private UserAccountStatus status;
        private String firstName;
        private String lastName;
        private String nickName;
        private String email;
        private String phone;
        private String graduation;
        private String schoolName;
        private String canadaIdentity;
        private String schoolBoard;
        private String country;
        private String province;
        private String city;
        private List<String> serviceItems;
        private String teacherNote;
        private boolean selectable;
        private BigDecimal totalVolunteerHours;
        private boolean volunteerCompleted;
        private boolean graduationStageEnabled;
        private int graduationApplicationCount;

        public StudentAccountItem(Long studentId,
                                  String studentName,
                                  String username,
                                  UserRole role,
                                  UserAccountStatus status,
                                  String firstName,
                                  String lastName,
                                  String nickName,
                                  String email,
                                  String phone,
                                  String graduation,
                                  String schoolName,
                                  String canadaIdentity,
                                  String schoolBoard,
                                  String country,
                                  String province,
                                  String city,
                                  List<String> serviceItems,
                                  String teacherNote,
                                  boolean selectable,
                                  BigDecimal totalVolunteerHours,
                                  boolean volunteerCompleted,
                                  boolean graduationStageEnabled,
                                  int graduationApplicationCount) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.username = username;
            this.role = role;
            this.status = status;
            this.firstName = firstName;
            this.lastName = lastName;
            this.nickName = nickName;
            this.email = email;
            this.phone = phone;
            this.graduation = graduation;
            this.schoolName = schoolName;
            this.canadaIdentity = canadaIdentity;
            this.schoolBoard = schoolBoard;
            this.country = country;
            this.province = province;
            this.city = city;
            this.serviceItems = serviceItems == null
                    ? new ArrayList<String>()
                    : new ArrayList<String>(serviceItems);
            this.teacherNote = teacherNote;
            this.selectable = selectable;
            this.totalVolunteerHours = totalVolunteerHours;
            this.volunteerCompleted = volunteerCompleted;
            this.graduationStageEnabled = graduationStageEnabled;
            this.graduationApplicationCount = graduationApplicationCount;
        }

        public Long getStudentId() {
            return studentId;
        }

        public String getStudentName() {
            return studentName;
        }

        public String getUsername() {
            return username;
        }

        public UserRole getRole() {
            return role;
        }

        public UserAccountStatus getStatus() {
            return status;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getNickName() {
            return nickName;
        }

        public String getEmail() {
            return email;
        }

        public String getPhone() {
            return phone;
        }

        public String getGraduation() {
            return graduation;
        }

        public String getSchoolName() {
            return schoolName;
        }

        public String getCanadaIdentity() {
            return canadaIdentity;
        }

        public String getSchoolBoard() {
            return schoolBoard;
        }

        public String getCountry() {
            return country;
        }

        public String getProvince() {
            return province;
        }

        public String getCity() {
            return city;
        }

        public List<String> getServiceItems() {
            return serviceItems;
        }

        public String getTeacherNote() {
            return teacherNote;
        }

        public boolean isSelectable() {
            return selectable;
        }

        public BigDecimal getTotalVolunteerHours() {
            return totalVolunteerHours;
        }

        public boolean isVolunteerCompleted() {
            return volunteerCompleted;
        }

        public boolean isGraduationStageEnabled() {
            return graduationStageEnabled;
        }

        public int getGraduationApplicationCount() {
            return graduationApplicationCount;
        }
    }

    public static class ResetStudentPasswordResponse {
        private Long studentId;
        private String username;
        private String tempPassword;
        private String message;

        public ResetStudentPasswordResponse(Long studentId, String username, String tempPassword, String message) {
            this.studentId = studentId;
            this.username = username;
            this.tempPassword = tempPassword;
            this.message = message;
        }

        public Long getStudentId() {
            return studentId;
        }

        public String getUsername() {
            return username;
        }

        public String getTempPassword() {
            return tempPassword;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class UpdateStudentStatusResponse {
        private Long studentId;
        private String username;
        private UserAccountStatus status;

        public UpdateStudentStatusResponse(Long studentId, String username, UserAccountStatus status) {
            this.studentId = studentId;
            this.username = username;
            this.status = status;
        }

        public Long getStudentId() {
            return studentId;
        }

        public String getUsername() {
            return username;
        }

        public UserAccountStatus getStatus() {
            return status;
        }
    }
}
