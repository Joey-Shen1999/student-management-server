# Student Profile Multi-Transcript Upgrade

## 1. Schema / Migration

### New table
- `student_school_transcript`
  - `id` (PK)
  - `school_record_id` (FK -> `student_school_record.id`)
  - `storage_key`
  - `original_filename`
  - `mime_type`
  - `size_bytes`
  - `uploaded_at`
  - `uploaded_by`
  - plus `created_at`, `updated_at` (from `BaseEntity`)

### Indexes
- `idx_school_transcript_school_record_id` on `school_record_id`
- `idx_school_transcript_uploaded_at` on `uploaded_at`

### FK / Cascade strategy
- DB constraint: `FOREIGN KEY (school_record_id) REFERENCES student_school_record(id) ON DELETE CASCADE`
- Meaning:
  - deleting a school record deletes child transcript rows automatically.
  - object-storage files are still deleted explicitly by service logic before sync-remove paths.

### SQL migration script
- `scripts/migrations/20260304_add_student_school_transcript.sql`
  - create table + indexes + FK
  - backfill legacy single transcript columns into child table

## 2. API examples

### 2.1 Append upload (student self)
`POST /api/student/profile/schools/{schoolRecordId}/transcript` (`multipart/form-data`, field=`file` or `transcript`)

Response:
```json
{
  "schoolRecordId": 80,
  "transcriptFileName": "latest.pdf",
  "transcriptContentType": "application/pdf",
  "transcriptSizeBytes": 12345,
  "transcriptUploadedAt": "2026-03-04T12:00:00",
  "hasTranscript": true,
  "transcripts": [
    {
      "id": 12,
      "transcriptFileName": "latest.pdf",
      "transcriptContentType": "application/pdf",
      "transcriptSizeBytes": 12345,
      "transcriptUploadedAt": "2026-03-04T12:00:00",
      "uploadedBy": 11
    },
    {
      "id": 11,
      "transcriptFileName": "old.pdf",
      "transcriptContentType": "application/pdf",
      "transcriptSizeBytes": 9876,
      "transcriptUploadedAt": "2026-03-03T10:00:00",
      "uploadedBy": 11
    }
  ]
}
```

Teacher upload endpoint has same response shape:
- `POST /api/teacher/students/{studentId}/profile/schools/{schoolRecordId}/transcript`

### 2.2 Profile GET now returns transcripts[]

`GET /api/student/profile` and `GET /api/teacher/students/{studentId}/profile`

`schools[]` item example:
```json
{
  "schoolRecordId": 80,
  "schoolType": "MAIN",
  "schoolName": "Unionville High School",
  "startTime": "2023-09-01",
  "endTime": null,
  "transcriptFileName": "latest.pdf",
  "transcriptSizeBytes": 12345,
  "transcriptUploadedAt": "2026-03-04T12:00:00",
  "hasTranscript": true,
  "transcripts": [
    {
      "id": 12,
      "transcriptFileName": "latest.pdf",
      "transcriptContentType": "application/pdf",
      "transcriptSizeBytes": 12345,
      "transcriptUploadedAt": "2026-03-04T12:00:00",
      "uploadedBy": 11
    },
    {
      "id": 11,
      "transcriptFileName": "old.pdf",
      "transcriptContentType": "application/pdf",
      "transcriptSizeBytes": 9876,
      "transcriptUploadedAt": "2026-03-03T10:00:00",
      "uploadedBy": 11
    }
  ]
}
```

Compatibility fields are preserved and mapped to latest transcript:
- `transcriptFileName`
- `transcriptSizeBytes`
- `transcriptUploadedAt`
- `hasTranscript`

### 2.3 PUT schools[].transcripts final-state sync

`PUT /api/student/profile` (teacher endpoint same behavior)

- `schools[].transcripts` omitted or `null`: keep existing transcripts.
- `schools[].transcripts = []`: remove all transcripts under that school.
- `schools[].transcripts` provided with subset IDs: keep listed, delete missing.

Example request fragment:
```json
{
  "schools": [
    {
      "schoolRecordId": 80,
      "schoolType": "MAIN",
      "schoolName": "Unionville High School",
      "startTime": "2023-09-01",
      "endTime": null,
      "transcripts": [
        {
          "id": 12,
          "transcriptFileName": "latest.pdf",
          "transcriptSizeBytes": 12345,
          "transcriptUploadedAt": "2026-03-04T12:00:00"
        }
      ]
    }
  ]
}
```

### 2.4 Precise download endpoint (P1)
- Student:
  - `GET /api/student/profile/schools/{schoolRecordId}/transcripts/{transcriptId}`
- Teacher:
  - `GET /api/teacher/students/{studentId}/profile/schools/{schoolRecordId}/transcripts/{transcriptId}`

Legacy endpoint kept:
- `/.../schools/{schoolRecordId}/transcript`
- behavior: download latest transcript.

## 3. Error code conventions

- `401 UNAUTHENTICATED`
  - missing/invalid/expired session token.
- `403 FORBIDDEN`
  - role or ownership check failed.
- `404 NOT_FOUND`
  - school/transcript does not exist.
- `422 UNPROCESSABLE_ENTITY`
  - transcript storage delete failure during PUT final-state sync.
- `500 INTERNAL_SERVER_ERROR`
  - unexpected server fault.

## 4. Logging contract

New transcript logic logs include:
- `traceId`
- `userId`
- `schoolRecordId`
- `transcriptId`

Covered paths:
- upload append success
- PUT final-state upsert
- storage deletion success/failure
