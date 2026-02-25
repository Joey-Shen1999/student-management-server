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

## Student Profile Contract (Latest)

### 1) High-school history (`schools`)
Represents all high-school history (past + current).

Each item:
- `schoolType`: `MAIN | OTHER`
- `schoolName`: string
- `startTime`: `yyyy-MM-dd` (nullable)
- `endTime`: `yyyy-MM-dd` (nullable)

Compatibility alias:
- response also includes `schoolRecords`
- request supports `schools` or `schoolRecords`

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
- `403`: forbidden (including teacher not actively assigned)
- `404`: student not found
- `400`: validation failure

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

`demo_teacher_active_01` is ACTIVE-assigned to `demo_student_active_01`.

