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
        <a-menu-item key="/help">
          <template #icon><IconQuestion /></template>
          帮助文档
        </a-menu-item>
      </a-menu>
    </aside>
    <div class="layout-main">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { IconDashboard, IconFolder, IconUpload, IconUser, IconSettings, IconQuestion } from '@arco-design/web-vue/es/icon'

const router = useRouter()
const route = useRoute()
const roleName = localStorage.getItem('role') || 'user'

const selectedKeys = ref([route.path])

const onMenuClick = (key) => { router.push(key) }
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; }
.sidebar { width: 220px; background: #1e1e1e; display: flex; flex-direction: column; flex-shrink: 0; }
.sidebar-logo { height: 60px; display: flex; align-items: center; justify-content: center; border-bottom: 1px solid rgba(255,255,255,0.1); }
.logo-text { color: #165DFF; font-size: 16px; font-weight: 600; letter-spacing: 1px; }
.layout-main { flex: 1; overflow: auto; background: #f0f2f5; }
</style>