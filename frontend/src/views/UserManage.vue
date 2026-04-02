<template>
  <div class="user-container">
    <div class="header">
      <div class="header-left">
        <h2>用户管理</h2>
      </div>
      <div class="header-right">
        <el-button link @click="$router.push('/')">返回首页</el-button>
      </div>
    </div>
    <div class="main">
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="150" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-select v-model="row.role" size="small" style="width:100px" @change="handleRoleChange(row)">
              <el-option label="普通用户" value="user" />
              <el-option label="管理员" value="admin" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="160">
          <template #default="{ row }">{{ row.createdAt ? row.createdAt.replace("T"," ").substring(0,16) : "-" }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleResetPassword(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="pwdDialogVisible" title="重置密码" width="400px">
      <el-form :model="pwdForm" label-width="80px">
        <el-form-item label="用户">
          <el-input v-model="pwdForm.username" disabled />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.password" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmResetPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue"
import { ElMessage } from "element-plus"
import { userApi } from "@/api"

const loading = ref(false)
const tableData = ref([])
const pwdDialogVisible = ref(false)
const pwdForm = ref({ id: null, username: '', password: '' })

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await userApi.list()
    if (res.code === 200) tableData.value = res.data
  } catch (e) { ElMessage.error("加载用户列表失败") }
  finally { loading.value = false }
}

const handleRoleChange = async (row) => {
  try {
    await userApi.updateRole(row.id, row.role)
    ElMessage.success("角色更新成功")
  } catch (e) { ElMessage.error("更新失败") }
}

const handleResetPassword = (row) => {
  pwdForm.value = { id: row.id, username: row.username, password: '' }
  pwdDialogVisible.value = true
}

const confirmResetPassword = async () => {
  if (!pwdForm.value.password || pwdForm.value.password.length < 6) {
    ElMessage.warning("密码至少6位")
    return
  }
  try {
    await userApi.updatePassword(pwdForm.value.id, pwdForm.value.password)
    ElMessage.success("密码重置成功")
    pwdDialogVisible.value = false
  } catch (e) { ElMessage.error("重置失败") }
}

onMounted(fetchUsers)
</script>

<style scoped>
.user-container { min-height: 100vh; background: #f0f2f5; }
.header { background: #fff; padding: 0 24px; height: 60px; display: flex; align-items: center; justify-content: space-between; box-shadow: 0 1px 4px rgba(0,0,0,.1); }
.main { padding: 24px; max-width: 1000px; margin: 0 auto; }
</style>
