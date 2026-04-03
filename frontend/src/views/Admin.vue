<template>
  <div class="admin-container">
    <div class="header">
      <el-button icon="Back" @click="goHome">返回首页</el-button>
      <h2>上传招标文件</h2>
      <div></div>
    </div>
    <div class="main">
      <el-card>
        <el-upload ref="uploadRef" drag :auto-upload="false" :limit="1" :on-change="handleFileChange" :file-list="fileList" accept=".pdf,.doc,.docx">
          <el-icon><UploadFilled /></el-icon>
          <div>将文件拖到此处，<span style="color:#409eff">点击选择</span></div>
          <template #tip><div class="el-upload__tip">支持 PDF、Word 文档，单个文件不超过100MB</div></template>
        </el-upload>
        <div class="actions">
          <el-button type="primary" size="large" :loading="uploading" :disabled="!file" @click="handleUpload">上传并提取</el-button>
          <el-button size="large" @click="goHome">取消</el-button>
        </div>
        <div v-if="uploading" class="progress-area">
          <el-progress
            :percentage="progressPercent"
            :indeterminate="isExtracting"
            :format="(p) => isExtracting ? '正在提取信息，请稍候...' : `上传中... ${p}%`"
          />
        </div>
      </el-card>
      <el-card style="margin-top:24px">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span>最近上传</span>
            <el-button link @click="goHome" type="primary">查看全部</el-button>
          </div>
        </template>
        <el-table :data="recentProjects" stripe @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="序号" width="60" align="center">
            <template #default="{ $index }">{{ $index + 1 }}</template>
          </el-table-column>
          <el-table-column prop="projectName" label="项目名称">
            <template #default="{ row }">
              <div class="name-cell">{{ row.projectName }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="projectCode" label="项目编号" width="150" />
          <el-table-column label="开标时间" width="160">
            <template #default="{ row }">{{ row.bidOpenTime ? row.bidOpenTime.replace('T',' ').substring(0,16) : "-" }}</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="录入时间" width="160">
            <template #default="{ row }">{{ row.createdAt ? row.createdAt.replace('T',' ').substring(0,16) : "-" }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }"><el-button link type="primary" @click="goDetail(row.id)">查看</el-button></template>
          </el-table-column>
        </el-table>
      </el-card>

    <div v-if="selectedIds.length > 0" class="batch-action-bar">
      <span class="batch-info">已选择 {{ selectedIds.length }} 项</span>
      <el-button @click="selectedIds = []; selectedRows = []">取消</el-button>
      <el-button type="danger" @click="confirmBatchDelete">批量删除</el-button>
    </div>

    <el-dialog v-model="showDeleteDialog" title="确认删除" width="500px">
      <div style="margin-bottom: 12px;">确定要删除以下 {{ selectedIds.length }} 项吗？此操作不可恢复。</div>
      <div class="delete-list">
        <div v-for="row in selectedRows.slice(0, 10)" :key="row.id" class="delete-item">
          {{ row.projectName }}
        </div>
        <div v-if="selectedRows.length > 10" style="color: #999; font-size: 12px; margin-top: 6px;">
          还有 {{ selectedRows.length - 10 }} 项...
        </div>
      </div>
      <template #footer>
        <el-button @click="showDeleteDialog = false">取消</el-button>
        <el-button type="danger" :loading="deleting" @click="handleBatchDelete">确认删除</el-button>
      </template>
    </el-dialog>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from "vue"
import { useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import { projectApi } from "@/api"
const router = useRouter()
const uploadRef = ref()
const file = ref(null)
const fileList = ref([])
const uploading = ref(false)
const recentProjects = ref([])
const progressPercent = ref(0)
const isExtracting = ref(false)
const selectedIds = ref([])
const selectedRows = ref([])
const showDeleteDialog = ref(false)
const deleting = ref(false)

const handleSelectionChange = (rows) => {
  selectedRows.value = rows
  selectedIds.value = rows.map(r => r.id)
}

const confirmBatchDelete = () => {
  if (!selectedIds.value.length) return
  showDeleteDialog.value = true
}

const handleBatchDelete = async () => {
  deleting.value = true
  try {
    const res = await projectApi.batchDelete(selectedIds.value)
    if (res.code === 200) {
      ElMessage.success("Deleted " + res.data + " items")
      showDeleteDialog.value = false
      selectedIds.value = []
      selectedRows.value = []
      fetchRecent()
    } else {
      ElMessage.error(res.msg || "Delete failed")
    }
  } catch (e) {
    ElMessage.error("Delete failed")
  } finally {
    deleting.value = false
  }
}


const handleFileChange = (uploadFile) => {
  file.value = uploadFile.raw
  progressPercent.value = 0
  isExtracting.value = false
}

const handleUpload = async () => {
  if (!file.value) { ElMessage.warning("请先选择文件"); return }
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
      ElMessage.success("上传并提取成功！")
      router.push("/project/" + res.data.id)
    } else {
      ElMessage.error(res.msg || "上传失败")
    }
  } catch (e) {
    ElMessage.error("上传失败: " + (e.response?.data?.msg || e.message))
  } finally {
    uploading.value = false
    progressPercent.value = 0
    isExtracting.value = false
  }
}

const goHome = () => router.push("/")
const goDetail = (id) => router.push("/project/" + id)
const fetchRecent = async () => {
  try {
    const res = await projectApi.list({ page: 1, size: 20 })
    if (res.code === 200) recentProjects.value = res.data.records
  } catch {}
}
onMounted(fetchRecent)
</script>
<style scoped>
.admin-container { min-height: 100vh; background: #f0f2f5; }
.header { max-width: 1400px; margin: 0 auto 24px; display: flex; align-items: center; justify-content: space-between; }
.main { max-width: 1400px; margin: 0 auto; }
.actions { margin-top: 24px; display: flex; gap: 12px; justify-content: center; }
.progress-area { margin-top: 16px; }
.name-cell { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; line-height: 1.4; }

.batch-action-bar { position: fixed; bottom: 0; left: 0; right: 0; background: #fff; border-top: 1px solid #eee; padding: 12px 24px; display: flex; align-items: center; gap: 12px; box-shadow: 0 -2px 8px rgba(0,0,0,.1); z-index: 100; }
.batch-info { color: #333; font-size: 14px; margin-right: auto; }
.delete-list { max-height: 300px; overflow-y: auto; }
.delete-item { padding: 4px 0; border-bottom: 1px solid #f0f0f0; font-size: 13px; color: #333; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>
