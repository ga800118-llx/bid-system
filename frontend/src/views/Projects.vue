<template>
  <div class="projects-page">
    <PageHeader title="项目列表" />>
    <div class="main">
      <div class="toolbar">
        <a-input-search v-model="keyword" placeholder="请输入项目名称搜索" style="width:300px" @search="handleSearch" allow-clear />
        <a-button v-if="roleName == 'admin'" type="primary" @click="goAdmin">上传列表文件</a-button>
      </div>
      <a-table :columns="columns" :data="tableData" :loading="loading" :pagination="false" row-key="id" stripe>
        <template #index="{ rowIndex }">{{ (page - 1) * size + rowIndex + 1 }}</template>
        <template #projectName="{ record }">
          <span class="name-cell">{{ record.projectName }}</span>
        </template>
        <template #bidOpenTime="{ record }">{{ record.bidOpenTime ? record.bidOpenTime.replace('T',' ').substring(0,16) : '-' }}</template>
        <template #ceilingPrice="{ record }">{{ record.ceilingPrice ? record.ceilingPrice : '-' }}</template>
        <template #createdAt="{ record }">{{ record.createdAt ? record.createdAt.replace('T',' ').substring(0,16) : '-' }}</template>
        <template #actions="{ record }">
          <a-button type="text" size="small" @click="goDetail(record.id)">查看详情</a-button>
        </template>
      </a-table>
      <div class="pagination">
        <a-pagination
          v-model:current="page"
          v-model:page-size="size"
          :total="total"
          :page-size-options="[10, 20, 50, 100]"
          show-total
          show-page-size
          @page-size-change="handleSizeChange"
          @change="fetchData"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { projectApi } from '@/api'

const router = useRouter()
const roleName = localStorage.getItem('role') || 'user'
const keyword = ref('')
const page = ref(1)
const size = ref(50)
const total = ref(0)
const loading = ref(false)
const tableData = ref([])

const columns = [
  { title: '序号', width: 70, align: 'center', slotName: 'index' },
  { title: '项目名称', dataIndex: 'projectName', minWidth: 200, slotName: 'projectName' },
  { title: '项目编号', dataIndex: 'projectCode', width: 180 },
  { title: '招标单位', dataIndex: 'clientUnit', width: 150 },
  { title: '开标时间', dataIndex: 'bidOpenTime', width: 160, slotName: 'bidOpenTime' },
  { title: '拦标价(元)', dataIndex: 'ceilingPrice', width: 120, slotName: 'ceilingPrice' },
  { title: '录入时间', dataIndex: 'createdAt', width: 160, slotName: 'createdAt' },
  { title: '操作', width: 120, fixed: 'right', slotName: 'actions' }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await projectApi.list({ keyword: keyword.value, page: page.value, size: size.value })
    if (res.code === 200) { tableData.value = res.data.records; total.value = res.data.total }
  } catch { Message.error('加载失败') } finally { loading.value = false }
}
const handleSizeChange = () => { page.value = 1; fetchData() }
const goDetail = (id) => router.push('/project/' + id)
const goAdmin = () => router.push('/admin')
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
.name-cell { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; line-height: 1.4; }
</style>