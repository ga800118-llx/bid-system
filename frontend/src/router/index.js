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
import RoleManage from '@/views/RoleManage.vue'
import OperationLog from '@/views/OperationLog.vue'
import SystemConfig from '@/views/SystemConfig.vue'
import DictManage from '@/views/DictManage.vue'
import SystemFile from '@/views/SystemFile.vue'
import MessageCenter from '@/views/MessageCenter.vue'
import TodoCenter from '@/views/TodoCenter.vue'
import AccountCenter from '@/views/AccountCenter.vue'
import PromptConfig from '@/views/PromptConfig.vue'
import Department from '@/views/Department.vue'
import Help from '@/views/Help.vue'
import DesignSystemLibrary from '@/views/DesignSystemLibrary.vue'
import { getPermissions, getUserInfo, hasPermission as checkPermission, isAdminUser, loadUserContext } from '@/utils/permission'
import { readStoredToken } from '@/utils/userContextStorage'

const routes = [
  { path: '/login', name: 'Login', component: Login, meta: { title: '登录' } },
  { path: '/register', name: 'Register', component: Register, meta: { title: '注册' } },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      { path: '/dashboard', name: 'Dashboard', component: Dashboard, meta: { title: '工作台' } },
      { path: '/home', name: 'Home', component: Home, meta: { title: '首页' } },
      { path: '/projects', name: 'Projects', component: Projects, meta: { title: '项目列表' } },
      { path: '/message', name: 'MessageCenter', component: MessageCenter, meta: { title: '消息中心' } },
      { path: '/todo', name: 'TodoCenter', component: TodoCenter, meta: { title: '待办中心', permission: 'system_todo:read' } },
      { path: '/account', name: 'AccountCenter', component: AccountCenter, meta: { title: '个人中心' } },
      { path: '/project/:id', name: 'ProjectDetail', component: ProjectDetail, meta: { title: '项目详情' } },
      { path: '/admin', name: 'Admin', component: Admin, meta: { title: '上传文件', requiresAdmin: true } },
      { path: '/system/department', name: 'Department', component: Department, meta: { title: '部门管理', permission: 'system_department:read' } },
      { path: '/system/user', name: 'UserManage', component: UserManage, meta: { title: '用户管理', permission: 'system_user:read' } },
      { path: '/system/role', name: 'RoleManage', component: RoleManage, meta: { title: '角色权限', permission: 'system_role:read' } },
      { path: '/system/log', name: 'OperationLog', component: OperationLog, meta: { title: '操作日志', permission: 'system_log:read' } },
      { path: '/system/config', name: 'SystemConfig', component: SystemConfig, meta: { title: '系统配置', permission: 'system_config:read' } },
      { path: '/system/dict', name: 'DictManage', component: DictManage, meta: { title: '字典管理', permission: 'system_dict:read' } },
      { path: '/system/file', name: 'SystemFile', component: SystemFile, meta: { title: '文件中心', permission: 'system_file:read' } },
      { path: '/system/design-system', name: 'DesignSystemLibrary', component: DesignSystemLibrary, meta: { title: '基础平台 UI 组件库' } },
      { path: '/prompt', name: 'PromptConfig', component: PromptConfig, meta: { title: 'Prompt配置', requiresAdmin: true } },
      { path: '/help', name: 'Help', component: Help, meta: { title: '帮助文档' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const token = readStoredToken()
  if (to.meta.requiresAuth && !token) {
    next('/login')
    return
  }
  let userInfo = getUserInfo()
  if (to.meta.requiresAuth && token && !userInfo) {
    try {
      const context = await loadUserContext()
      userInfo = context.user
    } catch {
      userInfo = null
    }
  }
  if (to.meta.requiresAuth && token && Number(userInfo?.forcePasswordChange || 0) === 1 && to.path !== '/account') {
    next('/account')
    return
  }
  if ((to.meta.requiresAdmin || to.meta.permission) && token) {
    let permissions = getPermissions()
    if (permissions.length === 0 || !userInfo) {
      try {
        const context = await loadUserContext()
        permissions = context.permissions || []
        userInfo = context.user
      } catch {
        permissions = []
      }
    }
    if (to.meta.requiresAdmin && !isAdminUser()) {
      next('/projects')
      return
    }
    if (!checkPermission(to.meta.permission)) {
      next('/dashboard')
      return
    }
  }
  next()
})

export default router
