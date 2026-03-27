<template>
  <div class="home-container">
    <div class="header">
      <div class="header-left"><h2>招投标管理系统</h2></div>
      <div class="header-right">
        <span>欢迎，{{ username }}</span>
        <el-tag v-if="role == 'admin'" type="warning" size="small">管理用户</el-tag>
        <el-tag v-else size="small">普通用户</el-tag>
        <el-button link @click="handleLogout">退出</el-button>
      </div>
    </div>
    <div class="main">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="按项目名称搜索" style="width:300px" clearable @keyup.enter="handleSearch">
          <template #append><el-button icon="Search" @click="handleSearch" /></template>
        </el-input>
        <el-button v-if="role == 'admin'" type="primary" icon="Upload" @click="goAdmin">上传招标文件</el-button>
      </div>
      <el-table :data="tableData" stripe style="width:100%" v-loading="loading">
        <el-table-column prop="projectName" label="项目名称" min-width="200" />
        <el-table-column prop="projectCode" label="项目编号" width="180" />
        <el-table-column prop="clientUnit" label="发标单位" width="150" />
        <el-table-column prop="bidOpenTime" label="开标时间" width="160">
          <template #default="{ row }">{{ row.bidOpenTime ? row.bidOpenTime.replace("T"," ").substring(0,16) : "-" }}</template>
        </el-table-column>
        <el-table-column prop="ceilingPrice" label="拦标价(元)" width="120">
          <template #default="{ row }">{{ row.ceilingPrice ? row.ceilingPrice : "-" }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="goDetail(row.id)">查看详情</el-button></template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
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
const username = localStorage.getItem("username") || "用户"
const role = localStorage.getItem("role") || "user"
const keyword = ref("")
const page = ref(1)
const size = ref(10)
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
const goDetail = (id) => router.push("/project/" + id)
const goAdmin = () => router.push("/admin")
const handleSearch = () => { page.value = 1; fetchData() }
const handleLogout = () => { localStorage.clear(); router.push("/login") }
onMounted(fetchData)
</script>
<style scoped>
.home-container { min-height: 100vh; background: #f0f2f5; }
.header { background: #fff; padding: 0 24px; height: 60px; display: flex; align-items: center; justify-content: space-between; box-shadow: 0 1px 4px rgba(0,0,0,.1); }
.header-right { display: flex; align-items: center; gap: 12px; }
.main { padding: 24px; max-width: 1400px; margin: 0 auto; }
.toolbar { display: flex; gap: 16px; margin-bottom: 16px; justify-content: space-between; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>