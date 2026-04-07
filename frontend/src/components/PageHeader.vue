<template>
  <div class="page-header">
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
</template>

<script setup>
import { computed, useRoute } from 'vue'

defineProps({
  title: { type: String, required: true }
})

const route = useRoute()

const showProjectList = computed(() => {
  return route.path.startsWith('/project/') && route.path !== '/projects'
})
</script>

<style scoped>
.page-header {
  background: #fff;
  padding: 16px 24px;
  border-bottom: 1px solid #e8e8e8;
}
.page-header h2 {
  margin: 8px 0 0;
  font-size: 18px;
  font-weight: 600;
  color: #1d1d1d;
}
</style>