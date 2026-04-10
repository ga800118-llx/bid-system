import axios from 'axios'

const api = axios.create({
  baseURL: window.location.protocol + '//' + window.location.hostname + ':8080',
  timeout: 600000
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

api.interceptors.response.use(
  resp => resp.data,
  err => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export const userApi = {
  login: (data) => api.post('/api/user/login', data),
  register: (data) => api.post('/api/user/register', data),
  getInfo: () => api.get('/api/user/info'),
  list: () => api.get('/api/user/list'),
  updateRole: (id, role) => api.put('/api/user/role/' + id, null, { params: { role } }),
  updatePassword: (id, password) => api.put('/api/user/password/' + id, null, { params: { password } })
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
,

export const roleApi = {
  list: () => api.get('/api/system/roles'),
  add: (data) => api.post('/api/system/roles', data),
  update: (id, data) => api.put('/api/system/roles/' + id, data),
  delete: (id) => api.delete('/api/system/roles/' + id)
}

export const deptApi = {
  tree: () => api.get('/api/system/departments/tree'),
  users: (deptId, page, size) => api.get('/api/system/departments/' + deptId + '/users', { params: { page, size } }),
  add: (data) => api.post('/api/system/departments', data),
  update: (id, data) => api.put('/api/system/departments/' + id, data),
  delete: (id) => api.delete('/api/system/departments/' + id)
}}

export const roleApi = {
 list: () => api.get("/api/system/roles")
 add: (data) => api.post("/api/system/roles", data)
 update: (id, data) => api.put("/api/system/roles/"+ id, data)
 delete: (id) => api.delete("/api/system/roles/"+ id)
}
export const deptApi = {
 tree: () => api.get("/api/system/departments/tree")
 users: (deptId, page, size) => api.get("/api/system/departments/"+ deptId+ "/users", { params: { page, size } })
 add: (data) => api.post("/api/system/departments", data)
 update: (id, data) => api.put("/api/system/departments/"+ id, data)
 delete: (id) => api.delete("/api/system/departments/"+ id)
}
export default api

