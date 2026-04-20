ALTER TABLE student_profile
    ADD COLUMN IF NOT EXISTS profile_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS student_profile_change_events (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    student_id BIGINT NOT NULL,
    from_version BIGINT NOT NULL,
    to_version BIGINT NOT NULL,
    change_source VARCHAR(40) NOT NULL,
    changed_fields_json TEXT,
    actor_user_id BIGINT,
    actor_role VARCHAR(40),
    actor_name VARCHAR(120),
    changed_at TIMESTAMP NOT NULL,
    request_id VARCHAR(120),
    CONSTRAINT fk_student_profile_change_events_student
        FOREIGN KEY (student_id)
            REFERENCES students(id)
            ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_student_profile_change_events_student_id
    ON student_profile_change_events(student_id);
CREATE INDEX IF NOT EXISTS idx_student_profile_change_events_changed_at
    ON student_profile_change_events(changed_at);
CREATE INDEX IF NOT EXISTS idx_student_profile_change_events_to_version
    ON student_profile_change_events(to_version);
CREATE INDEX IF NOT EXISTS idx_student_profile_change_events_actor_user_id
    ON student_profile_change_events(actor_user_id);

CREATE TABLE IF NOT EXISTS student_profile_versions (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    student_id BIGINT NOT NULL,
    profile_version BIGINT NOT NULL,
    profile_snapshot_json TEXT NOT NULL,
    snapshot_hash VARCHAR(128),
    previous_hash VARCHAR(128),
    changed_by_user_id BIGINT,
    changed_by_role VARCHAR(40),
    changed_at TIMESTAMP NOT NULL,
    change_event_id BIGINT,
    request_id VARCHAR(120),
    CONSTRAINT fk_student_profile_versions_student
        FOREIGN KEY (student_id)
            REFERENCES students(id)
            ON DELETE CASCADE,
    CONSTRAINT fk_student_profile_versions_change_event
        FOREIGN KEY (change_event_id)
            REFERENCES student_profile_change_events(id)
            ON DELETE SET NULL,
    CONSTRAINT uk_student_profile_versions_student_version
        UNIQUE (student_id, profile_version)
);

CREATE INDEX IF NOT EXISTS idx_student_profile_versions_student_id
    ON student_profile_versions(student_id);
CREATE INDEX IF NOT EXISTS idx_student_profile_versions_profile_version
    ON student_profile_versions(profile_version);
