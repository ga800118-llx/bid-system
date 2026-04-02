import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue') },
  { path: '/', name: 'Home', component: () => import('@/views/Home.vue'), meta: { requiresAuth: true } },
  { path: '/project/:id', name: 'ProjectDetail', component: () => import('@/views/ProjectDetail.vue'), meta: { requiresAuth: true } },
  { path: '/admin', name: 'Admin', component: () => import('@/views/Admin.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/user', name: 'UserManage', component: () => import('@/views/UserManage.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/prompt', name: 'PromptConfig', component: () => import('@/views/PromptConfig.vue'), meta: { requiresAuth: true, requiresAdmin: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.meta.requiresAdmin && localStorage.getItem('role') !== 'admin') {
    next('/')
  } else {
    next()
  }
})

export default router
