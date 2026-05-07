CREATE TABLE IF NOT EXISTS universities (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    name VARCHAR(180) NOT NULL,
    province VARCHAR(80),
    city VARCHAR(120),
    country VARCHAR(80) NOT NULL DEFAULT 'Canada',
    website VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS university_programs (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    university_id BIGINT NOT NULL,
    program_name VARCHAR(180) NOT NULL,
    faculty_name VARCHAR(180),
    degree_type VARCHAR(40),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_university_programs_university FOREIGN KEY (university_id) REFERENCES universities (id)
);

CREATE TABLE IF NOT EXISTS university_aspirations (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    student_id BIGINT NOT NULL,
    university_id BIGINT NOT NULL,
    program_id BIGINT NOT NULL,
    notes TEXT,
    sort_order INTEGER NOT NULL,
    CONSTRAINT fk_university_aspirations_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    CONSTRAINT fk_university_aspirations_university FOREIGN KEY (university_id) REFERENCES universities (id),
    CONSTRAINT fk_university_aspirations_program FOREIGN KEY (program_id) REFERENCES university_programs (id)
);

CREATE INDEX IF NOT EXISTS idx_universities_active_name ON universities (active, name);
CREATE INDEX IF NOT EXISTS idx_universities_province_city ON universities (province, city);
CREATE INDEX IF NOT EXISTS idx_university_programs_university_active ON university_programs (university_id, active);
CREATE INDEX IF NOT EXISTS idx_university_programs_name ON university_programs (program_name);
CREATE INDEX IF NOT EXISTS idx_university_aspirations_student_sort ON university_aspirations (student_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_university_aspirations_university ON university_aspirations (university_id);
CREATE INDEX IF NOT EXISTS idx_university_aspirations_program ON university_aspirations (program_id);
