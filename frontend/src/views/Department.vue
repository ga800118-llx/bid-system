<template>
  <div class="dept-page">
    <PageHeader title="部门管理" />
    <div class="main">
      <div class="dept-layout">
        <!-- 左侧：部门树 -->
        <div class="dept-tree-panel">
          <div class="panel-header">
            <span class="panel-title">部门列表</span>
            <a-button type="primary" size="small" @click="openAddModal(null)" v-if="roleName == 'admin'">
              <template #icon><IconPlus /></template>
              新增
            </a-button>
          </div>
          <div class="tree-container">
            <a-tree
              :data="treeData"
              :selected-keys="selectedKeys"
              :default-expand-all="true"
              @select="onTreeSelect"
              blockNode
            >
              <template #title="{ data }">
                <div class="tree-node">
                  <span class="node-name" :class="{ 'node-disabled': data.status === 0 }">
                    {{ data.name }}
                    <span v-if="data.status === 0" class="status-tag">禁用</span>
                  </span>
                  <div class="node-actions" v-if="roleName == 'admin'" @click.stop>
                    <a-button type="text" size="mini" @click="openEditModal(data)"><IconEdit /></a-button>
                    <a-button type="text" size="mini" status="danger" @click="confirmDelete(data)"><IconDelete /></a-button>
                  </div>
                </div>
              </template>
            </a-tree>
          </div>
        </div>

        <!-- 右侧：用户列表（占位） -->
        <div class="user-list-panel">
          <div class="panel-header">
            <span class="panel-title">
              {{ selectedDeptName ? selectedDeptName + ' - 用户列表' : '请选择部门' }}
            </span>
          </div>
          <div class="user-placeholder" v-if="!selectedDeptId">
            <IconUser style="font-size:48px;color:#ccc" />
            <div style="color:#999;margin-top:12px">请从左侧选择一个部门查看用户</div>
          </div>
          <div class="user-list" v-else>
            <div class="user-table-area">
              <a-table :columns="columns" :data="userList" :loading="userLoading" :pagination="false" stripe size="small">
                <template #status="{ record }">
                  <a-tag :color="record.status === 1 ? 'green' : 'red'">
                    {{ record.status === 1 ? '正常' : '禁用' }}
                  </a-tag>
                </template>
              </a-table>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑部门弹窗 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="isEdit ? '编辑部门' : '新增部门'"
      :width="400"
      @before-ok="handleModalOk"
      @cancel="modalVisible = false"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="部门名称" required>
          <a-input v-model="form.name" placeholder="请输入部门名称" :max-length="50" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model="form.sortOrder" :min="0" :max="9999" placeholder="数字越小越靠前" style="width:100%" />
        </a-form-item>
        <a-form-item label="状态" v-if="isEdit">
          <a-radio-group v-model="form.status">
            <a-radio :value="1">正常</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 删除确认弹窗 -->
    <a-modal
      v-model:visible="deleteModalVisible"
      title="确认删除"
      :width="440"
      @before-ok="handleDeleteOk"
      @cancel="deleteModalVisible = false"
    >
      <div style="color:#333;line-height:1.8">
        <p>确认删除部门「<strong>{{ deleteTarget?.name }}</strong>」吗？</p>
        <p v-if="deleteTarget && deleteTarget.childCount > 0" style="color:#F53F3F">
          提示：该部门下有 {{ deleteTarget.childCount }} 个子部门，将一并删除。
        </p>
        <p style="color:#999;font-size:12px">此操作不可恢复。</p>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconPlus, IconEdit, IconDelete, IconUser } from '@arco-design/web-vue/es/icon'

const roleName = localStorage.getItem('role') || 'user'
const baseURL = '/api'
const token = localStorage.getItem('token')

const treeData = ref([])
const selectedKeys = ref([])
const selectedDeptId = ref(null)
const selectedDeptName = ref('')
const userList = ref([])
const userLoading = ref(false)
const modalVisible = ref(false)
const deleteModalVisible = ref(false)
const isEdit = ref(false)
const deleteTarget = ref(null)

const form = reactive({ id: null, name: '', sortOrder: 0, status: 1 })

const columns = [
  { title: '序号', width: 70, align: 'center', slotName: 'index' },
  { title: '姓名', dataIndex: 'realName', width: 120 },
  { title: '账号', dataIndex: 'username', width: 150 },
  { title: '状态', slotName: 'status', width: 80 }
]

const fetchTree = async () => {
  try {
    const res = await fetch(baseURL + '/system/departments/tree', {
      headers: { Authorization: 'Bearer ' + token }
    }).then(r => r.json())
    if (res.code === 200) {
      treeData.value = res.data || []
    } else { Message.error(res.msg || '加载失败') }
  } catch { Message.error('加载部门树失败') }
}

const fetchUsers = async (deptId) => {
  userLoading.value = true
  try {
    const res = await fetch(baseURL + '/system/departments/' + deptId + '/users', {
      headers: { Authorization: 'Bearer ' + token }
    }).then(r => r.json())
    if (res.code === 200) {
      userList.value = res.data?.records || []
    } else { userList.value = [] }
  } catch { userList.value = [] }
  finally { userLoading.value = false }
}

const onTreeSelect = (keys) => {
  if (keys.length === 0) return
  selectedDeptId.value = keys[0]
  const node = findNode(treeData.value, keys[0])
  selectedDeptName.value = node ? node.name : ''
  selectedKeys.value = keys
  fetchUsers(keys[0])
}

const findNode = (nodes, id) => {
  for (const n of nodes) {
    if (n.id == id) return n
    if (n.children?.length) {
      const f = findNode(n.children, id)
      if (f) return f
    }
  }
  return null
}

const openAddModal = (parent) => {
  isEdit.value = false
  form.id = null
  form.name = ''
  form.sortOrder = 0
  form.status = 1
  modalVisible.value = true
}

const openEditModal = (node) => {
  isEdit.value = true
  form.id = node.id
  form.name = node.name
  form.sortOrder = node.sortOrder || 0
  form.status = node.status
  modalVisible.value = true
}

const handleModalOk = async (done) => {
  if (!form.name.trim()) {
    Message.error('部门名称不能为空')
    done(false)
    return
  }
  try {
    const url = isEdit.value ? baseURL + '/system/departments/' + form.id : baseURL + '/system/departments'
    const method = isEdit.value ? 'PUT' : 'POST'
    const body = isEdit.value
      ? { name: form.name, sortOrder: form.sortOrder, status: form.status }
      : { name: form.name, parentId: selectedDeptId.value || null, sortOrder: form.sortOrder }
    const res = await fetch(url, {
      method,
      headers: { Authorization: 'Bearer ' + token, 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    }).then(r => r.json())
    if (res.code === 200) {
      Message.success(isEdit.value ? '更新成功' : '新增成功')
      modalVisible.value = false
      fetchTree()
    } else {
      Message.error(res.msg || '操作失败')
    }
  } catch { Message.error('操作失败') }
  done()
}

const confirmDelete = (node) => {
  const childCount = countChildren(node)
  deleteTarget.value = { ...node, childCount }
  deleteModalVisible.value = true
}

const countChildren = (node) => {
  let c = node.children?.length || 0
  for (const ch of node.children || []) c += countChildren(ch)
  return c
}

const handleDeleteOk = async (done) => {
  try {
    const res = await fetch(baseURL + '/system/departments/' + deleteTarget.value.id, {
      method: 'DELETE',
      headers: { Authorization: 'Bearer ' + token }
    }).then(r => r.json())
    if (res.code === 200) {
      Message.success('删除成功')
      deleteModalVisible.value = false
      if (selectedDeptId.value == deleteTarget.value.id) {
        selectedDeptId.value = null
        selectedDeptName.value = ''
        userList.value = []
      }
      fetchTree()
    } else {
      Message.error(res.msg || '删除失败')
    }
  } catch { Message.error('删除失败') }
  done()
}

onMounted(fetchTree)
</script>

<style scoped>
.dept-page { min-height: 100vh; background: #f0f2f5; }
.main { padding: 24px; max-width: 1200px; margin: 0 auto; }
.dept-layout { display: flex; gap: 16px; height: calc(100vh - 140px); }
.dept-tree-panel { width: 280px; background: #fff; border-radius: 8px; display: flex; flex-direction: column; flex-shrink: 0; }
.user-list-panel { flex: 1; background: #fff; border-radius: 8px; display: flex; flex-direction: column; min-width: 0; }
.panel-header { padding: 16px 20px; border-bottom: 1px solid #e8e8e8; display: flex; align-items: center; justify-content: space-between; flex-shrink: 0; }
.panel-title { font-size: 15px; font-weight: 600; color: #333; }
.tree-container { flex: 1; overflow: auto; padding: 12px; }
.user-placeholder { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.user-table-area { flex: 1; overflow: auto; padding: 16px; }
.tree-node { display: flex; align-items: center; justify-content: space-between; width: 100%; padding-right: 4px; }
.node-name { flex: 1; }
.node-disabled { color: #999; text-decoration: line-through; }
.status-tag { font-size: 11px; background: #f2f2f2; color: #999; padding: 0 4px; border-radius: 2px; margin-left: 4px; }
.node-actions { display: flex; gap: 2px; opacity: 0; transition: opacity 0.2s; }
.tree-node:hover .node-actions { opacity: 1; }
</style>