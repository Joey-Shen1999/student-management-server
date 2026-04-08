package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherPagePreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherPagePreferenceRepository extends JpaRepository<TeacherPagePreference, Long> {

    Optional<TeacherPagePreference> findByTeacher_IdAndPageKey(Long teacherId, String pageKey);
}
