CREATE TABLE IF NOT EXISTS graduation_application_change_events (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    student_id BIGINT NOT NULL,
    application_id BIGINT,
    operation VARCHAR(60) NOT NULL,
    changed_fields_json TEXT,
    actor_user_id BIGINT,
    actor_role VARCHAR(40),
    actor_name VARCHAR(120),
    changed_at TIMESTAMP NOT NULL,
    request_id VARCHAR(120),
    CONSTRAINT fk_graduation_application_change_events_student
        FOREIGN KEY (student_id)
            REFERENCES students(id)
            ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_grad_app_change_events_student_id
    ON graduation_application_change_events(student_id);
CREATE INDEX IF NOT EXISTS idx_grad_app_change_events_application_id
    ON graduation_application_change_events(application_id);
CREATE INDEX IF NOT EXISTS idx_grad_app_change_events_changed_at
    ON graduation_application_change_events(changed_at);
CREATE INDEX IF NOT EXISTS idx_grad_app_change_events_actor_user_id
    ON graduation_application_change_events(actor_user_id);
CREATE INDEX IF NOT EXISTS idx_grad_app_change_events_operation
    ON graduation_application_change_events(operation);
