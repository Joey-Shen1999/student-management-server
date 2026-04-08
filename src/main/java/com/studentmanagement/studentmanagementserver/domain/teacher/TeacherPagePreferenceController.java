package com.studentmanagement.studentmanagementserver.domain.teacher;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/teacher/preferences")
public class TeacherPagePreferenceController {

    private final TeacherPagePreferenceService teacherPagePreferenceService;

    public TeacherPagePreferenceController(TeacherPagePreferenceService teacherPagePreferenceService) {
        this.teacherPagePreferenceService = teacherPagePreferenceService;
    }

    @GetMapping("/{pageKey}")
    public ResponseEntity<TeacherPagePreferenceResponseDto> getPreference(@PathVariable String pageKey,
                                                                          HttpServletRequest request) {
        return ResponseEntity.ok(teacherPagePreferenceService.getPreference(pageKey, request));
    }

    @PutMapping("/{pageKey}")
    public ResponseEntity<TeacherPagePreferenceResponseDto> upsertPreference(
            @PathVariable String pageKey,
            @RequestBody(required = false) TeacherPagePreferencePutRequestDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(teacherPagePreferenceService.upsertPreference(pageKey, requestBody, request));
    }
}
