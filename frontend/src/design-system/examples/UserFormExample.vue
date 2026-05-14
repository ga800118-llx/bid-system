<template>
  <div class="example-grid">
    <DsInput v-model="form.realName" label="姓名" required />
    <DsInput v-model="form.username" label="账号" required />
    <DsDeptTreeSelect v-model="form.deptId" label="所属部门" include-unassigned />
    <DsRoleMultiSelect v-model="form.roleIds" label="关联角色" />
    <DsStatusSelect v-model="form.status" label="账号状态" />
    <DsPasswordInput v-model="form.password" label="初始密码" />
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { DsDeptTreeSelect, DsInput, DsPasswordInput, DsRoleMultiSelect, DsStatusSelect } from '@/design-system'

const form = reactive({
  realName: '',
  username: '',
  deptId: '',
  roleIds: [],
  status: 1,
  password: ''
})
</script>

<style scoped>
.example-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
@media (max-width: 960px) {
  .example-grid { grid-template-columns: 1fr; }
}
</style>
