# 开发文档

## 1. 技术栈

| 分类 | 技术 | 版本/说明 |
|------|------|----------|
| 后端 | Java Spring Boot | 3.2 |
| ORM | MyBatis Plus |.baomidou.mybatisplus |
| 数据库 | MySQL | douge_bid 库，utf8mb4 |
| 前端 | Vue3 + Element Plus | Vite 构建 |
| JDK | OpenJDK 21 | - |
| PDF 提取 | pdfplumber | Python pip 包 |
| AI 接口 | MiniMax Chat API | M2.7 模型 |
| JWT | jjwt | io.jsonwebtoken |
| 工具类 | Hutool | 通用工具 |

## 2. 项目结构

`
bid-system/
├── backend/
│   ├── src/main/java/com/bid/system/
│   │   ├── BidSystemApplication.java          # Spring Boot 启动入口
│   │   ├── config/
│   │   │   ├── WebConfig.java                # CORS 配置 + JWT 拦截器注册
│   │   │   ├── JwtInterceptor.java           # Token 解析过滤器
│   │   │   ├── RestConfig.java               # RestTemplate Bean（30s连接+120s读取超时）
│   │   │   ├── MybatisPlusConfig.java        # 分页插件配置
│   │   │   └── JacksonConfig.java            # JSON 配置
│   │   ├── controller/
│   │   │   ├── ProjectController.java        # 上传/列表/详情/下载/删除
│   │   │   ├── UserController.java           # 登录/注册/用户管理
│   │   │   └── PromptTemplateController.java # Prompt 配置读写
│   │   ├── dto/
│   │   │   ├── ApiResponse.java              # 统一响应 {code, msg, data}
│   │   │   ├── LoginRequest.java
│   │   │   └── RegisterRequest.java
│   │   ├── entity/
│   │   │   ├── Project.java                  # 项目实体（14 个提取字段 + 文件信息）
│   │   │   ├── User.java                    # 用户实体
│   │   │   ├── ExtractLog.java               # 提取日志（记录每次调用）
│   │   │   └── PromptTemplate.java           # Prompt 模板实体（DB 留存，非主要来源）
│   │   ├── mapper/
│   │   │   ├── ProjectMapper.java
│   │   │   ├── UserMapper.java
│   │   │   ├── ExtractLogMapper.java
│   │   │   └── PromptTemplateMapper.java
│   │   ├── service/
│   │   │   ├── ProjectService.java          # 上传/搜索/详情/批量删除
│   │   │   ├── UserService.java             # 注册/登录/用户CRUD
│   │   │   ├── MiniMaxTextExtractService.java # PDF提取 + AI调用 + JSON解析
│   │   │   └── PromptTemplateService.java   # 读/写/构建 Prompt（含内存缓存）
│   │   └── util/
│   │       └── JwtUtil.java                 # Token 生成与解析
│   └── src/main/resources/
│       └── application.yml
├── frontend/
│   ├── src/
│   │   ├── api/index.js                     # axios 封装，所有 API 统一出口
│   │   ├── views/
│   │   │   ├── Login.vue / Register.vue     # 登录注册
│   │   │   ├── Home.vue                     # 首页（项目列表 + 搜索 + 分页）
│   │   │   ├── Admin.vue                    # 上传页（进度条 + 批量删除）
│   │   │   ├── ProjectDetail.vue            # 详情页（字段展示 + 文件下载）
│   │   │   ├── UserManage.vue              # 用户管理（改角色 + 重置密码）
│   │   │   └── PromptConfig.vue            # Prompt 配置（system/content/fieldDef）
│   │   └── router/index.js                  # 路由 + 守卫
│   └── vite.config.js                       # dev:3000 / proxy:/api→8080
└── docs/
    ├── 需求文档/
    ├── 开发文档/
    ├── 测试文档/
    └── 部署文档/
`

## 3. 核心逻辑详解

### 3.1 项目上传流程（ProjectService.uploadAndExtract）

`
用户上传文件
    ↓
1. 保存到本地（UUID + 原扩展名）
   路径：app.upload.path（application.yml 配置）
    ↓
2. 调用 pdfplumber 提取文字
   ProcessBuilder 执行 python C:/UPQQ/pdf_extract.py <filePath>
   返回 JSON：{text, textLength, pageCount}
   若 text 为空 → 抛异常"此文件为扫描件或无文字层，请上传文字版PDF"
    ↓
3. 写入 Markdown 文件（{原文件名_[MD.md]}）
   保存路径记入 markdownFilePath
    ↓
4. 截断文字（maxTextLength，默认 25 万字符）
    ↓
5. 从 JSON 文件读取 Prompt 模板
   替换 {content} → PDF 文字
   替换 {field_def} → 字段定义的人类可读文本
    ↓
6. RestTemplate POST MiniMax /v1/text/chatcompletion_v2
   Model: MiniMax-M2.7
   Temperature: 0.1
   超时：30s 连接 / 120s 读取
    ↓
7. 解析返回
   去掉 \\\json ... \\\ 包裹
   字段名兼容多组 key（如 projectName / project_name / 项目名称）
   parseMoney() 清洗金额 → BigDecimal
   入库
`

### 3.2 JWT 认证流程

`
请求进入 → JwtInterceptor（preHandle）
    ↓
从 Header 提取 Authorization: Bearer <token>
    ↓
JwtUtil.parseToken() 验证签名和过期时间
    ↓
从 Token 中取出 username 和 role
存入 request.attribute("username") 和 request.attribute("role")
    ↓
继续执行 Controller

excludePathPatterns：
  /api/user/login
  /api/user/register
`

### 3.3 Prompt 模板机制

- 模板存储在 C:/UPQQ/prompt_template.json（JSON 文件，非数据库）
- 字段定义（fieldDef）是 JSON，包含 ields 数组，每个字段有 key、label、semantic
- 后端 uildFieldDefText() 将 fieldDef JSON 转换为可读文本
- PromptTemplateService 带内存缓存（olatile Map cachedTemplate），PUT 时清空缓存

### 3.4 批量删除

- Controller：POST /api/project/batch-delete，Body：{ids: [1,2,3]}
- 需 admin 角色
- Service：先 selectBatchIds 查到文件路径，再 deleteBatchIds 删库，同时删 PDF 和 Markdown 文件

### 3.5 MiniMax JSON 解析细节

MiniMax 返回示例：
\\\
\\\json
{
  "choices": [{"message": {"content": "\\\json {...} \\\"}}]
}
\\\

解析步骤：
1. 提取 choices[0].message.content
2. 去掉首尾空白
3. 若以 ` json ` 开头 → 去掉前缀
4. 若以 " 开头（JSON 字符串包裹）→ 去掉首尾引号
5. objectMapper.readValue(content, Map.class)

mapToProject() 兼容多组 key 名：
- projectName / project_name / 项目名称
- ceilingPrice / ceiling_price / 最高投标限价 / 拦标价
- 其他字段同理（多组 key 映射到同一 Java 字段）

## 4. 数据库结构

### 4.1 users 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 AUTO_INCREMENT |
| username | VARCHAR(100) | 唯一，不可重复 |
| password | VARCHAR(64) | MD5 哈希 |
| role | VARCHAR(20) | user / admin |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间（自动） |

### 4.2 projects 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| project_name | VARCHAR(200) | 项目名称（文件名） |
| project_code | VARCHAR(100) | 项目编号 |
| bidding_agency | VARCHAR(200) | 招标代理机构 |
| client_unit | VARCHAR(200) | 发标单位 |
| bid_open_time | VARCHAR(200) | 开标时间（原文保存） |
| complaint_deadline | VARCHAR(200) | 质疑截止时间（原文保存） |
| ceiling_price | DECIMAL(15,2) | 拦标价 |
| floor_price | DECIMAL(15,2) | 下限价 |
| contract_payment | VARCHAR(500) | 付款方式 |
| expert_composition | VARCHAR(200) | 专家组成 |
| price_score_method | VARCHAR(200) | 价格分计算方法 |
| subjective_score_details | VARCHAR(500) | 主观分评审细则 |
| bid_bond | DECIMAL(15,2) | 投标保证金 |
| performance_bond | VARCHAR(500) | 履约保证金（原文保存） |
| file_path | VARCHAR(500) | PDF 文件本地路径 |
| markdown_file_path | VARCHAR(500) | Markdown 文件本地路径 |
| text_length | INT | PDF 总字数 |
| page_count | INT | PDF 总页数 |
| file_original_name | VARCHAR(200) | 上传时的原始文件名 |
| uploader_id | BIGINT | 上传用户 FK → users.id |
| created_at | DATETIME | 录入时间 |
| updated_at | DATETIME | 更新时间 |

### 4.3 extract_logs 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| file_name | VARCHAR | 原文件名 |
| extract_type | VARCHAR | 提取类型（如 "pdf"） |
| text_length | INT | PDF 原始文字数 |
| response_length | INT | AI 返回字数 |
| success | TINYINT(1) | 是否成功（1/0） |
| error_message | VARCHAR | 错误信息 |
| duration_ms | INT | 耗时（毫秒） |
| created_at | DATETIME | 创建时间 |

### 4.4 prompt_template 表（DB 留存）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR | 模板名称 |
| content | TEXT | Prompt 模板内容 |
| field_def | TEXT | 字段定义 JSON |
| is_active | TINYINT | 是否激活 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

> 注：当前 Prompt 模板实际存储在 JSON 文件（C:/UPQQ/prompt_template.json），数据库表仅作历史留存。

## 5. 接口清单

### 5.1 认证相关

| 接口 | 方法 | 认证 | 说明 |
|------|------|------|------|
| /api/user/register | POST | 公开 | 注册，Body: {username, password} |
| /api/user/login | POST | 公开 | 登录，Body: {username, password}，返回 {token, username} |
| /api/user/info | GET | 用户 | 当前用户信息 |

### 5.2 项目相关

| 接口 | 方法 | 认证 | 说明 |
|------|------|------|------|
| /api/project/upload | POST | admin | 上传文件，Form: file=xxx.pdf |
| /api/project/list | GET | 用户 | 列表，参数: keyword, page, size |
| /api/project/{id} | GET | 用户 | 详情，filePath 字段不返回 |
| /api/project/download/{id} | GET | 用户 | 下载原始 PDF |
| /api/project/download/{id}/markdown | GET | 用户 | 下载提取后的 Markdown |
| /api/project/{id} | DELETE | admin | 删除项目（含文件） |
| /api/project/batch-delete | POST | admin | 批量删除，Body: {ids: [1,2,3]} |

### 5.3 用户管理

| 接口 | 方法 | 认证 | 说明 |
|------|------|------|------|
| /api/user/list | GET | admin | 用户列表 |
| /api/user/role/{id} | PUT | admin | 更新角色，参数: role=user/admin |
| /api/user/password/{id} | PUT | admin | 重置密码，参数: password |

### 5.4 Prompt 配置

| 接口 | 方法 | 认证 | 说明 |
|------|------|------|------|
| /api/prompt | GET | admin | 获取 Prompt 配置（从 JSON 文件） |
| /api/prompt | PUT | admin | 保存 Prompt 配置（更新 JSON 文件，清缓存） |

## 6. 配置项（application.yml）

`yaml
server:
  port: 8080

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/douge_bid?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: douge
    password: douge123456
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true   # 下划线→驼峰自动映射
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

app:
  jwt:
    secret: bid-system-jwt-secret-key-2024-very-long-and-secure
    expiration: 86400000                   # 24小时
  upload:
    path: ./uploads/                       # 相对 JAR 包路径
  minimax:
    api-key: <API-KEY>                     # MiniMax API Key
    api-url: https://api.minimax.chat
    max-text-length: 250000                # 最大输入字符数
  pdf-extract:
    script: C:/UPQQ/pdf_extract.py         # PDF 提取 Python 脚本
  prompt-template:
    path: C:/UPQQ/prompt_template.json     # Prompt 模板 JSON 文件
`

## 7. 前端 API 封装（api/index.js）

- aseURL: /api，通过 Vite proxy 转发到 8080
- 请求拦截器：自动附加 Authorization: Bearer <token>
- 响应拦截器：401 时清除 localStorage 并跳转 /login
- 所有接口都返回 esp.data（axios interceptor 处理）
- 上传接口支持 onUploadProgress，用于进度条

## 8. 更新记录

- **2026-04-02**：新增批量删除接口；Prompt 配置改为 JSON 文件存储（后端内存缓存）；字段类型调整（bidOpenTime、complaintDeadline、bidBond、performanceBond 改为 VARCHAR 存原文）
- **2026-04-01**：新增原文 Markdown 下载（textLength、pageCount 字段）；Prompt 配置前后端；Admin.vue 改造（序号列、录入时间列、分页修复）；新增 UserManage.vue
- **2026-03-31**：PDF 提取从 Vision 截图方案改为 pdfplumber 文字提取；Prompt 模板管理从数据库迁移到 JSON 配置文件；axios 超时调整为 600s
- **2026-03-30**：完成登录注册、用户管理、项目管理基础功能
