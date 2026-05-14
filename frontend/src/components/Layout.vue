<template>
  <div class="layout" :class="{ 'layout-collapsed': sidebarCollapsed }">
    <aside class="sidebar">
      <div class="sidebar-logo">
        <button class="logo-toggle" type="button" @click="toggleSidebar">
          <IconMenuUnfold v-if="sidebarCollapsed" />
          <IconMenuFold v-else />
        </button>
        <span class="logo-text" :title="systemTitle">{{ sidebarCollapsed ? collapsedSystemTitle : systemTitle }}</span>
      </div>
      <a-menu
        :selected-keys="selectedKeys"
        :collapsed="sidebarCollapsed"
        :collapsed-width="64"
        theme="dark"
        mode="inline"
        @menu-item-click="onMenuClick"
      >
        <a-menu-item key="/dashboard">
          <template #icon><IconDashboard /></template>
          工作台
        </a-menu-item>
        <a-menu-item key="/projects">
          <template #icon><IconFolder /></template>
          项目列表
        </a-menu-item>
        <a-menu-item key="/message">
          <template #icon><IconNotification /></template>
          消息中心
        </a-menu-item>
        <a-menu-item v-if="hasPermission('system_todo:read')" key="/todo">
          <template #icon><IconCalendar /></template>
          待办中心
        </a-menu-item>
        <a-menu-item v-if="roleName == 'admin'" key="/admin">
          <template #icon><IconUpload /></template>
          上传文件
        </a-menu-item>
        <div class="menu-divider" v-if="showSystemMenu && !sidebarCollapsed">系统管理</div>
        <a-menu-item v-if="hasPermission('system_department:read')" key="/system/department">
          <template #icon><IconApps /></template>
          部门管理
        </a-menu-item>
        <a-menu-item v-if="hasPermission('system_user:read')" key="/system/user">
          <template #icon><IconUser /></template>
          用户管理
        </a-menu-item>
        <a-menu-item v-if="hasPermission('system_role:read')" key="/system/role">
          <template #icon><IconApps /></template>
          角色权限
        </a-menu-item>
        <a-menu-item v-if="hasPermission('system_log:read')" key="/system/log">
          <template #icon><IconSettings /></template>
          操作日志
        </a-menu-item>
        <a-menu-item v-if="hasPermission('system_config:read')" key="/system/config">
          <template #icon><IconSettings /></template>
          系统配置
        </a-menu-item>
        <a-menu-item v-if="hasPermission('system_dict:read')" key="/system/dict">
          <template #icon><IconApps /></template>
          字典管理
        </a-menu-item>
        <a-menu-item v-if="hasPermission('system_file:read')" key="/system/file">
          <template #icon><IconFile /></template>
          文件中心
        </a-menu-item>
        <a-menu-item v-if="roleName == 'admin'" key="/prompt">
          <template #icon><IconSettings /></template>
          Prompt配置
        </a-menu-item>
        <a-menu-item key="/help">
          <template #icon><IconQuestion /></template>
          帮助文档
        </a-menu-item>
      </a-menu>
    </aside>
    <div class="layout-main">
      <div v-if="watermarkEnabled && watermarkBackgroundImage" class="watermark-layer" :style="watermarkStyle"></div>
      <div class="workspace-top">
        <DsTopBar
          title="综合管理平台"
          :subtitle="systemTitle"
          :user-name="userName"
          :avatar-text="userAvatarText"
          :notification-count="0"
          :notification-items="notificationItems"
          :user-items="userMenuItems"
          :show-theme-toggle="true"
          :theme-mode="themeMode"
          @notification-select="handleNotificationSelect"
          @user-select="handleUserSelect"
          @theme-toggle="handleThemeToggle"
          @logout="handleLogout"
        >
          <template #logo>
            <IconApps />
          </template>
          <template #notificationIcon>
            <IconNotification />
          </template>
        </DsTopBar>

        <div class="workspace-tabs">
          <div class="tab-list">
            <button
              v-for="tab in tabs"
              :key="tab.fullPath"
              type="button"
              class="workspace-tab"
              :class="{ active: tab.fullPath === route.fullPath }"
              @click="activateTab(tab)"
            >
              <span class="tab-title">{{ tab.title }}</span>
              <span
                v-if="!isHomeTab(tab)"
                class="tab-close"
                @click.stop="closeTab(tab.fullPath)"
              >×</span>
            </button>
          </div>

          <DsHeaderActions
            class="workspace-tabs__actions"
            :action-items="closeActionItems"
            :show-primary="false"
            @action-click="handleCloseAction"
          />
        </div>
      </div>
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { IconDashboard, IconFolder, IconUpload, IconUser, IconSettings, IconQuestion, IconApps, IconMenuFold, IconMenuUnfold, IconFile, IconNotification, IconCalendar } from '@arco-design/web-vue/es/icon'
import { systemConfigApi, userApi } from '@/api'
import { DsHeaderActions, DsTopBar } from '@/design-system'
import { clearUserContext, getPermissions, getUserInfo, loadUserContext, refreshUserContext } from '@/utils/permission'
import { DEFAULT_SYSTEM_TITLE } from '@/utils/systemConfig'
import { initTheme, toggleTheme } from '@/utils/theme'

const router = useRouter()
const route = useRoute()
const SIDEBAR_STORAGE_KEY = 'bid-system-sidebar-collapsed'
const HOME_PATH = '/dashboard'
const homeTab = { fullPath: HOME_PATH, path: HOME_PATH, title: '工作台' }

const sidebarCollapsed = ref(localStorage.getItem(SIDEBAR_STORAGE_KEY) === 'true')
const systemTitle = ref(DEFAULT_SYSTEM_TITLE)
const userInfo = ref(getUserInfo() || {})
const permissions = ref([])
const tabs = ref([homeTab])
const watermarkEnabled = ref(false)
const watermarkTextTemplate = ref('{realName} {username}')
const watermarkOpacity = ref(0.12)
const watermarkFontSize = ref(16)
const watermarkRotate = ref(-24)
const themeMode = ref(initTheme())
let permissionRefreshTimer = null

const activeMenuKey = computed(() => {
  if (route.path.startsWith('/project/')) return '/projects'
  return route.path
})
const selectedKeys = computed(() => [activeMenuKey.value])
const currentTitle = computed(() => route.meta?.title || '页面')
const collapsedSystemTitle = computed(() => systemTitle.value?.trim()?.slice(0, 1) || '系')
const roleName = computed(() => userInfo.value?.role || 'user')
const userName = computed(() => userInfo.value?.realName || userInfo.value?.username || '用户')
const userAvatarText = computed(() => {
  const source = userInfo.value?.realName || userInfo.value?.username || 'U'
  return String(source).trim().slice(0, 1).toUpperCase()
})
const notificationItems = computed(() => {
  const items = [{ key: 'message', label: '消息中心' }]
  if (hasPermission('system_todo:read')) items.push({ key: 'todo', label: '待办中心' })
  return items
})
const userMenuItems = computed(() => ([
  { key: 'account', label: '个人中心' },
  { key: 'help', label: '帮助文档' }
]))
const closeActionItems = computed(() => ([
  { key: 'current', label: '关闭当前', disabled: isHomePath(route.fullPath) },
  { key: 'others', label: '关闭其他', disabled: tabs.value.length <= 1 },
  { key: 'all', label: '关闭全部', disabled: tabs.value.length <= 1 }
]))
const watermarkText = computed(() => {
  const template = String(watermarkTextTemplate.value || '').trim()
  if (!template) return ''
  const now = new Date()
  const dateTime = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
  const roleNames = Array.isArray(userInfo.value?.roleNames) ? userInfo.value.roleNames.filter(Boolean).join(' / ') : ''
  return template
    .replaceAll('{userId}', String(userInfo.value?.userId ?? ''))
    .replaceAll('{username}', userInfo.value?.username || '')
    .replaceAll('{realName}', userInfo.value?.realName || userInfo.value?.username || '')
    .replaceAll('{deptName}', userInfo.value?.deptName || '')
    .replaceAll('{roleNames}', roleNames)
    .replaceAll('{systemTitle}', systemTitle.value || DEFAULT_SYSTEM_TITLE)
    .replaceAll('{dateTime}', dateTime)
    .trim()
})
const watermarkBackgroundImage = computed(() => {
  const text = watermarkText.value
  if (!watermarkEnabled.value || !text) return ''
  const alpha = normalizeWatermarkOpacity(watermarkOpacity.value)
  const watermarkColor = themeMode.value === 'dark' ? '238,243,255' : '29,33,41'
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="320" height="240" viewBox="0 0 320 240">
      <g transform="translate(160 120) rotate(${normalizeWatermarkRotate(watermarkRotate.value)})">
        <text
          x="0"
          y="0"
          text-anchor="middle"
          dominant-baseline="middle"
          fill="rgba(${watermarkColor},${alpha})"
          font-size="${normalizeWatermarkFontSize(watermarkFontSize.value)}"
          font-family="Arial, PingFang SC, Microsoft YaHei, sans-serif"
        >${escapeXml(text)}</text>
      </g>
    </svg>
  `.trim()
  return `url("data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}")`
})
const watermarkStyle = computed(() => ({
  backgroundImage: watermarkBackgroundImage.value
}))
const showSystemMenu = computed(() => [
  'system_department:read',
  'system_user:read',
  'system_role:read',
  'system_log:read',
  'system_config:read',
  'system_dict:read',
  'system_file:read',
  'system_todo:read'
].some(hasPermission))
const getRouteTitle = (targetRoute) => {
  return targetRoute.meta?.title || targetRoute.name || '未命名页面'
}

const addTab = (targetRoute) => {
  if (!targetRoute.meta?.requiresAuth && targetRoute.path !== HOME_PATH) return
  const title = getRouteTitle(targetRoute)
  const existed = tabs.value.some((tab) => tab.fullPath === targetRoute.fullPath)
  if (!existed) {
    tabs.value.push({
      fullPath: targetRoute.fullPath,
      path: targetRoute.path,
      title
    })
  }
}

const isHomePath = (fullPath) => fullPath === HOME_PATH
const isHomeTab = (tab) => isHomePath(tab.fullPath)

const activateTab = (tab) => {
  if (tab.fullPath !== route.fullPath) router.push(tab.fullPath)
}

const closeTab = (fullPath) => {
  if (isHomePath(fullPath)) return

  const closingIndex = tabs.value.findIndex((tab) => tab.fullPath === fullPath)
  if (closingIndex === -1) return

  const isActive = route.fullPath === fullPath
  tabs.value = tabs.value.filter((tab) => tab.fullPath !== fullPath)

  if (isActive) {
    const nextTab = tabs.value[closingIndex] || tabs.value[closingIndex - 1] || tabs.value[0]
    router.push(nextTab.fullPath)
  }
}

const closeCurrentTab = () => {
  closeTab(route.fullPath)
}

const closeOtherTabs = () => {
  const current = tabs.value.find((tab) => tab.fullPath === route.fullPath)
  tabs.value = current && !isHomePath(current.fullPath) ? [homeTab, current] : [homeTab]
}

const closeAllTabs = () => {
  tabs.value = [homeTab]
  if (!isHomePath(route.fullPath)) router.push(HOME_PATH)
}

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem(SIDEBAR_STORAGE_KEY, String(sidebarCollapsed.value))
}

const handleLogout = async () => {
  try {
    await userApi.logout()
  } catch {
    // 退出时本地登录态清理优先，审计失败不阻断用户离开。
  }
  clearUserContext()
  router.push('/login')
}

const onMenuClick = (key) => { router.push(key) }
const handleNotificationSelect = (key) => {
  if (key === 'message') router.push('/message')
  if (key === 'todo') router.push('/todo')
}
const handleUserSelect = (key) => {
  if (key === 'account') router.push('/account')
  if (key === 'help') router.push('/help')
}
const handleCloseAction = (key) => {
  if (key === 'current') closeCurrentTab()
  if (key === 'others') closeOtherTabs()
  if (key === 'all') closeAllTabs()
}

const handleThemeToggle = () => {
  themeMode.value = toggleTheme(themeMode.value)
}

const parsePermissions = () => {
  return getPermissions()
}

const hasPermission = (permission) => {
  return permissions.value.includes(permission)
}

const routeHasAccess = (targetRoute) => {
  if (targetRoute.meta?.requiresAdmin && roleName.value !== 'admin') return false
  if (targetRoute.meta?.permission && !hasPermission(targetRoute.meta.permission)) return false
  return true
}

const pruneTabs = () => {
  tabs.value = tabs.value.filter(tab => {
    if (isHomeTab(tab)) return true
    const resolved = router.resolve(tab.fullPath)
    return routeHasAccess(resolved)
  })
  if (!tabs.value.some(tab => isHomeTab(tab))) {
    tabs.value.unshift(homeTab)
  }
}

const ensureCurrentRouteAccess = () => {
  if (!routeHasAccess(route)) {
    router.push(HOME_PATH)
  }
}

const refreshPermissions = async () => {
  permissions.value = parsePermissions()
  userInfo.value = getUserInfo() || userInfo.value
  try {
    const context = await loadUserContext()
    userInfo.value = context.user || {}
    permissions.value = context.permissions || []
  } catch {
    userInfo.value = getUserInfo() || userInfo.value
    permissions.value = parsePermissions()
  }
}

const fetchBasicConfig = async () => {
  try {
    const res = await systemConfigApi.publicBasic()
    if (res.code === 200) {
      systemTitle.value = res.data?.systemTitle || DEFAULT_SYSTEM_TITLE
      applySidebarDefault(res.data)
      applyWatermarkConfig(res.data)
    }
  } catch {
    systemTitle.value = DEFAULT_SYSTEM_TITLE
    applyWatermarkConfig({})
  }
}

const applySidebarDefault = (config = {}) => {
  if (localStorage.getItem(SIDEBAR_STORAGE_KEY) !== null) return
  sidebarCollapsed.value = config.sidebarCollapsedDefault === true
}

const applyWatermarkConfig = (config = {}) => {
  watermarkEnabled.value = config.watermarkEnabled === true
  watermarkTextTemplate.value = String(config.watermarkTextTemplate || '{realName} {username}')
  watermarkOpacity.value = normalizeWatermarkOpacity(config.watermarkOpacity)
  watermarkFontSize.value = normalizeWatermarkFontSize(config.watermarkFontSize)
  watermarkRotate.value = normalizeWatermarkRotate(config.watermarkRotate)
}

const normalizeWatermarkOpacity = (value) => {
  const next = Number(value)
  if (!Number.isFinite(next)) return 0.12
  if (next < 0.03) return 0.03
  if (next > 0.3) return 0.3
  return next
}

const normalizeWatermarkFontSize = (value) => {
  const next = Number(value)
  if (!Number.isFinite(next)) return 16
  if (next < 10) return 10
  if (next > 32) return 32
  return Math.round(next)
}

const normalizeWatermarkRotate = (value) => {
  const next = Number(value)
  if (!Number.isFinite(next)) return -24
  if (next < -60) return -60
  if (next > 60) return 60
  return Math.round(next)
}

const escapeXml = (value) => {
  return String(value || '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&apos;')
}

const updateDocumentTitle = () => {
  document.title = `${currentTitle.value} - ${systemTitle.value || DEFAULT_SYSTEM_TITLE}`
}

const handleSystemConfigChanged = () => {
  fetchBasicConfig()
}

const handlePermissionsChanged = () => {
  refreshUserContext()
    .then(context => {
      userInfo.value = context.user || {}
      permissions.value = context.permissions || []
      pruneTabs()
      ensureCurrentRouteAccess()
    })
    .catch(() => {
      refreshPermissions()
    })
}

const handleApiPermissionDenied = () => {
  if (permissionRefreshTimer) {
    clearTimeout(permissionRefreshTimer)
  }
  permissionRefreshTimer = setTimeout(() => {
    permissionRefreshTimer = null
    handlePermissionsChanged()
  }, 300)
}

watch(
  () => route.fullPath,
  () => addTab(route),
  { immediate: true }
)

watch([currentTitle, systemTitle], updateDocumentTitle, { immediate: true })

onMounted(() => {
  refreshPermissions()
  fetchBasicConfig()
  window.addEventListener('system-config-updated', handleSystemConfigChanged)
  window.addEventListener('user-permissions-updated', handlePermissionsChanged)
  window.addEventListener('api-permission-denied', handleApiPermissionDenied)
})

onBeforeUnmount(() => {
  if (permissionRefreshTimer) {
    clearTimeout(permissionRefreshTimer)
  }
  window.removeEventListener('system-config-updated', handleSystemConfigChanged)
  window.removeEventListener('user-permissions-updated', handlePermissionsChanged)
  window.removeEventListener('api-permission-denied', handleApiPermissionDenied)
})
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; }
.sidebar { width: 220px; background: var(--ds-color-bg-sidebar); display: flex; flex-direction: column; flex-shrink: 0; transition: width .2s ease; }
.layout-collapsed .sidebar { width: 64px; }
.sidebar-logo { position: relative; height: 60px; display: flex; align-items: center; border-bottom: 1px solid var(--ds-color-sidebar-border); }
.logo-toggle { width: 64px; height: 60px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; border: 0; background: transparent; color: var(--ds-color-sidebar-text); cursor: pointer; font-size: 18px; }
.logo-toggle:hover { color: var(--ds-color-inverse-text); background: var(--ds-color-sidebar-hover); }
.logo-text { color: var(--ds-color-primary); font-size: 16px; font-weight: 600; letter-spacing: 1px; white-space: nowrap; min-width: 0; overflow: hidden; text-overflow: ellipsis; }
.layout-collapsed .logo-text { display: none; }
.layout-main { position: relative; flex: 1; min-width: 0; height: 100vh; overflow: auto; background: linear-gradient(180deg, var(--ds-color-bg-layout-start) 0%, var(--ds-color-bg-layout-mid) 220px, var(--ds-color-bg-layout) 100%); }
.watermark-layer { position: absolute; inset: 0; pointer-events: none; z-index: 5; background-repeat: repeat; background-position: 0 0; }
.menu-divider { color: var(--ds-color-sidebar-text-muted); font-size: 12px; padding: 16px 16px 4px; margin-top: 4px; }
.workspace-top {
  position: sticky;
  top: 0;
  z-index: 20;
  backdrop-filter: blur(12px);
}

.workspace-tabs {
  min-height: 44px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: var(--ds-color-bg-overlay);
  border-bottom: 1px solid color-mix(in srgb, var(--ds-color-primary) 4%, var(--ds-color-border));
}

.tab-list {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
}

.tab-list::-webkit-scrollbar {
  display: none;
}

.workspace-tab {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 180px;
  height: 30px;
  padding: 0 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--ds-color-text-regular);
  cursor: pointer;
  white-space: nowrap;
  flex: 0 0 auto;
}

.workspace-tab:hover {
  background: var(--ds-color-bg-hover);
}

.workspace-tab.active {
  background: var(--ds-color-bg-selected);
  border-color: color-mix(in srgb, var(--ds-color-primary) 20%, transparent);
  color: var(--ds-color-primary);
}

.tab-title {
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 13px;
  font-weight: 500;
}

.tab-close {
  color: var(--ds-color-text-secondary);
  font-size: 14px;
  line-height: 1;
}

.tab-close:hover {
  color: var(--ds-color-danger);
}

.workspace-tabs__actions {
  flex: 0 0 auto;
}

.workspace-tabs__actions :deep(.ds-header-actions) {
  gap: 8px;
}

.workspace-tabs__actions :deep(.ds-header-actions__secondary) {
  min-width: auto;
  height: 30px;
  padding: 0 10px;
  font-size: 12px;
}

@media (max-width: 960px) {
  .workspace-tabs {
    padding: 12px 16px;
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
