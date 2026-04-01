-- Read-only DB preflight for release with:
-- - V20260330_01__task_group_refactor.sql
-- - V20260330_02__ielts_tracking_module.sql
--
-- This script is non-destructive. It validates migration integrity and key data/schema invariants.

DO $$
DECLARE
    issues INTEGER := 0;
    has_flyway BOOLEAN;
    has_v20260330_01 BOOLEAN := FALSE;
    has_v20260330_02 BOOLEAN := FALSE;
    has_goal_task_group_col BOOLEAN := FALSE;
    has_info_task_group_col BOOLEAN := FALSE;
    failed_migrations INTEGER := 0;
    checksum_v20260330_01 INTEGER;
    goal_missing_task_group INTEGER := 0;
    dup_goal_pairs INTEGER := 0;
    dup_info_groups INTEGER := 0;
    orphan_info_recipients INTEGER := 0;
    found_ielts_constraints INTEGER := 0;
    expected_checksum_v20260330_01 CONSTANT INTEGER := 1323014771;
BEGIN
    SELECT to_regclass('public.flyway_schema_history') IS NOT NULL INTO has_flyway;
    IF NOT has_flyway THEN
        RAISE NOTICE '[SKIP] flyway_schema_history not found (fresh DB).';
        RETURN;
    END IF;

    SELECT COUNT(*) INTO failed_migrations
    FROM flyway_schema_history
    WHERE success = FALSE;
    IF failed_migrations > 0 THEN
        issues := issues + 1;
        RAISE NOTICE '[FAIL] failed Flyway migrations: %', failed_migrations;
    ELSE
        RAISE NOTICE '[PASS] no failed Flyway migrations';
    END IF;

    SELECT EXISTS (
        SELECT 1
        FROM flyway_schema_history
        WHERE version = '20260330.01' AND success = TRUE
    ) INTO has_v20260330_01;

    SELECT EXISTS (
        SELECT 1
        FROM flyway_schema_history
        WHERE version = '20260330.02' AND success = TRUE
    ) INTO has_v20260330_02;

    IF has_v20260330_01 THEN
        SELECT checksum INTO checksum_v20260330_01
        FROM flyway_schema_history
        WHERE version = '20260330.01' AND success = TRUE
        ORDER BY installed_rank DESC
        LIMIT 1;

        IF checksum_v20260330_01 IS DISTINCT FROM expected_checksum_v20260330_01 THEN
            issues := issues + 1;
            RAISE NOTICE '[FAIL] checksum mismatch for 20260330.01 (db=% expected=%)',
                checksum_v20260330_01, expected_checksum_v20260330_01;
        ELSE
            RAISE NOTICE '[PASS] checksum for 20260330.01 matches current release';
        END IF;
    ELSE
        RAISE NOTICE '[INFO] 20260330.01 not applied yet (acceptable for first rollout)';
    END IF;

    SELECT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'goal_tasks'
          AND column_name = 'task_group_id'
    ) INTO has_goal_task_group_col;

    SELECT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'info_tasks'
          AND column_name = 'task_group_id'
    ) INTO has_info_task_group_col;

    IF has_goal_task_group_col THEN
        SELECT COUNT(*) INTO goal_missing_task_group
        FROM goal_tasks
        WHERE task_group_id IS NULL OR btrim(task_group_id) = '';

        IF goal_missing_task_group > 0 THEN
            issues := issues + 1;
            RAISE NOTICE '[FAIL] goal_tasks rows with empty task_group_id: %', goal_missing_task_group;
        ELSE
            RAISE NOTICE '[PASS] goal_tasks.task_group_id is populated';
        END IF;

        SELECT COUNT(*) INTO dup_goal_pairs
        FROM (
            SELECT task_group_id, assigned_student_id
            FROM goal_tasks
            GROUP BY task_group_id, assigned_student_id
            HAVING COUNT(*) > 1
        ) t;

        IF dup_goal_pairs > 0 THEN
            issues := issues + 1;
            RAISE NOTICE '[FAIL] duplicate goal task pairs (task_group_id, assigned_student_id): %', dup_goal_pairs;
        ELSE
            RAISE NOTICE '[PASS] no duplicate goal task pairs';
        END IF;
    ELSIF has_v20260330_01 THEN
        issues := issues + 1;
        RAISE NOTICE '[FAIL] goal_tasks.task_group_id column missing after 20260330.01';
    ELSE
        RAISE NOTICE '[INFO] goal_tasks.task_group_id not present yet (pre-migration state)';
    END IF;

    IF has_info_task_group_col THEN
        SELECT COUNT(*) INTO dup_info_groups
        FROM (
            SELECT published_by_teacher_id, task_group_id
            FROM info_tasks
            WHERE task_group_id IS NOT NULL
            GROUP BY published_by_teacher_id, task_group_id
            HAVING COUNT(*) > 1
        ) t;

        IF dup_info_groups > 0 THEN
            issues := issues + 1;
            RAISE NOTICE '[FAIL] duplicate info task groups (teacher_id, task_group_id): %', dup_info_groups;
        ELSE
            RAISE NOTICE '[PASS] no duplicate info task groups';
        END IF;
    ELSIF has_v20260330_01 THEN
        issues := issues + 1;
        RAISE NOTICE '[FAIL] info_tasks.task_group_id column missing after 20260330.01';
    ELSE
        RAISE NOTICE '[INFO] info_tasks.task_group_id not present yet (pre-migration state)';
    END IF;

    IF to_regclass('public.info_task_recipients') IS NOT NULL
       AND to_regclass('public.info_tasks') IS NOT NULL THEN
        SELECT COUNT(*) INTO orphan_info_recipients
        FROM info_task_recipients r
        LEFT JOIN info_tasks t ON t.id = r.info_task_id
        WHERE t.id IS NULL;

        IF orphan_info_recipients > 0 THEN
            issues := issues + 1;
            RAISE NOTICE '[FAIL] orphan info_task_recipients rows: %', orphan_info_recipients;
        ELSE
            RAISE NOTICE '[PASS] no orphan info_task_recipients rows';
        END IF;
    END IF;

    IF has_v20260330_01 THEN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'uq_goal_tasks_group_student'
        ) THEN
            issues := issues + 1;
            RAISE NOTICE '[FAIL] missing constraint uq_goal_tasks_group_student';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_indexes
            WHERE schemaname = 'public'
              AND indexname = 'idx_goal_tasks_task_group_id'
        ) THEN
            issues := issues + 1;
            RAISE NOTICE '[FAIL] missing index idx_goal_tasks_task_group_id';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_indexes
            WHERE schemaname = 'public'
              AND indexname = 'idx_info_tasks_publisher_task_group'
        ) THEN
            issues := issues + 1;
            RAISE NOTICE '[FAIL] missing index idx_info_tasks_publisher_task_group';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_indexes
            WHERE schemaname = 'public'
              AND indexname = 'uq_info_tasks_teacher_group'
        ) THEN
            issues := issues + 1;
            RAISE NOTICE '[FAIL] missing index uq_info_tasks_teacher_group';
        END IF;
    END IF;

    IF has_v20260330_02 THEN
        IF to_regclass('public.student_ielts_module') IS NULL
           OR to_regclass('public.student_ielts_record') IS NULL THEN
            issues := issues + 1;
            RAISE NOTICE '[FAIL] IELTS tables missing after 20260330.02';
        END IF;

        SELECT COUNT(*) INTO found_ielts_constraints
        FROM pg_constraint
        WHERE conname IN (
            'uk_student_ielts_module_student_id',
            'fk_student_ielts_module_student',
            'uk_student_ielts_record_module_record_id',
            'fk_student_ielts_record_module'
        );

        IF found_ielts_constraints <> 4 THEN
            issues := issues + 1;
            RAISE NOTICE '[FAIL] IELTS constraints found=% expected=4', found_ielts_constraints;
        ELSE
            RAISE NOTICE '[PASS] IELTS constraints are complete';
        END IF;
    END IF;

    IF issues > 0 THEN
        RAISE EXCEPTION 'DB preflight failed with % issue(s). Stop deployment and fix before release.', issues;
    END IF;

    RAISE NOTICE '[PASS] DB preflight completed successfully.';
END $$;
