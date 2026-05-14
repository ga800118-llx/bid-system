import { userApi } from '@/api'
import { ref } from 'vue'
import {
  clearStoredUserContext,
  readStoredPermissions,
  readStoredUserInfo,
  writeStoredUserContext
} from '@/utils/userContextStorage'

export const permissionVersion = ref(0)

const touchPermissions = () => {
  permissionVersion.value += 1
}

export const getUserInfo = () => {
  return readStoredUserInfo()
}

export const setUserContext = (data = {}) => {
  const user = data.user || data
  const permissions = data.permissions || []
  const userInfo = {
    userId: data.userId ?? user.id,
    username: data.username || user.username,
    realName: data.realName || user.realName,
    role: data.role || user.role || 'user',
    deptId: data.deptId ?? user.deptId,
    deptName: data.deptName || user.deptName || '',
    status: data.status ?? user.status,
    forcePasswordChange: data.forcePasswordChange ?? user.forcePasswordChange ?? 0,
    roleIds: data.roleIds || user.roleIds || [],
    roleNames: data.roleNames || user.roleNames || []
  }
  writeStoredUserContext(userInfo, permissions)
  touchPermissions()
  return { user: userInfo, permissions }
}

export const clearUserContext = () => {
  clearStoredUserContext()
  touchPermissions()
  window.dispatchEvent(new CustomEvent('user-context-refreshed', {
    detail: { user: null, permissions: [] }
  }))
}

export const getPermissions = () => {
  return readStoredPermissions()
}

export const hasPermission = (code) => {
  permissionVersion.value
  if (!code) return true
  return getPermissions().includes(code)
}

export const hasAnyPermission = (codes) => {
  if (!codes || codes.length === 0) return true
  return codes.some(code => hasPermission(code))
}

export const isAdminUser = () => getUserInfo()?.role === 'admin'

export const loadUserContext = async () => {
  const res = await userApi.getInfo()
  if (res.code === 200) {
    return setUserContext(res.data || {})
  }
  return { user: getUserInfo(), permissions: getPermissions() }
}

export const refreshUserContext = async () => {
  const context = await loadUserContext()
  window.dispatchEvent(new CustomEvent('user-context-refreshed', { detail: context }))
  return context
}
