# Frontend Handoff: OSSLT + Language Score Tracking V1

Date: 2026-04-06  
Audience: Frontend, QA

## 1) Current Backend Scope

Delivered:

1. IELTS naming split with compatibility:
   - Canonical: `languageScoreTrackingStatus`, `languageScoreTrackingManualStatus`
   - Legacy alias still accepted/returned during transition.
2. OSSLT teacher/admin APIs:
   - `GET /api/teacher/students/{studentId}/osslt-module`
   - `PUT /api/teacher/students/{studentId}/osslt-module`
   - `GET /api/teacher/students/osslt-summary?studentIds=...`
3. OSSLT student APIs:
   - `GET /api/student/osslt-module`
   - `PUT /api/student/osslt-module`

Out of scope:

1. OSSLT reminder/notification flow.
2. OSSLT complex statuses (exempt/deferred/etc.).

## 2) Enums (Frontend Must Align)

OSSLT tracking statuses:

1. `WAITING_UPDATE`
2. `NEEDS_TRACKING`
3. `PASSED`

OSSLT latest result:

1. `PASS`
2. `FAIL`
3. `UNKNOWN`

## 3) IELTS Compatibility Rule

Write:

1. Prefer `languageScoreTrackingManualStatus`.
2. Legacy `languageTrackingManualStatus` is still accepted.

Read:

1. Prefer `languageScoreTrackingStatus` / `languageScoreTrackingManualStatus`.
2. Fallback to legacy `languageTrackingStatus` / `languageTrackingManualStatus`.

## 4) OSSLT API Contract

## 4.1 Teacher/Admin: GET single

`GET /api/teacher/students/{studentId}/osslt-module`

Response example:

```json
{
  "studentId": 1001,
  "graduationYear": 2028,
  "latestOssltResult": "UNKNOWN",
  "latestOssltDate": null,
  "hasOsslc": null,
  "ossltTrackingManualStatus": null,
  "ossltTrackingStatus": "WAITING_UPDATE",
  "updatedAt": "2026-04-06T17:20:00Z"
}
```

## 4.2 Teacher/Admin: PUT single (PATCH semantics)

`PUT /api/teacher/students/{studentId}/osslt-module`

Optional updatable fields:

1. `latestOssltResult`: `PASS | FAIL | UNKNOWN`
2. `latestOssltDate`: `YYYY-MM-DD` or `null/""` (clear)
3. `hasOsslc`: `true | false | null`
4. `ossltTrackingManualStatus`: `WAITING_UPDATE | NEEDS_TRACKING | PASSED` or `null/""` (clear)

## 4.3 Teacher/Admin: GET batch summary

`GET /api/teacher/students/osslt-summary?studentIds=1001,1002`

Response array includes:

1. `studentId`
2. `graduationYear`
3. `latestOssltResult`
4. `latestOssltDate`
5. `hasOsslc`
6. `ossltTrackingManualStatus`
7. `ossltTrackingStatus`
8. `updatedAt`

## 4.4 Student: GET self

`GET /api/student/osslt-module`

Notes:

1. Same core state fields as teacher API.
2. `teacherNote` is not returned in OSSLT responses.

## 4.5 Student: PUT self

`PUT /api/student/osslt-module`

Student allowed fields only:

1. `latestOssltResult`: only `PASS | FAIL`
2. `hasOsslc`: only `true | false`

Forbidden for student PUT (returns `400`):

1. `ossltTrackingManualStatus`

Student PUT example:

```json
{
  "latestOssltResult": "FAIL",
  "hasOsslc": false
}
```

Compatibility note:

1. Legacy request field `teacherNote` is ignored if sent by older clients.

## 5) OSSLT Status Logic (Updated)

Final status:

1. If `ossltTrackingManualStatus != null`, manual status wins.
2. Otherwise auto:
   - `PASS` -> `PASSED`
   - `FAIL` + `hasOsslc=false` -> `NEEDS_TRACKING`
   - `FAIL` + `hasOsslc=true` -> `WAITING_UPDATE`
   - `UNKNOWN/null` -> `WAITING_UPDATE`

## 6) Permission Matrix

1. Student OSSLT APIs (`/api/student/osslt-module`): `STUDENT` only.
2. Teacher OSSLT APIs (`/api/teacher/.../osslt-*`): `TEACHER` and `ADMIN`.
3. Unauthorized role: `403`.

## 7) Error Shape (Unified)

All API errors keep:

```json
{
  "status": 400,
  "message": "Validation failed.",
  "code": "BAD_REQUEST",
  "details": []
}
```

Expected status families:

1. `400`: validation/forbidden field in body
2. `403`: role forbidden
3. `404`: student not found

## 8) Frontend Integration Order

1. Add `hasOsslc` in OSSLT model types.
2. Integrate student OSSLT page with `/api/student/osslt-module` GET/PUT.
3. Keep teacher pages on teacher endpoints.
4. Use new IELTS canonical keys first; keep legacy fallback read.

## 9) OpenAPI Source

1. `docs/osslt-language-tracking-openapi.yaml`
