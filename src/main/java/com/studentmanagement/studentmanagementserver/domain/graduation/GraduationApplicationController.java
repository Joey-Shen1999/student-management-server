package com.studentmanagement.studentmanagementserver.domain.graduation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api")
public class GraduationApplicationController {

    private final GraduationApplicationService graduationApplicationService;

    public GraduationApplicationController(GraduationApplicationService graduationApplicationService) {
        this.graduationApplicationService = graduationApplicationService;
    }

    @GetMapping("/students/{studentId}/graduation-applications")
    public ResponseEntity<List<GraduationApplicationDto>> list(@PathVariable Long studentId,
                                                               HttpServletRequest request) {
        return ResponseEntity.ok(graduationApplicationService.listByStudent(studentId, request));
    }

    @GetMapping("/students/{studentId}/graduation-applications/history")
    public ResponseEntity<GraduationApplicationHistoryListDto> listHistory(
            @PathVariable Long studentId,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            HttpServletRequest request) {
        return ResponseEntity.ok(graduationApplicationService.listHistory(studentId, page, size, request));
    }

    @PutMapping("/students/{studentId}/graduation-applications/confirm")
    public ResponseEntity<List<GraduationApplicationDto>> confirm(
            @PathVariable Long studentId,
            @RequestBody(required = false) GraduationApplicationConfirmRequest requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(graduationApplicationService.confirmStage(studentId, requestBody, request));
    }

    @PostMapping("/students/{studentId}/graduation-applications")
    public ResponseEntity<GraduationApplicationDto> create(
            @PathVariable Long studentId,
            @RequestBody(required = false) GraduationApplicationRequest requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(graduationApplicationService.create(studentId, requestBody, request));
    }

    @PutMapping("/graduation-applications/{applicationId}")
    public ResponseEntity<GraduationApplicationDto> update(
            @PathVariable Long applicationId,
            @RequestBody(required = false) GraduationApplicationRequest requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(graduationApplicationService.update(applicationId, requestBody, request));
    }

    @DeleteMapping("/graduation-applications/{applicationId}")
    public ResponseEntity<Void> delete(@PathVariable Long applicationId,
                                       HttpServletRequest request) {
        graduationApplicationService.delete(applicationId, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/students/{studentId}/graduation-applications/reorder")
    public ResponseEntity<List<GraduationApplicationDto>> reorder(
            @PathVariable Long studentId,
            @RequestBody(required = false) List<GraduationApplicationReorderRequest> requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(graduationApplicationService.reorder(studentId, requestBody, request));
    }
}
