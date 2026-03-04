-- Create child table for 0..* transcripts per school record.
CREATE TABLE IF NOT EXISTS student_school_transcript (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    school_record_id BIGINT NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    uploaded_by BIGINT NOT NULL,
    CONSTRAINT fk_school_transcript_school_record
        FOREIGN KEY (school_record_id)
        REFERENCES student_school_record (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_school_transcript_school_record_id
    ON student_school_transcript (school_record_id);

CREATE INDEX IF NOT EXISTS idx_school_transcript_uploaded_at
    ON student_school_transcript (uploaded_at);

-- Backfill legacy single-file columns into child table.
INSERT INTO student_school_transcript (
    created_at,
    updated_at,
    school_record_id,
    storage_key,
    original_filename,
    mime_type,
    size_bytes,
    uploaded_at,
    uploaded_by
)
SELECT
    COALESCE(ssr.transcript_uploaded_at, ssr.updated_at, NOW()) AS created_at,
    COALESCE(ssr.transcript_uploaded_at, ssr.updated_at, NOW()) AS updated_at,
    ssr.id AS school_record_id,
    ssr.transcript_storage_key AS storage_key,
    COALESCE(ssr.transcript_original_filename, 'transcript.bin') AS original_filename,
    COALESCE(ssr.transcript_content_type, 'application/octet-stream') AS mime_type,
    COALESCE(ssr.transcript_size_bytes, 0) AS size_bytes,
    COALESCE(ssr.transcript_uploaded_at, ssr.updated_at, NOW()) AS uploaded_at,
    COALESCE(s.user_id, 0) AS uploaded_by
FROM student_school_record ssr
LEFT JOIN students s ON s.id = ssr.student_id
WHERE ssr.transcript_storage_key IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM student_school_transcript sst
      WHERE sst.school_record_id = ssr.id
        AND sst.storage_key = ssr.transcript_storage_key
  );
