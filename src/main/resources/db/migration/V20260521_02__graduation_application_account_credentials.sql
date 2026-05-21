CREATE TABLE IF NOT EXISTS graduation_application_account_credentials (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    student_id BIGINT NOT NULL,
    application_email VARCHAR(200),
    application_password VARCHAR(200),
    CONSTRAINT fk_grad_app_account_credentials_student
        FOREIGN KEY (student_id)
            REFERENCES students(id)
            ON DELETE CASCADE,
    CONSTRAINT uk_grad_app_account_credentials_student
        UNIQUE (student_id)
);

CREATE INDEX IF NOT EXISTS idx_grad_app_account_credentials_student
    ON graduation_application_account_credentials(student_id);
