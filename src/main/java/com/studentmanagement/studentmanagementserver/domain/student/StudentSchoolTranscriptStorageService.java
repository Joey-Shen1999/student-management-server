package com.studentmanagement.studentmanagementserver.domain.student;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@Service
public class StudentSchoolTranscriptStorageService {

    private static final String NAMESPACE = "student-school-transcripts";

    private final Path rootDirectory;
    private final StudentFileStorageBackend storageBackend;

    public StudentSchoolTranscriptStorageService(
            @Value("${app.student-profile.transcript-dir:uploads/student-school-transcripts}") String rootDir,
            StudentFileStorageBackend storageBackend
    ) {
        this.rootDirectory = Paths.get(rootDir).toAbsolutePath().normalize();
        this.storageBackend = storageBackend;
    }

    public StoredTranscript store(Long studentId, Long schoolRecordId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("transcript file is required");
        }
        if (studentId == null || studentId.longValue() <= 0L) {
            throw new IllegalArgumentException("studentId must be positive");
        }
        if (schoolRecordId == null || schoolRecordId.longValue() <= 0L) {
            throw new IllegalArgumentException("schoolRecordId must be positive");
        }

        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        String extension = extractExtension(originalFilename);
        String storageKey = "student-" + studentId
                + "_school-" + schoolRecordId
                + "_" + UUID.randomUUID().toString().replace("-", "")
                + extension;

        try (InputStream inputStream = file.getInputStream()) {
            storageBackend.store(NAMESPACE, rootDirectory, storageKey, inputStream, file.getSize(), normalizeContentType(file.getContentType()));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to store transcript file");
        }

        String contentType = normalizeContentType(file.getContentType());
        return new StoredTranscript(
                storageKey,
                originalFilename,
                contentType,
                file.getSize()
        );
    }

    public byte[] readAllBytes(String storageKey) {
        return storageBackend.readAllBytes(NAMESPACE, rootDirectory, storageKey, "Transcript file not found.");
    }

    public void deleteIfExists(String storageKey) {
        storageBackend.deleteIfExists(NAMESPACE, rootDirectory, storageKey);
    }

    public void deleteRequired(String storageKey) {
        storageBackend.deleteRequired(NAMESPACE, rootDirectory, storageKey);
    }

    private String sanitizeOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return "transcript.bin";
        }

        String fileNameOnly = Paths.get(originalFilename).getFileName().toString().trim();
        if (fileNameOnly.isEmpty()) {
            return "transcript.bin";
        }
        return fileNameOnly;
    }

    private String extractExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return ".bin";
        }
        String extension = fileName.substring(index).toLowerCase(Locale.ROOT);
        if (!extension.matches("\\.[a-z0-9]{1,12}")) {
            return ".bin";
        }
        return extension;
    }

    private String normalizeContentType(String rawContentType) {
        if (rawContentType == null || rawContentType.trim().isEmpty()) {
            return "application/octet-stream";
        }
        return rawContentType.trim();
    }

    public static class StoredTranscript {
        private final String storageKey;
        private final String originalFilename;
        private final String contentType;
        private final long sizeBytes;

        private StoredTranscript(String storageKey,
                                 String originalFilename,
                                 String contentType,
                                 long sizeBytes) {
            this.storageKey = storageKey;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.sizeBytes = sizeBytes;
        }

        public String getStorageKey() {
            return storageKey;
        }

        public String getOriginalFilename() {
            return originalFilename;
        }

        public String getContentType() {
            return contentType;
        }

        public long getSizeBytes() {
            return sizeBytes;
        }
    }
}
