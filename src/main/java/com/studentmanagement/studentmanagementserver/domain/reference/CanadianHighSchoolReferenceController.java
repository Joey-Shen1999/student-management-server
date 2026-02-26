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
@RequestMapping("/api/reference/canadian-high-schools")
public class CanadianHighSchoolReferenceController {

    private final AuthSessionService authSessionService;
    private final CanadianHighSchoolReferenceService canadianHighSchoolReferenceService;

    public CanadianHighSchoolReferenceController(AuthSessionService authSessionService,
                                                 CanadianHighSchoolReferenceService canadianHighSchoolReferenceService) {
        this.authSessionService = authSessionService;
        this.canadianHighSchoolReferenceService = canadianHighSchoolReferenceService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<CanadianHighSchoolReferenceDto>> search(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "limit", required = false) Integer limit,
            HttpServletRequest request) {
        authSessionService.requireAuthenticatedUser(request);
        return ResponseEntity.ok(canadianHighSchoolReferenceService.search(query, limit));
    }
}
