package com.studentmanagement.studentmanagementserver.service;

import com.studentmanagement.studentmanagementserver.domain.student.StudentSchoolRecord;
import com.studentmanagement.studentmanagementserver.domain.student.StudentSchoolTranscript;
import com.studentmanagement.studentmanagementserver.repo.StudentSchoolRecordRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentSchoolTranscriptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("!test")
public class StudentSchoolTranscriptMigrationService {

    private static final Logger log = LoggerFactory.getLogger(StudentSchoolTranscriptMigrationService.class);

    private final StudentSchoolRecordRepository studentSchoolRecordRepository;
    private final StudentSchoolTranscriptRepository studentSchoolTranscriptRepository;

    public StudentSchoolTranscriptMigrationService(StudentSchoolRecordRepository studentSchoolRecordRepository,
                                                   StudentSchoolTranscriptRepository studentSchoolTranscriptRepository) {
        this.studentSchoolRecordRepository = studentSchoolRecordRepository;
        this.studentSchoolTranscriptRepository = studentSchoolTranscriptRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateOnStartup() {
        MigrationResult result = migrateLegacyTranscriptColumns();
        log.info(
                "Student transcript migration completed. scannedLegacyRows={}, inserted={}, skippedAlreadyMigrated={}",
                result.scannedLegacyRows,
                result.inserted,
                result.skippedAlreadyMigrated
        );
    }

    @Transactional
    public MigrationResult migrateLegacyTranscriptColumns() {
        List<StudentSchoolRecord> schools = studentSchoolRecordRepository.findAll();
        int scannedLegacyRows = 0;
        int inserted = 0;
        int skippedAlreadyMigrated = 0;

        for (StudentSchoolRecord school : schools) {
            String storageKey = trimToNull(school.getTranscriptStorageKey());
            if (storageKey == null) {
                continue;
            }
            scannedLegacyRows++;

            if (studentSchoolTranscriptRepository.countBySchoolRecord_Id(school.getId()) > 0L) {
                skippedAlreadyMigrated++;
                continue;
            }

            String fileName = trimToNull(school.getTranscriptOriginalFilename());
            if (fileName == null) {
                fileName = "transcript.bin";
            }

            String mimeType = trimToNull(school.getTranscriptContentType());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }

            Long sizeBytes = school.getTranscriptSizeBytes();
            if (sizeBytes == null || sizeBytes.longValue() < 0L) {
                sizeBytes = Long.valueOf(0L);
            }

            LocalDateTime uploadedAt = school.getTranscriptUploadedAt();
            if (uploadedAt == null) {
                uploadedAt = school.getUpdatedAt() == null ? LocalDateTime.now() : school.getUpdatedAt();
            }

            Long uploadedBy = school.getStudent() == null || school.getStudent().getUser() == null
                    ? Long.valueOf(0L)
                    : school.getStudent().getUser().getId();

            StudentSchoolTranscript transcript = new StudentSchoolTranscript(
                    school,
                    storageKey,
                    fileName,
                    mimeType,
                    sizeBytes,
                    uploadedAt,
                    uploadedBy
            );
            studentSchoolTranscriptRepository.save(transcript);
            inserted++;
        }

        return new MigrationResult(scannedLegacyRows, inserted, skippedAlreadyMigrated);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static class MigrationResult {
        private final int scannedLegacyRows;
        private final int inserted;
        private final int skippedAlreadyMigrated;

        public MigrationResult(int scannedLegacyRows, int inserted, int skippedAlreadyMigrated) {
            this.scannedLegacyRows = scannedLegacyRows;
            this.inserted = inserted;
            this.skippedAlreadyMigrated = skippedAlreadyMigrated;
        }

        public int getScannedLegacyRows() {
            return scannedLegacyRows;
        }

        public int getInserted() {
            return inserted;
        }

        public int getSkippedAlreadyMigrated() {
            return skippedAlreadyMigrated;
        }
    }
}
