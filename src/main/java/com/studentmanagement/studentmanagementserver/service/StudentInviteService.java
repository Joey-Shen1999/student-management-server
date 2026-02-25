package com.studentmanagement.studentmanagementserver.service;

import com.studentmanagement.studentmanagementserver.domain.enums.StudentInviteStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.StudentInvite;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentInviteRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class StudentInviteService {

    private static final String REGISTER_PATH_PREFIX = "/register?inviteToken=";
    private static final long MAX_TTL_HOURS = 24L * 30L;

    private final StudentInviteRepository studentInviteRepository;
    private final TeacherRepository teacherRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long defaultInviteTtlHours;

    public StudentInviteService(StudentInviteRepository studentInviteRepository,
                                TeacherRepository teacherRepository,
                                @Value("${app.student-invite.ttl-hours:72}") long defaultInviteTtlHours) {
        this.studentInviteRepository = studentInviteRepository;
        this.teacherRepository = teacherRepository;
        this.defaultInviteTtlHours = defaultInviteTtlHours;
    }

    @Transactional
    public CreateStudentInviteResponse createInvite(User operator, Long requestedTeacherId, Long requestedExpiresHours) {
        Teacher teacher = resolveTeacherForInvite(operator, requestedTeacherId);
        long ttlHours = resolveTtlHours(requestedExpiresHours);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(ttlHours);
        String token = generateUniqueInviteToken();

        StudentInvite invite = new StudentInvite(teacher, token, expiresAt);
        studentInviteRepository.save(invite);
        return new CreateStudentInviteResponse(
                token,
                REGISTER_PATH_PREFIX + token,
                expiresAt.toString()
        );
    }

    @Transactional
    public PreviewStudentInviteResponse previewInvite(String tokenRaw) {
        String token = normalizeToken(tokenRaw);
        if (token == null) {
            return PreviewStudentInviteResponse.invalid();
        }

        StudentInvite invite = studentInviteRepository.findByInviteTokenWithTeacher(token).orElse(null);
        if (invite == null) {
            return PreviewStudentInviteResponse.invalid();
        }

        expireIfNeeded(invite);
        boolean valid = invite.getStatus() == StudentInviteStatus.PENDING;
        return new PreviewStudentInviteResponse(
                valid,
                invite.getStatus().name(),
                invite.getTeacher().getName(),
                invite.getExpiresAt() == null ? null : invite.getExpiresAt().toString()
        );
    }

    @Transactional
    public StudentInvite lockPendingInviteForRegistration(String tokenRaw) {
        String token = normalizeToken(tokenRaw);
        if (token == null) {
            throw StudentInviteException.invalid();
        }

        StudentInvite invite = studentInviteRepository.findByInviteTokenForUpdate(token)
                .orElseThrow(StudentInviteException::notFound);
        expireIfNeeded(invite);

        if (invite.getStatus() == StudentInviteStatus.PENDING) {
            return invite;
        }
        if (invite.getStatus() == StudentInviteStatus.EXPIRED) {
            throw StudentInviteException.expired();
        }
        if (invite.getStatus() == StudentInviteStatus.USED) {
            throw StudentInviteException.used();
        }
        throw StudentInviteException.invalid();
    }

    @Transactional
    public void markInviteUsed(StudentInvite invite, Long usedUserId) {
        invite.markUsed(usedUserId);
        studentInviteRepository.save(invite);
    }

    private Teacher resolveTeacherForInvite(User operator, Long requestedTeacherId) {
        if (operator.getRole() == UserRole.TEACHER) {
            return teacherRepository.findByUser_Id(operator.getId())
                    .orElseThrow(TeacherBindingRequiredException::new);
        }
        if (operator.getRole() == UserRole.ADMIN) {
            if (requestedTeacherId == null) {
                return teacherRepository.findByUser_Id(operator.getId())
                        .orElseThrow(TeacherBindingRequiredException::new);
            }
            return teacherRepository.findById(requestedTeacherId)
                    .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + requestedTeacherId));
        }
        throw new IllegalArgumentException("Forbidden: teacher/admin role required.");
    }

    private long resolveTtlHours(Long requestedExpiresHours) {
        long ttl = requestedExpiresHours == null ? defaultInviteTtlHours : requestedExpiresHours;
        if (ttl <= 0L || ttl > MAX_TTL_HOURS) {
            throw new IllegalArgumentException("expiresInHours must be between 1 and " + MAX_TTL_HOURS);
        }
        return ttl;
    }

    private void expireIfNeeded(StudentInvite invite) {
        if (invite.getStatus() == StudentInviteStatus.PENDING && invite.isExpiredAt(LocalDateTime.now())) {
            invite.markExpired();
            studentInviteRepository.save(invite);
        }
    }

    private String normalizeToken(String tokenRaw) {
        if (tokenRaw == null) {
            return null;
        }
        String token = tokenRaw.trim();
        return token.isEmpty() ? null : token;
    }

    private String generateUniqueInviteToken() {
        for (int i = 0; i < 20; i++) {
            byte[] bytes = new byte[32];
            secureRandom.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            if (!studentInviteRepository.existsByInviteToken(token)) {
                return token;
            }
        }
        throw new IllegalStateException("Unable to generate unique invite token.");
    }

    public static class CreateStudentInviteResponse {
        private final String inviteToken;
        private final String inviteUrl;
        private final String expiresAt;

        public CreateStudentInviteResponse(String inviteToken, String inviteUrl, String expiresAt) {
            this.inviteToken = inviteToken;
            this.inviteUrl = inviteUrl;
            this.expiresAt = expiresAt;
        }

        public String getInviteToken() {
            return inviteToken;
        }

        public String getInviteUrl() {
            return inviteUrl;
        }

        public String getExpiresAt() {
            return expiresAt;
        }
    }

    public static class PreviewStudentInviteResponse {
        private final boolean valid;
        private final String status;
        private final String teacherName;
        private final String expiresAt;

        public PreviewStudentInviteResponse(boolean valid, String status, String teacherName, String expiresAt) {
            this.valid = valid;
            this.status = status;
            this.teacherName = teacherName;
            this.expiresAt = expiresAt;
        }

        public static PreviewStudentInviteResponse invalid() {
            return new PreviewStudentInviteResponse(false, "INVALID", null, null);
        }

        public boolean isValid() {
            return valid;
        }

        public String getStatus() {
            return status;
        }

        public String getTeacherName() {
            return teacherName;
        }

        public String getExpiresAt() {
            return expiresAt;
        }
    }
}
