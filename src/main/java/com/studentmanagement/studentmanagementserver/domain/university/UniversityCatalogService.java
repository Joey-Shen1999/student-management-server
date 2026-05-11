package com.studentmanagement.studentmanagementserver.domain.university;

import com.studentmanagement.studentmanagementserver.repo.UniversityProgramRepository;
import com.studentmanagement.studentmanagementserver.repo.UniversityRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class UniversityCatalogService {

    private final UniversityRepository universityRepository;
    private final UniversityProgramRepository universityProgramRepository;

    public UniversityCatalogService(UniversityRepository universityRepository,
                                    UniversityProgramRepository universityProgramRepository) {
        this.universityRepository = universityRepository;
        this.universityProgramRepository = universityProgramRepository;
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
