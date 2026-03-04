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
@RequestMapping("/api/teacher/students/{studentId}/profile")
public class TeacherStudentProfileController {

    private final TeacherStudentProfileService teacherStudentProfileService;

    public TeacherStudentProfileController(TeacherStudentProfileService teacherStudentProfileService) {
        this.teacherStudentProfileService = teacherStudentProfileService;
    }

    @GetMapping
    public ResponseEntity<StudentProfileDto> getProfile(@PathVariable Long studentId,
                                                        HttpServletRequest request) {
        return ResponseEntity.ok(teacherStudentProfileService.getProfile(studentId, request));
    }

    @PutMapping
    public ResponseEntity<StudentProfileDto> saveProfile(@PathVariable Long studentId,
                                                         @RequestBody(required = false) StudentProfileDto requestBody,
                                                         HttpServletRequest request) {
        return ResponseEntity.ok(teacherStudentProfileService.saveProfile(studentId, requestBody, request));
    }

    @PostMapping(value = "/schools/{schoolRecordId}/transcript", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentSchoolTranscriptDto> uploadSchoolTranscript(@PathVariable Long studentId,
                                                                              @PathVariable Long schoolRecordId,
                                                                              @RequestParam(value = "file", required = false) MultipartFile file,
                                                                              @RequestParam(value = "transcript", required = false) MultipartFile transcript,
                                                                              HttpServletRequest request) {
        MultipartFile effectiveFile = chooseUploadFile(file, transcript);
        return ResponseEntity.ok(
                teacherStudentProfileService.uploadSchoolTranscript(studentId, schoolRecordId, effectiveFile, request)
        );
    }

    @PostMapping(value = "/identity-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentIdentityFileUploadDto> uploadIdentityFile(@PathVariable Long studentId,
                                                                            @RequestParam(value = "file", required = false) MultipartFile file,
                                                                            @RequestParam(value = "identity", required = false) MultipartFile identity,
                                                                            HttpServletRequest request) {
        MultipartFile effectiveFile = chooseUploadFile(file, identity);
        return ResponseEntity.ok(
                teacherStudentProfileService.uploadIdentityFile(studentId, effectiveFile, request)
        );
    }

    @GetMapping("/schools/{schoolRecordId}/transcript")
    public ResponseEntity<byte[]> downloadSchoolTranscript(@PathVariable Long studentId,
                                                           @PathVariable Long schoolRecordId,
                                                           HttpServletRequest request) {
        StudentProfileService.SchoolTranscriptDownload download =
                teacherStudentProfileService.downloadSchoolTranscript(studentId, schoolRecordId, request);
        return buildTranscriptDownloadResponse(download);
    }

    @GetMapping("/schools/{schoolRecordId}/transcripts/{transcriptId}")
    public ResponseEntity<byte[]> downloadSchoolTranscriptById(@PathVariable Long studentId,
                                                               @PathVariable Long schoolRecordId,
                                                               @PathVariable Long transcriptId,
                                                               HttpServletRequest request) {
        StudentProfileService.SchoolTranscriptDownload download =
                teacherStudentProfileService.downloadSchoolTranscriptByTranscriptId(
                        studentId,
                        schoolRecordId,
                        transcriptId,
                        request
                );
        return buildTranscriptDownloadResponse(download);
    }

    @GetMapping("/identity-files/{identityFileId}")
    public ResponseEntity<byte[]> downloadIdentityFileById(@PathVariable Long studentId,
                                                           @PathVariable Long identityFileId,
                                                           HttpServletRequest request) {
        StudentProfileService.IdentityFileDownload download =
                teacherStudentProfileService.downloadIdentityFileByIdentityFileId(studentId, identityFileId, request);
        return buildIdentityDownloadResponse(download);
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

    private ResponseEntity<byte[]> buildIdentityDownloadResponse(StudentProfileService.IdentityFileDownload download) {
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(download.getFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType(download.getContentType()))
                .contentLength(download.getContent().length)
                .body(download.getContent());
    }

    private MultipartFile chooseUploadFile(MultipartFile file, MultipartFile transcript) {
        if (file != null && !file.isEmpty()) {
            return file;
        }
        if (transcript != null && !transcript.isEmpty()) {
            return transcript;
        }
        return file != null ? file : transcript;
    }
}
