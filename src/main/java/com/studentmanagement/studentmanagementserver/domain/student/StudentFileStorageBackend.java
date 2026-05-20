package com.studentmanagement.studentmanagementserver.domain.student;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

@Service
public class StudentFileStorageBackend {

    private static final String TYPE_S3 = "s3";

    private final String storageType;
    private final String bucket;
    private final String prefix;
    private final ObjectProvider<S3Client> s3ClientProvider;

    public StudentFileStorageBackend(
            @Value("${app.storage.type:local}") String storageType,
            @Value("${app.storage.s3.bucket:}") String bucket,
            @Value("${app.storage.s3.prefix:}") String prefix,
            ObjectProvider<S3Client> s3ClientProvider
    ) {
        this.storageType = normalizeStorageType(storageType);
        this.bucket = bucket == null ? "" : bucket.trim();
        this.prefix = normalizePrefix(prefix);
        this.s3ClientProvider = s3ClientProvider;
    }

    public void store(String namespace,
                      Path localRootDirectory,
                      String storageKey,
                      InputStream inputStream,
                      long sizeBytes,
                      String contentType) {
        if (isS3()) {
            storeS3(namespace, storageKey, inputStream, sizeBytes, contentType);
            return;
        }
        storeLocal(localRootDirectory, storageKey, inputStream);
    }

    public byte[] readAllBytes(String namespace, Path localRootDirectory, String storageKey, String notFoundMessage) {
        if (isS3()) {
            return readS3(namespace, storageKey, notFoundMessage);
        }
        return readLocal(localRootDirectory, storageKey, notFoundMessage);
    }

    public void deleteIfExists(String namespace, Path localRootDirectory, String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            return;
        }
        if (isS3()) {
            deleteS3(namespace, storageKey);
            return;
        }
        deleteLocalIfExists(localRootDirectory, storageKey);
    }

    public void deleteRequired(String namespace, Path localRootDirectory, String storageKey) {
        if (isS3()) {
            deleteS3(namespace, storageKey);
            return;
        }
        deleteLocalRequired(localRootDirectory, storageKey);
    }

    private boolean isS3() {
        return TYPE_S3.equals(storageType);
    }

    private void storeLocal(Path localRootDirectory, String storageKey, InputStream inputStream) {
        ensureLocalRootDirectory(localRootDirectory);
        Path targetPath = resolveLocalPath(localRootDirectory, storageKey);
        try {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store uploaded file");
        }
    }

    private byte[] readLocal(Path localRootDirectory, String storageKey, String notFoundMessage) {
        Path path = resolveLocalPath(localRootDirectory, storageKey);
        try {
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, notFoundMessage);
        }
    }

    private void deleteLocalIfExists(Path localRootDirectory, String storageKey) {
        Path path = resolveLocalPath(localRootDirectory, storageKey);
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best effort cleanup.
        }
    }

    private void deleteLocalRequired(Path localRootDirectory, String storageKey) {
        Path path = resolveLocalPath(localRootDirectory, storageKey);
        try {
            Files.delete(path);
        } catch (NoSuchFileException ex) {
            // Treat missing file as already deleted.
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to delete uploaded file");
        }
    }

    private void ensureLocalRootDirectory(Path localRootDirectory) {
        try {
            Files.createDirectories(localRootDirectory);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to initialize upload storage directory");
        }
    }

    private Path resolveLocalPath(Path localRootDirectory, String storageKey) {
        validateStorageKey(storageKey);
        Path path = localRootDirectory.resolve(storageKey.trim()).normalize();
        if (!path.startsWith(localRootDirectory)) {
            throw new IllegalArgumentException("invalid upload storage key");
        }
        return path;
    }

    private void storeS3(String namespace,
                         String storageKey,
                         InputStream inputStream,
                         long sizeBytes,
                         String contentType) {
        S3Client s3Client = requireS3Client();
        String objectKey = resolveS3ObjectKey(namespace, storageKey);
        PutObjectRequest.Builder request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey);
        if (StringUtils.hasText(contentType)) {
            request.contentType(contentType.trim());
        }
        try {
            s3Client.putObject(request.build(), RequestBody.fromInputStream(inputStream, sizeBytes));
        } catch (S3Exception ex) {
            throw new IllegalStateException("Failed to store uploaded file in S3");
        }
    }

    private byte[] readS3(String namespace, String storageKey, String notFoundMessage) {
        S3Client s3Client = requireS3Client();
        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(resolveS3ObjectKey(namespace, storageKey))
                    .build());
            return response.asByteArray();
        } catch (NoSuchKeyException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, notFoundMessage);
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, notFoundMessage);
            }
            throw new IllegalStateException("Failed to read uploaded file from S3");
        }
    }

    private void deleteS3(String namespace, String storageKey) {
        S3Client s3Client = requireS3Client();
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(resolveS3ObjectKey(namespace, storageKey))
                    .build());
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                return;
            }
            throw new IllegalStateException("Failed to delete uploaded file from S3");
        }
    }

    private S3Client requireS3Client() {
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException("app.storage.s3.bucket is required when app.storage.type=s3");
        }
        S3Client s3Client = s3ClientProvider.getIfAvailable();
        if (s3Client == null) {
            throw new IllegalStateException("S3 client is not configured");
        }
        return s3Client;
    }

    private String resolveS3ObjectKey(String namespace, String storageKey) {
        validateNamespace(namespace);
        validateStorageKey(storageKey);
        String normalizedNamespace = namespace.trim();
        String normalizedStorageKey = storageKey.trim();
        return prefix + normalizedNamespace + "/" + normalizedStorageKey;
    }

    private void validateNamespace(String namespace) {
        if (!StringUtils.hasText(namespace)) {
            throw new IllegalArgumentException("storage namespace is required");
        }
        String trimmed = namespace.trim();
        if (trimmed.contains("..") || trimmed.contains("\\") || trimmed.startsWith("/") || trimmed.endsWith("/")) {
            throw new IllegalArgumentException("invalid storage namespace");
        }
    }

    private void validateStorageKey(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            throw new IllegalArgumentException("upload storage key is required");
        }
        String key = storageKey.trim();
        if (key.contains("..") || key.contains("/") || key.contains("\\")) {
            throw new IllegalArgumentException("invalid upload storage key");
        }
    }

    private String normalizeStorageType(String rawStorageType) {
        if (!StringUtils.hasText(rawStorageType)) {
            return "local";
        }
        String normalized = rawStorageType.trim().toLowerCase(Locale.ROOT);
        if (!"local".equals(normalized) && !TYPE_S3.equals(normalized)) {
            throw new IllegalArgumentException("app.storage.type must be local or s3");
        }
        return normalized;
    }

    private String normalizePrefix(String rawPrefix) {
        if (!StringUtils.hasText(rawPrefix)) {
            return "";
        }
        String normalized = rawPrefix.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        if (normalized.contains("..") || normalized.contains("\\")) {
            throw new IllegalArgumentException("invalid S3 storage prefix");
        }
        return normalized + "/";
    }
}
