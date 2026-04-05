<template>
  <div class="login-container">
    <a-card class="login-card">
      <template #header><h2>注册账号</h2></template>
      <a-form :model="form" :rules="rules" ref="formRef" layout="vertical">
        <a-form-item field="username" :validate-trigger="['\''blur'\'', '\''change'\'']">
          <a-input v-model="form.username" placeholder="用户名" allow-clear>
            <template #prefix><icon-message /></template>
          </a-input>
        </a-form-item>
        <a-form-item field="password" :validate-trigger="['\''blur'\'', '\''change'\'']">
          <a-input v-model="form.password" placeholder="密码（至少6位）" password allow-clear>
            <template #prefix><icon-lock /></template>
          </a-input>
        </a-form-item>
        <a-form-item field="confirmPassword" :validate-trigger="['\''blur'\'', '\''change'\'']">
          <a-input v-model="form.confirmPassword" placeholder="确认密码" password allow-clear @press-enter="handleRegister">
            <template #prefix><icon-lock /></template>
          </a-input>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" long :loading="loading" @click="handleRegister">注册</a-button>
        </a-form-item>
        <div class="footer"><router-link to="/login">已有账号？去登录</router-link></div>
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
const form = reactive({ username: '\'''\'', password: '\'''\'', confirmPassword: '\'''\'' })

const validateConfirm = (value, callback) => {
  if (value !== form.password) callback(new Error('\''两次密码不一致'\''))
  else callback()
}

const rules = {
  username: [{ required: true, message: '\''请输入用户名'\'', trigger: '\''blur'\'' }],
  password: [{ required: true, minLength: 6, message: '\''密码至少6位'\'', trigger: '\''blur'\'' }],
  confirmPassword: [{ required: true, validator: validateConfirm, trigger: '\''blur'\'' }]
}

const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await userApi.register(form)
    if (res.code === 200) { Message.success('\''注册成功，请登录'\''); router.push("/login") }
    else { Message.error(res.msg) }
  } catch (e) { Message.error(e.response?.data?.msg || '\''注册失败'\'') }
  finally { loading.value = false }
}
</script>

<style scoped>
.login-container { height: 100vh; display: flex; align-items: center; justify-content: center; background: #f0f2f5; }
.login-card { width: 400px; }
.footer { text-align: right; }
</style>