package com.studentmanagement.studentmanagementserver.domain.student;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.service.ManagementAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Service
public class TeacherStudentDocumentService {

    private final ManagementAccessService managementAccessService;
    private final StudentDocumentService studentDocumentService;

    public TeacherStudentDocumentService(ManagementAccessService managementAccessService,
                                         StudentDocumentService studentDocumentService) {
        this.managementAccessService = managementAccessService;
        this.studentDocumentService = studentDocumentService;
    }

    public List<StudentDocumentDto> listDocuments(Long studentId, HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        ensureValidStudentId(studentId);
        ensureTeacherCanAccessStudent(operator, studentId);
        return studentDocumentService.listDocumentsByStudentId(studentId);
    }

    public StudentDocumentHistoryListDto listDocumentHistory(Long studentId,
                                                             Integer page,
                                                             Integer size,
                                                             HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        ensureValidStudentId(studentId);
        ensureTeacherCanAccessStudent(operator, studentId);
        return studentDocumentService.listDocumentHistoryByStudentId(studentId, page, size);
    }

    public StudentDocumentDto uploadDocument(Long studentId,
                                             MultipartFile file,
                                             String documentCategory,
                                             String identityDocumentType,
                                             String academicRecordType,
                                             Integer reportYear,
                                             String reportMonth,
                                             String title,
                                             String notes,
                                             HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        ensureValidStudentId(studentId);
        ensureTeacherCanAccessStudent(operator, studentId);
        return studentDocumentService.uploadDocumentForStudentId(
                studentId,
                file,
                documentCategory,
                identityDocumentType,
                academicRecordType,
                reportYear,
                reportMonth,
                title,
                notes,
                operator.getId()
        );
    }

    public StudentDocumentService.DocumentDownload downloadDocument(Long studentId,
                                                                    Long documentId,
                                                                    HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        ensureValidStudentId(studentId);
        ensureTeacherCanAccessStudent(operator, studentId);
        return studentDocumentService.downloadDocumentForStudentId(studentId, documentId);
    }

    public void deleteDocument(Long studentId, Long documentId, HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        ensureValidStudentId(studentId);
        ensureTeacherCanAccessStudent(operator, studentId);
        studentDocumentService.deleteDocumentForStudentId(studentId, documentId, operator.getId());
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
