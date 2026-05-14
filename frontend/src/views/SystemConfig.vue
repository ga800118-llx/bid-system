<template>
  <div class="config-page">
    <div class="main">
      <DsPageHeader
        :breadcrumb="['工作台', '系统管理', '系统配置']"
        title="系统配置"
        description="统一维护企业名称、系统标题、安全策略、分页和水印等基础配置。"
      >
        <template #icon><IconSettings /></template>
        <template #actions>
          <DsHeaderActions
            :tool-items="headerToolItems"
            :show-primary="canUpdateConfig"
            primary-label="新增配置"
            @tool-select="handleHeaderToolSelect"
            @primary-click="openAddModal"
          >
            <template #primaryIcon><IconPlus /></template>
          </DsHeaderActions>
        </template>
        <template #meta>
          <span>共 {{ total }} 项配置</span>
          <span>{{ enabledCount }} 项启用</span>
          <span>{{ builtInCount }} 项内置</span>
        </template>
      </DsPageHeader>

      <section class="summary-grid">
        <DsStatsCard
          v-for="card in summaryCards"
          :key="card.key"
          :label="card.label"
          :value="card.value"
          :hint="card.hint"
          :tone="card.tone"
        >
          <template #icon><component :is="card.icon" /></template>
        </DsStatsCard>
      </section>

      <DsFilterBar title="筛选查询" description="按配置键、配置名称、分组、状态和内置属性筛选系统配置。">
        <template #title>
          <span class="filter-title">
            <span class="filter-title__icon"><IconFilter /></span>
            <span>筛选查询</span>
          </span>
        </template>
        <div class="filter-fields">
          <DsKeywordSearch
            v-model="filters.keyword"
            class="filter-item filter-item--keyword"
            label="关键词"
            placeholder="搜索键、名称或值"
            @search="handleSearch"
          />
          <DsDictSelect v-model="filters.groupCode" type-code="config_group" label="分组" placeholder="全部分组" class="filter-item filter-item--group" @update:model-value="handleSearch" />
          <DsStatusSelect v-model="filters.status" label="状态" placeholder="全部状态" class="filter-item filter-item--status" @update:model-value="handleSearch" />
          <div class="filter-check">
            <a-checkbox v-model="filters.builtIn" @change="handleSearch">仅看内置</a-checkbox>
          </div>
        </div>
        <template #actions>
          <div class="filter-actions">
            <a-button type="primary" class="filter-button" @click="handleSearch">查询</a-button>
            <a-button class="filter-button" @click="resetFilters">重置</a-button>
          </div>
        </template>
      </DsFilterBar>

      <section class="table-card card-shell">
        <div class="section-header section-header--table">
          <div class="table-header-copy">
            <h2 class="section-title">配置列表</h2>
            <p class="section-description">共 {{ total }} 条，内置配置受保护，非内置配置可按权限维护。</p>
          </div>
          <DsActionBar
            class="table-header-tools"
            :selection-text="`当前页 ${tableData.length} 条`"
            :selection-active="tableData.length > 0"
          >
            <template #tools>
              <a-button v-if="canExportConfig" class="table-export-button" @click="handleExport">导出配置</a-button>
              <DsIconButton tooltip="刷新配置" @click="fetchConfigs"><IconRefresh /></DsIconButton>
              <DsIconButton tooltip="列设置待补充" disabled><IconSettings /></DsIconButton>
            </template>
          </DsActionBar>
        </div>
      <DsDataTable
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :scroll="{ x: tableScrollX }"
        row-key="id"
        :pagination="paginationConfig"
        @pagination-change="handlePageChange"
        @page-size-change="handleSizeChange"
      >
        <template #index="{ rowIndex }">{{ (page - 1) * size + rowIndex + 1 }}</template>
        <template #configKey="{ record }">
          <div class="config-key-cell">
            <span class="config-key">{{ record.configKey }}</span>
            <DsStatusTag v-if="isBuiltInConfig(record)" label="内置" tone="warning" />
          </div>
        </template>
        <template #groupCode="{ record }">
          <DsStatusTag :label="groupLabel(record.groupCode)" :tone="groupTone(record.groupCode)" />
        </template>
        <template #valueType="{ record }">
          <DsStatusTag :label="valueTypeLabel(record.valueType)" :tone="valueTypeTone(record.valueType)" />
        </template>
        <template #status="{ record }">
          <DsStatusTag :label="statusLabel(statusOptions, record.status)" :tone="statusTone(record.status)" />
        </template>
        <template #configValue="{ record }">
          <div class="value-cell">
            <a-popover v-if="record.configValue" trigger="hover" position="top" class="config-value-popover">
              <template #content>
                <div class="config-popover">
                  <div class="config-popover__title">配置值</div>
                  <div class="config-popover__text">{{ record.configValue }}</div>
                </div>
              </template>
              <span class="value-text">{{ record.configValue || '-' }}</span>
            </a-popover>
            <span v-else>-</span>
          </div>
        </template>
        <template #updatedAt="{ record }">
          <a-popover v-if="record.updatedAt" trigger="hover" position="top">
            <template #content>
              <div class="config-popover config-popover--time">
                <div class="config-popover__title">更新时间</div>
                <div class="config-popover__text">{{ formatTime(record.updatedAt) }}</div>
              </div>
            </template>
            <span class="time-text">{{ formatShortTime(record.updatedAt) }}</span>
          </a-popover>
          <span v-else>-</span>
        </template>
        <template #actions="{ record }">
          <div class="row-actions">
            <a-button v-if="canUpdateConfig" type="text" size="small" @click="openEditModal(record)">编辑</a-button>
            <a-button
              v-if="canUpdateConfig && !isBuiltInConfig(record) && record.status === 1"
              type="text"
              size="small"
              status="danger"
              @click="disableConfig(record)"
            >
              禁用
            </a-button>
            <a-button
              v-else-if="canUpdateConfig && !isBuiltInConfig(record) && record.status !== 1"
              type="text"
              size="small"
              status="success"
              @click="enableConfig(record)"
            >
              启用
            </a-button>
            <span v-else-if="canUpdateConfig && isBuiltInConfig(record)" class="no-actions">内置保护</span>
            <span v-else class="no-actions">无可用操作</span>
          </div>
        </template>
        <template #empty>
          <DsEmptyState title="暂无系统配置" description="当前筛选条件下没有可展示的系统配置。" />
        </template>
      </DsDataTable>
      </section>
    </div>

    <DsModalForm
      v-model:visible="modalVisible"
      :title="isEdit ? '编辑配置' : '新增配置'"
      :description="isEdit ? '维护配置值、分组、状态和说明。' : '新增 key-value 配置项，供基础平台功能读取。'"
      :width="720"
      @cancel="modalVisible = false"
    >
      <DsFormSection title="基础信息">
        <DsFormGrid>
          <DsInput v-model="form.configKey" label="配置键" type="text" required :max-length="100" placeholder="如 enterprise.name" :disabled="isEdit && isBuiltInForm" />
          <DsInput v-model="form.configName" label="配置名称" type="text" required :max-length="100" placeholder="请输入配置名称" />
          <DsDictSelect v-model="form.groupCode" type-code="config_group" label="分组" placeholder="请选择分组" />
          <DsDictSelect v-model="form.valueType" type-code="config_value_type" label="值类型" placeholder="请选择值类型" :allow-clear="false" />
          <DsStatusSelect v-model="form.status" label="状态" :include-all="false" :allow-clear="false" :disabled="isEdit && isBuiltInForm" />
        </DsFormGrid>
        <div v-if="isEdit && isBuiltInForm" class="form-tip">内置配置键不允许修改，且需要保持启用，可继续维护配置值。</div>
        <div v-if="watermarkKeyTip" class="form-tip">{{ watermarkKeyTip }}</div>
      </DsFormSection>

      <DsFormSection title="配置值" :description="valueTypeTip">
          <DsRadioGroup
            v-if="form.valueType === 'BOOLEAN'"
            v-model="form.configValue"
            :options="booleanOptions"
          />
          <DsNumberInput
            v-else-if="form.valueType === 'NUMBER'"
            v-model="numberValue"
            label="配置值"
            :placeholder="valuePlaceholder"
            :precision="2"
          />
          <template v-else-if="form.valueType === 'JSON'">
            <DsTextarea
              v-model="form.configValue"
              label="配置值"
              :placeholder="valuePlaceholder"
              :auto-size="{ minRows: 5, maxRows: 10 }"
              class="json-textarea"
            />
            <div class="value-actions">
              <a-button size="small" @click="formatJsonValue">格式化 JSON</a-button>
            </div>
          </template>
          <DsTextarea
            v-else
            v-model="form.configValue"
            label="配置值"
            :placeholder="valuePlaceholder"
            :auto-size="{ minRows: 3, maxRows: 8 }"
          />
      </DsFormSection>

      <DsFormSection v-if="isWatermarkConfig" title="水印预览">
          <div class="watermark-preview-section">
            <div class="watermark-preview" :style="watermarkPreviewStyle">
              <div class="watermark-preview-mask"></div>
            </div>
            <div class="watermark-preview-title">后台页面水印效果预览</div>
            <div class="watermark-preview-desc">
              {{ watermarkPreviewEnabled ? '当前开关已启用，预览按当前配置实时更新。' : '当前开关关闭，启用后将按下面样式渲染。' }}
            </div>
            <div class="form-tip">占位符支持：{userId}、{realName}、{username}、{deptName}、{roleNames}、{systemTitle}、{dateTime}</div>
          </div>
      </DsFormSection>

      <DsFormSection title="说明">
        <DsTextarea v-model="form.description" label="说明" placeholder="请输入说明" :max-length="300" :auto-size="{ minRows: 2, maxRows: 4 }" />
      </DsFormSection>
      <template #footer>
        <DsFormActions @cancel="modalVisible = false" @submit="submitConfigForm" />
      </template>
    </DsModalForm>

    <DsModalForm
      v-model:visible="importResultVisible"
      title="系统配置导入结果"
      description="查看本次导入的成功、跳过和失败明细。"
      :width="560"
      @cancel="importResultVisible = false"
    >
      <DsFormSection v-if="importResult" title="导入结果">
        <div class="detail-grid">
          <span>总行数</span>
          <span>{{ importResult.totalCount || 0 }}</span>
          <span>成功</span>
          <span>{{ importResult.successCount || 0 }}</span>
          <span>跳过</span>
          <span>{{ importResult.skippedCount || 0 }}</span>
          <span>失败</span>
          <span>{{ importResult.failureCount || 0 }}</span>
        </div>
        <div v-if="importResult.message" class="detail-block">
          <div class="detail-title">导入结果</div>
          <div class="detail-text">{{ importResult.message }}</div>
        </div>
        <div v-if="importResult.details?.length" class="impact-usage">
          <div class="impact-title">明细</div>
          <ul>
            <li v-for="item in importResult.details" :key="item">{{ item }}</li>
          </ul>
        </div>
      </DsFormSection>
      <template #footer>
        <div class="single-footer-action">
          <a-button type="primary" @click="importResultVisible = false">关闭</a-button>
        </div>
      </template>
    </DsModalForm>

    <input ref="importFileInput" type="file" accept=".xlsx" class="hidden-file-input" @change="handleImportFileChange" />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconCheckCircle,
  IconExclamationCircle,
  IconFilter,
  IconHistory,
  IconPlus,
  IconRefresh,
  IconSettings,
  IconUser
} from '@arco-design/web-vue/es/icon'
import { systemConfigApi } from '@/api'
import { getUserInfo, hasPermission } from '@/utils/permission'
import { DEFAULT_PAGE_SIZE_OPTIONS, applyPaginationConfig } from '@/utils/systemConfig'
import { loadCommonStatusOptions, loadPublicDictItems, statusLabel } from '@/utils/dict'
import {
  DsActionBar,
  DsDataTable,
  DsDictSelect,
  DsEmptyState,
  DsFilterBar,
  DsFormActions,
  DsFormGrid,
  DsFormSection,
  DsHeaderActions,
  DsIconButton,
  DsInput,
  DsKeywordSearch,
  DsModalForm,
  DsNumberInput,
  DsPageHeader,
  DsRadioGroup,
  DsStatsCard,
  DsStatusTag,
  DsStatusSelect,
  DsTextarea
} from '@/design-system'

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const size = ref(20)
const pageSizeOptions = ref([...DEFAULT_PAGE_SIZE_OPTIONS])
const total = ref(0)
const tableScrollX = '100%'
const modalVisible = ref(false)
const importResultVisible = ref(false)
const importResult = ref(null)
const importFileInput = ref(null)
const isEdit = ref(false)
const filters = reactive({ keyword: '', groupCode: '', status: '', builtIn: false })
const canImportConfig = computed(() => hasPermission('system_config:import'))
const canUpdateConfig = computed(() => hasPermission('system_config:update'))
const canExportConfig = computed(() => hasPermission('system_config:export'))
const headerToolItems = computed(() => [
  { key: 'template', label: '下载模板', visible: canImportConfig.value },
  { key: 'import', label: '导入配置', visible: canImportConfig.value },
  { key: 'export', label: '导出配置', visible: canExportConfig.value }
])
const booleanOptions = [
  { label: 'true', value: 'true' },
  { label: 'false', value: 'false' }
]
const fallbackValueTypeOptions = [
  { itemLabel: '文本', itemValue: 'TEXT', tagColor: 'arcoblue' },
  { itemLabel: '数字', itemValue: 'NUMBER', tagColor: 'green' },
  { itemLabel: '布尔', itemValue: 'BOOLEAN', tagColor: 'gold' },
  { itemLabel: 'JSON', itemValue: 'JSON', tagColor: 'purple' }
]
const fallbackGroupOptions = [
  { itemLabel: '基础设置', itemValue: 'basic', tagColor: 'arcoblue' },
  { itemLabel: '安全设置', itemValue: 'security', tagColor: 'red' },
  { itemLabel: '界面设置', itemValue: 'ui', tagColor: 'purple' }
]
const builtInKeys = [
  'enterprise.name',
  'system.title',
  'security.password.min_length',
  'security.password.require_strong',
  'security.login.max_failed_attempts',
  'security.session.timeout_minutes',
  'ui.default.page_size',
  'ui.page_size_options',
  'ui.sidebar.collapsed_default',
  'ui.watermark.enabled',
  'ui.watermark.text_template',
  'ui.watermark.opacity',
  'ui.watermark.font_size',
  'ui.watermark.rotate',
  'file.storage.type',
  'file.local.base_path',
  'file.upload.max_size_mb',
  'file.allowed_extensions',
  'file.previewable_extensions',
  'message.default_page_size'
]
const watermarkConfigKeys = [
  'ui.watermark.enabled',
  'ui.watermark.text_template',
  'ui.watermark.opacity',
  'ui.watermark.font_size',
  'ui.watermark.rotate'
]
const form = reactive({
  id: null,
  configKey: '',
  configName: '',
  configValue: '',
  description: '',
  valueType: 'TEXT',
  groupCode: 'basic',
  status: 1
})

const groupOptions = ref([])
const valueTypeOptions = ref([])
const statusOptions = ref([])
const columns = [
  { title: '序号', width: 96, align: 'center', slotName: 'index' },
  { title: '配置键', slotName: 'configKey', width: 350 },
  { title: '名称', dataIndex: 'configName' },
  { title: '配置值', slotName: 'configValue', width: 176 },
  { title: '分组', slotName: 'groupCode', width: 132 },
  { title: '类型', slotName: 'valueType', width: 112 },
  { title: '状态', slotName: 'status', width: 104 },
  { title: '更新时间', slotName: 'updatedAt', width: 136 },
  { title: '操作', slotName: 'actions', width: 188 }
]
const paginationConfig = computed(() => ({
  current: page.value,
  pageSize: size.value,
  total: total.value,
  pageSizeOptions: pageSizeOptions.value
}))
const enabledCount = computed(() => tableData.value.filter(item => Number(item.status) === 1).length)
const disabledCount = computed(() => tableData.value.filter(item => Number(item.status) !== 1).length)
const builtInCount = computed(() => tableData.value.filter(item => isBuiltInConfig(item)).length)
const editableCount = computed(() => tableData.value.filter(item => !isBuiltInConfig(item)).length)
const summaryCards = computed(() => [
  {
    key: 'total',
    label: '配置总数',
    value: total.value,
    hint: '当前筛选结果总量',
    tone: 'primary',
    icon: IconSettings
  },
  {
    key: 'enabled',
    label: '启用配置',
    value: enabledCount.value,
    hint: '当前页启用配置',
    tone: 'success',
    icon: IconCheckCircle
  },
  {
    key: 'disabled',
    label: '禁用配置',
    value: disabledCount.value,
    hint: '当前页禁用配置',
    tone: 'danger',
    icon: IconExclamationCircle
  },
  {
    key: 'builtIn',
    label: '内置配置',
    value: builtInCount.value,
    hint: '受保护的基础配置',
    tone: 'warning',
    icon: IconHistory
  },
  {
    key: 'editable',
    label: '可维护配置',
    value: editableCount.value,
    hint: '当前页非内置配置',
    tone: 'primary',
    icon: IconUser
  }
])
const isBuiltInForm = computed(() => builtInKeys.includes((form.configKey || '').trim()))
const numberValue = computed({
  get: () => {
    const text = String(form.configValue ?? '').trim()
    if (!text) return undefined
    const value = Number(text)
    return Number.isFinite(value) ? value : undefined
  },
  set: value => {
    form.configValue = value === undefined || value === null ? '' : String(value)
  }
})
const valuePlaceholder = computed(() => {
  if (form.valueType === 'NUMBER') return '请输入数字，如 30 或 99.5'
  if (form.valueType === 'BOOLEAN') return '请输入 true 或 false'
  if (form.valueType === 'JSON') return '请输入合法 JSON，如 {"enabled": true}'
  return '请输入配置值'
})
const valueTypeTip = computed(() => {
  if (form.valueType === 'NUMBER') return 'NUMBER 类型仅支持数字，允许小数。'
  if (form.valueType === 'BOOLEAN') return 'BOOLEAN 类型仅支持 true 或 false。'
  if (form.valueType === 'JSON') return 'JSON 类型需要是合法 JSON 文本。'
  return 'TEXT 类型按原文保存。'
})
const currentUser = computed(() => getUserInfo() || {})
const isWatermarkConfig = computed(() => watermarkConfigKeys.includes((form.configKey || '').trim()))
const watermarkKeyTip = computed(() => {
  const key = (form.configKey || '').trim()
  if (key === 'ui.watermark.enabled') return '控制后台页面是否显示全局用户水印。'
  if (key === 'ui.watermark.text_template') return '用模板定义水印内容，推荐组合用户、部门和时间信息。'
  if (key === 'ui.watermark.opacity') return '控制水印透明度，建议 0.05 - 0.2。'
  if (key === 'ui.watermark.font_size') return '控制水印字号，建议 12 - 24。'
  if (key === 'ui.watermark.rotate') return '控制水印旋转角度，建议 -45 到 45。'
  return ''
})
const watermarkConfigMap = computed(() => {
  const map = {}
  for (const item of tableData.value || []) {
    map[item.configKey] = item.configValue
  }
  return map
})
const watermarkPreviewEnabled = computed(() => {
  const key = (form.configKey || '').trim()
  if (key === 'ui.watermark.enabled') return String(form.configValue).toLowerCase() === 'true'
  return String(watermarkConfigMap.value['ui.watermark.enabled'] || 'false').toLowerCase() === 'true'
})
const watermarkPreviewTemplate = computed(() => {
  const key = (form.configKey || '').trim()
  if (key === 'ui.watermark.text_template') return String(form.configValue || '').trim() || '{realName} {username}'
  return String(watermarkConfigMap.value['ui.watermark.text_template'] || '{realName} {username}')
})
const watermarkPreviewOpacity = computed(() => {
  const key = (form.configKey || '').trim()
  const raw = key === 'ui.watermark.opacity' ? form.configValue : watermarkConfigMap.value['ui.watermark.opacity']
  return normalizeWatermarkOpacity(raw)
})
const watermarkPreviewFontSize = computed(() => {
  const key = (form.configKey || '').trim()
  const raw = key === 'ui.watermark.font_size' ? form.configValue : watermarkConfigMap.value['ui.watermark.font_size']
  return normalizeWatermarkFontSize(raw)
})
const watermarkPreviewRotate = computed(() => {
  const key = (form.configKey || '').trim()
  const raw = key === 'ui.watermark.rotate' ? form.configValue : watermarkConfigMap.value['ui.watermark.rotate']
  return normalizeWatermarkRotate(raw)
})
const watermarkPreviewText = computed(() => {
  const template = watermarkPreviewTemplate.value
  const now = new Date()
  const dateTime = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
  const roleNames = Array.isArray(currentUser.value?.roleNames) ? currentUser.value.roleNames.filter(Boolean).join(' / ') : ''
  const systemTitle = String(watermarkConfigMap.value['system.title'] || '投标管理系统').trim() || '投标管理系统'
  return template
    .replaceAll('{userId}', String(currentUser.value?.userId ?? ''))
    .replaceAll('{username}', currentUser.value?.username || '')
    .replaceAll('{realName}', currentUser.value?.realName || currentUser.value?.username || '')
    .replaceAll('{deptName}', currentUser.value?.deptName || '')
    .replaceAll('{roleNames}', roleNames)
    .replaceAll('{systemTitle}', systemTitle)
    .replaceAll('{dateTime}', dateTime)
    .trim()
})
const watermarkPreviewStyle = computed(() => {
  const alpha = watermarkPreviewEnabled.value ? watermarkPreviewOpacity.value : 0.05
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="320" height="220" viewBox="0 0 320 220">
      <g transform="translate(160 110) rotate(${watermarkPreviewRotate.value})">
        <text
          x="0"
          y="0"
          text-anchor="middle"
          dominant-baseline="middle"
          fill="rgba(29,33,41,${alpha})"
          font-size="${watermarkPreviewFontSize.value}"
          font-family="Arial, PingFang SC, Microsoft YaHei, sans-serif"
        >${escapeXml(watermarkPreviewText.value || '水印预览')}</text>
      </g>
    </svg>
  `.trim()
  return {
    backgroundImage: `url("data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}")`
  }
})

const isBuiltInConfig = (record) => builtInKeys.includes((record?.configKey || '').trim())

const fetchConfigs = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: size.value,
      keyword: filters.keyword?.trim() || undefined,
      groupCode: filters.groupCode || undefined,
      status: filters.status === '' ? undefined : filters.status,
      builtIn: filters.builtIn ? true : undefined
    }
    const res = await systemConfigApi.list(params)
    if (res.code === 200) {
      tableData.value = res.data?.records || []
      total.value = res.data?.total || 0
    } else {
      Message.error(res.msg || '加载系统配置失败')
    }
  } catch {
    Message.error('加载系统配置失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchConfigs()
}

const handleExport = async () => {
  try {
    const params = {
      keyword: filters.keyword?.trim() || undefined,
      groupCode: filters.groupCode || undefined,
      status: filters.status === '' ? undefined : filters.status,
      builtIn: filters.builtIn || undefined
    }
    const blob = await systemConfigApi.export(params)
    const objectUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = `系统配置_${new Date().toISOString().slice(0, 10).replace(/-/g, '')}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(objectUrl)
    Message.success('系统配置已开始导出')
  } catch {
    Message.error('导出失败')
  }
}

const downloadTemplate = async () => {
  try {
    const blob = await systemConfigApi.downloadTemplate()
    downloadBlob(blob, '系统配置导入模板.xlsx')
    Message.success('模板已下载')
  } catch {
    Message.error('下载模板失败')
  }
}

const triggerImport = () => importFileInput.value?.click()

const handlePageChange = (nextPage) => {
  page.value = nextPage
  fetchConfigs()
}

const handleSizeChange = (nextSize) => {
  size.value = nextSize
  page.value = 1
  fetchConfigs()
}

const handleHeaderToolSelect = (key) => {
  if (key === 'template') downloadTemplate()
  if (key === 'import') triggerImport()
  if (key === 'export') handleExport()
}

const refreshRuntimeConfig = async () => {
  await applyPaginationConfig(size, pageSizeOptions)
  await Promise.all([loadGroupOptions(), loadValueTypeOptions(), loadStatusOptions()])
  page.value = 1
  await fetchConfigs()
}

const resetFilters = () => {
  Object.assign(filters, { keyword: '', groupCode: '', status: '', builtIn: false })
  handleSearch()
}

const handleImportFileChange = async (event) => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await systemConfigApi.importConfigs(formData)
    if (res.code !== 200) {
      Message.error(res.msg || '导入失败')
      return
    }
    importResult.value = res.data || {}
    importResultVisible.value = true
    Message.success(res.data?.message || '导入完成')
    await fetchConfigs()
    window.dispatchEvent(new CustomEvent('system-config-updated'))
  } catch {
    Message.error('导入失败')
  }
}

const openAddModal = () => {
  isEdit.value = false
  Object.assign(form, {
    id: null,
    configKey: '',
    configName: '',
    configValue: '',
    description: '',
    valueType: 'TEXT',
    groupCode: 'basic',
    status: 1
  })
  modalVisible.value = true
}

const openEditModal = (record) => {
  isEdit.value = true
  Object.assign(form, {
    id: record.id,
    configKey: record.configKey || '',
    configName: record.configName || '',
    configValue: record.configValue || '',
    description: record.description || '',
    valueType: record.valueType || 'TEXT',
    groupCode: record.groupCode || 'basic',
    status: record.status === 0 ? 0 : 1
  })
  if (isBuiltInConfig(record)) {
    form.status = 1
  }
  modalVisible.value = true
}

const handleModalOk = async (done) => {
  if (!form.configKey.trim()) {
    Message.error('配置键不能为空')
    done(false)
    return
  }
  if (!form.configName.trim()) {
    Message.error('配置名称不能为空')
    done(false)
    return
  }
  const valueError = validateConfigValue(form.configValue, form.valueType)
  if (valueError) {
    Message.error(valueError)
    done(false)
    return
  }
  try {
    const payload = {
      configKey: form.configKey,
      configName: form.configName,
      configValue: form.configValue,
      description: form.description,
      valueType: form.valueType,
      groupCode: form.groupCode,
      status: form.status
    }
    const res = isEdit.value ? await systemConfigApi.update(form.id, payload) : await systemConfigApi.add(payload)
    if (res.code === 200) {
      Message.success(isEdit.value ? '配置已更新' : '配置已新增')
      modalVisible.value = false
      await fetchConfigs()
      window.dispatchEvent(new CustomEvent('system-config-updated'))
    } else {
      Message.error(res.msg || '操作失败')
      done(false)
      return
    }
  } catch {
    Message.error('操作失败')
    done(false)
    return
  }
  done()
}

const submitConfigForm = () => {
  handleModalOk((result) => {
    if (result === false) return
  })
}

const disableConfig = (record) => {
  if (isBuiltInConfig(record)) {
    Message.warning('内置配置不允许禁用')
    return
  }
  Modal.confirm({
    title: '确认禁用配置',
    content: `确认禁用「${record.configName}」吗？`,
    okText: '禁用',
    okButtonProps: { status: 'danger' },
    async onOk () {
      const res = await systemConfigApi.updateStatus(record.id, 0)
      if (res.code === 200) {
        Message.success('配置已禁用')
        await fetchConfigs()
        window.dispatchEvent(new CustomEvent('system-config-updated'))
      } else {
        Message.error(res.msg || '禁用失败')
      }
    }
  })
}

const enableConfig = async (record) => {
  const res = await systemConfigApi.updateStatus(record.id, 1)
  if (res.code === 200) {
    Message.success('配置已启用')
    await fetchConfigs()
    window.dispatchEvent(new CustomEvent('system-config-updated'))
  } else {
    Message.error(res.msg || '启用失败')
  }
}

const downloadBlob = (blob, fileName) => {
  const objectUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(objectUrl)
}

const groupOption = (value) => groupOptions.value.find(item => item.value === value)
const groupLabel = (value) => groupOption(value)?.label || value || '-'
const groupColor = (value) => groupOption(value)?.color || 'arcoblue'
const groupTone = (value) => colorToTone(groupColor(value))
const valueTypeOption = (value) => valueTypeOptions.value.find(item => item.value === (value || 'TEXT'))
const valueTypeLabel = (value) => valueTypeOption(value)?.label || value || 'TEXT'
const valueTypeColor = (value) => valueTypeOption(value)?.color || 'arcoblue'
const valueTypeTone = (value) => colorToTone(valueTypeColor(value))
const statusTone = (value) => Number(value) === 1 ? 'success' : 'danger'
const formatTime = (value) => value ? value.replace('T', ' ').substring(0, 19) : '-'
const formatShortTime = (value) => {
  if (!value) return '-'
  const text = value.replace('T', ' ').substring(0, 16)
  return text.length >= 16 ? text.substring(5) : text
}

const colorToTone = (color) => {
  if (['green', 'success'].includes(color)) return 'success'
  if (['red', 'danger'].includes(color)) return 'danger'
  if (['gold', 'orange', 'warning'].includes(color)) return 'warning'
  if (['purple', 'gray', 'grey', 'neutral'].includes(color)) return 'neutral'
  return 'primary'
}

const loadValueTypeOptions = async () => {
  valueTypeOptions.value = await loadPublicDictItems('config_value_type', fallbackValueTypeOptions)
}

const loadGroupOptions = async () => {
  groupOptions.value = await loadPublicDictItems('config_group', fallbackGroupOptions)
}

const loadStatusOptions = async () => {
  statusOptions.value = await loadCommonStatusOptions()
}

const formatJsonValue = () => {
  const text = String(form.configValue ?? '').trim()
  if (!text) {
    Message.warning('请先输入 JSON')
    return
  }
  try {
    form.configValue = JSON.stringify(JSON.parse(text), null, 2)
    Message.success('JSON 已格式化')
  } catch {
    Message.error('JSON 类型配置值格式不正确')
  }
}

const validateConfigValue = (value, valueType) => {
  const text = String(value ?? '').trim()
  if (!text || valueType === 'TEXT') return ''
  if (valueType === 'NUMBER') {
    return /^[-+]?(\d+(\.\d*)?|\.\d+)([eE][-+]?\d+)?$/.test(text) ? '' : 'NUMBER 类型配置值必须是数字'
  }
  if (valueType === 'BOOLEAN') {
    return /^(true|false)$/i.test(text) ? '' : 'BOOLEAN 类型配置值必须是 true 或 false'
  }
  if (valueType === 'JSON') {
    try {
      JSON.parse(text)
      return ''
    } catch {
      return 'JSON 类型配置值格式不正确'
    }
  }
  return ''
}

const normalizeWatermarkOpacity = (value) => {
  const next = Number(value)
  if (!Number.isFinite(next)) return 0.12
  if (next < 0.03) return 0.03
  if (next > 0.3) return 0.3
  return next
}

const normalizeWatermarkFontSize = (value) => {
  const next = Number(value)
  if (!Number.isFinite(next)) return 16
  if (next < 10) return 10
  if (next > 32) return 32
  return Math.round(next)
}

const normalizeWatermarkRotate = (value) => {
  const next = Number(value)
  if (!Number.isFinite(next)) return -24
  if (next < -60) return -60
  if (next > 60) return 60
  return Math.round(next)
}

const escapeXml = (value) => {
  return String(value || '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&apos;')
}

onMounted(async () => {
  await applyPaginationConfig(size, pageSizeOptions)
  await Promise.all([loadGroupOptions(), loadValueTypeOptions(), loadStatusOptions()])
  await fetchConfigs()
  window.addEventListener('system-config-updated', refreshRuntimeConfig)
  window.addEventListener('system-dict-updated', refreshRuntimeConfig)
})

onUnmounted(() => {
  window.removeEventListener('system-config-updated', refreshRuntimeConfig)
  window.removeEventListener('system-dict-updated', refreshRuntimeConfig)
})
</script>

<style scoped>
.config-page {
  min-height: 100vh;
  width: 100%;
  background: var(--ds-color-bg-page);
  overflow-x: hidden;
}

.main {
  box-sizing: border-box;
  width: 100%;
  max-width: 1680px;
  min-width: 0;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.filter-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.filter-title__icon {
  width: 24px;
  height: 24px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--ds-color-tag-blue-bg);
  color: var(--ds-color-primary);
}

.filter-fields {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
  min-width: 0;
}

.filter-item {
  flex: 0 0 auto;
}

.filter-item--keyword { width: 280px; }
.filter-item--group { width: 200px; }
.filter-item--status { width: 160px; }

.filter-check {
  min-height: 36px;
  display: flex;
  align-items: center;
  padding-bottom: 1px;
  white-space: nowrap;
}

.filter-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.filter-button {
  min-width: 76px;
  height: 36px;
  border-radius: 8px;
}

.card-shell {
  border: 1px solid color-mix(in srgb, var(--ds-color-primary) 4%, var(--ds-color-border));
  border-radius: 12px;
  background: var(--ds-color-bg-card);
  box-shadow: 0 8px 24px var(--ds-color-shadow);
}

.table-card {
  padding: 16px 20px 20px;
  min-width: 0;
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.section-header--table {
  padding-bottom: 12px;
}

.table-header-copy {
  min-width: 420px;
}

.section-title {
  margin: 0;
  color: var(--ds-color-text-primary);
  font-size: 16px;
  font-weight: 600;
}

.section-description {
  margin: 6px 0 0;
  color: var(--ds-color-text-secondary);
  font-size: 13px;
  line-height: 1.5;
  white-space: nowrap;
}

.table-header-tools {
  width: auto;
  flex: 0 0 auto;
  justify-content: flex-end;
}

.table-header-tools :deep(.ds-action-bar__main),
.table-header-tools :deep(.ds-action-bar__divider) {
  display: none;
}

.table-export-button {
  height: 32px;
  border-radius: 8px;
}

.config-key-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  min-width: 0;
}

.value-cell {
  width: 100%;
  min-width: 0;
}

.value-cell :deep(.arco-trigger) {
  display: block;
  min-width: 0;
}

.value-text,
.config-key {
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.value-text {
  display: block;
  width: 100%;
  max-width: 100%;
}

.config-key {
  max-width: 292px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
}

.config-popover {
  max-width: 260px;
  padding: 2px 0;
}

.config-popover__title {
  margin-bottom: 8px;
  color: var(--ds-color-text-primary);
  font-size: 13px;
  font-weight: 600;
}

.config-popover__text {
  color: var(--ds-color-text-regular);
  font-size: 12px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}

.config-popover--time {
  max-width: 180px;
}

.time-text {
  color: var(--ds-color-text-regular);
  white-space: nowrap;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.row-actions :deep(.arco-btn-text) {
  height: 28px;
  padding: 0 8px;
  border-radius: 6px;
}

.no-actions {
  color: var(--ds-color-text-secondary);
  padding: 0 8px;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
}

.table-card :deep(.arco-table-container) {
  max-width: 100%;
}

.table-card :deep(.arco-table-content),
.table-card :deep(.arco-table-body) {
  overflow-x: auto;
}

.table-card :deep(.arco-table-element) {
  min-width: 100%;
}

.table-card :deep(.arco-table-td),
.table-card :deep(.arco-table-th) {
  padding-left: 12px;
  padding-right: 12px;
  white-space: nowrap;
}

@media (max-width: 960px) {
  .main {
    width: 100%;
    padding: 16px;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .section-header {
    flex-direction: column;
  }

  .section-description {
    white-space: normal;
  }
}
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.form-tip { margin-top: 6px; color: var(--ds-color-text-secondary); font-size: 12px; line-height: 1.5; }
.watermark-preview-section {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  width: 100%;
}
.watermark-preview {
  position: relative;
  width: 100%;
  min-height: 168px;
  border: 1px solid var(--ds-color-border);
  border-radius: 8px;
  overflow: hidden;
  background-color: var(--ds-color-bg-soft);
  background-repeat: repeat;
  background-position: 0 0;
}
.watermark-preview-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, color-mix(in srgb, var(--ds-color-bg-card) 14%, transparent), color-mix(in srgb, var(--ds-color-bg-card) 24%, transparent));
}
.watermark-preview-title {
  margin-top: 10px;
  width: 100%;
  font-size: 14px;
  font-weight: 600;
  color: var(--ds-color-text-primary);
}
.watermark-preview-desc {
  margin-top: 8px;
  width: 100%;
  color: var(--ds-color-text-regular);
  font-size: 12px;
}
.hidden-file-input { display: none; }
</style>
