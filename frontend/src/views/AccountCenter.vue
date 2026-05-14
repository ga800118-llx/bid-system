<template>
  <div class="account-page">
    <div class="main">
      <div class="account-layout">
        <section class="account-section">
          <div class="section-header">
            <div>
              <div class="section-title">个人资料</div>
              <div class="section-desc">查看当前账号、所属组织和角色信息。</div>
            </div>
            <a-button size="small" @click="fetchProfile">刷新</a-button>
          </div>
          <a-spin :loading="loading" class="section-body">
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">姓名</span>
                <span class="info-value">{{ profile.realName || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">账号</span>
                <span class="info-value">{{ profile.username || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">所属部门</span>
                <span class="info-value">{{ profile.deptName || '未分配' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">账号状态</span>
                <span class="info-value">
                  <a-tag :color="Number(profile.status) === 1 ? 'green' : 'red'">
                    {{ Number(profile.status) === 1 ? '启用' : '禁用' }}
                  </a-tag>
                </span>
              </div>
              <div class="info-item">
                <span class="info-label">角色</span>
                <span class="info-value role-tags">
                  <a-tag v-for="name in profile.roleNames || []" :key="name" color="arcoblue">{{ name }}</a-tag>
                  <span v-if="!profile.roleNames?.length">-</span>
                </span>
              </div>
              <div class="info-item">
                <span class="info-label">创建时间</span>
                <span class="info-value">{{ formatTime(profile.createdAt) }}</span>
              </div>
            </div>
          </a-spin>
        </section>

        <section class="account-section">
          <div class="section-header">
            <div>
              <div class="section-title">账号安全</div>
              <div class="section-desc">集中查看登录状态和密码策略。</div>
            </div>
          </div>
          <a-spin :loading="loading" class="section-body">
            <div class="security-grid">
              <div class="security-card">
                <div class="card-label">最近登录</div>
                <div class="card-value">{{ formatTime(profile.lastLoginAt) }}</div>
                <div class="card-tip">成功登录后自动更新。</div>
              </div>
              <div class="security-card">
                <div class="card-label">连续失败次数</div>
                <div class="card-value">{{ profile.failedLoginCount ?? 0 }}</div>
                <div class="card-tip">最近失败时间：{{ formatTime(profile.lastFailedLoginAt, '无失败记录') }}</div>
              </div>
              <div class="security-card">
                <div class="card-label">失败锁定阈值</div>
                <div class="card-value">{{ profile.loginMaxFailedAttempts ?? '-' }}</div>
                <div class="card-tip">达到上限后将按系统策略锁定。</div>
              </div>
              <div class="security-card">
                <div class="card-label">会话时长</div>
                <div class="card-value">{{ profile.sessionTimeoutMinutes ?? '-' }} 分钟</div>
                <div class="card-tip">登录后的默认会话超时时间。</div>
              </div>
            </div>
            <div class="policy-box">
              <div class="policy-title">当前密码策略</div>
              <div class="policy-text">密码{{ passwordPolicyText(passwordPolicy) }}</div>
            </div>
          </a-spin>
        </section>

        <section class="account-section">
          <div class="section-header">
            <div>
              <div class="section-title">修改密码</div>
              <div class="section-desc">请输入原密码，并设置一个符合策略的新密码。</div>
            </div>
          </div>
          <div class="section-body">
            <a-form :model="passwordForm" layout="vertical" class="password-form">
              <a-form-item label="原密码" required>
                <a-input-password v-model="passwordForm.oldPassword" placeholder="请输入当前登录密码" />
              </a-form-item>
              <a-form-item label="新密码" required>
                <a-input-password v-model="passwordForm.newPassword" :placeholder="`请输入新密码，${passwordPolicyText(passwordPolicy)}`" />
              </a-form-item>
              <a-form-item label="确认新密码" required>
                <a-input-password v-model="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
              </a-form-item>
              <div class="form-tip">修改成功后，下次登录请使用新密码。</div>
              <div class="form-actions">
                <a-button @click="resetPasswordForm">清空</a-button>
                <a-button type="primary" :loading="submitting" @click="submitPassword">保存新密码</a-button>
              </div>
            </a-form>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import { userApi } from '@/api'
import { refreshUserContext } from '@/utils/permission'
import { passwordPolicyText, validatePasswordByPolicy } from '@/utils/systemConfig'

const loading = ref(false)
const submitting = ref(false)
const profile = ref({})
const passwordPolicy = reactive({ minLength: 6, requireStrong: false })
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const fetchProfile = async () => {
  loading.value = true
  try {
    const res = await userApi.profile()
    if (res.code === 200) {
      profile.value = res.data || {}
      passwordPolicy.minLength = Number(res.data?.passwordMinLength) > 0 ? Number(res.data.passwordMinLength) : 6
      passwordPolicy.requireStrong = res.data?.passwordRequireStrong === true
    } else {
      Message.error(res.msg || '加载个人中心失败')
    }
  } catch {
    Message.error('加载个人中心失败')
  } finally {
    loading.value = false
  }
}

const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

const submitPassword = async () => {
  if (!passwordForm.oldPassword) {
    Message.warning('请输入原密码')
    return
  }
  const passwordError = validatePasswordByPolicy(passwordForm.newPassword, passwordPolicy)
  if (passwordError) {
    Message.error(passwordError)
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    Message.error('两次输入的新密码不一致')
    return
  }
  submitting.value = true
  try {
    const res = await userApi.changeOwnPassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    if (res.code === 200) {
      Message.success('密码已修改')
      resetPasswordForm()
      await fetchProfile()
      await refreshUserContext()
    } else {
      Message.error(res.msg || '修改密码失败')
    }
  } catch {
    Message.error('修改密码失败')
  } finally {
    submitting.value = false
  }
}

const formatTime = (value, fallback = '-') => {
  if (!value) return fallback
  return String(value).replace('T', ' ').slice(0, 16)
}

onMounted(fetchProfile)
</script>

<style scoped>
.account-page { min-height: 100vh; background: var(--ds-color-bg-page); }
.main { padding: 24px; }
.account-layout { display: flex; flex-direction: column; gap: 16px; }
.account-section { background: var(--ds-color-bg-card); border-radius: 8px; overflow: hidden; }
.section-header { padding: 16px 20px; border-bottom: 1px solid var(--ds-color-border); display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.section-title { color: var(--ds-color-text-primary); font-size: 15px; font-weight: 600; }
.section-desc { margin-top: 4px; color: var(--ds-color-text-secondary); font-size: 12px; }
.section-body { padding: 20px; }
.info-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.info-item { display: flex; flex-direction: column; gap: 8px; min-width: 0; }
.info-label { color: var(--ds-color-text-secondary); font-size: 12px; }
.info-value { color: var(--ds-color-text-primary); font-size: 14px; line-height: 22px; min-height: 22px; }
.role-tags { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }
.security-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
.security-card { border: 1px solid var(--ds-color-border); border-radius: 8px; padding: 16px; background: var(--ds-color-bg-soft); min-width: 0; }
.card-label { color: var(--ds-color-text-secondary); font-size: 12px; }
.card-value { margin-top: 10px; color: var(--ds-color-text-primary); font-size: 20px; font-weight: 600; }
.card-tip { margin-top: 10px; color: var(--ds-color-text-secondary); font-size: 12px; line-height: 20px; }
.policy-box { margin-top: 16px; padding: 14px 16px; background: var(--ds-color-bg-soft); border-radius: 8px; }
.policy-title { color: var(--ds-color-text-regular); font-size: 13px; font-weight: 600; }
.policy-text { margin-top: 6px; color: var(--ds-color-text-primary); font-size: 13px; }
.password-form { max-width: 420px; }
.form-tip { margin-top: 4px; color: var(--ds-color-text-secondary); font-size: 12px; }
.form-actions { margin-top: 20px; display: flex; gap: 8px; }

@media (max-width: 1200px) {
  .security-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .info-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 768px) {
  .main { padding: 16px; }
  .section-header { align-items: flex-start; flex-direction: column; }
  .info-grid, .security-grid { grid-template-columns: 1fr; }
  .password-form { max-width: none; }
}
</style>
