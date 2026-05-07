package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.university.University;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UniversityRepository extends JpaRepository<University, Long> {
    List<University> findByActiveTrueOrderByNameAscProvinceAscCityAsc();

    Optional<University> findFirstByNameIgnoreCase(String name);
}
