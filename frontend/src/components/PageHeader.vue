<template>
  <div class="page-header">
    <div class="header-left">
      <a-breadcrumb>
        <a-breadcrumb-item><router-link to="/dashboard">首页</router-link></a-breadcrumb-item>
        <a-breadcrumb-item v-if="showProjectList">项目列表</a-breadcrumb-item>
        <a-breadcrumb-item v-if="route.path.startsWith('/project/') && route.path !== '/projects'">项目详情</a-breadcrumb-item>
        <a-breadcrumb-item v-if="route.path === '/projects'">项目列表</a-breadcrumb-item>
        <a-breadcrumb-item v-if="route.path === '/admin'">上传管理</a-breadcrumb-item>
        <a-breadcrumb-item v-if="route.path === '/user'">用户管理</a-breadcrumb-item>
        <a-breadcrumb-item v-if="route.path === '/prompt'">Prompt配置</a-breadcrumb-item>
        <a-breadcrumb-item v-if="route.path === '/dashboard'">数据看板</a-breadcrumb-item>
      </a-breadcrumb>
      <h2>{{ title }}</h2>
    </div>
    <div class="header-right">
      <span class="username">{{ userName }}</span>
      <a-tag v-if="userRole === 'admin'" color="gold" size="small">管理员</a-tag>
      <a-tag v-else color="arcoblue" size="small">普通用户</a-tag>
      <a-button type="text" size="small" @click="handleLogout">退出</a-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

defineProps({
  title: { type: String, required: true }
})

const route = useRoute()
const router = useRouter()
const userName = localStorage.getItem('username') || '用户'
const userRole = localStorage.getItem('role') || 'user'

const showProjectList = computed(() => {
  return route.path.startsWith('/project/') && route.path !== '/projects'
})

const handleLogout = () => {
  localStorage.clear()
  router.push('/login')
}
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 16px 24px;
  border-bottom: 1px solid #e8e8e8;
}
.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.header-left h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1d1d1d;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.username {
  font-size: 13px;
  color: #666;
}
</style>