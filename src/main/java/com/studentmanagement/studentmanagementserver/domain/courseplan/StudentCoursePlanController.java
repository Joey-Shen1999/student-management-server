package com.studentmanagement.studentmanagementserver.domain.courseplan;

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
public class StudentCoursePlanController {

    private final StudentCoursePlanService studentCoursePlanService;

    public StudentCoursePlanController(StudentCoursePlanService studentCoursePlanService) {
        this.studentCoursePlanService = studentCoursePlanService;
    }

    @GetMapping("/api/student/course-plan")
    public ResponseEntity<StudentCoursePlanDto> getCurrentStudentCoursePlan(HttpServletRequest request) {
        return ResponseEntity.ok(studentCoursePlanService.getCurrentStudentCoursePlan(request));
    }

    @PutMapping("/api/student/course-plan")
    public ResponseEntity<StudentCoursePlanDto> updateCurrentStudentCoursePlan(
            @RequestBody(required = false) StudentCoursePlanDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(studentCoursePlanService.updateCurrentStudentCoursePlan(requestBody, request));
    }

    @GetMapping("/api/teacher/students/{studentId}/course-plan")
    public ResponseEntity<StudentCoursePlanDto> getTeacherStudentCoursePlan(@PathVariable Long studentId,
                                                                            HttpServletRequest request) {
        return ResponseEntity.ok(studentCoursePlanService.getTeacherStudentCoursePlan(studentId, request));
    }

    @PutMapping("/api/teacher/students/{studentId}/course-plan")
    public ResponseEntity<StudentCoursePlanDto> updateTeacherStudentCoursePlan(@PathVariable Long studentId,
                                                                               @RequestBody(required = false) StudentCoursePlanDto requestBody,
                                                                               HttpServletRequest request) {
        return ResponseEntity.ok(studentCoursePlanService.updateTeacherStudentCoursePlan(studentId, requestBody, request));
    }
}
