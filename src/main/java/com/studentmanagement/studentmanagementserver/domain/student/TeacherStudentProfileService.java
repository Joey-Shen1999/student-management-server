package com.studentmanagement.studentmanagementserver.domain.student;

import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.service.ManagementAccessService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

    public StudentProfileDto getProfile(Long studentId, HttpServletRequest request) {
        managementAccessService.requireStudentAccountManagementAccess(request);
        ensureValidStudentId(studentId);
        return studentProfileService.getProfileByStudentId(studentId);
    }

    public StudentProfileDto saveProfile(Long studentId,
                                         StudentProfileDto requestBody,
                                         HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        ensureValidStudentId(studentId);
        return studentProfileService.saveProfileByStudentId(
                studentId,
                requestBody,
                operator.getId(),
                resolveTraceId(request)
        );
    }

    public StudentSchoolTranscriptDto uploadSchoolTranscript(Long studentId,
                                                             Long schoolRecordId,
                                                             MultipartFile file,
                                                             HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        ensureValidStudentId(studentId);
        return studentProfileService.uploadStudentSchoolTranscriptByStudentId(
                studentId,
                schoolRecordId,
                file,
                operator.getId(),
                resolveTraceId(request)
        );
    }

    public StudentProfileService.SchoolTranscriptDownload downloadSchoolTranscript(Long studentId,
                                                                                   Long schoolRecordId,
                                                                                   HttpServletRequest request) {
        managementAccessService.requireStudentAccountManagementAccess(request);
        ensureValidStudentId(studentId);
        return studentProfileService.downloadStudentSchoolTranscriptByStudentId(studentId, schoolRecordId);
    }

    public StudentProfileService.SchoolTranscriptDownload downloadSchoolTranscriptByTranscriptId(Long studentId,
                                                                                                  Long schoolRecordId,
                                                                                                  Long transcriptId,
                                                                                                  HttpServletRequest request) {
        managementAccessService.requireStudentAccountManagementAccess(request);
        ensureValidStudentId(studentId);
        return studentProfileService.downloadStudentSchoolTranscriptByStudentIdAndTranscriptId(
                studentId,
                schoolRecordId,
                transcriptId
        );
    }

    public StudentIdentityFileUploadDto uploadIdentityFile(Long studentId,
                                                           MultipartFile file,
                                                           HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        ensureValidStudentId(studentId);
        return studentProfileService.uploadStudentIdentityFileByStudentId(
                studentId,
                file,
                operator.getId(),
                resolveTraceId(request)
        );
    }

    public StudentProfileService.IdentityFileDownload downloadIdentityFileByIdentityFileId(Long studentId,
                                                                                            Long identityFileId,
                                                                                            HttpServletRequest request) {
        managementAccessService.requireStudentAccountManagementAccess(request);
        ensureValidStudentId(studentId);
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
}
