package com.studentmanagement.studentmanagementserver.domain.reference;

import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/reference/ontario-course-providers")
public class OntarioCourseProviderReferenceController {

    private final AuthSessionService authSessionService;
    private final OntarioCourseProviderReferenceService ontarioCourseProviderReferenceService;

    public OntarioCourseProviderReferenceController(AuthSessionService authSessionService,
                                                    OntarioCourseProviderReferenceService ontarioCourseProviderReferenceService) {
        this.authSessionService = authSessionService;
        this.ontarioCourseProviderReferenceService = ontarioCourseProviderReferenceService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<OntarioCourseProviderReferenceDto>> search(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "limit", required = false) Integer limit,
            HttpServletRequest request) {
        authSessionService.requireAuthenticatedUser(request);
        return ResponseEntity.ok(ontarioCourseProviderReferenceService.search(query, limit));
    }
}
