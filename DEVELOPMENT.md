# DEVELOPMENT.md - 招投标系统开发规范

_本文档记录系统开发中确认的重要决策点，作为后续开发的依据。_

---

## 1. 当前开发重心

当前阶段优先建设基础平台层能力，不扩展项目管理、上传、项目删除等业务功能。

基础平台层实施顺序：

1. 部门管理
2. 用户管理
3. 角色权限管理
4. 操作日志
5. 系统配置
6. 字典管理
7. 定时任务

当前已完成并验收的基础平台能力：

- 部门树、部门搜索、部门新增/编辑/删除前影响提示
- 禁止删除存在子部门或用户的部门
- 部门负责人设置、负责人候选、负责人调部门或禁用后自动清空
- 部门内用户搜索、新增、编辑、批量启用/禁用、调部门、转未分配
- `dept_id = NULL` 可落库为未分配，`manager_user_id = NULL` 可落库为清空负责人
- 用户管理 V2 全局页已支持多角色分配、密码策略、登录安全展示、DataScope 和字段读写权限
- 角色权限已支持角色 CRUD、Feature/DataScope/Field 配置、真实权限拦截和权限变更即时刷新
- 操作日志已支持基础平台关键写操作、权限拒绝审计、登录安全审计、字段权限过滤和 `/system/log` 查询页
- 系统配置已支持 key-value 配置、公开基础配置读取，并驱动企业名称、系统标题、密码策略、会话超时、分页和侧边栏默认状态
- 系统配置已支持可配置全局用户水印，统一由 Layout 读取并渲染，支持 `{userId}`、`{realName}`、`{username}`、`{deptName}`、`{roleNames}`、`{systemTitle}`、`{dateTime}` 占位符，并支持透明度、字号、旋转角度配置
- 字典管理已支持字典类型/字典项维护，并为操作日志等页面提供下拉选项
- 文件中心、消息中心、待办中心、个人中心、导入导出基础能力已接入基础平台样板和权限体系
- 前端已形成 `frontend/src/design-system` 基础组件库、`frontend/src/components/<domain>` 业务组件分层和亮色 / 暗色主题 token 体系

当前阶段判断：

- 基础平台能力已基本封板，可作为后续业务模块底座
- 现阶段不启动定时任务、文件存储、消息通知、API 密钥、请求限流
- 后续新增需求应优先按“业务模块接入”思路推进，而不是继续无限扩基础平台

下一步建议：

- 前端系统页人工走查
- 业务模块接入前，先评估系统配置、字典、权限接入点
- 继续保持基础平台文档同步，避免口径再次分叉

---

## 2. 本地开发运行方式

项目路径：`/Users/myfile/bid-system`

### 2.1 后端

当前本地后端使用已打包的 Spring Boot jar 和 H2 本地库运行。

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

说明：

- H2 数据库文件：`backend/.local-db/bid`
- `backend/local-schema.sql` 只用于建表、必要种子和兼容性 `ALTER`，不得覆盖已有业务数据
- 本地管理员账号：`admin@bid.com / admin123`

### 2.2 后端局部编译

本地 Maven portable 环境没有完整 JDK 时，优先用 ECJ 做局部编译，并把 class 替换进 jar。

```bash
JAVA="$PWD/.tools/jdk-17.0.18+8-jre/Contents/Home/bin/java"
CP=".tools/backend-classes:$(printf ':%s' .tools/backend-libs/*.jar | cut -c2-)"
rm -rf .tools/backend-compile
mkdir -p .tools/backend-compile
"$JAVA" -jar .tools/ecj-3.42.0.jar -17 -encoding UTF-8 -cp "$CP" -d .tools/backend-compile \
  backend/src/main/java/com/bid/system/entity/Department.java \
  backend/src/main/java/com/bid/system/entity/User.java \
  backend/src/main/java/com/bid/system/controller/DepartmentController.java \
  backend/src/main/java/com/bid/system/controller/SystemUserController.java \
  backend/src/main/java/com/bid/system/service/DepartmentService.java \
  backend/src/main/java/com/bid/system/service/UserService.java
```

替换 jar 时使用 Python `zipfile` 重写 `BOOT-INF/classes/*.class`，不要直接 append jar，避免 duplicate class entry。

### 2.3 前端

前端技术栈为 Vue 3 + Vite + Arco Design。

```bash
cd frontend
PATH="/Users/myfile/bid-system/.tools/node/bin:$PATH" npm run build
```

当前本地前端通过 Python SPA proxy 暴露在 `http://localhost:3000`，静态服务 `frontend/dist`，并把 `/api` 代理到 `http://localhost:8080`。

---

## 3. 前端页面开发规范

> 新增或修改前端后台页面前，必须先阅读本节、`docs/后台页面开发规范.md`、`docs/UI组件库规范.md`、`docs/业务组件复用规范.md` 和 `docs/主题与暗黑模式规范.md`。

当前前端后台布局已经统一为 `Layout.vue` 工作台模式，新增页面不得再自行实现旧式页面外壳。

### 3.0 当前前端基础设施

当前前端已形成以下可复用成果：

- 基础 UI 组件库：`frontend/src/design-system`
- 业务组件目录：`frontend/src/components/user`、`frontend/src/components/department`、`frontend/src/components/message`
- 样板页：用户管理、部门管理、角色权限、操作日志、系统配置、字典管理
- 主题能力：`frontend/src/utils/theme.js` + `frontend/src/design-system/tokens`

后续开发必须先复用这些成果。只使用少量 `Ds` 组件但重新拼一套页面结构，不符合当前规范。

### 3.1 路由与顶部标题

- 新增后台页面必须挂在主 `Layout` 子路由下。
- 路由必须配置 `meta.title`，全局顶部标题和标签页标题统一读取该字段。
- `PageHeader.vue` 目前仅保留兼容占位，不要在新页面中依赖它渲染标题、面包屑、用户信息或退出按钮。
- 顶部由 `Layout.vue` 统一管理：左侧栏、收缩按钮、页面标题、用户信息、退出按钮、多标签页。

### 3.2 页面布局

- 页面主体默认全宽自适应，禁止在后台页面默认使用 `max-width + margin: 0 auto`。
- 页面主体统一使用 `padding: 24px`。
- 页面背景使用 `--ds-color-bg-page / --ds-color-bg-layout`，不要单页自定义大面积背景。
- 业务页面只负责内容区，不要重复实现顶部栏、用户信息、退出按钮。
- 搜索筛选区统一使用 `DsFilterBar`，不要单页散写筛选区样式。
- 不要写行内宽度样式，输入框、选择器、日期选择器宽度用 class 管理。

### 3.3 表格与分页

- 列表/管理页表格统一使用 `DsDataTable`。
- 分页统一使用 `DsPagination`。
- 表格右上角操作区使用 `DsActionBar`。
- 批量操作使用 `DsBatchActions`。
- 用户信息、角色、状态、安全状态分别使用 `DsUserCell / DsRoleTag / DsStatusTag / DsSecurityStatus`。
- 行内操作统一外露主操作，次级或危险动作进入 `更多`。

### 3.4 页面样板

新增或修改页面前必须先选择样板：

- 标准列表管理页：对齐 `frontend/src/views/UserManage.vue`
- 左树右表管理页：对齐 `frontend/src/views/Department.vue`
- 复杂权限 / 配置页：对齐 `frontend/src/views/RoleManage.vue`
- 日志审计页：对齐 `frontend/src/views/OperationLog.vue`
- 配置类页面：对齐 `SystemConfig.vue`、`DictManage.vue`

### 3.5 业务组件复用

必须复用的业务组件：

- 新增 / 编辑用户：`frontend/src/components/user/UserFormModal.vue`
- 部门管理左侧树：`frontend/src/components/department/DepartmentTreePanel.vue`
- 消息收件人 / 抄送人 / 待办处理人：`frontend/src/components/message/MessageRecipientPicker.vue`
- 消息 / 待办附件：`frontend/src/components/message/MessageAttachmentUploader.vue`

同一业务片段第二次出现时，优先抽到 `frontend/src/components/<domain>`。

### 3.6 主题与暗黑模式

- 主题状态统一使用 `frontend/src/utils/theme.js`。
- 颜色、背景、边框、阴影必须使用 `frontend/src/design-system/tokens` 中的 `--ds-*` token。
- 不允许业务页新增硬编码白底、浅灰、文字色、边框色。
- 不允许在业务页自行实现暗黑模式。
- ECharts、Canvas、SVG data URL 等非 CSS 场景必须读取 CSS token。

### 3.7 新页面检查清单

新增页面提交前至少检查：

- 是否配置了 `meta.title`
- 是否出现在 `Layout.vue` 菜单中，并受角色权限控制
- 是否没有独立顶部白条或旧式 PageHeader
- 是否没有默认居中限宽
- 是否选择并对齐了对应样板页
- 是否复用了 `DsDataTable / DsPagination / DsModalForm / DsFilterBar`
- 是否复用了已有业务组件
- 是否没有新增硬编码 UI 颜色
- 是否同时检查了亮色和暗色
- 构建是否通过：`npm run build`

---

## 4. 上传与 PDF 文本提取现状

当前实现已经从“MiniMax 直发文件”调整为“本地提取文本后调用大模型”的流程。

```text
前端上传文件
  -> 后端保存上传文件
  -> 后端调用 python3 + PyMuPDF 提取 PDF 文本
  -> 服务端组装提示词与提取文本
  -> 调用模型服务抽取字段
  -> 解析结果并写入数据库
  -> 必要字段生成 Excel 附件
```

约定：

- Python PDF 依赖位于 `.tools/python-packages`
- 本地 PDF 提取脚本位于 `backend/scripts/`
- 上传和项目解析属于业务层功能；当前基础平台阶段不主动扩展该模块

---

## 5. 开发调试规范

- 工作树可能包含历史改动、本地数据和运行产物，开发时只改当前任务相关文件
- 不随意 `git checkout`、`git reset --hard` 或回滚未确认的文件
- 重要改动需同步更新对应规划/设计文档
- `System.err.println`、临时日志和一次性调试代码使用后立即删除
- `local-schema.sql` 可做兼容性补表补字段，不做数据重置

---

## 6. 版本历史

| 日期 | 变更内容 |
|------|----------|
| 2026-03-30 | 早期确认流程：上传文件直发 MiniMax、100M 文件限制、一次文件一次 API |
| 2026-04-14 | 更新当前真实流程：本地 PyMuPDF 提取文本；明确基础平台阶段优先级与本地运行方式 |
| 2026-04-14 | 确认前端后台页面规范：统一 Layout、全宽内容区、顶部标题、多标签页、标准表格与分页 |
| 2026-04-20 | 同步基础平台进度：系统配置、字典管理、真实权限接入、DataScope、字段权限和权限拒绝审计已完成第一版 |
| 2026-04-20 | 操作日志补齐登录安全审计：登录失败、账号锁定、退出登录 |
| 2026-05-14 | 同步前端复用规范：新增后台页面开发、业务组件复用、主题与暗黑模式规范；前端构建统一使用 `.tools/node/bin` |
