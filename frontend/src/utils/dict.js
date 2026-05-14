import { dictApi } from '@/api'

const dictCache = new Map()

if (typeof window !== 'undefined') {
  window.addEventListener('system-dict-updated', () => {
    dictCache.clear()
  })
}

export const normalizeDictItems = (items = []) => {
  return Array.isArray(items)
    ? items
      .filter(item => item && item.status !== 0)
      .map(item => ({
        ...item,
        label: item.itemLabel || item.itemValue,
        value: item.itemValue,
        color: item.tagColor || 'arcoblue'
      }))
      .filter(item => item.value)
    : []
}

export const loadPublicDictItems = async (typeCode, fallback = []) => {
  if (!typeCode) return normalizeDictItems(fallback)
  if (!dictCache.has(typeCode)) {
    dictCache.set(typeCode, dictApi.publicItems(typeCode)
      .then(res => {
        const items = res.code === 200 ? normalizeDictItems(res.data || []) : []
        return items.length ? items : normalizeDictItems(fallback)
      })
      .catch(() => normalizeDictItems(fallback)))
  }
  return dictCache.get(typeCode)
}

export const COMMON_STATUS_FALLBACK = [
  { itemLabel: '正常', itemValue: 'enabled', tagColor: 'green' },
  { itemLabel: '禁用', itemValue: 'disabled', tagColor: 'red' }
]

export const statusValue = (value) => {
  if (value === 'enabled' || value === '1' || value === 1) return 1
  if (value === 'disabled' || value === '0' || value === 0) return 0
  return value
}

export const statusKey = (value) => Number(value) === 1 ? 'enabled' : 'disabled'

export const loadCommonStatusOptions = async () => {
  const items = await loadPublicDictItems('common_status', COMMON_STATUS_FALLBACK)
  return items.map(item => ({ ...item, value: statusValue(item.value) }))
}

export const statusOption = (options = [], value) => {
  const key = statusKey(value)
  const list = Array.isArray(options) ? options : (options?.value || [])
  return list.find(item => item.value === statusValue(value) || item.itemValue === key)
}

export const statusLabel = (options = [], value) => {
  return statusOption(options, value)?.label || (Number(value) === 1 ? '正常' : '禁用')
}

export const statusColor = (options = [], value) => {
  return statusOption(options, value)?.color || (Number(value) === 1 ? 'green' : 'red')
}

export const USER_ROLE_TYPE_FALLBACK = [
  { itemLabel: '管理员', itemValue: 'admin', tagColor: 'gold' },
  { itemLabel: '普通用户', itemValue: 'user', tagColor: 'arcoblue' }
]

export const loadUserRoleTypeOptions = async () => {
  return loadPublicDictItems('user_role_type', USER_ROLE_TYPE_FALLBACK)
}

export const roleTypeOption = (options = [], value) => {
  const list = Array.isArray(options) ? options : (options?.value || [])
  return list.find(item => item.value === value || item.itemValue === value)
}

export const roleTypeLabel = (options = [], value) => {
  return roleTypeOption(options, value)?.label || (value === 'admin' ? '管理员' : '普通用户')
}

export const roleTypeColor = (options = [], value) => {
  return roleTypeOption(options, value)?.color || (value === 'admin' ? 'gold' : 'arcoblue')
}
