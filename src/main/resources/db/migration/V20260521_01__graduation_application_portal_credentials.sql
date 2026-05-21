CREATE TABLE IF NOT EXISTS graduation_application_portal_credentials (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    student_id BIGINT NOT NULL,
    university_id BIGINT NOT NULL,
    school_account VARCHAR(160),
    school_email VARCHAR(200),
    school_password VARCHAR(200),
    CONSTRAINT fk_grad_app_portal_credentials_student
        FOREIGN KEY (student_id)
            REFERENCES students(id)
            ON DELETE CASCADE,
    CONSTRAINT fk_grad_app_portal_credentials_university
        FOREIGN KEY (university_id)
            REFERENCES universities(id),
    CONSTRAINT uk_grad_app_portal_credentials_student_university
        UNIQUE (student_id, university_id)
);

CREATE INDEX IF NOT EXISTS idx_grad_app_portal_credentials_student
    ON graduation_application_portal_credentials(student_id);
CREATE INDEX IF NOT EXISTS idx_grad_app_portal_credentials_university
    ON graduation_application_portal_credentials(university_id);
