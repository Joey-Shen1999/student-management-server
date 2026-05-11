package com.studentmanagement.studentmanagementserver.domain.university;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api")
public class UniversityAspirationController {

    private final UniversityAspirationService universityAspirationService;

    public UniversityAspirationController(UniversityAspirationService universityAspirationService) {
        this.universityAspirationService = universityAspirationService;
    }

    @GetMapping("/students/{studentId}/university-aspirations")
    public ResponseEntity<List<UniversityAspirationDto>> list(@PathVariable Long studentId,
                                                              HttpServletRequest request) {
        return ResponseEntity.ok(universityAspirationService.listByStudent(studentId, request));
    }

    @PostMapping("/students/{studentId}/university-aspirations")
    public ResponseEntity<UniversityAspirationDto> create(@PathVariable Long studentId,
                                                          @RequestBody(required = false) UniversityAspirationRequest requestBody,
                                                          HttpServletRequest request) {
        return ResponseEntity.ok(universityAspirationService.create(studentId, requestBody, request));
    }

    @PutMapping("/university-aspirations/{aspirationId}")
    public ResponseEntity<UniversityAspirationDto> update(@PathVariable Long aspirationId,
                                                          @RequestBody(required = false) UniversityAspirationRequest requestBody,
                                                          HttpServletRequest request) {
        return ResponseEntity.ok(universityAspirationService.update(aspirationId, requestBody, request));
    }

    @DeleteMapping("/university-aspirations/{aspirationId}")
    public ResponseEntity<Void> delete(@PathVariable Long aspirationId,
                                       HttpServletRequest request) {
        universityAspirationService.delete(aspirationId, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/students/{studentId}/university-aspirations/reorder")
    public ResponseEntity<List<UniversityAspirationDto>> reorder(
            @PathVariable Long studentId,
            @RequestBody(required = false) List<UniversityAspirationReorderRequest> requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(universityAspirationService.reorder(studentId, requestBody, request));
    }
}
