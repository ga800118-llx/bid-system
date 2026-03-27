<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header><h2>招投标管理系统</h2></template>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="邮箱" prefix-icon="Message" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width:100%" :loading="loading" @click="handleLogin">登录</el-button>
        </el-form-item>
        <div class="footer"><router-link to="/register">没有账号？去注册</router-link></div>
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
const form = reactive({ username: "", password: "" })
const rules = {
  username: [{ required: true, message: "请输入邮箱", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }]
}
const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await userApi.login(form)
    if (res.code === 200) {
      localStorage.setItem("token", res.data.token)
      const payload = JSON.parse(atob(res.data.token.split(".")[1]))
      localStorage.setItem("role", payload.role)
      localStorage.setItem("username", form.username)
      ElMessage.success("登录成功")
      router.push("/")
    } else { ElMessage.error(res.msg) }
  } catch (e) { ElMessage.error(e.response?.data?.msg || "登录失败") }
  finally { loading.value = false }
}
</script>
<style scoped>
.login-container { height: 100vh; display: flex; align-items: center; justify-content: center; background: #f0f2f5; }
.login-card { width: 400px; }
.footer { text-align: right; }
</style>