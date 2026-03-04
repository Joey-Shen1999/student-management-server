package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.student.StudentIdentityFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentIdentityFileRepository extends JpaRepository<StudentIdentityFile, Long> {

    List<StudentIdentityFile> findByStudentProfile_IdOrderByUploadedAtDescIdDesc(Long studentProfileId);

    Optional<StudentIdentityFile> findByIdAndStudentProfile_Id(Long identityFileId, Long studentProfileId);

    long countByStudentProfile_Id(Long studentProfileId);
}
