<template>
  <div class="login-container">
    <a-card class="login-card">
      <template #header><h2>招投标管理系统</h2></template>
      <a-form :model="formData" :rules="formRules" ref="formRef" layout="vertical">
        <a-form-item field="username" :rules="[{ required: true, message: '\''请输入用户名'\'' }]" :validate-trigger="['\''blur'\'', '\''change'\'']">
          <a-input v-model="formData.username" placeholder="用户名" allow-clear>
            <template #prefix><icon-message /></template>
          </a-input>
        </a-form-item>
        <a-form-item field="password" :rules="[{ required: true, message: '\''请输入密码'\'' }]" :validate-trigger="['\''blur'\'', '\''change'\'']">
          <a-input v-model="formData.password" placeholder="密码" password allow-clear @press-enter="handleLogin">
            <template #prefix><icon-lock /></template>
          </a-input>
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
import { ref, reactive } from '\''vue'\''
import { useRouter } from '\''vue-router'\''
import { Message } from '\''@arco-design/web-vue'\''
import { userApi } from '\''@/api'\''

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const formData = reactive({ username: '\'''\'', password: '\'''\'' })

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await userApi.login(formData)
    if (res.code === 200) {
      localStorage.setItem('\''token'\'', res.data.token)
      const payload = JSON.parse(atob(res.data.token.split('\''.'\'')[1]))
      localStorage.setItem('\''role'\'', payload.role)
      localStorage.setItem('\''username'\'', formData.username)
      Message.success('\''登录成功'\'')
      const dest = payload.role === '\''admin'\'' ? '\''/dashboard'\'' : '\''/projects'\''
      router.push(dest)
    } else { Message.error(res.msg) }
  } catch (e) { Message.error(e.response?.data?.msg || '\''登录失败'\'') }
  finally { loading.value = false }
}
</script>

<style scoped>
.login-container { height: 100vh; display: flex; align-items: center; justify-content: center; background: #f0f2f5; }
.login-card { width: 400px; }
.footer { text-align: right; }
</style>