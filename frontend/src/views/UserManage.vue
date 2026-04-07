<template>
  <div class="user-container">
    <div class="header">
      <div class="header-left">
              <a-breadcrumb style="margin-bottom:8px">
        <a-breadcrumb-item><router-link to="/dashboard">首页</router-link></a-breadcrumb-item>
        <a-breadcrumb-item>用户管理</a-breadcrumb-item>
      </a-breadcrumb>
        <h2>用户管理</h2>
      </div>
      <div class="header-right">
        <a-link @click="$router.push('/')">返回首页</a-link>
      </div>
    </div>
    <div class="main">
      <a-table :columns="columns" :data="tableData" :loading="loading" :pagination="false" row-key="id" stripe>
        <template #index="{ rowIndex }">{{ rowIndex + 1 }}</template>
        <template #role="{ record }">
          <a-select v-model="record.role" size="small" style="width:100px" @change="handleRoleChange(record)">
            <a-option value="user">普通用户</a-option>
            <a-option value="admin">管理员</a-option>
          </a-select>
        </template>
        <template #createdAt="{ record }">{{ record.createdAt ? record.createdAt.replace('T',' ').substring(0,16) : '-' }}</template>
        <template #actions="{ record }">
          <a-button type="text" size="small" @click="handleResetPassword(record)">重置密码</a-button>
        </template>
      </a-table>
    </div>

    <a-modal v-model:visible="pwdDialogVisible" title="重置密码" :width="'400px'" @ok="confirmResetPassword" @cancel="pwdDialogVisible = false">
      <a-form :model="pwdForm" layout="vertical">
        <a-form-item label="用户">
          <a-input v-model="pwdForm.username" disabled />
        </a-form-item>
        <a-form-item label="新密码">
          <a-input-password v-model="pwdForm.password" placeholder="请输入新密码（至少6位）" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { userApi } from '@/api'

const loading = ref(false)
const tableData = ref([])
const pwdDialogVisible = ref(false)
const pwdForm = ref({ id: null, username: '', password: '' })

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '用户名', dataIndex: 'username', minWidth: 150 },
  { title: '角色', dataIndex: 'role', width: 120, slotName: 'role' },
  { title: '注册时间', dataIndex: 'createdAt', width: 160, slotName: 'createdAt' },
  { title: '操作', width: 150, slotName: 'actions' }
]

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await userApi.list()
    if (res.code === 200) tableData.value = res.data
  } catch (e) { Message.error('加载用户列表失败') }
  finally { loading.value = false }
}

const handleRoleChange = async (row) => {
  try {
    await userApi.updateRole(row.id, row.role)
    Message.success('角色更新成功')
  } catch (e) { Message.error('更新失败') }
}

const handleResetPassword = (row) => {
  pwdForm.value = { id: row.id, username: row.username, password: '' }
  pwdDialogVisible.value = true
}

const confirmResetPassword = async () => {
  if (!pwdForm.value.password || pwdForm.value.password.length < 6) {
    Message.warning('密码至少6位')
    return
  }
  try {
    await userApi.updatePassword(pwdForm.value.id, pwdForm.value.password)
    Message.success('密码重置成功')
    pwdDialogVisible.value = false
  } catch (e) { Message.error('操作失败') }
}

onMounted(fetchUsers)
</script>

<style scoped>
.user-container { min-height: 100vh; background: #f0f2f5; }
.header { background: #fff; padding: 0 24px; height: 60px; display: flex; align-items: center; justify-content: space-between; box-shadow: 0 1px 4px rgba(0,0,0,.1); }
.header h2 { margin: 0; font-size: 18px; font-weight: 600; color: #1d1d1d; }
.main { padding: 24px; max-width: 1000px; margin: 0 auto; }
</style>