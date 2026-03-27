import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截：带上token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

// 响应拦截
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

// 用户相关
export const userApi = {
  login: (data) => api.post('/user/login', data),
  register: (data) => api.post('/user/register', data),
  getInfo: () => api.get('/user/info'),
  list: () => api.get('/user/list'),
  updateRole: (id, role) => api.put('/user/role/' + id, null, { params: { role } })
}

// 项目相关
export const projectApi = {
  upload: (formData) => api.post('/project/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  list: (params) => api.get('/project/list', { params }),
  detail: (id) => api.get('/project/' + id),
  download: (id) => api.get('/project/download/' + id, { responseType: 'blob' }),
  delete: (id) => api.delete('/project/' + id)
}

export default api
