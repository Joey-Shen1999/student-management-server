CREATE TABLE IF NOT EXISTS student_profile_service_item (
    student_profile_id BIGINT NOT NULL,
    item_order INTEGER NOT NULL,
    service_item VARCHAR(120) NOT NULL,
    CONSTRAINT pk_student_profile_service_item PRIMARY KEY (student_profile_id, item_order),
    CONSTRAINT fk_student_profile_service_item_profile
        FOREIGN KEY (student_profile_id)
            REFERENCES student_profile(id)
            ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_student_profile_service_item_profile_id
    ON student_profile_service_item(student_profile_id);
