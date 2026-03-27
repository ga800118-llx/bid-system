<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header><h2>注册账号</h2></template>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="邮箱（作为用户名）" prefix-icon="Message" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（至少6位）" prefix-icon="Lock" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock" @keyup.enter="handleRegister" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width:100%" :loading="loading" @click="handleRegister">注册</el-button>
        </el-form-item>
        <div class="footer"><router-link to="/login">已有账号？去登录</router-link></div>
      </el-form>
    </el-card>
  </div>
</template>
<script setup>
import { ref, reactive } from "vue"
import { useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import { userApi } from "@/api"
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: "", password: "", confirmPassword: "" })
const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) callback(new Error("两次密码不一致"))
  else callback()
}
const rules = {
  username: [{ required: true, message: "请输入邮箱", trigger: "blur" }],
  password: [{ required: true, min: 6, message: "密码至少6位", trigger: "blur" }],
  confirmPassword: [{ required: true, validator: validateConfirm, trigger: "blur" }]
}
const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await userApi.register(form)
    if (res.code === 200) { ElMessage.success("注册成功，请登录"); router.push("/login") }
    else { ElMessage.error(res.msg) }
  } catch (e) { ElMessage.error(e.response?.data?.msg || "注册失败") }
  finally { loading.value = false }
}
</script>
<style scoped>
.login-container { height: 100vh; display: flex; align-items: center; justify-content: center; background: #f0f2f5; }
.login-card { width: 400px; }
.footer { text-align: right; }
</style>