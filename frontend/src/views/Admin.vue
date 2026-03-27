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
          <div>将文件拖到此处，或<span style="color:#409eff">点击选择</span></div>
          <template #tip><div class="el-upload__tip">支持 PDF、Word 文档，单个文件不超过100MB</div></template>
        </el-upload>
        <div class="actions">
          <el-button type="primary" size="large" :loading="uploading" :disabled="!file" @click="handleUpload">上传并提取</el-button>
          <el-button size="large" @click="goHome">取消</el-button>
        </div>
      </el-card>
      <el-card style="margin-top:24px">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span>最近上传</span>
            <el-button link @click="goHome" type="primary">查看全部</el-button>
          </div>
        </template>
        <el-table :data="recentProjects" stripe>
          <el-table-column prop="projectName" label="项目名称" />
          <el-table-column prop="projectCode" label="项目编号" width="150" />
          <el-table-column label="开标时间" width="160">
            <template #default="{ row }">{{ row.bidOpenTime ? row.bidOpenTime.substring(0,10) : "-" }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }"><el-button link type="primary" @click="goDetail(row.id)">查看</el-button></template>
          </el-table-column>
        </el-table>
      </el-card>
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
const handleFileChange = (uploadFile) => { file.value = uploadFile.raw }
const handleUpload = async () => {
  if (!file.value) { ElMessage.warning("请先选择文件"); return }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append("file", file.value)
    const res = await projectApi.upload(formData)
    if (res.code === 200) { ElMessage.success("上传并提取成功！"); router.push("/project/" + res.data.id) }
    else { ElMessage.error(res.msg || "上传失败") }
  } catch (e) { ElMessage.error("上传失败: " + (e.response?.data?.msg || e.message)) }
  finally { uploading.value = false }
}
const goHome = () => router.push("/")
const goDetail = (id) => router.push("/project/" + id)
const fetchRecent = async () => {
  try {
    const res = await projectApi.list({ page: 1, size: 5 })
    if (res.code === 200) recentProjects.value = res.data.records
  } catch {}
}
onMounted(fetchRecent)
</script>
<style scoped>
.admin-container { min-height: 100vh; background: #f0f2f5; padding: 24px; }
.header { max-width: 800px; margin: 0 auto 24px; display: flex; align-items: center; justify-content: space-between; }
.main { max-width: 800px; margin: 0 auto; }
.actions { margin-top: 24px; display: flex; gap: 12px; justify-content: center; }
</style>