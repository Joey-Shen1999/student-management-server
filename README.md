# Student Management Server

Backend service for student-management platform.

## Tech Stack
- Java 8
- Spring Boot 2.7
- Spring Data JPA
- PostgreSQL (runtime)
- H2 (tests)

## Quick Start
1. Start database:
```bash
docker compose up -d
```
2. Run server:
```bash
./mvnw spring-boot:run
```
3. Run tests:
```bash
./mvnw test
```

Default local API base:
- `http://localhost:8080`

## Auth
Login endpoint:
- `POST /api/auth/login`

Use returned bearer token:
- `Authorization: Bearer <accessToken>`

## Student Profile APIs

### Self profile
- `GET /api/student/profile`
- `PUT /api/student/profile`

### Teacher/Admin managed profile
- `GET /api/teacher/students/{studentId}/profile`
- `PUT /api/teacher/students/{studentId}/profile`
- `POST /api/teacher/students/{studentId}/profile/schools/{schoolRecordId}/transcript` (multipart file upload)
- `GET /api/teacher/students/{studentId}/profile/schools/{schoolRecordId}/transcript` (download transcript file)

Access rule:
- `ADMIN`: can access all students.
- `TEACHER`: can access only students with `teacher_student.status=ACTIVE` assignment.
- `STUDENT`: forbidden (`403`).

## Student Invite APIs

### Create student invite
- `POST /api/teacher/student-invites`

Request body:
```json
{}
```

Optional field:
- `expiresInHours`: `1..720` (default from `app.student-invite.ttl-hours`)

Response fields:
- `inviteToken`
- `inviteUrl`
- `expiresAt`

### Invite preview
- `GET /api/auth/student-invites/{inviteToken}`

Response fields:
- `valid`
- `status`
- `expiresAt`

Note:
- Preview no longer returns teacher ownership fields.

## Student Account Management APIs

- `GET /api/teacher/student-accounts`
- `POST /api/teacher/student-accounts/{studentId}/reset-password`
- `PATCH /api/teacher/student-accounts/{studentId}/status`

Access rule:
- `TEACHER` / `ADMIN`: can manage all student accounts.
- `STUDENT`: forbidden (`403`).

Migration note:
- If your DB still enforces invite ownership (`student_invites.teacher_id NOT NULL`), run:
  - `scripts/migrations/20260318_teacher_student_ownership_deprecation.sql`
- For teacher internal note field, run:
  - `scripts/migrations/20260323_add_student_profile_teacher_note.sql`

## Student Profile Contract (Latest)

### 0) OUAC gender fields
- `gender`: `Male | Female | Other`
- `genderOther`: required when `gender=Other`

Backward compatibility:
- Request still accepts legacy combined value in `gender`, for example `Other: Non-binary`
- Response is normalized to:
  - `gender = "Other"`
  - `genderOther = "Non-binary"`

### 1) High-school history (`schools`)
Represents all high-school history (past + current).

Each item:
- `schoolRecordId`: number (server-generated id)
- `schoolType`: `MAIN | OTHER`
- `schoolName`: string
- `startTime`: `yyyy-MM-dd` (nullable)
- `endTime`: `yyyy-MM-dd` (nullable)
- `hasTranscript`: boolean
- `transcriptFileName`: string (nullable)
- `transcriptSizeBytes`: number (nullable)
- `transcriptUploadedAt`: ISO datetime string (nullable)

Compatibility alias:
- response also includes `schoolRecords`
- request supports `schools` or `schoolRecords`

Transcript APIs for student self-service:
- `POST /api/student/profile/schools/{schoolRecordId}/transcript` with `multipart/form-data`, file field name: `file`
- `GET /api/student/profile/schools/{schoolRecordId}/transcript`

### 2) External credits (`otherCourses`)
Represents only external/summer/night courses.

Each item:
- `schoolName`: string
- `courseCode`: string
- `mark`: `0-100` (nullable)
- `gradeLevel`: `1-12` (nullable)
- `startTime`: `yyyy-MM-dd` (nullable)
- `endTime`: `yyyy-MM-dd` (nullable)

Compatibility alias:
- response also includes `externalCourses`
- request supports `otherCourses` or `externalCourses`

### 3) Error rules
- `401`: unauthenticated
- `403`: forbidden (role mismatch)
- `404`: student not found
- `400`: validation failure

### 4) Teacher internal note (`teacherNote`)
- Teacher endpoints (`/api/teacher/students/{studentId}/profile`) support read/write `teacherNote`.
- Max length: `5000`.
- Empty string is allowed and means clear.
- Student self profile endpoints (`/api/student/profile`) never return `teacherNote` and ignore it in request body.

Error body:
```json
{
  "status": 400,
  "code": "BAD_REQUEST",
  "message": "birthday must be yyyy-mm-dd"
}
```

## Demo Accounts (non-test profile)
Auto-initialized on startup:
- ADMIN: `demo_admin_active_01 / Admin!234`
- TEACHER: `demo_teacher_active_01 / Teacher!234`
- STUDENT: `demo_student_active_01 / Student!234`
