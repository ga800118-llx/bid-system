<template>
  <div ref="pickerRef" class="message-recipient-picker ds-field">
    <label v-if="label" class="ds-field__label message-recipient-picker__label">
      <span v-if="required" class="ds-field__required">*</span>
      <span>{{ label }}</span>
      <span class="message-recipient-picker__count">{{ selectedUsers.length }} 人</span>
    </label>

    <button
      type="button"
      class="message-recipient-picker__control arco-select-view"
      :class="{ 'is-open': open }"
      @click="toggleDropdown"
    >
      <span ref="tagsRef" class="message-recipient-picker__tags">
        <span v-if="!selectedUsers.length" class="message-recipient-picker__placeholder">{{ placeholder }}</span>
        <span v-for="user in visibleSelectedUsers" :key="user.id" class="message-recipient-picker__tag">
          {{ user.realName || user.username || '未命名用户' }}
          <span class="message-recipient-picker__tag-close" @click.stop="removeUser(user.id)">×</span>
        </span>
        <span
          v-if="hiddenSelectedUsers.length"
          class="message-recipient-picker__tag message-recipient-picker__tag--more"
          :title="hiddenSelectedUsers.map(user => user.realName || user.username).join('、')"
        >
          +{{ hiddenSelectedUsers.length }}
        </span>
      </span>
      <span class="message-recipient-picker__arrow" aria-hidden="true">
        <svg viewBox="0 0 16 16" focusable="false">
          <path d="M4.2 6.1 8 9.9l3.8-3.8" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </span>
    </button>

    <div v-if="open" class="message-recipient-picker__dropdown arco-select-popup" @click.stop>
      <div class="message-recipient-picker__search">
        <DsKeywordSearch v-model="keyword" placeholder="搜索部门 / 姓名 / 账号" />
      </div>
      <div class="message-recipient-picker__summary">
        <span>勾选部门可批量选择下级人员</span>
        <button type="button" :disabled="!selectedUsers.length" @click="clearSelected">清空</button>
      </div>
      <div class="message-recipient-picker__tree">
        <template v-if="visibleRows.length">
          <div
            v-for="row in visibleRows"
            :key="row.node.key"
            class="message-recipient-picker__node"
            :class="[`is-${row.node.type}`, { selected: row.checked }]"
            :style="{ paddingLeft: `${12 + row.level * 18}px` }"
          >
            <button
              v-if="row.node.type === 'dept' && row.node.children?.length"
              type="button"
              class="message-recipient-picker__expand"
              @click.stop="toggleExpand(row.node.key)"
            >
              {{ isExpanded(row.node.key) ? '⌄' : '›' }}
            </button>
            <span v-else class="message-recipient-picker__expand-placeholder"></span>
            <a-checkbox
              :model-value="row.checked"
              :indeterminate="row.indeterminate"
              @change="toggleNode(row.node, $event)"
              @click.stop
            />
            <template v-if="row.node.type === 'dept'">
              <span class="message-recipient-picker__dept-icon">
                <DepartmentIcon />
              </span>
              <span class="message-recipient-picker__dept-name" :title="row.node.label">{{ row.node.label }}</span>
              <span class="message-recipient-picker__dept-count">{{ row.node.userIds.length }} 人</span>
            </template>
            <template v-else>
              <DsUserAvatar
                :name="row.node.user.realName || row.node.user.username"
                :sub-text="row.node.user.username || row.node.user.deptName || ''"
                :size="28"
              />
            </template>
          </div>
        </template>
        <DsEmptyState v-else title="暂无可选人员" description="当前关键词下没有匹配的正常用户。" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { deptApi, userApi } from '@/api'
import { DsEmptyState, DsKeywordSearch, DsUserAvatar } from '@/design-system'
import DepartmentIcon from '@/design-system/icons/DepartmentIcon.vue'

const UNASSIGNED_DEPT_ID = '__unassigned'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  label: { type: String, default: '选择人员' },
  placeholder: { type: String, default: '请选择人员' },
  required: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

const pickerRef = ref(null)
const tagsRef = ref(null)
const deptTree = ref([])
const users = ref([])
const keyword = ref('')
const open = ref(false)
const expandedKeys = ref(new Set())
const visibleTagLimit = ref(2)
let resizeObserver = null

const selectedIds = computed(() => (props.modelValue || []).map(id => Number(id)).filter(id => Number.isFinite(id)))
const selectedSet = computed(() => new Set(selectedIds.value))

const usersByDept = computed(() => {
  const map = new Map()
  for (const user of users.value) {
    const key = user.deptId === null || user.deptId === undefined ? UNASSIGNED_DEPT_ID : String(user.deptId)
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(user)
  }
  return map
})

const treeRoot = computed(() => {
  const departmentChildren = mapDeptNodes(deptTree.value)
  const unassignedUsers = usersByDept.value.get(UNASSIGNED_DEPT_ID) || []
  if (unassignedUsers.length) {
    departmentChildren.push(buildDeptNode({
      id: UNASSIGNED_DEPT_ID,
      name: '未分配部门',
      children: []
    }))
  }
  return departmentChildren
})

const filteredRoots = computed(() => {
  const term = keyword.value.trim().toLowerCase()
  if (!term) return treeRoot.value
  return treeRoot.value.map(node => filterNode(node, term)).filter(Boolean)
})

const visibleRows = computed(() => {
  const rows = []
  for (const node of filteredRoots.value) {
    flattenVisible(node, 0, rows)
  }
  return rows.map(row => {
    const userIds = row.node.type === 'dept' ? row.node.userIds : [Number(row.node.user.id)]
    const checkedCount = userIds.filter(id => selectedSet.value.has(Number(id))).length
    return {
      ...row,
      checked: userIds.length > 0 && checkedCount === userIds.length,
      indeterminate: checkedCount > 0 && checkedCount < userIds.length
    }
  })
})

const selectedUsers = computed(() => {
  const map = new Map(users.value.map(user => [Number(user.id), user]))
  return selectedIds.value.map(id => map.get(Number(id))).filter(Boolean)
})

const visibleSelectedUsers = computed(() => selectedUsers.value.slice(0, visibleTagLimit.value))
const hiddenSelectedUsers = computed(() => selectedUsers.value.slice(visibleTagLimit.value))

const mapDeptNodes = nodes => (nodes || []).map(node => buildDeptNode(node))

const buildDeptNode = node => {
  const deptId = String(node.id)
  const childDepartments = mapDeptNodes(node.children || [])
  const childUsers = (usersByDept.value.get(deptId) || []).map(user => ({
    type: 'user',
    key: `user:${user.id}`,
    label: user.realName || user.username || '未命名用户',
    user,
    children: [],
    userIds: [Number(user.id)]
  }))
  const deptNode = {
    type: 'dept',
    key: `dept:${deptId}`,
    deptId,
    label: node.name || '未命名部门',
    children: [...childDepartments, ...childUsers]
  }
  deptNode.userIds = collectUserIds(deptNode)
  return deptNode
}

const collectUserIds = node => {
  if (!node) return []
  if (node.type === 'user') return [Number(node.user.id)]
  return (node.children || []).flatMap(child => collectUserIds(child))
}

const filterNode = (node, term) => {
  if (!node) return null
  if (node.type === 'user') {
    const haystack = `${node.user.realName || ''}${node.user.username || ''}${node.user.deptName || ''}`.toLowerCase()
    return haystack.includes(term) ? node : null
  }
  const selfMatch = String(node.label || '').toLowerCase().includes(term)
  if (selfMatch) return node
  const children = (node.children || []).map(child => filterNode(child, term)).filter(Boolean)
  if (!children.length) return null
  const next = { ...node, children }
  next.userIds = collectUserIds(next)
  return next
}

const flattenVisible = (node, level, rows) => {
  if (!node) return
  rows.push({ node, level })
  const forceExpand = Boolean(keyword.value.trim())
  if (node.type !== 'dept') return
  if (!forceExpand && !expandedKeys.value.has(node.key)) return
  for (const child of node.children || []) {
    flattenVisible(child, level + 1, rows)
  }
}

const isExpanded = key => expandedKeys.value.has(key)

const toggleExpand = key => {
  const next = new Set(expandedKeys.value)
  if (next.has(key)) next.delete(key)
  else next.add(key)
  expandedKeys.value = next
}

const toggleNode = (node, checked) => {
  const next = new Set(selectedIds.value)
  const ids = node.type === 'dept' ? node.userIds : [Number(node.user.id)]
  for (const id of ids) {
    if (checked) next.add(Number(id))
    else next.delete(Number(id))
  }
  emit('update:modelValue', Array.from(next))
  nextTick(updateVisibleTagLimit)
}

const removeUser = id => {
  const next = selectedIds.value.filter(value => Number(value) !== Number(id))
  emit('update:modelValue', next)
  nextTick(updateVisibleTagLimit)
}

const clearSelected = () => {
  emit('update:modelValue', [])
  nextTick(updateVisibleTagLimit)
}

const toggleDropdown = () => {
  open.value = !open.value
  if (open.value) nextTick(updateVisibleTagLimit)
}

const handleDocumentClick = event => {
  if (!pickerRef.value?.contains(event.target)) {
    open.value = false
  }
}

const updateVisibleTagLimit = () => {
  const width = tagsRef.value?.clientWidth || 280
  visibleTagLimit.value = Math.max(1, Math.floor((width - 72) / 128))
}

const fetchDeptTree = async () => {
  try {
    const res = await deptApi.tree()
    deptTree.value = res.code === 200 ? (res.data || []) : []
    expandedKeys.value = new Set((deptTree.value || []).map(node => `dept:${node.id}`))
  } catch {
    deptTree.value = []
  }
}

const fetchUsers = async () => {
  try {
    const res = await userApi.systemList({ page: 1, size: 1000, status: 1 })
    const records = res.code === 200 ? (res.data?.records || []) : []
    users.value = records.filter(user => Number(user.status) === 1)
  } catch {
    users.value = []
  }
}

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
  if (window.ResizeObserver && tagsRef.value) {
    resizeObserver = new ResizeObserver(updateVisibleTagLimit)
    resizeObserver.observe(tagsRef.value)
  }
  fetchDeptTree()
  fetchUsers()
  nextTick(updateVisibleTagLimit)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  resizeObserver?.disconnect()
})
</script>

<style scoped>
.message-recipient-picker {
  position: relative;
  width: 100%;
}

.message-recipient-picker__label {
  align-items: center;
  gap: 6px;
}

.message-recipient-picker__count {
  margin-left: auto;
  color: var(--ds-color-text-secondary);
  font-weight: 400;
}

.message-recipient-picker__control {
  width: 100%;
  min-height: 36px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 10px;
  border: 1px solid var(--ds-color-border);
  border-radius: 8px;
  background: var(--ds-color-bg-card);
  color: var(--ds-color-text-primary);
  cursor: pointer;
  text-align: left;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.message-recipient-picker__control:hover,
.message-recipient-picker__control.is-open {
  border-color: var(--ds-color-primary);
}

.message-recipient-picker__control.is-open {
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ds-color-primary) 10%, transparent);
}

.message-recipient-picker__control.arco-select-view {
  box-sizing: border-box;
  font-family: inherit;
}

.message-recipient-picker__tags {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  overflow: hidden;
  white-space: nowrap;
}

.message-recipient-picker__placeholder {
  color: var(--ds-color-text-placeholder);
  font-size: 13px;
}

.message-recipient-picker__tag {
  max-width: 180px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0 9px;
  border: 1px solid color-mix(in srgb, var(--ds-color-primary) 20%, var(--ds-color-bg-card));
  border-radius: 999px;
  background: var(--ds-color-bg-card);
  color: var(--ds-color-primary);
  font-size: 12px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
}

.message-recipient-picker__tag-close {
  color: var(--ds-color-text-secondary);
  line-height: 1;
}

.message-recipient-picker__tag-close:hover {
  color: var(--ds-color-danger);
}

.message-recipient-picker__tag--more {
  flex: 0 0 auto;
  background: var(--ds-color-bg-card);
  color: var(--ds-color-primary);
  border-color: color-mix(in srgb, var(--ds-color-primary) 20%, var(--ds-color-bg-card));
  font-weight: 600;
}

.message-recipient-picker__arrow {
  flex: 0 0 auto;
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--ds-color-text-secondary);
  transition: transform 0.16s ease;
}

.message-recipient-picker__arrow svg {
  width: 14px;
  height: 14px;
}

.message-recipient-picker__control.is-open .message-recipient-picker__arrow {
  transform: rotate(180deg);
}

.message-recipient-picker__dropdown {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(100% + 4px);
  z-index: 1200;
  min-width: min(520px, calc(100vw - 48px));
  border: 1px solid var(--ds-color-border);
  border-radius: 10px;
  background: var(--ds-color-bg-card);
  box-shadow: 0 8px 22px var(--ds-color-shadow-strong);
  overflow: hidden;
}

.message-recipient-picker__search {
  padding: 8px;
  border-bottom: 1px solid var(--ds-color-border);
}

.message-recipient-picker__search :deep(.ds-keyword-search),
.message-recipient-picker__search :deep(.arco-input-wrapper) {
  height: 32px;
}

.message-recipient-picker__summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 7px 10px;
  border-bottom: 1px solid var(--ds-color-border);
  background: var(--ds-color-bg-card);
  color: var(--ds-color-text-secondary);
  font-size: 12px;
}

.message-recipient-picker__summary button {
  border: 0;
  background: transparent;
  color: var(--ds-color-primary);
  cursor: pointer;
}

.message-recipient-picker__summary button:disabled {
  color: var(--ds-color-text-placeholder);
  cursor: not-allowed;
}

.message-recipient-picker__tree {
  max-height: 380px;
  overflow: auto;
  padding: 4px 0;
}

.message-recipient-picker__node {
  min-height: 36px;
  display: flex;
  align-items: center;
  gap: 7px;
  padding-top: 3px;
  padding-bottom: 3px;
  padding-right: 10px;
  color: var(--ds-color-text-primary);
  white-space: nowrap;
}

.message-recipient-picker__node:hover,
.message-recipient-picker__node.selected {
  background: var(--ds-color-bg-selected);
}

.message-recipient-picker__expand,
.message-recipient-picker__expand-placeholder {
  width: 18px;
  height: 22px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.message-recipient-picker__expand {
  border: 0;
  background: transparent;
  color: var(--ds-color-text-secondary);
  cursor: pointer;
}

.message-recipient-picker__dept-icon {
  width: 22px;
  height: 22px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: color-mix(in srgb, var(--ds-color-primary) 10%, var(--ds-color-bg-card));
  color: var(--ds-color-primary);
  font-size: 12px;
}

.message-recipient-picker__dept-icon :deep(svg) {
  width: 14px;
  height: 14px;
}

.message-recipient-picker__dept-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--ds-color-text-primary);
  font-size: 13px;
  font-weight: 500;
}

.message-recipient-picker__dept-count {
  margin-left: auto;
  flex: 0 0 auto;
  color: var(--ds-color-text-secondary);
  font-size: 12px;
}

.message-recipient-picker :deep(.arco-checkbox) {
  flex: 0 0 auto;
}

@media (max-width: 760px) {
  .message-recipient-picker__dropdown {
    min-width: 100%;
  }
}
</style>
