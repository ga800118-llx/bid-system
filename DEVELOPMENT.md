# DEVELOPMENT.md - 招投标系统开发规范

_本文档记录系统开发中确认的重要决策点，作为后续开发的依据。_

---

## 1. MiniMax 文件上传流程（确认于 2026-03-30）

### 确认流程（目标实现，不改）

`
① 前端 POST multipart file
         ↓
② 后端保存文件（UUID 命名，不保留原文件名）
         ↓
③ 直接将原文件发送给 MiniMax 文档理解接口
   - 不做本地 PDF 文本提取
   - 不做 8000 字符截断
   - 完整文件一次发送
         ↓
④ MiniMax 返回 JSON（15 个字段）
         ↓
⑤ 解析 JSON，结果写入数据库
         ↓
⑥ scoringMethod / productList 额外生成 Excel（Apache POI）
`

### 文件限制
- 最大支持 **100M**（前端 + 后端均需校验）

### 计费规则
- **一次 = 一个完整文件 = 一次 API 调用**
- 不切片、不分段、不重试

### API 信息
- 地址：https://api.minimaxi.com/v1/chat/completions（文档理解接口）
- 模型：MiniMax-M2.7
- 鉴权：Bearer Token（见 MiniMaxExtractService.java）

### 返回字段（15 个）
| 字段 | 说明 |
|------|------|
| projectName | 项目名称 |
| projectCode | 项目编号 |
| biddingAgency | 招标代理机构 |
| clientUnit | 发标单位 |
| bidOpenTime | 开标时间（YYYY-MM-DD） |
| complaintDeadline | 质疑截止时间（YYYY-MM-DD） |
| ceilingPrice | 拦标价（元） |
| floorPrice | 下限价（元） |
| contractPayment | 合同付款方式 |
| expertComposition | 专家组成 |
| priceScoreMethod | 价格分计算方法 |
| subjectiveScoreDetails | 主观分评审细则 |
| bidBond | 投标保证金（元） |
| performanceBond | 履约保证金（比例或金额） |

### Excel 生成
- scoringMethod 和 productList 由 LLM 返回文本后，在服务端通过 Apache POI 生成 .xlsx
- 文件路径保存到数据库对应字段

---

## 2. 禁止事项（开发中禁止改动）

- 不做本地文本提取（不用 PDFBox、DOCX 解析等方式提取文字发给 MiniMax）
- 不做 8000 字符截断
- 计费逻辑：**一次文件 = 一次 API**，不改
- 文件命名：**UUID 命名**，不保留原文件名
- EXTRACT_PROMPT：禁止 git checkout 覆盖（已修复字段丢失问题，参考 2026-03-30 日志）

---

## 3. 开发调试规范

- 调试前先 git stash 暂存当前版本，调试完再决定是否合回
- 重要改动（Prompt、字段列表）必须同步更新本文档
- System.err.println 等调试代码使用后立即删除，禁止提交到主分支

---

## 4. 版本历史

| 日期 | 变更内容 |
|------|----------|
| 2026-03-30 | 确认流程：上传文件 → 直发 MiniMax → 返回 JSON；文件大小 100M；一次文件=一次 API；15 个提取字段 |

---
