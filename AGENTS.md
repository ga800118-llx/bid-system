# 开发代理约束

## 基础平台 UI 组件库优先级

后续开发任何后台页面时，必须优先复用：

- `frontend/src/design-system`

## 页面样板优先级

后续修改或新增后台页面前，必须先识别页面类型和现有样板页，不允许每个页面各做各的：

- 标准列表管理页：优先对齐 `frontend/src/views/UserManage.vue`
- 左树右表管理页：优先对齐 `frontend/src/views/Department.vue`
- 复杂权限 / 配置页：优先对齐 `frontend/src/views/RoleManage.vue`
- 日志审计页：优先对齐 `frontend/src/views/OperationLog.vue`
- 配置类页面：优先对齐系统配置、字典管理现有页面

同类页面必须优先复用样板页的页面宽度、内容区节奏、卡片层级、筛选区、表格、分页、弹窗表单和操作区组织方式。只复用 `Ds` 基础组件但重新拼一套页面结构，视为不符合规范。

## 业务组件复用优先级

遇到跨页面重复业务片段时，必须优先抽取或复用业务组件，不允许在不同页面复制一套相似实现：

- 用户列表、用户单元格、用户状态、用户角色展示，应优先复用用户管理页已确认样式
- 新增 / 编辑用户表单如在多个页面出现，应抽取为业务组件后共用
- 新增 / 编辑用户表单必须优先使用 `frontend/src/components/user/UserFormModal.vue`
- 部门树、部门节点、部门负责人选择，如在多个页面出现，应抽取为业务组件后共用
- 部门管理左侧组织树必须优先使用 `frontend/src/components/department/DepartmentTreePanel.vue`
- 导入结果、删除确认、批量操作等通用业务片段，应统一成可复用组件或固定样板

组件分层规则：

- `frontend/src/design-system` 只沉淀基础 UI、表单、选择器、表格、分页、标签、弹窗壳层
- 跨页面业务能力不得硬塞进 design-system，应放入业务组件目录，例如 `frontend/src/components/user`、`frontend/src/components/department`
- 业务页面只负责组合组件、传入数据和调用接口，不重复实现已有业务片段
- 消息收件人 / 抄送人选择、消息附件上传等跨页面业务片段应放入 `frontend/src/components/message`

## 开发前复用检查

修改任何页面前，必须先做复用检查：

1. 识别页面类型和对应样板页。
2. 检查是否已有 design-system 基础组件可用。
3. 检查是否已有业务组件可复用。
4. 如果同类 UI 或同类业务能力第二次出现，优先抽取为组件。
5. 如果现有组件能力不足，优先扩展组件，不允许在业务页面复制一套。

满足以下任一条件，应优先考虑抽取：

- 同一 UI 结构第二次出现
- 同一业务表单第二次出现
- 同一表格列和操作第二次出现
- 同一弹窗确认逻辑第二次出现
- 同一筛选组合第二次出现
- 同一状态展示规则在两个页面出现
- 单个页面区域超过 150 行且有清晰子区域

每次交付必须说明：

- 本次复用了哪些组件
- 本次新增了哪些可复用组件
- 哪些重复点暂未抽取，以及原因

## 文档同步要求

后续开发如果新增页面类型、抽取业务组件、扩展 design-system 组件、调整主题 token、改变页面样板或修改基础平台交互口径，必须同步更新对应文档：

- 页面结构与样板：`docs/后台页面开发规范.md`
- 基础 UI 组件：`docs/UI组件库规范.md`
- 跨页面业务组件：`docs/业务组件复用规范.md`
- 主题、暗黑模式、颜色 token：`docs/主题与暗黑模式规范.md`
- 本地运行、构建、验证方式：`DEVELOPMENT.md`

禁止只改代码不更新规范，导致后续开发继续沿用旧口径。

## 表格统一规则

所有后台列表页的表格内样式必须统一，不允许页面各自定义一套表格风格：

- 表格壳层必须优先使用 `DsDataTable`
- 分页必须使用 `DsPagination`
- 表头高度、背景、字体、分割线、行高、hover、selected、多选框必须保持一致
- 用户信息必须使用 `DsUserCell`
- 角色展示必须使用 `DsRoleTag`
- 状态展示必须使用 `DsStatusTag`
- 安全状态必须使用 `DsSecurityStatus`
- 行内操作统一为 `编辑 / 更多`，次级或危险动作放入更多菜单
- 特殊列只能扩展单元格内容，不允许改变整张表格的基础视觉

禁止事项：

- 禁止在业务页面直接覆盖表格行高、hover、表头、checkbox、分页样式
- 禁止在业务页面临时拼接表格内标签、用户单元格、行内操作按钮
- 如果现有表格组件能力不足，必须先扩展 `DsDataTable` 或对应单元格组件

## 内页表单统一规则

所有内页、弹窗、抽屉、配置表单必须统一使用表单规范：

- 弹窗壳层必须使用 `DsModalForm`
- 表单分组必须使用 `DsFormSection`
- 表单布局必须使用 `DsFormGrid`
- 底部操作必须使用 `DsFormActions`
- 只读字段必须使用 `DsReadonlyField`
- 部门、人员、角色、状态、日期等选择器必须使用对应 `Ds` 组件
- 新增 / 编辑用户必须使用 `UserFormModal`
- 普通文本、手机号、邮箱、身份证号必须使用 `DsInput`，并按字段语义设置 `type`
- 数字、排序、金额类字段必须使用 `DsNumberInput / DsAmountInput`
- 文本长度、格式校验和错误文案优先在 design-system 组件或共享校验工具中实现，业务页面不得重复写一套

禁止事项：

- 禁止业务页面直接拼 `a-modal + a-form` 实现新增 / 编辑表单
- 操作日志详情、系统配置新增 / 编辑、字典类型 / 字典项新增编辑、导入结果和禁用影响确认必须使用 `DsModalForm`
- 操作日志模块 / 动作 / 结果筛选必须使用 `DsDictSelect`，时间范围必须使用 `DsDateRangePicker`
- 系统配置和字典管理中的分组、值类型、颜色、状态选择必须优先使用 `DsDictSelect / DsStatusSelect`
- 禁止业务页面散写部门、角色、状态选择器
- 禁止业务页面直接用 `a-input` 实现手机号、邮箱、身份证号字段
- 禁止业务页面自行编写手机号、邮箱、身份证号正则并绕开 `DsInput`
- 禁止定义了字段长度或格式规范但只写文档、不接入组件和提交校验
- 禁止只为单页写临时弹窗样式
- 如果当前表单组件不满足，必须扩展组件或抽业务组件，不允许单页绕开

## 必须复用的场景

遇到以下场景时，必须使用对应 `Ds` 组件，不允许在业务页面重复实现：

- 部门选择：`DsDeptTreeSelect`
- 人员选择：`DsUserAvatarSelect`
- 角色选择：`DsRoleMultiSelect`
- 字典选择：`DsDictSelect`
- 状态选择：`DsStatusSelect`
- 日期范围：`DsDateRangePicker`
- 页面头部：`DsPageHeader`
- 页面头部操作区：`DsHeaderActions`
- 全局顶部栏：`DsTopBar`
- 统计卡片：`DsStatsCard` / `DsSecuritySummaryCard`
- 列表页筛选区：`DsFilterBar` / `DsAdvancedFilterPanel` / `DsKeywordSearch`
- 状态展示：`DsStatusTag`
- 角色标签：`DsRoleTag`
- 通用标签：`DsTag`
- 安全状态：`DsSecurityStatus`
- 业务详情提示：`DsSecurityPopover`
- 用户头像兜底：`DsUserAvatar`
- 空状态：`DsEmptyState`
- 列表页表格：`DsDataTable`
- 列表页分页：`DsPagination`
- 卡片右上角操作区：`DsActionBar`
- 批量操作：`DsBatchActions`
- 图标按钮：`DsIconButton`
- 用户信息单元格：`DsUserCell`
- 弹窗表单：`DsModalForm`
- 表单分组：`DsFormSection`
- 表单布局：`DsFormGrid`
- 表单操作区：`DsFormActions`
- 只读字段：`DsReadonlyField`
- 富文本编辑：`DsRichTextEditor`

## 禁止事项

- 不允许在业务页面重新实现部门、人员、角色、状态、日期范围筛选器
- 不允许在业务页面重复实现页面头部卡片和统计卡片
- 后续后台管理页如需统计概览，必须优先使用 `DsStatsCard`
- 风险、安全、异常汇总类卡片必须优先使用 `DsSecuritySummaryCard`
- 禁止在业务页面单独拼接统计卡片样式
- 不允许在业务页面单独拼接页面标题和右上角按钮
- 不允许在 Layout 或业务页面单独拼接全局顶部栏
- 后续所有列表页筛选区必须优先使用 `DsFilterBar`
- 关键词搜索必须使用 `DsKeywordSearch`
- 部门筛选必须使用 `DsDeptTreeSelect`
- 状态筛选必须使用 `DsStatusSelect`
- 角色筛选必须使用 `DsRoleMultiSelect`
- 后续所有角色选择必须优先使用简化版 `DsRoleMultiSelect`
- 角色选择中的已选标签必须使用 `DsTag`
- 角色多选超过可视数量时必须使用 `+N` 折叠展示，避免撑高表单或隐藏无提示
- 禁止在角色选择中使用多级勾选框
- 禁止在业务页面直接散写复杂 `a-select` / `a-tree-select` / 筛选布局样式
- 后续所有列表页必须优先使用 `DsDataTable`
- 后续所有表格右上角操作区必须优先使用 `DsActionBar`
- 后续所有批量操作必须使用 `DsBatchActions`
- 后续所有用户信息展示必须使用 `DsUserCell`
- 后续所有角色、状态、安全状态展示必须使用对应 Tag 组件
- 业务详情提示、状态详情提示必须优先使用白色 Popover 类组件
- 禁止在表格中使用黑底大块 Tooltip 展示业务信息
- 禁止在业务页面临时拼接表格头部、批量操作和用户信息单元格
- 不允许在业务页面重复实现用户信息单元格
- 不允许在业务页面临时拼接批量按钮样式
- 不允许在业务页面混用纯文字状态、色块状态和标签状态
- 后续所有新增 / 编辑弹窗必须优先使用 `DsModalForm`
- 后续所有表单分组必须使用 `DsFormSection`
- 后续所有表单两列布局必须使用 `DsFormGrid`
- 后续所有弹窗底部操作区必须使用 `DsFormActions`
- 后续所有只读字段必须使用 `DsReadonlyField`
- 部门管理页新增 / 编辑部门、部门内新增 / 编辑用户、调部门、删除确认、导入结果均必须纳入 `DsModalForm` 表单体系
- 部门负责人选择必须使用 `DsUserAvatarSelect` 并传入本部门正常用户候选项，不允许绕开负责人业务约束拉全局用户
- 部门树节点状态必须使用 `DsStatusTag`，部门内用户列表必须使用 `DsDataTable / DsActionBar / DsBatchActions`
- 文件管理、消息中心、待办中心必须沿用当前基础平台样板，不允许单独设计新风格
- 文件管理列表必须使用 `DsDataTable / DsPagination / FileTypeIcon / DsStatusTag`，文件关联表单必须使用 `DsModalForm`
- 消息中心列表必须使用 `DsDataTable / DsPagination / DsStatusTag`，发送消息表单必须使用 `DsModalForm`
- 消息中心富文本内容必须使用 `DsRichTextEditor`，不得直接散写富文本编辑器或 textarea 代替增强消息正文
- 消息中心指定收件人、抄送人必须使用 `frontend/src/components/message/MessageRecipientPicker`，不得回退为普通用户下拉或旧左右分栏选择器
- `MessageRecipientPicker` 必须保持下拉框式部门人员树：部门节点可选中整棵子树，人员节点可单独多选，最终只向业务接口输出用户 ID 数组
- 消息中心当前隐藏抄送入口；后续重新启用时仍必须复用同一 `MessageRecipientPicker`，不得另做一套
- 消息附件必须使用 `frontend/src/components/message/MessageAttachmentUploader` 并复用文件中心上传和 `system_file_link` 绑定
- 消息中心抄送只做轻量 `CC`，不允许顺手扩展为内部邮件的回复、转发、草稿箱、发件箱、线程、密送等能力
- 待办中心列表必须使用 `DsDataTable / DsPagination / DsStatusTag / DsPriorityStars`，新增待办表单必须使用 `DsModalForm`
- 待办中心新增待办必须复用消息中心三段式弹窗结构：基础信息、处理范围、待办内容
- 待办处理人必须使用 `frontend/src/components/message/MessageRecipientPicker`，不允许回退为单处理人下拉
- 待办内容必须使用 `DsRichTextEditor`，待办附件必须使用 `MessageAttachmentUploader` 并复用文件中心上传和 `system_file_link` 绑定
- 多处理人待办必须通过处理人明细实现个人状态独立流转，不允许用逗号字段或单条状态模拟多人处理
- 文件、消息、待办详情必须使用统一白色详情抽屉样式，禁止回退到旧式散写详情块
- 文件、消息、待办行内操作只外露主操作，预览、下载、标为已读、删除、状态流转等动作必须进入 `更多`
- 禁止部门字段显示原始 id
- 禁止手机号、邮箱、身份证号字段绕开 `DsInput type="phone/email/idCard"`
- 禁止在业务页面直接使用原生 `a-select` 实现角色多选
- 禁止新增 / 编辑表单继续散写临时样式
- 不允许在业务页面直接散写 `a-pagination`
- 不允许把“每页条数”和“页码区”拆成两层分页栏
- 后续所有后台列表页分页必须使用 `DsPagination`
- 禁止每页条数、总数、跳转页在不同页面使用不同布局
- `DsDataTable` 如需分页，默认接入 `DsPagination`
- 不允许在业务页面硬编码基础控件颜色和边框规则
- 不允许为单页需求直接改全局样式绕开 design-system
- 不允许业务页面自行实现暗黑模式或主题切换
- 不允许新增 `#fff`、`#f0f2f5`、`#1d2129`、`#4e5969`、`#86909c` 等硬编码 UI 颜色
- 不允许在主题相关样式中使用 `color-mix(..., white)` 或 `color-mix(..., black)` 作为基准色，应使用 `--ds-color-bg-card / --ds-color-border` 等 token
- ECharts、Canvas、SVG data URL、水印等非 CSS 场景必须通过 `getComputedStyle(document.documentElement)` 读取 `--ds-*` token，不允许写死亮色主题色

## 开发要求

- 优先复用 `design-system/tokens` 中的 CSS 变量
- 优先复用 `design-system/icons` 中的 SVG 组件
- 新增基础组件时，先补到 `frontend/src/design-system`，再在业务页使用
- 后续所有后台页面顶部必须优先复用 `DsTopBar + DsPageHeader + DsHeaderActions`
- 主题状态统一使用 `frontend/src/utils/theme.js`
- 暗黑模式只通过 `frontend/src/design-system/tokens` 扩展，不在业务页面单独写一套暗色样式
- 新增或修改后台页面后，必须同时检查亮色和暗色下的可读性
