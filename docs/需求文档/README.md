# 需求文档

## 1. 项目概述

### 1.1 项目背景
招投标是政府采购和工程建设领域最核心的环节之一。传统方式下，招标文件的审阅和关键信息提取完全依赖人工完成，效率低下且容易出错。本系统旨在通过数字化手段，提升招标文件处理效率，降低信息遗漏风险。

### 1.2 项目目标
**核心目标：** 实现招标需求文档的在线管理 + AI 自动提取关键结构化信息。

- 招标文件上传后，系统自动解析 PDF 中的关键字段
- 项目信息结构化存储，支持搜索、查看、下载
- 多人协作查看，支持角色权限区分（普通用户 / 管理员）
- 管理员可批量删除项目记录
- 支持原文 Markdown 下载，保留 PDF 提取后的原始文本
- Prompt 模板可管理，管理员可编辑字段定义和 AI 指令

### 1.3 术语说明

| 术语 | 解释 |
|------|------|
| 拦标价 | 招标人对投标价格的最高限价，超出则废标 |
| 下限价 | 投标成本警戒线，低于此价可能存在恶意低价竞争 |
| 投标保证金 | 投标人缴纳的保证履约的押金，一般为固定金额或比例 |
| 履约保证金 | 中标后缴纳的保证合同执行的押金 |
| 价格分 | 评标中价格评分的计算方法（如低价优先法，平均法等） |
| 主观分 | 评标中由评委主观打分的部分（如技术方案，施工组织设计） |

---

## 2. 功能模块

### 2.1 用户管理

| ID | 功能点 | 说明 |
|----|------|------|
| F1.1 | 注册 | 用户名 + 密码注册，后台存储 |
| F1.2 | 登录 | 用户名 + 密码，JWT token 返回 |
| F1.3 | 用户列表 | 管理员可查看所有用户列表 |
| F1.4 | 重置密码 | 管理员可为用户重置密码 |

### 2.2 项目管理

| ID | 功能点 | 说明 |
|----|------|------|
| F2.1 | 上传 PDF | 接受 .pdf 文件，自动调用 MiniMax AI 提取 14 个字段 |
| F2.2 | 项目列表 | 分页查看，支持按项目名称搜索 |
| F2.3 | 项目详情 | 查看具体字段内容 |
| F2.4 | 下载原始 PDF | 下载上传的原始文件 |
| F2.5 | 下载原文 Markdown | 下载 PDF 提取后的原始文本，Markdown 格式 |
| F2.6 | 批量删除 | 管理员可多选项目后批量删除，同时删除 PDF 和 Markdown 文件 |

### 2.3 Prompt 模板配置

| ID | 功能点 | 说明 |
|----|------|------|
| F3.1 | 查看 Prompt 配置 | 管理员可查看当前的 system、content、fieldDef 内容 |
| F3.2 | 修改 Prompt 配置 | 管理员可编辑并保存，生效后新的 AI 调用将使用新配置 |

---

## 3. 数据字段详细说明

| 字段名称 | 字段键 | 类型 | 说明 |
|------|------|------|------|
| 项目名称 | projectName | VARCHAR(200) | PDF 文件名，即为项目名称 |
| 项目编号 | projectCode | VARCHAR(100) | 招标人自行编制或监管部门备案的项目唯一标识码 |
| 招标代理机构 | biddingAgency | VARCHAR(200) | 受招标人委托负责本次招标事宜的中介机构 |
| 招标人/发标单位 | clientUnit | VARCHAR(200) | 招标项目的发包方和招标主体 |
| 开标时间 | bidOpenTime | VARCHAR(200) | 投标人必须在此时时间前递交投标文件，存储原文，无需格式化 |
| 质疑截止时间 | complaintDeadline | VARCHAR(200) | 投标人对招标文件内容有异议可以提出质疑的最后期限，存储原文 |
| 拦标价/最高投标限价 | ceilingPrice | DECIMAL(15,2) | 招标人设定的投标最高上限价格 |
| 下限价/成本警戒线 | floorPrice | DECIMAL(15,2) | 招标人设定的投标最低价格下限 |
| 付款方式 | contractPayment | VARCHAR(500) | 合同价款的支付方式和节点安排 |
| 评标委员会/专家组成 | expertComposition | VARCHAR(200) | 评标委员会的人员构成和专家来源 |
| 价格分计算方法 | priceScoreMethod | VARCHAR(200) | 评标办法中价格分（定量分）的计算公式或方法 |
| 主观分/技术分详情 | subjectiveScoreDetails | VARCHAR(500) | 评标中由评委主观打分的部分，如技术标评分，施工组织设计评分 |
| 投标保证金 | bidBond | VARCHAR(200) | 投标人向招标人提交的保证其参与投标的担保金，存储原文描述（如10.00元或合同金额的2%） |
| 履约保证金 | performanceBond | VARCHAR(500) | 中标人向招标人提交的保证其履行合同的担保金，存储原文描述 |
| PDF文件路径 | filePath | VARCHAR(500) | 服务器上存储 PDF 文件的路径 |
| Markdown文件路径 | markdownFilePath | VARCHAR(500) | PDF 提取后生成的 Markdown 文件路径 |
| PDF总字数 | textLength | INT | PDF 提取后的文本字符总字数 |
| PDF总页数 | pageCount | INT | PDF 文件的总页数 |
| 原始文件名 | fileOriginalName | VARCHAR(200) | 上传时的原始文件名 |
| 录入时间 | createdAt | DATETIME | 记录录入时间 |

---

## 4. 系统接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/project/upload | POST | 上传 PDF，AI 提取字段，返回项目 ID |
| /api/project/list | GET | 分页查询项目列表 |
| /api/project/{id} | GET | 项目详情 |
| /api/project/download/{id} | GET | 下载原始 PDF |
| /api/project/download/{id}/markdown | GET | 下载原文 Markdown |
| /api/project/{id} | DELETE | 单条删除 |
| /api/project/batch-delete | POST | 批量删除，Body: {ids: []} |
| /api/user/login | POST | 登录 |
| /api/user/register | POST | 注册 |
| /api/user/list | GET | 用户列表（管理员） |
| /api/user/role/{id} | PUT | 更新用户角色 |
| /api/user/password/{id} | PUT | 重置密码 |
| /api/prompt | GET | 获取 Prompt 配置（管理员） |
| /api/prompt | PUT | 保存 Prompt 配置（管理员） |

---

## 5. 用户角色

| 角色 | 权限 |
|------|------|
| 普通用户 | 上传 PDF、查看项目列表和详情、下载 PDF/Markdown |
| 管理员 (admin) | 包含普通用户权限 + 用户管理、Prompt 配置、批量删除 |

---

## 6. 技术实现细节

### 6.1 PDF 文字提取
使用 pdfplumber 库提取 PDF 文字内容。返回结果：text（全文）、textLength（字数）、pageCount（页数）。扫描件（无文字层）会返回错误提示。

### 6.2 MiniMax AI 提取
- 调用 MiniMax /v1/text/chatcompletion_v2 接口
- 输入：PDF 提取全文 + 字段定义（来自 Prompt 模板）
- 输出：JSON，解析 14 个字段
- parseResponse 方法处理 Markdown 代码块包裹的 JSON 响应

### 6.3 文件存储
- 原始 PDF：UUID + 原文件名，存储于配置路径
- Markdown：原文件名 + _[MD.md]，同目录
- 删除项目时同时删除 PDF 和 Markdown 文件

### 6.4 Prompt 模板
- 存储位置：C:/UPQQ/prompt_template.json
- 包含：name（模板名）、system（系统指令）、content（正文，含 {content} 和 {field_def} 占位符）、field_def（字段定义数组）
- system 为选填，为空时不发送 system 消息
- content 和 fieldDef 为必填

**更新记录：**
- 2026-04-02：新增批量删除功能（前后端）；Prompt 配置页支持 system 字段；开标时间、质疑截止时间、履约保证金改为存原文
- 2026-04-01：新增原文 Markdown 下载 + 统计信息（字数/页数）；Prompt 配置功能；首页改造和用户管理入口
- 2026-03-31：重构 PDF 提取流程为 pdfminer 文字提取；Prompt 模板管理迁移到 JSON 配置文件；axios 超时调整为 600s
