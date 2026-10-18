package com.studentmanagement.studentmanagementserver.domain.graduation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class InterviewCourseController {
    private final InterviewCourseService service;

    public InterviewCourseController(InterviewCourseService service) {
        this.service = service;
    }

    @GetMapping("/graduation-applications/interview-courses")
    public ResponseEntity<List<InterviewCoursePlanDto>> list(@RequestParam(required = false) Integer year,
                                                              HttpServletRequest request) {
        return ResponseEntity.ok(service.list(year, request));
    }

    @GetMapping("/graduation-applications/interview-courses/enabled-students")
    public ResponseEntity<List<Long>> listEnabledStudentIds(HttpServletRequest request) {
        return ResponseEntity.ok(service.listEnabledStudentIds(request));
    }

    @GetMapping("/students/{studentId}/graduation-applications/interview-course")
    public ResponseEntity<InterviewCoursePlanDto> get(@PathVariable Long studentId, HttpServletRequest request) {
        return ResponseEntity.ok(service.get(studentId, request));
    }

    @GetMapping("/me/interview-course")
    public ResponseEntity<InterviewCoursePlanDto> getMyInterviewCourse(HttpServletRequest request) {
        return ResponseEntity.ok(service.getMyInterviewCourse(request));
    }

    @PutMapping("/students/{studentId}/graduation-applications/interview-course/enabled")
    public ResponseEntity<InterviewCoursePlanDto> setEnabled(@PathVariable Long studentId,
                                                             @RequestBody InterviewCourseEnabledRequest body,
                                                             HttpServletRequest request) {
        return ResponseEntity.ok(service.setEnabled(studentId, body, request));
    }

    @PostMapping("/students/{studentId}/graduation-applications/interview-course/lessons")
    public ResponseEntity<InterviewCoursePlanDto> addLesson(@PathVariable Long studentId,
                                                            HttpServletRequest request) {
        return ResponseEntity.ok(service.addLesson(studentId, request));
    }

    @PutMapping("/students/{studentId}/graduation-applications/interview-course/lessons/{lessonId}")
    public ResponseEntity<InterviewCoursePlanDto> updateLesson(@PathVariable Long studentId,
                                                               @PathVariable Long lessonId,
                                                               @RequestBody InterviewCourseLessonRequest body,
                                                               HttpServletRequest request) {
        return ResponseEntity.ok(service.updateLesson(studentId, lessonId, body, request));
    }

    @PostMapping("/students/{studentId}/graduation-applications/interview-course/requirements")
    public ResponseEntity<InterviewCoursePlanDto> addRequirement(@PathVariable Long studentId,
                                                                  @RequestBody InterviewCourseRequirementRequest body,
                                                                  HttpServletRequest request) {
        return ResponseEntity.ok(service.addRequirement(studentId, body, request));
    }

    @PutMapping("/students/{studentId}/graduation-applications/interview-course/requirements/{requirementId}")
    public ResponseEntity<InterviewCoursePlanDto> updateRequirement(@PathVariable Long studentId,
                                                                     @PathVariable Long requirementId,
                                                                     @RequestBody InterviewCourseRequirementRequest body,
                                                                     HttpServletRequest request) {
        return ResponseEntity.ok(service.updateRequirement(studentId, requirementId, body, request));
    }

    @DeleteMapping("/students/{studentId}/graduation-applications/interview-course/requirements/{requirementId}")
    public ResponseEntity<InterviewCoursePlanDto> deleteRequirement(@PathVariable Long studentId,
                                                                     @PathVariable Long requirementId,
                                                                     HttpServletRequest request) {
        return ResponseEntity.ok(service.deleteRequirement(studentId, requirementId, request));
    }
}
