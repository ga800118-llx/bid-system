<template>
  <div class="login-container">
    <a-card class="login-card">
      <template #header><h2>招投标管理系统</h2></template>
      <a-form :model="formData" layout="vertical">
        <a-form-item>
          <a-input v-model="formData.username" placeholder="用户名" allow-clear>
            <template #prefix><IconMessage /></template>
          </a-input>
        </a-form-item>
        <a-form-item>
          <a-input-password v-model="formData.password" placeholder="密码" @press-enter="handleLogin" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" long :loading="loading" @click="handleLogin">登录</a-button>
        </a-form-item>
        <div class="footer"><router-link to="/register">没有账号？去注册</router-link></div>
      </a-form>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { IconMessage } from '@arco-design/web-vue/es/icon'
import { userApi } from '@/api'

const router = useRouter()
const loading = ref(false)
const formData = reactive({ username: '', password: '' })

const handleLogin = async () => {
  console.log('[Login] clicked', JSON.stringify(formData))
  if (!formData.username.trim()) {
    Message.warning('请输入用户名')
    return
  }
  if (!formData.password) {
    Message.warning('请输入密码')
    return
  }
  loading.value = true
  try {
    const res = await userApi.login(formData)
    console.log('[Login] response:', JSON.stringify(res))
    if (res.code === 200) {
      localStorage.setItem('token', res.data.token)
      const payload = JSON.parse(atob(res.data.token.split('.')[1]))
      localStorage.setItem('role', payload.role)
      localStorage.setItem('username', res.data.username || formData.username)
      Message.success('登录成功')
      const dest = payload.role === 'admin' ? '/dashboard' : '/projects'
      router.push(dest)
    } else {
      Message.error(res.msg || '登录失败')
    }
  } catch (e) {
    console.error('[Login] error:', e)
    Message.error(e.response?.data?.msg || e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container { height: 100vh; display: flex; align-items: center; justify-content: center; background: #f0f2f5; }
.login-card { width: 400px; }
.footer { text-align: right; }
</style>
