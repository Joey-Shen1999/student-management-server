package com.studentmanagement.studentmanagementserver.domain.extracurricular;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping
public class ExtracurricularTrackingController {

    private final ExtracurricularTrackingService extracurricularTrackingService;

    public ExtracurricularTrackingController(ExtracurricularTrackingService extracurricularTrackingService) {
        this.extracurricularTrackingService = extracurricularTrackingService;
    }

    @GetMapping("/api/teacher/students/{studentId}/extracurricular-tracking")
    public ResponseEntity<ExtracurricularTrackingDto> getTeacherStudentExtracurricularTracking(
            @PathVariable Long studentId,
            HttpServletRequest request) {
        return ResponseEntity.ok(
                extracurricularTrackingService.getTeacherStudentExtracurricularTracking(studentId, request)
        );
    }

    @PutMapping("/api/teacher/students/{studentId}/extracurricular-tracking")
    public ResponseEntity<ExtracurricularTrackingDto> upsertTeacherStudentExtracurricularTracking(
            @PathVariable Long studentId,
            @RequestBody(required = false) ExtracurricularTrackingUpsertRequestDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(
                extracurricularTrackingService.upsertTeacherStudentExtracurricularTracking(studentId, requestBody, request)
        );
    }

    @PostMapping("/api/teacher/students/extracurricular-tracking/batch-summary")
    public ResponseEntity<List<ExtracurricularTrackingBatchSummaryItemDto>> getExtracurricularTrackingBatchSummary(
            @RequestBody(required = false) ExtracurricularTrackingBatchSummaryRequestDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(
                extracurricularTrackingService.getExtracurricularTrackingBatchSummary(requestBody, request)
        );
    }

    @GetMapping("/api/student/extracurricular-tracking")
    public ResponseEntity<ExtracurricularTrackingDto> getCurrentStudentExtracurricularTracking(
            HttpServletRequest request) {
        return ResponseEntity.ok(extracurricularTrackingService.getCurrentStudentExtracurricularTracking(request));
    }

    @PutMapping("/api/student/extracurricular-tracking")
    public ResponseEntity<ExtracurricularTrackingDto> upsertCurrentStudentExtracurricularTracking(
            @RequestBody(required = false) ExtracurricularTrackingUpsertRequestDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(
                extracurricularTrackingService.upsertCurrentStudentExtracurricularTracking(requestBody, request)
        );
    }
}
