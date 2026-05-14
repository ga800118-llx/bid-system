# 招投标管理系统

[![GitHub Repo](https://img.shields.io/badge/GitHub-bid--system-blue.svg)](https://github.com/ga800118-llx/bid-system)

## 项目简介

招投标管理系统。项目早期以“上传文件 + AI 提取项目信息”为主，当前已完成一套可复用的基础平台底座，并以此承接后续业务模块。

当前基础平台已基本收尾，核心能力包括：

- 部门管理
- 用户管理 V2
- 角色权限管理
- 操作日志
- 系统配置
- 字典管理
- 文件中心
- 消息中心
- 待办中心
- 个人中心 / 账号安全
- 导入导出基础能力
- 前端 design-system、业务组件复用和亮色 / 暗色主题

当前阶段不继续扩展定时任务、文件存储、消息通知、API Key、限流，也不扩展项目管理、上传、项目删除相关功能。

## 技术栈

后端：

- Java 17
- Spring Boot 3.2
- MyBatis-Plus
- JWT
- H2 本地开发库 / MySQL 生产库
- Apache POI
- Python 3 + PyMuPDF（本地 PDF 文本提取）

前端：

- Vue 3
- Vite
- Arco Design
- Axios

## 本地运行

项目路径：

```bash
/Users/myfile/bid-system
```

### 后端

```bash
JAVA="$PWD/.tools/jdk-17.0.18+8-jre/Contents/Home/bin/java"
SCHEMA="$PWD/backend/local-schema.sql"
"$JAVA" -jar backend/target/bid-system-local.jar \
  --server.port=8080 \
  --spring.datasource.driver-class-name=org.h2.Driver \
  --spring.datasource.url="jdbc:h2:file:$PWD/backend/.local-db/bid;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;INIT=RUNSCRIPT FROM '$SCHEMA'" \
  --spring.datasource.username=sa \
  --spring.datasource.password= \
  --mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl
```

### 前端构建

```bash
cd frontend
PATH="/Users/myfile/bid-system/.tools/node/bin:$PATH" npm run build
```

当前本地调试使用 Python SPA proxy：

- 前端：`http://localhost:3000`
- 后端：`http://localhost:8080`
- `/api` 代理到后端

登录账号：

```text
admin@bid.com / admin123
```

## 文档目录

所有项目文档统一存放在 `docs/` 目录：

```text
docs/
├── README.md
├── 项目现状与交接.md
├── 基础平台基线快照-2026-04-28.md
├── 基础平台基线验收清单-2026-04-28.md
├── 基础模块实施计划.md
├── 数据库设计说明.md
├── 附件与文件中心实施方案.md
├── 基础平台帮助与使用手册.md
├── 部门与用户管理设计.md
├── 权限管理系统设计.md
├── 后台页面开发规范.md
├── UI组件库规范.md
├── UI设计规范.md
├── 业务组件复用规范.md
├── 主题与暗黑模式规范.md
├── 需求文档/
├── 开发文档/
├── 测试文档/
└── 部署文档/
```

## 开发约定

- 当前阶段优先做基础平台层能力，不扩展项目管理、上传、项目删除相关功能
- 基础平台当前已基本封板，后续需求优先判断是否属于“业务模块接入”
- 新增或修改前端后台页面前，必须先阅读 `DEVELOPMENT.md` 的“前端页面开发规范”、`docs/后台页面开发规范.md`、`docs/UI组件库规范.md`、`docs/业务组件复用规范.md`、`docs/主题与暗黑模式规范.md`
- 前端后台页面必须使用统一 `Layout`、路由 `meta.title`、样板页结构、`DsDataTable`、`DsPagination`、`DsModalForm` 和已有业务组件
- 前端样式必须使用 `frontend/src/design-system/tokens` 中的 `--ds-*` token，新增页面必须兼容亮色和暗色主题
- 本地 H2 初始化脚本 `backend/local-schema.sql` 只做建表、必要种子和兼容性补字段，不覆盖已有数据
- 工作树可能包含历史改动和本地运行产物，修改时只碰当前任务相关文件
- 重要实现口径同步更新 `DEVELOPMENT.md` 与 `docs/` 下对应设计文档

## 接手阅读顺序

如果是新加入项目或准备继续开发，建议按下面顺序看：

1. `docs/项目现状与交接.md`
2. `docs/基础平台基线快照-2026-04-28.md`
3. `docs/基础平台基线验收清单-2026-04-28.md`
4. `docs/接手人30分钟阅读清单.md`
5. `docs/数据库设计说明.md`
6. `docs/附件与文件中心实施方案.md`
7. `docs/基础平台帮助与使用手册.md`
8. `DEVELOPMENT.md`
9. `docs/后台页面开发规范.md`
10. `docs/UI组件库规范.md`
11. `docs/业务组件复用规范.md`
12. `docs/主题与暗黑模式规范.md`
13. `docs/基础模块实施计划.md`
14. `docs/权限管理系统设计.md`
15. `docs/UI设计规范.md`

## Git 提交规范

- `feat`: 新功能
- `fix`: 修复 Bug
- `docs`: 文档变更
- `refactor`: 重构
- `test`: 测试相关

## 远程仓库

https://github.com/ga800118-llx/bid-system
