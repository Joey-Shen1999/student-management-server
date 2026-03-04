package com.studentmanagement.studentmanagementserver.domain.student;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class StudentSchoolTranscriptStorageService {

    private final Path rootDirectory;

    public StudentSchoolTranscriptStorageService(
            @Value("${app.student-profile.transcript-dir:uploads/student-school-transcripts}") String rootDir
    ) {
        this.rootDirectory = Paths.get(rootDir).toAbsolutePath().normalize();
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

        ensureRootDirectory();

        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        String extension = extractExtension(originalFilename);
        String storageKey = "student-" + studentId
                + "_school-" + schoolRecordId
                + "_" + UUID.randomUUID().toString().replace("-", "")
                + extension;

        Path targetPath = resolveStoragePath(storageKey);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
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
        Path path = resolveStoragePath(storageKey);
        try {
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transcript file not found.");
        }
    }

    public void deleteIfExists(String storageKey) {
        if (storageKey == null || storageKey.trim().isEmpty()) {
            return;
        }

        Path path = resolveStoragePath(storageKey);
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best effort cleanup.
        }
    }

    public void deleteRequired(String storageKey) {
        Path path = resolveStoragePath(storageKey);
        try {
            Files.delete(path);
        } catch (NoSuchFileException ex) {
            // Treat missing file as already deleted.
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to delete transcript file");
        }
    }

    private void ensureRootDirectory() {
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to initialize transcript storage directory");
        }
    }

    private Path resolveStoragePath(String storageKey) {
        String key = storageKey == null ? "" : storageKey.trim();
        if (key.isEmpty()) {
            throw new IllegalArgumentException("transcript storage key is required");
        }
        if (key.contains("..") || key.contains("/") || key.contains("\\")) {
            throw new IllegalArgumentException("invalid transcript storage key");
        }

        Path path = rootDirectory.resolve(key).normalize();
        if (!path.startsWith(rootDirectory)) {
            throw new IllegalArgumentException("invalid transcript storage key");
        }
        return path;
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
