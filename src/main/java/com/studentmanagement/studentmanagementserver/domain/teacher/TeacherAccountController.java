package com.studentmanagement.studentmanagementserver.domain.teacher;

import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.service.ManagementAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/accounts")
public class TeacherAccountController {

    private final TeacherAccountService teacherAccountService;
    private final ManagementAccessService managementAccessService;

    public TeacherAccountController(TeacherAccountService teacherAccountService,
                                    ManagementAccessService managementAccessService) {
        this.teacherAccountService = teacherAccountService;
        this.managementAccessService = managementAccessService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(HttpServletRequest request) {
        managementAccessService.requireTeacherManagementAccess(request);
        List<TeacherAccountService.TeacherAccountItem> accounts = teacherAccountService.listTeacherAccounts();

        Map<String, Object> response = new HashMap<String, Object>();
        response.put("data", accounts);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{teacherId}/reset-password")
    public ResponseEntity<TeacherAccountService.ResetTeacherPasswordResponse> resetPassword(
            @PathVariable Long teacherId,
            @RequestBody(required = false) Map<String, Object> ignoredBody,
            HttpServletRequest request) {
        User operator = managementAccessService.requireTeacherManagementAccess(request);
        TeacherAccountService.ResetTeacherPasswordResponse response =
                teacherAccountService.resetTeacherPassword(teacherId, operator);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{teacherId}/role")
    public ResponseEntity<TeacherAccountService.UpdateTeacherRoleResponse> updateRole(
            @PathVariable Long teacherId,
            @RequestBody(required = false) UpdateTeacherRoleRequest req,
            HttpServletRequest request) {
        managementAccessService.requireTeacherManagementAccess(request);
        String role = req == null ? null : req.getRole();
        TeacherAccountService.UpdateTeacherRoleResponse response =
                teacherAccountService.updateTeacherRole(teacherId, role);
        return ResponseEntity.ok(response);
    }

    public static class UpdateTeacherRoleRequest {
        private String role;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}
