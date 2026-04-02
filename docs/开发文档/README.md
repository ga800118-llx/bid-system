# 开发文档

## 1. 技术选型

| 分类 | 技术 | 说明 |
|------|------|------|
| 后端 | Java Spring Boot 3.2 | REST API 框架 |
| ORM | MyBatis Plus | 数据库访问 |
| 数据库 | MySQL | douge_bid 库 |
| 前端 | Vue3 + Element Plus | 组件化 UI |
| 构建工具 | Maven (后端) / Vite (前端) | 包管理和构建 |
| JDK | OpenJDK 21 | Java 运行时 |
| PDF 提取 | pdfplumber | Python 库，替代 pdfminer |
| AI 接口 | MiniMax Chat API | 文字理解提取 |

## 2. 项目结构

`
bid-system/
├── backend/
│   ├── src/main/java/com/bid/system/
│   │   ├── BidSystemApplication.java        # 启动入口
│   │   ├── config/                          # 配置类
│   │   │   └── WebConfig.java               # CORS + JWT Filter
│   │   ├── controller/                       # 控制器
│   │   │   ├── ProjectController.java        # 项目相关接口
│   │   │   ├── UserController.java          # 用户相关接口
│   │   │   └── PromptTemplateController.java # Prompt 配置接口
│   │   ├── dto/                             # 数据传输对象
│   │   │   └── ApiResponse.java
│   │   ├── entity/                          # 实体类
│   │   │   └── Project.java                 # 项目实体
│   │   ├── mapper/                          # MyBatis Mapper
│   │   │   └── ProjectMapper.java
│   │   ├── service/                          # 业务逻辑
│   │   │   ├── ProjectService.java          # 项目服务（含批量删除）
│   │   │   ├── UserService.java
│   │   │   ├── MiniMaxTextExtractService.java # MiniMax AI 提取
│   │   │   └── PromptTemplateService.java   # Prompt 模板读写
│   │   └── util/                            # 工具类
│   │       └── JwtUtil.java                 # JWT 工具
│   └── src/main/resources/
│       └── application.yml                   # 配置文件
├── frontend/
│   └── src/
│       ├── api/
│       │   └── index.js                      # axios 接口封装
│       ├── views/
│       │   ├── Login.vue
│       │   ├── Admin.vue                    # 首页（含批量删除 UI）
│       │   ├── ProjectDetail.vue
│       │   ├── UserManage.vue
│       │   └── PromptConfig.vue            # Prompt 配置页
│       └── router/index.js
└── docs/                                    # 项目文档
    ├── 需求文档/
    ├── 开发文档/
    ├── 测试文档/
    └── 部署文档/
`

## 3. 关键代码说明

### 3.1 项目上传流程
1. 文件保存本地（UUID + 原扩展名）
2. Python 脚本 pdfplumber 提取 PDF 全文，返回 text / textLength / pageCount
3. 从 prompt_template.json 读取 Prompt 模板，替换 {content} 和 {field_def}
4. RestTemplate（30s 连接超时 + 120s 读取超时）调用 MiniMax /v1/text/chatcompletion_v2
5. 解析返回的 JSON，映射到 Project 实体
6. 入库，生成 Markdown 文件

### 3.2 MiniMax JSON 解析
MiniMax 返回格式： `json ... ` ，需去掉代码块标记后解析。

### 3.3 批量删除
- 接口：POST /api/project/batch-delete，Body: {ids: []}
- Service 层先查再删，同时删除 PDF 和 Markdown 文件
- 管理员权限校验

### 3.4 Prompt 模板
- 文件路径：C:/UPQQ/prompt_template.json
- system 字段为选填，content 和 fieldDef 为必填
- fieldDef 包含 14 个字段的 key/label/semantic 定义
- 占位符 {content} 和 {field_def} 在调用前被替换

## 4. 接口清单

| 接口 | 方法 | 认证 | 说明 |
|------|------|------|------|
| /api/project/upload | POST | 用户 | 上传 PDF |
| /api/project/list | GET | 用户 | 分页列表 |
| /api/project/{id} | GET | 用户 | 项目详情 |
| /api/project/download/{id} | GET | 用户 | 下载 PDF |
| /api/project/download/{id}/markdown | GET | 用户 | 下载 Markdown |
| /api/project/{id} | DELETE | 用户 | 单条删除 |
| /api/project/batch-delete | POST | admin | 批量删除 |
| /api/user/login | POST | 公开 | 登录 |
| /api/user/register | POST | 公开 | 注册 |
| /api/user/list | GET | admin | 用户列表 |
| /api/user/role/{id} | PUT | admin | 更新角色 |
| /api/user/password/{id} | PUT | admin | 重置密码 |
| /api/prompt | GET | admin | 获取 Prompt 配置 |
| /api/prompt | PUT | admin | 保存 Prompt 配置 |

## 5. 数据库

**projects 表主要字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| project_name | VARCHAR(200) | 项目名称（文件名） |
| project_code | VARCHAR(100) | 项目编号 |
| bidding_agency | VARCHAR(200) | 招标代理机构 |
| client_unit | VARCHAR(200) | 招标人 |
| bid_open_time | VARCHAR(200) | 开标时间（存原文） |
| complaint_deadline | VARCHAR(200) | 质疑截止时间（存原文） |
| ceiling_price | DECIMAL(15,2) | 拦标价 |
| floor_price | DECIMAL(15,2) | 下限价 |
| contract_payment | VARCHAR(500) | 付款方式 |
| expert_composition | VARCHAR(200) | 专家组成 |
| price_score_method | VARCHAR(200) | 价格分计算方法 |
| subjective_score_details | VARCHAR(500) | 主观分详情 |
| bid_bond | VARCHAR(200) | 投标保证金（存原文） |
| performance_bond | VARCHAR(500) | 履约保证金（存原文） |
| file_path | VARCHAR(500) | PDF 文件路径 |
| markdown_file_path | VARCHAR(500) | Markdown 文件路径 |
| text_length | INT | PDF 总字数 |
| page_count | INT | PDF 总页数 |
| file_original_name | VARCHAR(200) | 原始文件名 |
| uploader_id | BIGINT | 上传用户 ID |
| created_at | DATETIME | 录入时间 |

**users 表：** id, username, password(MD5), role(admin/user), created_at, updated_at

## 6. 配置项（application.yml）

`yaml
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
    path: C:\\Users\\Administrator\\.openclaw\\workspace\\bid-system\\backend\\uploads
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
`

## 7. 更新记录

- 2026-04-02：新增批量删除功能（ProjectService + ProjectController）；PromptTemplateController/Service 支持 system 字段；开标时间、质疑截止时间、履约保证金类型从 datetime/decimal 改为 varchar；pdfminer 替换为 pdfplumber
- 2026-04-01：新增原文 Markdown 下载接口；Prompt 配置功能；Admin.vue 改造（序号列、录入时间列、分页修复）；UserManage.vue 新增
- 2026-03-31：PDF 提取从 Vision 方案改为 pdfminer 文字提取；Prompt 模板迁移到 JSON 文件；axios 超时 600s
- 2026-03-30：完成登录注册、用户管理、项目管理基础功能
