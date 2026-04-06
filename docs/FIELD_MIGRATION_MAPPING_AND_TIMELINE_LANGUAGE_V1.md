# Field Migration Mapping + Timeline (Language Tracking V1)

Date: 2026-04-06  
Audience: Frontend, Backend, QA

## 1) Canonical vs Legacy Fields

| Domain | Canonical (new) | Legacy (old) | Notes |
|---|---|---|---|
| IELTS manual tracking | `languageScoreTrackingManualStatus` | `languageTrackingManualStatus` | 写入统一用 canonical |
| IELTS derived tracking | `languageScoreTrackingStatus` | `languageTrackingStatus` | 读取新优先旧兜底 |

## 2) Frontend Read/Write Rules (Hard Rule)

1. 写入（PUT/PATCH）只发送 canonical 字段：`languageScoreTracking*`
2. 读取时按优先级：
   - first: `languageScoreTracking*`
   - fallback: `languageTracking*`
3. 在迁移窗口内，后端响应应同时返回新旧字段，且值保持一致。

## 3) Backend Compatibility Rule

1. 请求兼容：
   - 接收 `languageScoreTrackingManualStatus`（主）
   - 接收 `languageTrackingManualStatus`（兼容）
2. 若两者同时存在且值冲突：
   - 以 `languageScoreTrackingManualStatus` 为准
   - 记录 warning 日志（便于排查旧前端）

## 4) Deprecation Timeline (Fixed Date)

- **Now ~ 2026-05-31 (inclusive):** 双读双写兼容窗口
- **Planned legacy sunset date: 2026-05-31**
- **From 2026-06-01:** 默认停止前端写入 legacy 字段（后端仍可短期容忍）
- **After 2026-06-15:** 评估移除后端对 legacy 写入的兼容（按监控结果执行）

## 5) Release Gate Before Legacy Removal

1. 最近 7 天内 legacy 字段请求占比 < 1%
2. 主要前端页面（teacher/student IELTS）均已切换 canonical
3. 回归测试通过：
   - canonical 写入成功
   - legacy 兜底读取不影响展示
   - 混合 payload 冲突策略符合约定

## 6) Frontend Quick Checklist

- [ ] DTO 类型已使用 `languageScoreTracking*`
- [ ] API submit payload 不再主动发送 `languageTracking*`
- [ ] selector/getter 实现“新优先旧兜底”
- [ ] toast 文案按 `code + details` 结构解析
