package com.studentmanagement.studentmanagementserver.domain.volunteer;

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
public class VolunteerTrackingController {

    private final VolunteerTrackingService volunteerTrackingService;

    public VolunteerTrackingController(VolunteerTrackingService volunteerTrackingService) {
        this.volunteerTrackingService = volunteerTrackingService;
    }

    @GetMapping("/api/teacher/students/{studentId}/volunteer-tracking")
    public ResponseEntity<VolunteerTrackingDto> getTeacherStudentVolunteerTracking(@PathVariable Long studentId,
                                                                                   HttpServletRequest request) {
        return ResponseEntity.ok(volunteerTrackingService.getTeacherStudentVolunteerTracking(studentId, request));
    }

    @PutMapping("/api/teacher/students/{studentId}/volunteer-tracking")
    public ResponseEntity<VolunteerTrackingDto> upsertTeacherStudentVolunteerTracking(
            @PathVariable Long studentId,
            @RequestBody(required = false) VolunteerTrackingUpsertRequestDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(
                volunteerTrackingService.upsertTeacherStudentVolunteerTracking(studentId, requestBody, request)
        );
    }

    @PostMapping("/api/teacher/students/volunteer-tracking/batch-summary")
    public ResponseEntity<List<VolunteerTrackingBatchSummaryItemDto>> getVolunteerTrackingBatchSummary(
            @RequestBody(required = false) VolunteerTrackingBatchSummaryRequestDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(volunteerTrackingService.getVolunteerTrackingBatchSummary(requestBody, request));
    }

    @GetMapping("/api/student/volunteer-tracking")
    public ResponseEntity<VolunteerTrackingDto> getCurrentStudentVolunteerTracking(HttpServletRequest request) {
        return ResponseEntity.ok(volunteerTrackingService.getCurrentStudentVolunteerTracking(request));
    }

    @PutMapping("/api/student/volunteer-tracking")
    public ResponseEntity<VolunteerTrackingDto> upsertCurrentStudentVolunteerTracking(
            @RequestBody(required = false) VolunteerTrackingUpsertRequestDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(volunteerTrackingService.upsertCurrentStudentVolunteerTracking(requestBody, request));
    }
}
