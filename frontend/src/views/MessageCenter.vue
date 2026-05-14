<template>
  <div class="message-page">
    <DsPageHeader
      :breadcrumb="['个人工作', '消息中心']"
      title="消息中心"
      description="查看系统通知、业务提醒和待处理消息。"
    >
      <template #actions>
        <DsHeaderActions
          :action-items="messageActionItems"
          :tool-items="messageToolItems"
          primary-label="发送消息"
          :show-primary="canSendMessage"
          @action-click="handleHeaderAction"
          @tool-select="handleHeaderTool"
          @primary-click="openSendModal"
        />
      </template>
      <template #meta>
        <span>未读 {{ unreadCount }} 条</span>
        <span>当前共 {{ total }} 条消息</span>
      </template>
    </DsPageHeader>

    <DsFilterBar title="筛选查询" description="按标题内容、消息类型和阅读状态快速定位消息。">
      <DsKeywordSearch
        v-model="filters.keyword"
        placeholder="搜索标题或内容"
        @search="handleSearch"
      />
      <DsDictSelect
        v-model="filters.messageType"
        type-code="message_type"
        class="filter-select"
        placeholder="全部类型"
      />
      <DsDictSelect
        v-model="filters.readStatus"
        :options="readStatusOptions"
        class="filter-select"
        placeholder="全部状态"
      />
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
          <h3 class="list-card__title">消息列表</h3>
          <p class="list-card__description">共 {{ total }} 条，未读 {{ unreadCount }} 条，支持查看、标记已读和消息发送。</p>
        </div>
        <div class="list-card__tools">
          <DsIconButton tooltip="刷新" @click="fetchMessages"><IconRefresh /></DsIconButton>
        </div>
      </div>

      <DsDataTable
        :columns="columns"
        :data="tableData"
        :loading="loading"
        row-key="id"
        :row-class="rowClassName"
        :pagination="paginationConfig"
        :scroll="{ x: 1180 }"
        @pagination-change="handlePageChange"
        @page-size-change="handleSizeChange"
      >
        <template #index="{ rowIndex }">{{ (page - 1) * size + rowIndex + 1 }}</template>
        <template #title="{ record }">
          <div class="message-title-cell">
            <DsStatusTag v-if="record.readStatus === 0" label="未读" tone="danger" />
            <span class="message-title">{{ record.title }}</span>
          </div>
        </template>
        <template #messageType="{ record }">
          <DsStatusTag :label="messageTypeLabel(record.messageType)" :tone="tagTone(messageTypeColor(record.messageType))" />
        </template>
        <template #content="{ record }">
          <span class="message-content" :title="record.contentText || plainText(record.content)">{{ record.contentText || plainText(record.content) || '-' }}</span>
        </template>
        <template #readStatus="{ record }">
          <DsStatusTag :label="record.readStatus === 1 ? '已读' : '未读'" :tone="record.readStatus === 1 ? 'success' : 'danger'" />
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
                <a-doption v-if="record.readStatus === 0" @click="markRead(record)">标为已读</a-doption>
                <a-doption class="danger-option" @click="confirmDeleteMessage(record)">删除</a-doption>
              </template>
            </a-dropdown>
          </div>
        </template>
        <template #empty>
          <DsEmptyState title="暂无站内消息" description="当前条件下没有可展示的消息。" />
        </template>
      </DsDataTable>
    </section>

    <a-drawer :visible="detailVisible" :width="680" title="消息详情" unmount-on-close @cancel="closeDetail">
      <div v-if="detailRecord" class="detail-content">
        <div class="detail-header">
          <DsStatusTag :label="messageTypeLabel(detailRecord.messageType)" :tone="tagTone(messageTypeColor(detailRecord.messageType))" />
          <DsStatusTag v-if="detailRecord.receiverType === 'CC'" label="抄送" tone="primary" />
          <DsStatusTag :label="detailRecord.readStatus === 1 ? '已读' : '未读'" :tone="detailRecord.readStatus === 1 ? 'success' : 'danger'" />
        </div>
        <h2 class="detail-title">{{ detailRecord.title }}</h2>
        <div class="detail-meta">
          <span>发送人：{{ detailRecord.senderName || '-' }}</span>
          <span>时间：{{ formatTime(detailRecord.createdAt) }}</span>
          <span v-if="detailRecord.bizType">关联对象：{{ detailRecord.bizType }}{{ detailRecord.bizId ? ` #${detailRecord.bizId}` : '' }}</span>
        </div>
        <div v-if="detailRecord.contentType === 'HTML'" class="detail-body rich-content" v-html="sanitizeHtml(detailRecord.content)"></div>
        <div v-else class="detail-body">{{ detailRecord.content }}</div>
        <div v-if="detailRecord.attachments?.length" class="detail-attachments">
          <div class="detail-attachments__title">附件</div>
          <div v-for="file in detailRecord.attachments" :key="file.linkId || file.fileId" class="detail-attachment">
            <span class="detail-attachment__icon"><FileTypeIcon :type="file.extension" /></span>
            <span class="detail-attachment__name">{{ file.originalName || '-' }}</span>
            <span class="detail-attachment__size">{{ formatFileSize(file.fileSize) }}</span>
            <a-button v-if="file.previewable" type="text" size="small" @click="previewAttachment(file)">预览</a-button>
            <a-button type="text" size="small" @click="downloadAttachment(file)">下载</a-button>
          </div>
        </div>
        <div class="detail-actions">
          <a-button v-if="detailRecord.relatedPath" type="primary" @click="openRelated(detailRecord)">前往处理</a-button>
          <a-button v-if="detailRecord.readStatus === 0" @click="markRead(detailRecord)">标为已读</a-button>
          <a-button status="danger" @click="confirmDeleteMessage(detailRecord)">删除消息</a-button>
        </div>
      </div>
    </a-drawer>

    <DsModalForm
      :visible="sendVisible"
      title="发送站内消息"
      description="向全员或指定用户发送系统通知、业务提醒和待办消息。"
      :width="880"
      @cancel="closeSendModal"
    >
      <DsFormSection title="基础信息" description="设置消息标题、类型和发送范围。">
        <DsFormGrid>
          <DsInput v-model="sendForm.title" label="消息标题" required placeholder="请输入消息标题" />
          <DsDictSelect v-model="sendForm.messageType" type-code="message_type" label="消息类型" required />
          <DsRadioGroup v-model="sendForm.targetType" label="发送对象" required :options="targetTypeOptions" />
        </DsFormGrid>
      </DsFormSection>
      <DsFormSection class="message-recipient-section" title="接收范围" description="指定用户时可按部门树选择收件人。">
        <MessageRecipientPicker
          v-if="sendForm.targetType === 'USER'"
          v-model="sendForm.userIds"
          label="接收用户"
          required
        />
        <div v-else class="message-recipient-all">
          <div class="message-recipient-all__title">全员发送</div>
          <div class="message-recipient-all__desc">消息将发送给当前所有正常启用用户，无需单独选择收件人。</div>
        </div>
      </DsFormSection>
      <DsFormSection title="消息正文" description="支持富文本、图片粘贴和附件。">
        <DsRichTextEditor
          v-model="sendForm.content"
          label="消息内容"
          required
          hint="支持富文本编辑和图片粘贴。"
          :upload-image="uploadRichImage"
        />
        <MessageAttachmentUploader v-model="sendForm.attachmentFileIds" label="附件" />
      </DsFormSection>
      <template #footer>
        <DsFormActions submit-text="发送" @cancel="closeSendModal" @submit="submitMessage" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Message, Modal } from '@arco-design/web-vue'
import { IconDown, IconRefresh } from '@arco-design/web-vue/es/icon'
import { dictApi, fileApi, messageApi } from '@/api'
import { hasPermission } from '@/utils/permission'
import { DEFAULT_PAGE_SIZE_OPTIONS, applyPaginationConfig } from '@/utils/systemConfig'
import FileTypeIcon from '@/design-system/icons/FileTypeIcon.vue'
import MessageAttachmentUploader from '@/components/message/MessageAttachmentUploader.vue'
import MessageRecipientPicker from '@/components/message/MessageRecipientPicker.vue'
import {
  DsDataTable,
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
  DsPageHeader,
  DsRadioGroup,
  DsRichTextEditor,
  DsStatusTag
} from '@/design-system'

const loading = ref(false)
const tableData = ref([])
const unreadCount = ref(0)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const pageSizeOptions = ref([...DEFAULT_PAGE_SIZE_OPTIONS])
const messageTypeOptions = ref([])
const detailVisible = ref(false)
const detailRecord = ref(null)
const sendVisible = ref(false)
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewUrl = ref('')
const previewType = ref('')
const previewTitle = ref('附件预览')
const router = useRouter()
const canSendMessage = hasPermission('system_message:send')
const canExportMessage = hasPermission('system_message:export')

const targetTypeOptions = [
  { label: '全员', value: 'ALL' },
  { label: '指定用户', value: 'USER' }
]

const readStatusOptions = [
  { label: '未读', value: '0', color: 'red' },
  { label: '已读', value: '1', color: 'green' }
]

const filters = reactive({
  keyword: '',
  messageType: '',
  readStatus: '',
  dateRange: []
})

const sendForm = reactive({
  title: '',
  content: '',
  messageType: 'SYSTEM',
  targetType: 'ALL',
  userIds: [],
  ccUserIds: [],
  attachmentFileIds: [],
  embeddedImageFileIds: [],
  contentType: 'HTML'
})

const messageActionItems = computed(() => [
  { key: 'readAll', label: '全部已读', disabled: unreadCount.value === 0 }
])

const messageToolItems = computed(() => [
  { key: 'export', label: '导出消息', visible: canExportMessage }
])

const paginationConfig = computed(() => ({
  current: page.value,
  pageSize: size.value,
  total: total.value,
  pageSizeOptions: pageSizeOptions.value
}))

const columns = [
  { title: '序号', slotName: 'index', width: 72, fixed: 'left', align: 'center' },
  { title: '标题', slotName: 'title', width: 240 },
  { title: '类型', slotName: 'messageType', width: 120 },
  { title: '内容摘要', slotName: 'content', width: 320 },
  { title: '发送人', dataIndex: 'senderName', width: 140 },
  { title: '阅读状态', slotName: 'readStatus', width: 110 },
  { title: '发送时间', slotName: 'createdAt', width: 170 },
  { title: '操作', slotName: 'actions', width: 140, fixed: 'right' }
]

const handleHeaderAction = (key) => {
  if (key === 'readAll') markAllRead()
}

const handleHeaderTool = (key) => {
  if (key === 'export') exportMessages()
}

const fetchMessages = async () => {
  loading.value = true
  try {
    const res = await messageApi.myList({
      page: page.value,
      size: size.value,
      keyword: filters.keyword?.trim() || undefined,
      messageType: filters.messageType || undefined,
      readStatus: filters.readStatus === '' ? undefined : Number(filters.readStatus),
      dateFrom: Array.isArray(filters.dateRange) && filters.dateRange[0] ? filters.dateRange[0] : undefined,
      dateTo: Array.isArray(filters.dateRange) && filters.dateRange[1] ? filters.dateRange[1] : undefined
    })
    if (res.code !== 200) {
      Message.error(res.msg || '获取消息失败')
      return
    }
    const data = res.data || {}
    tableData.value = Array.isArray(data.records) ? data.records : []
    total.value = Number(data.total || 0)
    unreadCount.value = Number(data.unreadCount || 0)
  } finally {
    loading.value = false
  }
}

const fetchMessageTypes = async () => {
  const res = await dictApi.publicItems('message_type')
  if (res.code !== 200) return
  messageTypeOptions.value = (res.data || []).map(item => ({
    label: item.itemLabel,
    value: item.itemValue,
    color: item.tagColor
  }))
}

const markRead = async (record) => {
  const res = await messageApi.markRead(record.id)
  if (res.code !== 200) {
    Message.error(res.msg || '标记已读失败')
    return
  }
  Message.success('已标记为已读')
  await fetchMessages()
  if (detailRecord.value?.id === record.id) {
    detailRecord.value.readStatus = 1
  }
}

const markAllRead = async () => {
  const res = await messageApi.markAllRead()
  if (res.code !== 200) {
    Message.error(res.msg || '全部已读失败')
    return
  }
  Message.success(`已处理 ${res.data?.count || 0} 条消息`)
  fetchMessages()
}

const confirmDeleteMessage = (record) => {
  Modal.confirm({
    title: '删除消息',
    content: '删除后仅从当前收件箱移除，确认删除吗？',
    onOk: () => deleteMessage(record)
  })
}

const deleteMessage = async (record) => {
  const res = await messageApi.delete(record.id)
  if (res.code !== 200) {
    Message.error(res.msg || '删除消息失败')
    return
  }
  Message.success('消息已删除')
  if (detailRecord.value?.id === record.id) {
    closeDetail()
  }
  if (tableData.value.length === 1 && page.value > 1) {
    page.value -= 1
  }
  await fetchMessages()
}

const openRelated = async (record) => {
  if (!record?.relatedPath) return
  if (record.readStatus === 0) {
    await markRead(record)
  }
  closeDetail()
  router.push(record.relatedPath)
}

const exportMessages = async () => {
  try {
    const blob = await messageApi.export({
      keyword: filters.keyword?.trim() || undefined,
      messageType: filters.messageType || undefined,
      readStatus: filters.readStatus === '' ? undefined : Number(filters.readStatus),
      dateFrom: Array.isArray(filters.dateRange) && filters.dateRange[0] ? filters.dateRange[0] : undefined,
      dateTo: Array.isArray(filters.dateRange) && filters.dateRange[1] ? filters.dateRange[1] : undefined
    })
    const objectUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = `站内消息_${new Date().toISOString().slice(0, 10).replace(/-/g, '')}.xlsx`
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(objectUrl)
    Message.success('站内消息已开始导出')
  } catch (error) {
    Message.error(error?.response?.data?.msg || '导出失败')
  }
}

const openDetail = (record) => {
  detailRecord.value = { ...record }
  detailVisible.value = true
  if (record.readStatus === 0) {
    markRead(record)
  }
}

const closeDetail = () => {
  detailVisible.value = false
  detailRecord.value = null
}

const openSendModal = () => {
  resetSendForm()
  sendVisible.value = true
}

const closeSendModal = () => {
  sendVisible.value = false
  resetSendForm()
}

const resetSendForm = () => {
  sendForm.title = ''
  sendForm.content = ''
  sendForm.messageType = messageTypeOptions.value[0]?.value || 'SYSTEM'
  sendForm.targetType = 'ALL'
  sendForm.userIds = []
  sendForm.ccUserIds = []
  sendForm.attachmentFileIds = []
  sendForm.embeddedImageFileIds = []
  sendForm.contentType = 'HTML'
}

const submitMessage = async () => {
  if (!sendForm.title.trim()) {
    Message.error('请输入消息标题')
    return false
  }
  if (!plainText(sendForm.content).trim()) {
    Message.error('请输入消息内容')
    return false
  }
  if (sendForm.targetType === 'USER' && sendForm.userIds.length === 0) {
    Message.error('请选择接收用户')
    return false
  }
  const res = await messageApi.send({
    title: sendForm.title.trim(),
    content: sendForm.content.trim(),
    contentType: sendForm.contentType,
    messageType: sendForm.messageType,
    targetType: sendForm.targetType,
    userIds: sendForm.targetType === 'USER' ? sendForm.userIds : [],
    ccUserIds: sendForm.ccUserIds,
    attachmentFileIds: Array.from(new Set([...sendForm.attachmentFileIds, ...sendForm.embeddedImageFileIds]))
  })
  if (res.code !== 200) {
    Message.error(res.msg || '发送消息失败')
    return false
  }
  Message.success(`发送成功，共 ${res.data?.targetCount || 0} 位接收人`)
  closeSendModal()
  fetchMessages()
  return true
}

const handleSearch = () => {
  page.value = 1
  fetchMessages()
}

const handlePageChange = (value) => {
  page.value = value
  fetchMessages()
}

const handleSizeChange = (value) => {
  size.value = value
  page.value = 1
  fetchMessages()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.messageType = ''
  filters.readStatus = ''
  filters.dateRange = []
  page.value = 1
  fetchMessages()
}

const messageTypeLabel = (value) => {
  return messageTypeOptions.value.find(item => item.value === value)?.label || value || '-'
}

const messageTypeColor = (value) => {
  return messageTypeOptions.value.find(item => item.value === value)?.color || 'arcoblue'
}

const tagTone = (color) => {
  if (color === 'green') return 'success'
  if (color === 'red' || color === 'orangered') return 'danger'
  if (color === 'orange' || color === 'gold') return 'warning'
  if (color === 'arcoblue' || color === 'blue' || color === 'purple') return 'primary'
  return 'neutral'
}

const formatTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

const rowClassName = (record) => {
  return record.readStatus === 0 ? 'message-row-unread' : ''
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
  if (!sendForm.embeddedImageFileIds.includes(res.data.id)) {
    sendForm.embeddedImageFileIds.push(res.data.id)
  }
  return readFileAsDataUrl(file)
}

const initPage = async () => {
  await Promise.all([
    fetchMessageTypes(),
    applyPaginationConfig({ pageSizeRef: size, pageSizeOptionsRef: pageSizeOptions })
  ])
  resetSendForm()
  fetchMessages()
}

onMounted(() => {
  initPage()
})
</script>

<style scoped>
.message-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 1680px;
  margin: 0 auto;
  padding: 0 16px 16px;
}

.filter-select {
  width: 160px;
}

.filter-date {
  width: 260px;
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

.message-title-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.message-title {
  max-width: 170px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ds-color-text-primary);
  font-weight: 500;
}

.message-content {
  max-width: 280px;
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
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

.message-recipient-section {
  min-height: 124px;
}

.message-recipient-all {
  min-height: 61px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  padding: 10px 12px;
  border: 1px solid var(--ds-color-border);
  border-radius: 8px;
  background: var(--ds-color-bg-soft);
}

.message-recipient-all__title {
  color: var(--ds-color-text-primary);
  font-size: 13px;
  font-weight: 600;
}

.message-recipient-all__desc {
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

:deep(.message-row-unread td) {
  background: color-mix(in srgb, var(--ds-color-primary) 5%, var(--ds-color-bg-card));
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-header,
.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.detail-title {
  margin: 0;
  font-size: 20px;
  color: var(--ds-color-text-primary);
}

.detail-meta {
  color: var(--ds-color-text-secondary);
  font-size: 13px;
}

.detail-body {
  padding: 16px;
  border: 1px solid var(--ds-color-border);
  border-radius: 10px;
  background: var(--ds-color-bg-soft);
  white-space: pre-wrap;
  line-height: 1.8;
  color: var(--ds-color-text-primary);
}

.rich-content {
  white-space: normal;
}

.rich-content :deep(p) {
  margin: 0 0 10px;
}

.rich-content :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.detail-attachments {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-attachments__title {
  color: var(--ds-color-text-primary);
  font-size: 14px;
  font-weight: 600;
}

.detail-attachment {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid var(--ds-color-border);
  border-radius: 10px;
  background: var(--ds-color-bg-card);
}

.detail-attachment__icon {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.detail-attachment__icon :deep(svg) {
  width: 24px;
  height: 24px;
}

.detail-attachment__name {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ds-color-text-primary);
  font-weight: 500;
}

.detail-attachment__size {
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  white-space: nowrap;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.preview-shell {
  min-height: 420px;
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
  height: 70vh;
  border: 0;
  background: var(--ds-color-bg-card);
}
</style>
