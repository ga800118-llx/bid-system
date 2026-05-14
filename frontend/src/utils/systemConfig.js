import { systemConfigApi } from '@/api'

export const DEFAULT_SYSTEM_TITLE = '投标管理系统'
export const DEFAULT_ENTERPRISE_NAME = '我的公司'
export const DEFAULT_PAGE_SIZE = 20
export const DEFAULT_PAGE_SIZE_OPTIONS = [10, 20, 50, 100]
export const DEFAULT_PASSWORD_MIN_LENGTH = 6

let publicConfigPromise = null

if (typeof window !== 'undefined') {
  window.addEventListener('system-config-updated', () => {
    publicConfigPromise = null
  })
}

export const loadPublicSystemConfig = async () => {
  if (!publicConfigPromise) {
    publicConfigPromise = systemConfigApi.publicBasic()
      .then(res => (res.code === 200 ? (res.data || {}) : {}))
      .catch(() => ({}))
  }
  return publicConfigPromise
}

export const normalizePageSizeOptions = (rawOptions, fallback = DEFAULT_PAGE_SIZE_OPTIONS) => {
  const options = Array.isArray(rawOptions)
    ? rawOptions.map(item => Number(item)).filter(item => Number.isInteger(item) && item > 0)
    : []
  return (options.length ? [...new Set(options)] : [...fallback]).sort((a, b) => a - b)
}

export const applyPaginationConfig = async (sizeRef, optionsRef) => {
  const config = await loadPublicSystemConfig()
  const nextOptions = normalizePageSizeOptions(config.pageSizeOptions)
  const nextSize = Number(config.defaultPageSize)
  optionsRef.value = nextOptions
  if (Number.isInteger(nextSize) && nextSize > 0) {
    sizeRef.value = nextSize
    if (!optionsRef.value.includes(nextSize)) {
      optionsRef.value = [...optionsRef.value, nextSize].sort((a, b) => a - b)
    }
  }
}

export const normalizePasswordPolicy = (config = {}) => {
  const minLength = Number(config.passwordMinLength)
  return {
    minLength: Number.isInteger(minLength) && minLength > 0 ? minLength : DEFAULT_PASSWORD_MIN_LENGTH,
    requireStrong: config.passwordRequireStrong === true
  }
}

export const passwordPolicyText = (policy) => {
  const base = `至少${policy.minLength}位`
  return policy.requireStrong ? `${base}，且包含大小写字母、数字或特殊字符中的至少三类` : base
}

export const validatePasswordByPolicy = (password, policy, allowEmpty = false) => {
  const value = String(password ?? '')
  if (!value) return allowEmpty ? '' : `密码${passwordPolicyText(policy)}`
  if (value.length < policy.minLength) return `密码至少${policy.minLength}位`
  if (!policy.requireStrong) return ''
  let categories = 0
  if (/[a-z]/.test(value)) categories += 1
  if (/[A-Z]/.test(value)) categories += 1
  if (/\d/.test(value)) categories += 1
  if (/[^a-zA-Z0-9]/.test(value)) categories += 1
  return categories >= 3 ? '' : '密码需包含大小写字母、数字或特殊字符中的至少三类'
}

export const applyPasswordPolicy = async (policyRef) => {
  const config = await loadPublicSystemConfig()
  policyRef.value = normalizePasswordPolicy(config)
}
