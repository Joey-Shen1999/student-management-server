package com.studentmanagement.studentmanagementserver.domain.osslt;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping
public class OssltTrackingController {

    private final OssltTrackingService ossltTrackingService;

    public OssltTrackingController(OssltTrackingService ossltTrackingService) {
        this.ossltTrackingService = ossltTrackingService;
    }

    @GetMapping("/api/student/osslt-module")
    public ResponseEntity<TeacherStudentOssltModuleStateDto> getCurrentStudentModule(HttpServletRequest request) {
        return ResponseEntity.ok(ossltTrackingService.getCurrentStudentModule(request));
    }

    @PutMapping("/api/student/osslt-module")
    public ResponseEntity<TeacherStudentOssltModuleStateDto> updateCurrentStudentModule(
            @RequestBody(required = false) StudentOssltModuleUpdateRequestDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(ossltTrackingService.updateCurrentStudentModule(requestBody, request));
    }

    @GetMapping("/api/teacher/students/{studentId}/osslt-module")
    public ResponseEntity<TeacherStudentOssltModuleStateDto> getTeacherStudentModule(@PathVariable Long studentId,
                                                                                      HttpServletRequest request) {
        return ResponseEntity.ok(ossltTrackingService.getTeacherStudentModule(studentId, request));
    }

    @PutMapping("/api/teacher/students/{studentId}/osslt-module")
    public ResponseEntity<TeacherStudentOssltModuleStateDto> updateTeacherStudentModule(
            @PathVariable Long studentId,
            @RequestBody(required = false) TeacherOssltModuleUpdateRequestDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(ossltTrackingService.updateTeacherStudentModule(studentId, requestBody, request));
    }

    @GetMapping("/api/teacher/students/osslt-summary")
    public ResponseEntity<List<TeacherStudentOssltSummaryDto>> getTeacherStudentsSummary(
            @RequestParam(name = "studentIds", required = false) String studentIds,
            HttpServletRequest request) {
        return ResponseEntity.ok(ossltTrackingService.getTeacherStudentsSummary(studentIds, request));
    }
}
