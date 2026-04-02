# 需求文档

## 1. 项目概述

**项目名称：** 投标管理系统
**项目类型：** 前后端分离的 Web 应用
**核心功能：** 用户上传招标 PDF 文件，系统通过 AI（MiniMax）自动提取结构化信息，统一管理招标项目资料。

---

## 2. 用户角色

| 角色 | 说明 |
|------|------|
| 普通用户（user） | 可搜索查看项目列表、项目详情、下载文件 |
| 管理员（admin） | 拥有人户管理、Prompt 配置、上传文件、删除文件权限 |

初始管理员账号：dmin@bid.com / dmin123

---

## 3. 功能需求

### 3.1 认证模块

#### 3.1.1 用户注册
- 用户输入用户名（邮箱）、密码即可注册
- 用户名唯一，不可重复
- 密码前端采用 MD5 传输（后端不存储明文）
- 注册成功后跳转登录页

#### 3.1.2 用户登录
- 输入用户名 + 密码，验证通过后获取 JWT Token
- Token 存入 localStorage，后续请求附在 Authorization Header
- 登录成功跳转到首页

#### 3.1.3 路由守卫
- 无 Token 访问需认证页面 → 重定向到 /login
- 无 admin 角色访问 admin 页面 → 重定向到首页

### 3.2 项目列表（首页 /）

- 支持关键字搜索（按项目名称模糊搜索）
- 分页展示，每页条数可选 10 / 20 / 50 / 100，默认 50
- 每行显示：序号、项目名称、项目编号、发标单位、开标时间、拦标价、录入时间
- 点击"查看详情"跳转到项目详情页

### 3.3 上传文件（/admin）

- 仅 admin 可见并可操作
- 支持上传 PDF、Word 文件（.pdf / .doc / .docx），单个文件不超过 100MB
- 上传后实时显示进度条（0~100%），100% 后显示"正在提取信息，请稍候..."
- 提取完成后自动跳转到项目详情页

**上传流程：**
1. 文件保存到本地（UUID + 原扩展名）
2. pdfplumber 提取 PDF 文字（返回 text / textLength / pageCount）
3. 若文字为空 → 报错"此文件为扫描件或无文字层，请上传文字版 PDF"
4. 文字写入本地 .md 文件（原文件名_[MD.md]）
5. 将文字截断（默认 25 万字符），替换 Prompt 模板占位符 {content} 和 {field_def}
6. 调用 MiniMax /v1/text/chatcompletion_v2，温度 0.1
7. 解析返回 JSON（支持多组 key 名映射），写入数据库
8. 返回项目详情

### 3.4 项目详情（/project/:id）

- 显示所有 14 个字段的提取结果
- 文件信息区显示：原始文件名（下载按钮）、PDF 总页数、PDF 总字数
- 提供"下载原文 Markdown"按钮，下载提取后的 markdown 文件
- admin 可删除本项目（同时删除 PDF 和 Markdown 文件）

### 3.5 用户管理（/user）

- 仅 admin 可见
- 用户列表：ID、用户名、角色（下拉切换）、注册时间、操作（重置密码）
- 修改角色或密码实时生效

### 3.6 Prompt 配置（/prompt）

- 仅 admin 可见
- 可编辑：模板名称、system prompt（可选）、content（必填）、field_def JSON（必填）
- {content} 会被替换为 PDF 提取的文字，{field_def} 会被替换为字段定义的人类可读文本
- system 为可选字段，不填则调用接口时不发 system 消息
- 保存后清空后端缓存，下次调用立即生效

---

## 4. AI 提取字段（14 个）

| 字段名 | 说明 | 示例 |
|--------|------|------|
| projectName | 项目名称 | "某市某区某项目" |
| projectCode | 项目编号 | "GC-2024-001" |
| biddingAgency | 招标代理机构 | "某某招标代理有限公司" |
| clientUnit | 发标单位 | "某市财政局" |
| bidOpenTime | 开标时间 | "2024-06-15 09:30" |
| complaintDeadline | 质疑截止时间 | "2024-06-10 17:00" |
| ceilingPrice | 拦标价（最高投标限价） | "¥8,500,000" |
| floorPrice | 下限价（成本警戒线） | "¥7,200,000" |
| contractPayment | 合同付款方式 | "按进度分期付款" |
| expertComposition | 评审专家组成 | "5 人评审专家组" |
| priceScoreMethod | 价格分计算方法 | "低价优先法" |
| subjectiveScoreDetails | 主观分评审细则 | "技术方案 40 分" |
| bidBond | 投标保证金 | "¥170,000" |
| performanceBond | 履约保证金 | "合同价的 10%" |

---

## 5. 数据库设计

### 5.1 users 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| username | VARCHAR(100) | 用户名（邮箱），唯一 |
| password | VARCHAR(64) | MD5 密码 |
| role | VARCHAR(20) | 角色：user / admin |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 5.2 projects 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| project_name | VARCHAR(200) | 项目名称（文件名） |
| project_code | VARCHAR(100) | 项目编号 |
| bidding_agency | VARCHAR(200) | 招标代理机构 |
| client_unit | VARCHAR(200) | 发标单位 |
| bid_open_time | VARCHAR(200) | 开标时间（存原文） |
| complaint_deadline | VARCHAR(200) | 质疑截止时间（存原文） |
| ceiling_price | DECIMAL(15,2) | 拦标价 |
| floor_price | DECIMAL(15,2) | 下限价 |
| contract_payment | VARCHAR(500) | 付款方式 |
| expert_composition | VARCHAR(200) | 专家组成 |
| price_score_method | VARCHAR(200) | 价格分计算方法 |
| subjective_score_details | VARCHAR(500) | 主观分评审细则 |
| bid_bond | DECIMAL(15,2) | 投标保证金 |
| performance_bond | VARCHAR(500) | 履约保证金（存原文） |
| file_path | VARCHAR(500) | PDF 文件路径 |
| markdown_file_path | VARCHAR(500) | Markdown 文件路径 |
| text_length | INT | PDF 总字数 |
| page_count | INT | PDF 总页数 |
| file_original_name | VARCHAR(200) | 原始文件名 |
| uploader_id | BIGINT | 上传用户 ID |
| created_at | DATETIME | 录入时间 |
| updated_at | DATETIME | 更新时间 |

---

## 6. 接口清单

### 6.1 公开接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/user/register | POST | 用户注册 |
| /api/user/login | POST | 用户登录，返回 JWT Token |

### 6.2 需认证接口（所有用户）

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/project/list | GET | 分页搜索项目列表 |
| /api/project/{id} | GET | 项目详情 |
| /api/project/download/{id} | GET | 下载原始文件 |
| /api/project/download/{id}/markdown | GET | 下载提取后的 Markdown |
| /api/user/info | GET | 当前用户信息 |

### 6.3 仅 admin 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/project/upload | POST | 上传文件并自动提取 |
| /api/project/{id} | DELETE | 删除项目（含文件） |
| /api/project/batch-delete | POST | 批量删除 |
| /api/user/list | GET | 用户列表 |
| /api/user/role/{id} | PUT | 更新用户角色 |
| /api/user/password/{id} | PUT | 重置用户密码 |
| /api/prompt | GET | 获取 Prompt 配置 |
| /api/prompt | PUT | 保存 Prompt 配置 |

---

## 7. 前端页面结构

| 路由 | 页面 | 认证要求 |
|------|------|---------|
| /login | 登录页 | 公开 |
| /register | 注册页 | 公开 |
| / | 首页（项目列表） | 需登录 |
| /admin | 上传文件页 | 需 admin |
| /project/:id | 项目详情页 | 需登录 |
| /user | 用户管理页 | 需 admin |
| /prompt | Prompt 配置页 | 需 admin |

admin 用户在首页导航栏显示"用户管理"和"Prompt 配置"入口。
