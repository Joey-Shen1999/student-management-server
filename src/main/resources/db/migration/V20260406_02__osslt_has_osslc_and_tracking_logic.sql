-- OSSLT: add has_osslc and align auto-tracking logic

ALTER TABLE student_osslt_module
    ADD COLUMN IF NOT EXISTS has_osslc BOOLEAN;

UPDATE student_osslt_module
SET osslt_tracking_status = CASE
    WHEN osslt_tracking_manual_status IS NOT NULL THEN osslt_tracking_manual_status
    WHEN latest_osslt_result = 'PASS' THEN 'PASSED'
    WHEN latest_osslt_result = 'FAIL' AND has_osslc = FALSE THEN 'NEEDS_TRACKING'
    ELSE 'WAITING_UPDATE'
END
WHERE osslt_tracking_status IS NULL
   OR osslt_tracking_status NOT IN ('WAITING_UPDATE', 'NEEDS_TRACKING', 'PASSED')
   OR (
       osslt_tracking_manual_status IS NULL
       AND latest_osslt_result = 'FAIL'
       AND has_osslc = FALSE
       AND osslt_tracking_status <> 'NEEDS_TRACKING'
   )
   OR (
       osslt_tracking_manual_status IS NULL
       AND latest_osslt_result = 'PASS'
       AND osslt_tracking_status <> 'PASSED'
   )
   OR (
       osslt_tracking_manual_status IS NULL
       AND (
           latest_osslt_result IS NULL
           OR latest_osslt_result = 'UNKNOWN'
           OR (latest_osslt_result = 'FAIL' AND (has_osslc IS NULL OR has_osslc = TRUE))
       )
       AND osslt_tracking_status <> 'WAITING_UPDATE'
   );
