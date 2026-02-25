package com.studentmanagement.studentmanagementserver.api;

import com.studentmanagement.studentmanagementserver.service.StudentInviteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/student-invites")
public class StudentInviteAuthController {

    private final StudentInviteService studentInviteService;

    public StudentInviteAuthController(StudentInviteService studentInviteService) {
        this.studentInviteService = studentInviteService;
    }

    @GetMapping("/{inviteToken}")
    public ResponseEntity<StudentInviteService.PreviewStudentInviteResponse> previewInvite(
            @PathVariable String inviteToken) {
        StudentInviteService.PreviewStudentInviteResponse response =
                studentInviteService.previewInvite(inviteToken);
        return ResponseEntity.ok(response);
    }
}
