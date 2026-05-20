package com.studentmanagement.studentmanagementserver.domain.student;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.service.ManagementAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

@Service
public class TeacherStudentProfileService {

    private final ManagementAccessService managementAccessService;
    private final StudentProfileService studentProfileService;

    public TeacherStudentProfileService(ManagementAccessService managementAccessService,
                                        StudentProfileService studentProfileService) {
        this.managementAccessService = managementAccessService;
        this.studentProfileService = studentProfileService;
    }

    public TeacherStudentProfileDto getProfile(Long studentId, HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        ensureValidStudentId(studentId);
        ensureTeacherCanAccessStudent(operator, studentId);
        return studentProfileService.getProfileByStudentIdForTeacher(studentId);
    }

    public TeacherStudentProfileDto saveProfile(Long studentId,
                                                TeacherStudentProfileDto requestBody,
                                                String ifMatch,
                                                String changeSource,
                                                HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        ensureValidStudentId(studentId);
        ensureTeacherCanAccessStudent(operator, studentId);
        return studentProfileService.saveProfileByStudentIdForTeacher(
                studentId,
                requestBody,
                operator.getId(),
                resolveTraceId(request),
                ifMatch,
                changeSource
        );
    }

    public StudentProfileHistoryListDto getProfileHistory(Long studentId,
                                                          Integer page,
                                                          Integer size,
                                                          HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        ensureValidStudentId(studentId);
        ensureTeacherCanAccessStudent(operator, studentId);
        return studentProfileService.getProfileHistoryByStudentId(studentId, page, size);
    }

    public StudentSchoolTranscriptDto uploadSchoolTranscript(Long studentId,
                                                             Long schoolRecordId,
                                                             MultipartFile file,
                                                             String academicRecordType,
                                                             Integer reportYear,
                                                             String reportMonth,
                                                             HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        ensureValidStudentId(studentId);
        ensureTeacherCanAccessStudent(operator, studentId);
        return studentProfileService.uploadStudentSchoolTranscriptByStudentId(
                studentId,
                schoolRecordId,
                file,
                academicRecordType,
                reportYear,
                reportMonth,
                operator.getId(),
                resolveTraceId(request)
        );
    }

    public StudentProfileService.SchoolTranscriptDownload downloadSchoolTranscript(Long studentId,
                                                                                   Long schoolRecordId,
                                                                                   HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        ensureValidStudentId(studentId);
        ensureTeacherCanAccessStudent(operator, studentId);
        return studentProfileService.downloadStudentSchoolTranscriptByStudentId(studentId, schoolRecordId);
    }

    public StudentProfileService.SchoolTranscriptDownload downloadSchoolTranscriptByTranscriptId(Long studentId,
                                                                                                  Long schoolRecordId,
                                                                                                  Long transcriptId,
                                                                                                  HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        ensureValidStudentId(studentId);
        ensureTeacherCanAccessStudent(operator, studentId);
        return studentProfileService.downloadStudentSchoolTranscriptByStudentIdAndTranscriptId(
                studentId,
                schoolRecordId,
                transcriptId
        );
    }

    public StudentIdentityFileUploadDto uploadIdentityFile(Long studentId,
                                                           MultipartFile file,
                                                           String identityDocumentType,
                                                           HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        ensureValidStudentId(studentId);
        ensureTeacherCanAccessStudent(operator, studentId);
        return studentProfileService.uploadStudentIdentityFileByStudentId(
                studentId,
                file,
                identityDocumentType,
                operator.getId(),
                resolveTraceId(request)
        );
    }

    public StudentProfileService.IdentityFileDownload downloadIdentityFileByIdentityFileId(Long studentId,
                                                                                            Long identityFileId,
                                                                                            HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        ensureValidStudentId(studentId);
        ensureTeacherCanAccessStudent(operator, studentId);
        return studentProfileService.downloadStudentIdentityFileByStudentIdAndIdentityFileId(studentId, identityFileId);
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request == null ? null : request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.trim().isEmpty()) {
            return "N/A";
        }
        return traceId.trim();
    }

    private void ensureValidStudentId(Long studentId) {
        if (studentId == null || studentId.longValue() <= 0L) {
            throw new IllegalArgumentException("studentId must be positive");
        }
    }

    private void ensureTeacherCanAccessStudent(User operator, Long studentId) {
        if (operator.getRole() == UserRole.ADMIN || operator.getRole() == UserRole.TEACHER) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
    }
}
