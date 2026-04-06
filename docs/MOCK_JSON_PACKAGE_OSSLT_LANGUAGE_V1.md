# Mock JSON Package (OSSLT + Language V1)

Date: 2026-04-06  
Path: `docs/mock-json/osslt-language-v1`

## Included Files

1. `osslt-module.get.success.json`  
   用途：详情页初次加载成功
2. `osslt-module.put.success.json`  
   用途：保存成功后回显
3. `osslt-module.put.error-invalid-enum.json`  
   用途：非法枚举报错
4. `osslt-module.put.error-invalid-date.json`  
   用途：非法日期报错
5. `osslt-summary.get.success.json`  
   用途：列表页批量状态展示
6. `ielts-module.get.success.json`  
   用途：新旧字段并存读取验证
7. `ielts-module.put.success.json`  
   用途：canonical 写入成功验证

## Frontend Usage Notes

1. 列表页先用 `osslt-summary.get.success.json` 验证三态渲染。
2. 详情页用 `osslt-module.get.success.json` + `osslt-module.put.success.json` 验证保存回显。
3. 错误处理用两份 `400` 样例验证 `code/details` toast 映射。
4. IELTS 页面读取优先 `languageScoreTracking*`，再兜底 legacy 字段。
