<template>
  <div class="login-page" :style="pageBackgroundStyle">
    <div class="background-overlay"></div>
    <div class="page-shell">
      <section class="brand-side">
        <div class="brand-top">
          <div class="brand-kicker">{{ enterpriseName }}</div>
          <h1>{{ systemTitle }}</h1>
          <p>统一账号入口，进入当前工作空间。</p>
        </div>

        <div class="brand-bottom">
          <div class="brand-line"></div>
          <div class="brand-note">组织、权限、配置与审计能力统一运行</div>
          <div class="brand-note">请使用已分配账号登录系统</div>
        </div>
      </section>

      <section class="login-panel">
        <div class="panel-header">
          <div class="panel-kicker">统一账号登录</div>
          <h2>欢迎登录</h2>
        </div>

        <a-form :model="formData" layout="vertical" class="login-form">
          <a-form-item label="登录账号">
            <a-input v-model="formData.username" placeholder="请输入用户名或邮箱" allow-clear size="large">
              <template #prefix><IconMessage /></template>
            </a-input>
          </a-form-item>
          <a-form-item label="登录密码">
            <a-input-password v-model="formData.password" placeholder="请输入密码" size="large" @press-enter="handleLogin">
              <template #prefix><IconLock /></template>
            </a-input-password>
          </a-form-item>
          <a-form-item>
            <a-button type="primary" long size="large" :loading="loading" @click="handleLogin">登录系统</a-button>
          </a-form-item>
        </a-form>

        <div class="panel-meta">
          <div class="meta-row">
            <span>当前系统</span>
            <strong>{{ systemTitle }}</strong>
          </div>
          <div class="meta-row">
            <span>企业主体</span>
            <strong>{{ enterpriseName }}</strong>
          </div>
        </div>

        <div class="panel-footer">
          <router-link to="/register">没有账号？去注册</router-link>
          <span>登录行为将记录到操作日志</span>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { IconLock, IconMessage } from '@arco-design/web-vue/es/icon'
import loginBackground from '@/assets/login/corporate-night.jpg'
import { userApi } from '@/api'
import { isAdminUser, loadUserContext } from '@/utils/permission'
import { DEFAULT_ENTERPRISE_NAME, DEFAULT_SYSTEM_TITLE, loadPublicSystemConfig } from '@/utils/systemConfig'
import { writeStoredToken } from '@/utils/userContextStorage'

const router = useRouter()
const loading = ref(false)
const systemTitle = ref(DEFAULT_SYSTEM_TITLE)
const enterpriseName = ref(DEFAULT_ENTERPRISE_NAME)
const formData = reactive({ username: '', password: '' })

const pageBackgroundStyle = computed(() => ({
  backgroundImage: `linear-gradient(108deg, rgba(6, 16, 31, 0.9) 0%, rgba(6, 16, 31, 0.78) 44%, rgba(6, 16, 31, 0.52) 100%), url(${loginBackground})`
}))

const handleLogin = async () => {
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
    if (res.code === 200) {
      writeStoredToken(res.data.token)
      const { permissions } = await loadUserContext()
      Message.success('登录成功')
      const dest = isAdminUser() || permissions.some(code => code.startsWith('system_')) ? '/dashboard' : '/projects'
      router.push(dest)
    } else {
      Message.error(res.msg || '登录失败')
    }
  } catch (e) {
    Message.error(e.response?.data?.msg || e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

const loadBranding = async () => {
  const config = await loadPublicSystemConfig()
  systemTitle.value = config.systemTitle || DEFAULT_SYSTEM_TITLE
  enterpriseName.value = config.enterpriseName || DEFAULT_ENTERPRISE_NAME
  document.title = `登录 - ${systemTitle.value}`
}

onMounted(() => {
  loadBranding()
})
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  padding: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #08101d;
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  overflow: hidden;
}

.background-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(6, 16, 31, 0.36), rgba(6, 16, 31, 0.08));
}

.page-shell {
  position: relative;
  z-index: 1;
  width: min(1180px, 100%);
  min-height: 680px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 408px;
  gap: 44px;
  align-items: center;
}

.brand-side {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 520px;
  color: #fff;
}

.brand-kicker {
  color: rgba(255, 255, 255, 0.68);
  font-size: 13px;
  line-height: 20px;
}

.brand-side h1 {
  max-width: 540px;
  margin: 12px 0 0;
  font-size: 46px;
  line-height: 1.18;
  font-weight: 600;
  color: #fff;
}

.brand-side p {
  max-width: 420px;
  margin: 18px 0 0;
  color: rgba(255, 255, 255, 0.8);
  font-size: 16px;
  line-height: 1.9;
}

.brand-bottom {
  display: flex;
  flex-direction: column;
  gap: 10px;
  color: rgba(255, 255, 255, 0.68);
  font-size: 13px;
  line-height: 20px;
}

.brand-line {
  width: 72px;
  height: 2px;
  background: rgba(255, 255, 255, 0.42);
}

.login-panel {
  padding: 30px 28px 24px;
  border: 1px solid var(--ds-color-border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--ds-color-bg-card) 96%, transparent);
  box-shadow: 0 24px 60px rgba(3, 10, 24, 0.28);
}

.panel-header {
  margin-bottom: 24px;
}

.panel-kicker {
  color: var(--ds-color-primary);
  font-size: 12px;
  line-height: 18px;
  font-weight: 600;
}

.panel-header h2 {
  margin: 8px 0 0;
  color: var(--ds-color-text-primary);
  font-size: 30px;
  line-height: 1.2;
  font-weight: 600;
}

.login-form :deep(.arco-form-item-label-col) {
  padding-bottom: 6px;
}

.panel-meta {
  margin-top: 8px;
  padding-top: 8px;
}

.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px solid var(--ds-color-border);
  color: var(--ds-color-text-regular);
  font-size: 13px;
  line-height: 20px;
}

.meta-row:last-child {
  border-bottom: 0;
}

.meta-row strong {
  color: var(--ds-color-text-primary);
  font-weight: 500;
  text-align: right;
}

.panel-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 18px;
}

.panel-footer a {
  color: var(--ds-color-primary);
}

@media (max-width: 1024px) {
  .page-shell {
    grid-template-columns: 1fr;
    min-height: unset;
    gap: 24px;
  }

  .brand-side {
    min-height: unset;
    gap: 28px;
  }

  .login-panel {
    width: min(440px, 100%);
  }
}

@media (max-width: 640px) {
  .login-page {
    padding: 20px 16px;
  }

  .brand-side h1 {
    font-size: 34px;
  }

  .brand-side p {
    font-size: 14px;
  }

  .login-panel {
    width: 100%;
    padding: 24px 18px 20px;
  }

  .panel-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
