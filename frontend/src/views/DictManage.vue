<template>
  <div class="dict-page">
    <div class="main">
      <DsPageHeader
        :breadcrumb="['工作台', '系统管理', '字典管理']"
        title="字典管理"
        description="统一维护字典类型和字典项，为状态、分类、模块和动作等下拉选项提供基础数据。"
      >
        <template #icon><IconBook /></template>
        <template #actions>
          <DsHeaderActions
            :tool-items="headerToolItems"
            :show-primary="canCreateDict"
            primary-label="新增类型"
            @tool-select="handleHeaderToolSelect"
            @primary-click="openTypeModal()"
          >
            <template #primaryIcon><IconPlus /></template>
          </DsHeaderActions>
        </template>
        <template #meta>
          <span>{{ typeTotal }} 个字典类型</span>
          <span>{{ selectedType ? `${itemTotal} 个当前字典项` : '未选择字典类型' }}</span>
        </template>
      </DsPageHeader>

      <div class="dict-layout">
        <section class="type-panel">
          <div class="panel-header">
            <div>
              <h2 class="panel-title">字典类型</h2>
              <p class="panel-description">选择左侧类型后维护右侧字典项。</p>
            </div>
            <DsIconButton tooltip="刷新类型" @click="refreshTypes"><IconRefresh /></DsIconButton>
          </div>
          <div class="panel-filter panel-filter--type">
            <DsKeywordSearch v-model="typeFilters.keyword" label="" placeholder="搜索类型编码或名称" class="type-search" @search="handleTypeSearch" />
            <DsStatusSelect v-model="typeFilters.status" label="" placeholder="全部状态" class="status-select" @update:model-value="handleTypeSearch" />
            <a-button class="filter-button" type="primary" @click="handleTypeSearch">查询</a-button>
            <a-button class="filter-button" @click="resetTypeFilters">重置</a-button>
          </div>

          <DsDataTable
            :columns="typeColumns"
            :data="typeList"
            :loading="typeLoading"
            :pagination="typePaginationConfig"
            :row-class="typeRowClass"
            row-key="id"
            @row-click="handleTypeRowClick"
            @pagination-change="handleTypePageChange"
            @page-size-change="handleTypeSizeChange"
          >
            <template #index="{ rowIndex, record }">
              <button class="type-index-button" type="button" @click.stop="selectType(record)">{{ (typePage - 1) * typeSize + rowIndex + 1 }}</button>
            </template>
            <template #typeInfo="{ record }">
              <div class="type-info type-select-trigger" @click.stop="selectType(record)">
                <span class="type-name">{{ record.typeName }}</span>
                <span class="type-code">{{ record.typeCode }}</span>
                <span class="type-tags">
                  <DsStatusTag :label="statusLabel(statusOptions, record.status)" :tone="statusTone(record.status)" />
                  <DsStatusTag v-if="isBuiltInType(record)" label="内置" tone="warning" />
                </span>
              </div>
            </template>
            <template #actions="{ record }">
              <div class="row-actions">
                <a-button v-if="canUpdateDict" type="text" size="mini" @click.stop="openTypeModal(record)">编辑</a-button>
                <a-button
                  v-if="canUpdateDict && !isBuiltInType(record)"
                  type="text"
                  size="mini"
                  :status="record.status === 1 ? 'danger' : 'success'"
                  @click.stop="toggleTypeStatus(record)"
                >
                  {{ record.status === 1 ? '禁用' : '启用' }}
                </a-button>
                <span v-else-if="canUpdateDict && isBuiltInType(record)" class="no-actions">内置保护</span>
                <span v-if="!canUpdateDict" class="no-actions">无可用操作</span>
              </div>
            </template>
            <template #empty>
              <DsEmptyState title="暂无字典类型" description="当前筛选条件下没有可展示的字典类型。" />
            </template>
          </DsDataTable>
        </section>

        <section class="item-panel">
          <div class="panel-header panel-header--items">
            <div class="selected-title">
              <span>{{ selectedType ? selectedType.typeName : '请选择字典类型' }}</span>
              <span v-if="selectedType" class="type-code">{{ selectedType.typeCode }}</span>
            </div>
            <DsHeaderActions
              :tool-items="itemToolItems"
              :show-primary="canCreateDict && !!selectedType"
              primary-label="新增字典项"
              @tool-select="handleItemToolSelect"
              @primary-click="openItemModal()"
            >
              <template #primaryIcon><IconPlus /></template>
            </DsHeaderActions>
          </div>
          <div class="panel-filter panel-filter--item">
            <DsKeywordSearch v-model="itemFilters.keyword" label="" placeholder="搜索标签或值" class="item-search" :disabled="!selectedType" @search="handleItemSearch" />
            <DsStatusSelect v-model="itemFilters.status" label="" placeholder="全部状态" class="status-select" :disabled="!selectedType" @update:model-value="handleItemSearch" />
            <a-button class="filter-button" type="primary" :disabled="!selectedType" @click="handleItemSearch">查询</a-button>
            <a-button class="filter-button" :disabled="!selectedType" @click="resetItemFilters">重置</a-button>
            <DsIconButton tooltip="刷新字典项" :disabled="!selectedType" @click="refreshItems"><IconRefresh /></DsIconButton>
          </div>

          <DsEmptyState
            v-if="!selectedType"
            title="请选择字典类型"
            description="请选择左侧字典类型查看字典项。"
            class="empty-state"
          />
          <template v-else>
            <DsDataTable
              :columns="itemColumns"
              :data="itemList"
              :loading="itemLoading"
              :pagination="itemPaginationConfig"
              row-key="id"
              @pagination-change="handleItemPageChange"
              @page-size-change="handleItemSizeChange"
            >
              <template #index="{ rowIndex }">{{ (itemPage - 1) * itemSize + rowIndex + 1 }}</template>
              <template #itemInfo="{ record }">
                <div class="item-info-cell">
                  <span class="item-label">{{ record.itemLabel }}</span>
                  <span class="item-value">{{ record.itemValue }}</span>
                  <span class="item-meta">
                    <span>排序 {{ record.sortOrder }}</span>
                    <DsStatusTag v-if="record.isDefault === 1" label="默认" tone="warning" />
                    <DsStatusTag v-if="isBuiltInItem(record)" label="内置" tone="warning" />
                  </span>
                </div>
              </template>
              <template #tagColor="{ record }">
                <DsStatusTag :label="record.itemLabel || record.tagColor || '预览'" :tone="tagTone(record.tagColor)" />
              </template>
              <template #status="{ record }">
                <DsStatusTag :label="statusLabel(statusOptions, record.status)" :tone="statusTone(record.status)" />
              </template>
              <template #actions="{ record }">
                <div class="row-actions">
                  <a-button v-if="canUpdateDict" type="text" size="small" @click="openItemModal(record)">编辑</a-button>
                  <a-button
                    v-if="canUpdateDict && !isBuiltInItem(record)"
                    type="text"
                    size="small"
                    :status="record.status === 1 ? 'danger' : 'success'"
                    @click="toggleItemStatus(record)"
                  >
                    {{ record.status === 1 ? '禁用' : '启用' }}
                  </a-button>
                  <span v-else-if="canUpdateDict && isBuiltInItem(record)" class="no-actions">内置保护</span>
                  <span v-if="!canUpdateDict" class="no-actions">无可用操作</span>
                </div>
              </template>
              <template #empty>
                <DsEmptyState title="暂无字典项" description="当前字典类型下没有可展示的字典项。" />
              </template>
            </DsDataTable>
          </template>
        </section>
      </div>
    </div>

    <DsModalForm
      v-model:visible="typeModalVisible"
      :title="typeEditing ? '编辑字典类型' : '新增字典类型'"
      :description="typeEditing ? '维护字典类型名称、排序、状态和说明。' : '新增字典类型，用于承载一组下拉选项。'"
      :width="640"
      @cancel="typeModalVisible = false"
    >
      <DsFormSection title="类型信息">
        <DsFormGrid>
          <DsInput v-model="typeForm.typeCode" label="类型编码" type="text" required :max-length="64" placeholder="如 bid_status" :disabled="typeEditing" />
          <DsInput v-model="typeForm.typeName" label="类型名称" type="text" required :max-length="100" placeholder="请输入类型名称" />
          <DsNumberInput v-model="typeForm.sortOrder" label="排序" :min="0" :max="9999" />
          <DsStatusSelect v-model="typeForm.status" label="状态" :include-all="false" :allow-clear="false" :disabled="isBuiltInType(typeForm)" />
        </DsFormGrid>
          <div v-if="typeEditing && isBuiltInType(typeForm)" class="form-tip">内置字典类型编码不允许修改，也不允许禁用。</div>
      </DsFormSection>
      <DsFormSection title="说明">
        <DsTextarea v-model="typeForm.description" label="说明" placeholder="请输入说明" :max-length="300" :auto-size="{ minRows: 2, maxRows: 4 }" />
      </DsFormSection>
      <template #footer>
        <DsFormActions @cancel="typeModalVisible = false" @submit="submitTypeForm" />
      </template>
    </DsModalForm>

    <DsModalForm
      v-model:visible="itemModalVisible"
      :title="itemEditing ? '编辑字典项' : '新增字典项'"
      :description="itemEditing ? '维护字典项标签、值、颜色、默认项和状态。' : '在当前字典类型下新增一个可复用选项。'"
      :width="720"
      @cancel="itemModalVisible = false"
    >
      <DsFormSection title="字典项信息">
        <DsFormGrid>
          <DsInput v-model="itemForm.itemLabel" label="标签" type="text" required :max-length="100" placeholder="请输入显示标签" />
          <DsInput v-model="itemForm.itemValue" label="值" type="text" required :max-length="100" placeholder="请输入字典值" :disabled="itemEditing && isBuiltInItem(itemForm)" />
          <DsDictSelect v-model="itemForm.tagColor" type-code="tag_color" label="标签颜色" placeholder="请选择颜色" />
          <DsNumberInput v-model="itemForm.sortOrder" label="排序" :min="0" :max="9999" />
          <DsStatusSelect v-model="itemForm.status" label="状态" :include-all="false" :allow-clear="false" :disabled="isBuiltInItem(itemForm)" />
          <DsRadioGroup v-model="itemForm.isDefault" label="默认项" :options="defaultOptions" />
        </DsFormGrid>
            <div v-if="itemEditing && isBuiltInItem(itemForm)" class="form-tip">内置字典项值不允许修改，也不允许禁用。</div>
            <div class="form-tip">设为默认后，同类型其他默认项会自动取消；禁用字典项时默认标记会自动清除。</div>
      </DsFormSection>
      <DsFormSection title="说明">
        <DsTextarea v-model="itemForm.description" label="说明" placeholder="请输入说明" :max-length="300" :auto-size="{ minRows: 2, maxRows: 4 }" />
      </DsFormSection>
      <template #footer>
        <DsFormActions @cancel="itemModalVisible = false" @submit="submitItemForm" />
      </template>
    </DsModalForm>

    <DsModalForm
      v-model:visible="impactModalVisible"
      :title="impactTargetKind === 'type' ? '确认禁用字典类型' : '确认禁用字典项'"
      description="禁用前先检查内置保护、默认项和公共下拉影响。"
      :width="520"
      @cancel="closeImpactModal"
    >
      <div class="impact-content">
        <p v-if="impactLoading" class="muted-text">正在检查禁用影响...</p>
        <template v-else-if="disableImpact">
          <p>
            确认禁用
            <strong>{{ impactTargetKind === 'type' ? disableImpact.typeName : disableImpact.itemLabel }}</strong>
            吗？
          </p>
          <p v-if="disableImpact.builtIn" class="danger-text">{{ disableImpact.message }}</p>
          <p v-else class="muted-text">{{ disableImpact.message }}</p>
          <div class="impact-grid">
            <span>类型编码</span>
            <span>{{ disableImpact.typeCode }}</span>
            <span v-if="impactTargetKind === 'item'">字典项值</span>
            <span v-if="impactTargetKind === 'item'">{{ disableImpact.itemValue }}</span>
            <span v-if="impactTargetKind === 'type'">字典项数量</span>
            <span v-if="impactTargetKind === 'type'">{{ disableImpact.itemCount }} 项，其中启用 {{ disableImpact.enabledItemCount }} 项</span>
            <span v-if="impactTargetKind === 'item'">默认项</span>
            <span v-if="impactTargetKind === 'item'">{{ disableImpact.isDefault === 1 ? '是' : '否' }}</span>
            <span>公共展示</span>
            <span>{{ impactPublicText }}</span>
          </div>
          <div v-if="disableImpact.usage?.length" class="impact-usage">
            <div class="impact-title">可能影响</div>
            <ul>
              <li v-for="item in disableImpact.usage" :key="item">{{ item }}</li>
            </ul>
          </div>
        </template>
      </div>
      <template #footer>
        <DsFormActions
          cancel-text="取消"
          submit-text="确认禁用"
          :loading="impactLoading"
          @cancel="closeImpactModal"
          @submit="submitImpactForm"
        />
      </template>
    </DsModalForm>

    <DsModalForm
      v-model:visible="importResultVisible"
      :title="importResultTitle"
      description="查看本次导入的成功、跳过和失败明细。"
      :width="560"
      @cancel="importResultVisible = false"
    >
      <DsFormSection v-if="importResult" title="导入结果">
        <div class="impact-grid">
          <span>总行数</span>
          <span>{{ importResult.totalCount || 0 }}</span>
          <span>成功</span>
          <span>{{ importResult.successCount || 0 }}</span>
          <span>跳过</span>
          <span>{{ importResult.skippedCount || 0 }}</span>
          <span>失败</span>
          <span>{{ importResult.failureCount || 0 }}</span>
        </div>
        <div v-if="importResult.message" class="detail-block">
          <div class="detail-title">导入结果</div>
          <div class="detail-text">{{ importResult.message }}</div>
        </div>
        <div v-if="importResult.details?.length" class="impact-usage">
          <div class="impact-title">明细</div>
          <ul>
            <li v-for="item in importResult.details" :key="item">{{ item }}</li>
          </ul>
        </div>
      </DsFormSection>
      <template #footer>
        <DsFormActions submit-text="关闭" :show-cancel="false" @submit="importResultVisible = false" />
      </template>
    </DsModalForm>

    <input ref="typeFileInput" type="file" accept=".xlsx" class="hidden-file-input" @change="handleTypeFileChange" />
    <input ref="itemFileInput" type="file" accept=".xlsx" class="hidden-file-input" @change="handleItemFileChange" />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconBook, IconPlus, IconRefresh } from '@arco-design/web-vue/es/icon'
import { dictApi } from '@/api'
import { hasPermission } from '@/utils/permission'
import { DEFAULT_PAGE_SIZE_OPTIONS, applyPaginationConfig } from '@/utils/systemConfig'
import { loadCommonStatusOptions, loadPublicDictItems, statusLabel } from '@/utils/dict'
import {
  DsDataTable,
  DsDictSelect,
  DsEmptyState,
  DsFormActions,
  DsFormGrid,
  DsFormSection,
  DsHeaderActions,
  DsIconButton,
  DsInput,
  DsKeywordSearch,
  DsModalForm,
  DsNumberInput,
  DsPageHeader,
  DsRadioGroup,
  DsStatusSelect,
  DsStatusTag,
  DsTextarea
} from '@/design-system'

const typeLoading = ref(false)
const itemLoading = ref(false)
const typeList = ref([])
const itemList = ref([])
const selectedType = ref(null)
const typePage = ref(1)
const itemPage = ref(1)
const typeSize = ref(20)
const itemSize = ref(20)
const pageSizeOptions = ref([...DEFAULT_PAGE_SIZE_OPTIONS])
const typeTotal = ref(0)
const itemTotal = ref(0)
const typeModalVisible = ref(false)
const itemModalVisible = ref(false)
const impactModalVisible = ref(false)
const impactLoading = ref(false)
const impactTargetKind = ref('')
const impactTarget = ref(null)
const disableImpact = ref(null)
const typeEditing = ref(false)
const itemEditing = ref(false)
const importResultVisible = ref(false)
const importResultTitle = ref('')
const importResult = ref(null)
const typeFileInput = ref(null)
const itemFileInput = ref(null)

const typeFilters = reactive({ keyword: '', status: '' })
const itemFilters = reactive({ keyword: '', status: '' })
const typeForm = reactive({ id: null, typeCode: '', typeName: '', description: '', sortOrder: 0, status: 1 })
const itemForm = reactive({ id: null, itemLabel: '', itemValue: '', tagColor: 'arcoblue', sortOrder: 0, status: 1, isDefault: 0, description: '' })

const canCreateDict = computed(() => hasPermission('system_dict:create'))
const canImportDict = computed(() => hasPermission('system_dict:import'))
const canUpdateDict = computed(() => hasPermission('system_dict:update'))
const canExportDict = computed(() => hasPermission('system_dict:export'))
const headerToolItems = computed(() => [
  { key: 'template', label: '下载类型模板', visible: canImportDict.value },
  { key: 'import', label: '导入类型', visible: canImportDict.value },
  { key: 'export', label: '导出类型', visible: canExportDict.value }
])
const itemToolItems = computed(() => [
  { key: 'template', label: '下载模板', visible: canImportDict.value && !!selectedType.value },
  { key: 'import', label: '导入字典项', visible: canImportDict.value && !!selectedType.value },
  { key: 'export', label: '导出字典项', visible: canExportDict.value && !!selectedType.value }
])
const defaultOptions = [
  { label: '是', value: 1 },
  { label: '否', value: 0 }
]
const fallbackColorOptions = [
  { itemLabel: '蓝色', itemValue: 'arcoblue', tagColor: 'arcoblue' },
  { itemLabel: '绿色', itemValue: 'green', tagColor: 'green' },
  { itemLabel: '橙红', itemValue: 'orangered', tagColor: 'orangered' },
  { itemLabel: '红色', itemValue: 'red', tagColor: 'red' },
  { itemLabel: '金色', itemValue: 'gold', tagColor: 'gold' },
  { itemLabel: '紫色', itemValue: 'purple', tagColor: 'purple' },
  { itemLabel: '灰色', itemValue: 'gray', tagColor: 'gray' }
]
const colorOptions = ref([])
const statusOptions = ref([])
const builtInTypeCodes = [
  'common_status',
  'success_status',
  'user_role_type',
  'config_group',
  'config_value_type',
  'message_type',
  'todo_type',
  'todo_priority',
  'todo_status',
  'tag_color',
  'file_storage_type',
  'operation_log_module',
  'operation_log_action'
]
const builtInItemValues = {
  common_status: ['enabled', 'disabled'],
  success_status: ['success', 'failed'],
  user_role_type: ['admin', 'user'],
  config_group: ['basic', 'security', 'ui'],
  config_value_type: ['TEXT', 'NUMBER', 'BOOLEAN', 'JSON'],
  message_type: ['SYSTEM', 'NOTICE', 'TODO', 'WARNING'],
  todo_type: ['GENERAL', 'SYSTEM', 'FOLLOW_UP'],
  todo_priority: ['LOW', 'MEDIUM', 'HIGH', 'URGENT'],
  todo_status: ['PENDING', 'PROCESSING', 'DONE'],
  tag_color: ['arcoblue', 'green', 'orangered', 'red', 'gold', 'purple', 'gray'],
  file_storage_type: ['LOCAL'],
  operation_log_module: ['DEPARTMENT', 'USER', 'ROLE', 'CONFIG', 'DICT', 'AUTH', 'FILE', 'MESSAGE', 'TODO', 'LOG'],
  operation_log_action: [
    'CREATE', 'UPDATE', 'DELETE', 'ENABLE', 'DISABLE', 'STATUS',
    'EXPORT', 'IMPORT',
    'RESET_PASSWORD', 'MOVE_DEPT', 'ASSIGN_ROLES', 'SAVE_FEATURES',
    'SAVE_DATA_SCOPE', 'SAVE_FIELDS', 'LOGIN', 'LOGIN_FAILED', 'ACCOUNT_LOCKED', 'LOGOUT', 'CREATE_TYPE',
    'UPDATE_TYPE', 'ENABLE_TYPE', 'DISABLE_TYPE', 'STATUS_TYPE',
    'CREATE_ITEM', 'UPDATE_ITEM', 'ENABLE_ITEM', 'DISABLE_ITEM', 'STATUS_ITEM'
  ]
}

const typeColumns = [
  { title: '序号', width: 56, align: 'center', slotName: 'index' },
  { title: '字典类型', slotName: 'typeInfo' },
  { title: '操作', slotName: 'actions', width: 120 }
]

const itemColumns = [
  { title: '序号', width: 68, align: 'center', slotName: 'index' },
  { title: '字典项', slotName: 'itemInfo', width: 230 },
  { title: '颜色', slotName: 'tagColor', width: 120 },
  { title: '状态', slotName: 'status', width: 96 },
  { title: '说明', dataIndex: 'description' },
  { title: '操作', slotName: 'actions', width: 160 }
]
const typePaginationConfig = computed(() => ({
  current: typePage.value,
  pageSize: typeSize.value,
  total: typeTotal.value,
  pageSizeOptions: pageSizeOptions.value,
  compact: true
}))
const itemPaginationConfig = computed(() => ({
  current: itemPage.value,
  pageSize: itemSize.value,
  total: itemTotal.value,
  pageSizeOptions: pageSizeOptions.value,
  compact: true
}))

const fetchTypes = async () => {
  typeLoading.value = true
  try {
    const res = await dictApi.types({
      page: typePage.value,
      size: typeSize.value,
      keyword: typeFilters.keyword?.trim() || undefined,
      status: typeFilters.status === '' ? undefined : typeFilters.status
    })
    if (res.code === 200) {
      typeList.value = res.data?.records || []
      typeTotal.value = res.data?.total || 0
      if (!selectedType.value && typeList.value.length) {
        await selectType(typeList.value[0])
      } else if (selectedType.value && !typeList.value.some(item => item.id === selectedType.value.id)) {
        selectedType.value = null
        itemList.value = []
        itemTotal.value = 0
      }
    } else {
      Message.error(res.msg || '加载字典类型失败')
    }
  } catch {
    Message.error('加载字典类型失败')
  } finally {
    typeLoading.value = false
  }
}

const fetchItems = async () => {
  if (!selectedType.value?.id) return
  itemLoading.value = true
  try {
    const res = await dictApi.items(selectedType.value.id, {
      page: itemPage.value,
      size: itemSize.value,
      keyword: itemFilters.keyword?.trim() || undefined,
      status: itemFilters.status === '' ? undefined : itemFilters.status
    })
    if (res.code === 200) {
      itemList.value = res.data?.records || []
      itemTotal.value = res.data?.total || 0
    } else {
      Message.error(res.msg || '加载字典项失败')
    }
  } catch {
    Message.error('加载字典项失败')
  } finally {
    itemLoading.value = false
  }
}

const selectType = async (record) => {
  if (!record?.id) return
  selectedType.value = record
  itemPage.value = 1
  await fetchItems()
}

const handleTypeRowClick = async (...args) => {
  const record = args.find(item => item && typeof item === 'object' && item.id)
  if (!record) return
  await selectType(record)
}

const typeRowClass = (record) => record?.id === selectedType.value?.id ? 'type-row-selected' : ''
const isBuiltInType = (record) => builtInTypeCodes.includes((record?.typeCode || '').trim())
const isBuiltInItem = (record) => {
  const typeCode = (selectedType.value?.typeCode || '').trim()
  const value = (record?.itemValue || '').trim()
  return !!typeCode && (builtInItemValues[typeCode] || []).includes(value)
}
const handleTypeSearch = () => { typePage.value = 1; fetchTypes() }
const handleItemSearch = () => { itemPage.value = 1; fetchItems() }
const handleTypePageChange = (nextPage) => { typePage.value = nextPage; fetchTypes() }
const handleItemPageChange = (nextPage) => { itemPage.value = nextPage; fetchItems() }
const handleTypeSizeChange = (nextSize) => { typeSize.value = nextSize; typePage.value = 1; fetchTypes() }
const handleItemSizeChange = (nextSize) => { itemSize.value = nextSize; itemPage.value = 1; fetchItems() }
const refreshTypes = () => fetchTypes()
const refreshItems = () => fetchItems()
const handleHeaderToolSelect = (key) => {
  if (key === 'template') downloadTypeTemplate()
  if (key === 'import') triggerTypeImport()
  if (key === 'export') exportTypes()
}
const handleItemToolSelect = (key) => {
  if (key === 'template') downloadItemTemplate()
  if (key === 'import') triggerItemImport()
  if (key === 'export') exportItems()
}
const exportTypes = async () => {
  try {
    const blob = await dictApi.exportTypes({
      keyword: typeFilters.keyword?.trim() || undefined,
      status: typeFilters.status === '' ? undefined : typeFilters.status
    })
    downloadBlob(blob, `字典类型_${new Date().toISOString().slice(0, 10).replace(/-/g, '')}.xlsx`)
    Message.success('字典类型已开始导出')
  } catch {
    Message.error('导出失败')
  }
}

const downloadTypeTemplate = async () => {
  try {
    const blob = await dictApi.downloadTypeTemplate()
    downloadBlob(blob, '字典类型导入模板.xlsx')
    Message.success('模板已下载')
  } catch {
    Message.error('下载模板失败')
  }
}

const triggerTypeImport = () => typeFileInput.value?.click()

const exportItems = async () => {
  if (!selectedType.value) {
    Message.warning('请先选择字典类型')
    return
  }
  try {
    const blob = await dictApi.exportItems(selectedType.value.id, {
      keyword: itemFilters.keyword?.trim() || undefined,
      status: itemFilters.status === '' ? undefined : itemFilters.status
    })
    downloadBlob(blob, `${selectedType.value.typeCode || '字典项'}_${new Date().toISOString().slice(0, 10).replace(/-/g, '')}.xlsx`)
    Message.success('字典项已开始导出')
  } catch {
    Message.error('导出失败')
  }
}

const downloadItemTemplate = async () => {
  if (!selectedType.value) {
    Message.warning('请先选择字典类型')
    return
  }
  try {
    const blob = await dictApi.downloadItemTemplate(selectedType.value.id)
    downloadBlob(blob, `${selectedType.value.typeCode || '字典项'}_导入模板.xlsx`)
    Message.success('模板已下载')
  } catch {
    Message.error('下载模板失败')
  }
}

const triggerItemImport = () => itemFileInput.value?.click()

const resetTypeFilters = () => {
  Object.assign(typeFilters, { keyword: '', status: '' })
  typePage.value = 1
  fetchTypes()
}
const resetItemFilters = () => {
  Object.assign(itemFilters, { keyword: '', status: '' })
  itemPage.value = 1
  fetchItems()
}

const handleTypeFileChange = async (event) => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await dictApi.importTypes(formData)
    if (res.code !== 200) {
      Message.error(res.msg || '导入失败')
      return
    }
    showImportResult('字典类型导入结果', res.data)
    Message.success(res.data?.message || '导入完成')
    await fetchTypes()
    window.dispatchEvent(new CustomEvent('system-dict-updated'))
  } catch {
    Message.error('导入失败')
  }
}

const handleItemFileChange = async (event) => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file || !selectedType.value) return
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await dictApi.importItems(selectedType.value.id, formData)
    if (res.code !== 200) {
      Message.error(res.msg || '导入失败')
      return
    }
    showImportResult(`${selectedType.value.typeName} 字典项导入结果`, res.data)
    Message.success(res.data?.message || '导入完成')
    await fetchItems()
    window.dispatchEvent(new CustomEvent('system-dict-updated'))
  } catch {
    Message.error('导入失败')
  }
}

const showImportResult = (title, data) => {
  importResultTitle.value = title
  importResult.value = data || {}
  importResultVisible.value = true
}

const openTypeModal = (record) => {
  typeEditing.value = !!record
  Object.assign(typeForm, record ? {
    id: record.id,
    typeCode: record.typeCode || '',
    typeName: record.typeName || '',
    description: record.description || '',
    sortOrder: record.sortOrder || 0,
    status: record.status === 0 ? 0 : 1
  } : { id: null, typeCode: '', typeName: '', description: '', sortOrder: 0, status: 1 })
  typeModalVisible.value = true
}

const handleTypeOk = async (done) => {
  if (!typeForm.typeCode.trim()) return rejectModal(done, '字典编码不能为空')
  if (!typeForm.typeName.trim()) return rejectModal(done, '字典名称不能为空')
  const payload = {
    typeCode: typeForm.typeCode,
    typeName: typeForm.typeName,
    description: typeForm.description,
    sortOrder: typeForm.sortOrder,
    status: isBuiltInType(typeForm) ? 1 : typeForm.status
  }
  const res = typeEditing.value ? await dictApi.updateType(typeForm.id, payload) : await dictApi.addType(payload)
  if (res.code !== 200) return rejectModal(done, res.msg || '操作失败')
  Message.success(typeEditing.value ? '字典类型已更新' : '字典类型已新增')
  typeModalVisible.value = false
  await fetchTypes()
  window.dispatchEvent(new CustomEvent('system-dict-updated'))
  done()
}

const submitTypeForm = () => {
  handleTypeOk((result) => {
    if (result === false) return
  })
}

const toggleTypeStatus = async (record) => {
  const next = record.status === 1 ? 0 : 1
  if (next === 0) {
    await openDisableImpact('type', record)
    return
  }
  const res = await dictApi.updateTypeStatus(record.id, next)
  if (res.code === 200) {
    Message.success(next === 1 ? '字典类型已启用' : '字典类型已禁用')
    await fetchTypes()
    window.dispatchEvent(new CustomEvent('system-dict-updated'))
  } else {
    Message.error(res.msg || '操作失败')
  }
}

const openItemModal = (record) => {
  itemEditing.value = !!record
  Object.assign(itemForm, record ? {
    id: record.id,
    itemLabel: record.itemLabel || '',
    itemValue: record.itemValue || '',
    tagColor: record.tagColor || 'arcoblue',
    sortOrder: record.sortOrder || 0,
    status: record.status === 0 ? 0 : 1,
    isDefault: record.isDefault === 1 ? 1 : 0,
    description: record.description || ''
  } : { id: null, itemLabel: '', itemValue: '', tagColor: 'arcoblue', sortOrder: 0, status: 1, isDefault: 0, description: '' })
  itemModalVisible.value = true
}

const handleItemOk = async (done) => {
  if (!selectedType.value) return rejectModal(done, '请选择字典类型')
  if (!itemForm.itemLabel.trim()) return rejectModal(done, '字典项标签不能为空')
  if (!itemForm.itemValue.trim()) return rejectModal(done, '字典项值不能为空')
  const payload = {
    itemLabel: itemForm.itemLabel,
    itemValue: itemForm.itemValue,
    tagColor: itemForm.tagColor,
    sortOrder: itemForm.sortOrder,
    status: isBuiltInItem(itemForm) ? 1 : itemForm.status,
    isDefault: itemForm.isDefault,
    description: itemForm.description
  }
  const res = itemEditing.value ? await dictApi.updateItem(itemForm.id, payload) : await dictApi.addItem(selectedType.value.id, payload)
  if (res.code !== 200) return rejectModal(done, res.msg || '操作失败')
  Message.success(itemEditing.value ? '字典项已更新' : '字典项已新增')
  itemModalVisible.value = false
  await fetchItems()
  window.dispatchEvent(new CustomEvent('system-dict-updated'))
  done()
}

const submitItemForm = () => {
  handleItemOk((result) => {
    if (result === false) return
  })
}

const toggleItemStatus = async (record) => {
  const next = record.status === 1 ? 0 : 1
  if (next === 0) {
    await openDisableImpact('item', record)
    return
  }
  const res = await dictApi.updateItemStatus(record.id, next)
  if (res.code === 200) {
    Message.success(next === 1 ? '字典项已启用' : '字典项已禁用')
    await fetchItems()
    window.dispatchEvent(new CustomEvent('system-dict-updated'))
  } else {
    Message.error(res.msg || '操作失败')
  }
}

const rejectModal = (done, msg) => {
  Message.error(msg)
  done(false)
}

const impactPublicText = computed(() => {
  if (!disableImpact.value) return '-'
  if (impactTargetKind.value === 'type') {
    return disableImpact.value.publicAvailable ? '禁用后公共接口将返回空列表' : '当前未启用或无启用字典项'
  }
  return disableImpact.value.willRemoveFromPublic ? '禁用后将从公共下拉和标签中移除' : '当前不会出现在公共字典结果中'
})

const openDisableImpact = async (kind, record) => {
  impactTargetKind.value = kind
  impactTarget.value = record
  disableImpact.value = null
  impactModalVisible.value = true
  impactLoading.value = true
  try {
    const res = kind === 'type' ? await dictApi.typeDisableImpact(record.id) : await dictApi.itemDisableImpact(record.id)
    if (res.code === 200) {
      disableImpact.value = res.data || {}
    } else {
      Message.error(res.msg || '检查禁用影响失败')
      impactModalVisible.value = false
    }
  } catch {
    Message.error('检查禁用影响失败')
    impactModalVisible.value = false
  } finally {
    impactLoading.value = false
  }
}

const closeImpactModal = () => {
  impactModalVisible.value = false
  impactTargetKind.value = ''
  impactTarget.value = null
  disableImpact.value = null
  impactLoading.value = false
}

const statusTone = (status) => Number(status) === 1 ? 'success' : 'danger'
const tagTone = (color) => {
  if (color === 'green') return 'success'
  if (color === 'red' || color === 'orangered') return 'danger'
  if (color === 'gold' || color === 'orange') return 'warning'
  if (color === 'arcoblue' || color === 'blue' || color === 'purple') return 'primary'
  return 'neutral'
}

const downloadBlob = (blob, fileName) => {
  const objectUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(objectUrl)
}

const handleImpactOk = async (done) => {
  if (impactLoading.value) {
    done(false)
    return
  }
  if (!disableImpact.value?.canDisable) {
    Message.warning(disableImpact.value?.message || '当前字典不允许禁用')
    done(false)
    return
  }
  const target = impactTarget.value
  if (!target) {
    done(false)
    return
  }
  try {
    const kind = impactTargetKind.value
    const res = impactTargetKind.value === 'type'
      ? await dictApi.updateTypeStatus(target.id, 0)
      : await dictApi.updateItemStatus(target.id, 0)
    if (res.code !== 200) {
      Message.error(res.msg || '禁用失败')
      done(false)
      return
    }
    Message.success(kind === 'type' ? '字典类型已禁用' : '字典项已禁用')
    closeImpactModal()
    if (kind === 'type') {
      await fetchTypes()
    } else {
      await fetchItems()
    }
    window.dispatchEvent(new CustomEvent('system-dict-updated'))
  } catch {
    Message.error('禁用失败')
    done(false)
    return
  }
  done()
}

const submitImpactForm = () => {
  handleImpactOk((result) => {
    if (result === false) return
  })
}

const refreshRuntimeConfig = async () => {
  await applyPaginationConfig(typeSize, pageSizeOptions)
  await Promise.all([loadColorOptions(), loadStatusOptions()])
  itemSize.value = typeSize.value
  typePage.value = 1
  itemPage.value = 1
  await fetchTypes()
  if (selectedType.value) await fetchItems()
}

const loadColorOptions = async () => {
  colorOptions.value = await loadPublicDictItems('tag_color', fallbackColorOptions)
}

const loadStatusOptions = async () => {
  statusOptions.value = await loadCommonStatusOptions()
}

onMounted(async () => {
  await applyPaginationConfig(typeSize, pageSizeOptions)
  await Promise.all([loadColorOptions(), loadStatusOptions()])
  itemSize.value = typeSize.value
  await fetchTypes()
  window.addEventListener('system-config-updated', refreshRuntimeConfig)
  window.addEventListener('system-dict-updated', refreshRuntimeConfig)
})

onUnmounted(() => {
  window.removeEventListener('system-config-updated', refreshRuntimeConfig)
  window.removeEventListener('system-dict-updated', refreshRuntimeConfig)
})
</script>

<style scoped>
.dict-page { min-height: 100vh; background: var(--ds-color-bg-page); }
.main { box-sizing: border-box; width: 100%; max-width: 1680px; margin: 0 auto; padding: 24px; display: grid; gap: 16px; }
.dict-layout { display: grid; grid-template-columns: 500px minmax(0, 1fr); gap: 16px; align-items: start; }
.type-panel, .item-panel { min-width: 0; padding: 16px; border: 1px solid color-mix(in srgb, var(--ds-color-primary) 4%, var(--ds-color-border)); border-radius: 12px; background: var(--ds-color-bg-card); box-shadow: 0 8px 24px var(--ds-color-shadow); }
.panel-header { margin-bottom: 14px; display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.panel-header--items { align-items: center; }
.panel-title { margin: 0; color: var(--ds-color-text-primary); font-size: 16px; font-weight: 600; line-height: 24px; }
.panel-description { margin: 4px 0 0; color: var(--ds-color-text-secondary); font-size: 13px; line-height: 1.5; }
.panel-filter { margin-bottom: 14px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.panel-filter--item { justify-content: flex-start; }
.type-search { width: 180px; }
.item-search { width: 240px; }
.status-select { width: 120px; }
.filter-button { min-width: 68px; height: 36px; border-radius: 8px; }
.selected-title { min-width: 0; display: flex; flex-direction: column; gap: 2px; font-weight: 600; color: var(--ds-color-text-primary); }
.hidden-file-input { display: none; }
.type-info { min-width: 0; display: grid; grid-template-columns: minmax(0, 1fr); gap: 4px; align-items: center; }
.type-select-trigger { cursor: pointer; padding: 2px 0; border-radius: 8px; }
.type-select-trigger:hover .type-name { color: var(--ds-color-primary); }
.type-index-button {
  width: 100%;
  border: 0;
  padding: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font: inherit;
  line-height: 24px;
}
.type-index-button:hover {
  color: var(--ds-color-primary);
}
.type-name { min-width: 0; color: var(--ds-color-text-primary); font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.type-code { color: var(--ds-color-text-secondary); font-size: 12px; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.type-tags { display: inline-flex; align-items: center; gap: 4px; min-width: 0; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.row-actions { display: inline-flex; align-items: center; gap: 4px; flex-wrap: nowrap; white-space: nowrap; }
.row-actions :deep(.arco-btn-text) { height: 28px; padding: 0 8px; border-radius: 6px; }
.no-actions { color: var(--ds-color-text-secondary); padding: 0 8px; white-space: nowrap; display: inline-flex; align-items: center; }
.empty-state { padding: 80px 0; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.form-tip { margin-top: 6px; color: var(--ds-color-text-secondary); font-size: 12px; line-height: 1.5; }
.impact-content { color: var(--ds-color-text-primary); line-height: 1.7; }
.impact-grid { margin-top: 12px; display: grid; grid-template-columns: 96px 1fr; gap: 6px 12px; font-size: 13px; }
.impact-grid span:nth-child(odd) { color: var(--ds-color-text-secondary); }
.impact-title { margin-top: 12px; font-weight: 600; }
.impact-usage ul { margin: 6px 0 0; padding-left: 20px; color: var(--ds-color-text-regular); }
.muted-text { color: var(--ds-color-text-secondary); }
.danger-text { color: var(--ds-color-danger); }
.item-value { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace; }
.item-info-cell { min-width: 0; display: grid; grid-template-columns: minmax(0, 1fr); gap: 4px; align-items: center; max-width: 100%; }
.item-info-cell .item-label { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--ds-color-text-primary); font-weight: 500; }
.item-info-cell .item-value { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--ds-color-text-secondary); font-size: 12px; }
.item-meta { display: inline-flex; align-items: center; gap: 6px; min-width: 0; color: var(--ds-color-text-secondary); font-size: 12px; white-space: nowrap; }
.type-panel :deep(.arco-table-content),
.item-panel :deep(.arco-table-content),
.type-panel :deep(.arco-table-body),
.item-panel :deep(.arco-table-body) { overflow-x: hidden; }
.type-panel :deep(.arco-table-element),
.item-panel :deep(.arco-table-element) { min-width: 100%; }
.type-panel :deep(.arco-table-td),
.type-panel :deep(.arco-table-th),
.item-panel :deep(.arco-table-td),
.item-panel :deep(.arco-table-th) { padding-left: 12px; padding-right: 12px; white-space: nowrap; }
.type-panel :deep(.arco-table-tr) { cursor: pointer; }
:deep(.type-row-selected td) { background: var(--ds-color-bg-selected) !important; }
:deep(.type-row-selected td:first-child) { box-shadow: inset 3px 0 0 var(--ds-color-primary); }
@media (max-width: 1100px) {
  .dict-layout { grid-template-columns: 1fr; }
  .panel-header--items { align-items: flex-start; flex-direction: column; }
}
</style>
