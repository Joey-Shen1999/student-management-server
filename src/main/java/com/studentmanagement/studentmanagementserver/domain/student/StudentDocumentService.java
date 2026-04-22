package com.studentmanagement.studentmanagementserver.domain.student;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentDocumentRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentIdentityFileRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentProfileRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentSchoolRecordRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentSchoolTranscriptRepository;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class StudentDocumentService {

    public static final String DOCUMENT_CATEGORY_IDENTITY = "Identity Document";
    public static final String DOCUMENT_CATEGORY_ACADEMIC = "Academic Record";
    public static final String DOCUMENT_CATEGORY_OTHER = "Other";

    public static final String IDENTITY_DOCUMENT_TYPE_PASSPORT = "Passport";
    public static final String IDENTITY_DOCUMENT_TYPE_STUDY_PERMIT_VISA = "Study Permit / Visa";
    public static final String IDENTITY_DOCUMENT_TYPE_PR_CARD = "PR Card";
    public static final String IDENTITY_DOCUMENT_TYPE_OTHER = "Other";

    public static final String ACADEMIC_RECORD_TYPE_TRANSCRIPT = "Transcript";
    public static final String ACADEMIC_RECORD_TYPE_REPORT_CARD = "Report Card";

    private static final long MAX_UPLOAD_SIZE_BYTES = 50L * 1024L * 1024L;
    private static final int MIN_REPORT_YEAR = 1900;
    private static final int MAX_REPORT_YEAR = 2200;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final Set<String> DOCUMENT_CATEGORIES = unmodifiableSet(
            DOCUMENT_CATEGORY_IDENTITY,
            DOCUMENT_CATEGORY_ACADEMIC,
            DOCUMENT_CATEGORY_OTHER
    );

    private static final Set<String> IDENTITY_DOCUMENT_TYPES = unmodifiableSet(
            IDENTITY_DOCUMENT_TYPE_PASSPORT,
            IDENTITY_DOCUMENT_TYPE_STUDY_PERMIT_VISA,
            IDENTITY_DOCUMENT_TYPE_PR_CARD,
            IDENTITY_DOCUMENT_TYPE_OTHER
    );

    private static final Set<String> ACADEMIC_RECORD_TYPES = unmodifiableSet(
            ACADEMIC_RECORD_TYPE_TRANSCRIPT,
            ACADEMIC_RECORD_TYPE_REPORT_CARD
    );

    private static final Set<String> REPORT_MONTHS = unmodifiableSet(
            "January",
            "February",
            "March",
            "April",
            "May",
            "June",
            "July",
            "August",
            "September",
            "October",
            "November",
            "December"
    );

    private final AuthSessionService authSessionService;
    private final StudentRepository studentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final StudentIdentityFileRepository studentIdentityFileRepository;
    private final StudentSchoolRecordRepository studentSchoolRecordRepository;
    private final StudentSchoolTranscriptRepository studentSchoolTranscriptRepository;
    private final StudentDocumentStorageService studentDocumentStorageService;
    private final StudentIdentityFileStorageService identityFileStorageService;
    private final StudentSchoolTranscriptStorageService transcriptStorageService;

    public StudentDocumentService(AuthSessionService authSessionService,
                                  StudentRepository studentRepository,
                                  StudentProfileRepository studentProfileRepository,
                                  StudentDocumentRepository studentDocumentRepository,
                                  StudentIdentityFileRepository studentIdentityFileRepository,
                                  StudentSchoolRecordRepository studentSchoolRecordRepository,
                                  StudentSchoolTranscriptRepository studentSchoolTranscriptRepository,
                                  StudentDocumentStorageService studentDocumentStorageService,
                                  StudentIdentityFileStorageService identityFileStorageService,
                                  StudentSchoolTranscriptStorageService transcriptStorageService) {
        this.authSessionService = authSessionService;
        this.studentRepository = studentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentDocumentRepository = studentDocumentRepository;
        this.studentIdentityFileRepository = studentIdentityFileRepository;
        this.studentSchoolRecordRepository = studentSchoolRecordRepository;
        this.studentSchoolTranscriptRepository = studentSchoolTranscriptRepository;
        this.studentDocumentStorageService = studentDocumentStorageService;
        this.identityFileStorageService = identityFileStorageService;
        this.transcriptStorageService = transcriptStorageService;
    }

    @Transactional(readOnly = true)
    public List<StudentDocumentDto> listCurrentStudentDocuments(HttpServletRequest request) {
        Student student = requireCurrentStudent(request);
        List<StudentDocument> documents = studentDocumentRepository.findByStudent_IdOrderByUploadedAtDescIdDesc(student.getId());
        List<StudentDocumentDto> items = new ArrayList<StudentDocumentDto>();
        for (StudentDocument document : documents) {
            items.add(toDto(document));
        }
        return items;
    }

    @Transactional
    public StudentDocumentDto uploadCurrentStudentDocument(MultipartFile file,
                                                           String documentCategory,
                                                           String identityDocumentType,
                                                           String academicRecordType,
                                                           Integer reportYear,
                                                           String reportMonth,
                                                           String title,
                                                           String notes,
                                                           HttpServletRequest request) {
        Student student = requireCurrentStudent(request);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        assertUploadSizeWithinLimit(file);
        assertPdfUpload(file);

        NormalizedUploadMetadata normalized = normalizeUploadMetadata(
                documentCategory,
                identityDocumentType,
                academicRecordType,
                reportYear,
                reportMonth,
                title,
                notes
        );

        LocalDateTime now = LocalDateTime.now();
        StudentDocumentStorageService.StoredDocument stored = studentDocumentStorageService.store(student.getId(), file);

        StudentDocument document = new StudentDocument(
                student,
                normalized.documentCategory,
                normalized.identityDocumentType,
                normalized.academicRecordType,
                normalized.reportYear,
                normalized.reportMonth,
                normalized.title,
                normalized.notes,
                stored.getStorageKey(),
                stored.getOriginalFilename(),
                stored.getContentType(),
                Long.valueOf(stored.getSizeBytes()),
                now,
                student.getUser().getId()
        );
        StudentDocument persisted = studentDocumentRepository.save(document);
        return toDto(persisted);
    }

    @Transactional(readOnly = true)
    public DocumentDownload downloadCurrentStudentDocument(Long documentId, HttpServletRequest request) {
        Student student = requireCurrentStudent(request);
        StudentDocument document = requireOwnedDocument(student, documentId);
        byte[] content = studentDocumentStorageService.readAllBytes(document.getStorageKey());

        String fileName = trimToNull(document.getOriginalFilename());
        if (fileName == null) {
            fileName = "student-document.pdf";
        }
        String contentType = trimToNull(document.getMimeType());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return new DocumentDownload(fileName, contentType, content);
    }

    @Transactional
    public void deleteCurrentStudentDocument(Long documentId, HttpServletRequest request) {
        Student student = requireCurrentStudent(request);
        StudentDocument document = requireOwnedDocument(student, documentId);
        deleteLinkedLegacyRows(student, document);
        deleteDocumentStorageOrThrow(document, "student_delete");
        studentDocumentRepository.delete(document);
    }

    @Transactional
    public void createLinkedIdentityDocument(Student student,
                                             StudentIdentityFile identityFile,
                                             MultipartFile sourceFile,
                                             String identityDocumentType,
                                             Long uploadedBy) {
        if (student == null || identityFile == null || sourceFile == null || sourceFile.isEmpty()) {
            return;
        }

        String normalizedIdentityType = normalizeIdentityDocumentType(identityDocumentType);
        if (normalizedIdentityType == null) {
            normalizedIdentityType = IDENTITY_DOCUMENT_TYPE_OTHER;
        }

        StudentDocumentStorageService.StoredDocument stored = studentDocumentStorageService.store(student.getId(), sourceFile);
        StudentDocument document = new StudentDocument(
                student,
                DOCUMENT_CATEGORY_IDENTITY,
                normalizedIdentityType,
                null,
                null,
                null,
                buildLinkedIdentityTitle(identityFile),
                null,
                stored.getStorageKey(),
                stored.getOriginalFilename(),
                stored.getContentType(),
                Long.valueOf(stored.getSizeBytes()),
                identityFile.getUploadedAt() == null ? LocalDateTime.now() : identityFile.getUploadedAt(),
                uploadedBy == null ? student.getUser().getId() : uploadedBy
        );
        document.setLinkedIdentityFileId(identityFile.getId());
        studentDocumentRepository.save(document);
    }

    @Transactional
    public void createLinkedAcademicDocument(Student student,
                                             StudentSchoolRecord schoolRecord,
                                             StudentSchoolTranscript transcript,
                                             MultipartFile sourceFile,
                                             String academicRecordType,
                                             Integer reportYear,
                                             String reportMonth,
                                             Long uploadedBy) {
        if (student == null || schoolRecord == null || transcript == null || sourceFile == null || sourceFile.isEmpty()) {
            return;
        }

        NormalizedAcademicMetadata normalized = normalizeAcademicMetadata(academicRecordType, reportYear, reportMonth);
        StudentDocumentStorageService.StoredDocument stored = studentDocumentStorageService.store(student.getId(), sourceFile);

        StudentDocument document = new StudentDocument(
                student,
                DOCUMENT_CATEGORY_ACADEMIC,
                null,
                normalized.academicRecordType,
                normalized.reportYear,
                normalized.reportMonth,
                buildLinkedAcademicTitle(schoolRecord, normalized),
                null,
                stored.getStorageKey(),
                stored.getOriginalFilename(),
                stored.getContentType(),
                Long.valueOf(stored.getSizeBytes()),
                transcript.getUploadedAt() == null ? LocalDateTime.now() : transcript.getUploadedAt(),
                uploadedBy == null ? student.getUser().getId() : uploadedBy
        );
        document.setLinkedSchoolRecordId(schoolRecord.getId());
        document.setLinkedSchoolTranscriptId(transcript.getId());
        studentDocumentRepository.save(document);
    }

    @Transactional
    public void deleteDocumentsLinkedToIdentityFile(Long identityFileId) {
        if (identityFileId == null) {
            return;
        }
        List<StudentDocument> linked = studentDocumentRepository.findByLinkedIdentityFileId(identityFileId);
        for (StudentDocument document : linked) {
            deleteDocumentStorageOrThrow(document, "linked_identity_removed");
            studentDocumentRepository.delete(document);
        }
    }

    @Transactional
    public void deleteDocumentsLinkedToSchoolTranscript(Long schoolTranscriptId) {
        if (schoolTranscriptId == null) {
            return;
        }
        List<StudentDocument> linked = studentDocumentRepository.findByLinkedSchoolTranscriptId(schoolTranscriptId);
        for (StudentDocument document : linked) {
            deleteDocumentStorageOrThrow(document, "linked_transcript_removed");
            studentDocumentRepository.delete(document);
        }
    }

    private Student requireCurrentStudent(HttpServletRequest request) {
        User user = authSessionService.requireAuthenticatedUser(request);
        if (user.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: student role required.");
        }
        return studentRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found."));
    }

    private StudentDocument requireOwnedDocument(Student student, Long documentId) {
        if (documentId == null || documentId.longValue() <= 0L) {
            throw new IllegalArgumentException("documentId must be positive");
        }
        return studentDocumentRepository.findByIdAndStudent_Id(documentId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found."));
    }

    private void deleteLinkedLegacyRows(Student student, StudentDocument document) {
        Long linkedIdentityFileId = document.getLinkedIdentityFileId();
        if (linkedIdentityFileId != null) {
            deleteLinkedIdentityFile(student, document, linkedIdentityFileId);
        }

        Long linkedTranscriptId = document.getLinkedSchoolTranscriptId();
        if (linkedTranscriptId != null) {
            deleteLinkedTranscript(student, document, linkedTranscriptId);
        }
    }

    private void deleteLinkedIdentityFile(Student student,
                                          StudentDocument currentDocument,
                                          Long linkedIdentityFileId) {
        StudentProfile profile = studentProfileRepository.findByStudent_Id(student.getId()).orElse(null);
        if (profile == null) {
            return;
        }

        StudentIdentityFile identityFile = studentIdentityFileRepository
                .findByIdAndStudentProfile_Id(linkedIdentityFileId, profile.getId())
                .orElse(null);
        if (identityFile == null) {
            return;
        }

        deleteIdentityStorageOrThrow(identityFile);
        studentIdentityFileRepository.delete(identityFile);

        // Delete all sibling rows linked to the same identity file to avoid stale duplicates.
        List<StudentDocument> siblings = studentDocumentRepository.findByLinkedIdentityFileId(linkedIdentityFileId);
        for (StudentDocument sibling : siblings) {
            if (currentDocument.getId() != null && currentDocument.getId().equals(sibling.getId())) {
                continue;
            }
            deleteDocumentStorageOrThrow(sibling, "linked_identity_sibling_cleanup");
            studentDocumentRepository.delete(sibling);
        }
    }

    private void deleteLinkedTranscript(Student student,
                                        StudentDocument currentDocument,
                                        Long linkedTranscriptId) {
        StudentSchoolRecord schoolRecord = null;
        Long linkedSchoolRecordId = currentDocument.getLinkedSchoolRecordId();
        if (linkedSchoolRecordId != null) {
            schoolRecord = studentSchoolRecordRepository.findByIdAndStudent_Id(linkedSchoolRecordId, student.getId())
                    .orElse(null);
        }

        StudentSchoolTranscript transcript = null;
        if (schoolRecord != null) {
            transcript = studentSchoolTranscriptRepository.findByIdAndSchoolRecord_Id(linkedTranscriptId, schoolRecord.getId())
                    .orElse(null);
        }
        if (transcript == null) {
            StudentSchoolTranscript candidate = studentSchoolTranscriptRepository.findById(linkedTranscriptId).orElse(null);
            if (candidate != null
                    && candidate.getSchoolRecord() != null
                    && candidate.getSchoolRecord().getStudent() != null
                    && student.getId().equals(candidate.getSchoolRecord().getStudent().getId())) {
                transcript = candidate;
                schoolRecord = candidate.getSchoolRecord();
            }
        }
        if (transcript == null || schoolRecord == null) {
            return;
        }

        deleteTranscriptStorageOrThrow(transcript);
        studentSchoolTranscriptRepository.delete(transcript);
        refreshLegacyTranscriptFields(schoolRecord);

        List<StudentDocument> siblings = studentDocumentRepository.findByLinkedSchoolTranscriptId(linkedTranscriptId);
        for (StudentDocument sibling : siblings) {
            if (currentDocument.getId() != null && currentDocument.getId().equals(sibling.getId())) {
                continue;
            }
            deleteDocumentStorageOrThrow(sibling, "linked_transcript_sibling_cleanup");
            studentDocumentRepository.delete(sibling);
        }
    }

    private void refreshLegacyTranscriptFields(StudentSchoolRecord schoolRecord) {
        List<StudentSchoolTranscript> transcripts =
                studentSchoolTranscriptRepository.findBySchoolRecord_IdOrderByUploadedAtDescIdDesc(schoolRecord.getId());
        if (transcripts.isEmpty()) {
            schoolRecord.setTranscriptOriginalFilename(null);
            schoolRecord.setTranscriptContentType(null);
            schoolRecord.setTranscriptStorageKey(null);
            schoolRecord.setTranscriptSizeBytes(null);
            schoolRecord.setTranscriptUploadedAt(null);
            studentSchoolRecordRepository.save(schoolRecord);
            return;
        }

        StudentSchoolTranscript latest = transcripts.get(0);
        schoolRecord.setTranscriptOriginalFilename(latest.getOriginalFilename());
        schoolRecord.setTranscriptContentType(latest.getMimeType());
        schoolRecord.setTranscriptStorageKey(latest.getStorageKey());
        schoolRecord.setTranscriptSizeBytes(latest.getSizeBytes());
        schoolRecord.setTranscriptUploadedAt(latest.getUploadedAt());
        studentSchoolRecordRepository.save(schoolRecord);
    }

    private void deleteDocumentStorageOrThrow(StudentDocument document, String reason) {
        try {
            studentDocumentStorageService.deleteRequired(document.getStorageKey());
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Failed to delete student document file. reason=" + reason
            );
        }
    }

    private void deleteIdentityStorageOrThrow(StudentIdentityFile identityFile) {
        try {
            identityFileStorageService.deleteRequired(identityFile.getStorageKey());
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Failed to delete identity file.");
        }
    }

    private void deleteTranscriptStorageOrThrow(StudentSchoolTranscript transcript) {
        try {
            transcriptStorageService.deleteRequired(transcript.getStorageKey());
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Failed to delete transcript file.");
        }
    }

    private void assertUploadSizeWithinLimit(MultipartFile file) {
        long size = file == null ? 0L : file.getSize();
        if (size > MAX_UPLOAD_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds maximum upload size.");
        }
    }

    private void assertPdfUpload(MultipartFile file) {
        String fileName = trimToNull(file == null ? null : file.getOriginalFilename());
        String contentType = trimToNull(file == null ? null : file.getContentType());
        boolean byName = fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".pdf");
        boolean byContentType = contentType != null && contentType.equalsIgnoreCase("application/pdf");

        if (!byName && !byContentType) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF files are supported.");
        }
    }

    private NormalizedUploadMetadata normalizeUploadMetadata(String documentCategory,
                                                             String identityDocumentType,
                                                             String academicRecordType,
                                                             Integer reportYear,
                                                             String reportMonth,
                                                             String title,
                                                             String notes) {
        String normalizedCategory = normalizeDocumentCategory(documentCategory);
        if (normalizedCategory == null) {
            throw new IllegalArgumentException("documentCategory is required");
        }

        String normalizedTitle = limitLength(trimToNull(title), 255);
        if (normalizedTitle == null) {
            throw new IllegalArgumentException("title is required");
        }
        String normalizedNotes = limitLength(trimToNull(notes), 2000);

        if (DOCUMENT_CATEGORY_IDENTITY.equals(normalizedCategory)) {
            String normalizedIdentityType = normalizeIdentityDocumentType(identityDocumentType);
            if (normalizedIdentityType == null) {
                throw new IllegalArgumentException(
                        "identityDocumentType must be one of: " + String.join(", ", IDENTITY_DOCUMENT_TYPES)
                );
            }
            return new NormalizedUploadMetadata(
                    normalizedCategory,
                    normalizedIdentityType,
                    null,
                    null,
                    null,
                    normalizedTitle,
                    normalizedNotes
            );
        }

        if (DOCUMENT_CATEGORY_ACADEMIC.equals(normalizedCategory)) {
            NormalizedAcademicMetadata normalizedAcademic = normalizeAcademicMetadata(
                    academicRecordType,
                    reportYear,
                    reportMonth
            );
            return new NormalizedUploadMetadata(
                    normalizedCategory,
                    null,
                    normalizedAcademic.academicRecordType,
                    normalizedAcademic.reportYear,
                    normalizedAcademic.reportMonth,
                    normalizedTitle,
                    normalizedNotes
            );
        }

        return new NormalizedUploadMetadata(
                normalizedCategory,
                null,
                null,
                null,
                null,
                normalizedTitle,
                normalizedNotes
        );
    }

    private NormalizedAcademicMetadata normalizeAcademicMetadata(String academicRecordType,
                                                                 Integer reportYear,
                                                                 String reportMonth) {
        String normalizedAcademicType = normalizeAcademicRecordType(academicRecordType);
        if (normalizedAcademicType == null) {
            normalizedAcademicType = ACADEMIC_RECORD_TYPE_TRANSCRIPT;
        }

        if (ACADEMIC_RECORD_TYPE_REPORT_CARD.equals(normalizedAcademicType)) {
            Integer normalizedYear = normalizeReportYear(reportYear);
            if (normalizedYear == null) {
                throw new IllegalArgumentException("reportYear is required when academicRecordType is Report Card");
            }
            String normalizedMonth = normalizeReportMonth(reportMonth);
            if (normalizedMonth == null) {
                throw new IllegalArgumentException("reportMonth is required when academicRecordType is Report Card");
            }
            return new NormalizedAcademicMetadata(normalizedAcademicType, normalizedYear, normalizedMonth);
        }

        return new NormalizedAcademicMetadata(normalizedAcademicType, null, null);
    }

    private String normalizeDocumentCategory(String value) {
        return normalizeEnum(value, DOCUMENT_CATEGORIES);
    }

    private String normalizeIdentityDocumentType(String value) {
        return normalizeEnum(value, IDENTITY_DOCUMENT_TYPES);
    }

    private String normalizeAcademicRecordType(String value) {
        return normalizeEnum(value, ACADEMIC_RECORD_TYPES);
    }

    private String normalizeReportMonth(String value) {
        return normalizeEnum(value, REPORT_MONTHS);
    }

    private Integer normalizeReportYear(Integer value) {
        if (value == null) {
            return null;
        }
        int year = value.intValue();
        if (year < MIN_REPORT_YEAR || year > MAX_REPORT_YEAR) {
            throw new IllegalArgumentException(
                    "reportYear must be between " + MIN_REPORT_YEAR + " and " + MAX_REPORT_YEAR
            );
        }
        return Integer.valueOf(year);
    }

    private String normalizeEnum(String value, Set<String> candidates) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (candidate.equalsIgnoreCase(text)) {
                return candidate;
            }
        }
        return null;
    }

    private StudentDocumentDto toDto(StudentDocument document) {
        StudentDocumentDto dto = new StudentDocumentDto();
        dto.setId(document.getId());
        dto.setDocumentCategory(document.getDocumentCategory());
        dto.setIdentityDocumentType(document.getIdentityDocumentType());
        dto.setAcademicRecordType(document.getAcademicRecordType());
        dto.setReportYear(document.getReportYear());
        dto.setReportMonth(document.getReportMonth());
        dto.setTitle(document.getTitle());
        dto.setNotes(document.getNotes());
        dto.setFileName(document.getOriginalFilename());
        dto.setContentType(document.getMimeType());
        dto.setSizeBytes(document.getSizeBytes());
        dto.setUploadedAt(formatDateTime(document.getUploadedAt()));
        return dto;
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.format(DATE_TIME_FORMATTER);
    }

    private String buildLinkedIdentityTitle(StudentIdentityFile identityFile) {
        String fileName = trimToNull(identityFile.getOriginalFilename());
        if (fileName == null) {
            return "Identity Document";
        }
        return "Identity Document - " + fileName;
    }

    private String buildLinkedAcademicTitle(StudentSchoolRecord schoolRecord,
                                            NormalizedAcademicMetadata metadata) {
        String schoolName = trimToNull(schoolRecord.getSchoolName());
        if (ACADEMIC_RECORD_TYPE_REPORT_CARD.equals(metadata.academicRecordType)
                && metadata.reportYear != null
                && metadata.reportMonth != null) {
            if (schoolName == null) {
                return metadata.reportMonth + " " + metadata.reportYear + " Report Card";
            }
            return schoolName + " - " + metadata.reportMonth + " " + metadata.reportYear + " Report Card";
        }
        if (schoolName == null) {
            return "Transcript";
        }
        return schoolName + " - Transcript";
    }

    private String limitLength(String value, int maxLength) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Set<String> unmodifiableSet(String... values) {
        LinkedHashSet<String> set = new LinkedHashSet<String>(Arrays.asList(values));
        return Collections.unmodifiableSet(set);
    }

    public static class DocumentDownload {
        private final String fileName;
        private final String contentType;
        private final byte[] content;

        public DocumentDownload(String fileName, String contentType, byte[] content) {
            this.fileName = fileName;
            this.contentType = contentType;
            this.content = content;
        }

        public String getFileName() {
            return fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public byte[] getContent() {
            return content;
        }
    }

    private static class NormalizedUploadMetadata {
        private final String documentCategory;
        private final String identityDocumentType;
        private final String academicRecordType;
        private final Integer reportYear;
        private final String reportMonth;
        private final String title;
        private final String notes;

        private NormalizedUploadMetadata(String documentCategory,
                                         String identityDocumentType,
                                         String academicRecordType,
                                         Integer reportYear,
                                         String reportMonth,
                                         String title,
                                         String notes) {
            this.documentCategory = documentCategory;
            this.identityDocumentType = identityDocumentType;
            this.academicRecordType = academicRecordType;
            this.reportYear = reportYear;
            this.reportMonth = reportMonth;
            this.title = title;
            this.notes = notes;
        }
    }

    private static class NormalizedAcademicMetadata {
        private final String academicRecordType;
        private final Integer reportYear;
        private final String reportMonth;

        private NormalizedAcademicMetadata(String academicRecordType, Integer reportYear, String reportMonth) {
            this.academicRecordType = academicRecordType;
            this.reportYear = reportYear;
            this.reportMonth = reportMonth;
        }
    }
}
