import axios from 'axios'
import { Message } from '@arco-design/web-vue'
import { clearStoredUserContext, readStoredToken } from '@/utils/userContextStorage'

const api = axios.create({
  baseURL: window.location.protocol + '//' + window.location.hostname + ':8080',
  timeout: 600000
})

const emitPermissionDenied = (detail) => {
  window.dispatchEvent(new CustomEvent('api-permission-denied', { detail }))
}

api.interceptors.request.use(config => {
  const token = readStoredToken()
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

api.interceptors.response.use(
  resp => {
    const data = resp.data
    if (data?.code === 403) {
      emitPermissionDenied(data)
    }
    return data
  },
  err => {
    if (err.response && err.response.status === 401) {
      clearStoredUserContext()
      window.dispatchEvent(new CustomEvent('user-context-refreshed', {
        detail: { user: null, permissions: [] }
      }))
      window.location.href = '/login'
    } else if (err.response && err.response.status === 403) {
      emitPermissionDenied(err.response.data)
      Message.error(err.response.data?.msg || '无权限操作')
    }
    return Promise.reject(err)
  }
)

export const userApi = {
  login: (data) => api.post('/api/user/login', data),
  logout: () => api.post('/api/user/logout'),
  register: (data) => api.post('/api/user/register', data),
  getInfo: () => api.get('/api/user/info'),
  profile: () => api.get('/api/user/profile'),
  changeOwnPassword: (data) => api.put('/api/user/profile/password', data),
  permissions: () => api.get('/api/user/permissions'),
  list: () => api.get('/api/user/list'),
  updateRole: (id, role) => api.put('/api/user/role/' + id, null, { params: { role } }),
  updatePassword: (id, password) => api.put('/api/user/password/' + id, null, { params: { password } }),
  systemList: (params) => api.get('/api/system/users', { params }),
  systemExport: (params) => api.get('/api/system/users/export', { params, responseType: 'blob' }),
  systemDownloadTemplate: () => api.get('/api/system/users/template', { responseType: 'blob' }),
  systemImport: (formData) => api.post('/api/system/users/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
  systemAdd: (data) => api.post('/api/system/users', data),
  systemUpdate: (id, data) => api.put('/api/system/users/' + id, data),
  systemUpdatePassword: (id, password, forcePasswordChange = 0) => api.put('/api/system/users/' + id + '/password', { password, forcePasswordChange }),
  systemUpdateStatus: (id, status) => api.put('/api/system/users/' + id + '/status', { status }),
  systemBatchStatus: (userIds, status) => api.put('/api/system/users/status', { userIds, status }),
  systemMoveDept: (userIds, deptId) => api.put('/api/system/users/dept', { userIds, deptId }),
  systemUserRoles: (id) => api.get('/api/system/users/' + id + '/roles'),
  systemAssignRoles: (id, roleIds) => api.put('/api/system/users/' + id + '/roles', { roleIds })
}

export const projectApi = {
  upload: (formData, onProgress) => {
    const config = {
      timeout: 600000
    }
    if (onProgress) {
      config.onUploadProgress = (progressEvent) => {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percent)
      }
    }
    return api.post('/api/project/upload', formData, config)
  },
  list: (params) => api.get('/api/project/list', { params }),
  detail: (id) => api.get('/api/project/' + id),
  download: (id) => api.get('/api/project/download/' + id, { responseType: 'blob' }),
  downloadMarkdown: (id) => api.get('/api/project/download/' + id + '/markdown', { responseType: 'blob' }),
  delete: (id) => api.delete('/api/project/' + id),
  batchDelete: (ids) => api.post('/api/project/batch-delete', { ids: ids })
}

export const roleApi = {
  list: () => api.get('/api/system/roles'),
  export: () => api.get('/api/system/roles/export', { responseType: 'blob' }),
  add: (data) => api.post('/api/system/roles', data),
  update: (id, data) => api.put('/api/system/roles/' + id, data),
  delete: (id) => api.delete('/api/system/roles/' + id),
  disableImpact: (id) => api.get('/api/system/roles/' + id + '/disable-impact'),
  deleteImpact: (id) => api.get('/api/system/roles/' + id + '/delete-impact'),
  permissionCatalog: () => api.get('/api/system/roles/permission-catalog'),
  permissions: (id) => api.get('/api/system/roles/' + id + '/permissions'),
  updateFeatures: (id, features) => api.put('/api/system/roles/' + id + '/features', { features }),
  updateDataScopes: (id, dataScopes) => api.put('/api/system/roles/' + id + '/data-scopes', { dataScopes }),
  updateFields: (id, fields) => api.put('/api/system/roles/' + id + '/fields', { fields })
}

export const operationLogApi = {
  list: (params) => api.get('/api/system/logs', { params }),
  export: (params) => api.get('/api/system/logs/export', { params, responseType: 'blob' })
}

export const systemConfigApi = {
  list: (params) => api.get('/api/system/configs', { params }),
  export: (params) => api.get('/api/system/configs/export', { params, responseType: 'blob' }),
  downloadTemplate: () => api.get('/api/system/configs/template', { responseType: 'blob' }),
  importConfigs: (formData) => api.post('/api/system/configs/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
  add: (data) => api.post('/api/system/configs', data),
  update: (id, data) => api.put('/api/system/configs/' + id, data),
  updateStatus: (id, status) => api.put('/api/system/configs/' + id + '/status', { status }),
  disable: (id) => api.delete('/api/system/configs/' + id),
  publicBasic: () => api.get('/api/system/configs/public/basic')
}

export const fileApi = {
  list: (params) => api.get('/api/system/files', { params }),
  export: (params) => api.get('/api/system/files/export', { params, responseType: 'blob' }),
  detail: (id) => api.get('/api/system/files/' + id),
  rules: () => api.get('/api/system/files/rules'),
  upload: (formData) => api.post('/api/system/files/upload', formData),
  download: (id) => api.get('/api/system/files/' + id + '/download', { responseType: 'blob' }),
  preview: (id) => api.get('/api/system/files/' + id + '/preview', { responseType: 'blob' }),
  updateStatus: (id, status) => api.put('/api/system/files/' + id + '/status', { status }),
  disable: (id) => api.delete('/api/system/files/' + id),
  links: (params) => api.get('/api/system/files/links', { params }),
  addLink: (data) => api.post('/api/system/files/links', data),
  removeLink: (id) => api.delete('/api/system/files/links/' + id)
}

export const messageApi = {
  myList: (params) => api.get('/api/system/messages/my', { params }),
  export: (params) => api.get('/api/system/messages/export', { params, responseType: 'blob' }),
  unreadCount: () => api.get('/api/system/messages/unread-count'),
  markRead: (id) => api.put('/api/system/messages/' + id + '/read'),
  markAllRead: () => api.put('/api/system/messages/read-all'),
  delete: (id) => api.delete('/api/system/messages/' + id),
  send: (data) => api.post('/api/system/messages', data)
}

export const todoApi = {
  myList: (params) => api.get('/api/system/todos/my', { params }),
  detail: (id) => api.get('/api/system/todos/' + id),
  export: (params) => api.get('/api/system/todos/export', { params, responseType: 'blob' }),
  create: (data) => api.post('/api/system/todos', data),
  updateStatus: (id, status) => api.put('/api/system/todos/' + id + '/status', { status })
}

export const dictApi = {
  types: (params) => api.get('/api/system/dicts/types', { params }),
  exportTypes: (params) => api.get('/api/system/dicts/types/export', { params, responseType: 'blob' }),
  downloadTypeTemplate: () => api.get('/api/system/dicts/types/template', { responseType: 'blob' }),
  importTypes: (formData) => api.post('/api/system/dicts/types/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
  addType: (data) => api.post('/api/system/dicts/types', data),
  updateType: (id, data) => api.put('/api/system/dicts/types/' + id, data),
  updateTypeStatus: (id, status) => api.put('/api/system/dicts/types/' + id + '/status', { status }),
  deleteType: (id) => api.delete('/api/system/dicts/types/' + id),
  typeDisableImpact: (id) => api.get('/api/system/dicts/types/' + id + '/disable-impact'),
  items: (typeId, params) => api.get('/api/system/dicts/types/' + typeId + '/items', { params }),
  exportItems: (typeId, params) => api.get('/api/system/dicts/types/' + typeId + '/items/export', { params, responseType: 'blob' }),
  downloadItemTemplate: (typeId) => api.get('/api/system/dicts/types/' + typeId + '/items/template', { responseType: 'blob' }),
  importItems: (typeId, formData) => api.post('/api/system/dicts/types/' + typeId + '/items/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
  addItem: (typeId, data) => api.post('/api/system/dicts/types/' + typeId + '/items', data),
  updateItem: (id, data) => api.put('/api/system/dicts/items/' + id, data),
  updateItemStatus: (id, status) => api.put('/api/system/dicts/items/' + id + '/status', { status }),
  deleteItem: (id) => api.delete('/api/system/dicts/items/' + id),
  itemDisableImpact: (id) => api.get('/api/system/dicts/items/' + id + '/disable-impact'),
  publicItems: (typeCode) => api.get('/api/system/dicts/public/' + typeCode)
}

export const deptApi = {
  tree: () => api.get('/api/system/departments/tree'),
  export: () => api.get('/api/system/departments/export', { responseType: 'blob' }),
  downloadTemplate: () => api.get('/api/system/departments/template', { responseType: 'blob' }),
  importDepartments: (formData) => api.post('/api/system/departments/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
  users: (deptId, params) => api.get('/api/system/departments/' + deptId + '/users', { params }),
  unassignedUsers: (params) => api.get('/api/system/departments/unassigned/users', { params }),
  managerCandidates: (id) => api.get('/api/system/departments/' + id + '/manager-candidates'),
  add: (data) => api.post('/api/system/departments', data),
  update: (id, data) => api.put('/api/system/departments/' + id, data),
  deleteImpact: (id) => api.get('/api/system/departments/' + id + '/delete-impact'),
  delete: (id) => api.delete('/api/system/departments/' + id)
}

export default api
