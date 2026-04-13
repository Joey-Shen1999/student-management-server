CREATE TABLE IF NOT EXISTS student_course_plan (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    student_id BIGINT NOT NULL,
    current_grade_level INTEGER,
    grade13_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_student_course_plan_student_id UNIQUE (student_id),
    CONSTRAINT fk_student_course_plan_student FOREIGN KEY (student_id) REFERENCES students (id)
);

CREATE TABLE IF NOT EXISTS student_course_plan_grade (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    course_plan_id BIGINT NOT NULL,
    grade_level INTEGER NOT NULL,
    year_structure VARCHAR(16) NOT NULL,
    CONSTRAINT uk_student_course_plan_grade_level UNIQUE (course_plan_id, grade_level),
    CONSTRAINT fk_student_course_plan_grade_plan FOREIGN KEY (course_plan_id) REFERENCES student_course_plan (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS student_course_plan_course (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    grade_id BIGINT NOT NULL,
    client_course_id VARCHAR(128) NOT NULL,
    course_code VARCHAR(64),
    status VARCHAR(16) NOT NULL,
    mark INTEGER,
    semester VARCHAR(2),
    sort_order INTEGER NOT NULL,
    CONSTRAINT uk_student_course_plan_course_key UNIQUE (grade_id, client_course_id),
    CONSTRAINT fk_student_course_plan_course_grade FOREIGN KEY (grade_id) REFERENCES student_course_plan_grade (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_student_course_plan_student_id ON student_course_plan (student_id);
CREATE INDEX IF NOT EXISTS idx_student_course_plan_grade_plan_id ON student_course_plan_grade (course_plan_id);
CREATE INDEX IF NOT EXISTS idx_student_course_plan_course_grade_sort ON student_course_plan_course (grade_id, sort_order);
