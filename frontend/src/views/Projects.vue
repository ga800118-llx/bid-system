<template>
  <div class="projects-page">
    <div class="page-header">
      <h2>项目列表</h2>
    </div>
    <div class="main">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="按项目名称搜索" style="width:300px" clearable @keyup.enter="handleSearch">
          <template #append><el-button icon="Search" @click="handleSearch" /></template>
        </el-input>
        <el-button v-if="role == '"'"'admin'"'"'" type="primary" icon="Upload" @click="goAdmin">上传招标文件</el-button>
      </div>
      <el-table :data="tableData" stripe style="width:100%" v-loading="loading">
        <el-table-column label="序号" width="70" align="center">
          <template #default="{ $index }">{{ (page - 1) * size + $index + 1 }}</template>
        </el-table-column>
        <el-table-column prop="projectName" label="项目名称" min-width="200" />
        <el-table-column prop="projectCode" label="项目编号" width="180" />
        <el-table-column prop="clientUnit" label="发标单位" width="150" />
        <el-table-column prop="bidOpenTime" label="开标时间" width="160">
          <template #default="{ row }">{{ row.bidOpenTime ? row.bidOpenTime.replace('"'"'T'"'"','"'"' '"'"').substring(0,16) : "-" }}</template>
        </el-table-column>
        <el-table-column prop="ceilingPrice" label="拦标价(万)" width="120">
          <template #default="{ row }">{{ row.ceilingPrice ? row.ceilingPrice : "-" }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="录入时间" width="160">
          <template #default="{ row }">{{ row.createdAt ? row.createdAt.replace('"'"'T'"'"','"'"' '"'"').substring(0,16) : "-" }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="goDetail(row.id)">查看详情</el-button></template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="sizes, total, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="fetchData"
        />
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from "vue"
import { useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import { projectApi } from "@/api"
const router = useRouter()
const role = localStorage.getItem("role") || "user"
const keyword = ref("")
const page = ref(1)
const size = ref(50)
const total = ref(0)
const loading = ref(false)
const tableData = ref([])
const fetchData = async () => {
  loading.value = true
  try {
    const res = await projectApi.list({ keyword: keyword.value, page: page.value, size: size.value })
    if (res.code === 200) { tableData.value = res.data.records; total.value = res.data.total }
  } catch { ElMessage.error("加载失败") } finally { loading.value = false }
}
const handleSizeChange = () => { page.value = 1; fetchData() }
const goDetail = (id) => router.push("/project/" + id)
const goAdmin = () => router.push("/admin")
const handleSearch = () => { page.value = 1; fetchData() }
onMounted(fetchData)
</script>
<style scoped>
.projects-page { min-height: 100vh; background: #f0f2f5; }
.page-header { background: #fff; padding: 16px 24px; border-bottom: 1px solid #e8e8e8; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; color: #1d1d1d; }
.main { padding: 24px; max-width: 1400px; margin: 0 auto; }
.toolbar { display: flex; gap: 16px; margin-bottom: 16px; justify-content: space-between; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
