package com.studentmanagement.studentmanagementserver.domain.ielts;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping
public class IeltsTrackingController {

    private final IeltsTrackingService ieltsTrackingService;

    public IeltsTrackingController(IeltsTrackingService ieltsTrackingService) {
        this.ieltsTrackingService = ieltsTrackingService;
    }

    @GetMapping("/api/student/ielts-module")
    public ResponseEntity<StudentIeltsModuleStateDto> getCurrentStudentModule(HttpServletRequest request) {
        return ResponseEntity.ok(ieltsTrackingService.getCurrentStudentModule(request));
    }

    @PutMapping("/api/student/ielts-module/records")
    public ResponseEntity<StudentIeltsModuleStateDto> updateCurrentStudentRecords(
            @RequestBody(required = false) StudentIeltsRecordsUpdateRequestDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(ieltsTrackingService.updateCurrentStudentRecords(requestBody, request));
    }

    @PutMapping("/api/student/ielts-module/preparation-intent")
    public ResponseEntity<StudentIeltsModuleStateDto> updateCurrentStudentPreparationIntent(
            @RequestBody(required = false) StudentIeltsPreparationIntentUpdateRequestDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(ieltsTrackingService.updateCurrentStudentPreparationIntent(requestBody, request));
    }

    @GetMapping("/api/teacher/students/{studentId}/ielts-module")
    public ResponseEntity<StudentIeltsModuleStateDto> getTeacherStudentModule(@PathVariable Long studentId,
                                                                               HttpServletRequest request) {
        return ResponseEntity.ok(ieltsTrackingService.getTeacherStudentModule(studentId, request));
    }

    @PutMapping("/api/teacher/students/{studentId}/ielts-module")
    public ResponseEntity<StudentIeltsModuleStateDto> updateTeacherStudentModule(@PathVariable Long studentId,
                                                                                  @RequestBody(required = false) TeacherIeltsModuleUpdateRequestDto requestBody,
                                                                                  HttpServletRequest request) {
        return ResponseEntity.ok(ieltsTrackingService.updateTeacherStudentModule(studentId, requestBody, request));
    }

    @GetMapping("/api/teacher/students/{studentId}/ielts-summary")
    public ResponseEntity<StudentIeltsSummaryDto> getTeacherStudentSummary(@PathVariable Long studentId,
                                                                            HttpServletRequest request) {
        return ResponseEntity.ok(ieltsTrackingService.getTeacherStudentSummary(studentId, request));
    }
}
