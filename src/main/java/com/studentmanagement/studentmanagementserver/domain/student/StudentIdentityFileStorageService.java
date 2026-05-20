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
public class StudentIdentityFileStorageService {

    private static final String NAMESPACE = "student-identity-files";

    private final Path rootDirectory;
    private final StudentFileStorageBackend storageBackend;

    public StudentIdentityFileStorageService(
            @Value("${app.student-profile.identity-file-dir:uploads/student-identity-files}") String rootDir,
            StudentFileStorageBackend storageBackend
    ) {
        this.rootDirectory = Paths.get(rootDir).toAbsolutePath().normalize();
        this.storageBackend = storageBackend;
    }

    public StoredIdentityFile store(Long studentId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("identity file is required");
        }
        if (studentId == null || studentId.longValue() <= 0L) {
            throw new IllegalArgumentException("studentId must be positive");
        }

        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        String extension = extractExtension(originalFilename);
        String storageKey = "student-" + studentId
                + "_identity-"
                + UUID.randomUUID().toString().replace("-", "")
                + extension;

        try (InputStream inputStream = file.getInputStream()) {
            storageBackend.store(NAMESPACE, rootDirectory, storageKey, inputStream, file.getSize(), normalizeContentType(file.getContentType()));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to store identity file");
        }

        return new StoredIdentityFile(
                storageKey,
                originalFilename,
                normalizeContentType(file.getContentType()),
                file.getSize()
        );
    }

    public byte[] readAllBytes(String storageKey) {
        return storageBackend.readAllBytes(NAMESPACE, rootDirectory, storageKey, "Identity file not found.");
    }

    public void deleteRequired(String storageKey) {
        storageBackend.deleteRequired(NAMESPACE, rootDirectory, storageKey);
    }

    private String sanitizeOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return "identity.bin";
        }

        String fileNameOnly = Paths.get(originalFilename).getFileName().toString().trim();
        if (fileNameOnly.isEmpty()) {
            return "identity.bin";
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

    public static class StoredIdentityFile {
        private final String storageKey;
        private final String originalFilename;
        private final String contentType;
        private final long sizeBytes;

        private StoredIdentityFile(String storageKey,
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
