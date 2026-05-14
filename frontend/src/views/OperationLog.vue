<template>
  <div class="log-page">
    <div class="main">
      <DsPageHeader
        :breadcrumb="['工作台', '系统管理', '操作日志']"
        title="操作日志"
        description="查询系统关键操作、登录安全和权限审计记录，辅助问题追踪与安全复盘。"
      >
        <template #icon><IconHistory /></template>
        <template #meta>
          <span>共 {{ total }} 条日志</span>
          <span>{{ tableData.length }} 条当前页记录</span>
          <span>{{ failedLogCount }} 条失败记录</span>
        </template>
      </DsPageHeader>

      <section class="summary-grid">
        <template v-for="card in summaryCards" :key="card.key">
          <DsSecuritySummaryCard
            v-if="card.key === 'risk'"
            :label="card.label"
            :items="riskSummaryItems"
          >
            <template #icon><component :is="card.icon" /></template>
          </DsSecuritySummaryCard>
          <DsStatsCard
            v-else
            :label="card.label"
            :value="card.value"
            :hint="card.hint"
            :tone="card.tone"
          >
            <template #icon><component :is="card.icon" /></template>
          </DsStatsCard>
        </template>
      </section>

      <DsFilterBar title="筛选查询" description="按关键词、模块、动作、结果和操作时间快速定位日志。">
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
            placeholder="搜索操作人或内容"
            @search="handleSearch"
          />
          <DsDictSelect v-model="filters.module" type-code="operation_log_module" label="模块" placeholder="全部模块" class="filter-item filter-item--module" @update:model-value="handleSearch" />
          <DsDictSelect v-model="filters.action" type-code="operation_log_action" label="动作" placeholder="全部动作" class="filter-item filter-item--action" @update:model-value="handleSearch" />
          <DsDictSelect v-model="filters.success" type-code="success_status" label="结果" placeholder="全部结果" class="filter-item filter-item--result" @update:model-value="handleSearch" />
          <DsDateRangePicker v-model="dateRange" label="操作时间" class="filter-item filter-item--date" @update:model-value="handleSearch" />
          <a-button type="text" class="filter-expand-button filter-expand-button--inline" @click="showAdvancedFilters = !showAdvancedFilters">
            {{ showAdvancedFilters ? '收起更多' : '展开更多' }}
            <template #icon><IconDown :class="{ 'filter-expand-button__icon--expanded': showAdvancedFilters }" /></template>
          </a-button>
        </div>
        <template #actions>
          <div class="filter-actions">
            <a-button type="primary" class="filter-button" @click="handleSearch">查询</a-button>
            <a-button class="filter-button" @click="resetFilters">重置</a-button>
          </div>
        </template>
      </DsFilterBar>
      <DsAdvancedFilterPanel v-if="showAdvancedFilters" class="advanced-filter-panel">
        <div class="advanced-filter-fields">
          <div class="advanced-filter-item">
            <label>操作人</label>
            <a-input placeholder="开发中，暂不参与查询" disabled />
          </div>
          <div class="advanced-filter-item">
            <label>请求路径</label>
            <a-input placeholder="开发中，暂不参与查询" disabled />
          </div>
          <div class="advanced-filter-item">
            <label>IP 地址</label>
            <a-input placeholder="开发中，暂不参与查询" disabled />
          </div>
          <div class="advanced-filter-item">
            <label>失败原因</label>
            <a-input placeholder="开发中，暂不参与查询" disabled />
          </div>
        </div>
        <div class="advanced-filter-placeholder">
          <span>更多审计筛选位已预留，本轮不新增接口参数。</span>
        </div>
      </DsAdvancedFilterPanel>

      <section class="table-card card-shell">
        <div class="section-header section-header--table">
          <div class="table-header-copy">
            <h2 class="section-title">日志列表</h2>
            <p class="section-description">共 {{ total }} 条，支持分页查询、审计追踪和日志详情查看。</p>
          </div>
          <DsActionBar
            class="table-header-tools"
            :selection-text="`当前页 ${tableData.length} 条`"
            :selection-active="tableData.length > 0"
          >
            <template #tools>
              <a-button v-if="canExportLogs" class="table-export-button" @click="handleExport">导出日志</a-button>
              <DsIconButton tooltip="刷新日志" @click="fetchLogs"><IconRefresh /></DsIconButton>
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
        <template #module="{ record }">
          <DsStatusTag :label="moduleLabel(record.module)" :tone="moduleTone(record.module)" />
        </template>
        <template #action="{ record }">
          <DsStatusTag :label="actionLabel(record.action)" :tone="actionTone(record.action)" />
        </template>
        <template #success="{ record }">
          <DsStatusTag :label="resultLabel(record.success)" :tone="Number(record.success) === 1 ? 'success' : 'danger'" />
        </template>
        <template #content="{ record }">
          <a-popover v-if="record.errorMessage || record.content" trigger="hover" position="top" class="log-content-popover">
            <template #content>
              <div class="log-popover">
                <div class="log-popover__title">{{ record.errorMessage ? '错误信息' : '操作内容' }}</div>
                <div class="log-popover__text" :class="{ 'log-popover__text--danger': record.errorMessage }">
                  {{ record.errorMessage || record.content }}
                </div>
              </div>
            </template>
            <span class="content-text">{{ record.errorMessage || record.content || '-' }}</span>
          </a-popover>
          <span v-else>-</span>
        </template>
        <template #createdAt="{ record }">{{ formatTime(record.createdAt) }}</template>
        <template #actions="{ record }">
          <div class="row-actions">
            <a-button type="text" size="small" @click="openDetail(record)">查看</a-button>
          </div>
        </template>
        <template #empty>
          <DsEmptyState title="暂无操作日志" description="当前筛选条件下没有可展示的操作日志。" />
        </template>
      </DsDataTable>
      </section>
    </div>

    <DsModalForm
      v-model:visible="detailVisible"
      title="操作日志详情"
      description="查看单条日志的操作人、请求信息和执行结果。"
      :width="680"
      @cancel="closeDetail"
    >
      <DsFormSection v-if="detailRecord" title="基础信息">
        <div class="detail-grid">
          <span>操作时间</span>
          <span>{{ formatTime(detailRecord.createdAt) }}</span>
          <span v-if="canReadField('operator')">操作人</span>
          <span v-if="canReadField('operator')">{{ detailRecord.operatorUsername || '-' }}</span>
          <span>模块</span>
          <span><DsStatusTag :label="moduleLabel(detailRecord.module)" :tone="moduleTone(detailRecord.module)" /></span>
          <span>动作</span>
          <span><DsStatusTag :label="actionLabel(detailRecord.action)" :tone="actionTone(detailRecord.action)" /></span>
          <span>结果</span>
          <span><DsStatusTag :label="resultLabel(detailRecord.success)" :tone="Number(detailRecord.success) === 1 ? 'success' : 'danger'" /></span>
        </div>
      </DsFormSection>

      <DsFormSection v-if="detailRecord && (canReadField('request') || canReadField('ip'))" title="请求信息">
        <div class="detail-grid">
          <span v-if="canReadField('request')">请求方法</span>
          <span v-if="canReadField('request')">{{ detailRecord.requestMethod || '-' }}</span>
          <span v-if="canReadField('request')">请求路径</span>
          <span v-if="canReadField('request')" class="mono-text">{{ detailRecord.requestPath || '-' }}</span>
          <span v-if="canReadField('ip')">IP 地址</span>
          <span v-if="canReadField('ip')">{{ detailRecord.ip || '-' }}</span>
        </div>
      </DsFormSection>

      <DsFormSection v-if="detailRecord && canReadField('content')" title="操作内容">
        <div v-if="canReadField('content')" class="detail-block">
          <div class="detail-text">{{ detailRecord.content || '-' }}</div>
        </div>
      </DsFormSection>

      <DsFormSection v-if="detailRecord && canReadField('content') && detailRecord.errorMessage" title="错误信息">
        <div v-if="canReadField('content') && detailRecord.errorMessage" class="detail-block">
          <div class="detail-text danger-box">{{ detailRecord.errorMessage }}</div>
        </div>
      </DsFormSection>
      <template #footer>
        <div class="detail-footer">
          <a-button type="primary" class="detail-close-button" @click="closeDetail">关闭</a-button>
        </div>
      </template>
    </DsModalForm>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import {
  IconCheckCircle,
  IconDown,
  IconExclamationCircle,
  IconFilter,
  IconHistory,
  IconRefresh,
  IconSettings,
  IconUser
} from '@arco-design/web-vue/es/icon'
import { operationLogApi } from '@/api'
import { hasPermission } from '@/utils/permission'
import { DEFAULT_PAGE_SIZE_OPTIONS, applyPaginationConfig } from '@/utils/systemConfig'
import { loadPublicDictItems } from '@/utils/dict'
import {
  DsActionBar,
  DsAdvancedFilterPanel,
  DsDateRangePicker,
  DsDataTable,
  DsDictSelect,
  DsEmptyState,
  DsFilterBar,
  DsFormSection,
  DsKeywordSearch,
  DsModalForm,
  DsPageHeader,
  DsSecuritySummaryCard,
  DsStatsCard,
  DsIconButton,
  DsStatusTag
} from '@/design-system'

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const size = ref(20)
const pageSizeOptions = ref([...DEFAULT_PAGE_SIZE_OPTIONS])
const total = ref(0)
const tableScrollX = 1290
const readableFields = ref(['operator', 'ip', 'content', 'request'])
const dateRange = ref([])
const detailVisible = ref(false)
const detailRecord = ref(null)
const showAdvancedFilters = ref(false)
const filters = reactive({ keyword: '', module: '', action: '', success: '' })

const fallbackModuleOptions = [
  { itemLabel: '部门管理', itemValue: 'DEPARTMENT', tagColor: 'arcoblue' },
  { itemLabel: '用户管理', itemValue: 'USER', tagColor: 'green' },
  { itemLabel: '角色权限', itemValue: 'ROLE', tagColor: 'purple' },
  { itemLabel: '系统配置', itemValue: 'CONFIG', tagColor: 'gold' },
  { itemLabel: '字典管理', itemValue: 'DICT', tagColor: 'orangered' },
  { itemLabel: '登录认证', itemValue: 'AUTH', tagColor: 'gray' },
  { itemLabel: '文件中心', itemValue: 'FILE', tagColor: 'purple' },
  { itemLabel: '消息中心', itemValue: 'MESSAGE', tagColor: 'gold' },
  { itemLabel: '待办中心', itemValue: 'TODO', tagColor: 'orangered' },
  { itemLabel: '操作日志', itemValue: 'LOG', tagColor: 'gray' }
]

const fallbackActionOptions = [
  { itemLabel: '新增', itemValue: 'CREATE', tagColor: 'green' },
  { itemLabel: '编辑', itemValue: 'UPDATE', tagColor: 'arcoblue' },
  { itemLabel: '删除', itemValue: 'DELETE', tagColor: 'red' },
  { itemLabel: '启用', itemValue: 'ENABLE', tagColor: 'green' },
  { itemLabel: '禁用', itemValue: 'DISABLE', tagColor: 'red' },
  { itemLabel: '状态变更', itemValue: 'STATUS', tagColor: 'gold' },
  { itemLabel: '重置密码', itemValue: 'RESET_PASSWORD', tagColor: 'gold' },
  { itemLabel: '调部门', itemValue: 'MOVE_DEPT', tagColor: 'arcoblue' },
  { itemLabel: '分配角色', itemValue: 'ASSIGN_ROLES', tagColor: 'purple' },
  { itemLabel: '保存功能权限', itemValue: 'SAVE_FEATURES', tagColor: 'purple' },
  { itemLabel: '保存数据范围', itemValue: 'SAVE_DATA_SCOPE', tagColor: 'purple' },
  { itemLabel: '保存字段权限', itemValue: 'SAVE_FIELDS', tagColor: 'purple' },
  { itemLabel: '登录', itemValue: 'LOGIN', tagColor: 'green' },
  { itemLabel: '登录失败', itemValue: 'LOGIN_FAILED', tagColor: 'red' },
  { itemLabel: '账号锁定', itemValue: 'ACCOUNT_LOCKED', tagColor: 'red' },
  { itemLabel: '退出登录', itemValue: 'LOGOUT', tagColor: 'gray' },
  { itemLabel: '修改个人密码', itemValue: 'CHANGE_PASSWORD', tagColor: 'gold' },
  { itemLabel: '权限拒绝', itemValue: 'PERMISSION_DENIED', tagColor: 'red' },
  { itemLabel: '导出', itemValue: 'EXPORT', tagColor: 'arcoblue' },
  { itemLabel: '导入', itemValue: 'IMPORT', tagColor: 'cyan' },
  { itemLabel: '新增字典类型', itemValue: 'CREATE_TYPE', tagColor: 'green' },
  { itemLabel: '编辑字典类型', itemValue: 'UPDATE_TYPE', tagColor: 'arcoblue' },
  { itemLabel: '启用字典类型', itemValue: 'ENABLE_TYPE', tagColor: 'green' },
  { itemLabel: '禁用字典类型', itemValue: 'DISABLE_TYPE', tagColor: 'red' },
  { itemLabel: '字典类型状态', itemValue: 'STATUS_TYPE', tagColor: 'gold' },
  { itemLabel: '新增字典项', itemValue: 'CREATE_ITEM', tagColor: 'green' },
  { itemLabel: '编辑字典项', itemValue: 'UPDATE_ITEM', tagColor: 'arcoblue' },
  { itemLabel: '启用字典项', itemValue: 'ENABLE_ITEM', tagColor: 'green' },
  { itemLabel: '禁用字典项', itemValue: 'DISABLE_ITEM', tagColor: 'red' },
  { itemLabel: '字典项状态', itemValue: 'STATUS_ITEM', tagColor: 'gold' }
]
const fallbackResultOptions = [
  { itemLabel: '成功', itemValue: 'success', tagColor: 'green' },
  { itemLabel: '失败', itemValue: 'failed', tagColor: 'red' }
]

const moduleOptions = ref([])
const actionOptions = ref([])
const resultOptions = ref([])
const canExportLogs = hasPermission('system_log:export')
const paginationConfig = computed(() => ({
  current: page.value,
  pageSize: size.value,
  total: total.value,
  pageSizeOptions: pageSizeOptions.value
}))
const localDateText = (value = new Date()) => {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
const todayLogCount = computed(() => {
  const today = localDateText()
  return tableData.value.filter(item => String(item.createdAt || '').slice(0, 10) === today).length
})
const failedLogCount = computed(() => tableData.value.filter(item => Number(item.success) !== 1).length)
const authLogCount = computed(() => tableData.value.filter(item => item.module === 'AUTH').length)
const permissionDeniedCount = computed(() => tableData.value.filter(item => item.action === 'PERMISSION_DENIED').length)
const lockedCount = computed(() => tableData.value.filter(item => item.action === 'ACCOUNT_LOCKED').length)
const riskSummaryItems = computed(() => [
  { label: '失败 / 异常', value: `${failedLogCount.value} 条` },
  { label: '权限拒绝', value: `${permissionDeniedCount.value} 条` },
  { label: '账号锁定', value: `${lockedCount.value} 条` }
])
const summaryCards = computed(() => [
  { key: 'total', label: '日志总数', value: total.value, hint: '当前筛选结果总量', tone: 'primary', icon: IconHistory },
  { key: 'today', label: '今日操作', value: todayLogCount.value, hint: '当前页今日记录', tone: 'success', icon: IconCheckCircle },
  { key: 'failed', label: '失败操作', value: failedLogCount.value, hint: '当前页失败记录', tone: failedLogCount.value > 0 ? 'danger' : 'success', icon: IconExclamationCircle },
  { key: 'auth', label: '登录认证', value: authLogCount.value, hint: '当前页认证记录', tone: 'warning', icon: IconUser },
  { key: 'risk', label: '风险提醒', icon: IconExclamationCircle }
])

const baseColumns = [
  { title: '序号', width: 96, align: 'center', slotName: 'index' },
  { title: '时间', slotName: 'createdAt', width: 190 },
  { title: '操作人', dataIndex: 'operatorUsername', width: 170, fieldCode: 'operator' },
  { title: '模块', slotName: 'module', width: 130 },
  { title: '动作', slotName: 'action', width: 130 },
  { title: '结果', slotName: 'success', width: 110 },
  { title: '内容', slotName: 'content', minWidth: 360, fieldCode: 'content' },
  { title: '操作', slotName: 'actions', width: 100 }
]

const columns = computed(() => baseColumns.filter(column => !column.fieldCode || readableFields.value.includes(column.fieldCode)))
const canReadField = (fieldCode) => readableFields.value.includes(fieldCode)

const fetchLogs = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: size.value,
      keyword: filters.keyword?.trim() || undefined,
      module: filters.module || undefined,
      action: filters.action || undefined,
      success: resultFilterValue(filters.success),
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined
    }
    const res = await operationLogApi.list(params)
    if (res.code === 200) {
      tableData.value = res.data?.records || []
      total.value = res.data?.total || 0
      readableFields.value = Array.isArray(res.data?.readableFields) ? res.data.readableFields : readableFields.value
    } else {
      Message.error(res.msg || '加载操作日志失败')
    }
  } catch {
    Message.error('加载操作日志失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchLogs()
}

const handlePageChange = (nextPage) => {
  page.value = nextPage
  fetchLogs()
}

const handleSizeChange = (nextSize) => {
  size.value = nextSize
  page.value = 1
  fetchLogs()
}

const handleExport = async () => {
  try {
    const params = {
      keyword: filters.keyword?.trim() || undefined,
      module: filters.module || undefined,
      action: filters.action || undefined,
      success: resultFilterValue(filters.success),
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined
    }
    const blob = await operationLogApi.export(params)
    const objectUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = `操作日志_${new Date().toISOString().slice(0, 10).replace(/-/g, '')}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(objectUrl)
    Message.success('操作日志已开始导出')
  } catch {
    Message.error('导出失败')
  }
}

const refreshRuntimeConfig = async () => {
  await applyPaginationConfig(size, pageSizeOptions)
  await loadLogDictOptions()
  page.value = 1
  await fetchLogs()
}

const resetFilters = () => {
  Object.assign(filters, { keyword: '', module: '', action: '', success: '' })
  dateRange.value = []
  handleSearch()
}

const openDetail = (record) => {
  detailRecord.value = record
  detailVisible.value = true
}

const closeDetail = () => {
  detailVisible.value = false
  detailRecord.value = null
}

const optionList = (options) => Array.isArray(options) ? options : (options?.value || [])
const findOption = (options, value) => optionList(options).find(item => item.value === value)
const moduleLabel = (value) => findOption(moduleOptions, value)?.label || value || '-'
const actionLabel = (value) => findOption(actionOptions, value)?.label || value || '-'
const resultKey = (value) => Number(value) === 1 ? 'success' : 'failed'
const resultLabel = (value) => findOption(resultOptions, resultKey(value))?.label || (Number(value) === 1 ? '成功' : '失败')
const resultFilterValue = (value) => {
  if (value === '') return undefined
  if (value === 'success' || value === '1') return 1
  if (value === 'failed' || value === '0') return 0
  return value
}
const formatTime = (value) => value ? value.replace('T', ' ').substring(0, 19) : '-'
const colorToTone = (color) => {
  if (color === 'green') return 'success'
  if (color === 'red' || color === 'orangered') return 'danger'
  if (color === 'gold' || color === 'orange') return 'warning'
  if (color === 'gray') return 'neutral'
  return 'primary'
}
const actionTone = (value) => {
  if (['DELETE', 'DISABLE', 'LOGIN_FAILED', 'ACCOUNT_LOCKED', 'PERMISSION_DENIED', 'DISABLE_TYPE', 'DISABLE_ITEM'].includes(value)) return 'danger'
  if (['CREATE', 'ENABLE', 'LOGIN', 'CREATE_TYPE', 'CREATE_ITEM', 'ENABLE_TYPE', 'ENABLE_ITEM'].includes(value)) return 'success'
  if (['RESET_PASSWORD', 'STATUS', 'STATUS_TYPE', 'STATUS_ITEM'].includes(value)) return 'warning'
  return colorToTone(findOption(actionOptions, value)?.color)
}
const moduleTone = (value) => colorToTone(findOption(moduleOptions, value)?.color)

const loadLogDictOptions = async () => {
  const [modules, actions, results] = await Promise.all([
    loadPublicDictItems('operation_log_module', fallbackModuleOptions),
    loadPublicDictItems('operation_log_action', fallbackActionOptions),
    loadPublicDictItems('success_status', fallbackResultOptions)
  ])
  moduleOptions.value = modules
  actionOptions.value = actions
  resultOptions.value = results
}

onMounted(async () => {
  await applyPaginationConfig(size, pageSizeOptions)
  await loadLogDictOptions()
  await fetchLogs()
  window.addEventListener('system-config-updated', refreshRuntimeConfig)
  window.addEventListener('system-dict-updated', refreshRuntimeConfig)
  window.addEventListener('user-context-refreshed', fetchLogs)
})

onUnmounted(() => {
  window.removeEventListener('system-config-updated', refreshRuntimeConfig)
  window.removeEventListener('system-dict-updated', refreshRuntimeConfig)
  window.removeEventListener('user-context-refreshed', fetchLogs)
})
</script>

<style scoped>
.log-page { min-height: 100vh; overflow-x: hidden; background: var(--ds-color-bg-page); }
.main { box-sizing: border-box; max-width: 1680px; width: 100%; min-width: 0; margin: 0 auto; padding: 24px; display: grid; gap: 16px; }
.summary-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 16px; align-items: stretch; }
.filter-title { display: inline-flex; align-items: center; gap: 8px; }
.filter-title__icon { width: 18px; height: 18px; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center; background: var(--ds-color-bg-selected); color: var(--ds-color-primary); flex: 0 0 auto; }
.filter-title__icon :deep(svg) { width: 14px; height: 14px; }
.filter-fields { display: flex; align-items: flex-end; gap: 16px; flex-wrap: wrap; min-width: 0; }
.filter-item { flex: 0 0 auto; min-width: 0; }
.filter-item--keyword { width: 260px; }
.filter-item--module { width: 160px; }
.filter-item--action { width: 160px; }
.filter-item--result { width: 130px; }
.filter-item--date { width: 280px; }
.filter-actions { display: flex; align-items: center; gap: 8px; justify-content: flex-end; }
.filter-expand-button {
  height: 36px;
  align-self: flex-end;
  padding: 0 4px;
  border-radius: 8px;
  color: var(--ds-color-text-regular);
}
.filter-expand-button__icon--expanded { transform: rotate(180deg); }
.filter-button { min-width: 76px; height: 36px; border-radius: 8px; }
.advanced-filter-panel { margin-top: -4px; }
.advanced-filter-fields { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px 16px; width: 100%; }
.advanced-filter-item { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.advanced-filter-item label { color: var(--ds-color-text-regular); font-size: 13px; font-weight: 500; line-height: 1.4; }
.advanced-filter-placeholder { margin-top: 10px; color: var(--ds-color-text-secondary); font-size: 12px; line-height: 1.6; }
.card-shell {
  min-width: 0;
  background: var(--ds-color-bg-card);
  border: 1px solid color-mix(in srgb, var(--ds-color-primary) 4%, var(--ds-color-border));
  border-radius: 12px;
  box-shadow: 0 8px 24px var(--ds-color-shadow);
}
.table-card { overflow: hidden; padding: 16px 20px 12px; }
.section-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 12px; }
.section-header--table { margin-bottom: 12px; }
.table-header-copy { min-width: 0; flex: 1 1 360px; }
.section-title { margin: 0; color: var(--ds-color-text-primary); font-size: 18px; line-height: 1.3; font-weight: 600; }
.section-description { margin: 8px 0 0; color: var(--ds-color-text-secondary); font-size: 13px; line-height: 1.6; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.table-header-tools { display: flex; align-items: center; justify-content: flex-end; gap: 8px; flex: 0 1 auto; min-width: 0; max-width: 100%; }
.table-header-tools :deep(.ds-action-bar) { width: auto; max-width: 100%; flex-shrink: 1; flex-wrap: nowrap; justify-content: flex-end; }
.table-header-tools :deep(.ds-action-bar__selection) { flex: 0 0 auto; }
.table-header-tools :deep(.ds-action-bar__tools) { flex: 0 1 auto; min-width: 0; }
.table-header-tools :deep(.ds-action-bar__main) { display: none; }
.table-header-tools :deep(.ds-action-bar__divider) { display: none; }
.table-export-button { height: 32px; border-radius: 8px; }
.table-card :deep(.ds-data-table) { min-width: 0; }
.table-card :deep(.arco-table-container),
.table-card :deep(.arco-table-content),
.table-card :deep(.arco-table-scroll) { max-width: 100%; }
.table-card :deep(.arco-table-th:first-child),
.table-card :deep(.arco-table-td:first-child) {
  padding-right: 8px;
  padding-left: 8px;
  text-align: center;
}
.table-card :deep(.arco-table-th:first-child .arco-table-th-title),
.table-card :deep(.arco-table-td:first-child .arco-table-td-content) {
  overflow: visible;
  text-overflow: clip;
}
.content-text { display: inline-block; max-width: 340px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; vertical-align: bottom; }
.row-actions { display: flex; align-items: center; gap: 4px; white-space: nowrap; }
.row-actions :deep(.arco-btn-text) { height: 28px; padding: 0 10px; border-radius: 8px; color: var(--ds-color-primary); }
.row-actions :deep(.arco-btn-text:hover) { background: var(--ds-color-bg-hover); }
.log-popover { max-width: 320px; padding: 2px 0; }
.log-popover__title { margin-bottom: 8px; color: var(--ds-color-text-primary); font-size: 13px; font-weight: 600; }
.log-popover__text { color: var(--ds-color-text-regular); font-size: 12px; line-height: 1.7; white-space: pre-wrap; word-break: break-word; }
.log-popover__text--danger { color: var(--ds-color-danger); }
.detail-grid { display: grid; grid-template-columns: 96px 1fr; gap: 10px 12px; align-items: center; }
.detail-grid span:nth-child(odd) { color: var(--ds-color-text-secondary); }
.detail-block { margin-top: 0; }
.detail-title { margin-bottom: 8px; color: var(--ds-color-text-regular); font-weight: 600; }
.detail-text { padding: 10px 12px; background: var(--ds-color-bg-soft); border-radius: 8px; white-space: pre-wrap; word-break: break-word; line-height: 1.7; color: var(--ds-color-text-regular); }
.danger-box { background: var(--ds-color-tag-red-bg); color: var(--ds-color-tag-red-text); }
.mono-text { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace; word-break: break-all; }
.detail-footer { display: flex; justify-content: flex-end; width: 100%; }
.detail-close-button { min-width: 84px; height: 36px; border-radius: 8px; }
@media (max-width: 1440px) {
  .summary-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 1080px) {
  .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .advanced-filter-fields { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .section-description { white-space: normal; }
}
@media (max-width: 720px) {
  .main { padding: 16px; }
  .summary-grid { grid-template-columns: 1fr; }
  .filter-fields { width: 100%; }
  .filter-item,
  .filter-item--keyword,
  .filter-item--module,
  .filter-item--action,
  .filter-item--result,
  .filter-item--date { width: 100%; }
  .table-header-copy { min-width: 0; }
  .section-header { flex-direction: column; }
  .advanced-filter-fields { grid-template-columns: 1fr; }
}
</style>
