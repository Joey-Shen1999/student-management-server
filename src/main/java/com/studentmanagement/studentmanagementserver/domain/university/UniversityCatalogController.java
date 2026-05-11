package com.studentmanagement.studentmanagementserver.domain.university;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
