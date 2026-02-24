# Frontend Sync Tasks (Backend Security Upgrade)

## What Changed
- Backend auth for protected APIs is now `Authorization: Bearer <accessToken>`.
- Login now returns session token fields.
- Added logout endpoint.
- `set-password` now requires authenticated session and can only set current user's password.
- `change-password` now works on current authenticated user (no username needed).
- Teacher invite supports optional `displayName`.
- Added teacher role switch endpoint for teacher management UI.

## API Contract Changes

### 1) Login response changed
`POST /api/auth/login`

New response fields:
- `accessToken` (string)
- `tokenType` (string, always `Bearer`)
- `tokenExpiresAt` (ISO datetime string)

Example:
```json
{
  "userId": 12,
  "role": "ADMIN",
  "studentId": null,
  "teacherId": null,
  "mustChangePassword": false,
  "accessToken": "....",
  "tokenType": "Bearer",
  "tokenExpiresAt": "2026-02-24T23:10:00.123"
}
```

### 2) New logout endpoint
`POST /api/auth/logout`

Headers:
- `Authorization: Bearer <accessToken>`

Response for active token:
```json
{
  "success": true,
  "message": "Logged out."
}
```

Authentication semantics:
- Active token: `200 OK`
- Missing token / malformed header / unknown token / revoked token / expired token: `401 UNAUTHORIZED`

### 3) set-password tightened
`POST /api/auth/set-password`

Headers:
- `Authorization: Bearer <accessToken>`

Body:
```json
{
  "newPassword": "NewPass!2",
  "userId": 12
}
```

Notes:
- `userId` is optional for compatibility, but if provided it must equal current user.
- This endpoint is only available when `mustChangePassword=true`.

### 4) change-password request simplified
`POST /api/auth/change-password`

Headers:
- `Authorization: Bearer <accessToken>`

Body:
```json
{
  "oldPassword": "OldPass!1",
  "newPassword": "NewPass!2"
}
```

### 5) Teacher management auth header changed
Protected endpoints now require bearer token:
- `POST /api/teacher/invites`
- `GET /api/teacher/accounts`
- `POST /api/teacher/accounts/{teacherId}/reset-password`
- `PATCH /api/teacher/accounts/{teacherId}/role`

`X-User-Id` is no longer accepted.

`GET /api/teacher/accounts` items now include `role` (`ADMIN` or `TEACHER`) for role-switch UI state rendering.

### 6) Teacher invite request enhancement
`POST /api/teacher/invites`

Body now supports:
```json
{
  "username": "teacher001",
  "displayName": "Mr. Smith"
}
```

If `displayName` is empty, backend falls back to `username`.

### 7) Teacher role switch endpoint
`PATCH /api/teacher/accounts/{teacherId}/role`

Headers:
- `Authorization: Bearer <accessToken>`

Body:
```json
{
  "role": "ADMIN"
}
```
or
```json
{
  "role": "TEACHER"
}
```

Success response:
```json
{
  "teacherId": 1,
  "username": "teacher001",
  "role": "ADMIN",
  "message": "Role updated successfully."
}
```

Error semantics:
- `401 UNAUTHORIZED`: token missing/malformed/unknown/revoked/expired
- `403 FORBIDDEN`: non-admin
- `403 MUST_CHANGE_PASSWORD_REQUIRED`: admin must change password first
- `404 NOT_FOUND`: teacher account not found
- `400 BAD_REQUEST`: role missing or not in `ADMIN|TEACHER`

## Frontend Mandatory Tasks
1. After login, store `accessToken`, `tokenType`, and `tokenExpiresAt`.
2. Add request interceptor to inject `Authorization: Bearer <accessToken>` to protected APIs.
3. Remove all `X-User-Id` usage.
4. Update change-password API payload to only send `oldPassword` and `newPassword`.
5. For set-password flow, ensure user is logged in first and include auth header.
6. Implement logout action that calls `/api/auth/logout` and clears local auth state.
   - If logout itself returns `401`, still clear local auth state and redirect/login as unauthenticated.
7. Handle `401 UNAUTHORIZED` globally by redirecting to login.
8. Handle `403` with code `MUST_CHANGE_PASSWORD_REQUIRED` by forcing password setup page.
9. Wire teacher role switch UI action to `PATCH /api/teacher/accounts/{teacherId}/role` with role value `ADMIN` or `TEACHER`.

## Suggested Joint Validation
1. Login as admin and call teacher management APIs using token (should succeed).
2. Call same APIs without token (should get 401).
3. Invite teacher with `displayName` and verify display name in list.
4. Teacher with `mustChangePassword=true` should be blocked from management APIs (403 with business code).
5. Reset teacher password and confirm old token cannot be used anymore.
6. Logout then call protected API with old token (should get 401).
7. Call logout with an already revoked/invalid token (should get 401).
8. Patch teacher role with admin token and verify role changes to ADMIN/TEACHER.
