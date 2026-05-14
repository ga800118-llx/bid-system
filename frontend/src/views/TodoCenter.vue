<template>
  <div class="todo-page">
    <DsPageHeader
      :breadcrumb="['个人工作', '待办中心']"
      title="待办中心"
      description="集中处理个人待办、跟踪优先级、截止时间和处理状态。"
    >
      <template #actions>
        <DsHeaderActions
          :tool-items="todoToolItems"
          primary-label="新增待办"
          :show-primary="canCreateTodo"
          @tool-select="handleHeaderTool"
          @primary-click="openCreateModal"
        />
      </template>
      <template #meta>
        <span>待处理 {{ summary.pending }}</span>
        <span>处理中 {{ summary.processing }}</span>
        <span>已完成 {{ summary.done }}</span>
        <span>已逾期 {{ summary.overdue }}</span>
      </template>
    </DsPageHeader>

    <DsFilterBar title="筛选查询" description="按标题内容、类型、优先级、状态和截止时间快速定位待办。">
      <DsKeywordSearch
        v-model="filters.keyword"
        placeholder="搜索标题或内容"
        @search="handleSearch"
      />
      <DsDictSelect
        v-model="filters.todoType"
        type-code="todo_type"
        class="filter-select"
        placeholder="全部类型"
      />
      <DsDictSelect
        v-model="filters.priority"
        type-code="todo_priority"
        class="filter-select"
        placeholder="全部优先级"
      />
      <DsDictSelect
        v-model="filters.status"
        type-code="todo_status"
        class="filter-select"
        placeholder="全部状态"
      />
      <label class="overdue-checkbox">
        <a-checkbox v-model="filters.overdueOnly" @change="handleSearch">仅看逾期</a-checkbox>
      </label>
      <DsDateRangePicker
        v-model="filters.dateRange"
        class="filter-date"
      />
      <template #actions>
        <a-button type="primary" class="query-button" @click="handleSearch">查询</a-button>
        <a-button class="query-button" @click="resetFilters">重置</a-button>
      </template>
    </DsFilterBar>

    <section class="list-card ds-card">
      <div class="list-card__header">
        <div>
          <h3 class="list-card__title">待办列表</h3>
          <p class="list-card__description">共 {{ total }} 条，支持状态流转、逾期识别和分页查询。</p>
        </div>
        <div class="list-card__tools">
          <DsIconButton tooltip="刷新" @click="fetchTodos"><IconRefresh /></DsIconButton>
        </div>
      </div>

      <DsDataTable
        :columns="columns"
        :data="tableData"
        :loading="loading"
        row-key="id"
        :row-class="rowClassName"
        :pagination="paginationConfig"
        :scroll="{ x: 1280 }"
        @pagination-change="handlePageChange"
        @page-size-change="handleSizeChange"
      >
        <template #index="{ rowIndex }">{{ (page - 1) * size + rowIndex + 1 }}</template>
        <template #title="{ record }">
          <div class="todo-title-cell">
            <span class="todo-title">{{ record.title }}</span>
            <span v-if="record.bizType" class="todo-biz">{{ record.bizType }}{{ record.bizId ? ` #${record.bizId}` : '' }}</span>
          </div>
        </template>
        <template #todoType="{ record }">
          <DsStatusTag :label="todoTypeLabel(record.todoType)" :tone="tagTone(todoTypeColor(record.todoType))" />
        </template>
        <template #priority="{ record }">
          <div class="priority-cell">
            <DsStatusTag :label="priorityLabel(record.priority)" :tone="tagTone(priorityColor(record.priority))" />
            <DsPriorityStars :value="priorityStars(record.priority)" :max="4" />
          </div>
        </template>
        <template #status="{ record }">
          <div class="status-cell">
            <DsStatusTag :label="statusLabel(record.status)" :tone="tagTone(statusColor(record.status))" />
            <DsStatusTag v-if="record.overdue" label="已逾期" tone="danger" />
          </div>
        </template>
        <template #assigneeName="{ record }">{{ record.assigneeName || '-' }}</template>
        <template #dueAt="{ record }">
          <div class="due-cell" :class="{ overdue: record.overdue, dueToday: isDueToday(record.dueAt) && !record.overdue }">
            <span>{{ formatTime(record.dueAt, '未设置') }}</span>
            <span v-if="record.overdue" class="due-note">已逾期</span>
            <span v-else-if="isDueToday(record.dueAt)" class="due-note">今日到期</span>
          </div>
        </template>
        <template #createdAt="{ record }">{{ formatTime(record.createdAt) }}</template>
        <template #actions="{ record }">
          <div class="row-actions">
            <a-button type="text" size="small" @click="openDetail(record)">查看</a-button>
            <a-dropdown>
              <a-button type="text" size="small">
                更多 <IconDown />
              </a-button>
              <template #content>
                <a-doption v-if="record.status === 'PENDING'" @click="changeStatus(record, 'PROCESSING')">开始处理</a-doption>
                <a-doption v-if="record.status !== 'DONE'" @click="changeStatus(record, 'DONE')">完成</a-doption>
                <a-doption v-if="record.status === 'DONE'" @click="changeStatus(record, 'PENDING')">重新打开</a-doption>
              </template>
            </a-dropdown>
          </div>
        </template>
        <template #empty>
          <DsEmptyState title="暂无待办事项" description="当前条件下没有待处理或历史待办。" />
        </template>
      </DsDataTable>
    </section>

    <a-drawer :visible="detailVisible" :width="680" title="待办详情" unmount-on-close @cancel="closeDetail">
      <div v-if="detailRecord" class="detail-content">
        <div class="detail-header">
          <DsStatusTag :label="todoTypeLabel(detailRecord.todoType)" :tone="tagTone(todoTypeColor(detailRecord.todoType))" />
          <DsStatusTag :label="priorityLabel(detailRecord.priority)" :tone="tagTone(priorityColor(detailRecord.priority))" />
          <DsStatusTag :label="statusLabel(detailRecord.status)" :tone="tagTone(statusColor(detailRecord.status))" />
        </div>
        <h2 class="detail-title">{{ detailRecord.title }}</h2>
        <div class="detail-meta">
          <span>创建人：{{ detailRecord.creatorName || '-' }}</span>
          <span>处理人：{{ assigneeNames(detailRecord) }}</span>
          <span>截止时间：{{ formatTime(detailRecord.dueAt, '未设置') }}</span>
          <span>完成时间：{{ formatTime(detailRecord.processedAt, '未完成') }}</span>
          <span v-if="detailRecord.bizType">业务标识：{{ detailRecord.bizType }}{{ detailRecord.bizId ? ` #${detailRecord.bizId}` : '' }}</span>
        </div>
        <div
          class="detail-body"
          :class="{ 'is-html': detailRecord.contentType === 'HTML' }"
          v-html="detailRecord.contentType === 'HTML' ? sanitizeHtml(detailRecord.content) : plainText(detailRecord.content)"
        ></div>
        <div v-if="detailRecord.attachments?.length" class="detail-attachments">
          <div class="detail-attachments__title">附件</div>
          <div v-for="file in detailRecord.attachments" :key="file.linkId || file.fileId || file.id" class="detail-attachment">
            <span class="detail-attachment__icon"><FileTypeIcon :type="file.extension" /></span>
            <span class="detail-attachment__name">{{ file.originalName || '-' }}</span>
            <span class="detail-attachment__size">{{ formatFileSize(file.fileSize) }}</span>
            <a-button v-if="file.previewable" type="text" size="small" @click="previewAttachment(file)">预览</a-button>
            <a-button type="text" size="small" @click="downloadAttachment(file)">下载</a-button>
          </div>
        </div>
        <div class="detail-actions">
          <a-button v-if="detailRecord.status === 'PENDING'" @click="changeStatus(detailRecord, 'PROCESSING')">开始处理</a-button>
          <a-button v-if="detailRecord.status !== 'DONE'" type="primary" @click="changeStatus(detailRecord, 'DONE')">完成待办</a-button>
          <a-button v-if="detailRecord.status === 'DONE'" @click="changeStatus(detailRecord, 'PENDING')">重新打开</a-button>
        </div>
      </div>
    </a-drawer>

    <DsModalForm
      :visible="createVisible"
      title="新增待办"
      description="创建需要处理人跟进的待办事项，并设置类型、优先级、处理人和截止时间。"
      :width="880"
      @cancel="closeCreateModal"
    >
      <DsFormSection title="基础信息" description="设置待办标题、类型、优先级和截止时间。">
        <DsFormGrid>
          <DsInput v-model="createForm.title" label="待办标题" required placeholder="请输入待办标题" />
          <DsDictSelect v-model="createForm.todoType" type-code="todo_type" label="待办类型" required />
          <DsDictSelect v-model="createForm.priority" type-code="todo_priority" label="优先级" required />
          <DsDatePicker
            v-model="createForm.dueAt"
            label="截止时间"
            show-time
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm"
            placeholder="请选择截止时间"
          />
          <DsInput v-model="createForm.bizType" label="业务类型" placeholder="如 project / contract" />
          <DsNumberInput v-model="createForm.bizId" label="业务 ID" :min="1" />
        </DsFormGrid>
      </DsFormSection>
      <DsFormSection title="处理范围" description="可按部门树选择多个处理人，每位处理人的待办状态独立流转。">
        <MessageRecipientPicker
          v-model="createForm.assigneeIds"
          label="处理人"
          required
        />
      </DsFormSection>
      <DsFormSection title="待办内容" description="支持富文本、图片粘贴和附件。">
        <DsRichTextEditor
          v-model="createForm.content"
          label="待办内容"
          required
          hint="支持富文本编辑和图片粘贴。"
          :upload-image="uploadRichImage"
        />
        <MessageAttachmentUploader v-model="createForm.attachmentFileIds" label="附件" />
      </DsFormSection>
      <template #footer>
        <DsFormActions submit-text="保存" @cancel="closeCreateModal" @submit="submitTodo" />
      </template>
    </DsModalForm>

    <a-modal
      v-model:visible="previewVisible"
      :title="previewTitle"
      :width="960"
      :footer="false"
      unmount-on-close
      @cancel="closePreview"
    >
      <div class="preview-shell">
        <a-spin v-if="previewLoading" />
        <template v-else>
          <img v-if="previewType === 'image' && previewUrl" :src="previewUrl" alt="预览图片" class="preview-image" />
          <iframe v-else-if="previewType === 'pdf' && previewUrl" :src="previewUrl" class="preview-pdf"></iframe>
          <DsEmptyState v-else title="暂不支持在线预览" description="当前文件类型暂不支持在线预览，请使用下载查看。" />
        </template>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { IconDown, IconRefresh } from '@arco-design/web-vue/es/icon'
import { dictApi, fileApi, todoApi } from '@/api'
import { hasPermission } from '@/utils/permission'
import { DEFAULT_PAGE_SIZE_OPTIONS, applyPaginationConfig } from '@/utils/systemConfig'
import FileTypeIcon from '@/design-system/icons/FileTypeIcon.vue'
import MessageAttachmentUploader from '@/components/message/MessageAttachmentUploader.vue'
import MessageRecipientPicker from '@/components/message/MessageRecipientPicker.vue'
import {
  DsDataTable,
  DsDatePicker,
  DsDateRangePicker,
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
  DsPriorityStars,
  DsRichTextEditor,
  DsStatusTag
} from '@/design-system'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const pageSizeOptions = ref([...DEFAULT_PAGE_SIZE_OPTIONS])
const todoTypeOptions = ref([])
const priorityOptions = ref([])
const statusOptions = ref([])
const detailVisible = ref(false)
const detailRecord = ref(null)
const createVisible = ref(false)
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewUrl = ref('')
const previewType = ref('')
const previewTitle = ref('附件预览')
const summary = reactive({ pending: 0, processing: 0, done: 0, overdue: 0 })
const route = useRoute()
const router = useRouter()
const canCreateTodo = hasPermission('system_todo:create')
const canExportTodo = hasPermission('system_todo:export')

const filters = reactive({
  keyword: '',
  todoType: '',
  priority: '',
  status: '',
  overdueOnly: false,
  dateRange: []
})

const createForm = reactive({
  title: '',
  content: '',
  contentType: 'HTML',
  todoType: 'GENERAL',
  priority: 'MEDIUM',
  assigneeIds: [],
  dueAt: '',
  bizType: '',
  bizId: undefined,
  attachmentFileIds: [],
  embeddedImageFileIds: []
})

const todoToolItems = computed(() => [
  { key: 'export', label: '导出待办', visible: canExportTodo }
])

const paginationConfig = computed(() => ({
  current: page.value,
  pageSize: size.value,
  total: total.value,
  pageSizeOptions: pageSizeOptions.value
}))

const columns = [
  { title: '序号', slotName: 'index', width: 72, fixed: 'left', align: 'center' },
  { title: '标题', slotName: 'title', width: 260 },
  { title: '类型', slotName: 'todoType', width: 120 },
  { title: '优先级', slotName: 'priority', width: 140 },
  { title: '状态', slotName: 'status', width: 150 },
  { title: '创建人', dataIndex: 'creatorName', width: 130 },
  { title: '处理人', slotName: 'assigneeName', width: 130 },
  { title: '截止时间', slotName: 'dueAt', width: 160 },
  { title: '创建时间', slotName: 'createdAt', width: 160 },
  { title: '操作', slotName: 'actions', width: 140, fixed: 'right' }
]

const handleHeaderTool = (key) => {
  if (key === 'export') exportTodos()
}

const fetchTodos = async () => {
  loading.value = true
  try {
    const res = await todoApi.myList({
      page: page.value,
      size: size.value,
      keyword: filters.keyword?.trim() || undefined,
      todoType: filters.todoType || undefined,
      priority: filters.priority || undefined,
      status: filters.status || undefined,
      overdueOnly: filters.overdueOnly ? 1 : undefined,
      dateFrom: Array.isArray(filters.dateRange) && filters.dateRange[0] ? filters.dateRange[0] : undefined,
      dateTo: Array.isArray(filters.dateRange) && filters.dateRange[1] ? filters.dateRange[1] : undefined
    })
    if (res.code !== 200) {
      Message.error(res.msg || '获取待办失败')
      return
    }
    const data = res.data || {}
    tableData.value = Array.isArray(data.records) ? data.records : []
    total.value = Number(data.total || 0)
    summary.pending = Number(data.summary?.pending || 0)
    summary.processing = Number(data.summary?.processing || 0)
    summary.done = Number(data.summary?.done || 0)
    summary.overdue = Number(data.summary?.overdue || 0)
    if (route.query.todoId) {
      await maybeOpenTodoFromRoute()
    }
  } finally {
    loading.value = false
  }
}

const fetchDictOptions = async (typeCode, targetRef) => {
  const res = await dictApi.publicItems(typeCode)
  if (res.code !== 200) return
  targetRef.value = (res.data || []).map(item => ({
    label: item.itemLabel,
    value: item.itemValue,
    color: item.tagColor
  }))
}

const exportTodos = async () => {
  try {
    const blob = await todoApi.export({
      keyword: filters.keyword?.trim() || undefined,
      todoType: filters.todoType || undefined,
      priority: filters.priority || undefined,
      status: filters.status || undefined,
      overdueOnly: filters.overdueOnly ? 1 : undefined,
      dateFrom: Array.isArray(filters.dateRange) && filters.dateRange[0] ? filters.dateRange[0] : undefined,
      dateTo: Array.isArray(filters.dateRange) && filters.dateRange[1] ? filters.dateRange[1] : undefined
    })
    const objectUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = `我的待办_${new Date().toISOString().slice(0, 10).replace(/-/g, '')}.xlsx`
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(objectUrl)
    Message.success('我的待办已开始导出')
  } catch (error) {
    Message.error(error?.response?.data?.msg || '导出失败')
  }
}

const changeStatus = async (record, status) => {
  const res = await todoApi.updateStatus(record.id, status)
  if (res.code !== 200) {
    Message.error(res.msg || '更新待办状态失败')
    return
  }
  Message.success('待办状态已更新')
  await fetchTodos()
  if (detailRecord.value?.id === record.id) {
    detailRecord.value.status = status
    detailRecord.value.processedAt = status === 'DONE' ? new Date().toISOString() : null
  }
}

const openDetail = async (record) => {
  const res = await todoApi.detail(record.id)
  detailRecord.value = res.code === 200 ? (res.data || { ...record }) : { ...record }
  detailVisible.value = true
}

const closeDetail = () => {
  detailVisible.value = false
  detailRecord.value = null
  if (route.query.todoId) {
    router.replace({ path: route.path, query: { ...route.query, todoId: undefined } })
  }
}

const openCreateModal = () => {
  resetCreateForm()
  createVisible.value = true
}

const closeCreateModal = () => {
  createVisible.value = false
  resetCreateForm()
}

const resetCreateForm = () => {
  createForm.title = ''
  createForm.content = ''
  createForm.contentType = 'HTML'
  createForm.todoType = todoTypeOptions.value[0]?.value || 'GENERAL'
  createForm.priority = priorityOptions.value[0]?.value || 'MEDIUM'
  createForm.assigneeIds = []
  createForm.dueAt = ''
  createForm.bizType = ''
  createForm.bizId = undefined
  createForm.attachmentFileIds = []
  createForm.embeddedImageFileIds = []
}

const submitTodo = async () => {
  if (!createForm.title.trim()) {
    Message.error('请输入待办标题')
    return false
  }
  if (!plainText(createForm.content).trim()) {
    Message.error('请输入待办内容')
    return false
  }
  if (!createForm.assigneeIds.length) {
    Message.error('请选择处理人')
    return false
  }
  const res = await todoApi.create({
    title: createForm.title.trim(),
    content: createForm.content.trim(),
    contentType: createForm.contentType,
    todoType: createForm.todoType,
    priority: createForm.priority,
    assigneeIds: createForm.assigneeIds,
    dueAt: createForm.dueAt || undefined,
    bizType: createForm.bizType || undefined,
    bizId: createForm.bizId || undefined,
    attachmentFileIds: Array.from(new Set([...createForm.attachmentFileIds, ...createForm.embeddedImageFileIds]))
  })
  if (res.code !== 200) {
    Message.error(res.msg || '新增待办失败')
    return false
  }
  Message.success('待办已创建')
  closeCreateModal()
  fetchTodos()
  return true
}

const handleSearch = () => {
  page.value = 1
  fetchTodos()
}

const handlePageChange = (value) => {
  page.value = value
  fetchTodos()
}

const handleSizeChange = (value) => {
  size.value = value
  page.value = 1
  fetchTodos()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.todoType = ''
  filters.priority = ''
  filters.status = ''
  filters.overdueOnly = false
  filters.dateRange = []
  page.value = 1
  fetchTodos()
}

const findOption = (options, value) => options.value.find(item => item.value === value)
const todoTypeLabel = (value) => findOption(todoTypeOptions, value)?.label || value || '-'
const todoTypeColor = (value) => findOption(todoTypeOptions, value)?.color || 'arcoblue'
const priorityLabel = (value) => findOption(priorityOptions, value)?.label || value || '-'
const priorityColor = (value) => findOption(priorityOptions, value)?.color || 'gray'
const statusLabel = (value) => findOption(statusOptions, value)?.label || value || '-'
const statusColor = (value) => findOption(statusOptions, value)?.color || 'gray'

const tagTone = (color) => {
  if (color === 'green') return 'success'
  if (color === 'red' || color === 'orangered') return 'danger'
  if (color === 'orange' || color === 'gold') return 'warning'
  if (color === 'arcoblue' || color === 'blue' || color === 'purple') return 'primary'
  return 'neutral'
}

const priorityStars = (value) => {
  const map = { LOW: 1, MEDIUM: 2, HIGH: 3, URGENT: 4 }
  return map[String(value || '').toUpperCase()] || 0
}

const formatTime = (value, fallback = '-') => {
  if (!value) return fallback
  return String(value).replace('T', ' ').slice(0, 16)
}

const plainText = (value) => {
  const text = String(value || '')
  if (!text) return ''
  return text
    .replace(/<style[\s\S]*?<\/style>/gi, '')
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&amp;/g, '&')
    .replace(/\s+/g, ' ')
    .trim()
}

const sanitizeHtml = (value) => {
  const html = String(value || '')
  if (!html) return ''
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')
  doc.querySelectorAll('script, style, iframe, object, embed, form, input, button').forEach(node => node.remove())
  doc.body.querySelectorAll('*').forEach(node => {
    Array.from(node.attributes).forEach(attribute => {
      const name = attribute.name.toLowerCase()
      const rawValue = String(attribute.value || '').trim().toLowerCase()
      if (name.startsWith('on') || rawValue.startsWith('javascript:')) {
        node.removeAttribute(attribute.name)
      }
    })
  })
  return doc.body.innerHTML
}

const assigneeNames = (record) => {
  const assignees = Array.isArray(record?.assignees) ? record.assignees : []
  if (!assignees.length) return record?.assigneeName || '-'
  return assignees.map(item => item.assigneeName || item.name || '-').join('、')
}

const formatFileSize = (value) => {
  const number = Number(value || 0)
  if (!Number.isFinite(number) || number <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let current = number
  let index = 0
  while (current >= 1024 && index < units.length - 1) {
    current /= 1024
    index++
  }
  return `${current >= 100 || index === 0 ? current.toFixed(0) : current.toFixed(1)} ${units[index]}`
}

const fileIdOf = file => file?.fileId || file?.id

const previewTypeOf = (file) => {
  const extension = String(file?.extension || '').toLowerCase()
  const contentType = String(file?.contentType || '').toLowerCase()
  if (contentType.startsWith('image/') || ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp'].includes(extension)) return 'image'
  if (contentType.includes('pdf') || extension === 'pdf') return 'pdf'
  return 'other'
}

const revokePreviewUrl = () => {
  if (previewUrl.value) {
    window.URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}

const previewAttachment = async (file) => {
  const fileId = fileIdOf(file)
  if (!fileId) return
  previewVisible.value = true
  previewLoading.value = true
  previewTitle.value = file.originalName || '附件预览'
  previewType.value = previewTypeOf(file)
  revokePreviewUrl()
  try {
    if (previewType.value === 'other') return
    const blob = await fileApi.preview(fileId)
    previewUrl.value = window.URL.createObjectURL(blob)
  } catch (error) {
    Message.error(error?.response?.data?.msg || '附件预览失败')
  } finally {
    previewLoading.value = false
  }
}

const downloadAttachment = async (file) => {
  const fileId = fileIdOf(file)
  if (!fileId) return
  try {
    const blob = await fileApi.download(fileId)
    const objectUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = file.originalName || file.fileName || '附件'
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(objectUrl)
  } catch (error) {
    Message.error(error?.response?.data?.msg || '附件下载失败')
  }
}

const closePreview = () => {
  previewVisible.value = false
  previewLoading.value = false
  previewType.value = ''
  previewTitle.value = '附件预览'
  revokePreviewUrl()
}

const readFileAsDataUrl = file => new Promise((resolve, reject) => {
  const reader = new FileReader()
  reader.onload = () => resolve(reader.result)
  reader.onerror = () => reject(reader.error)
  reader.readAsDataURL(file)
})

const uploadRichImage = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  const res = await fileApi.upload(formData)
  if (res.code !== 200 || !res.data?.id) {
    Message.error(res.msg || '图片上传失败')
    return ''
  }
  if (!createForm.embeddedImageFileIds.includes(res.data.id)) {
    createForm.embeddedImageFileIds.push(res.data.id)
  }
  return readFileAsDataUrl(file)
}

const isDueToday = (value) => {
  if (!value) return false
  return String(value).slice(0, 10) === new Date().toISOString().slice(0, 10)
}

const rowClassName = (record) => {
  return record.overdue ? 'todo-row-overdue' : ''
}

const maybeOpenTodoFromRoute = async () => {
  const todoId = Number(route.query.todoId)
  if (!todoId) return
  if (detailVisible.value && Number(detailRecord.value?.id) === todoId) return
  const matched = tableData.value.find(item => Number(item.id) === todoId)
  if (matched) {
    await openDetail(matched)
    return
  }
  const res = await todoApi.detail(todoId)
  if (res.code !== 200) {
    Message.error(res.msg || '获取待办详情失败')
    return
  }
  detailRecord.value = res.data || null
  detailVisible.value = true
}

onMounted(async () => {
  await applyPaginationConfig({ pageSizeRef: size, pageSizeOptionsRef: pageSizeOptions })
  await Promise.all([
    fetchDictOptions('todo_type', todoTypeOptions),
    fetchDictOptions('todo_priority', priorityOptions),
    fetchDictOptions('todo_status', statusOptions)
  ])
  await fetchTodos()
})

watch(() => route.query.todoId, async (value) => {
  if (!value) return
  await maybeOpenTodoFromRoute()
})
</script>

<style scoped>
.todo-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 1680px;
  margin: 0 auto;
  padding: 0 16px 16px;
}

.filter-select {
  width: 150px;
}

.filter-date {
  width: 260px;
}

.overdue-checkbox {
  height: 36px;
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
  color: var(--ds-color-text-regular);
}

.query-button {
  min-width: 72px;
  height: 36px;
  border-radius: 8px;
}

.list-card {
  padding: 20px;
  border-radius: 12px;
  border: 1px solid var(--ds-color-border);
  background: var(--ds-color-bg-card);
  box-shadow: 0 10px 28px var(--ds-color-shadow);
}

.list-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.list-card__title {
  margin: 0;
  color: var(--ds-color-text-primary);
  font-size: 16px;
  font-weight: 600;
}

.list-card__description {
  margin: 6px 0 0;
  color: var(--ds-color-text-secondary);
  font-size: 13px;
}

.todo-title-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.todo-title {
  max-width: 210px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ds-color-text-primary);
  font-weight: 500;
}

.todo-biz {
  max-width: 210px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ds-color-text-secondary);
  font-size: 12px;
}

.priority-cell,
.status-cell,
.due-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.due-cell.overdue {
  color: var(--ds-color-danger);
  font-weight: 500;
}

.due-cell.dueToday {
  color: var(--ds-color-warning);
}

.due-note {
  font-size: 12px;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

:deep(.todo-row-overdue td) {
  background: color-mix(in srgb, var(--ds-color-danger) 5%, var(--ds-color-bg-card));
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-header {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-title {
  margin: 0;
  color: var(--ds-color-text-primary);
  font-size: 22px;
  line-height: 30px;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 20px;
  color: var(--ds-color-text-regular);
  font-size: 13px;
}

.detail-body {
  padding: 16px;
  border: 1px solid var(--ds-color-border);
  border-radius: 10px;
  background: var(--ds-color-bg-soft);
  color: var(--ds-color-text-primary);
  line-height: 1.8;
  white-space: pre-wrap;
}

.detail-body.is-html {
  background: var(--ds-color-bg-card);
  white-space: normal;
}

.detail-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.detail-body :deep(p) {
  margin: 0 0 8px;
}

.detail-attachments {
  border: 1px solid var(--ds-color-border);
  border-radius: 10px;
  background: var(--ds-color-bg-card);
  overflow: hidden;
}

.detail-attachments__title {
  padding: 10px 12px;
  border-bottom: 1px solid var(--ds-color-border);
  color: var(--ds-color-text-primary);
  font-size: 13px;
  font-weight: 600;
}

.detail-attachment {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--ds-color-border);
}

.detail-attachment:last-child {
  border-bottom: 0;
}

.detail-attachment__icon {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.detail-attachment__icon :deep(svg) {
  width: 20px;
  height: 20px;
}

.detail-attachment__name {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ds-color-text-primary);
}

.detail-attachment__size {
  color: var(--ds-color-text-secondary);
  font-size: 12px;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.preview-shell {
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ds-color-bg-soft);
  border-radius: 10px;
  overflow: hidden;
}

.preview-image {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}

.preview-pdf {
  width: 100%;
  min-height: 70vh;
  border: 0;
}
</style>
