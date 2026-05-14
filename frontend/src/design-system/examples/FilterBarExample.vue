<template>
  <DsFilterBar title="筛选查询" description="筛选区统一使用白底卡片、紧凑表单和右侧操作按钮。">
    <DsKeywordSearch v-model="filters.keyword" label="关键词" placeholder="搜索姓名 / 账号" />
    <DsDeptTreeSelect v-model="filters.deptId" label="部门" include-all include-unassigned />
    <DsStatusSelect v-model="filters.status" label="状态" />
    <DsRoleMultiSelect v-model="filters.roleIds" label="角色" />
    <template #actions>
      <a-button type="primary">查询</a-button>
      <a-button>重置</a-button>
    </template>
  </DsFilterBar>
</template>

<script setup>
import { reactive } from 'vue'
import { DsDeptTreeSelect, DsFilterBar, DsKeywordSearch, DsRoleMultiSelect, DsStatusSelect } from '@/design-system'

const filters = reactive({
  keyword: '',
  deptId: '',
  status: '',
  roleIds: []
})
</script>
