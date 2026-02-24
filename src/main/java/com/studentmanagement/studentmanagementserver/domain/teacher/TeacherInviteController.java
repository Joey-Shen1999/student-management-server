package com.studentmanagement.studentmanagementserver.domain.teacher;

import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.service.ManagementAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/teacher/invites")
public class TeacherInviteController {

    private final TeacherInviteService teacherInviteService;
    private final ManagementAccessService managementAccessService;

    public TeacherInviteController(TeacherInviteService teacherInviteService,
                                   ManagementAccessService managementAccessService) {
        this.teacherInviteService = teacherInviteService;
        this.managementAccessService = managementAccessService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateTeacherRequest req, HttpServletRequest request) {
        User operator = managementAccessService.requireTeacherManagementAccess(request);

        TeacherInviteService.CreateTeacherInviteResponse response =
                teacherInviteService.createTeacher(
                        req.getUsername(),
                        req.getDisplayName(),
                        operator
                );

        return ResponseEntity.ok(response);
    }

    public static class CreateTeacherRequest {
        private String username;
        private String displayName;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
}
