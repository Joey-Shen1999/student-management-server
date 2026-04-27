package com.studentmanagement.studentmanagementserver.domain.student;

import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherStudentRepository;
import com.studentmanagement.studentmanagementserver.service.ManagementAccessService;
import com.studentmanagement.studentmanagementserver.service.TeacherBindingRequiredException;
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
    private final TeacherRepository teacherRepository;
    private final TeacherStudentRepository teacherStudentRepository;

    public TeacherStudentDocumentService(ManagementAccessService managementAccessService,
                                         StudentDocumentService studentDocumentService,
                                         TeacherRepository teacherRepository,
                                         TeacherStudentRepository teacherStudentRepository) {
        this.managementAccessService = managementAccessService;
        this.studentDocumentService = studentDocumentService;
        this.teacherRepository = teacherRepository;
        this.teacherStudentRepository = teacherStudentRepository;
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
}
