# Error Code Handling Matrix (OSSLT + Language V1)

Date: 2026-04-06  
Audience: Frontend, QA

## 1) Standard Error Shape

```json
{
  "status": 400,
  "message": "Validation failed.",
  "code": "BAD_REQUEST",
  "details": ["latestOssltDate must be yyyy-mm-dd"]
}
```

## 2) Endpoint-by-Endpoint Matrix

| Endpoint | 400 BAD_REQUEST | 403 FORBIDDEN | 404 NOT_FOUND |
|---|---|---|---|
| `GET /api/teacher/students/{studentId}/osslt-module` | `studentId` 非法 | 角色非 `TEACHER/ADMIN` | 学生不存在 |
| `PUT /api/teacher/students/{studentId}/osslt-module` | 枚举非法、日期格式非法 | 角色非 `TEACHER/ADMIN` | 学生不存在 |
| `GET /api/teacher/students/osslt-summary` | `studentIds` 格式非法 | 角色非 `TEACHER/ADMIN` | 可选：全部无效 ID 时返回 404 或空数组（按后端最终实现） |
| `GET /api/teacher/students/{studentId}/ielts-module` | 参数非法 | 角色非 `TEACHER/ADMIN` | 学生不存在 |
| `PUT /api/teacher/students/{studentId}/ielts-module` | 枚举非法 | 角色非 `TEACHER/ADMIN` | 学生不存在 |

## 3) Error Examples

### 3.1 Invalid Enum (400)
```json
{
  "status": 400,
  "message": "Validation failed.",
  "code": "BAD_REQUEST",
  "details": [
    "ossltTrackingManualStatus must be one of: WAITING_UPDATE, NEEDS_TRACKING, PASSED"
  ]
}
```

### 3.2 Invalid Date (400)
```json
{
  "status": 400,
  "message": "Validation failed.",
  "code": "BAD_REQUEST",
  "details": [
    "latestOssltDate must be yyyy-mm-dd"
  ]
}
```

### 3.3 Forbidden Role (403)
```json
{
  "status": 403,
  "message": "Access denied.",
  "code": "FORBIDDEN",
  "details": ["role STUDENT cannot access this resource"]
}
```

### 3.4 Student Not Found (404)
```json
{
  "status": 404,
  "message": "Student not found.",
  "code": "NOT_FOUND",
  "details": ["studentId=999999"]
}
```

## 4) Frontend Toast Mapping (Recommended)

| `code` | Toast 文案建议 |
|---|---|
| `BAD_REQUEST` | 提交失败，请检查输入格式。 |
| `FORBIDDEN` | 你没有权限执行该操作。 |
| `NOT_FOUND` | 学生不存在或已删除。 |
| other | 请求失败，请稍后重试。 |

## 5) Frontend Parsing Rule

1. 先读 `code` 决定大类文案
2. 若存在 `details[0]`，追加到 toast 次行（或错误面板）
3. 不直接展示后端原始堆栈/内部错误文本
