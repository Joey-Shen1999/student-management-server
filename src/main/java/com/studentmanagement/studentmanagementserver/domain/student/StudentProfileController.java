package com.studentmanagement.studentmanagementserver.domain.student;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/student/profile")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    public StudentProfileController(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    @GetMapping
    public ResponseEntity<StudentProfileDto> getProfile(HttpServletRequest request) {
        return ResponseEntity.ok(studentProfileService.getCurrentStudentProfile(request));
    }

    @PutMapping
    public ResponseEntity<StudentProfileDto> saveProfile(@RequestBody(required = false) StudentProfileDto requestBody,
                                                         HttpServletRequest request) {
        return ResponseEntity.ok(studentProfileService.saveCurrentStudentProfile(requestBody, request));
    }
}
