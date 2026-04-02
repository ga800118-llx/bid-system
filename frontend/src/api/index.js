import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
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
  login: (data) => api.post('/user/login', data),
  register: (data) => api.post('/user/register', data),
  getInfo: () => api.get('/user/info'),
  list: () => api.get('/user/list'),
  updateRole: (id, role) => api.put('/user/role/' + id, null, { params: { role } }),
  updatePassword: (id, password) => api.put('/user/password/' + id, null, { params: { password } })
}

export const projectApi = {
  upload: (formData, onProgress) => {
    const config = {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 600000
    }
    if (onProgress) {
      config.onUploadProgress = (progressEvent) => {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percent)
      }
    }
    return api.post('/project/upload', formData, config)
  },
  list: (params) => api.get('/project/list', { params }),
  detail: (id) => api.get('/project/' + id),
  download: (id) => api.get('/project/download/' + id, { responseType: 'blob' }),
  downloadMarkdown: (id) => api.get('/project/download/' + id + '/markdown', { responseType: 'blob' }),
  delete: (id) => api.delete('/project/' + id),
  batchDelete: (ids) => api.post('/project/batch-delete', { ids: ids })
}

export default api
