package com.studentmanagement.studentmanagementserver.domain.teacher;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher/invites")
public class TeacherInviteController {

    private final TeacherInviteService teacherInviteService;

    public TeacherInviteController(TeacherInviteService teacherInviteService) {
        this.teacherInviteService = teacherInviteService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateTeacherRequest req) {

        TeacherInviteService.CreateTeacherInviteResponse response =
                teacherInviteService.createTeacher(
                        req.getUsername(),
                        req.getUsername()   // 默认 name = username
                );

        return ResponseEntity.ok(response);
    }

    public static class CreateTeacherRequest {
        private String username;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }
}
