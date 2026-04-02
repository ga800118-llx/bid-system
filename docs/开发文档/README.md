# 开发文档

## 1. 技术栈

| 分类 | 技术 | 说明 |
|------|------|------|
| 后端 | Java Spring Boot 3.2 | REST API 框架 |
| ORM | MyBatis Plus | 数据库访问 |
| 数据库 | MySQL | douge_bid 库 |
| 前端 | Vue3 + Element Plus | 组件化 UI |
| 构建工具 | Maven (后端) / Vite (前端) | 包管理和构建 |
| JDK | OpenJDK 21 | Java 运行时 |
| PDF 提取 | pdfminer.six | Python 库，提取 PDF 文字层 |
| AI 接口 | MiniMax Chat API | 文字理解字段提取 |

## 2. 项目结构

```
bid-system/
├── backend/
│   ├── src/main/java/com/bid/system/
│   │   ├── BidSystemApplication.java        # 启动入口
│   │   ├── config/
│   │   │   └── RestConfig.java              # RestTemplate 超时配置（30s连接+120s读取）
│   │   ├── controller/
│   │   │   ├── ProjectController.java        # 项目相关接口
│   │   │   ├── UserController.java           # 用户相关接口
│   │   │   └── PromptTemplateController.java # Prompt 配置接口
│   │   ├── dto/
│   │   │   └── ApiResponse.java              # 统一响应封装
│   │   ├── entity/
│   │   │   ├── Project.java                  # 项目实体
│   │   │   ├── User.java                     # 用户实体
│   │   │   ├── ExtractLog.java               # 提取日志实体
│   │   │   └── PromptTemplate.java           # Prompt 模板实体
│   │   ├── mapper/
│   │   │   └── ProjectMapper.java             # 项目 MyBatis Mapper
│   │   ├── service/
│   │   │   ├── ProjectService.java           # 项目服务（含批量删除）
│   │   │   ├── UserService.java
│   │   │   ├── MiniMaxTextExtractService.java # PDF 提取 + AI 字段提取
│   │   │   └── PromptTemplateService.java    # Prompt 模板读写
│   │   └── util/
│   │       └── JwtUtil.java                 # JWT 工具
│   └── src/main/resources/
│       └── application.yml                   # 配置文件
├── frontend/
│   ├── src/
│   │   ├── api/
│   │   │   └── index.js                     # axios 接口封装
│   │   ├── views/
│   │   │   ├── Login.vue                    # 登录页
│   │   │   ├── Admin.vue                    # 首页（含批量删除 UI）
│   │   │   ├── ProjectDetail.vue            # 项目详情
│   │   │   ├── UserManage.vue              # 用户管理（admin）
│   │   │   └── PromptConfig.vue            # Prompt 配置（admin）
│   │   └── router/index.js
└── docs/                                    # 项目文档
    ├── 需求文档/
    ├── 开发文档/
    ├── 测试文档/
    └── 部署文档/
```

## 3. 关键代码说明

### 3.1 项目上传流程

1. 文件保存到本地（UUID + 原扩展名），路径由 `app.upload.path` 配置
2. Python 脚本 pdfminer（pdfminer.six）提取 PDF 全文，返回 JSON：`{text, textLength, pageCount}`
3. 若提取文字为空，报错"此文件为扫描件或无文字层，请上传文字版PDF"
4. 文字写入本地 `.md` 文件（`原文件名_[MD.md]`），保存路径记入 `markdownFilePath`
5. 文字截断到 `app.minimax.max-text-length`（默认 250000 字符）
6. 从 `prompt_template.json` 读取 Prompt 模板，替换 `{content}` 和 `{field_def}` 占位符
7. RestTemplate（30秒连接超时 + 120秒读取超时）调用 MiniMax `/v1/text/chatcompletion_v2`
8. 解析返回的 JSON，提取 `json ... ` 包裹的内容，映射到 Project 实体（支持多组 key 名）
9. 入库，返回完整 Project 信息

### 3.2 MiniMax JSON 解析

MiniMax 返回格式为 `` `json ... ` ``，需去掉代码块标记后解析。

`mapToProject()` 支持多组 key 名映射，兼容不同格式的字段名：
- `projectName` / `project_name` / `项目名称`
- `ceilingPrice` / `ceiling_price` / `最高投标限价` / `拦标价`
- 金额字段统一用 `parseMoney()` 清洗后转为 `BigDecimal`
- 日期字段统一用 `parseDateTime()` 解析

### 3.3 批量删除

- **接口**：`POST /api/project/batch-delete`，Body：`{ids: [1,2,3]}`
- **权限**：需 admin 角色
- **逻辑**：Service 层先查再删，同时删除 PDF 文件和 Markdown 文件
- **SQL 批量删除**：`delete from projects where id in (1,2,3)`

### 3.4 Prompt 模板

- 存储文件：`C:/UPQQ/prompt_template.json`
- system 字段为可选，content 和 fieldDef 为必填占位符
- 调用前替换 `{content}`（PDF 文字）和 `{field_def}`（字段定义）
- 后端缓存：读取后放入内存缓存，PUT 时清空缓存

## 4. 接口清单

| 接口 | 方法 | 认证 | 说明 |
|------|------|------|------|
| `/api/project/upload` | POST | 用户 | 上传 PDF，自动提取字段 |
| `/api/project/list` | GET | 用户 | 分页列表（keyword 搜索） |
| `/api/project/{id}` | GET | 用户 | 项目详情 |
| `/api/project/download/{id}` | GET | 用户 | 下载原始 PDF |
| `/api/project/download/{id}/markdown` | GET | 用户 | 下载提取后的 Markdown |
| `/api/project/{id}` | DELETE | 用户 | 单条删除（同时删文件） |
| `/api/project/batch-delete` | POST | admin | 批量删除 |
| `/api/user/login` | POST | 公开 | 登录 |
| `/api/user/register` | POST | 公开 | 注册 |
| `/api/user/list` | GET | admin | 用户列表 |
| `/api/user/role/{id}` | PUT | admin | 更新角色 |
| `/api/user/password/{id}` | PUT | admin | 重置密码 |
| `/api/prompt` | GET | admin | 获取 Prompt 配置 |
| `/api/prompt` | PUT | admin | 保存 Prompt 配置（清缓存） |

## 5. 数据库结构

### 5.1 users 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键自增 |
| username | VARCHAR(100) | 用户名（邮箱），唯一 |
| password | VARCHAR(64) | MD5 密码 |
| role | VARCHAR(20) | 角色：user / admin |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 5.2 projects 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键自增 |
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

### 5.3 extract_logs 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键自增 |
| file_name | VARCHAR | 原文件名 |
| extract_type | VARCHAR | 提取类型（如 "pdf"） |
| text_length | INT | PDF 原文字数 |
| response_length | INT | AI 返回字数 |
| success | TINYINT(1) | 是否成功 |
| error_message | VARCHAR | 错误信息 |
| duration_ms | INT | 耗时（毫秒） |
| created_at | DATETIME | 创建时间 |

### 5.4 prompt_template 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键自增 |
| name | VARCHAR | 模板名称 |
| content | TEXT | Prompt 内容模板 |
| field_def | TEXT | 字段定义 JSON |
| is_active | TINYINT | 是否激活（1=激活） |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## 6. 配置项（application.yml）

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/douge_bid
    username: douge
    password: douge123456
  servlet:
    multipart:
      max-file-size: 100MB

app:
  upload:
    path: C:\Users\Administrator\.openclaw\workspace\bid-system\backend\uploads
  minimax:
    api-key: <API-KEY>
    api-url: https://api.minimax.chat
    max-text-length: 250000
  pdf-extract:
    script: C:/UPQQ/pdf_extract.py
  prompt-template:
    path: C:/UPQQ/prompt_template.json
  jwt:
    secret: <JWT-SECRET>
    expiration: 86400000
```

## 7. 更新记录

- **2026-04-02**：新增批量删除接口（ProjectController + ProjectService）；PromptTemplateController/Service 支持 is_active 字段；开标时间、质疑截止时间、投标保证金类型从 datetime/decimal 改为 varchar；pdfplumber 替换为 pdfminer.six
- **2026-04-01**：新增原文 Markdown 下载接口（含 textLength、pageCount 统计）；新增 Prompt 配置功能（前后端）；Admin.vue 改造（序号列、录入时间列、分页修复）；新增 UserManage.vue
- **2026-03-31**：PDF 提取从 Vision 截图方案改为 pdfminer 文字提取；Prompt 模板管理从数据库迁移到 JSON 配置文件；axios 超时调整为 600s
- **2026-03-30**：完成登录注册、用户管理、项目管理基础功能
