import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/components/Layout.vue'
import Login from '@/views/Login.vue'
import Register from '@/views/Register.vue'
import Home from '@/views/Home.vue'
import Dashboard from '@/views/Dashboard.vue'
import Projects from '@/views/Projects.vue'
import ProjectDetail from '@/views/ProjectDetail.vue'
import Admin from '@/views/Admin.vue'
import UserManage from '@/views/UserManage.vue'
import PromptConfig from '@/views/PromptConfig.vue'

const routes = [
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  {
    path: '/',
    component: Layout,
    redirect: (to) => {
      const role = localStorage.getItem('role')
      return role === 'admin' ? '/dashboard' : '/projects'
    },
    meta: { requiresAuth: true },
    children: [
      { path: '/dashboard', name: 'Dashboard', component: Dashboard, meta: { requiresAdmin: true } },
      { path: '/home', name: 'Home', component: Home },
      { path: '/projects', name: 'Projects', component: Projects },
      { path: '/project/:id', name: 'ProjectDetail', component: ProjectDetail },
      { path: '/admin', name: 'Admin', component: Admin, meta: { requiresAdmin: true } },
      { path: '/user', name: 'UserManage', component: UserManage, meta: { requiresAdmin: true } },
      { path: '/prompt', name: 'PromptConfig', component: PromptConfig, meta: { requiresAdmin: true } }
    ]
  }
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
    next('/projects')
  } else {
    next()
  }
})

export default router
