-- Multi identity files for student profile.
CREATE TABLE IF NOT EXISTS student_identity_file (
    id BIGSERIAL PRIMARY KEY,
    student_profile_id BIGINT NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    uploaded_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW() NOT NULL,
    CONSTRAINT fk_student_identity_file_profile
        FOREIGN KEY (student_profile_id)
            REFERENCES student_profile(id)
            ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_student_identity_file_profile_id
    ON student_identity_file(student_profile_id);

CREATE INDEX IF NOT EXISTS idx_student_identity_file_uploaded_at
    ON student_identity_file(uploaded_at);
