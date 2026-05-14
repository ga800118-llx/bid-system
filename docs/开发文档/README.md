# 开发文档

> 更新时间：2026-04-21
> 当前口径：以基础平台底座为主，早期项目上传相关内容降级为历史背景

## 当前技术栈

### 后端

- Java 17
- Spring Boot 3.2
- MyBatis-Plus
- JWT
- H2 本地开发库 / MySQL 生产库

### 前端

- Vue 3
- Vite
- Arco Design
- Axios

### 本地辅助依赖

- Python 3 + PyMuPDF
- ECJ 局部编译
- 本地 jar 替换

## 当前项目结构

```text
bid-system/
├── backend/                      # Spring Boot 后端
├── frontend/                     # Vue 3 前端
├── docs/                         # 项目文档
├── .tools/                       # 本地运行依赖（非业务源码）
├── uploads/                      # 本地上传/附件运行产物
└── backend/.local-db/            # 本地 H2 数据库
```

## 当前后端重点模块

### 基础平台

- `controller/DepartmentController.java`
- `controller/SystemUserController.java`
- `controller/RoleController.java`
- `controller/OperationLogController.java`
- `controller/SystemConfigController.java`
- `controller/DictController.java`

### 关键服务

- `service/DepartmentService.java`
- `service/UserService.java`
- `service/RoleService.java`
- `service/OperationLogService.java`
- `service/SystemConfigService.java`
- `service/DictService.java`

### 权限相关

- `config/PermissionInterceptor.java`
- `config/RequirePermission.java`
- `config/RequireAnyPermission.java`

## 当前前端重点页面

- `views/Department.vue`
- `views/UserManage.vue`
- `views/RoleManage.vue`
- `views/OperationLog.vue`
- `views/SystemConfig.vue`
- `views/DictManage.vue`

公共能力主要在：

- `components/Layout.vue`
- `api/index.js`
- `utils/permission.*`
- `utils/dict.*`
- `utils/systemConfig.*`

## 当前真实开发重点

当前不再继续把精力投入在项目上传、项目删除、Prompt 配置等早期业务功能扩展上。

当前真实口径是：

- 基础平台已基本收口
- 后续更适合按新业务模块接入系统配置、字典、权限、日志等底座

## 新模块接入时建议先看

1. [项目现状与交接](../项目现状与交接.md)
2. [基础模块实施计划](../基础模块实施计划.md)
3. [权限管理系统设计](../权限管理系统设计.md)
4. [UI设计规范](../UI设计规范.md)
5. [DEVELOPMENT.md](../../DEVELOPMENT.md)

## 本地运行产物边界

以下内容通常不作为源码管理对象理解：

- `.tools/`
- `backend/.local-db/`
- `backend/*.log`
- `backend/*.gz`
- `backend/*.pdf`
- `backend/*.xlsx`
- `uploads/`
- 各类本地 SQL 备份、临时文件、远程工具残留文件

这类内容应优先通过 `.gitignore` 隔离，而不是混进日常开发变更里。

## 当前仍保留的历史能力

项目里仍保留早期业务代码，如：

- 项目管理
- 文件上传与下载
- PDF 文本提取
- Prompt 配置

这些模块当前不作为基础平台开发重点，但代码仍在仓库中，可在后续业务重启时继续梳理。
