package com.studentmanagement.studentmanagementserver.domain.student;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/teacher/students/{studentId}/profile")
public class TeacherStudentProfileController {

    private final TeacherStudentProfileService teacherStudentProfileService;

    public TeacherStudentProfileController(TeacherStudentProfileService teacherStudentProfileService) {
        this.teacherStudentProfileService = teacherStudentProfileService;
    }

    @GetMapping
    public ResponseEntity<StudentProfileDto> getProfile(@PathVariable Long studentId,
                                                        HttpServletRequest request) {
        return ResponseEntity.ok(teacherStudentProfileService.getProfile(studentId, request));
    }

    @PutMapping
    public ResponseEntity<StudentProfileDto> saveProfile(@PathVariable Long studentId,
                                                         @RequestBody(required = false) StudentProfileDto requestBody,
                                                         HttpServletRequest request) {
        return ResponseEntity.ok(teacherStudentProfileService.saveProfile(studentId, requestBody, request));
    }
}
