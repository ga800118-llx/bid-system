# 部署文档

> 更新时间：2026-05-14
> 当前主要用于本地开发和交接说明，生产部署仍需按实际环境补充

## 当前本地运行方式

### 后端

当前本地后端通过已打包 jar + H2 本地库运行。

```bash
JAVA="$PWD/.tools/jdk-17.0.18+8-jre/Contents/Home/bin/java"
SCHEMA="$PWD/backend/local-schema.sql"
"$JAVA" -jar backend/target/bid-system-local.jar \
  --server.port=8080 \
  --spring.datasource.driver-class-name=org.h2.Driver \
  --spring.datasource.url="jdbc:h2:file:$PWD/backend/.local-db/bid;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;INIT=RUNSCRIPT FROM '$SCHEMA'" \
  --spring.datasource.username=sa \
  --spring.datasource.password=
```

说明：

- 本地数据库：`backend/.local-db/bid`
- 初始化脚本：`backend/local-schema.sql`
- 管理员账号：`admin@bid.com / admin123`

### 前端

```bash
cd frontend
PATH="/Users/myfile/bid-system/.tools/node/bin:$PATH" npm run build
```

当前本地访问：

- 前端：`http://localhost:3000`
- 后端：`http://localhost:8080`

## 本地局部编译口径

由于本地 Maven portable 环境不完整，后端默认采用：

- ECJ 局部编译
- Python `zipfile` 替换 jar 中 class

详细口径见：

- [DEVELOPMENT.md](../../DEVELOPMENT.md)

## 当前部署边界

当前项目文档里的部署说明，以“本地开发环境”优先。

以下内容仍需后续按真实生产环境单独补充：

- MySQL 生产初始化脚本
- Nginx / 网关配置
- 生产环境密钥管理
- 文件存储部署策略
- 定时任务部署方式

## 运行产物说明

以下目录或文件属于本地运行数据：

- `backend/.local-db/`
- `backend/*.log`
- `backend/*.gz`
- `uploads/`

这些内容默认不作为可提交源码资产看待。
