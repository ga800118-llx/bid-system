<template>
  <div class="dashboard-page">
    <div class="content">
      <section class="welcome-panel">
        <div class="welcome-copy">
          <div class="welcome-badge">{{ enterpriseName }}</div>
          <h2 class="welcome-title">{{ greeting }}，{{ displayName }}</h2>
          <p class="welcome-summary">{{ headlineText }}</p>
          <div class="welcome-meta">
            <span>{{ systemTitle }}</span>
            <span>{{ todayText }}</span>
          </div>
        </div>
      </section>

      <section class="task-card-grid">
        <button
          type="button"
          class="task-card"
          :class="{ emphasized: todoSummary.pending > 0 }"
          @click="router.push('/todo')"
        >
          <div class="task-card-label">待处理待办</div>
          <div class="task-card-value">{{ todoSummary.pending }}</div>
          <div class="task-card-meta">进入待办中心处理事项</div>
        </button>

        <button
          type="button"
          class="task-card warning"
          :class="{ emphasized: todoSummary.overdue > 0 }"
          @click="router.push('/todo')"
        >
          <div class="task-card-label">逾期待办</div>
          <div class="task-card-value">{{ todoSummary.overdue }}</div>
          <div class="task-card-meta">优先跟进超时任务</div>
        </button>

        <button
          type="button"
          class="task-card info"
          :class="{ emphasized: unreadCount > 0 }"
          @click="router.push('/message')"
        >
          <div class="task-card-label">未读消息</div>
          <div class="task-card-value">{{ unreadCount }}</div>
          <div class="task-card-meta">查看通知与业务提醒</div>
        </button>

        <button
          type="button"
          class="task-card subtle"
          @click="openFourthCard"
        >
          <div class="task-card-label">{{ fourthCardTitle }}</div>
          <div class="task-card-value">{{ fourthCardValue }}</div>
          <div class="task-card-meta">{{ fourthCardMeta }}</div>
        </button>
      </section>

      <section class="workbench-main">
        <div class="todo-panel">
          <div class="section-heading">
            <div>
              <h3>我的待办</h3>
              <span>优先处理未完成和即将到期事项</span>
            </div>
            <a-button type="text" size="small" @click="router.push('/todo')">查看全部</a-button>
          </div>
          <div v-if="todoLoading" class="panel-loading">待办加载中...</div>
          <a-empty v-else-if="recentTodos.length === 0" description="当前没有待处理事项" />
          <div v-else class="task-list">
            <button
              v-for="item in recentTodos"
              :key="item.id"
              type="button"
              class="task-list-item"
              :class="{ overdue: item.overdue }"
              @click="openTodo(item)"
            >
              <div class="task-list-main">
                <div class="task-list-title-row">
                  <div class="task-list-title-wrap">
                    <a-tag :color="priorityColor(item.priority)" size="small">{{ priorityLabel(item.priority) }}</a-tag>
                    <span class="task-list-title">{{ item.title }}</span>
                  </div>
                  <span class="task-list-arrow">›</span>
                </div>
                <div class="task-list-subtitle">
                  <span class="task-list-status" :class="{ overdue: item.overdue }">
                    {{ item.overdue ? '已逾期' : todoStatusLabel(item.status) }}
                  </span>
                  <span>{{ formatCompactTime(item.dueAt, '未设置截止时间') }}</span>
                  <span v-if="item.bizType">{{ item.bizType }}{{ item.bizId ? ` #${item.bizId}` : '' }}</span>
                </div>
              </div>
            </button>
          </div>
        </div>

        <div class="message-panel">
          <div class="section-heading">
            <div>
              <h3>消息通知</h3>
              <span>最近 5 条消息，支持直接前往处理</span>
            </div>
            <a-button type="text" size="small" @click="router.push('/message')">查看全部</a-button>
          </div>
          <div v-if="messageLoading" class="panel-loading">消息加载中...</div>
          <a-empty v-else-if="recentMessages.length === 0" description="当前没有新的消息通知" />
          <div v-else class="message-list">
            <button
              v-for="item in recentMessages"
              :key="item.id"
              type="button"
              class="message-list-item"
              @click="openMessage(item)"
            >
              <div class="message-list-side">
                <a-tag :color="messageTypeColor(item.messageType)" size="small">{{ messageTypeLabel(item.messageType) }}</a-tag>
              </div>
              <div class="message-list-main">
                <div class="message-list-title-row">
                  <span class="message-list-title">{{ item.title }}</span>
                  <span class="message-list-time">{{ formatCompactTime(item.createdAt) }}</span>
                </div>
                <div class="message-list-subtitle">{{ item.content }}</div>
              </div>
              <div class="message-list-tail">
                <span v-if="item.readStatus === 0" class="message-unread-dot"></span>
                <span class="message-list-arrow">›</span>
              </div>
            </button>
          </div>
        </div>
      </section>

      <section class="entry-section">
        <div class="section-heading">
          <div>
            <h3>常用入口</h3>
            <span>根据当前权限展示平台常用页面</span>
          </div>
        </div>
        <div class="entry-group-list">
          <div
            v-for="group in quickLinkGroups"
            :key="group.key"
            class="entry-group"
          >
            <div class="entry-group-title">{{ group.title }}</div>
            <div class="entry-grid">
              <button
                v-for="item in group.items"
                :key="item.key"
                type="button"
                class="entry-card"
                @click="router.push(item.path)"
              >
                <div class="entry-title-row">
                  <span class="entry-title">{{ item.title }}</span>
                  <span class="entry-arrow">›</span>
                </div>
                <div class="entry-desc">{{ item.description }}</div>
              </button>
            </div>
          </div>
        </div>
      </section>

      <section class="overview-section">
        <div class="section-heading">
          <div>
            <h3>历史业务概览</h3>
            <span>保留原有统计和趋势图，降级为辅助参考信息</span>
          </div>
        </div>

        <div class="overview-card-grid">
          <div class="overview-card">
            <div class="overview-card-label">总项目数</div>
            <div class="overview-card-value">{{ stats.total }}</div>
          </div>
          <div class="overview-card">
            <div class="overview-card-label">本月新增</div>
            <div class="overview-card-value">{{ stats.monthlyNew }}</div>
          </div>
          <div class="overview-card">
            <div class="overview-card-label">待开标</div>
            <div class="overview-card-value">{{ stats.pendingBidOpen }}</div>
          </div>
          <div class="overview-card">
            <div class="overview-card-label">代理机构数</div>
            <div class="overview-card-value">{{ stats.agencyCount }}</div>
          </div>
        </div>

        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-heading">
              <h4>月度上传趋势</h4>
              <span>最近月份项目入库情况</span>
            </div>
            <div ref="monthlyChartRef" class="chart"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-heading">
              <h4>代理机构 TOP5</h4>
              <span>当前项目分布最多的机构</span>
            </div>
            <div ref="agencyChartRef" class="chart"></div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import axios from 'axios'
import { fileApi, messageApi, todoApi } from '@/api'
import { getUserInfo, hasPermission } from '@/utils/permission'
import { DEFAULT_ENTERPRISE_NAME, DEFAULT_SYSTEM_TITLE, loadPublicSystemConfig } from '@/utils/systemConfig'
import { readStoredToken } from '@/utils/userContextStorage'

const router = useRouter()
const token = readStoredToken()
const baseURL = window.location.protocol + '//' + window.location.hostname + ':8080/api'
const userInfo = getUserInfo() || {}

const systemTitle = ref(DEFAULT_SYSTEM_TITLE)
const enterpriseName = ref(DEFAULT_ENTERPRISE_NAME)
const stats = ref({ total: 0, monthlyNew: 0, pendingBidOpen: 0, agencyCount: 0 })
const unreadCount = ref(0)
const recentMessages = ref([])
const recentTodos = ref([])
const todoSummary = ref({ pending: 0, processing: 0, done: 0, overdue: 0 })
const recentFileCount = ref(0)
const messageLoading = ref(false)
const todoLoading = ref(false)
const monthlyChartRef = ref(null)
const agencyChartRef = ref(null)
const monthlyChartData = ref({})
const agencyChartData = ref({})

let monthlyChart = null
let agencyChart = null
let themeObserver = null

const canReadTodo = hasPermission('system_todo:read')
const canReadFile = hasPermission('system_file:read')
const displayName = computed(() => userInfo.realName || userInfo.username || '用户')
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})
const todayText = computed(() => {
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  }).format(new Date())
})
const headlineText = computed(() => {
  const parts = []
  if (canReadTodo) {
    parts.push(`今天有 ${todoSummary.value.pending} 个待办`)
    parts.push(`${todoSummary.value.overdue} 个逾期待办`)
  }
  parts.push(`${unreadCount.value} 条未读消息`)
  return parts.join('，') + '。'
})
const fourthCardTitle = computed(() => canReadFile ? '最近文件' : '帮助中心')
const fourthCardValue = computed(() => canReadFile ? String(recentFileCount.value) : '1')
const fourthCardMeta = computed(() => canReadFile ? '查看近期文件记录' : '查看平台帮助与接手文档')

const quickLinkGroups = computed(() => {
  const groups = [
    {
      key: 'personal',
      title: '个人工作',
      items: [
        {
          key: 'todo',
          title: '待办中心',
          description: '查看待处理、处理中和逾期事项。',
          path: '/todo',
          visible: canReadTodo
        },
        {
          key: 'message',
          title: '消息中心',
          description: '查看通知、提醒和业务消息。',
          path: '/message',
          visible: true
        },
        {
          key: 'account',
          title: '个人中心',
          description: '查看账号信息和登录安全状态。',
          path: '/account',
          visible: true
        },
        {
          key: 'help',
          title: '帮助中心',
          description: '查看平台说明、接手入口和使用手册。',
          path: '/help',
          visible: true
        }
      ]
    },
    {
      key: 'common',
      title: '通用能力',
      items: [
        {
          key: 'file',
          title: '文件中心',
          description: '统一查看上传、预览和关联文件。',
          path: '/system/file',
          visible: hasPermission('system_file:read')
        }
      ]
    },
    {
      key: 'system',
      title: '系统管理',
      items: [
        {
          key: 'user',
          title: '用户管理',
          description: '维护账号、角色和部门归属。',
          path: '/system/user',
          visible: hasPermission('system_user:read')
        },
        {
          key: 'department',
          title: '部门管理',
          description: '维护组织结构和负责人信息。',
          path: '/system/department',
          visible: hasPermission('system_department:read')
        },
        {
          key: 'role',
          title: '角色权限',
          description: '配置角色、权限范围和字段控制。',
          path: '/system/role',
          visible: hasPermission('system_role:read')
        },
        {
          key: 'config',
          title: '系统配置',
          description: '集中管理系统标题、水印和规则。',
          path: '/system/config',
          visible: hasPermission('system_config:read')
        },
        {
          key: 'dict',
          title: '字典管理',
          description: '维护固定选项和业务枚举值。',
          path: '/system/dict',
          visible: hasPermission('system_dict:read')
        },
        {
          key: 'log',
          title: '操作日志',
          description: '查看关键写操作和安全审计记录。',
          path: '/system/log',
          visible: hasPermission('system_log:read')
        }
      ]
    }
  ]
  return groups
    .map(group => ({
      ...group,
      items: group.items.filter(item => item.visible)
    }))
    .filter(group => group.items.length > 0)
})

async function loadBranding() {
  const config = await loadPublicSystemConfig()
  systemTitle.value = config.systemTitle || DEFAULT_SYSTEM_TITLE
  enterpriseName.value = config.enterpriseName || DEFAULT_ENTERPRISE_NAME
}

async function fetchStats() {
  try {
    const [statsRes, monthlyRes, agencyRes] = await Promise.all([
      axios.get(baseURL + '/dashboard/stats', { headers: { Authorization: 'Bearer ' + token } }),
      axios.get(baseURL + '/dashboard/monthly', { headers: { Authorization: 'Bearer ' + token } }),
      axios.get(baseURL + '/dashboard/by-agency', { headers: { Authorization: 'Bearer ' + token } })
    ])
    if (statsRes.data.code === 200) stats.value = statsRes.data.data || stats.value
    if (monthlyRes.data.code === 200) {
      monthlyChartData.value = monthlyRes.data.data || {}
      drawMonthly(monthlyChartData.value)
    }
    if (agencyRes.data.code === 200) {
      agencyChartData.value = agencyRes.data.data || {}
      drawAgency(agencyChartData.value)
    }
  } catch (error) {
    console.error(error)
  }
}

async function fetchMessages() {
  messageLoading.value = true
  try {
    const [countRes, listRes] = await Promise.all([
      messageApi.unreadCount(),
      messageApi.myList({ page: 1, size: 5 })
    ])
    if (countRes.code === 200) {
      unreadCount.value = Number(
        countRes.data?.unreadCount ??
        countRes.data?.count ??
        countRes.data ??
        0
      )
    }
    if (listRes.code === 200) {
      recentMessages.value = Array.isArray(listRes.data?.records) ? listRes.data.records.slice(0, 5) : []
      if (!Number.isFinite(unreadCount.value)) {
        unreadCount.value = Number(listRes.data?.unreadCount || 0)
      }
    }
    if (!Number.isFinite(unreadCount.value)) unreadCount.value = 0
  } finally {
    messageLoading.value = false
  }
}

async function fetchTodos() {
  if (!canReadTodo) return
  todoLoading.value = true
  try {
    const res = await todoApi.myList({ page: 1, size: 5 })
    if (res.code === 200) {
      recentTodos.value = Array.isArray(res.data?.records) ? res.data.records.slice(0, 5) : []
      todoSummary.value = {
        pending: Number(res.data?.summary?.pending || 0),
        processing: Number(res.data?.summary?.processing || 0),
        done: Number(res.data?.summary?.done || 0),
        overdue: Number(res.data?.summary?.overdue || 0)
      }
    }
  } finally {
    todoLoading.value = false
  }
}

async function fetchRecentFiles() {
  if (!canReadFile) return
  try {
    const res = await fileApi.list({ page: 1, size: 5 })
    if (res.code === 200) {
      const records = Array.isArray(res.data?.records) ? res.data.records : []
      recentFileCount.value = records.length
    }
  } catch (error) {
    console.error(error)
  }
}

function drawMonthly(data) {
  if (!monthlyChartRef.value) return
  if (!monthlyChart) monthlyChart = echarts.init(monthlyChartRef.value)
  const colors = readChartColors()
  monthlyChart.setOption({
    textStyle: { color: colors.textRegular },
    tooltip: { trigger: 'axis', backgroundColor: colors.card, borderColor: colors.border, textStyle: { color: colors.textPrimary } },
    xAxis: {
      type: 'category',
      data: Array.isArray(data.months) ? data.months : [],
      axisLabel: { color: colors.textSecondary },
      axisTick: { show: false },
      axisLine: { lineStyle: { color: colors.axis } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: colors.textSecondary },
      splitLine: { lineStyle: { color: colors.grid } }
    },
    series: [{
      type: 'bar',
      data: Array.isArray(data.counts) ? data.counts : [],
      itemStyle: { color: colors.primary, borderRadius: [6, 6, 0, 0] },
      barWidth: '42%'
    }],
    grid: { left: 42, right: 16, top: 16, bottom: 28 }
  })
}

function drawAgency(data) {
  if (!agencyChartRef.value) return
  if (!agencyChart) agencyChart = echarts.init(agencyChartRef.value)
  const colors = readChartColors()
  const agencies = Array.isArray(data.agencies) ? [...data.agencies].reverse() : []
  const counts = Array.isArray(data.counts) ? [...data.counts].reverse() : []
  agencyChart.setOption({
    textStyle: { color: colors.textRegular },
    tooltip: { trigger: 'axis', backgroundColor: colors.card, borderColor: colors.border, textStyle: { color: colors.textPrimary } },
    xAxis: {
      type: 'value',
      axisLabel: { color: colors.textSecondary },
      splitLine: { lineStyle: { color: colors.grid } }
    },
    yAxis: {
      type: 'category',
      data: agencies,
      axisLabel: { overflow: 'truncate', width: 120, color: colors.textSecondary }
    },
    series: [{
      type: 'bar',
      data: counts,
      itemStyle: { color: colors.success, borderRadius: [0, 6, 6, 0] },
      barWidth: '42%'
    }],
    grid: { left: 130, right: 16, top: 16, bottom: 28 }
  })
}

function handleResize() {
  monthlyChart?.resize()
  agencyChart?.resize()
}

function readThemeVar(name) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

function readChartColors() {
  return {
    primary: readThemeVar('--ds-color-primary') || '#165dff',
    success: readThemeVar('--ds-color-success') || '#00b42a',
    axis: readThemeVar('--ds-color-chart-axis') || '#d9dee8',
    grid: readThemeVar('--ds-color-chart-grid') || '#eef1f6',
    card: readThemeVar('--ds-color-bg-card') || '#ffffff',
    border: readThemeVar('--ds-color-border') || '#e5e8ef',
    textPrimary: readThemeVar('--ds-color-text-primary') || '#1d2129',
    textRegular: readThemeVar('--ds-color-text-regular') || '#4e5969',
    textSecondary: readThemeVar('--ds-color-text-secondary') || '#86909c'
  }
}

function redrawChartsForTheme() {
  drawMonthly(monthlyChartData.value)
  drawAgency(agencyChartData.value)
}

function formatTime(value, fallback = '-') {
  if (!value) return fallback
  return String(value).replace('T', ' ').slice(0, 16)
}

function formatCompactTime(value, fallback = '-') {
  if (!value) return fallback
  const normalized = String(value).replace('T', ' ')
  const [datePart = '', timePart = ''] = normalized.split(' ')
  const today = new Date()
  const yyyy = today.getFullYear()
  const mm = String(today.getMonth() + 1).padStart(2, '0')
  const dd = String(today.getDate()).padStart(2, '0')
  const todayText = `${yyyy}-${mm}-${dd}`
  if (datePart === todayText) {
    return `今天 ${timePart.slice(0, 5)}`
  }
  return normalized.slice(5, 16)
}

function priorityLabel(priority) {
  if (priority === 'HIGH') return '高优先级'
  if (priority === 'LOW') return '低优先级'
  return '中优先级'
}

function priorityColor(priority) {
  if (priority === 'HIGH') return 'orangered'
  if (priority === 'LOW') return 'green'
  return 'gold'
}

function todoStatusLabel(status) {
  if (status === 'DONE') return '已完成'
  if (status === 'PROCESSING') return '处理中'
  return '待处理'
}

function todoStatusColor(status) {
  if (status === 'DONE') return 'green'
  if (status === 'PROCESSING') return 'gold'
  return 'arcoblue'
}

function messageTypeLabel(type) {
  if (type === 'NOTICE') return '通知'
  if (type === 'TODO') return '待办'
  if (type === 'WARNING') return '预警'
  return '系统'
}

function messageTypeColor(type) {
  if (type === 'NOTICE') return 'arcoblue'
  if (type === 'TODO') return 'purple'
  if (type === 'WARNING') return 'orangered'
  return 'green'
}

function openMessage(record) {
  if (!record) return
  if (record.relatedPath) {
    router.push(record.relatedPath)
    return
  }
  router.push('/message')
}

function openTodo(record) {
  if (!record?.id) return
  router.push(`/todo?todoId=${record.id}`)
}

function openFourthCard() {
  router.push(canReadFile ? '/system/file' : '/help')
}

onMounted(async () => {
  await loadBranding()
  await Promise.all([
    fetchStats(),
    fetchMessages(),
    fetchTodos(),
    fetchRecentFiles()
  ])
  await nextTick()
  handleResize()
  window.addEventListener('resize', handleResize)
  themeObserver = new MutationObserver(redrawChartsForTheme)
  themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  themeObserver?.disconnect()
  monthlyChart?.dispose()
  agencyChart?.dispose()
  monthlyChart = null
  agencyChart = null
})
</script>

<style scoped>
.dashboard-page {
  min-height: 100%;
  background: var(--ds-color-bg-page);
}

.content {
  padding: 24px;
}

.welcome-panel,
.task-card,
.todo-panel,
.message-panel,
.entry-section,
.overview-section,
.chart-panel,
.overview-card {
  border-radius: 8px;
  background: var(--ds-color-bg-card);
  box-shadow: var(--ds-shadow-card);
}

.welcome-panel {
  margin-bottom: 16px;
  padding: 24px;
}

.welcome-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--ds-color-bg-selected);
  color: var(--ds-color-primary);
  font-size: 12px;
  line-height: 18px;
}

.welcome-title {
  margin: 12px 0 8px;
  color: var(--ds-color-text-primary);
  font-size: 28px;
  line-height: 36px;
}

.welcome-summary {
  margin: 0;
  color: var(--ds-color-text-regular);
  font-size: 15px;
  line-height: 24px;
}

.welcome-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 16px;
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 18px;
}

.task-card-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.task-card {
  padding: 18px 20px;
  border: 1px solid var(--ds-color-border);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.task-card:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--ds-color-primary) 24%, var(--ds-color-border));
}

.task-card.emphasized {
  border-color: color-mix(in srgb, var(--ds-color-primary) 24%, var(--ds-color-border));
  background: var(--ds-color-bg-hover);
}

.task-card.warning.emphasized {
  border-color: color-mix(in srgb, var(--ds-color-warning) 28%, var(--ds-color-border));
  background: var(--ds-color-tag-orange-bg);
}

.task-card.info.emphasized {
  border-color: color-mix(in srgb, var(--ds-color-info) 24%, var(--ds-color-border));
  background: var(--ds-color-tag-blue-bg);
}

.task-card-label {
  color: var(--ds-color-text-secondary);
  font-size: 13px;
  line-height: 20px;
}

.task-card-value {
  margin-top: 10px;
  color: var(--ds-color-text-primary);
  font-size: 32px;
  line-height: 38px;
  font-weight: 700;
}

.task-card.warning .task-card-value {
  color: var(--ds-color-tag-orange-text);
}

.task-card.info .task-card-value {
  color: var(--ds-color-primary);
}

.task-card-meta {
  margin-top: 10px;
  color: var(--ds-color-text-regular);
  font-size: 12px;
  line-height: 18px;
}

.workbench-main {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.todo-panel,
.message-panel,
.entry-section,
.overview-section {
  padding: 20px;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.section-heading h3,
.chart-heading h4 {
  margin: 0;
  color: var(--ds-color-text-primary);
  font-size: 16px;
  line-height: 24px;
}

.section-heading span,
.chart-heading span {
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 18px;
}

.panel-loading {
  padding: 24px 0;
  color: var(--ds-color-text-secondary);
  font-size: 13px;
  line-height: 20px;
}

.task-list,
.message-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.task-list-item,
.message-list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--ds-color-border);
  border-radius: 8px;
  background: var(--ds-color-bg-card);
  text-align: left;
  cursor: pointer;
}

.task-list-item:hover,
.message-list-item:hover {
  border-color: color-mix(in srgb, var(--ds-color-primary) 24%, var(--ds-color-border));
  background: var(--ds-color-bg-hover);
}

.task-list-item.overdue {
  border-color: color-mix(in srgb, var(--ds-color-warning) 24%, var(--ds-color-border));
  background: linear-gradient(90deg, var(--ds-color-tag-orange-bg) 0, var(--ds-color-bg-card) 72px);
}

.task-list-main,
.message-list-main {
  min-width: 0;
  flex: 1;
}

.task-list-title-row,
.message-list-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.task-list-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.task-list-title,
.message-list-title {
  color: var(--ds-color-text-primary);
  font-size: 14px;
  line-height: 22px;
  font-weight: 600;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-list-tags,
.message-list-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: none;
}

.task-list-subtitle,
.message-list-subtitle {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
  color: var(--ds-color-text-regular);
  font-size: 12px;
  line-height: 18px;
}

.task-list-status {
  color: var(--ds-color-text-regular);
}

.task-list-status.overdue {
  color: var(--ds-color-tag-orange-text);
}

.message-list-subtitle {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

.task-list-arrow,
.message-list-arrow {
  color: var(--ds-color-text-placeholder);
  font-size: 18px;
  line-height: 18px;
  flex: none;
}

.message-list-side,
.message-list-tail,
.message-list-time {
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-list-side,
.message-list-time {
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 18px;
  white-space: nowrap;
}

.message-list-tail {
  flex: none;
}

.message-unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--ds-color-danger);
}

.entry-section {
  margin-bottom: 16px;
}

.entry-group-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.entry-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.entry-group-title {
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 18px;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.entry-card {
  padding: 16px;
  border: 1px solid var(--ds-color-border);
  border-radius: 8px;
  background: var(--ds-color-bg-card);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.entry-card:hover {
  border-color: color-mix(in srgb, var(--ds-color-primary) 24%, var(--ds-color-border));
  transform: translateY(-1px);
  box-shadow: 0 4px 12px color-mix(in srgb, var(--ds-color-primary) 12%, transparent);
}

.entry-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.entry-title {
  color: var(--ds-color-text-primary);
  font-size: 15px;
  line-height: 22px;
  font-weight: 600;
}

.entry-arrow {
  color: var(--ds-color-primary);
  font-size: 20px;
  line-height: 20px;
}

.entry-desc {
  margin-top: 8px;
  color: var(--ds-color-text-regular);
  font-size: 13px;
  line-height: 20px;
}

.overview-card-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.overview-card {
  padding: 18px 20px;
}

.overview-card-label {
  color: var(--ds-color-text-secondary);
  font-size: 13px;
  line-height: 20px;
}

.overview-card-value {
  margin-top: 10px;
  color: var(--ds-color-text-primary);
  font-size: 28px;
  line-height: 34px;
  font-weight: 700;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.chart-panel {
  padding: 20px;
}

.chart-heading {
  margin-bottom: 12px;
}

.chart {
  width: 100%;
  height: 300px;
}

@media (max-width: 1280px) {
  .entry-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1080px) {
  .task-card-grid,
  .overview-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .workbench-main,
  .chart-grid {
    grid-template-columns: 1fr;
  }

  .entry-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .content {
    padding: 16px;
  }

  .welcome-panel,
  .todo-panel,
  .message-panel,
  .entry-section,
  .overview-section,
  .chart-panel,
  .overview-card {
    padding: 16px;
  }

  .task-card-grid,
  .entry-grid,
  .overview-card-grid {
    grid-template-columns: 1fr;
  }

  .task-list-item,
  .message-list-item,
  .task-list-title-row,
  .message-list-title-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .task-list-title-wrap {
    width: 100%;
  }

  .message-list-side,
  .message-list-tail {
    align-self: flex-start;
  }

  .task-list-side,
  .message-list-time {
    white-space: normal;
  }
}
</style>
