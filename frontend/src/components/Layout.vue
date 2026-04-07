<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="sidebar-logo">
        <span class="logo-text">投标管理系统</span>
      </div>
      <a-menu v-model:selected-keys="selectedKeys" theme="dark" mode="inline" @menu-item-click="onMenuClick">
        <a-menu-item key="/dashboard">
          <template #icon><IconDashboard /></template>
          仪表盘
        </a-menu-item>
        <a-menu-item key="/projects">
          <template #icon><IconFolder /></template>
          项目列表
        </a-menu-item>
        <a-menu-item v-if="roleName == 'admin'" key="/admin">
          <template #icon><IconUpload /></template>
          上传文件
        </a-menu-item>
        <a-menu-item v-if="roleName == 'admin'" key="/user">
          <template #icon><IconUser /></template>
          用户管理
        </a-menu-item>
        <a-menu-item v-if="roleName == 'admin'" key="/prompt">
          <template #icon><IconSettings /></template>
          Prompt配置
        </a-menu-item>
      </a-menu>
      <div class="sidebar-footer">
        <div class="user-info">
          <span class="username">{{ userName }}</span>
          <a-tag v-if="roleName == 'admin'" color="gold" size="small">管理员</a-tag>
          <a-tag v-else color="arcoblue" size="small">普通用户</a-tag>
        </div>
        <a-button type="text" size="small" @click="handleLogout">退出</a-button>
      </div>
    </aside>
    <div class="layout-main">
      <div class="layout-breadcrumb">
        <a-breadcrumb>
          <a-breadcrumb-item><router-link to="/dashboard">??</router-link></a-breadcrumb-item>
          <a-breadcrumb-item v-if="$route.path == '/projects'">????</a-breadcrumb-item>
          <a-breadcrumb-item v-if="$route.path.startsWith('/project/') && $route.path != '/projects'">????</a-breadcrumb-item>
          <a-breadcrumb-item v-if="$route.path.startsWith('/project/') && $route.path != '/projects'">????</a-breadcrumb-item>
          <a-breadcrumb-item v-if="$route.path == '/admin'">????</a-breadcrumb-item>
          <a-breadcrumb-item v-if="$route.path == '/admin'">????</a-breadcrumb-item>
          <a-breadcrumb-item v-if="$route.path == '/user'">????</a-breadcrumb-item>
          <a-breadcrumb-item v-if="$route.path == '/prompt'">Prompt??</a-breadcrumb-item>
          <a-breadcrumb-item v-if="$route.path == '/dashboard'">????</a-breadcrumb-item>
        </a-breadcrumb>

      </div>
      <router-view />

    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { IconDashboard, IconFolder, IconUpload, IconUser, IconSettings } from '@arco-design/web-vue/es/icon'

const router = useRouter()
const route = useRoute()
const userName = localStorage.getItem('username') || '用户'
const roleName = localStorage.getItem('role') || 'user'

const selectedKeys = ref([route.path])

const onMenuClick = (key) => { router.push(key) }

const handleLogout = () => { localStorage.clear(); router.push('/login') }
</script>
<style scoped>
.layout { display: flex; min-height: 100vh; }
.sidebar { width: 220px; background: #1e1e1e; display: flex; flex-direction: column; flex-shrink: 0; }
.sidebar-logo { height: 60px; display: flex; align-items: center; justify-content: center; border-bottom: 1px solid rgba(255,255,255,0.1); }
.logo-text { color: #165DFF; font-size: 16px; font-weight: 600; letter-spacing: 1px; }
.layout-main { flex: 1; overflow: auto; background: #f0f2f5; }
.layout-breadcrumb { background: #fff; padding: 12px 24px; border-bottom: 1px solid #e8e8e8; }
.sidebar-footer { margin-top: auto; padding: 16px; border-top: 1px solid rgba(255,255,255,0.1); display: flex; flex-direction: column; gap: 8px; }
.user-info { display: flex; align-items: center; gap: 8px; color: rgba(255,255,255,0.7); font-size: 13px; }
</style>
