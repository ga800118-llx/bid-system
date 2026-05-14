<template>
  <PageHeader title="帮助文档" />
  <div class="help-page">
    <div class="help-hero">
      <div class="hero-label">{{ enterpriseName }}</div>
      <h2>{{ systemTitle }}</h2>
      <p>
        当前帮助页聚焦基础平台能力，方便管理员、实施人员和后续开发者快速了解这套系统已经具备什么、该从哪里开始用。
      </p>
    </div>

    <a-tabs class="help-tabs">
      <a-tab-pane key="overview" title="平台概览">
        <div class="help-section">
          <h3>当前平台包含什么</h3>
          <p>当前基础平台已经覆盖组织、账号、权限、配置、字典和审计这些通用底座能力，可直接承接后续业务模块。</p>
          <div class="summary-grid">
            <div v-for="item in summaryCards" :key="item.title" class="summary-item">
              <div class="summary-title">{{ item.title }}</div>
              <div class="summary-desc">{{ item.desc }}</div>
            </div>
          </div>
        </div>

        <div class="help-section">
          <h3>核心模块一览</h3>
          <a-table :columns="moduleColumns" :data="moduleData" size="small" stripe :pagination="false" />
        </div>

        <div class="help-section">
          <h3>当前边界</h3>
          <a-alert type="warning">
            <template #title>当前阶段不在基础平台封板范围内</template>
            定时任务、文件存储、消息通知、API Key、限流，以及项目管理、上传、项目删除相关平台化改造，当前都不属于这轮基础平台收口范围。
          </a-alert>
        </div>

        <div class="help-section">
          <h3>接手开发入口</h3>
          <p>如果是新同事接手，或者隔一段时间再回来看这个项目，建议先按下面顺序读文档。</p>
          <a-table :columns="docColumns" :data="docData" size="small" stripe :pagination="false">
            <template #path="{ record }">
              <span class="doc-path">{{ record.path }}</span>
            </template>
          </a-table>
        </div>
      </a-tab-pane>

      <a-tab-pane key="system" title="系统管理">
        <div class="help-section">
          <h3>系统管理页面</h3>
          <a-table :columns="pageColumns" :data="pageData" size="small" stripe :pagination="false" />
        </div>

        <div class="help-section">
          <h3>推荐使用顺序</h3>
          <a-steps :current="4" size="small">
            <a-step title="先看部门" description="确认组织架构、根部门和负责人设置是否正确。" />
            <a-step title="再看用户" description="新增账号、分配部门、设置角色并检查登录策略。" />
            <a-step title="然后配角色" description="按功能权限、数据范围、字段权限配置角色。" />
            <a-step title="最后收配置与字典" description="统一系统标题、企业名称、水印、分页和下拉字典。" />
          </a-steps>
        </div>

        <div class="help-section">
          <h3>管理员首次上线检查清单</h3>
          <div class="checklist">
            <div v-for="item in launchChecklist" :key="item.title" class="checklist-item">
              <div class="checklist-title">{{ item.title }}</div>
              <div class="checklist-desc">{{ item.desc }}</div>
            </div>
          </div>
        </div>
      </a-tab-pane>

      <a-tab-pane key="permission" title="权限说明">
        <div class="help-section">
          <h3>权限模型</h3>
          <div class="summary-grid">
            <div v-for="item in permissionCards" :key="item.title" class="summary-item">
              <div class="summary-title">{{ item.title }}</div>
              <div class="summary-desc">{{ item.desc }}</div>
            </div>
          </div>
        </div>

        <div class="help-section">
          <h3>字段权限当前覆盖范围</h3>
          <a-table :columns="fieldColumns" :data="fieldData" size="small" stripe :pagination="false" />
          <div class="section-tip">当前字段权限第一版只覆盖用户管理和操作日志，这属于当前阶段有意收住的范围。</div>
        </div>

        <div class="help-section">
          <h3>权限使用口径</h3>
          <ul>
            <li>普通用户的菜单、按钮和接口访问，以角色配置为准。</li>
            <li>系统管理员仍保留兼容兜底，默认拥有全部权限。</li>
            <li>数据范围决定“能看到哪些数据”，字段权限决定“能看到哪些列、能改哪些字段”。</li>
            <li>权限拒绝、登录成功、登录失败、账号锁定、退出登录都会写入操作日志。</li>
          </ul>
        </div>
      </a-tab-pane>

      <a-tab-pane key="config" title="配置与字典">
        <div class="help-section">
          <h3>系统配置适合放什么</h3>
          <a-table :columns="configColumns" :data="configData" size="small" stripe :pagination="false" />
        </div>

        <div class="help-section">
          <h3>用户水印</h3>
          <p>用户水印已经作为全局能力接入后台，可在系统配置中统一调整，适合审计、截图留痕和内部信息保护。</p>
          <a-table :columns="watermarkColumns" :data="watermarkData" size="small" stripe :pagination="false" />
        </div>

        <div class="help-section">
          <h3>字典管理怎么用</h3>
          <ul>
            <li>字典类型用于定义一组固定选项，例如状态、模块、动作、分组。</li>
            <li>字典项用于定义具体选项，例如启用、禁用、基础设置、安全设置。</li>
            <li>适合放入字典的内容通常是下拉选项、标签状态、筛选条件和未来可能扩展的枚举值。</li>
            <li>后续开发业务模块时，建议先判断是否需要字典，再开发页面和接口。</li>
          </ul>
        </div>
      </a-tab-pane>

      <a-tab-pane key="faq" title="常见问题">
        <div class="faq-list">
          <div v-for="item in faqItems" :key="item.q" class="faq-item">
            <div class="faq-question">{{ item.q }}</div>
            <div class="faq-answer">{{ item.a }}</div>
          </div>
        </div>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { DEFAULT_ENTERPRISE_NAME, DEFAULT_SYSTEM_TITLE, loadPublicSystemConfig } from '@/utils/systemConfig'

const systemTitle = ref(DEFAULT_SYSTEM_TITLE)
const enterpriseName = ref(DEFAULT_ENTERPRISE_NAME)

const summaryCards = [
  { title: '组织与账号', desc: '部门管理、用户管理、多角色分配、负责人自动清理已经可用。' },
  { title: '权限体系', desc: '功能权限、数据范围、字段权限已在核心基础平台接口真实生效。' },
  { title: '统一配置', desc: '企业名称、系统标题、密码策略、分页、水印等都已从系统配置读取。' },
  { title: '审计留痕', desc: '关键写操作、权限拒绝和登录安全事件都已进入操作日志。' }
]

const moduleColumns = [
  { title: '模块', dataIndex: 'name', width: 140 },
  { title: '说明', dataIndex: 'desc', minWidth: 280 },
  { title: '状态', dataIndex: 'status', width: 120 }
]

const moduleData = [
  { name: '部门管理', desc: '部门树、搜索、新增、编辑、启停、负责人和删除前影响提示。', status: '已完成' },
  { name: '用户管理', desc: '分页筛选、新增编辑、启停、调部门、重置密码、多角色分配。', status: '已完成' },
  { name: '角色权限', desc: '角色 CRUD、功能权限、数据范围、字段权限三层配置。', status: '已完成' },
  { name: '操作日志', desc: '关键写操作审计、权限拒绝审计、登录安全审计和筛选查询。', status: '已完成' },
  { name: '系统配置', desc: '统一维护企业名称、系统标题、安全配置、分页和用户水印。', status: '已完成' },
  { name: '字典管理', desc: '统一维护字典类型和字典项，服务状态、模块、动作和下拉复用。', status: '已完成' }
]

const docColumns = [
  { title: '顺序', dataIndex: 'order', width: 70 },
  { title: '文档', dataIndex: 'name', width: 220 },
  { title: '用途', dataIndex: 'usage', minWidth: 260 },
  { title: '路径', slotName: 'path', minWidth: 320 }
]

const docData = [
  { order: '1', name: '项目现状与交接', usage: '快速了解当前项目做到哪、哪些已完成、哪些当前不做。', path: 'docs/项目现状与交接.md' },
  { order: '2', name: '接手人30分钟阅读清单', usage: '用最短时间建立整体认知，适合第一次接手时先看。', path: 'docs/接手人30分钟阅读清单.md' },
  { order: '3', name: '基础模块实施计划', usage: '了解基础平台能力边界、收口状态和最近验证结果。', path: 'docs/基础模块实施计划.md' },
  { order: '4', name: '权限管理系统设计', usage: '了解 Feature、DataScope、Field 的设计和新模块权限接入方式。', path: 'docs/权限管理系统设计.md' },
  { order: '5', name: '部门与用户管理设计', usage: '了解组织、用户、多角色和负责人相关规则。', path: 'docs/部门与用户管理设计.md' },
  { order: '6', name: '仓库整理清单', usage: '区分源码、本地运行产物和历史文件，避免误删误改。', path: 'docs/仓库文件整理清单.md' }
]

const pageColumns = [
  { title: '页面', dataIndex: 'path', width: 180 },
  { title: '主要用途', dataIndex: 'usage', minWidth: 220 },
  { title: '常见操作', dataIndex: 'actions', minWidth: 260 }
]

const pageData = [
  { path: '/system/department', usage: '维护组织架构与部门负责人。', actions: '新增部门、编辑部门、启停、查看部门内用户。' },
  { path: '/system/user', usage: '从全局视角维护用户。', actions: '搜索筛选、新增、编辑、重置密码、调部门、分配角色。' },
  { path: '/system/role', usage: '配置角色与权限。', actions: '新增角色、保存功能权限、设置数据范围、配置字段权限。' },
  { path: '/system/log', usage: '查询审计日志与登录安全日志。', actions: '按模块、动作、结果、时间范围和关键词筛选。' },
  { path: '/system/config', usage: '维护系统全局配置。', actions: '搜索、分组筛选、新增配置、编辑配置、启停非内置项。' },
  { path: '/system/dict', usage: '维护字典类型和字典项。', actions: '维护类型、维护字典项、启停、设置默认项与标签色。' }
]

const launchChecklist = [
  { title: '确认企业名称和系统标题', desc: '检查 enterprise.name 和 system.title 是否已改成当前环境正式值。' },
  { title: '检查管理员账号', desc: '确认管理员密码已调整，登录、退出、失败锁定日志能正常记录。' },
  { title: '检查组织架构', desc: '确认根部门、部门层级和负责人设置正确，未分配用户已清理。' },
  { title: '检查角色权限', desc: '确认关键角色的功能权限、数据范围和字段权限配置符合预期。' },
  { title: '检查系统配置', desc: '确认分页、水印、密码策略、侧边栏默认状态等基础配置符合使用场景。' },
  { title: '检查字典项', desc: '确认通用状态、日志模块、日志动作、配置分组等内置字典正常可用。' },
  { title: '检查操作日志', desc: '确认新增、编辑、启停、登录失败、权限拒绝等事件都能查询到。' }
]

const permissionCards = [
  { title: '功能权限', desc: '决定是否能进入页面、看到菜单、使用新增编辑删除等操作。' },
  { title: '数据范围', desc: '决定用户可查看哪些部门或用户数据，例如全部、本部门、本人。' },
  { title: '字段权限', desc: '决定某个字段是否可读、是否可写，当前已在用户管理和操作日志落地。' }
]

const fieldColumns = [
  { title: '模块', dataIndex: 'module', width: 140 },
  { title: '受控字段', dataIndex: 'fields', minWidth: 280 },
  { title: '作用', dataIndex: 'effect', minWidth: 220 }
]

const fieldData = [
  { module: 'system_user', fields: '账号、姓名、部门、角色、状态、登录安全', effect: '控制列表列显示、编辑表单可写性和接口返回字段。' },
  { module: 'system_log', fields: '操作人、IP、请求信息、操作内容', effect: '控制日志列表列显示和接口字段返回。' }
]

const configColumns = [
  { title: '配置项类型', dataIndex: 'type', width: 140 },
  { title: '典型示例', dataIndex: 'examples', minWidth: 280 },
  { title: '说明', dataIndex: 'desc', minWidth: 220 }
]

const configData = [
  { type: '品牌信息', examples: 'enterprise.name、system.title', desc: '统一管理企业名称和系统名称。' },
  { type: '安全策略', examples: '密码最小长度、强密码、登录失败次数、会话超时', desc: '用于登录和密码规则统一控制。' },
  { type: '界面偏好', examples: '默认分页、分页选项、侧边栏默认折叠', desc: '用于基础交互体验统一。' },
  { type: '全局水印', examples: '开关、模板、透明度、字号、旋转角度', desc: '用于后台页面水印审计和信息保护。' }
]

const watermarkColumns = [
  { title: '配置键', dataIndex: 'key', width: 220 },
  { title: '含义', dataIndex: 'desc', minWidth: 220 },
  { title: '常见值', dataIndex: 'example', minWidth: 220 }
]

const watermarkData = [
  { key: 'ui.watermark.enabled', desc: '控制后台页面是否显示水印。', example: 'true / false' },
  { key: 'ui.watermark.text_template', desc: '定义水印内容模板。', example: '{realName} {username} {dateTime}' },
  { key: 'ui.watermark.opacity', desc: '控制水印透明度。', example: '0.12' },
  { key: 'ui.watermark.font_size', desc: '控制水印字号。', example: '16' },
  { key: 'ui.watermark.rotate', desc: '控制水印旋转角度。', example: '-24' }
]

const faqItems = [
  {
    q: '系统管理员是不是必须在角色页里把权限全部勾满？',
    a: '当前不是。系统管理员仍保留兼容兜底，即使角色页未完整勾选，默认仍拥有全部权限。'
  },
  {
    q: '为什么字段权限现在只覆盖用户管理和操作日志？',
    a: '这是当前阶段有意收住的范围。先覆盖最敏感、最有价值的模块，后续业务模块再按需要逐步接入。'
  },
  {
    q: '新增业务模块时，应该先做什么？',
    a: '建议先判断这个模块是否需要系统配置、字典、权限点、数据范围和字段权限，再开始页面和接口开发。'
  },
  {
    q: '哪些内容更适合放到字典管理里？',
    a: '下拉选项、状态标签、筛选条件、未来可能扩展的枚举值，通常都适合放进字典。'
  },
  {
    q: '为什么帮助页里不再详细介绍上传和 Prompt？',
    a: '当前基础平台已经接近封板，帮助页现在以基础平台底座为主，项目、上传和 Prompt 相关能力后续更适合单独整理业务帮助文档。'
  }
]

onMounted(async () => {
  const config = await loadPublicSystemConfig()
  systemTitle.value = config.systemTitle || DEFAULT_SYSTEM_TITLE
  enterpriseName.value = config.enterpriseName || DEFAULT_ENTERPRISE_NAME
})
</script>

<style scoped>
.help-page {
  padding: 24px;
}

.help-hero {
  padding: 20px 24px;
  background: var(--ds-color-bg-card);
  border-radius: 8px;
  margin-bottom: 16px;
}

.help-hero h2 {
  margin: 4px 0 8px;
  font-size: 24px;
  line-height: 32px;
  color: var(--ds-color-text-primary);
}

.help-hero p {
  margin: 0;
  color: var(--ds-color-text-regular);
  line-height: 1.8;
}

.hero-label {
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 18px;
}

.help-tabs {
  background: var(--ds-color-bg-card);
  border-radius: 8px;
  padding: 0 20px 20px;
}

.help-section {
  margin-top: 20px;
}

.help-section h3 {
  margin: 0 0 12px;
  padding-left: 8px;
  border-left: 3px solid var(--ds-color-primary);
  color: var(--ds-color-text-primary);
  font-size: 15px;
  line-height: 22px;
  font-weight: 600;
}

.help-section p,
.help-section ul,
.help-section ol {
  color: var(--ds-color-text-regular);
  line-height: 1.8;
}

.help-section ul,
.help-section ol {
  padding-left: 20px;
  margin: 0;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  padding: 16px;
  border: 1px solid var(--ds-color-border);
  border-radius: 8px;
  background: var(--ds-color-bg-soft);
}

.summary-title {
  color: var(--ds-color-text-primary);
  font-size: 14px;
  line-height: 22px;
  font-weight: 600;
}

.summary-desc {
  margin-top: 6px;
  color: var(--ds-color-text-regular);
  font-size: 13px;
  line-height: 1.8;
}

.section-tip {
  margin-top: 8px;
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.doc-path {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  color: var(--ds-color-text-regular);
  word-break: break-all;
}

.checklist {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.checklist-item {
  padding: 16px;
  border: 1px solid var(--ds-color-border);
  border-radius: 8px;
  background: var(--ds-color-bg-soft);
}

.checklist-title {
  color: var(--ds-color-text-primary);
  font-size: 14px;
  line-height: 22px;
  font-weight: 600;
}

.checklist-desc {
  margin-top: 6px;
  color: var(--ds-color-text-regular);
  font-size: 13px;
  line-height: 1.8;
}

.faq-list {
  margin-top: 20px;
}

.faq-item {
  padding: 16px 0;
  border-bottom: 1px solid var(--ds-color-bg-layout);
}

.faq-item:first-child {
  padding-top: 0;
}

.faq-item:last-child {
  border-bottom: 0;
}

.faq-question {
  color: var(--ds-color-text-primary);
  font-size: 14px;
  line-height: 22px;
  font-weight: 600;
}

.faq-answer {
  margin-top: 6px;
  color: var(--ds-color-text-regular);
  line-height: 1.8;
}

@media (max-width: 900px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .checklist {
    grid-template-columns: 1fr;
  }

  .help-page {
    padding: 16px;
  }

  .help-tabs {
    padding: 0 16px 16px;
  }
}
</style>
