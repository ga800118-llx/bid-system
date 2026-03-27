# 开发文档

## 1. 技术选型

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2 |
| ORM | MyBatis Plus | 3.5+ |
| 安全认证 | JWT (jjwt) | 0.12+ |
| 文档解析 | PDFBox + Apache POI | PDFBox 2.x / POI 5.x |
| AI 集成 | MiniMax 豆包 API | doubao-seed-2-0-pro-260215 |
| 前端框架 | Vue3 | 3.4+ |
| 前端构建 | Vite | 5.x |
| UI 组件 | Element Plus | 2.x |
| HTTP 客户端 | Axios | 1.x |
| 数据库 | MySQL | 8.0+ |

---

## 2. 项目结构

 bid-system/
|- backend/                          # 后端 Spring Boot 项目
|   |- src/main/java/com/bid/system/
|   |   |- BidSystemApplication.java # 启动类
|   |   |- config/                   # 配置类
|   |   |   |- WebConfig.java        # CORS + JWT 拦截器注册
|   |   |   |- JwtInterceptor.java   # Token 校验拦截器
|   |   |   |- JacksonConfig.java    # JSON 配置
|   |   |- controller/               # REST 控制器
|   |   |- service/                  # 业务逻辑层
|   |   |- entity/                   # 数据库实体
|   |   |- mapper/                   # MyBatis Mapper
|   |   |- dto/                      # 数据传输对象
|   |   |- util/                     # 工具类
|   |- src/main/resources/
|   |   |- application.yml           # 应用配置
|   |- pom.xml                       # Maven 依赖
|   |- uploads/                      # 文件上传目录（运行时创建）
|
|- frontend/                         # 前端 Vue3 项目
|   |- src/
|   |   |- main.js                   # Vue 入口
|   |   |- App.vue                  # 根组件
|   |   |- api/
|   |   |   |- index.js             # Axios 实例 + API 方法
|   |   |- router/
|   |   |   |- index.js             # Vue Router 配置
|   |   |- views/
|   |   |   |- Login.vue            # 登录页
|   |   |   |- Register.vue         # 注册页
|   |   |   |- Home.vue             # 项目列表（首页）
|   |   |   |- Admin.vue            # 管理页（上传文件）
|   |   |   |- ProjectDetail.vue    # 项目详情页
|   |- index.html
|   |- vite.config.js
|   |- package.json
|
|- docs/                            # 项目文档
    |- 需求文档/
    |- 开发文档/
    |- 测试文档/
    |- 部署文档/

---

## 3. 数据库设计

### 3.1 ER 图

 users ──1:N── projects
 (users.id = projects.uploader_id)

### 3.2 表结构

#### users 用户表

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| username | VARCHAR(100) | UNIQUE, NOT NULL | 用户名（邮箱） |
| password | VARCHAR(64) | NOT NULL | MD5 哈希后的密码 |
| role | VARCHAR(20) | NOT NULL, DEFAULT 'user' | 角色：user / admin |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

#### projects 项目表

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 |
| project_name | VARCHAR(255) | 项目名称 |
| project_code | VARCHAR(50) | 项目编号 |
| bidding_agency | VARCHAR(100) | 招标代理机构 |
| client_unit | VARCHAR(100) | 发标单位 |
| bid_open_time | DATETIME | 开标时间 |
| complaint_deadline | DATETIME | 质疑截止时间 |
| ceiling_price | DECIMAL(15,2) | 拦标价 |
| floor_price | DECIMAL(15,2) | 下限价 |
| contract_payment | VARCHAR(300) | 合同付款方式 |
| expert_composition | VARCHAR(200) | 评审专家组成 |
| price_score_method | VARCHAR(300) | 价格分计算方法 |
| subjective_score_details | VARCHAR(300) | 主观分评审细则 |
| bid_bond | DECIMAL(15,2) | 投标保证金 |
| performance_bond | DECIMAL(15,4) | 履约保证金（比例或金额） |
| file_path | VARCHAR(500) | 原始文件存储路径 |
| file_original_name | VARCHAR(255) | 原始文件名 |
| uploader_id | BIGINT | FK -> users.id，上传人 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 3.3 数据库初始化 SQL

`sql
CREATE DATABASE IF NOT EXISTS douge_bid DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE douge_bid;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE COMMENT '用户名（邮箱）',
    password VARCHAR(64) NOT NULL COMMENT 'MD5密码',
    role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色：user/admin',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_name VARCHAR(255) COMMENT '项目名称',
    project_code VARCHAR(50) COMMENT '项目编号',
    bidding_agency VARCHAR(100) COMMENT '招标代理机构',
    client_unit VARCHAR(100) COMMENT '发标单位',
    bid_open_time DATETIME COMMENT '开标时间',
    complaint_deadline DATETIME COMMENT '质疑截止时间',
    ceiling_price DECIMAL(15,2) COMMENT '拦标价',
    floor_price DECIMAL(15,2) COMMENT '下限价',
    contract_payment VARCHAR(300) COMMENT '合同付款方式',
    expert_composition VARCHAR(200) COMMENT '评审专家组成',
    price_score_method VARCHAR(300) COMMENT '价格分计算方法',
    subjective_score_details VARCHAR(300) COMMENT '主观分评审细则',
    bid_bond DECIMAL(15,2) COMMENT '投标保证金',
    performance_bond DECIMAL(15,4) COMMENT '履约保证金',
    file_path VARCHAR(500) COMMENT '文件存储路径',
    file_original_name VARCHAR(255) COMMENT '原始文件名',
    uploader_id BIGINT COMMENT 'FK->users.id',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_name (project_name),
    INDEX idx_uploader (uploader_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- 初始化管理员账户（密码：admin123，MD5：e10adc3949ba59abbe56e057f20f883e）
INSERT INTO users (username, password, role) VALUES
('admin@bid.com', 'e10adc3949ba59abbe56e057f20f883e', 'admin');
`

---

## 4. API 接口文档

> Base URL: http://localhost:8080/api

### 4.1 统一响应格式

成功：
{
  "code": 200,
  "data": { ... },
  "msg": "success"
}

错误：
{
  "code": 400,
  "data": null,
  "msg": "错误描述"
}

### 4.2 用户接口

#### POST /user/register - 用户注册

**请求体：**
{
  "username": "user@example.com",
  "password": "yourpassword"
}

---

#### POST /user/login - 用户登录

**请求体：**
{
  "username": "user@example.com",
  "password": "yourpassword"
}

**响应（成功）：**
{
  "code": 200,
  "data": { "token": "eyJhbGciOiJIUzI1NiJ9..." },
  "msg": "success"
}

登录后，后续所有请求需在 Header 中携带：
Authorization: Bearer <token>

---

#### GET /user/info - 获取当前用户信息

需要认证：是

响应：
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "admin@bid.com",
    "role": "admin",
    "createdAt": "2026-03-25T10:00:00"
  }
}

---

#### GET /user/list - 用户列表（仅管理员）

需要认证：是 (admin)

---

#### PUT /user/role/{id} - 修改用户角色（仅管理员）

需要认证：是 (admin)
参数：role = user 或 admin

---

### 4.3 项目接口

#### POST /project/upload - 上传招标文件（仅管理员）

需要认证：是 (admin)
Content-Type：multipart/form-data

表单字段：
| 字段 | 类型 | 说明 |
|------|------|------|
| file | File | PDF/Word 文件，最大 100MB |

---

#### GET /project/list - 项目列表（支持搜索+分页）

需要认证：是

查询参数：
| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| keyword | string | null | 按项目名称模糊搜索 |
| page | int | 1 | 页码 |
| size | int | 10 | 每页条数 |

---

#### GET /project/{id} - 项目详情

需要认证：是

返回完整 Project 对象（含所有提取字段，不含 filePath）

---

#### GET /project/download/{id} - 下载原始文件

需要认证：是

响应：文件流（Blob），Content-Disposition: attachment

---

#### DELETE /project/{id} - 删除项目（仅管理员）

需要认证：是 (admin)

---

## 5. AI 提取流程详解

### 5.1 文本提取模式（优先）

用户上传文件
  -> PDFBox 提取纯文本（.pdf）
  -> POI HWPF/XWPF 提取文本（.doc/.docx）
  -> 截断至前 8000 字符
  -> 调用豆包 chat 接口（文本模式）
  -> 模型输出 JSON
  -> 解析 JSON -> Project 实体
  -> 存入数据库

### 5.2 图像提取模式（兜底）

若文本提取结果不完整（字段缺失过多）
  -> PDF 前 5 页转为 150DPI JPG 图片
  -> 调用豆包 vision 多模态接口
  -> 返回结构化 JSON

### 5.3 提取 Prompt 设计

关键字段处理逻辑：
- subjectiveScoreDetails 主观分：仅包含需要评委主观判断的条目（如技术方案、售后服务），客观评分项（业绩、资质证书数量）不填入此字段
- performanceBond 履约保证金：比例写成小数（如合同价5% -> 0.05），固定金额写数字

---

## 6. 前端路由

| 路径 | 组件 | 权限 |
|------|------|------|
| /login | Login.vue | 公开 |
| /register | Register.vue | 公开 |
| / | Home.vue | 需登录 |
| /admin | Admin.vue | 需登录+admin |
| /project/:id | ProjectDetail.vue | 需登录 |

---

## 7. 环境变量配置

### 后端 application.yml

 server:
   port: 8080

 spring:
   datasource:
     driver-class-name: com.mysql.cj.jdbc.Driver
     url: jdbc:mysql://localhost:3306/douge_bid?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
     username: douge
     password: douge123456

 app:
   jwt:
     secret: bid-system-jwt-secret-key-2024-very-long-and-secure
     expiration: 86400000        # 24小时
   upload:
     path: ./uploads/           # 上传文件存放目录
   minimax:
     api-key: <your-api-key>     # MiniMax API Key
     model: doubao-seed-2-0-pro-260215

### 前端 vite.config.js

需要配置代理解决跨域：

 server: {
   port: 5173,
   proxy: {
     '/api': {
       target: 'http://localhost:8080',
       changeOrigin: true
     }
   }
 }