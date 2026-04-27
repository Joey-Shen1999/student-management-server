CREATE TABLE IF NOT EXISTS student_document_history (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    student_id BIGINT NOT NULL,
    document_id BIGINT,
    action VARCHAR(20) NOT NULL,
    document_category VARCHAR(40),
    identity_document_type VARCHAR(40),
    academic_record_type VARCHAR(40),
    report_year INTEGER,
    report_month VARCHAR(20),
    title VARCHAR(255),
    notes VARCHAR(2000),
    file_name VARCHAR(255),
    content_type VARCHAR(120),
    size_bytes BIGINT,
    actor_user_id BIGINT,
    actor_role VARCHAR(40),
    actor_name VARCHAR(120),
    action_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_student_document_history_student
        FOREIGN KEY (student_id)
            REFERENCES students(id)
            ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_student_document_history_student_id
    ON student_document_history(student_id);
CREATE INDEX IF NOT EXISTS idx_student_document_history_document_id
    ON student_document_history(document_id);
CREATE INDEX IF NOT EXISTS idx_student_document_history_action_at
    ON student_document_history(action_at);
CREATE INDEX IF NOT EXISTS idx_student_document_history_actor_user_id
    ON student_document_history(actor_user_id);

INSERT INTO student_document_history (
    student_id,
    document_id,
    action,
    document_category,
    identity_document_type,
    academic_record_type,
    report_year,
    report_month,
    title,
    notes,
    file_name,
    content_type,
    size_bytes,
    actor_user_id,
    actor_role,
    actor_name,
    action_at
)
SELECT
    d.student_id,
    d.id,
    'UPLOAD',
    d.document_category,
    d.identity_document_type,
    d.academic_record_type,
    d.report_year,
    d.report_month,
    d.title,
    d.notes,
    d.original_filename,
    d.mime_type,
    d.size_bytes,
    d.uploaded_by,
    u.role,
    COALESCE(t.name, NULLIF(TRIM(CONCAT(COALESCE(s.nick_name, ''), '')), ''), u.username),
    d.uploaded_at
FROM student_document d
LEFT JOIN users u
    ON u.id = d.uploaded_by
LEFT JOIN teachers t
    ON t.user_id = u.id
LEFT JOIN students s
    ON s.user_id = u.id
WHERE NOT EXISTS (
    SELECT 1
    FROM student_document_history h
    WHERE h.document_id = d.id
      AND h.action = 'UPLOAD'
);
