-- Manual one-off backfill for production before DUOLINGO rollout.
-- Safe to run repeatedly.

UPDATE student_ielts_module m
SET language_score_type = 'DUOLINGO'
WHERE (m.language_score_type IS NULL OR m.language_score_type = 'IELTS')
  AND EXISTS (
    SELECT 1
    FROM student_ielts_record r
    WHERE r.ielts_module_id = m.id
      AND (
        r.listening > 9.0
        OR r.reading > 9.0
        OR r.writing > 9.0
        OR r.speaking > 9.0
      )
  );

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'student_ielts_module'
          AND column_name = 'test_type'
    ) THEN
        EXECUTE '
            UPDATE student_ielts_module
            SET language_score_type = ''DUOLINGO''
            WHERE (language_score_type IS NULL OR language_score_type = ''IELTS'')
              AND UPPER(TRIM(COALESCE(test_type::text, ''''))) = ''DUOLINGO''
        ';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'student_ielts_module'
          AND column_name = 'testType'
    ) THEN
        EXECUTE '
            UPDATE student_ielts_module
            SET language_score_type = ''DUOLINGO''
            WHERE (language_score_type IS NULL OR language_score_type = ''IELTS'')
              AND UPPER(TRIM(COALESCE("testType"::text, ''''))) = ''DUOLINGO''
        ';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'student_ielts_record'
          AND column_name = 'test_type'
    ) THEN
        EXECUTE '
            UPDATE student_ielts_module m
            SET language_score_type = ''DUOLINGO''
            WHERE (m.language_score_type IS NULL OR m.language_score_type = ''IELTS'')
              AND EXISTS (
                SELECT 1
                FROM student_ielts_record r
                WHERE r.ielts_module_id = m.id
                  AND UPPER(TRIM(COALESCE(r.test_type::text, ''''))) = ''DUOLINGO''
              )
        ';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'student_ielts_record'
          AND column_name = 'testType'
    ) THEN
        EXECUTE '
            UPDATE student_ielts_module m
            SET language_score_type = ''DUOLINGO''
            WHERE (m.language_score_type IS NULL OR m.language_score_type = ''IELTS'')
              AND EXISTS (
                SELECT 1
                FROM student_ielts_record r
                WHERE r.ielts_module_id = m.id
                  AND UPPER(TRIM(COALESCE(r."testType"::text, ''''))) = ''DUOLINGO''
              )
        ';
    END IF;
END $$;
