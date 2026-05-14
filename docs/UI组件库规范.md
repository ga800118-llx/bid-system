# 基础平台 UI 组件库规范

## 目标

基础平台 UI 组件库用于沉淀后台页面已经确认的表单、下拉、选择器、筛选区和状态展示规范。后续页面开发应优先复用 `frontend/src/design-system`，避免在业务页面重复实现基础控件。

本规范只说明基础 UI 组件库。页面结构看 `docs/后台页面开发规范.md`，跨页面业务组件看 `docs/业务组件复用规范.md`，主题和暗黑模式看 `docs/主题与暗黑模式规范.md`。

## 复用原则

后续页面开发采用“先找样板，再做页面”的规则：

1. 先识别页面类型。
2. 再查找已确认的样板页。
3. 再复用已有 design-system 组件和业务组件。
4. 只有现有组件无法覆盖时，才新增组件。
5. 新增组件后必须补本文档和 `AGENTS.md`。

只使用 `Ds` 基础组件但重新组织一套页面结构，不算完成统一规范。页面宽度、内容区节奏、卡片层级、筛选条、表格、分页、弹窗和操作区也必须对齐同类样板。

## 抽取标准

出现以下情况时，优先抽取或复用组件，而不是在页面内继续复制实现：

- 同一 UI 结构第二次出现
- 同一业务表单第二次出现
- 同一表格列和操作第二次出现
- 同一弹窗确认逻辑第二次出现
- 同一筛选组合第二次出现
- 同一状态展示规则在两个页面出现
- 单个页面区域超过 150 行且有清晰子区域

抽取边界：

- 组件负责展示、交互和基础校验
- 页面负责接口、权限、刷新、路由上下文和业务副作用
- 组件通过 props 接收数据，通过 emits 通知动作
- 不在业务组件里写死接口
- 不为单个页面特例污染公共组件

交付时必须说明本次复用了哪些组件、新增了哪些可复用组件，以及哪些重复点暂未抽取。

## 表格统一规范

所有后台页面表格必须使用统一的表格体系。

基础要求：

- 表格壳层使用 `DsDataTable`
- 分页使用 `DsPagination`
- 表头高度、背景、字体、分割线统一
- 行高、hover、selected、多选框统一
- 表格卡片头部操作区使用 `DsActionBar`
- 批量操作使用 `DsBatchActions`
- 工具按钮使用 `DsIconButton`

单元格要求：

- 用户信息使用 `DsUserCell`
- 角色使用 `DsRoleTag`
- 状态使用 `DsStatusTag`
- 安全状态使用 `DsSecurityStatus`
- 空状态使用 `DsEmptyState`
- 行内操作统一为 `编辑 / 更多`
- 次级动作、危险动作放入更多菜单

禁止事项：

- 不允许业务页面直接覆盖表格行高、表头、hover、多选框、分页样式
- 不允许业务页面临时拼接用户单元格、状态标签、角色标签
- 不允许一个页面使用文字状态，另一个页面使用标签状态
- 如果表格组件能力不足，优先扩展 `DsDataTable` 或对应单元格组件

特殊列处理：

- 允许业务页面定义特殊列内容
- 特殊列只能控制该列展示，不得改变整张表格基础视觉
- 特殊列如第二次出现，应抽成业务单元格组件

## 内页表单统一规范

所有内页、弹窗、抽屉、配置表单必须统一使用表单体系。

基础要求：

- 弹窗壳层使用 `DsModalForm`
- 表单分组使用 `DsFormSection`
- 两列布局使用 `DsFormGrid`
- 底部操作使用 `DsFormActions`
- 只读字段使用 `DsReadonlyField`
- 输入框、密码、数字、开关、文本域使用 `DsInput / DsPasswordInput / DsNumberInput / DsSwitch / DsTextarea`
- 部门、人员、角色、状态、字典、日期范围使用对应 `Ds` 选择器

字段校验执行规则：

- 普通文本使用 `DsInput type="text"`，必须按业务字段设置 `maxLength`
- 手机号使用 `DsInput type="phone"`，统一校验 11 位大陆手机号，错误文案为「手机号格式不正确」
- 邮箱使用 `DsInput type="email"`，统一校验邮箱格式，错误文案为「邮箱格式不正确」
- 身份证号使用 `DsInput type="idCard"`，统一校验 18 位身份证号和校验位，错误文案为「身份证格式不正确」
- 多行文本使用 `DsTextarea`，如有长度限制必须设置 `maxLength`
- 数字字段使用 `DsNumberInput`，金额字段使用 `DsAmountInput`，不得用普通文本框模拟数字输入
- 组件展示层和提交前校验必须复用同一套 design-system 校验规则，避免“提示一套、保存一套”
- 已脱敏回显值如包含 `*`，允许通过格式校验，避免未修改的历史脱敏数据被错误拦截

业务表单要求：

- 新增 / 编辑用户必须使用 `frontend/src/components/user/UserFormModal.vue`
- 部门组织树必须使用 `frontend/src/components/department/DepartmentTreePanel.vue`
- 后续新增跨页面业务表单时，必须放入 `frontend/src/components/<domain>` 并补 README

禁止事项：

- 禁止业务页面直接拼 `a-modal + a-form`
- 禁止业务页面直接散写复杂 `a-select / a-tree-select / a-radio-group`
- 禁止业务页面直接使用 `a-input` 实现手机号、邮箱、身份证号
- 禁止业务页面自行复制手机号、邮箱、身份证号正则
- 禁止只在文档定义字段长度和格式，不接入 `DsInput / DsNumberInput / DsTextarea` 和提交校验
- 禁止单页写临时弹窗、抽屉、表单样式
- 如果现有组件不满足，必须扩展组件或抽业务组件

## 样板页映射

- 标准列表管理页：
  - 样板：用户管理
  - 适用：用户、文件、配置、字典、消息、待办等标准列表
  - 必须复用：页面宽度、`DsPageHeader`、统计卡片、`DsFilterBar`、`DsDataTable`、`DsActionBar`、`DsPagination`、`DsModalForm`
- 左树右表管理页：
  - 样板：部门管理
  - 适用：组织、分类、目录、区域等树形对象加明细列表
  - 必须复用：左侧树卡片、选中态、右侧列表、批量操作、树节点操作样式
  - 如果右侧展示用户列表，必须继续复用用户管理中的用户列表样式和用户表单规范
  - 部门组织树已沉淀为 `frontend/src/components/department/DepartmentTreePanel.vue`，后续组织树优先复用该业务组件
- 复杂配置页：
  - 样板：角色权限
  - 适用：权限配置、规则配置、策略配置、字段配置等多维配置页
  - 必须复用：左侧对象列表、右侧配置卡片、Tab 配置、模块卡片、保存操作区
- 日志审计页：
  - 样板：操作日志
  - 适用：审计日志、登录日志、任务记录、接口调用记录
  - 必须复用：`DsPageHeader`、`DsFilterBar`、`DsKeywordSearch`、`DsDictSelect`、`DsDateRangePicker`、`DsDataTable`、`DsPagination`、`DsModalForm`
  - 日志详情必须使用白色 `DsModalForm` 信息弹窗，不使用黑底 Tooltip 或临时 `a-modal`
- 配置类页面：
  - 样板：系统配置、字典管理
  - 适用：键值配置、分类配置、状态配置、基础参数维护
  - 必须复用：配置列表、状态标签、`DsModalForm` 编辑弹窗、`DsFormSection`、`DsFormGrid`、`DsFormActions`、`DsDictSelect`、`DsStatusSelect`
  - 禁止在配置类页面继续散写 `a-modal + a-form + a-input/a-select`

## 组件分层

### design-system 组件

`frontend/src/design-system` 只负责基础 UI 能力：

- 页面头部、统计卡片、筛选条、表格、分页
- 输入框、选择器、日期、开关、弹窗表单
- 状态标签、角色标签、用户头像、空状态
- 图标按钮、批量操作、业务详情浮层

### 业务组件

跨页面重复的业务片段，应抽成业务组件，不应在多个页面复制实现：

- 用户列表、用户表单、用户行操作
  - 新增 / 编辑用户表单：`frontend/src/components/user/UserFormModal.vue`
- 部门树、部门节点、部门负责人设置
  - 部门管理组织树：`frontend/src/components/department/DepartmentTreePanel.vue`
- 导入结果、删除确认、批量调整部门
- 其他在两个以上页面复用的业务区域

建议目录：

- `frontend/src/components/user`
- `frontend/src/components/department`
- `frontend/src/components/common`

业务组件可以组合 design-system 组件，但不能在内部绕开设计系统重新散写原生控件样式。

## 组件清单

### Tokens

- `tokens/colors.css`
- `tokens/radius.css`
- `tokens/shadow.css`
- `tokens/spacing.css`
- `tokens/form.css`

Token 使用要求：

- 所有基础组件的颜色、背景、边框、阴影必须使用 `--ds-*` token。
- 暗色主题只通过 token 覆盖实现，不在组件内写单独暗色分支。
- 新增 token 后必须同时补亮色和暗色值。
- 需要混色时，使用 `var(--ds-color-bg-card)`、`var(--ds-color-border)` 等 token 作为基准，不直接使用 `white / black`。

### Icons

- `DepartmentIcon.vue`
- `UserAvatarFallback.vue`
- `RoleIcon.vue`
- `StatusIcon.vue`
- `CalendarIcon.vue`
- `CheckIcon.vue`
- `EmptyStateIcon.vue`
- `FileTypeIcon.vue`

### Form

- `DsModalForm.vue`
- `DsFormSection.vue`
- `DsFormGrid.vue`
- `DsFormActions.vue`
- `DsReadonlyField.vue`
- `DsInput.vue`
- `DsPasswordInput.vue`
- `DsNumberInput.vue`
- `DsAmountInput.vue`
- `DsTextarea.vue`
- `DsSwitch.vue`
- `DsCheckboxGroup.vue`
- `DsRadioGroup.vue`
- `DsDatePicker.vue`
- `DsDateRangePicker.vue`

### Selectors

- `DsDeptTreeSelect.vue`
- `DsUserAvatarSelect.vue`
- `DsRoleMultiSelect.vue`
- `DsDictSelect.vue`
- `DsStatusSelect.vue`
- `DsCascaderSelect.vue`
- `DsFileCategorySelect.vue`

### Filter

- `DsFilterBar.vue`
- `DsAdvancedFilterPanel.vue`
- `DsKeywordSearch.vue`

### Display

- `DsPageHeader.vue`
- `DsHeaderActions.vue`
- `DsTopBar.vue`
- `DsStatsCard.vue`
- `DsSecuritySummaryCard.vue`
- `DsActionBar.vue`
- `DsBatchActions.vue`
- `DsIconButton.vue`
- `DsDataTable.vue`
- `DsUserCell.vue`
- `DsPagination.vue`
- `DsStatusTag.vue`
- `DsRoleTag.vue`
- `DsTag.vue`
- `DsSecurityPopover.vue`
- `DsSecurityStatus.vue`
- `DsUserAvatar.vue`
- `DsPriorityStars.vue`
- `DsEmptyState.vue`

### Examples

- `FormComponentShowcase.vue`
- `DropdownComponentShowcase.vue`
- `UserFormExample.vue`
- `FilterBarExample.vue`

## 使用场景

### 表单类

- 账号、姓名、金额、备注、状态切换
- 新增、编辑、审批、配置项录入
- 新增 / 编辑弹窗表单统一使用 `DsModalForm`
- 表单分组统一使用 `DsFormSection`
- 两列布局统一使用 `DsFormGrid`
- 弹窗底部按钮统一使用 `DsFormActions`
- 只读字段统一使用 `DsReadonlyField`

### 业务选择器

- `DsDeptTreeSelect`
  - 所属部门、部门筛选、调整部门
- `DsUserAvatarSelect`
  - 负责人、处理人、接收人、审批人
- `DsRoleMultiSelect`
  - 分配角色、角色筛选、关联角色
- `DsDictSelect`
  - 类型、分类、模块、动作、优先级
- `DsStatusSelect`
  - 启用/禁用、状态类筛选
- `DsDateRangePicker`
  - 创建时间、更新时间、统计周期

### 展示类

- `DsActionBar`
  - 卡片右上角操作区，统一选择反馈、批量操作、工具按钮层级
- `DsPageHeader`
  - 页面级标题区，统一面包屑、标题、说明和右侧主次操作
- `DsHeaderActions`
  - 页面头部右上角操作区，统一“更多工具 + 主操作按钮”结构
- `DsTopBar`
  - 后台全局顶部栏，统一品牌、通知、用户菜单和退出操作
- `DsStatsCard`
  - 标准统计卡片，统一图标、标题、大数字和辅助说明
- `DsSecuritySummaryCard`
  - 安全提醒、风险摘要等多行提醒卡片
- `DsBatchActions`
  - 批量启用、批量禁用、批量调整部门等操作
- `DsIconButton`
  - 刷新、列设置、密度切换等图标按钮
- `DsDataTable`
  - 后台列表页统一表格骨架、分页和空状态
- `DsUserCell`
  - 用户头像 + 姓名 + 账号的标准单元格
- `DsPagination`
  - 后台列表页统一分页
- `DsStatusTag`
  - 状态色统一
- `DsRoleTag`
  - 角色标签统一
- `DsSecurityStatus`
  - 登录安全、失败次数、锁定状态统一展示
- `DsSecurityPopover`
  - 登录安全、状态详情等业务信息浮层
- `DsUserAvatar`
  - 列表、下拉、详情页统一头像兜底
- `DsEmptyState`
  - 空列表、空结果、未配置内容

## 视觉规则

- 白底卡片
- 6px 到 8px 圆角
- 细边框
- 轻阴影
- 主色使用 design-system 变量
- hover 使用浅蓝灰背景
- selected 使用浅蓝底、蓝色文字、对勾状态
- 表单控件高度统一
- 必填星号统一使用危险色
- 错误提示统一 12px 红色
- 禁用态统一灰化
- 所有颜色必须来自 CSS 变量

## 表单弹窗规则

### DsModalForm

- 新增、编辑、重置密码、调整部门、分配角色等弹窗优先使用 `DsModalForm`
- 标准宽度建议 720px 到 760px
- 标题区包含：
  - 标题
  - 说明文案
  - 关闭按钮
- 内容区默认 24px padding
- 底部操作区高度约 64px，右对齐
- 弹窗保持白底、12px 圆角、弱边框和柔和阴影
- 遮罩保持轻量，不做厚重黑色压暗

### DsFormSection

- 表单信息按业务含义分组
- 分组标题 14px / 600
- 分组说明可选，使用 12px 弱文本
- 分组之间保持 24px 左右间距

### DsFormGrid

- 默认两列布局
- 列间距 20px，行间距 16px
- 窄屏自动回落单列
- 单字段场景可使用单列模式

### DsFormActions

- 弹窗底部统一使用 `取消 / 保存`
- 按钮高度 36px
- 保存按钮使用主色
- 取消按钮使用轻按钮
- loading 状态不改变布局

### DsReadonlyField

- 账号、编码、系统内置标识等不可编辑字段统一使用 `DsReadonlyField`
- 只读字段使用浅灰底、深色文字和细边框
- 禁止用粗重灰块表达只读状态

### 编辑类弹窗表单

- 编辑类弹窗必须按业务含义分组，不直接平铺一整张表单
- 部门选择必须使用 `DsDeptTreeSelect`，不得显示原始部门 id
- 角色选择必须使用 `DsRoleMultiSelect`，不得在业务页直接散写原生多选
- 状态选择必须使用 `DsStatusSelect` 或组件库内统一状态控件
- 保存时仍提交后端需要的 id / roleIds / status，不改变接口语义

## 展开态规则

- 选择器下拉项统一使用白底和清晰 hover 态
- 已选项使用浅蓝底和蓝色文字
- 树形选择器保持层级缩进
- 人员选择展示头像 + 姓名 + 次级信息
- 多选角色保持标签化输入和可读的下拉说明
- 日期范围统一提供快捷项：
  - 今天
  - 昨天
  - 近 7 天
  - 近 30 天
  - 本月
  - 上月
  - 今年

## 列表页规则

### DsPageHeader

- 页面头部统一使用 `DsPageHeader`
- 左侧展示：
  - 面包屑
  - 页面标题
  - 说明文案
- 右侧展示页面主操作和次操作
- 不在业务页面重复拼装头部卡片样式

### DsHeaderActions

- 页面头部右上角统一使用 `DsHeaderActions`
- 默认承载：
  - 更多工具
  - 主操作按钮
- 更多工具使用白底轻按钮
- 主操作使用主色按钮
- 按钮高度统一 36px
- 业务页面只负责传入工具项和点击事件，不重复拼接按钮组样式

### DsTopBar

- 后台全局顶部栏统一使用 `DsTopBar`
- 左侧统一展示：
  - 品牌 Logo
  - 平台名称
  - 可选副标题
- 右侧统一展示：
  - 通知入口
  - 用户头像 + 名称 + 下拉菜单
  - 退出按钮
- 顶栏高度建议 64px 到 80px
- 使用浅蓝灰或白色背景，底部使用弱阴影或分割线
- 图标、文字、头像垂直居中对齐
- 业务页面和 Layout 不重复手写顶栏按钮布局

### 顶部层级规则

- 全局顶栏由 `DsTopBar` 承载品牌、通知和用户操作
- 标签页条和关闭当前 / 关闭其他 / 关闭全部操作应放在同一轻量层，不与面包屑混排
- 页面面包屑、标题和说明统一由 `DsPageHeader` 承载
- 关闭当前 / 关闭其他 / 关闭全部等页面级关闭操作，统一复用 `DsHeaderActions`

### DsStatsCard / DsSecuritySummaryCard

- 统计卡片统一使用 `DsStatsCard`
- 多行风险摘要统一使用 `DsSecuritySummaryCard`
- 保持：
  - 统一圆角
  - 统一弱阴影
  - 统一图标尺寸
  - 标题、数字、说明文案层级一致
- 不在业务页面零散实现不同风格的统计卡和安全提醒卡
- 推荐布局：
  - `DsStatsCard`：图标 + 标题 + 大数字 + 辅助说明
  - `DsSecuritySummaryCard`：安全图标 + 标题 + 多行风险项 + 右对齐异常数字
- 统计卡片区默认保持同高、同间距
- 安全提醒类卡片中的 0 值应弱化显示，不与异常值同权重

### DsFilterBar

- 列表页筛选区统一使用 `DsFilterBar`
- 默认结构：
  - 左侧筛选字段
  - 右侧查询 / 重置操作组
  - 顶部可选标题、说明、展开更多入口
- 白底卡片、统一圆角、20px 内边距
- 筛选项默认支持自动换行，但首屏保持一行主筛选优先

### DsKeywordSearch

- 关键词搜索统一使用 `DsKeywordSearch`
- 默认用于：
  - 姓名
  - 账号
  - 手机号
  等关键词检索
- 高度统一 36px
- 支持清空和 Enter 触发查询

### DsDeptTreeSelect

- 部门筛选和部门选择统一使用 `DsDeptTreeSelect`
- 必须保持树形下拉，不允许退回普通 `select`
- 支持：
  - 搜索
  - 全部部门
  - 未分配
  - 展开 / 收起树节点

### DsStatusSelect

- 状态筛选统一使用 `DsStatusSelect`
- 选项必须来自字典或统一状态源
- 启用、禁用使用状态标签或清晰的状态语义展示

### DsRoleMultiSelect

- 角色筛选和角色分配统一使用 `DsRoleMultiSelect`
- 使用单一层级角色列表，不使用多级勾选框
- 下拉项展示角色名称和角色描述
- 支持搜索角色名称、角色描述和角色编码
- 已选角色使用 `DsTag` 标签展示
- 标签内容优先展示 `角色名称 · 角色描述`
- 标签可删除，删除后同步取消选择
- 默认最多展示前 2 个已选标签，超出部分显示 `+N`
- `+N` 悬浮展示完整已选角色列表
- 支持多选和角色说明
- 如果当前接口只支持单选语义，可以用单选兼容模式落地，但组件本身保留多选能力

### DsTag

- 用于输入框内已选项和轻量信息标识
- 角色选择中的已选角色统一使用 `DsTag`
- 默认白底、蓝色文字、蓝色细边框
- hover 使用浅蓝底
- 可关闭标签保持轻量，不使用厚重色块

### DsAdvancedFilterPanel

- “展开更多”筛选统一使用 `DsAdvancedFilterPanel`
- 默认收起
- 更多筛选字段优先放在统一面板中扩展，不直接把主筛选条拉高
- 如果后端参数尚未接入，允许先以 disabled 预留字段展示，不制造假查询

### DsActionBar

- 用于卡片右上角操作区
- 左侧显示已选人数或状态反馈
- 中间放批量操作
- 右侧放刷新、列设置等图标按钮
- 操作区高度 40px，内部按钮高度 32px
- 在列表卡片头部优先使用 `space-between` 结构：
  - 左：选择反馈
  - 中：批量操作
  - 右：工具按钮

### DsBatchActions

- 未选择数据时统一禁用
- 选中数据后激活
- 危险操作使用 danger 风格
- 禁止在业务页面临时拼接一排批量按钮样式

### DsIconButton

- 32px 正方形
- 白底、细边框
- hover 使用浅蓝灰背景和主色边框
- 用于刷新、列设置、密度设置等场景

### DsDataTable

- 白底卡片、弱边框
- 极浅灰表头
- 行 hover 使用浅蓝灰背景
- 选中行使用极淡蓝色背景
- 统一行高 56px 左右
- 单元格 padding 12px 16px
- 只保留横向分割线，不强调竖线
- 分页区作为表格底部区域
- 默认优先搭配 `DsPagination`

### DsUserCell

- 左侧 28px 圆形头像
- 右侧两行：
  - 姓名
  - 账号
- 姓名为主信息，账号为辅助文本
- 姓名建议 14px / 500，账号 12px 弱文本

### DsStatusTag / DsRoleTag / DsSecurityStatus

- 状态和角色使用轻量胶囊标签
- 启用、禁用、锁定、无异常、未分配等都应复用标签组件
- 安全状态优先使用 `DsSecurityStatus`
- 业务页面不再自行写状态色块

### DsSecurityPopover

- 安全状态、业务状态详情优先使用 `DsSecurityPopover`
- 使用白色背景、浅边框、柔和阴影的轻量 Popover
- 不在浅色后台表格中使用黑底大 Tooltip 展示业务详情
- 标题 13px / 600，内容 12px
- 字段名弱化，字段值强调
- 异常值使用危险色，正常值使用成功色或深色

### DsPagination

- 所有列表页分页统一使用 `DsPagination`
- 固定放在表格卡片底部
- 默认作为 `DsDataTable` 的底部区域接入
- 分页栏统一为一行式布局
- 左侧显示 `共 N 条`
- 右侧显示：
  - 每页条数
  - 上一页 / 页码 / 下一页
  - 前往 N 页
- 分页区高度 56px
- 左右 padding 16px 或 20px
- 使用 `flex` 布局，`justify-content: space-between`
- 页码按钮尺寸 32px，圆角 6px
- 当前页使用主色蓝色填充
- 普通页 hover 使用浅蓝底
- 每页条数下拉宽度约 96px
- 跳转输入框宽度约 44px
- total = 0 时仍显示 `共 0 条`，页码按钮禁用
- 窄屏下可隐藏跳转页

### DsDataTable 与 DsPagination 组合

- 列表页如需分页，优先通过 `DsDataTable + pagination props` 组合使用
- `DsDataTable` 默认在表格底部渲染 `DsPagination`
- 表格主体和分页区应视为一个整体：
  - 表格顶部圆角
  - 分页底部圆角
  - 中间不留额外断层空隙

## 标准列表页样板

### 用户管理页样板地位

- `/system/user` 当前作为基础平台后台列表页样板
- 后续部门管理、角色权限、操作日志、系统配置、字典管理、文件中心等系统页，在结构和组件选型上应优先参考该页
- 该页的作用不是要求视觉逐像素复制，而是沉淀统一的信息层级、组件组合方式和交互口径

### 角色权限页样板地位

- `/system/role` 当前作为基础平台复杂配置页样板
- 该页重点验证“左侧对象列表 + 右侧配置面板 + 分区式权限配置 + design-system 弹窗表单”模式
- 后续如果出现字典规则配置、流程节点配置、模板规则配置等复杂配置页，应优先参考该页

### 系统管理页收口规则

- `/system/department`、`/system/log`、`/system/config`、`/system/dict` 已按基础平台页面样板收口
- 单表列表页优先使用：
  - `DsPageHeader`
  - `DsFilterBar`
  - `DsKeywordSearch`
  - `DsDataTable`
  - `DsPagination`
- 双栏管理页优先使用：
  - 顶部 `DsPageHeader`
  - 左侧对象选择卡片
  - 右侧明细列表卡片
  - 两侧列表均优先接入 `DsDataTable`
- 页面级导入、导出、新增等动作优先放入 `DsHeaderActions`
- 分页不得再使用页面内散写 `a-pagination`
- 列表空状态不得直接使用裸 `a-empty`，优先使用 `DsEmptyState`

### 用户管理页标准结构

- 顶部全局层级：
  - `DsTopBar`
  - 轻量标签页条
  - 页面标题卡 `DsPageHeader`
- 内容主体层级：
  - 统计卡片区：`DsStatsCard` / `DsSecuritySummaryCard`
  - 筛选查询区：`DsFilterBar`
  - 列表区：`DsDataTable`
  - 分页区：`DsPagination`
- 弹窗表单层级：
  - `DsModalForm`
  - `DsFormSection`
  - `DsFormGrid`
  - `DsFormActions`

### 用户管理页已验证的通用模式

- 页面头部：
  - 面包屑、标题、说明放入 `DsPageHeader`
  - 页面主操作和更多工具使用 `DsHeaderActions`
- 统计卡片：
  - 普通统计统一用 `DsStatsCard`
  - 风险摘要统一用 `DsSecuritySummaryCard`
- 筛选查询：
  - 关键词统一使用 `DsKeywordSearch`
  - 部门筛选统一使用 `DsDeptTreeSelect`
  - 状态筛选统一使用 `DsStatusSelect`
  - 角色筛选统一使用 `DsRoleMultiSelect`
- 列表头部：
  - 左侧标题 + 说明
  - 右侧统一使用 `DsActionBar`
  - 批量操作统一使用 `DsBatchActions`
  - 工具按钮统一使用 `DsIconButton`
- 表格单元格：
  - 用户信息统一使用 `DsUserCell`
  - 角色统一使用 `DsRoleTag`
  - 状态统一使用 `DsStatusTag`
  - 安全状态统一使用 `DsSecurityStatus`
- 行内操作：
  - 外露“编辑 / 更多”
  - 低频或危险操作进入“更多”菜单
- 分页：
  - 统一使用 `DsPagination`
  - 禁止拆成两层分页栏
- 编辑弹窗：
  - 账号只读使用 `DsReadonlyField`
  - 部门字段必须显示名称，不得显示原始 id
  - 角色选择使用扁平多选 `DsRoleMultiSelect`

### 角色权限页已验证的通用模式

- 页面结构：
  - 页面标题卡 + 摘要卡 + 筛选卡 + 双栏主体区
- 左栏：
  - 对象列表卡片
  - 支持关键词与状态筛选
  - 复杂配置页可使用 250px 到 280px 的紧凑对象选择列表，左栏固定宽度，右栏自适应
  - 当前选中对象必须有浅蓝背景和左侧高亮条
  - 对象主标题使用 14px / 500，编码、人数等辅助信息使用 12px / 400 弱文本
  - 内置状态、用户数量等摘要信息优先用小标签展示，不直接堆长文本
- 右栏：
  - 配置面板卡片
  - 顶部显示当前配置对象、状态、说明与保存操作
  - 使用 Tab 分离不同配置维度
- 配置项组织：
  - 功能权限按模块分组，每个模块使用白色卡片、10px 到 12px 圆角、弱阴影和 20px 到 24px 模块间距
  - 模块标题使用 14px 到 16px / 600，模块说明使用 12px 到 14px 弱文本
  - 复选框横向排列，控件高度 32px 到 36px，文字不换行，间距保持 16px 到 20px
  - 文字过长时单行省略，必要时用轻量白色 Popover 或 Tooltip 展示完整内容
  - 全选 / 清空按钮放在模块卡片顶部右侧
  - 保存使用主按钮，刷新使用轻按钮，操作按钮间距保持 12px 到 16px
  - 数据范围使用轻量配置表格
  - 字段权限按模块分组，并明确可读 / 可写关系
- 弹窗表单：
  - 新增 / 编辑角色统一使用 `DsModalForm`
  - 角色编码只读场景使用 `DsReadonlyField`

### 部门管理页已验证的通用模式

- 页面结构：
  - 页面标题统一使用 `DsPageHeader + DsHeaderActions`
  - 部门、用户、负责人、未分配等概览统一使用 `DsStatsCard`
  - 左侧组织树卡片固定宽度，右侧用户列表自适应
- 部门树：
  - 搜索使用 `DsKeywordSearch`
  - 选中节点使用浅蓝背景和左侧高亮条
  - 节点展示部门名称、负责人、子部门数量，禁用和未分配状态使用 `DsStatusTag`
  - 树节点可保留 Arco Tree / Dropdown 作为基础交互，但视觉必须使用组件库 token，不得写死颜色
- 部门内用户列表：
  - 表格壳层和分页统一使用 `DsDataTable / DsPagination`
  - 批量操作统一使用 `DsActionBar / DsBatchActions / DsIconButton`
  - 角色、状态分别使用 `DsRoleTag / DsStatusTag`
  - 空状态统一使用 `DsEmptyState`
- 部门管理弹窗：
  - 新增 / 编辑部门统一使用 `DsModalForm / DsFormSection / DsFormGrid / DsFormActions`
  - 部门名称使用 `DsInput`，排序使用 `DsNumberInput`
  - 部门负责人使用 `DsUserAvatarSelect`，且必须传入“本部门正常用户”候选项，不允许选择全局任意用户
  - 部门状态使用 `DsStatusSelect`
- 部门内用户弹窗：
  - 新增 / 编辑用户统一使用 `DsModalForm`
  - 账号、姓名、密码使用 `DsInput / DsPasswordInput / DsReadonlyField`
  - 角色使用扁平化 `DsRoleMultiSelect`
  - 状态使用 `DsStatusSelect`
  - 调整部门使用 `DsDeptTreeSelect`，保存时仍提交后端需要的 `deptId`
- 删除与导入结果：
  - 删除确认、导入结果也归入 `DsModalForm` 体系
  - 删除影响提示使用 token 色，不允许在模板里写 inline style 或 HEX 色值

### 文件、消息、待办页面已验证的通用模式

- 页面骨架：
  - 文件管理、消息中心、待办中心统一沿用 `DsPageHeader + DsHeaderActions`
  - 页面主体宽度、区块间距、卡片层级对齐用户管理、系统配置、字典管理样板
  - 不为文件、消息、待办单独设计新的列表、分页、筛选或弹窗体系
- 筛选区：
  - 统一使用 `DsFilterBar`
  - 关键词使用 `DsKeywordSearch`
  - 类型、状态、优先级、阅读状态等使用 `DsDictSelect / DsStatusSelect`
  - 时间范围使用 `DsDateRangePicker`
  - 查询、重置按钮放在筛选区操作槽，保持 36px 高度和统一间距
- 列表与分页：
  - 表格统一使用 `DsDataTable`
  - 分页统一使用 `DsPagination`，不得拆成两行
  - 空状态统一使用 `DsEmptyState`
  - 文件类型图形使用 `FileTypeIcon`
  - 状态、类型、优先级、阅读状态、预览能力统一使用 `DsStatusTag / DsTag`
- 行内操作：
  - 每行只外露一个主操作，通常为 `详情` 或 `查看`
  - 预览、下载、启用 / 禁用、标为已读、删除、开始处理、完成、重新打开等次级动作放入 `更多`
  - 危险动作在更多菜单中使用 danger 语义，不在行内平铺
- 详情与表单：
  - 文件详情、消息详情、待办详情使用统一白色详情抽屉样式
  - 消息发送、新增待办、新增文件关联必须使用 `DsModalForm`
  - 表单分组使用 `DsFormSection`，两列布局使用 `DsFormGrid`，底部按钮使用 `DsFormActions`
  - 人员选择使用 `DsUserAvatarSelect`，文件分类使用 `DsFileCategorySelect`，日期使用 `DsDatePicker / DsDateRangePicker`
- 模块语义：
  - 文件管理可突出文件图标、预览能力、文件大小、关联信息
  - 消息中心可突出未读状态、消息类型、正文摘要和前往处理
  - 待办中心可突出优先级、逾期、截止时间和状态流转
  - 模块语义只能扩展单元格内容，不得改变基础表格视觉规则

### 允许个性化，禁止偏离的部分

- 允许个性化的部分：
  - 页面标题文案
  - 统计卡片数量
  - 筛选字段组合
  - 表格列内容
  - 行内更多菜单项
- 禁止偏离的部分：
  - 自行重写顶部结构
  - 自行拼接筛选条和批量操作区
  - 重新实现用户单元格、状态标签、角色标签
  - 重新实现分页结构
  - 在业务页直接散写原生多选、树选、状态选择器

## 行内操作规范

- 每行最多外露两个操作
  - 常规情况保留 `编辑`
  - 次级或危险动作放入 `更多`
- 行内操作统一使用文字按钮
- hover 使用浅蓝灰背景
- 不在一行平铺多个风格不一致的按钮
- 分配角色、重置密码、启用 / 禁用等次级动作优先放入 `更多`

## 消息增强组件规范

### DsRichTextEditor

- 用于站内消息、公告、说明类富文本录入。
- 必须输出 HTML，并由业务接口显式提交 `contentType = HTML`。
- 支持加粗、斜体、下划线、标题、列表、链接、文字颜色、字号。
- 图片粘贴必须走文件中心上传能力，不允许在业务页自行处理上传。
- 编辑区使用白底、细边框、8-10px 圆角、统一 focus 蓝色描边。
- 详情展示时必须先做 HTML 安全清理，列表摘要只能展示纯文本摘要。

### MessageRecipientPicker

- 用于消息收件人、抄送人、待办处理人等需要按部门筛人的业务场景。
- 消息中心第一版使用下拉框式部门人员树，不使用左右分栏。
- 下拉树不展示“全部部门”虚拟根节点，顶层直接显示真实部门根节点；需要全员发送时使用发送对象的“全员”选项。
- 部门节点图标必须复用 `DepartmentIcon`，保持和部门管理组织树一致。
- 输入框展示已选人员标签，空间不足时使用 `+N` 折叠并悬浮展示完整名单。
- 下拉面板中部门和人员在同一棵树内展示，部门可展开，人员显示在所属部门下。
- 勾选父部门表示选中该部门及所有下级部门的正常启用人员；勾选子部门只影响该子树；人员节点可单独勾选。
- 支持按部门名称、姓名、账号搜索，搜索结果保留树形上下文。
- 多个页面出现时必须复用业务组件，不得回退为平铺用户多选。
- 收件人和抄送人同时选择同一用户时，业务层按收件人优先去重。

### MessageAttachmentUploader

- 用于消息附件和待办附件上传，必须复用文件中心 `/api/system/files/upload`。
- 上传成功后仅提交文件 ID，由后端通过 `system_file_link` 绑定业务对象。
- 附件列表展示文件类型图标、文件名、大小、移除操作。
- 消息 / 待办详情附件必须提供预览 / 下载入口，预览仍遵循文件中心预览规则。

### 待办新增表单

- 新增待办必须对齐消息中心发送弹窗的三段式结构：基础信息、处理范围、待办内容。
- 处理人必须使用 `MessageRecipientPicker`，支持部门节点批量选择和人员节点多选，不允许回退为单人下拉。
- 待办内容必须使用 `DsRichTextEditor`，提交 `contentType = HTML`，列表摘要展示纯文本。
- 待办附件必须使用 `MessageAttachmentUploader`，后端通过 `system_file_link` 绑定 `bizType = TODO`、`categoryCode = attachment`。
- 待办详情展示富文本前必须做 HTML 安全清理，附件区使用文件类型图标并提供预览 / 下载。
- 多处理人待办按个人处理状态独立流转，列表状态展示当前登录人的个人待办状态。

### 抄送规则

- 消息中心的抄送是轻量 `CC` 标识，不等同内部邮件系统。
- 不做回复、转发、草稿箱、发件箱、邮件线程、密送、签名、邮件规则。
- 抄送消息在列表和详情中可展示 `抄送` 标签，已读、删除、导出逻辑与收件消息保持一致。

## 图形资源规则

- 优先使用 `icons` 目录下的 Vue SVG 组件
- 不在业务页面重复写零散 SVG
- 不把基础图形拆成大量 PNG
- 头像兜底统一使用 `UserAvatarFallback`
- 空状态统一使用 `EmptyStateIcon`
- 文件类型统一使用 `FileTypeIcon`

## 禁止事项

- 禁止在业务页面写死 HEX 颜色
- 禁止在业务页面重复实现部门、人员、角色、状态、日期范围选择器
- 禁止在业务页面重复实现用户信息单元格
- 禁止在业务页面临时拼接批量操作按钮区
- 禁止为了某一页单独改全局 Arco 样式
- 禁止业务页面各自实现空状态图形和头像兜底
- 禁止把基础控件样式写成大量行内 style

## 示例代码

```vue
<template>
  <DsFilterBar title="筛选查询" description="按条件快速定位数据">
    <DsKeywordSearch v-model="filters.keyword" label="关键词" placeholder="搜索姓名 / 账号" @search="handleSearch" />
    <DsDeptTreeSelect v-model="filters.deptId" label="部门" include-all include-unassigned />
    <DsStatusSelect v-model="filters.status" label="状态" />
    <template #actions>
      <a-button type="primary" @click="handleSearch">查询</a-button>
      <a-button @click="handleReset">重置</a-button>
    </template>
  </DsFilterBar>
</template>
```

```vue
<template>
  <DsActionBar selection-text="已选择 3 人" :selection-active="true">
    <template #batch>
      <DsBatchActions :selected-count="3" :actions="batchActions" @action="handleBatchAction" />
    </template>
    <template #tools>
      <DsIconButton tooltip="刷新"><IconRefresh /></DsIconButton>
      <DsIconButton tooltip="列设置" disabled><IconSettings /></DsIconButton>
    </template>
  </DsActionBar>
</template>
```

```vue
<template>
  <DsRoleMultiSelect v-model="form.roleIds" label="关联角色" />
  <DsDeptTreeSelect v-model="form.deptId" label="所属部门" include-unassigned />
</template>
```

## 预览页

- 路由：`/system/design-system`
- 标题：`基础平台 UI 组件库`

预览页用于校验基础输入组件、选择类组件、筛选查询区、典型用户表单、状态和空状态展示是否符合规范。
