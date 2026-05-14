<template>
  <div class="auth-page" :style="pageBackgroundStyle">
    <div class="auth-mask"></div>
    <div class="auth-shell">
      <section class="brand-panel">
        <div class="brand-kicker">{{ enterpriseName }}</div>
        <h1>{{ systemTitle }}</h1>
        <p class="brand-summary">
          新账号注册完成后，可以继续进入统一平台，按分配的角色接入组织、权限、配置和后续业务模块。
        </p>
        <div class="brand-highlights">
          <div v-for="item in highlights" :key="item.title" class="highlight-item">
            <div class="highlight-title">{{ item.title }}</div>
            <div class="highlight-desc">{{ item.desc }}</div>
          </div>
        </div>
      </section>

      <section class="auth-panel">
        <div class="panel-header">
          <div class="panel-kicker">账号注册</div>
          <h2>创建账号</h2>
          <p>注册成功后，请使用新账号登录系统。</p>
        </div>

        <a-form ref="formRef" :model="form" layout="vertical" class="auth-form">
          <a-form-item field="username" label="登录账号" :rules="[{ required: true, message: '请输入用户名' }]" :validate-trigger="['blur', 'change']">
            <a-input v-model="form.username" placeholder="请输入用户名" allow-clear size="large">
              <template #prefix><IconMessage /></template>
            </a-input>
          </a-form-item>
          <a-form-item field="password" label="登录密码" :rules="passwordRules" :validate-trigger="['blur', 'change']">
            <a-input-password v-model="form.password" :placeholder="passwordPlaceholder" size="large">
              <template #prefix><IconLock /></template>
            </a-input-password>
            <div class="form-tip">{{ passwordTip }}</div>
          </a-form-item>
          <a-form-item field="confirmPassword" label="确认密码" :rules="confirmPasswordRules" :validate-trigger="['blur', 'change']">
            <a-input-password v-model="form.confirmPassword" placeholder="请再次输入密码" size="large" @press-enter="handleRegister">
              <template #prefix><IconLock /></template>
            </a-input-password>
          </a-form-item>
          <a-form-item>
            <a-button type="primary" long size="large" :loading="loading" @click="handleRegister">提交注册</a-button>
          </a-form-item>
        </a-form>

        <div class="panel-footer">
          <router-link to="/login">已有账号？去登录</router-link>
          <span>密码策略以系统配置为准</span>
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
import {
  DEFAULT_ENTERPRISE_NAME,
  DEFAULT_PASSWORD_MIN_LENGTH,
  DEFAULT_SYSTEM_TITLE,
  applyPasswordPolicy,
  loadPublicSystemConfig,
  passwordPolicyText,
  validatePasswordByPolicy
} from '@/utils/systemConfig'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const systemTitle = ref(DEFAULT_SYSTEM_TITLE)
const enterpriseName = ref(DEFAULT_ENTERPRISE_NAME)
const passwordPolicy = ref({ minLength: DEFAULT_PASSWORD_MIN_LENGTH, requireStrong: false })
const form = reactive({ username: '', password: '', confirmPassword: '' })
const highlights = [
  { title: '密码策略', desc: '最小长度与强密码规则统一从系统配置读取。' },
  { title: '统一身份', desc: '注册后继续纳入组织、角色和权限体系。' },
  { title: '审计留痕', desc: '账号相关操作会在系统中留下审计记录。' }
]

const pageBackgroundStyle = computed(() => ({
  backgroundImage: `linear-gradient(109deg, rgba(7, 17, 34, 0.88) 0%, rgba(7, 17, 34, 0.72) 40%, rgba(7, 17, 34, 0.46) 100%), url(${loginBackground})`
}))
const passwordTip = computed(() => `密码${passwordPolicyText(passwordPolicy.value)}`)
const passwordPlaceholder = computed(() => `请输入密码，${passwordPolicyText(passwordPolicy.value)}`)

const validateConfirm = (value, callback) => {
  if (value !== form.password) callback(new Error('两次密码不一致'))
  else callback()
}

const validatePassword = (value, callback) => {
  const message = validatePasswordByPolicy(value, passwordPolicy.value)
  if (message) callback(new Error(message))
  else callback()
}

const passwordRules = computed(() => [{ required: true, validator: validatePassword, trigger: 'blur' }])
const confirmPasswordRules = computed(() => [{ required: true, validator: validateConfirm, trigger: 'blur' }])

const handleRegister = async () => {
  const errors = await formRef.value.validate().catch(() => true)
  if (errors) return
  loading.value = true
  try {
    const res = await userApi.register(form)
    if (res.code === 200) {
      Message.success('注册成功，请登录')
      router.push('/login')
    } else {
      Message.error(res.msg)
    }
  } catch (e) {
    Message.error(e.response?.data?.msg || '注册失败')
  } finally {
    loading.value = false
  }
}

const loadBranding = async () => {
  const config = await loadPublicSystemConfig()
  systemTitle.value = config.systemTitle || DEFAULT_SYSTEM_TITLE
  enterpriseName.value = config.enterpriseName || DEFAULT_ENTERPRISE_NAME
  document.title = `注册 - ${systemTitle.value}`
}

onMounted(() => {
  applyPasswordPolicy(passwordPolicy)
  loadBranding()
})
</script>

<style scoped>
.auth-page {
  position: relative;
  min-height: 100vh;
  padding: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #09111f;
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  overflow: hidden;
}

.auth-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(6, 16, 31, 0.36), rgba(6, 16, 31, 0.08));
}

.auth-shell {
  position: relative;
  z-index: 1;
  width: min(1180px, 100%);
  min-height: 680px;
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) 440px;
  gap: 32px;
  align-items: stretch;
}

.brand-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 48px 0;
  color: #f7f8fa;
}

.brand-kicker {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  line-height: 20px;
}

.brand-panel h1 {
  max-width: 560px;
  margin: 10px 0 0;
  color: #ffffff;
  font-size: 42px;
  line-height: 1.2;
  font-weight: 600;
}

.brand-summary {
  max-width: 620px;
  margin: 18px 0 0;
  color: rgba(255, 255, 255, 0.82);
  font-size: 16px;
  line-height: 1.9;
}

.brand-highlights {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 36px;
}

.highlight-item {
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  background: rgba(7, 17, 34, 0.22);
  backdrop-filter: blur(6px);
}

.highlight-title {
  color: #ffffff;
  font-size: 14px;
  line-height: 22px;
  font-weight: 600;
}

.highlight-desc {
  margin-top: 6px;
  color: color-mix(in srgb, var(--ds-color-bg-card) 72%, transparent);
  font-size: 13px;
  line-height: 1.7;
}

.auth-panel {
  align-self: center;
  padding: 28px 28px 24px;
  border: 1px solid var(--ds-color-border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--ds-color-bg-card) 94%, transparent);
  box-shadow: 0 24px 60px rgba(3, 10, 24, 0.28);
}

.panel-header {
  margin-bottom: 22px;
}

.panel-kicker {
  color: var(--ds-color-primary);
  font-size: 12px;
  line-height: 18px;
  font-weight: 600;
}

.panel-header h2 {
  margin: 8px 0 6px;
  color: var(--ds-color-text-primary);
  font-size: 28px;
  line-height: 1.25;
  font-weight: 600;
}

.panel-header p {
  margin: 0;
  color: var(--ds-color-text-regular);
  font-size: 14px;
  line-height: 1.8;
}

.auth-form :deep(.arco-form-item-label-col) {
  padding-bottom: 6px;
}

.form-tip {
  margin-top: 6px;
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 1.6;
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
  .auth-shell {
    grid-template-columns: 1fr;
    min-height: unset;
  }

  .brand-panel {
    padding: 8px 0 0;
  }

  .brand-highlights {
    grid-template-columns: 1fr;
  }

  .auth-panel {
    width: min(460px, 100%);
  }
}

@media (max-width: 640px) {
  .auth-page {
    padding: 20px 16px;
  }

  .brand-panel h1 {
    font-size: 32px;
  }

  .brand-summary {
    font-size: 14px;
  }

  .auth-panel {
    width: 100%;
    padding: 24px 18px 20px;
  }

  .panel-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
