<template>
  <div class="role-page">
    <div class="main">
      <DsPageHeader
        :breadcrumb="['工作台', '系统管理', '角色权限']"
        title="角色权限"
        description="维护角色基础信息，并统一配置功能权限、数据范围和字段权限。"
      >
        <template #icon>
          <IconSafe />
        </template>
        <template #actions>
          <DsHeaderActions
            :tool-items="headerToolItems"
            :show-primary="canCreateRole"
            primary-label="新增角色"
            @tool-select="handleHeaderToolSelect"
            @primary-click="openAddModal"
          >
            <template #primaryIcon>
              <IconPlus />
            </template>
          </DsHeaderActions>
        </template>
        <template #meta>
          <span>{{ roles.length }} 个角色</span>
          <span>{{ enabledRoleCount }} 个启用</span>
          <span>{{ builtInRoleCount }} 个内置角色</span>
        </template>
      </DsPageHeader>

      <section class="summary-grid">
        <DsStatsCard label="角色总数" :value="roles.length" hint="当前平台已配置角色" tone="primary">
          <template #icon><IconApps /></template>
        </DsStatsCard>
        <DsStatsCard label="启用角色" :value="enabledRoleCount" hint="可参与权限分配" tone="success">
          <template #icon><IconCheckCircle /></template>
        </DsStatsCard>
        <DsStatsCard label="内置角色" :value="builtInRoleCount" hint="受系统保护，不建议删除" tone="warning">
          <template #icon><IconLock /></template>
        </DsStatsCard>
        <DsStatsCard label="当前选中" :value="selectedRole ? (selectedRole.roleName || '-') : '未选择'" :hint="selectedRoleHint" tone="primary">
          <template #icon><IconUserGroup /></template>
        </DsStatsCard>
      </section>

      <DsFilterBar title="筛选查询" description="按角色名称、编码和状态快速定位需要维护的角色。">
        <template #title>
          <span class="filter-title">
            <span class="filter-title__icon"><IconFilter /></span>
            <span>筛选查询</span>
          </span>
        </template>
        <DsKeywordSearch
          v-model="roleFilters.keyword"
          class="filter-item filter-item--keyword"
          label="关键词"
          placeholder="搜索角色名称 / 编码 / 说明"
          @search="handleRoleSearch"
        />
        <DsStatusSelect
          v-model="roleFilters.status"
          class="filter-item filter-item--status"
          label="状态"
          placeholder="全部状态"
        />
        <template #actions>
          <div class="filter-actions">
            <a-button type="primary" class="action-button" @click="handleRoleSearch">查询</a-button>
            <a-button class="action-button" @click="handleRoleReset">重置</a-button>
          </div>
        </template>
      </DsFilterBar>

      <section class="role-layout">
        <section class="role-card card-shell">
          <div class="section-header">
            <div class="section-copy">
              <h2 class="section-title">角色列表</h2>
              <p class="section-description">共 {{ filteredRoles.length }} 条，点击左侧角色切换右侧权限配置。</p>
            </div>
            <DsActionBar
              class="role-card-tools"
              :selection-text="selectedRole ? `当前角色：${selectedRole.roleName}` : '未选择角色'"
              :selection-active="Boolean(selectedRole)"
            >
              <template #batch>
                <span class="role-card-tools__hint">支持查看角色状态、用户数和权限摘要。</span>
              </template>
              <template #tools>
                <DsIconButton tooltip="刷新角色列表" @click="refreshRoles"><IconRefresh /></DsIconButton>
              </template>
            </DsActionBar>
          </div>

          <div v-if="roleLoading" class="role-list-loading">角色加载中...</div>
          <div v-else-if="filteredRoles.length" class="role-list">
            <div
              v-for="record in filteredRoles"
              :key="record.id"
              role="button"
              tabindex="0"
              class="role-list-item"
              :class="{ 'role-list-item--active': record.id === selectedRoleId }"
              @click="selectRole(record)"
              @keydown.enter.prevent="selectRole(record)"
            >
              <span class="role-list-item__main">
                <span class="role-list-item__title-row">
                  <span class="role-list-item__name">{{ record.roleName }}</span>
                </span>
                <span class="role-list-item__meta">
                  <span class="role-list-item__code">{{ record.roleCode || '-' }}</span>
                  <span class="role-list-item__badges">
                    <DsTag v-if="isBuiltInRole(record)" class="role-mini-tag" label="内置" />
                    <DsTag class="role-mini-tag role-mini-tag--count" :label="`${record.userCount || 0} 人`" />
                  </span>
                </span>
              </span>
              <span class="role-list-item__side">
                <DsStatusTag :value="record.status" :options="statusOptions" />
                <span class="role-list-item__actions">
                  <a-button v-if="canUpdateRole" type="text" size="mini" @click.stop="openEditModal(record)">编辑</a-button>
                  <a-button v-if="canDeleteRole" type="text" size="mini" status="danger" @click.stop="deleteRole(record)">删除</a-button>
                </span>
              </span>
            </div>
          </div>
          <DsEmptyState v-else title="暂无角色数据" description="当前筛选条件下没有可展示的角色。" />
        </section>

        <section class="permission-card card-shell">
          <div class="section-header section-header--permission">
            <div class="section-copy permission-copy">
              <h2 class="section-title">{{ selectedRole ? `正在配置：${selectedRole.roleName}` : '权限配置' }}</h2>
              <p v-if="!selectedRole" class="section-description">
                {{ selectedRole ? '统一维护该角色的功能权限、数据范围和字段权限。' : '请先从左侧选择一个角色，再查看或编辑权限配置。' }}
              </p>
              <div v-if="selectedRole" class="role-meta">
                <span class="role-meta__item">角色编码：<span class="role-meta__value">{{ selectedRole.roleCode || '-' }}</span></span>
                <span class="role-meta__item role-meta__item--status">状态：<DsStatusTag :value="selectedRole.status" :options="statusOptions" /></span>
                <span class="role-meta__item">分配用户：<span class="role-meta__value">{{ selectedRole.userCount || 0 }} 人</span></span>
              </div>
            </div>
            <div v-if="selectedRole" class="permission-tools">
              <div class="save-tip" :class="{ 'save-tip--readonly': !canSavePermission }">
                {{ canSavePermission ? savePermissionTip : readonlyPermissionTip }}
              </div>
              <div class="permission-tools__actions">
                <a-button size="small" @click="fetchRolePermissions">刷新</a-button>
                <a-button v-if="canSavePermission" type="primary" size="small" :loading="saving" @click="saveAllPermissions">保存权限</a-button>
              </div>
            </div>
          </div>

          <DsEmptyState
            v-if="!selectedRole"
            class="permission-empty"
            title="请选择角色"
            description="从左侧角色列表选择一个角色后，这里会展示对应的权限配置。"
          />

          <a-tabs v-else v-model:active-key="activeTab" class="permission-tabs">
            <a-tab-pane key="feature" title="功能权限">
              <div class="permission-help">决定该角色能进入哪些页面、看到哪些菜单、使用哪些按钮或操作。</div>
              <div v-if="!canSavePermission" class="readonly-tip">当前角色仅可查看权限配置，无法保存修改。</div>
              <div class="feature-groups">
                <div v-for="group in catalog.features" :key="group.moduleCode" class="permission-group">
                  <div class="group-header">
                    <div class="group-copy">
                      <div class="group-title">{{ group.moduleName }}</div>
                      <div class="group-description">{{ moduleDescription(group.moduleCode, group.moduleName) }}</div>
                    </div>
                    <a-space v-if="canSavePermission" size="mini">
                      <a-button type="text" size="mini" @click="selectFeatureGroup(group)">全选</a-button>
                      <a-button type="text" size="mini" @click="clearFeatureGroup(group)">清空</a-button>
                    </a-space>
                  </div>
                  <a-checkbox-group v-model="featureCodes">
                    <div class="feature-check-grid">
                      <a-checkbox
                        v-for="item in group.children"
                        :key="item.code"
                        :value="item.code"
                        :disabled="!canSavePermission"
                      >
                        {{ item.name }}
                      </a-checkbox>
                    </div>
                  </a-checkbox-group>
                </div>
              </div>
            </a-tab-pane>

            <a-tab-pane key="scope" title="数据范围">
              <div class="permission-help">决定该角色在相关模块中能查看哪些数据，例如全部、本部门、本人等范围。</div>
              <div v-if="!canSavePermission" class="readonly-tip">当前角色仅可查看数据范围配置，无法保存修改。</div>
              <DsDataTable
                :columns="scopeColumns"
                :data="dataScopes"
                :show-pagination="false"
                row-key="moduleCode"
              >
                <template #scopeRule="{ record }">
                  <a-select v-model="record.scopeRule" size="small" class="scope-select" :disabled="!canSavePermission">
                    <a-option v-for="rule in catalog.scopeRules" :key="rule.value" :value="rule.value">{{ rule.label }}</a-option>
                  </a-select>
                </template>
                <template #empty>
                  <DsEmptyState title="暂无数据范围配置" description="当前角色还没有可配置的数据范围项。" />
                </template>
              </DsDataTable>
            </a-tab-pane>

            <a-tab-pane key="field" title="字段权限">
              <div class="permission-help">决定该角色在相关模块中能查看或编辑哪些字段。</div>
              <div v-if="!canSavePermission" class="readonly-tip">当前角色仅可查看字段权限配置，无法保存修改。</div>
              <div v-if="fieldRows.length" class="field-groups">
                <div v-for="group in groupedFieldRows" :key="group.moduleCode" class="field-group">
                  <div class="group-copy">
                    <div class="group-title">{{ group.moduleName }}</div>
                    <div class="group-description">{{ moduleDescription(group.moduleCode, group.moduleName) }}</div>
                  </div>
                  <div class="field-list">
                    <div v-for="record in group.fields" :key="record.key" class="field-row">
                      <div class="field-name-wrap">
                        <span class="field-name">{{ record.fieldName }}</span>
                        <DsStatusTag v-if="record.canRead && !record.canWrite" label="只读" tone="neutral" />
                      </div>
                      <div class="field-permission-actions">
                        <label class="field-check">
                          <span class="field-check-label">可读</span>
                          <a-checkbox
                            :model-value="record.canRead"
                            :disabled="!canSavePermission"
                            @change="checked => handleFieldReadChange(record, checked)"
                          />
                        </label>
                        <label class="field-check">
                          <span class="field-check-label">可写</span>
                          <a-checkbox
                            v-if="record.writable"
                            :model-value="record.canWrite"
                            :disabled="!canSavePermission"
                            @change="checked => handleFieldWriteChange(record, checked)"
                          />
                          <DsStatusTag v-else label="只读" tone="neutral" />
                        </label>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <DsEmptyState v-else title="暂无字段权限配置" description="当前角色还没有字段权限项。" />
              <div class="field-permission-tip">{{ fieldPermissionTip }}</div>
            </a-tab-pane>
          </a-tabs>
        </section>
      </section>
    </div>

    <DsModalForm
      v-model:visible="roleModalVisible"
      :title="isEdit ? '编辑角色' : '新增角色'"
      :description="isEdit ? '维护角色基础信息和启用状态。' : '创建新的角色并为后续权限配置做好准备。'"
      :width="760"
      @cancel="closeRoleModal"
    >
      <DsFormSection title="基础信息" description="角色编码作为系统内唯一标识，创建后不建议频繁调整。">
        <DsFormGrid>
          <DsReadonlyField
            v-if="isEdit"
            label="角色编码"
            :value="roleForm.roleCode"
            hint="角色编码创建后保持稳定"
          />
          <DsInput
            v-else
            v-model="roleForm.roleCode"
            label="角色编码"
            placeholder="如 finance_manager"
            required
          />
          <DsInput
            v-model="roleForm.roleName"
            label="角色名称"
            placeholder="请输入角色名称"
            required
          />
        </DsFormGrid>
      </DsFormSection>

      <DsFormSection title="角色说明" description="说明会出现在角色选择和权限维护场景中，建议描述职责边界。">
        <DsFormGrid :columns="1">
          <DsTextarea
            v-model="roleForm.description"
            label="说明"
            placeholder="请输入角色说明"
            :auto-size="{ minRows: 3, maxRows: 5 }"
          />
        </DsFormGrid>
      </DsFormSection>

      <DsFormSection title="账号状态">
        <DsFormGrid>
          <DsStatusSelect
            v-model="roleForm.status"
            label="状态"
            placeholder="请选择状态"
            :include-all="false"
            :allow-clear="false"
          />
        </DsFormGrid>
      </DsFormSection>

      <template #footer>
        <DsFormActions :loading="roleSubmitting" @cancel="closeRoleModal" @submit="submitRoleForm" />
      </template>
    </DsModalForm>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconApps,
  IconCheckCircle,
  IconFilter,
  IconLock,
  IconPlus,
  IconRefresh,
  IconSafe,
  IconUserGroup
} from '@arco-design/web-vue/es/icon'
import { roleApi } from '@/api'
import {
  DsActionBar,
  DsDataTable,
  DsEmptyState,
  DsFilterBar,
  DsFormActions,
  DsFormGrid,
  DsFormSection,
  DsHeaderActions,
  DsIconButton,
  DsInput,
  DsKeywordSearch,
  DsModalForm,
  DsPageHeader,
  DsReadonlyField,
  DsStatsCard,
  DsStatusSelect,
  DsStatusTag,
  DsTag,
  DsTextarea
} from '@/design-system'
import { hasPermission } from '@/utils/permission'
import { loadCommonStatusOptions } from '@/utils/dict'

const roles = ref([])
const selectedRole = ref(null)
const roleLoading = ref(false)
const saving = ref(false)
const roleSubmitting = ref(false)
const activeTab = ref('feature')
const roleModalVisible = ref(false)
const statusOptions = ref([])
const isEdit = ref(false)
const featureCodes = ref([])
const dataScopes = ref([])
const fieldRows = ref([])
const catalog = reactive({ features: [], dataScopes: [], fields: [], scopeRules: [] })
const roleFilters = reactive({ keyword: '', status: '' })
const appliedRoleFilters = reactive({ keyword: '', status: '' })
const roleForm = reactive({ id: null, roleCode: '', roleName: '', description: '', status: 1 })
const originalRoleStatus = ref(1)

const scopeColumns = [
  { title: '模块', dataIndex: 'moduleName', width: 200 },
  { title: '数据范围', slotName: 'scopeRule', width: 240 }
]

const moduleDescriptions = {
  system_department: '管理系统部门信息与组织架构',
  department: '管理系统部门信息与组织架构',
  system_user: '管理系统用户账号与相关信息',
  user: '管理系统用户账号与相关信息',
  system_role: '管理角色信息与权限分配',
  role: '管理角色信息与权限分配',
  system_log: '查看系统操作与安全审计记录',
  log: '查看系统操作与安全审计记录',
  system_config: '维护系统基础配置与运行参数',
  config: '维护系统基础配置与运行参数',
  system_dict: '维护系统字典类型和字典项',
  dict: '维护系统字典类型和字典项',
  system_file: '管理文件中心的查看与操作能力',
  file: '管理文件中心的查看与操作能力'
}

const selectedRoleId = computed(() => selectedRole.value?.id)
const canCreateRole = computed(() => hasPermission('system_role:create'))
const canUpdateRole = computed(() => hasPermission('system_role:update'))
const canDeleteRole = computed(() => hasPermission('system_role:delete'))
const canExportRole = computed(() => hasPermission('system_role:export'))
const canSavePermission = computed(() => hasPermission('system_role:permission'))
const savePermissionTip = computed(() => '保存将提交当前角色的功能权限、数据范围和字段权限配置。')
const readonlyPermissionTip = computed(() => '当前角色仅可查看权限配置，无法保存修改。')
const fieldPermissionTip = computed(() => canSavePermission.value ? '勾选可写会自动包含可读；只读字段不支持配置可写。' : '只读字段不支持配置可写。')
const enabledRoleCount = computed(() => roles.value.filter(role => Number(role.status) === 1).length)
const builtInRoleCount = computed(() => roles.value.filter(role => role.builtIn === 1).length)
const selectedRoleHint = computed(() => {
  if (!selectedRole.value) return '请选择左侧角色'
  return `分配用户 ${selectedRole.value.userCount || 0} 人`
})
const headerToolItems = computed(() => ([
  { key: 'export', label: '导出角色', visible: canExportRole.value }
]))
const filteredRoles = computed(() => {
  const keyword = appliedRoleFilters.keyword.trim().toLowerCase()
  const status = appliedRoleFilters.status
  return roles.value.filter(role => {
    if (status !== '' && String(role.status) !== String(status)) return false
    if (!keyword) return true
    const name = String(role.roleName || '').toLowerCase()
    const code = String(role.roleCode || '').toLowerCase()
    const desc = String(role.description || '').toLowerCase()
    return name.includes(keyword) || code.includes(keyword) || desc.includes(keyword)
  })
})
const groupedFieldRows = computed(() => {
  const groups = []
  const groupMap = new Map()
  for (const row of fieldRows.value) {
    if (!groupMap.has(row.moduleCode)) {
      const group = {
        moduleCode: row.moduleCode,
        moduleName: row.moduleName,
        fields: []
      }
      groupMap.set(row.moduleCode, group)
      groups.push(group)
    }
    groupMap.get(row.moduleCode).fields.push(row)
  }
  return groups
})

const fetchCatalog = async () => {
  const res = await roleApi.permissionCatalog()
  if (res.code === 200) {
    catalog.features = res.data?.features || []
    catalog.dataScopes = res.data?.dataScopes || []
    catalog.fields = res.data?.fields || []
    catalog.scopeRules = res.data?.scopeRules || []
  }
}

const exportRoles = async () => {
  try {
    const blob = await roleApi.export()
    const objectUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = `角色列表_${new Date().toISOString().slice(0, 10).replace(/-/g, '')}.xlsx`
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(objectUrl)
    Message.success('角色列表已开始导出')
  } catch {
    Message.error('导出失败')
  }
}

const fetchRoles = async () => {
  roleLoading.value = true
  try {
    const res = await roleApi.list()
    if (res.code === 200) {
      roles.value = res.data || []
      if (!selectedRole.value && roles.value.length > 0) {
        selectedRole.value = roles.value[0]
        await fetchRolePermissions()
      } else if (selectedRole.value) {
        selectedRole.value = roles.value.find(r => r.id === selectedRole.value.id) || null
        if (selectedRole.value) {
          await fetchRolePermissions()
        }
      }
    } else {
      Message.error(res.msg || '加载角色失败')
    }
  } catch {
    Message.error('加载角色失败')
  } finally {
    roleLoading.value = false
  }
}

const refreshRoles = async () => {
  await fetchRoles()
}

const handleHeaderToolSelect = (key) => {
  if (key === 'export') exportRoles()
}

const handleRoleSearch = () => {
  appliedRoleFilters.keyword = roleFilters.keyword
  appliedRoleFilters.status = roleFilters.status
}

const handleRoleReset = () => {
  roleFilters.keyword = ''
  roleFilters.status = ''
  appliedRoleFilters.keyword = ''
  appliedRoleFilters.status = ''
}

const selectRole = async (record) => {
  selectedRole.value = record
  await fetchRolePermissions()
}

const moduleDescription = (moduleCode, moduleName) => {
  const key = String(moduleCode || '').toLowerCase()
  return moduleDescriptions[key] || `${moduleName || '模块'}相关权限配置`
}

const fetchRolePermissions = async () => {
  if (!selectedRoleId.value) return
  try {
    const res = await roleApi.permissions(selectedRoleId.value)
    if (res.code !== 200) {
      Message.error(res.msg || '加载权限失败')
      return
    }
    featureCodes.value = res.data?.features || []
    buildDataScopes(res.data?.dataScopes || [])
    buildFieldRows(res.data?.fields || [])
  } catch {
    Message.error('加载权限失败')
  }
}

const buildDataScopes = (savedScopes) => {
  const saved = new Map(savedScopes.map(item => [item.moduleCode, item.scopeRule]))
  dataScopes.value = catalog.dataScopes.map(item => ({
    moduleCode: item.moduleCode,
    moduleName: item.moduleName,
    scopeRule: saved.get(item.moduleCode) || 'DEPT'
  }))
}

const buildFieldRows = (savedFields) => {
  const saved = new Map(savedFields.map(item => [`${item.moduleCode}:${item.fieldCode}`, item]))
  const rows = []
  for (const group of catalog.fields) {
    for (const field of group.fields || []) {
      const key = `${group.moduleCode}:${field.fieldCode}`
      const item = saved.get(key)
      rows.push({
        key,
        moduleCode: group.moduleCode,
        moduleName: group.moduleName,
        fieldCode: field.fieldCode,
        fieldName: field.fieldName,
        writable: field.writable !== false,
        canRead: item ? item.canRead === 1 : true,
        canWrite: field.writable !== false && item ? item.canWrite === 1 : false
      })
    }
  }
  fieldRows.value = rows
}

const handleFieldReadChange = (record, checked) => {
  record.canRead = checked
  if (!checked) record.canWrite = false
}

const handleFieldWriteChange = (record, checked) => {
  if (!record.writable) {
    record.canWrite = false
    return
  }
  record.canWrite = checked
  if (checked) record.canRead = true
}

const normalizeFieldRows = () => {
  let adjusted = 0
  for (const row of fieldRows.value) {
    if (!row.writable && row.canWrite) {
      row.canWrite = false
      adjusted += 1
    }
    if (row.canWrite && !row.canRead) {
      row.canRead = true
      adjusted += 1
    }
  }
  return adjusted
}

const selectFeatureGroup = (group) => {
  const current = new Set(featureCodes.value)
  for (const item of group.children || []) current.add(item.code)
  featureCodes.value = Array.from(current)
}

const clearFeatureGroup = (group) => {
  const groupCodes = new Set((group.children || []).map(item => item.code))
  featureCodes.value = featureCodes.value.filter(code => !groupCodes.has(code))
}

const saveAllPermissions = async () => {
  if (!selectedRoleId.value) return
  const adjustedFields = normalizeFieldRows()
  if (adjustedFields > 0) {
    Message.info('已自动补齐可写字段的可读权限')
  }
  saving.value = true
  try {
    const fields = fieldRows.value.map(row => ({
      moduleCode: row.moduleCode,
      fieldCode: row.fieldCode,
      canRead: row.canRead ? 1 : 0,
      canWrite: row.canWrite ? 1 : 0
    }))
    const featureRes = await roleApi.updateFeatures(selectedRoleId.value, featureCodes.value)
    if (featureRes.code !== 200) throw new Error(featureRes.msg || '保存功能权限失败')
    const scopeRes = await roleApi.updateDataScopes(
      selectedRoleId.value,
      dataScopes.value.map(({ moduleCode, scopeRule }) => ({ moduleCode, scopeRule }))
    )
    if (scopeRes.code !== 200) throw new Error(scopeRes.msg || '保存数据范围失败')
    const fieldRes = await roleApi.updateFields(selectedRoleId.value, fields)
    if (fieldRes.code !== 200) throw new Error(fieldRes.msg || '保存字段权限失败')
    Message.success('权限已保存')
    await fetchRoles()
    notifyPermissionsUpdated()
  } catch (e) {
    Message.error(e.message || '保存权限失败')
  } finally {
    saving.value = false
  }
}

const openAddModal = () => {
  isEdit.value = false
  Object.assign(roleForm, { id: null, roleCode: '', roleName: '', description: '', status: 1 })
  roleModalVisible.value = true
}

const openEditModal = (record) => {
  isEdit.value = true
  originalRoleStatus.value = Number(record.status) === 0 ? 0 : 1
  Object.assign(roleForm, {
    id: record.id,
    roleCode: record.roleCode || '',
    roleName: record.roleName || '',
    description: record.description || '',
    status: Number(record.status) === 0 ? 0 : 1
  })
  roleModalVisible.value = true
}

const closeRoleModal = () => {
  if (roleSubmitting.value) return
  roleModalVisible.value = false
}

const submitRoleForm = async () => {
  if (roleSubmitting.value) return
  roleSubmitting.value = true
  try {
    await saveRoleForm()
  } finally {
    roleSubmitting.value = false
  }
}

const saveRoleForm = async () => {
  if (!String(roleForm.roleCode).trim()) {
    Message.error('角色编码不能为空')
    return
  }
  if (!String(roleForm.roleName).trim()) {
    Message.error('角色名称不能为空')
    return
  }
  try {
    if (isEdit.value && originalRoleStatus.value !== 0 && Number(roleForm.status) === 0) {
      const confirmed = await confirmRoleImpact('disable', { ...roleForm })
      if (!confirmed) return
    }
    const payload = {
      roleCode: String(roleForm.roleCode).trim(),
      roleName: String(roleForm.roleName).trim(),
      description: String(roleForm.description || '').trim(),
      status: Number(roleForm.status) === 0 ? 0 : 1
    }
    const res = isEdit.value ? await roleApi.update(roleForm.id, payload) : await roleApi.add(payload)
    if (res.code !== 200) {
      Message.error(res.msg || '操作失败')
      return
    }
    Message.success(isEdit.value ? '角色已更新' : '角色已新增')
    roleModalVisible.value = false
    await fetchRoles()
    notifyPermissionsUpdated()
  } catch (e) {
    Message.error(e.message || '操作失败')
  }
}

const deleteRole = async (record) => {
  const confirmed = await confirmRoleImpact('delete', record)
  if (!confirmed) return
  const res = await roleApi.delete(record.id)
  if (res.code === 200) {
    Message.success('角色已删除')
    if (selectedRole.value?.id === record.id) selectedRole.value = null
    await fetchRoles()
    notifyPermissionsUpdated()
  } else {
    Message.error(res.msg || '删除失败')
  }
}

const confirmRoleImpact = async (type, record) => {
  const res = type === 'disable'
    ? await roleApi.disableImpact(record.id)
    : await roleApi.deleteImpact(record.id)
  if (res.code !== 200) {
    Message.error(res.msg || '影响检查失败')
    return false
  }
  const impact = res.data || {}
  const allowed = type === 'disable' ? impact.canDisable !== false : impact.canDelete !== false
  const title = type === 'disable' ? '确认禁用角色' : '确认删除角色'
  const warnings = impact.warnings || []
  if (!allowed) {
    Modal.warning({
      title: '无法操作',
      content: buildImpactContent(impact, warnings)
    })
    return false
  }
  return new Promise(resolve => {
    Modal.confirm({
      title,
      content: buildImpactContent(impact, warnings),
      okText: type === 'disable' ? '继续禁用' : '继续删除',
      okButtonProps: type === 'delete' ? { status: 'danger' } : undefined,
      onOk: () => resolve(true),
      onCancel: () => resolve(false)
    })
  })
}

const buildImpactContent = (impact, warnings) => {
  const lines = [
    `角色：${impact.roleName || '-'}（${impact.roleCode || '-'}）`,
    `分配用户：${impact.userCount || 0} 人，其中启用 ${impact.activeUserCount || 0} 人`,
    `权限配置：功能 ${impact.featureCount || 0} 项，数据范围 ${impact.dataScopeCount || 0} 项，字段 ${impact.fieldCount || 0} 项`
  ]
  if (impact.builtIn === 1) lines.push('该角色为内置角色，受系统保护。')
  for (const warning of warnings || []) lines.push(warning)
  return lines.join('\n')
}

const isBuiltInRole = (record) => record?.builtIn === 1

const notifyPermissionsUpdated = () => {
  window.dispatchEvent(new CustomEvent('user-permissions-updated'))
}

const loadStatusOptions = async () => {
  statusOptions.value = await loadCommonStatusOptions()
}

const refreshDictOptions = async () => {
  await loadStatusOptions()
}

onMounted(async () => {
  await loadStatusOptions()
  await fetchCatalog()
  await fetchRoles()
  window.addEventListener('system-dict-updated', refreshDictOptions)
})

onUnmounted(() => {
  window.removeEventListener('system-dict-updated', refreshDictOptions)
})
</script>

<style scoped>
.role-page {
  min-height: 100vh;
  background: linear-gradient(180deg, var(--ds-color-bg-layout-start) 0%, var(--ds-color-bg-layout-mid) 200px, var(--ds-color-bg-layout) 100%);
}

.main {
  max-width: 1680px;
  width: 100%;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.card-shell {
  background: var(--ds-color-bg-card);
  border: 1px solid color-mix(in srgb, var(--ds-color-primary) 4%, var(--ds-color-border));
  border-radius: 12px;
  box-shadow: 0 10px 28px var(--ds-color-shadow);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  align-items: stretch;
}

.filter-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.filter-title__icon {
  width: 18px;
  height: 18px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--ds-color-bg-selected);
  color: var(--ds-color-primary);
}

.filter-title__icon :deep(svg) {
  width: 14px;
  height: 14px;
}

.filter-item {
  min-width: 0;
  flex: 0 0 auto;
}

.filter-item--keyword {
  width: 280px;
}

.filter-item--status {
  width: 200px;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: flex-end;
}

.filter-actions :deep(.arco-btn) {
  min-width: 76px;
  height: 36px;
  border-radius: 8px;
}

.role-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 20px;
  min-height: calc(100vh - 360px);
}

.role-card,
.permission-card {
  min-width: 0;
  overflow: hidden;
}

.role-card {
  padding: 16px 16px 14px;
}

.permission-card {
  padding: 16px 20px 20px;
}

.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.role-card > .section-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 32px;
  align-items: flex-start;
  gap: 8px;
}

.role-card > .section-header .section-title {
  white-space: nowrap;
}

.role-card > .section-header .section-description {
  overflow-wrap: normal;
  word-break: normal;
  writing-mode: horizontal-tb;
}

.section-header--permission {
  align-items: flex-start;
  margin-bottom: 18px;
}

.section-copy {
  min-width: 0;
  flex: 1 1 auto;
}

.section-title {
  margin: 0;
  color: var(--ds-color-text-primary);
  font-size: 18px;
  line-height: 1.3;
  font-weight: 600;
}

.section-header--permission .section-title {
  max-width: 100%;
  overflow-wrap: anywhere;
}

.section-description {
  margin: 8px 0 0;
  color: var(--ds-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.role-card-tools {
  flex: 0 0 auto;
  min-width: 0;
  justify-self: end;
}

.role-card-tools :deep(.ds-action-bar) {
  min-height: 32px;
  width: auto;
  gap: 6px;
  padding: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
}

.role-card-tools :deep(.ds-action-bar__selection),
.role-card-tools :deep(.ds-action-bar__main),
.role-card-tools :deep(.ds-action-bar__divider) {
  display: none;
}

.role-card-tools__hint {
  display: none;
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.role-list {
  display: grid;
  gap: 6px;
}

.role-list-loading {
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ds-color-text-secondary);
  font-size: 13px;
}

.role-list-item {
  position: relative;
  width: 100%;
  min-height: 52px;
  padding: 8px 30px 8px 16px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.role-list-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 9px;
  bottom: 9px;
  width: 3px;
  border-radius: 999px;
  background: transparent;
}

.role-list-item:hover {
  background: var(--ds-color-bg-hover);
}

.role-list-item--active {
  border-color: color-mix(in srgb, var(--ds-color-primary) 20%, var(--ds-color-bg-card));
  background: color-mix(in srgb, var(--ds-color-primary) 8%, var(--ds-color-bg-card));
  box-shadow: 0 6px 16px var(--ds-color-shadow);
}

.role-list-item--active::before {
  background: var(--ds-color-primary);
}

.role-list-item__main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.role-list-item__title-row {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.role-list-item__name {
  min-width: 0;
  overflow: hidden;
  color: var(--ds-color-text-primary);
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-list-item__meta {
  display: flex;
  align-items: center;
  gap: 6px;
  overflow: hidden;
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  font-weight: 400;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-list-item__code {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-list-item__badges {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.role-mini-tag {
  max-width: 58px;
}

:deep(.role-mini-tag.ds-tag) {
  min-height: 21px;
  padding: 0 7px;
  border-color: var(--ds-color-border);
  background: var(--ds-color-bg-hover);
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  font-weight: 400;
}

:deep(.role-mini-tag--count.ds-tag) {
  border-color: var(--ds-color-border);
  background: var(--ds-color-bg-hover);
  color: var(--ds-color-text-secondary);
}

.role-list-item__side {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  justify-content: flex-end;
  gap: 4px;
  padding-right: 10px;
  align-self: start;
  padding-top: 1px;
}

.role-list-item__actions {
  position: absolute;
  right: 44px;
  bottom: 4px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  opacity: 0;
  pointer-events: none;
}

.role-list-item:hover .role-list-item__actions,
.role-list-item--active .role-list-item__actions {
  opacity: 1;
  pointer-events: auto;
}

.role-list-item__actions :deep(.arco-btn-text) {
  height: 20px;
  padding: 0 3px;
  border-radius: 6px;
  font-size: 12px;
}

.role-list-item__actions :deep(.arco-btn-status-danger) {
  color: color-mix(in srgb, var(--ds-color-danger) 72%, var(--ds-color-bg-card));
}

.role-meta {
  display: flex;
  align-items: center;
  gap: 8px 18px;
  margin-top: 8px;
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  flex-wrap: wrap;
  line-height: 24px;
}

.role-meta__item {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 280px;
  white-space: nowrap;
}

.role-meta__item--status {
  flex: 0 0 auto;
}

.role-meta__value {
  min-width: 0;
  overflow: hidden;
  color: var(--ds-color-text-regular);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.permission-copy {
  flex: 1 1 auto;
  max-width: calc(100% - 380px);
}

.permission-tools {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  flex: 0 0 360px;
  max-width: 360px;
  min-width: 280px;
}

.permission-tools__actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.permission-tools__actions :deep(.arco-btn) {
  height: 36px;
  min-width: 84px;
  border-radius: 8px;
}

.save-tip {
  max-width: 340px;
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 1.6;
  text-align: right;
}

.save-tip--readonly {
  color: var(--ds-color-tag-orange-text);
}

.permission-empty {
  padding: 92px 0 96px;
}

.permission-tabs {
  margin-top: 4px;
}

.permission-help {
  margin: 12px 0 0;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--ds-color-bg-soft);
  color: var(--ds-color-text-regular);
  font-size: 13px;
  line-height: 1.6;
}

.readonly-tip {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--ds-color-tag-orange-bg);
  color: var(--ds-color-tag-orange-text);
  font-size: 13px;
}

.feature-groups {
  margin-top: 20px;
  display: grid;
  gap: 22px;
}

.permission-group,
.field-group {
  padding: 18px 20px;
  border: 1px solid color-mix(in srgb, var(--ds-color-primary) 4%, var(--ds-color-border));
  border-radius: 10px;
  background: var(--ds-color-bg-card);
  box-shadow: 0 8px 20px var(--ds-color-shadow);
}

.group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.group-header :deep(.arco-space) {
  flex: 0 0 auto;
}

.group-header :deep(.arco-btn-text) {
  height: 28px;
  padding: 0 8px;
  border-radius: 7px;
  font-size: 12px;
}

.group-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.group-title {
  overflow: hidden;
  color: var(--ds-color-text-primary);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-description {
  color: var(--ds-color-text-secondary);
  font-size: 13px;
  font-weight: 400;
  line-height: 1.5;
}

.feature-check-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px 20px;
  align-items: center;
}

.feature-check-grid :deep(.arco-checkbox) {
  min-width: 0;
  min-height: 32px;
  width: 100%;
  margin: 0;
  padding: 5px 10px 5px 6px;
  border-radius: 8px;
  white-space: nowrap;
}

.feature-check-grid :deep(.arco-checkbox-icon) {
  flex: 0 0 auto;
}

.feature-check-grid :deep(.arco-checkbox:hover) {
  background: var(--ds-color-bg-hover);
}

.feature-check-grid :deep(.arco-checkbox-checked) {
  background: var(--ds-color-bg-selected);
}

.feature-check-grid :deep(.arco-checkbox-checked .arco-checkbox-label) {
  color: var(--ds-color-primary);
  font-weight: 500;
}

.feature-check-grid :deep(.arco-checkbox-label) {
  max-width: 100%;
  overflow: hidden;
  color: var(--ds-color-text-regular);
  font-size: 13px;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-groups {
  margin-top: 20px;
  display: grid;
  gap: 22px;
}

.field-list {
  margin-top: 14px;
}

.field-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 16px;
  align-items: center;
  min-height: 40px;
  padding: 10px 0;
  border-top: 1px solid var(--ds-color-bg-layout);
}

.field-row:first-child {
  border-top: none;
}

.field-name-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.field-name {
  color: var(--ds-color-text-primary);
  line-height: 20px;
}

.field-permission-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  align-items: center;
}

.field-check {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 32px;
  padding: 0 8px;
  border-radius: 8px;
  color: var(--ds-color-text-regular);
  font-size: 13px;
}

.field-check:hover {
  background: var(--ds-color-bg-hover);
}

.field-check:has(.arco-checkbox-checked) {
  background: var(--ds-color-bg-selected);
  color: var(--ds-color-primary);
  font-weight: 500;
}

.field-check-label {
  white-space: nowrap;
}

.scope-select {
  width: 180px;
}

.field-permission-tip {
  margin-top: 12px;
  color: var(--ds-color-text-secondary);
  font-size: 13px;
}

@media (max-width: 1440px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .feature-check-grid {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }
}

@media (max-width: 1180px) {
  .role-layout {
    grid-template-columns: 1fr;
  }

  .permission-copy {
    max-width: calc(100% - 340px);
  }

  .permission-tools {
    flex-basis: 320px;
    max-width: 320px;
    min-width: 260px;
  }

  .feature-check-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .main {
    padding: 16px;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .section-header,
  .section-header--permission {
    flex-direction: column;
  }

  .role-card-tools,
  .permission-tools {
    width: 100%;
    min-width: 0;
  }

  .permission-tools {
    align-items: flex-start;
  }

  .permission-copy,
  .permission-tools {
    max-width: 100%;
    width: 100%;
  }

  .permission-tools {
    flex-basis: auto;
  }

  .filter-item--keyword,
  .filter-item--status {
    width: 100%;
  }

  .feature-check-grid,
  .field-row,
  .field-permission-actions {
    grid-template-columns: 1fr;
  }

  .feature-check-grid :deep(.arco-checkbox),
  .feature-check-grid :deep(.arco-checkbox-label) {
    white-space: nowrap;
  }
}
</style>
