<template>
  <div class="admin-container">
    <div class="header">
      <a-button @click="goHome">返回首页</a-button>
      <h2>上传招标文档</h2>
      <div></div>
    </div>
    <div class="main">
      <a-card>
        <a-upload
          drag
          :auto-upload="false"
          :limit="1"
          :on-change="handleFileChange"
          accept=".pdf,.doc,.docx"
          style="width:100%"
        >
          <template #upload-icon>
            <a-icon-plus />
          </template>
          <div class="upload-text">
            将文件拖到此处，或<span style="color:#1650ff">点击选择</span>
          </div>
          <template #tip>
            <div style="color:#86909c;font-size:12px;margin-top:8px">支持 PDF、Word 文档，单个文件不超过100MB</div>
          </template>
        </a-upload>
        <div class="actions">
          <a-button type="primary" size="large" :loading="uploading" :disabled="!file" @click="handleUpload">上传智能提取</a-button>
          <a-button size="large" @click="goHome">取消</a-button>
        </div>
        <div v-if="uploading" class="progress-area">
          <a-progress
            :percent="progressPercent"
            :status="isExtracting ? 'active' : 'success'"
            :format="(p) => isExtracting ? 'AI提取信息中，请稍候...' : `上传进度 ${p}%`"
          />
        </div>
      </a-card>
      <a-card style="margin-top:24px">
        <template #title>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span>最近上传</span>
            <a-button type="text" size="small" @click="goHome">查看全部</a-button>
          </div>
        </template>
        <a-table :columns="columns" :data="recentProjects" :loading="tableLoading" :pagination="false" row-key="id" stripe>
          <template #selection="{ record, onChange }">
            <a-checkbox :model-value="selectedIds.includes(record.id)" @change="(val) => { onChange(val ? [...selectedIds, record.id] : selectedIds.filter(id => id !== record.id)) }" />
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
  { title: '', width: 50, align: 'center', slotName: 'selection' },
  { title: '序号', width: 60, align: 'center', slotName: 'index' },
  { title: '项目名称', dataIndex: 'projectName', minWidth: 200, slotName: 'projectName' },
  { title: '项目代码', dataIndex: 'projectCode', width: 150 },
  { title: '开标时间', dataIndex: 'bidOpenTime', width: 160, slotName: 'bidOpenTime' },
  { title: '录入时间', dataIndex: 'createdAt', width: 160, slotName: 'createdAt' },
  { title: '操作', width: 100, slotName: 'actions' }
]

const handleFileChange = (fileList) => {
  file.value = fileList[0]?.file || null
  progressPercent.value = 0
  isExtracting.value = false
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

onMounted(fetchRecent)
</script>

<style scoped>
.admin-container { min-height: 100vh; background: #f0f2f5; }
.header { max-width: 1400px; margin: 0 auto 24px; background: #fff; padding: 16px 24px; display: flex; align-items: center; justify-content: space-between; }
.header h2 { margin: 0; font-size: 18px; font-weight: 600; color: #1d1d1d; }
.main { max-width: 1400px; margin: 0 auto; padding: 0 24px; }
.actions { margin-top: 24px; display: flex; gap: 12px; justify-content: center; }
.progress-area { margin-top: 16px; }
.name-cell { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; line-height: 1.4; }
.batch-action-bar { position: fixed; bottom: 0; left: 0; right: 0; background: #fff; border-top: 1px solid #eee; padding: 12px 24px; display: flex; align-items: center; gap: 12px; box-shadow: 0 -2px 8px rgba(0,0,0,.1); z-index: 100; }
.batch-info { color: #333; font-size: 14px; margin-right: auto; }
.delete-list { max-height: 300px; overflow-y: auto; }
.delete-item { padding: 4px 0; border-bottom: 1px solid #f0f0f0; font-size: 13px; color: #333; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.upload-text { color: #4e5969; font-size: 14px; }
</style>