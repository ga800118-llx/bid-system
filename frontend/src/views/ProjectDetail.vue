<template>
  <div class="detail-container">
    <div class="header">
      <el-button icon="Back" @click="goBack">返回</el-button>
      <el-button v-if="userRole == 'admin'" type="danger" icon="Delete" @click="handleDelete">删除项目</el-button>
    </div>
    <div class="content" v-loading="loading">
      <el-card v-if="project">
        <template #header>
          <div class="card-title"><h2>{{ project.projectName || "项目详情" }}</h2><el-tag>{{ project.projectCode || "无编号" }}</el-tag></div>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="项目名称" :span="2">{{ project.projectName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="项目编号">{{ project.projectCode || "-" }}</el-descriptions-item>
          <el-descriptions-item label="发标单位">{{ project.clientUnit || "-" }}</el-descriptions-item>
          <el-descriptions-item label="招标代理机构">{{ project.biddingAgency || "-" }}</el-descriptions-item>
          <el-descriptions-item label="开标时间">{{ project.bidOpenTime ? project.bidOpenTime.replace("T"," ").substring(0,16) : "-" }}</el-descriptions-item>
          <el-descriptions-item label="投诉质疑截止">{{ project.complaintDeadline ? project.complaintDeadline.replace("T"," ").substring(0,16) : "-" }}</el-descriptions-item>
          <el-descriptions-item label="拦标价（最高投标限价）">{{ project.ceilingPrice ? project.ceilingPrice + " 元" : "-" }}</el-descriptions-item>
          <el-descriptions-item label="下限价（投标成本警戒线）">{{ project.floorPrice ? project.floorPrice + " 元" : "-" }}</el-descriptions-item>
          <el-descriptions-item label="投标保证金">{{ project.bidBond ? project.bidBond + " 元" : "-" }}</el-descriptions-item>
          <el-descriptions-item label="履约保证金">{{ project.performanceBond ? project.performanceBond + " 元" : "-" }}</el-descriptions-item>
          <el-descriptions-item label="合同付款方式" :span="2">{{ project.contractPayment || "-" }}</el-descriptions-item>
          <el-descriptions-item label="专家的组成" :span="2">{{ project.expertComposition || "-" }}</el-descriptions-item>
          <el-descriptions-item label="价格分评分方式" :span="2">{{ project.priceScoreMethod || "-" }}</el-descriptions-item>
          <el-descriptions-item label="主观分分值" :span="2">{{ project.subjectiveScoreDetails || "-" }}</el-descriptions-item>
        </el-descriptions>
        <div class="file-section" v-if="project.fileOriginalName">
          <el-divider />
          <h3>原始文件</h3>
          <div class="file-info">
            <el-icon><Document /></el-icon>
            <span>{{ project.fileOriginalName }}</span>
            <el-button type="primary" size="small" icon="Download" @click="handleDownload">下载</el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from "vue"
import { useRoute, useRouter } from "vue-router"
import { ElMessage, ElMessageBox } from "element-plus"
import { projectApi } from "@/api"
const route = useRoute()
const router = useRouter()
const userRole = localStorage.getItem("role") || "user"
const project = ref(null)
const loading = ref(true)
const fetchDetail = async () => {
  loading.value = true
  try {
    const res = await projectApi.detail(route.params.id)
    if (res.code === 200) project.value = res.data
    else { ElMessage.error(res.msg); router.push("/") }
  } catch { ElMessage.error("加载失败") } finally { loading.value = false }
}
const handleDownload = async () => {
  try {
    const res = await projectApi.download(route.params.id)
    const blob = new Blob([res])
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url; a.download = project.value.fileOriginalName; a.click()
    window.URL.revokeObjectURL(url)
  } catch { ElMessage.error("下载失败") }
}
const handleDelete = async () => {
  try {
    await ElMessageBox.confirm("确定删除此项目？此操作不可恢复。", "警告", { type: "warning" })
    await projectApi.delete(route.params.id)
    ElMessage.success("删除成功"); router.push("/")
  } catch (e) { if (e !== "cancel") ElMessage.error("删除失败") }
}
const goBack = () => router.back()
onMounted(fetchDetail)
</script>
<style scoped>
.detail-container { min-height: 100vh; background: #f0f2f5; padding: 24px; }
.header { max-width: 900px; margin: 0 auto 16px; display: flex; justify-content: space-between; }
.content { max-width: 900px; margin: 0 auto; }
.card-title { display: flex; align-items: center; gap: 12px; }
.file-section { margin-top: 16px; }
.file-info { display: flex; align-items: center; gap: 12px; padding: 12px; background: #f5f7fa; border-radius: 4px; }
</style>