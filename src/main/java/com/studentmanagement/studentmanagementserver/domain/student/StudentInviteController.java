package com.studentmanagement.studentmanagementserver.domain.student;

import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.service.ManagementAccessService;
import com.studentmanagement.studentmanagementserver.service.StudentInviteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/teacher/student-invites")
public class StudentInviteController {

    private final ManagementAccessService managementAccessService;
    private final StudentInviteService studentInviteService;

    public StudentInviteController(ManagementAccessService managementAccessService,
                                   StudentInviteService studentInviteService) {
        this.managementAccessService = managementAccessService;
        this.studentInviteService = studentInviteService;
    }

    @PostMapping
    public ResponseEntity<StudentInviteService.CreateStudentInviteResponse> createInvite(
            @RequestBody(required = false) CreateStudentInviteRequest req,
            HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        Long teacherId = req == null ? null : req.getTeacherId();
        Long expiresInHours = req == null ? null : req.getExpiresInHours();
        StudentInviteService.CreateStudentInviteResponse response =
                studentInviteService.createInvite(operator, teacherId, expiresInHours);
        return ResponseEntity.ok(response);
    }

    public static class CreateStudentInviteRequest {
        private Long teacherId;
        private Long expiresInHours;

        public Long getTeacherId() {
            return teacherId;
        }

        public void setTeacherId(Long teacherId) {
            this.teacherId = teacherId;
        }

        public Long getExpiresInHours() {
            return expiresInHours;
        }

        public void setExpiresInHours(Long expiresInHours) {
            this.expiresInHours = expiresInHours;
        }
    }
}
