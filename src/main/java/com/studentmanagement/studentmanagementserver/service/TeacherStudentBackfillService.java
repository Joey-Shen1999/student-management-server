package com.studentmanagement.studentmanagementserver.service;

import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherStudent;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherStudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class TeacherStudentBackfillService {

    private static final Logger log = LoggerFactory.getLogger(TeacherStudentBackfillService.class);

    private final StudentRepository studentRepository;
    private final TeacherStudentRepository teacherStudentRepository;

    public TeacherStudentBackfillService(StudentRepository studentRepository,
                                         TeacherStudentRepository teacherStudentRepository) {
        this.studentRepository = studentRepository;
        this.teacherStudentRepository = teacherStudentRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void runOnStartup() {
        BackfillResult result = backfillFromLegacyStudentTeacher();
        log.info(
                "Teacher-student relation backfill completed. scanned={}, inserted={}",
                result.getScanned(),
                result.getInserted()
        );
    }

    @Transactional
    public BackfillResult backfillFromLegacyStudentTeacher() {
        List<Student> students = studentRepository.findAllWithTeacher();
        int inserted = 0;
        for (Student student : students) {
            Teacher teacher = student.getTeacher();
            if (teacher == null) {
                continue;
            }
            boolean existsActive = teacherStudentRepository.existsByTeacher_IdAndStudent_IdAndStatus(
                    teacher.getId(),
                    student.getId(),
                    TeacherStudentStatus.ACTIVE
            );
            if (!existsActive) {
                teacherStudentRepository.save(new TeacherStudent(
                        teacher,
                        student,
                        TeacherStudentStatus.ACTIVE,
                        "Backfilled from students.teacher_id"
                ));
                inserted++;
            }
        }
        return new BackfillResult(students.size(), inserted);
    }

    public static class BackfillResult {
        private final int scanned;
        private final int inserted;

        public BackfillResult(int scanned, int inserted) {
            this.scanned = scanned;
            this.inserted = inserted;
        }

        public int getScanned() {
            return scanned;
        }

        public int getInserted() {
            return inserted;
        }
    }
}
