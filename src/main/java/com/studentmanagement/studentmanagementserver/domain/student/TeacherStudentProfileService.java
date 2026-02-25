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
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

@Service
public class TeacherStudentProfileService {

    private final ManagementAccessService managementAccessService;
    private final TeacherRepository teacherRepository;
    private final TeacherStudentRepository teacherStudentRepository;
    private final StudentProfileService studentProfileService;

    public TeacherStudentProfileService(ManagementAccessService managementAccessService,
                                        TeacherRepository teacherRepository,
                                        TeacherStudentRepository teacherStudentRepository,
                                        StudentProfileService studentProfileService) {
        this.managementAccessService = managementAccessService;
        this.teacherRepository = teacherRepository;
        this.teacherStudentRepository = teacherStudentRepository;
        this.studentProfileService = studentProfileService;
    }

    public StudentProfileDto getProfile(Long studentId, HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        ensureCanAccessStudent(operator, studentId);
        return studentProfileService.getProfileByStudentId(studentId);
    }

    public StudentProfileDto saveProfile(Long studentId,
                                         StudentProfileDto requestBody,
                                         HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        ensureCanAccessStudent(operator, studentId);
        return studentProfileService.saveProfileByStudentId(studentId, requestBody, operator.getId());
    }

    private void ensureCanAccessStudent(User operator, Long studentId) {
        if (studentId == null || studentId.longValue() <= 0L) {
            throw new IllegalArgumentException("studentId must be positive");
        }
        if (operator.getRole() == UserRole.ADMIN) {
            return;
        }
        if (operator.getRole() != UserRole.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }

        Teacher teacher = teacherRepository.findByUser_Id(operator.getId())
                .orElseThrow(TeacherBindingRequiredException::new);
        boolean hasActiveRelation = teacherStudentRepository.existsByTeacher_IdAndStudent_IdAndStatus(
                teacher.getId(),
                studentId,
                TeacherStudentStatus.ACTIVE
        );
        if (!hasActiveRelation) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Forbidden: student is not actively assigned to this teacher."
            );
        }
    }
}
