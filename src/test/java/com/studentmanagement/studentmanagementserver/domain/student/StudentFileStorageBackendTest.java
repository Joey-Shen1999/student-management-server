package com.studentmanagement.studentmanagementserver.domain.student;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentFileStorageBackendTest {

    @TempDir
    Path tempDir;

    @Test
    void localStorageWritesReadsAndDeletesWithinNamespaceRoot() {
        byte[] data = "hello".getBytes();
        StudentFileStorageBackend backend = new StudentFileStorageBackend(
                "local",
                "",
                "",
                mock(ObjectProvider.class)
        );

        backend.store(
                "student-documents",
                tempDir.resolve("student-documents"),
                "student-1_document-test.pdf",
                new ByteArrayInputStream(data),
                data.length,
                "application/pdf"
        );

        assertArrayEquals(
                data,
                backend.readAllBytes(
                        "student-documents",
                        tempDir.resolve("student-documents"),
                        "student-1_document-test.pdf",
                        "not found"
                )
        );

        backend.deleteRequired(
                "student-documents",
                tempDir.resolve("student-documents"),
                "student-1_document-test.pdf"
        );

        assertThrows(
                ResponseStatusException.class,
                () -> backend.readAllBytes(
                        "student-documents",
                        tempDir.resolve("student-documents"),
                        "student-1_document-test.pdf",
                        "not found"
                )
        );
    }

    @Test
    void s3StorageUsesConfiguredPrefixAndNamespace() {
        byte[] data = "hello".getBytes();
        S3Client s3Client = mock(S3Client.class);
        ObjectProvider<S3Client> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(s3Client);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        StudentFileStorageBackend backend = new StudentFileStorageBackend(
                "s3",
                "globalvip-studentportal-prod-uploads-680458885427-us-east-2-an",
                "student-management-prod/",
                provider
        );

        backend.store(
                "student-documents",
                tempDir,
                "student-1_document-test.pdf",
                new ByteArrayInputStream(data),
                data.length,
                "application/pdf"
        );

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        PutObjectRequest request = requestCaptor.getValue();
        assertEquals("globalvip-studentportal-prod-uploads-680458885427-us-east-2-an", request.bucket());
        assertEquals("student-management-prod/student-documents/student-1_document-test.pdf", request.key());
        assertEquals("application/pdf", request.contentType());
    }
}
