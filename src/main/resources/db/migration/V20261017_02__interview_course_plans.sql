CREATE TABLE interview_course_plans (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL UNIQUE REFERENCES students(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE interview_course_lessons (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES interview_course_plans(id) ON DELETE CASCADE,
    lesson_number INTEGER NOT NULL,
    lesson_date DATE,
    lesson_time TIME,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_interview_course_lesson_number UNIQUE (plan_id, lesson_number)
);

CREATE INDEX idx_interview_course_lessons_plan ON interview_course_lessons(plan_id);

CREATE TABLE interview_course_requirements (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES interview_course_plans(id) ON DELETE CASCADE,
    university_name VARCHAR(200) NOT NULL,
    program_name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_interview_course_requirements_plan ON interview_course_requirements(plan_id);
