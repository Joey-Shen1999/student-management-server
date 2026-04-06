package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.osslt.StudentOssltModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudentOssltModuleRepository extends JpaRepository<StudentOssltModule, Long> {
    Optional<StudentOssltModule> findByStudent_Id(Long studentId);

    List<StudentOssltModule> findByStudent_IdIn(Collection<Long> studentIds);
}
