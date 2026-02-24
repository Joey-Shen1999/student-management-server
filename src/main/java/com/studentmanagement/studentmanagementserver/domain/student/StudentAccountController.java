package com.studentmanagement.studentmanagementserver.domain.student;

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
@RequestMapping("/api/teacher/student-accounts")
public class StudentAccountController {

    private final StudentAccountService studentAccountService;
    private final ManagementAccessService managementAccessService;

    public StudentAccountController(StudentAccountService studentAccountService,
                                    ManagementAccessService managementAccessService) {
        this.studentAccountService = studentAccountService;
        this.managementAccessService = managementAccessService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(HttpServletRequest request) {
        managementAccessService.requireStudentAccountManagementAccess(request);
        List<StudentAccountService.StudentAccountItem> accounts = studentAccountService.listStudentAccounts();
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("data", accounts);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{studentId}/reset-password")
    public ResponseEntity<StudentAccountService.ResetStudentPasswordResponse> resetPassword(
            @PathVariable Long studentId,
            @RequestBody(required = false) Map<String, Object> ignoredBody,
            HttpServletRequest request) {
        managementAccessService.requireStudentAccountManagementAccess(request);
        StudentAccountService.ResetStudentPasswordResponse response =
                studentAccountService.resetStudentPassword(studentId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{studentId}/status")
    public ResponseEntity<StudentAccountService.UpdateStudentStatusResponse> updateStatus(
            @PathVariable Long studentId,
            @RequestBody(required = false) UpdateStudentStatusRequest req,
            HttpServletRequest request) {
        User operator = managementAccessService.requireStudentAccountManagementAccess(request);
        String status = req == null ? null : req.getStatus();
        StudentAccountService.UpdateStudentStatusResponse response =
                studentAccountService.updateStudentStatus(studentId, status, operator);
        return ResponseEntity.ok(response);
    }

    public static class UpdateStudentStatusRequest {
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
