-- Backfill legacy VOLUNTEER content text into info_volunteer_task_items.
-- Usage:
--   psql "$DATABASE_URL" -f scripts/migrations/20260408_backfill_info_volunteer_task_items.sql
--
-- Rules:
--   1. Skip rows that already have structured volunteer items.
--   2. Parse each task block from legacy text format.
--   3. Log parse failures with WARNING and continue processing.

DO $$
DECLARE
    info_row RECORD;
    task_match TEXT[];
    parsed_count INTEGER;
    duration_hours NUMERIC(12, 2);
    start_date DATE;
    end_date DATE;
BEGIN
    IF to_regclass('public.info_tasks') IS NULL THEN
        RAISE WARNING 'info_tasks does not exist; backfill skipped.';
        RETURN;
    END IF;
    IF to_regclass('public.info_volunteer_task_items') IS NULL THEN
        RAISE WARNING 'info_volunteer_task_items does not exist; run schema migration first.';
        RETURN;
    END IF;

    FOR info_row IN
        SELECT i.id, i.content
        FROM info_tasks i
        WHERE i.category = 'VOLUNTEER'
          AND i.content IS NOT NULL
          AND btrim(i.content) <> ''
          AND NOT EXISTS (
              SELECT 1
              FROM info_volunteer_task_items v
              WHERE v.info_id = i.id
          )
        ORDER BY i.id ASC
    LOOP
        BEGIN
            parsed_count := 0;

            FOR task_match IN
                SELECT regexp_matches(
                    info_row.content,
                    '任务名称：\s*([^\r\n]+)\s*[\r\n]+' ||
                    '任务描述：\s*([^\r\n]+)\s*[\r\n]+' ||
                    '任务时长：\s*([0-9]+(?:\.[0-9]+)?)\s*小时\s*[\r\n]+' ||
                    '开始日期：\s*(\d{4}-\d{2}-\d{2})\s*[\r\n]+' ||
                    '结束日期：\s*(\d{4}-\d{2}-\d{2})\s*[\r\n]+' ||
                    '证明人联系方式：\s*([^\r\n]+)',
                    'g'
                )
            LOOP
                duration_hours := task_match[3]::NUMERIC(12, 2);
                IF duration_hours <= 0 THEN
                    RAISE WARNING '[info_id=%] duration_hours must be > 0, value=%', info_row.id, task_match[3];
                    CONTINUE;
                END IF;

                start_date := task_match[4]::DATE;
                end_date := task_match[5]::DATE;
                IF end_date < start_date THEN
                    RAISE WARNING '[info_id=%] end_date before start_date, start=% end=%',
                        info_row.id, task_match[4], task_match[5];
                    CONTINUE;
                END IF;

                INSERT INTO info_volunteer_task_items (
                    info_id,
                    task_name,
                    description,
                    duration_hours,
                    start_date,
                    end_date,
                    verifier_contact,
                    created_at,
                    updated_at
                ) VALUES (
                    info_row.id,
                    btrim(task_match[1]),
                    btrim(task_match[2]),
                    duration_hours,
                    start_date,
                    end_date,
                    btrim(task_match[6]),
                    NOW(),
                    NOW()
                );
                parsed_count := parsed_count + 1;
            END LOOP;

            IF parsed_count = 0 THEN
                RAISE WARNING '[info_id=%] no valid volunteer tasks parsed from content.', info_row.id;
            END IF;
        EXCEPTION
            WHEN OTHERS THEN
                RAISE WARNING '[info_id=%] unexpected backfill error: %', info_row.id, SQLERRM;
                CONTINUE;
        END;
    END LOOP;
END $$;
