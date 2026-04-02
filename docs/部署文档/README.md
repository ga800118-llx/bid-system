# 部署文档

## 1. 环境要求

### 1.1 基础环境

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | OpenJDK 21+ 或 JDK 17+ | 推荐 Eclipse Adoptium |
| Maven | 3.9+ | 后端构建 |
| Node.js | 18+ | 前端构建 |
| MySQL | 8.0+ | 数据库，推荐 8.0.33+ |
| Git | 任意版本 | 代码管理 |
| Python | 3.8+ | PDF 文字提取（pdfminer.six） |

### 1.2 硬件要求

- **开发环境**：4GB+ RAM，50GB+ 可用磁盘
- **生产环境**：建议 8GB+ RAM，100GB+ SSD

---

## 2. 数据库初始化

### 2.1 创建数据库和用户

```sql
-- root 用户执行
CREATE DATABASE IF NOT EXISTS douge_bid DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'douge'@'localhost' IDENTIFIED BY 'douge123456';
GRANT ALL PRIVILEGES ON douge_bid.* TO 'douge'@'localhost';
FLUSH PRIVILEGES;
```

### 2.2 初始化表结构和数据

```sql
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
    project_name VARCHAR(200) COMMENT '项目名称（文件名）',
    project_code VARCHAR(100) COMMENT '项目编号',
    bidding_agency VARCHAR(200) COMMENT '招标代理机构',
    client_unit VARCHAR(200) COMMENT '发标单位',
    bid_open_time VARCHAR(200) COMMENT '开标时间（存原文）',
    complaint_deadline VARCHAR(200) COMMENT '质疑截止时间（存原文）',
    ceiling_price DECIMAL(15,2) COMMENT '拦标价',
    floor_price DECIMAL(15,2) COMMENT '下限价',
    contract_payment VARCHAR(500) COMMENT '付款方式',
    expert_composition VARCHAR(200) COMMENT '专家组成',
    price_score_method VARCHAR(200) COMMENT '价格分计算方法',
    subjective_score_details VARCHAR(500) COMMENT '主观分评审细则',
    bid_bond VARCHAR(200) COMMENT '投标保证金（存原文）',
    performance_bond VARCHAR(500) COMMENT '履约保证金（存原文）',
    file_path VARCHAR(500) COMMENT 'PDF文件路径',
    markdown_file_path VARCHAR(500) COMMENT 'Markdown文件路径',
    text_length INT COMMENT 'PDF总字数',
    page_count INT COMMENT 'PDF总页数',
    file_original_name VARCHAR(200) COMMENT '原始文件名',
    uploader_id BIGINT COMMENT 'FK->users.id',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_name (project_name),
    INDEX idx_uploader (uploader_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- 初始化管理员账号（密码：admin123）
INSERT INTO users (username, password, role) VALUES
('admin@bid.com', 'e10adc3949ba59abbe56e057f20f883e', 'admin');
``

> 初始管理员：邮箱 admin@bid.com，密码 admin123（MD5：e10adc3949ba59abbe56e057f20f883e）

---

## 3. 后端部署

### 3.1 Python 环境准备

PDF 文字提取依赖 pdfminer.six：

```bash
pip install pdfminer.six
```

Prompt 模板文件路径（需提前创建或配置）：
- 默认路径：`C:/UPQQ/prompt_template.json`
- 可在 `application.yml` 中通过 `app.prompt-template.path` 自定义

### 3.2 开发环境启动

```bash
cd bid-system/backend
mvn spring-boot:run
```

启动成功后会看到类似输出：

```
Started BidSystemApplication in X.XXX seconds (JVM running for X.XXX)
```

后端地址：http://localhost:8080

### 3.3 生产环境打包

```bash
cd bid-system/backend
mvn clean package -DskipTests
```

生成 JAR：`target/bid-system-1.0.0.jar`

### 3.4 生产环境运行

```bash
java -jar bid-system-1.0.0.jar
``

或带参数运行：

```bash
java -jar bid-system-1.0.0.jar --server.port=8080
```

### 3.5 上传目录

文件上传存储在 `backend/uploads/` 目录（相对于 JAR 包所在目录）。

**重要：** 生产环境务必确保 uploads 目录所在磁盘有足够空间，建议定期清理或设置定期归档策略。

---

## 4. 前端部署

### 4.1 开发环境启动

```bash
cd bid-system/frontend
npm install
npm run dev -- --host
```

前端地址：http://localhost:3000

### 4.2 生产环境打包

```bash
cd bid-system/frontend
npm run build
```

生成文件在 `frontend/dist/` 目录，可直接部署到 Nginx 或任意静态服务器。

### 4.3 Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /path/to/bid-system/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    # API 代理
    location /api {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

---

## 5. 安全配置建议

### 5.1 生产环境必做

1. **修改数据库密码** — 将 `application.yml` 中的 douge123456 改为强密码
2. **修改 JWT Secret** — 将 `app.jwt.secret` 改为随机长字符串
3. **配置 CORS** — 生产环境 WebConfig 中将 `allowedOriginPatterns` 设为具体域名
4. **关闭调试日志** — 生产环境 `application.yml` 中关闭 MyBatis SQL 日志
5. **API Key 管理** — MiniMax API Key 不要提交到 Git，敏感配置使用环境变量

### 5.2 防火墙规则

- MySQL：仅允许应用服务器访问（127.0.0.1 或内网 IP）
- 后端 8080 端口：仅允许 Nginx/前端服务器访问
- 前端 3000 端口：开发环境可开，生产关闭或仅对内网开放

---

## 6. 运维监控

### 6.1 日志位置

后端日志输出到控制台（stdout），生产环境建议用 systemd 或 Docker 管理并收集日志。

### 6.2 定期备份

建议每日备份数据库：

```bash
mysqldump -u douge -p douge_bid > backup_YYYYMMDD.sql
```

---

## 7. 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 上传文件失败 | uploads 目录不存在或无写权限 | 创建目录并 chmod 777 |
| AI 提取全 null | MiniMax API Key 无效或余额不足 | 检查 API Key 和账户余额 |
| 登录 401 | Token 过期或 JWT Secret 不一致 | 重新登录或检查 JWT 配置 |
| CORS 跨域报错 | 生产环境 CORS 配置为 * | 改为具体域名 |
| PDF 提取失败 | 文件为扫描件无文字层 | 需上传文字版 PDF |
| 文字被截断 | 超出 max-text-length 上限 | 增大配置或压缩 PDF |
