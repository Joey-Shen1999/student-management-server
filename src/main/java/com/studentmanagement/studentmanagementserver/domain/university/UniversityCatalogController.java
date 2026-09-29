package com.studentmanagement.studentmanagementserver.domain.university;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/universities")
public class UniversityCatalogController {

    private final UniversityCatalogService universityCatalogService;

    public UniversityCatalogController(UniversityCatalogService universityCatalogService) {
        this.universityCatalogService = universityCatalogService;
    }

    @GetMapping
    public ResponseEntity<List<UniversityDto>> listUniversities() {
        return ResponseEntity.ok(universityCatalogService.listActiveUniversities());
    }

    @GetMapping("/{universityId}/programs")
    public ResponseEntity<List<UniversityProgramDto>> listPrograms(@PathVariable Long universityId) {
        return ResponseEntity.ok(universityCatalogService.listActivePrograms(universityId));
    }

    @PostMapping("/{universityId}/programs")
    public ResponseEntity<UniversityProgramDto> createCustomProgram(@PathVariable Long universityId,
                                                                      @RequestBody CustomUniversityProgramRequest body,
                                                                      HttpServletRequest request) {
        return ResponseEntity.ok(universityCatalogService.createCustomProgram(universityId, body, request));
    }
}
