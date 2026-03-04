package com.studentmanagement.studentmanagementserver.domain.student;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/student/profile")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    public StudentProfileController(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    @GetMapping
    public ResponseEntity<StudentProfileDto> getProfile(HttpServletRequest request) {
        return ResponseEntity.ok(studentProfileService.getCurrentStudentProfile(request));
    }

    @PutMapping
    public ResponseEntity<StudentProfileDto> saveProfile(@RequestBody(required = false) StudentProfileDto requestBody,
                                                         HttpServletRequest request) {
        return ResponseEntity.ok(studentProfileService.saveCurrentStudentProfile(requestBody, request));
    }

    @PostMapping(value = "/schools/{schoolRecordId}/transcript", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentSchoolTranscriptDto> uploadSchoolTranscript(@PathVariable Long schoolRecordId,
                                                                             @RequestParam("file") MultipartFile file,
                                                                             HttpServletRequest request) {
        return ResponseEntity.ok(studentProfileService.uploadCurrentStudentSchoolTranscript(schoolRecordId, file, request));
    }

    @GetMapping("/schools/{schoolRecordId}/transcript")
    public ResponseEntity<byte[]> downloadSchoolTranscript(@PathVariable Long schoolRecordId,
                                                           HttpServletRequest request) {
        StudentProfileService.SchoolTranscriptDownload download =
                studentProfileService.downloadCurrentStudentSchoolTranscript(schoolRecordId, request);
        return buildTranscriptDownloadResponse(download);
    }

    @GetMapping("/schools/{schoolRecordId}/transcripts/{transcriptId}")
    public ResponseEntity<byte[]> downloadSchoolTranscriptById(@PathVariable Long schoolRecordId,
                                                               @PathVariable Long transcriptId,
                                                               HttpServletRequest request) {
        StudentProfileService.SchoolTranscriptDownload download =
                studentProfileService.downloadCurrentStudentSchoolTranscriptByTranscriptId(
                        schoolRecordId,
                        transcriptId,
                        request
                );
        return buildTranscriptDownloadResponse(download);
    }

    private ResponseEntity<byte[]> buildTranscriptDownloadResponse(StudentProfileService.SchoolTranscriptDownload download) {
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(download.getFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType(download.getContentType()))
                .contentLength(download.getContent().length)
                .body(download.getContent());
    }
}
