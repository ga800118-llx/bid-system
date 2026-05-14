<template>
  <div class="file-page">
    <DsPageHeader
      :breadcrumb="['系统管理', '文件中心']"
      title="文件中心"
      description="统一管理系统上传文件、预览下载、状态维护和业务关联。"
    >
      <template #actions>
        <DsHeaderActions
          :tool-items="fileHeaderTools"
          primary-label="上传文件"
          :show-primary="canUploadFile"
          @tool-select="handleHeaderTool"
          @primary-click="triggerUpload"
        >
          <template #primaryIcon><IconUpload /></template>
        </DsHeaderActions>
      </template>
      <template #meta>
        <span>单文件上限 {{ uploadRules.maxSizeMb }} MB</span>
        <span v-if="uploadRules.allowedExtensions.length">允许上传：{{ uploadRules.allowedExtensions.join('、') }}</span>
        <span v-if="uploadRules.previewableExtensions.length">支持预览：{{ uploadRules.previewableExtensions.join('、') }}</span>
      </template>
    </DsPageHeader>

    <DsFilterBar title="筛选查询" description="按文件名、扩展名、状态、预览能力和上传时间定位文件。">
      <DsKeywordSearch
        v-model="filters.keyword"
        placeholder="搜索文件名"
        @search="handleSearch"
      />
      <DsInput
        v-model="filters.extension"
        class="filter-extension"
        placeholder="扩展名"
        @press-enter="handleSearch"
      />
      <DsStatusSelect
        v-model="filters.status"
        class="filter-status"
        placeholder="全部状态"
      />
      <DsDateRangePicker
        v-model="filters.dateRange"
        class="filter-date"
      />
      <label class="preview-checkbox">
        <a-checkbox v-model="filters.previewable" @change="handleSearch">仅看可预览</a-checkbox>
      </label>
      <template #actions>
        <a-button type="primary" class="query-button" @click="handleSearch">查询</a-button>
        <a-button class="query-button" @click="resetFilters">重置</a-button>
      </template>
    </DsFilterBar>

    <section class="list-card ds-card">
      <div class="list-card__header">
        <div>
          <h3 class="list-card__title">文件列表</h3>
          <p class="list-card__description">共 {{ total }} 条，支持上传、预览、下载、状态维护和业务关联。</p>
        </div>
        <div class="list-card__tools">
          <DsIconButton tooltip="刷新" @click="fetchFiles"><IconRefresh /></DsIconButton>
        </div>
      </div>

      <input ref="fileInputRef" type="file" multiple class="file-input" @change="handleFileChange" />

      <DsDataTable
        :columns="columns"
        :data="tableData"
        :loading="loading || uploading"
        row-key="id"
        :pagination="paginationConfig"
        :scroll="{ x: 1180 }"
        @pagination-change="handlePageChange"
        @page-size-change="handleSizeChange"
      >
        <template #index="{ rowIndex }">{{ (page - 1) * size + rowIndex + 1 }}</template>
        <template #originalName="{ record }">
          <div class="file-cell">
            <span class="file-cell__icon"><FileTypeIcon :type="record.extension" /></span>
            <div class="file-cell__main">
              <a-tooltip :content="record.originalName">
                <span class="file-cell__name">{{ record.originalName || '-' }}</span>
              </a-tooltip>
              <span class="file-cell__meta">{{ record.objectKey || record.fileHash || '暂无对象信息' }}</span>
            </div>
          </div>
        </template>
        <template #extension="{ record }">
          <DsTag :label="(record.extension || '未知').toUpperCase()" />
        </template>
        <template #previewable="{ record }">
          <DsStatusTag :label="record.previewable ? '可预览' : '仅下载'" :tone="record.previewable ? 'success' : 'neutral'" />
        </template>
        <template #fileSize="{ record }">{{ formatFileSize(record.fileSize) }}</template>
        <template #status="{ record }">
          <DsStatusTag :value="record.status" :options="statusOptions" />
        </template>
        <template #createdAt="{ record }">{{ formatTime(record.createdAt) }}</template>
        <template #actions="{ record }">
          <div class="row-actions">
            <a-button type="text" size="small" @click="openDetailDrawer(record)">详情</a-button>
            <a-dropdown>
              <a-button type="text" size="small">
                更多 <IconDown />
              </a-button>
              <template #content>
                <a-doption v-if="canDownloadFile && record.previewable" @click="previewFile(record)">预览</a-doption>
                <a-doption v-if="canDownloadFile" @click="downloadFile(record)">下载</a-doption>
                <a-doption v-if="canLinkFile" @click="openDetailDrawer(record)">关联管理</a-doption>
                <a-doption v-if="canManageFile && record.status === 1" class="danger-option" @click="disableFile(record)">禁用</a-doption>
                <a-doption v-else-if="canManageFile && record.status !== 1" @click="enableFile(record)">启用</a-doption>
              </template>
            </a-dropdown>
          </div>
        </template>
        <template #empty>
          <DsEmptyState title="暂无文件数据" description="当前筛选条件下没有可展示的文件。" />
        </template>
      </DsDataTable>
    </section>

    <a-drawer
      :visible="detailVisible"
      :width="760"
      title="文件详情"
      unmount-on-close
      @cancel="closeDetailDrawer"
    >
      <div v-if="detailLoading" class="drawer-loading">
        <a-spin />
      </div>
      <template v-else>
        <div v-if="detailRecord" class="detail-content">
          <DsFormSection title="基础信息">
            <div class="detail-grid">
              <div class="detail-item">
                <span class="detail-label">文件名</span>
                <span class="detail-value">{{ detailRecord.originalName || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">扩展名</span>
                <span class="detail-value">{{ detailRecord.extension || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">文件大小</span>
                <span class="detail-value">{{ formatFileSize(detailRecord.fileSize) }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">上传人</span>
                <span class="detail-value">{{ detailRecord.uploaderName || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">状态</span>
                <span class="detail-value"><DsStatusTag :value="detailRecord.status" :options="statusOptions" /></span>
              </div>
              <div class="detail-item">
                <span class="detail-label">预览能力</span>
                <span class="detail-value">
                  <DsStatusTag :label="detailRecord.previewable ? '支持在线预览' : '仅支持下载'" :tone="detailRecord.previewable ? 'success' : 'neutral'" />
                </span>
              </div>
              <div class="detail-item">
                <span class="detail-label">上传时间</span>
                <span class="detail-value">{{ formatTime(detailRecord.createdAt) }}</span>
              </div>
              <div class="detail-item detail-item-full">
                <span class="detail-label">对象键</span>
                <span class="detail-value detail-mono">{{ detailRecord.objectKey || '-' }}</span>
              </div>
              <div class="detail-item detail-item-full">
                <span class="detail-label">文件摘要</span>
                <span class="detail-value detail-mono">{{ detailRecord.fileHash || '-' }}</span>
              </div>
            </div>
          </DsFormSection>

          <DsFormSection title="关联信息">
            <template #default>
              <div class="section-header">
                <span class="section-description">当前文件绑定的业务对象和用途分类。</span>
                <a-button v-if="canLinkFile" type="primary" size="small" @click="openLinkModal">新增关联</a-button>
              </div>
              <DsDataTable
                :columns="linkColumns"
                :data="detailRecord.links || []"
                :show-pagination="false"
                row-key="id"
                :scroll="{ x: 640 }"
              >
                <template #bizObject="{ record }">
                  <span>{{ record.bizType || '-' }} / {{ record.bizId || '-' }}</span>
                </template>
                <template #categoryCode="{ record }">
                  <DsStatusTag v-if="categoryLabel(record.categoryCode)" :label="categoryLabel(record.categoryCode)" :tone="categoryTone(record.categoryCode)" />
                  <span v-else>{{ record.categoryCode || '-' }}</span>
                </template>
                <template #createdAt="{ record }">{{ formatTime(record.createdAt) }}</template>
                <template #actions="{ record }">
                  <a-button v-if="canLinkFile" type="text" size="small" status="danger" @click="removeLink(record)">解除关联</a-button>
                  <span v-else class="no-actions">无可用操作</span>
                </template>
                <template #empty>
                  <DsEmptyState title="暂无关联" description="当前文件暂无业务关联。" />
                </template>
              </DsDataTable>
            </template>
          </DsFormSection>
        </div>
      </template>
    </a-drawer>

    <a-modal
      v-model:visible="previewVisible"
      :title="previewTitle"
      :width="960"
      :footer="false"
      unmount-on-close
      @cancel="closePreview"
    >
      <div class="preview-shell">
        <div v-if="previewLoading" class="drawer-loading">
          <a-spin />
        </div>
        <template v-else>
          <div v-if="previewType && previewUrl" class="preview-tip">
            当前为{{ previewType === 'pdf' ? 'PDF' : '图片' }}在线预览，若显示异常可直接下载查看。
          </div>
          <img v-if="previewType === 'image' && previewUrl" :src="previewUrl" alt="预览图片" class="preview-image" />
          <iframe v-else-if="previewType === 'pdf' && previewUrl" :src="previewUrl" class="preview-pdf"></iframe>
          <DsEmptyState v-else title="暂不支持在线预览" description="当前文件类型暂不支持在线预览，请使用下载查看。" />
        </template>
      </div>
    </a-modal>

    <DsModalForm
      :visible="linkModalVisible"
      title="新增文件关联"
      description="为当前文件绑定业务对象，便于后续模块按业务维度查看文件。"
      :width="560"
      @cancel="closeLinkModal"
    >
      <DsFormSection title="关联信息">
        <DsFormGrid>
          <DsInput v-model="linkForm.bizType" label="业务类型" required placeholder="如 project、contract、notice" />
          <DsNumberInput v-model="linkForm.bizId" label="业务对象 ID" required :min="1" />
          <DsFileCategorySelect v-model="linkForm.categoryCode" label="关联分类" />
          <DsNumberInput v-model="linkForm.sortOrder" label="排序" :min="0" />
        </DsFormGrid>
        <div class="form-tip">第一版先按业务类型 + 业务对象 ID 建关联，后续业务模块可继续扩展分类字典和专属页面。</div>
      </DsFormSection>
      <template #footer>
        <DsFormActions submit-text="保存" @cancel="closeLinkModal" @submit="submitLink" />
      </template>
    </DsModalForm>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import { IconDown, IconRefresh, IconUpload } from '@arco-design/web-vue/es/icon'
import { fileApi } from '@/api'
import { hasPermission } from '@/utils/permission'
import { DEFAULT_PAGE_SIZE_OPTIONS, applyPaginationConfig } from '@/utils/systemConfig'
import { loadCommonStatusOptions, loadPublicDictItems } from '@/utils/dict'
import FileTypeIcon from '@/design-system/icons/FileTypeIcon.vue'
import {
  DsDataTable,
  DsDateRangePicker,
  DsEmptyState,
  DsFileCategorySelect,
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
  DsStatusSelect,
  DsStatusTag,
  DsTag
} from '@/design-system'

const loading = ref(false)
const uploading = ref(false)
const detailLoading = ref(false)
const tableData = ref([])
const detailRecord = ref(null)
const detailVisible = ref(false)
const linkModalVisible = ref(false)
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewUrl = ref('')
const previewType = ref('')
const previewTitle = ref('文件预览')
const page = ref(1)
const size = ref(20)
const total = ref(0)
const pageSizeOptions = ref([...DEFAULT_PAGE_SIZE_OPTIONS])
const statusOptions = ref([])
const linkCategoryOptions = ref([])
const uploadRules = ref({
  maxSizeMb: 100,
  allowedExtensions: [],
  previewableExtensions: []
})
const fileInputRef = ref(null)

const filters = reactive({
  keyword: '',
  extension: '',
  status: '',
  dateRange: [],
  previewable: false
})

const linkForm = reactive({
  bizType: '',
  bizId: null,
  categoryCode: '',
  sortOrder: 0
})

const canUploadFile = hasPermission('system_file:upload')
const canDownloadFile = hasPermission('system_file:download')
const canExportFile = hasPermission('system_file:export')
const canManageFile = hasPermission('system_file:delete')
const canLinkFile = hasPermission('system_file:link')

const fileHeaderTools = computed(() => [
  { key: 'export', label: '导出文件', visible: canExportFile }
])

const paginationConfig = computed(() => ({
  current: page.value,
  pageSize: size.value,
  total: total.value,
  pageSizeOptions: pageSizeOptions.value
}))

const columns = [
  { title: '序号', slotName: 'index', width: 72, fixed: 'left', align: 'center' },
  { title: '文件名', dataIndex: 'originalName', slotName: 'originalName', width: 340 },
  { title: '类型', dataIndex: 'extension', slotName: 'extension', width: 110 },
  { title: '大小', dataIndex: 'fileSize', slotName: 'fileSize', width: 120 },
  { title: '上传人', dataIndex: 'uploaderName', width: 140 },
  { title: '预览能力', slotName: 'previewable', width: 120 },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: 100 },
  { title: '上传时间', dataIndex: 'createdAt', slotName: 'createdAt', width: 170 },
  { title: '操作', slotName: 'actions', width: 140, fixed: 'right' }
]

const linkColumns = [
  { title: '业务对象', slotName: 'bizObject', width: 220 },
  { title: '分类', slotName: 'categoryCode', width: 140 },
  { title: '排序', dataIndex: 'sortOrder', width: 100 },
  { title: '创建时间', slotName: 'createdAt', width: 180 },
  { title: '操作', slotName: 'actions', width: 120, fixed: 'right' }
]

const handleHeaderTool = (key) => {
  if (key === 'export') exportFiles()
}

const fetchFiles = async () => {
  loading.value = true
  try {
    const res = await fileApi.list({
      page: page.value,
      size: size.value,
      keyword: filters.keyword?.trim() || undefined,
      extension: filters.extension?.trim() || undefined,
      status: filters.status === '' ? undefined : Number(filters.status),
      dateFrom: Array.isArray(filters.dateRange) && filters.dateRange[0] ? filters.dateRange[0] : undefined,
      dateTo: Array.isArray(filters.dateRange) && filters.dateRange[1] ? filters.dateRange[1] : undefined,
      previewable: filters.previewable ? true : undefined
    })
    if (res.code !== 200) {
      Message.error(res.msg || '获取文件列表失败')
      return
    }
    const data = res.data || {}
    tableData.value = Array.isArray(data.records) ? data.records : []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

const fetchRules = async () => {
  const res = await fileApi.rules()
  if (res.code !== 200) return
  uploadRules.value = {
    maxSizeMb: Number(res.data?.maxSizeMb || 100),
    allowedExtensions: Array.isArray(res.data?.allowedExtensions) ? res.data.allowedExtensions : [],
    previewableExtensions: Array.isArray(res.data?.previewableExtensions) ? res.data.previewableExtensions : []
  }
}

const fetchFileDetail = async (fileId) => {
  detailLoading.value = true
  try {
    const res = await fileApi.detail(fileId)
    if (res.code !== 200) {
      Message.error(res.msg || '获取文件详情失败')
      return
    }
    detailRecord.value = res.data || null
  } finally {
    detailLoading.value = false
  }
}

const triggerUpload = () => {
  fileInputRef.value?.click()
}

const handleFileChange = async (event) => {
  const files = Array.from(event.target?.files || [])
  if (!files.length) return
  uploading.value = true
  const successNames = []
  const failedNames = []
  try {
    for (const file of files) {
      const extension = String(file.name || '').includes('.') ? String(file.name).split('.').pop().toLowerCase() : ''
      if (uploadRules.value.allowedExtensions.length && !uploadRules.value.allowedExtensions.includes(extension)) {
        failedNames.push(`${file.name}（类型不允许）`)
        continue
      }
      const formData = new FormData()
      formData.append('file', file)
      try {
        const res = await fileApi.upload(formData)
        if (res.code === 200) {
          successNames.push(file.name)
        } else {
          failedNames.push(`${file.name}（${res.msg || '上传失败'}）`)
        }
      } catch (error) {
        failedNames.push(`${file.name}（${error?.response?.data?.msg || '上传失败'}）`)
      }
    }
    if (successNames.length) {
      page.value = 1
      await fetchFiles()
    }
    if (successNames.length && !failedNames.length) {
      Message.success(`上传成功，共 ${successNames.length} 个文件`)
    } else if (!successNames.length && failedNames.length) {
      Message.error(`上传失败，共 ${failedNames.length} 个文件`)
    } else {
      Modal.info({
        title: '批量上传结果',
        content: `成功 ${successNames.length} 个，失败 ${failedNames.length} 个。`
      })
    }
  } finally {
    uploading.value = false
    if (event.target) {
      event.target.value = ''
    }
  }
}

const downloadFile = async (record) => {
  try {
    const blob = await fileApi.download(record.id)
    const objectUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = record.originalName || 'file'
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(objectUrl)
  } catch (error) {
    Message.error(error?.response?.data?.msg || '下载失败')
  }
}

const previewFile = async (record) => {
  const type = detectPreviewType(record)
  if (!type) {
    Message.info('当前文件类型暂不支持在线预览，请使用下载查看。')
    return
  }
  revokePreviewUrl()
  previewVisible.value = true
  previewLoading.value = true
  previewType.value = type
  previewTitle.value = record.originalName || '文件预览'
  try {
    const blob = await fileApi.preview(record.id)
    const normalizedBlob = normalizePreviewBlob(blob, type, record)
    previewUrl.value = window.URL.createObjectURL(normalizedBlob)
  } catch (error) {
    previewVisible.value = false
    Message.error(error?.response?.data?.msg || '预览失败')
  } finally {
    previewLoading.value = false
  }
}

const updateFileStatus = async (record, status) => {
  const actionText = status === 1 ? '启用' : '禁用'
  Modal.confirm({
    title: `${actionText}文件`,
    content: `确认${actionText}“${record.originalName}”？`,
    onOk: async () => {
      const res = await fileApi.updateStatus(record.id, status)
      if (res.code !== 200) {
        Message.error(res.msg || `${actionText}失败`)
        return
      }
      Message.success(`${actionText}成功`)
      await fetchFiles()
      if (detailVisible.value && detailRecord.value?.id === record.id) {
        fetchFileDetail(record.id)
      }
    }
  })
}

const disableFile = (record) => updateFileStatus(record, 0)
const enableFile = (record) => updateFileStatus(record, 1)

const openDetailDrawer = async (record) => {
  detailVisible.value = true
  detailRecord.value = null
  await fetchFileDetail(record.id)
}

const closeDetailDrawer = () => {
  detailVisible.value = false
  detailRecord.value = null
  closeLinkModal()
}

const closePreview = () => {
  previewVisible.value = false
  previewLoading.value = false
  previewType.value = ''
  previewTitle.value = '文件预览'
  revokePreviewUrl()
}

const openLinkModal = () => {
  resetLinkForm()
  linkModalVisible.value = true
}

const closeLinkModal = () => {
  linkModalVisible.value = false
  resetLinkForm()
}

const resetLinkForm = () => {
  linkForm.bizType = ''
  linkForm.bizId = null
  linkForm.categoryCode = ''
  linkForm.sortOrder = 0
}

const revokePreviewUrl = () => {
  if (previewUrl.value) {
    window.URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}

const submitLink = async () => {
  if (!detailRecord.value?.id) {
    Message.error('请先选择文件')
    return false
  }
  if (!String(linkForm.bizType || '').trim()) {
    Message.error('请输入业务类型')
    return false
  }
  if (!Number(linkForm.bizId)) {
    Message.error('请输入业务对象 ID')
    return false
  }
  try {
    const res = await fileApi.addLink({
      fileId: detailRecord.value.id,
      bizType: String(linkForm.bizType).trim(),
      bizId: Number(linkForm.bizId),
      categoryCode: String(linkForm.categoryCode || '').trim(),
      sortOrder: Number(linkForm.sortOrder || 0)
    })
    if (res.code !== 200) {
      Message.error(res.msg || '新增关联失败')
      return false
    }
    Message.success('新增关联成功')
    await fetchFileDetail(detailRecord.value.id)
    closeLinkModal()
    return true
  } catch (error) {
    Message.error(error?.response?.data?.msg || '新增关联失败')
    return false
  }
}

const removeLink = (record) => {
  Modal.confirm({
    title: '解除文件关联',
    content: `确认解除 ${record.bizType || '-'} / ${record.bizId || '-'} 的关联？`,
    onOk: async () => {
      const res = await fileApi.removeLink(record.id)
      if (res.code !== 200) {
        Message.error(res.msg || '解除关联失败')
        return
      }
      Message.success('解除关联成功')
      if (detailRecord.value?.id) {
        fetchFileDetail(detailRecord.value.id)
      }
    }
  })
}

const categoryLabel = (value) => {
  return linkCategoryOptions.value.find(item => item.value === value)?.label || value || ''
}

const categoryTone = (value) => {
  const color = linkCategoryOptions.value.find(item => item.value === value)?.color
  if (color === 'green') return 'success'
  if (color === 'red' || color === 'orangered') return 'danger'
  if (color === 'orange' || color === 'gold') return 'warning'
  if (color === 'arcoblue' || color === 'blue' || color === 'purple') return 'primary'
  return 'neutral'
}

const handleSearch = () => {
  page.value = 1
  fetchFiles()
}

const exportFiles = async () => {
  try {
    const blob = await fileApi.export({
      keyword: filters.keyword?.trim() || undefined,
      extension: filters.extension?.trim() || undefined,
      status: filters.status === '' ? undefined : Number(filters.status),
      dateFrom: Array.isArray(filters.dateRange) && filters.dateRange[0] ? filters.dateRange[0] : undefined,
      dateTo: Array.isArray(filters.dateRange) && filters.dateRange[1] ? filters.dateRange[1] : undefined,
      previewable: filters.previewable ? true : undefined
    })
    downloadBlob(blob, `文件列表_${new Date().toISOString().slice(0, 10).replace(/-/g, '')}.xlsx`)
    Message.success('文件列表已开始导出')
  } catch (error) {
    Message.error(error?.response?.data?.msg || '导出失败')
  }
}

const handlePageChange = (value) => {
  page.value = value
  fetchFiles()
}

const handleSizeChange = (value) => {
  size.value = value
  page.value = 1
  fetchFiles()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.extension = ''
  filters.status = ''
  filters.dateRange = []
  filters.previewable = false
  page.value = 1
  fetchFiles()
}

const downloadBlob = (blob, fileName) => {
  const objectUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(objectUrl)
}

const formatTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

const formatFileSize = (value) => {
  const sizeValue = Number(value || 0)
  if (!Number.isFinite(sizeValue) || sizeValue <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let current = sizeValue
  let unitIndex = 0
  while (current >= 1024 && unitIndex < units.length - 1) {
    current /= 1024
    unitIndex += 1
  }
  return `${current >= 100 || unitIndex === 0 ? current.toFixed(0) : current.toFixed(1)} ${units[unitIndex]}`
}

const detectPreviewType = (record) => {
  if (!record?.previewable) return ''
  const contentType = String(record?.contentType || '').toLowerCase()
  const extension = String(record?.extension || '').toLowerCase()
  if (contentType.startsWith('image/') || ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg'].includes(extension)) {
    return 'image'
  }
  if (contentType === 'application/pdf' || extension === 'pdf') {
    return 'pdf'
  }
  return ''
}

const normalizePreviewBlob = (blob, type, record) => {
  const rawType = String(blob?.type || '').toLowerCase()
  if (rawType && rawType !== 'application/octet-stream') {
    return blob
  }
  if (type === 'pdf') {
    return new Blob([blob], { type: 'application/pdf' })
  }
  if (type === 'image') {
    const extension = String(record?.extension || '').toLowerCase()
    const imageTypeMap = {
      png: 'image/png',
      jpg: 'image/jpeg',
      jpeg: 'image/jpeg',
      gif: 'image/gif',
      webp: 'image/webp',
      bmp: 'image/bmp',
      svg: 'image/svg+xml'
    }
    return new Blob([blob], { type: imageTypeMap[extension] || 'image/*' })
  }
  return blob
}

const initPage = async () => {
  const [statusList, categoryList] = await Promise.all([
    loadCommonStatusOptions(),
    loadPublicDictItems('file_link_category', [
      { itemLabel: '主文件', itemValue: 'primary', tagColor: 'arcoblue' },
      { itemLabel: '附件', itemValue: 'attachment', tagColor: 'green' },
      { itemLabel: '归档件', itemValue: 'archive', tagColor: 'purple' }
    ]),
    applyPaginationConfig({ pageSizeRef: size, pageSizeOptionsRef: pageSizeOptions }),
    fetchRules()
  ])
  statusOptions.value = statusList
  linkCategoryOptions.value = categoryList
  fetchFiles()
}

onMounted(() => {
  initPage()
})
</script>

<style scoped>
.file-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 1680px;
  margin: 0 auto;
  padding: 0 16px 16px;
}

.filter-extension {
  width: 140px;
}

.filter-status {
  width: 160px;
}

.filter-date {
  width: 260px;
}

.preview-checkbox {
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

.list-card__tools {
  flex: 0 0 auto;
}

.file-input {
  display: none;
}

.file-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.file-cell__icon {
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: var(--ds-color-bg-soft);
}

.file-cell__icon :deep(svg) {
  width: 20px;
  height: 20px;
}

.file-cell__main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-cell__name {
  max-width: 240px;
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ds-color-text-primary);
  font-weight: 500;
  vertical-align: middle;
}

.file-cell__meta {
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ds-color-text-secondary);
  font-size: 12px;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.danger-option {
  color: var(--ds-color-danger);
}

.drawer-loading {
  display: flex;
  justify-content: center;
  padding: 48px 0;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  background: var(--ds-color-bg-soft);
  border: 1px solid var(--ds-color-border);
  border-radius: 8px;
}

.detail-item-full {
  grid-column: 1 / -1;
}

.detail-label {
  font-size: 12px;
  color: var(--ds-color-text-secondary);
}

.detail-value {
  color: var(--ds-color-text-primary);
  word-break: break-all;
}

.detail-mono {
  font-family: Menlo, Monaco, Consolas, 'Courier New', monospace;
  font-size: 12px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.section-description,
.no-actions,
.form-tip,
.preview-tip {
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.preview-shell {
  min-height: 420px;
}

.preview-tip {
  margin-bottom: 12px;
}

.preview-image {
  display: block;
  max-width: 100%;
  max-height: 72vh;
  margin: 0 auto;
  border-radius: 8px;
}

.preview-pdf {
  width: 100%;
  height: 72vh;
  border: 0;
  border-radius: 8px;
  background: var(--ds-color-bg-soft);
}

@media (max-width: 768px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
