<template>
  <div class="admin-container">
    <PageHeader title="上传招标文档" />
    <div class="main">
      <a-card>
        <div
          class="drag-zone"
          :class="{ 'drag-over': isDragOver }"
          @click="triggerFileInput"
          @dragover.prevent="isDragOver = true"
          @dragleave="isDragOver = false"
          @drop.prevent="handleNativeDrop"
        >
          <input
            ref="fileInputRef"
            type="file"
            accept=".pdf,.doc,.docx"
            style="display:none"
            @change="handleFileInputChange"
          />
          <div class="drag-icon">+</div>
          <div class="upload-text">
            将文件<span style="color:#1650ff">拖到此处</span>，或<span style="color:#1650ff">点击选择</span>
          </div>
          <div style="color:#86909c;font-size:12px;margin-top:8px">支持 PDF、Word 文档，单个文件不超过100MB</div>
        </div>
        <div v-if="file" class="file-info">
          <span class="file-icon">📄</span>
          <span class="file-name">{{ file.name }}</span>
          <span class="file-size">({{ (file.size / 1024 / 1024).toFixed(2) }} MB)</span>
          <a-button type="text" size="small" @click="triggerFileInput">更换文件</a-button>
        </div>
        <div class="actions">
          <a-button type="primary" size="large" :loading="uploading" :disabled="!file" @click="handleUpload">上传智能提取</a-button>
          <a-button size="large" @click="goHome">取消</a-button>
        </div>
        <div v-if="uploading" class="progress-area">
          <div v-if="isExtracting" class="extracting-area">
            <div class="extracting-icon"></div>
            <div class="extracting-text">AI 正在提取信息，请稍候...</div>
            <div class="extracting-sub">这可能需要 1-2 分钟，请勿关闭页面</div>
          </div>
          <a-progress
            v-else
            :percent="progressPercent"
            :format="(p) => `上传中 ${p}%`"
          />
        </div>
      </a-card>
      <a-card style="margin-top:24px">
        <template #title>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span>最近上传</span>
            <a-button type="text" size="small" @click="router.push('/projects')">查看全部</a-button>
          </div>
        </template>
        <a-table :columns="columns" :data="recentProjects" :loading="tableLoading" :pagination="false" row-key="id" stripe :selected-row-keys="selectedIds" @select="handleRowSelect" @select-all="handleSelectAll">
          <template #selection="{ record }">
            <a-checkbox :model-value="selectedIds.includes(record.id)" @change="(checked) => handleRowSelect(record, checked)" />
          </template>

          <template #index="{ rowIndex }">{{ rowIndex + 1 }}</template>
          <template #projectName="{ record }">
            <div class="name-cell">{{ record.projectName }}</div>
          </template>
          <template #bidOpenTime="{ record }">{{ record.bidOpenTime ? record.bidOpenTime.replace('T',' ').substring(0,16) : '-' }}</template>
          <template #createdAt="{ record }">{{ record.createdAt ? record.createdAt.replace('T',' ').substring(0,16) : '-' }}</template>
          <template #actions="{ record }">
            <a-button type="text" size="small" @click="goDetail(record.id)">查看详情</a-button>
          </template>
        </a-table>
      </a-card>

      <div v-if="selectedIds.length > 0" class="batch-action-bar">
        <span class="batch-info">已选择 {{ selectedIds.length }} 项</span>
        <a-button @click="selectedIds = []">取消</a-button>
        <a-button type="primary" status="danger" @click="showDeleteDialog = true">批量删除</a-button>
      </div>

      <a-modal
        v-model:visible="showDeleteDialog"
        title="确认删除"
        :width="500"
        @before-ok="handleBatchDelete"
        @cancel="showDeleteDialog = false"
      >
        <div style="margin-bottom:12px">确认要删除选中的 {{ selectedIds.length }} 项吗？此操作不可恢复。</div>
        <div class="delete-list">
          <div v-for="row in selectedRows.slice(0, 10)" :key="row.id" class="delete-item">
            {{ row.projectName }}
          </div>
          <div v-if="selectedRows.length > 10" style="color:#999;font-size:12px;margin-top:6px">
            还有 {{ selectedRows.length - 10 }} 项...
          </div>
        </div>
      </a-modal>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue"
import { useRouter } from "vue-router"
import { Message } from "@arco-design/web-vue"
import { projectApi } from "@/api"

const router = useRouter()
const file = ref(null)
const uploading = ref(false)
const recentProjects = ref([])
const tableLoading = ref(false)
const progressPercent = ref(0)
const isExtracting = ref(false)
const selectedIds = ref([])
const selectedRows = ref([])
const showDeleteDialog = ref(false)
const deleting = ref(false)

const columns = [
  { type: 'checkbox', width: 50, align: 'center', slotName: 'selection' },
  { title: '序号', width: 60, align: 'center', slotName: 'index' },
  { title: '项目名称', dataIndex: 'projectName', minWidth: 200, slotName: 'projectName' },
  { title: '项目代码', dataIndex: 'projectCode', width: 150 },
  { title: '开标时间', dataIndex: 'bidOpenTime', width: 160, slotName: 'bidOpenTime' },
  { title: '录入时间', dataIndex: 'createdAt', width: 160, slotName: 'createdAt' },
  { title: '操作', width: 100, slotName: 'actions' }
]

const isDragOver = ref(false)
const fileInputRef = ref(null)

const triggerFileInput = () => {
  fileInputRef.value?.click()
}

const handleNativeDrop = (e) => {
  isDragOver.value = false
  const files = e.dataTransfer?.files
  if (files && files.length > 0) {
    file.value = files[0] || null
    progressPercent.value = 0
    isExtracting.value = false
  }
}

const handleFileInputChange = (e) => {
  const files = e.target.files
  if (files && files.length > 0) {
    file.value = files[0] || null
    progressPercent.value = 0
    isExtracting.value = false
  }
  e.target.value = ''
}

const handleUpload = async () => {
  if (!file.value) { Message.warning("请先选择文件"); return }
  uploading.value = true
  progressPercent.value = 0
  isExtracting.value = false
  try {
    const formData = new FormData()
    formData.append("file", file.value)
    const res = await projectApi.upload(formData, (percent) => {
      progressPercent.value = percent
      if (percent >= 100) isExtracting.value = true
    })
    if (res.code === 200) {
      Message.success("上传智能提取成功")
      router.push("/project/" + res.data.id)
    } else {
      Message.error(res.msg || "上传失败")
    }
  } catch (e) {
    Message.error("上传失败: " + (e.response?.data?.msg || e.message))
  } finally {
    uploading.value = false
    progressPercent.value = 0
    isExtracting.value = false
  }
}

const handleBatchDelete = async (done) => {
  deleting.value = true
  try {
    const res = await projectApi.batchDelete(selectedIds.value)
    if (res.code === 200) {
      Message.success("已删除 " + res.data + " 项")
      showDeleteDialog.value = false
      selectedIds.value = []
      selectedRows.value = []
      fetchRecent()
    } else {
      Message.error(res.msg || "删除失败")
    }
  } catch {
    Message.error("删除失败")
  } finally {
    deleting.value = false
  }
  done()
}

const goHome = () => router.push("/")
const goDetail = (id) => router.push("/project/" + id)

const fetchRecent = async () => {
  tableLoading.value = true
  try {
    const res = await projectApi.list({ page: 1, size: 20 })
    if (res.code === 200) {
      recentProjects.value = res.data.records
      // sync selection
      selectedRows.value = selectedRows.value.filter(r =>
        recentProjects.value.some(p => p.id === r.id)
      )
      selectedIds.value = selectedIds.value.filter(id =>
        recentProjects.value.some(p => p.id === id)
      )
    }
  } catch {} finally { tableLoading.value = false }
}

// checkbox selection sync with table data
const syncSelection = (rows) => {
  selectedRows.value = rows
  selectedIds.value = rows.map(r => r.id)
}

const handleRowSelect = (record, checked) => {
  if (checked) {
    if (!selectedIds.value.includes(record.id)) selectedIds.value.push(record.id)
  } else {
    selectedIds.value = selectedIds.value.filter(id => id !== record.id)
  }
}
const handleSelectAll = (selected, records) => {
  if (selected) {
    const newIds = records.map(r => r.id).filter(id => !selectedIds.value.includes(id))
    selectedIds.value = [...selectedIds.value, ...newIds]
  } else {
    const allIds = records.map(r => r.id)
    selectedIds.value = selectedIds.value.filter(id => !allIds.includes(id))
  }
}

onMounted(fetchRecent)
</script>

<style scoped>
.admin-container { min-height: 100vh; background: #f0f2f5; }
.header { max-width: 1400px; margin: 0 auto 24px; background: #fff; padding: 16px 24px; display: flex; align-items: center; justify-content: space-between; }
.header h2 { margin: 0; font-size: 18px; font-weight: 600; color: #1d1d1d; }
.main { max-width: 1400px; margin: 0 auto; padding: 0 24px; }
.actions { margin-top: 24px; display: flex; gap: 12px; justify-content: center; }
.progress-area { margin-top: 16px; }
.extracting-area {
  text-align: center;
  padding: 24px 0;
}
.extracting-icon {
  width: 48px;
  height: 48px;
  border: 4px solid #e8e8e8;
  border-top-color: #1650ff;
  border-radius: 50%;
  margin: 0 auto 16px;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.extracting-text {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}
.extracting-sub {
  font-size: 13px;
  color: #86909c;
}
.extracting-area {
  text-align: center;
  padding: 24px 0;
}
.extracting-icon {
  width: 48px;
  height: 48px;
  border: 4px solid #e8e8e8;
  border-top-color: #1650ff;
  border-radius: 50%;
  margin: 0 auto 16px;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.extracting-text {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}
.extracting-sub {
  font-size: 13px;
  color: #86909c;
}
.name-cell { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; line-height: 1.4; }
.batch-action-bar { position: fixed; bottom: 0; left: 0; right: 0; background: #fff; border-top: 1px solid #eee; padding: 12px 24px; display: flex; align-items: center; gap: 12px; box-shadow: 0 -2px 8px rgba(0,0,0,.1); z-index: 100; }
.batch-info { color: #333; font-size: 14px; margin-right: auto; }
.delete-list { max-height: 300px; overflow-y: auto; }
.delete-item { padding: 4px 0; border-bottom: 1px solid #f0f0f0; font-size: 13px; color: #333; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.drag-zone {
  border: 2px dashed #c0c4cc;
  border-radius: 6px;
  padding: 40px 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;
}
.drag-zone:hover, .drag-zone.drag-over {
  border-color: #1650ff;
  background: #f0f5ff;
}
.drag-zone.drag-over {
  border-style: solid;
}
.drag-icon {
  font-size: 48px;
  color: #1650ff;
  line-height: 1;
  margin-bottom: 12px;
}
.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #f0f5ff;
  border: 1px solid #d9e0fd;
  border-radius: 6px;
  margin-top: 12px;
}
.file-icon { font-size: 20px; }
.file-name { flex: 1; font-size: 14px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-size { font-size: 12px; color: #86909c; }
.upload-text { color: #4e5969; font-size: 14px; }
</style>