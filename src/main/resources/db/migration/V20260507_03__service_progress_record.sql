CREATE TABLE IF NOT EXISTS service_progress_record (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    student_id BIGINT NOT NULL,
    appointment_time TIMESTAMP NOT NULL,
    advisor_id BIGINT NOT NULL,
    follow_up_content VARCHAR(5000),
    next_plan VARCHAR(5000),
    CONSTRAINT fk_service_progress_record_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    CONSTRAINT fk_service_progress_record_advisor FOREIGN KEY (advisor_id) REFERENCES teachers (id)
);

CREATE INDEX IF NOT EXISTS idx_service_progress_student_id ON service_progress_record (student_id);
CREATE INDEX IF NOT EXISTS idx_service_progress_appointment_time ON service_progress_record (appointment_time);
