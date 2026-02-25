package com.studentmanagement.studentmanagementserver.service;

import com.studentmanagement.studentmanagementserver.domain.enums.SchoolType;
import com.studentmanagement.studentmanagementserver.domain.student.StudentCourseRecord;
import com.studentmanagement.studentmanagementserver.domain.student.StudentSchoolRecord;
import com.studentmanagement.studentmanagementserver.repo.StudentCourseRecordRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentSchoolRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile("!test")
public class StudentProfileDataMigrationService {

    private static final Logger log = LoggerFactory.getLogger(StudentProfileDataMigrationService.class);

    private final StudentCourseRecordRepository studentCourseRecordRepository;
    private final StudentSchoolRecordRepository studentSchoolRecordRepository;

    public StudentProfileDataMigrationService(StudentCourseRecordRepository studentCourseRecordRepository,
                                              StudentSchoolRecordRepository studentSchoolRecordRepository) {
        this.studentCourseRecordRepository = studentCourseRecordRepository;
        this.studentSchoolRecordRepository = studentSchoolRecordRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateLegacyMixedDataOnStartup() {
        MigrationResult result = migrateLegacyMixedData();
        log.info(
                "Student profile mixed-data migration completed. scannedCourses={}, movedToSchools={}, deletedFromCourses={}",
                result.getScannedCourses(),
                result.getMovedToSchools(),
                result.getDeletedFromCourses()
        );
    }

    @Transactional
    public MigrationResult migrateLegacyMixedData() {
        List<StudentCourseRecord> allCourses = studentCourseRecordRepository.findAll();
        int movedToSchools = 0;
        List<StudentCourseRecord> toDelete = new ArrayList<StudentCourseRecord>();

        for (StudentCourseRecord course : allCourses) {
            if (!isSchoolHistoryRecord(course)) {
                continue;
            }

            SchoolType schoolType = course.getSchoolType() == null ? SchoolType.OTHER : course.getSchoolType();
            boolean exists = studentSchoolRecordRepository
                    .existsByStudent_IdAndSchoolTypeAndSchoolNameAndStartTimeAndEndTime(
                            course.getStudent().getId(),
                            schoolType,
                            course.getSchoolName(),
                            course.getStartTime(),
                            course.getEndTime()
                    );
            if (!exists) {
                studentSchoolRecordRepository.save(new StudentSchoolRecord(
                        course.getStudent(),
                        schoolType,
                        course.getSchoolName(),
                        course.getStartTime(),
                        course.getEndTime()
                ));
                movedToSchools++;
            }
            toDelete.add(course);
        }

        if (!toDelete.isEmpty()) {
            studentCourseRecordRepository.deleteAll(toDelete);
        }

        return new MigrationResult(allCourses.size(), movedToSchools, toDelete.size());
    }

    private boolean isSchoolHistoryRecord(StudentCourseRecord course) {
        String courseCode = course.getCourseCode();
        if (!isBlank(courseCode)) {
            return false;
        }
        return !isBlank(course.getSchoolName());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class MigrationResult {
        private final int scannedCourses;
        private final int movedToSchools;
        private final int deletedFromCourses;

        public MigrationResult(int scannedCourses, int movedToSchools, int deletedFromCourses) {
            this.scannedCourses = scannedCourses;
            this.movedToSchools = movedToSchools;
            this.deletedFromCourses = deletedFromCourses;
        }

        public int getScannedCourses() {
            return scannedCourses;
        }

        public int getMovedToSchools() {
            return movedToSchools;
        }

        public int getDeletedFromCourses() {
            return deletedFromCourses;
        }
    }
}
