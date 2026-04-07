<template>
  <div class="detail-container">
    <div class="header">
      <a-breadcrumb style="margin-bottom:12px">
        <a-breadcrumb-item><router-link to="/home">首页</router-link></a-breadcrumb-item>
        <a-breadcrumb-item><router-link to="/projects">项目列表</router-link></a-breadcrumb-item>
        <a-breadcrumb-item>项目详情</a-breadcrumb-item>
      </a-breadcrumb>
      <a-button @click="goBack">返回</a-button>
      <a-button v-if="userRole == 'admin'" status="danger" @click="handleDelete">删除项目</a-button>
    </div>
    <div class="content" :loading="loading">
      <a-card v-if="project">
        <template #header>
          <div class="card-title">
            <h2>{{ project.projectName || '项目详情' }}</h2>
            <a-tag>{{ project.projectCode || '无编号' }}</a-tag>
          </div>
        </template>
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="项目名称" :span="2">{{ project.projectName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="项目编号">{{ project.projectCode || '-' }}</a-descriptions-item>
          <a-descriptions-item label="招标单位">{{ project.clientUnit || '-' }}</a-descriptions-item>
          <a-descriptions-item label="招标代理机构">{{ project.biddingAgency || '-' }}</a-descriptions-item>
          <a-descriptions-item label="开标时间">{{ project.bidOpenTime ? project.bidOpenTime.replace('T',' ').substring(0,16) : '-' }}</a-descriptions-item>
          <a-descriptions-item label="质疑截止日期">{{ project.complaintDeadline ? project.complaintDeadline.replace('T',' ').substring(0,16) : '-' }}</a-descriptions-item>
          <a-descriptions-item label="拦标价（最高投标限价）">{{ project.ceilingPrice ? project.ceilingPrice + ' 元' : '-' }}</a-descriptions-item>
          <a-descriptions-item label="最低限价（成本警戒线）">{{ project.floorPrice ? project.floorPrice + ' 元' : '-' }}</a-descriptions-item>
          <a-descriptions-item label="投标保证金">{{ project.bidBond ? project.bidBond + ' 元' : '-' }}</a-descriptions-item>
          <a-descriptions-item label="履约保证金">{{ project.performanceBond ? project.performanceBond + ' 元' : '-' }}</a-descriptions-item>
          <a-descriptions-item label="合同款项方式" :span="2">{{ project.contractPayment || '-' }}</a-descriptions-item>
          <a-descriptions-item label="专家点评构成" :span="2">{{ project.expertComposition || '-' }}</a-descriptions-item>
          <a-descriptions-item label="价格评分方法（评分办法）" :span="2">{{ project.priceScoreMethod || '-' }}</a-descriptions-item>
          <a-descriptions-item label="技术分详情（主观分）" :span="2">{{ project.subjectiveScoreDetails || '-' }}</a-descriptions-item>
        </a-descriptions>

        <a-divider />
        <div class="file-section" v-if="project.fileOriginalName">
          <h3>文件信息</h3>
          <div class="file-info-row">
            <IconFile />
            <span class="file-name">{{ project.fileOriginalName }}</span>
            <a-button type="primary" size="small" @click="handleDownload">下载原始文件</a-button>
          </div>
          <div v-if="project.textLength || project.pageCount" class="stats-row">
            <span v-if="project.pageCount">PDF 总页数：<strong>{{ project.pageCount }} 页</strong></span>
            <span v-if="project.textLength">PDF 总字数：<strong>{{ project.textLength.toLocaleString() }} 字</strong></span>
            <a-button type="outline" size="small" @click="handleDownloadMarkdown" style="margin-left:auto">下载原文 Markdown</a-button>
          </div>
          <div v-else class="stats-row">
            <a-button type="outline" size="small" @click="handleDownloadMarkdown">下载原文 Markdown</a-button>
          </div>
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message, Modal } from '@arco-design/web-vue'
import { IconFile } from '@arco-design/web-vue/es/icon'
import { projectApi } from '@/api'

const route = useRoute()
const router = useRouter()
const userRole = localStorage.getItem('role') || 'user'
const project = ref(null)
const loading = ref(true)

const fetchDetail = async () => {
  loading.value = true
  try {
    const res = await projectApi.detail(route.params.id)
    if (res.code === 200) project.value = res.data
    else { Message.error(res.msg); router.push('/') }
  } catch { Message.error('加载失败') } finally { loading.value = false }
}

const handleDownload = async () => {
  try {
    const res = await projectApi.download(route.params.id)
    const blob = new Blob([res])
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = project.value.fileOriginalName; a.click()
    window.URL.revokeObjectURL(url)
  } catch { Message.error('下载失败') }
}

const handleDownloadMarkdown = async () => {
  try {
    const res = await projectApi.downloadMarkdown(route.params.id)
    const blob = new Blob([res], { type: 'text/markdown;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = project.value.fileOriginalName.replace(/\.[^.]+$/, '_[MD.md]'); a.click()
    window.URL.revokeObjectURL(url)
  } catch { Message.error('下载失败') }
}

const handleDelete = async () => {
  Modal.warning({
    title: '确认删除',
    content: '确认删除此项目？此操作不可恢复。',
    okText: '确认删除',
    cancelText: '取消',
    onOk: async () => {
      try {
        await projectApi.delete(route.params.id)
        Message.success('删除成功')
        router.push('/')
     
      } catch { Message.error('删除失败') }
    }
  })
}

const goBack = () => router.back()
onMounted(fetchDetail)
</script>

<style scoped>
.detail-container { min-height: 100vh; background: #f0f2f5; padding: 24px; }
.header { max-width: 900px; margin: 0 auto 16px; display: flex; justify-content: space-between; }
.content { max-width: 900px; margin: 0 auto; }
.card-title { display: flex; align-items: center; gap: 12px; }
.card-title h2 { margin: 0; font-size: 16px; font-weight: 600; }
.file-section { margin-top: 16px; }
.file-section h3 { margin: 0 0 12px; font-size: 14px; font-weight: 600; color: #333; }
.file-info-row { display: flex; align-items: center; gap: 12px; padding: 12px; background: #f5f7fa; border-radius: 4px; }
.file-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.stats-row { display: flex; align-items: center; gap: 24px; padding: 12px; background: #f0f9ff; border-radius: 4px; margin-top: 8px; font-size: 14px; color: #606266; }
</style>