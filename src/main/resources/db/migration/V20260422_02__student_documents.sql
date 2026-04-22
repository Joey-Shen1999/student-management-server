CREATE TABLE IF NOT EXISTS student_document (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    student_id BIGINT NOT NULL,
    document_category VARCHAR(40) NOT NULL,
    identity_document_type VARCHAR(40),
    academic_record_type VARCHAR(40),
    report_year INTEGER,
    report_month VARCHAR(20),
    title VARCHAR(255) NOT NULL,
    notes VARCHAR(2000),
    storage_key VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    uploaded_by BIGINT NOT NULL,
    linked_identity_file_id BIGINT,
    linked_school_record_id BIGINT,
    linked_school_transcript_id BIGINT,
    CONSTRAINT fk_student_document_student
        FOREIGN KEY (student_id)
            REFERENCES students(id)
            ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_student_document_student_id
    ON student_document(student_id);
CREATE INDEX IF NOT EXISTS idx_student_document_uploaded_at
    ON student_document(uploaded_at);
CREATE INDEX IF NOT EXISTS idx_student_document_student_uploaded
    ON student_document(student_id, uploaded_at, id);
CREATE INDEX IF NOT EXISTS idx_student_document_linked_identity
    ON student_document(linked_identity_file_id);
CREATE INDEX IF NOT EXISTS idx_student_document_linked_transcript
    ON student_document(linked_school_transcript_id);
