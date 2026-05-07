package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.university.UniversityProgram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UniversityProgramRepository extends JpaRepository<UniversityProgram, Long> {
    List<UniversityProgram> findByUniversity_IdAndActiveTrueOrderByProgramNameAscFacultyNameAscDegreeTypeAsc(Long universityId);

    List<UniversityProgram> findByUniversity_IdOrderByProgramNameAscFacultyNameAscDegreeTypeAsc(Long universityId);
}
