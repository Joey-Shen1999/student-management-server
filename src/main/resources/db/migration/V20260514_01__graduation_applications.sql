CREATE TABLE IF NOT EXISTS graduation_applications (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    student_id BIGINT NOT NULL,
    university_id BIGINT NOT NULL,
    program_id BIGINT NOT NULL,
    source_aspiration_id BIGINT,
    status VARCHAR(40) NOT NULL DEFAULT 'PREPARING',
    sort_order INTEGER NOT NULL,
    CONSTRAINT fk_graduation_applications_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    CONSTRAINT fk_graduation_applications_university FOREIGN KEY (university_id) REFERENCES universities (id),
    CONSTRAINT fk_graduation_applications_program FOREIGN KEY (program_id) REFERENCES university_programs (id)
);

CREATE INDEX IF NOT EXISTS idx_graduation_applications_student_sort ON graduation_applications (student_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_graduation_applications_university ON graduation_applications (university_id);
CREATE INDEX IF NOT EXISTS idx_graduation_applications_program ON graduation_applications (program_id);
CREATE INDEX IF NOT EXISTS idx_graduation_applications_status ON graduation_applications (status);
