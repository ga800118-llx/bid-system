<template>
  <div class="example-grid">
    <DsDeptTreeSelect v-model="state.deptId" label="部门树选择" include-all include-unassigned />
    <DsStatusSelect v-model="state.status" label="状态选择" />
    <DsDictSelect v-model="state.messageType" type-code="message_type" label="字典选择" />
    <DsFileCategorySelect v-model="state.fileCategory" label="文件分类" />
    <DsRoleMultiSelect v-model="state.roleIds" label="角色多选" />
    <DsUserAvatarSelect v-model="state.userId" label="人员选择" />
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { DsDeptTreeSelect, DsDictSelect, DsFileCategorySelect, DsRoleMultiSelect, DsStatusSelect, DsUserAvatarSelect } from '@/design-system'

const state = reactive({
  deptId: '',
  status: '',
  messageType: '',
  fileCategory: '',
  roleIds: [],
  userId: ''
})
</script>

<style scoped>
.example-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
@media (max-width: 960px) {
  .example-grid { grid-template-columns: 1fr; }
}
</style>
