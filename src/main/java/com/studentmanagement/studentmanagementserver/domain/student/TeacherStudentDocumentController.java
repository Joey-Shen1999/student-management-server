package com.studentmanagement.studentmanagementserver.domain.student;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/teacher/students/{studentId}/documents")
public class TeacherStudentDocumentController {

    private final TeacherStudentDocumentService teacherStudentDocumentService;

    public TeacherStudentDocumentController(TeacherStudentDocumentService teacherStudentDocumentService) {
        this.teacherStudentDocumentService = teacherStudentDocumentService;
    }

    @GetMapping
    public ResponseEntity<List<StudentDocumentDto>> listDocuments(@PathVariable Long studentId,
                                                                  HttpServletRequest request) {
        return ResponseEntity.ok(teacherStudentDocumentService.listDocuments(studentId, request));
    }

    @GetMapping("/history")
    public ResponseEntity<StudentDocumentHistoryListDto> listDocumentHistory(
            @PathVariable Long studentId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(teacherStudentDocumentService.listDocumentHistory(studentId, page, size, request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentDocumentDto> uploadDocument(
            @PathVariable Long studentId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "documentCategory", required = false) String documentCategory,
            @RequestParam(value = "identityDocumentType", required = false) String identityDocumentType,
            @RequestParam(value = "academicRecordType", required = false) String academicRecordType,
            @RequestParam(value = "reportYear", required = false) Integer reportYear,
            @RequestParam(value = "reportMonth", required = false) String reportMonth,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "notes", required = false) String notes,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(teacherStudentDocumentService.uploadDocument(
                studentId,
                file,
                documentCategory,
                identityDocumentType,
                academicRecordType,
                reportYear,
                reportMonth,
                title,
                notes,
                request
        ));
    }

    @GetMapping("/{documentId}/file")
    public ResponseEntity<byte[]> viewDocument(@PathVariable Long studentId,
                                               @PathVariable Long documentId,
                                               HttpServletRequest request) {
        StudentDocumentService.DocumentDownload download =
                teacherStudentDocumentService.downloadDocument(studentId, documentId, request);

        ContentDisposition contentDisposition = ContentDisposition.inline()
                .filename(download.getFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType(download.getContentType()))
                .contentLength(download.getContent().length)
                .body(download.getContent());
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long studentId,
                                               @PathVariable Long documentId,
                                               HttpServletRequest request) {
        teacherStudentDocumentService.deleteDocument(studentId, documentId, request);
        return ResponseEntity.noContent().build();
    }
}
