package com.studentmanagement.studentmanagementserver.domain.university;

import com.studentmanagement.studentmanagementserver.repo.UniversityProgramRepository;
import com.studentmanagement.studentmanagementserver.repo.UniversityRepository;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import com.studentmanagement.studentmanagementserver.service.MustChangePasswordRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

@Service
public class UniversityCatalogService {

    private final UniversityRepository universityRepository;
    private final UniversityProgramRepository universityProgramRepository;
    private final AuthSessionService authSessionService;

    public UniversityCatalogService(UniversityRepository universityRepository,
                                    UniversityProgramRepository universityProgramRepository,
                                    AuthSessionService authSessionService) {
        this.universityRepository = universityRepository;
        this.universityProgramRepository = universityProgramRepository;
        this.authSessionService = authSessionService;
    }

    @Transactional(readOnly = true)
    public List<UniversityDto> listActiveUniversities() {
        List<University> universities = universityRepository.findByActiveTrueOrderByNameAscProvinceAscCityAsc();
        List<UniversityDto> dtos = new ArrayList<UniversityDto>(universities.size());
        for (University university : universities) {
            dtos.add(toDto(university));
        }
        return dtos;
    }

    @Transactional(readOnly = true)
    public List<UniversityProgramDto> listActivePrograms(Long universityId) {
        University university = requireActiveUniversity(universityId);
        List<UniversityProgram> programs =
                universityProgramRepository.findByUniversity_IdAndActiveTrueOrderByProgramNameAscFacultyNameAscDegreeTypeAsc(
                        university.getId()
                );
        List<UniversityProgramDto> dtos = new ArrayList<UniversityProgramDto>(programs.size());
        for (UniversityProgram program : programs) {
            dtos.add(toDto(program));
        }
        return dtos;
    }

    @Transactional
    public UniversityProgramDto createCustomProgram(Long universityId,
                                                    CustomUniversityProgramRequest body,
                                                    HttpServletRequest request) {
        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.isMustChangePassword()) {
            throw new MustChangePasswordRequiredException();
        }
        if (operator.getRole() != UserRole.ADMIN && operator.getRole() != UserRole.TEACHER
                && operator.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "University program access denied.");
        }
        University university = requireActiveUniversity(universityId);
        String name = body == null || body.getProgramName() == null ? "" : body.getProgramName().trim();
        if (name.isEmpty() || name.length() > 180 || "other".equalsIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Program name must be 1-180 characters and cannot be Other.");
        }
        for (UniversityProgram existing : universityProgramRepository
                .findByUniversity_IdOrderByProgramNameAscFacultyNameAscDegreeTypeAsc(university.getId())) {
            if (existing.getProgramName().equalsIgnoreCase(name)) {
                if (!existing.isActive()) {
                    existing.setActive(true);
                    return toDto(universityProgramRepository.save(existing));
                }
                return toDto(existing);
            }
        }
        return toDto(universityProgramRepository.save(new UniversityProgram(university, name, null, null)));
    }

    private University requireActiveUniversity(Long universityId) {
        if (universityId == null || universityId.longValue() <= 0L) {
            throw new IllegalArgumentException("universityId must be positive");
        }
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "University not found: " + universityId));
        if (!university.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "University is not active: " + universityId);
        }
        return university;
    }

    static UniversityDto toDto(University university) {
        UniversityDto dto = new UniversityDto();
        dto.setId(university.getId());
        dto.setName(university.getName());
        dto.setProvince(university.getProvince());
        dto.setCity(university.getCity());
        dto.setCountry(university.getCountry());
        dto.setWebsite(university.getWebsite());
        return dto;
    }

    static UniversityProgramDto toDto(UniversityProgram program) {
        UniversityProgramDto dto = new UniversityProgramDto();
        dto.setId(program.getId());
        dto.setUniversityId(program.getUniversity() == null ? null : program.getUniversity().getId());
        dto.setProgramName(program.getProgramName());
        dto.setFacultyName(program.getFacultyName());
        dto.setDegreeType(program.getDegreeType());
        return dto;
    }
}
