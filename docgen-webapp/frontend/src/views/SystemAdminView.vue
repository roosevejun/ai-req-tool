<template>
  <div class="container">
    <section class="panel">
      <div class="panel-head">
        <h3>快捷入口</h3>
      </div>
      <div class="row">
        <button class="primary" :disabled="loading" @click="goCreateAi">AI 创建项目</button>
        <button class="ghost" :disabled="loading" @click="goAiDocgen">进入 AI 需求整理</button>
      </div>
    </section>

    <section class="panel">
      <div class="panel-head">
        <h3>用户管理</h3>
        <button class="ghost" :disabled="loading" @click="loadAll">刷新</button>
      </div>

      <div class="form-grid">
        <input v-model.trim="newUser.username" class="input" placeholder="用户名" />
        <input v-model="newUser.password" type="password" class="input" placeholder="密码" />
        <input v-model.trim="newUser.displayName" class="input" placeholder="显示名称" />
        <select v-model="newUser.status" class="input">
          <option value="ENABLED">启用</option>
          <option value="DISABLED">禁用</option>
        </select>
      </div>
      <div class="checkbox-row">
        <label v-for="r in roles" :key="`nu-${r.id}`" class="check-item">
          <input v-model="newUser.roleIds" type="checkbox" :value="r.id" />
          {{ r.roleCode }}
        </label>
      </div>
      <div class="row">
        <button class="primary" :disabled="loading || !newUser.username || !newUser.password" @click="createUser">
          创建用户
        </button>
      </div>

      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>显示名称</th>
            <th>状态</th>
            <th>角色</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.id }}</td>
            <td>{{ u.username }}</td>
            <td>{{ u.displayName }}</td>
            <td>{{ statusLabel(u.status) }}</td>
            <td>{{ (u.roleCodes || []).join(', ') }}</td>
            <td>
              <button class="mini" @click="openUserEdit(u)">编辑</button>
              <button class="mini" @click="openUserRoles(u)">角色</button>
              <button class="mini" @click="resetPassword(u)">重置密码</button>
            </td>
          </tr>
          <tr v-if="users.length === 0">
            <td colspan="6" class="muted">暂无用户</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="grid">
      <div class="panel">
        <h3>角色管理</h3>
        <div class="form-grid">
          <input v-model.trim="newRole.roleCode" class="input" placeholder="角色编码，例如 ADMIN" />
          <input v-model.trim="newRole.roleName" class="input" placeholder="角色名称" />
          <select v-model="newRole.status" class="input">
            <option value="ENABLED">启用</option>
            <option value="DISABLED">禁用</option>
          </select>
        </div>
        <div class="row">
          <button class="primary" :disabled="loading || !newRole.roleCode || !newRole.roleName" @click="createRole">
            创建角色
          </button>
        </div>
        <ul class="list">
          <li v-for="r in roles" :key="r.id">
            <span>{{ r.roleCode }} - {{ r.roleName }} ({{ statusLabel(r.status) }})</span>
            <span class="inline-actions">
              <button class="mini" @click="openRoleEdit(r)">编辑</button>
              <button class="mini" @click="openRolePerms(r)">权限</button>
            </span>
          </li>
          <li v-if="roles.length === 0" class="muted">暂无角色</li>
        </ul>
      </div>

      <div class="panel">
        <h3>权限管理</h3>
        <div class="form-grid">
          <input v-model.trim="newPerm.permCode" class="input" placeholder="权限编码，例如 SYSTEM:MANAGE" />
          <input v-model.trim="newPerm.permName" class="input" placeholder="权限名称" />
          <select v-model="newPerm.status" class="input">
            <option value="ENABLED">启用</option>
            <option value="DISABLED">禁用</option>
          </select>
        </div>
        <div class="row">
          <button
            class="primary"
            :disabled="loading || !newPerm.permCode || !newPerm.permName"
            @click="createPermission"
          >
            创建权限
          </button>
        </div>
        <ul class="list">
          <li v-for="p in permissions" :key="p.id">
            <span>{{ p.permCode }} - {{ p.permName }} ({{ statusLabel(p.status) }})</span>
            <button class="mini" @click="openPermEdit(p)">编辑</button>
          </li>
          <li v-if="permissions.length === 0" class="muted">暂无权限</li>
        </ul>
      </div>
    </section>

    <section v-if="editingUser" class="panel">
      <h3>编辑用户 {{ editingUser.username }}</h3>
      <div class="form-grid">
        <input v-model.trim="editUserForm.displayName" class="input" placeholder="显示名称" />
        <select v-model="editUserForm.status" class="input">
          <option value="ENABLED">启用</option>
          <option value="DISABLED">禁用</option>
        </select>
      </div>
      <div class="row">
        <button class="primary" :disabled="loading" @click="saveUserEdit">保存</button>
        <button class="ghost" :disabled="loading" @click="editingUser = null">取消</button>
      </div>
    </section>

    <section v-if="bindingUser" class="panel">
      <h3>为 {{ bindingUser.username }} 绑定角色</h3>
      <div class="checkbox-row">
        <label v-for="r in roles" :key="`bur-${r.id}`" class="check-item">
          <input v-model="bindUserRoleIds" type="checkbox" :value="r.id" />
          {{ r.roleCode }} - {{ r.roleName }}
        </label>
      </div>
      <div class="row">
        <button class="primary" :disabled="loading" @click="saveUserRoles">保存角色</button>
        <button class="ghost" :disabled="loading" @click="bindingUser = null">取消</button>
      </div>
    </section>

    <section v-if="editingRole" class="panel">
      <h3>编辑角色 {{ editingRole.roleCode }}</h3>
      <div class="form-grid">
        <input v-model.trim="editRoleForm.roleName" class="input" placeholder="角色名称" />
        <select v-model="editRoleForm.status" class="input">
          <option value="ENABLED">启用</option>
          <option value="DISABLED">禁用</option>
        </select>
      </div>
      <div class="row">
        <button class="primary" :disabled="loading" @click="saveRoleEdit">保存</button>
        <button class="ghost" :disabled="loading" @click="editingRole = null">取消</button>
      </div>
    </section>

    <section v-if="bindingRole" class="panel">
      <h3>为 {{ bindingRole.roleCode }} 绑定权限</h3>
      <div class="checkbox-row">
        <label v-for="p in permissions" :key="`brp-${p.id}`" class="check-item">
          <input v-model="bindRolePermIds" type="checkbox" :value="p.id" />
          {{ p.permCode }} - {{ p.permName }}
        </label>
      </div>
      <div class="row">
        <button class="primary" :disabled="loading" @click="saveRolePerms">保存权限</button>
        <button class="ghost" :disabled="loading" @click="bindingRole = null">取消</button>
      </div>
    </section>

    <section v-if="editingPerm" class="panel">
      <h3>编辑权限 {{ editingPerm.permCode }}</h3>
      <div class="form-grid">
        <input v-model.trim="editPermForm.permName" class="input" placeholder="权限名称" />
        <select v-model="editPermForm.status" class="input">
          <option value="ENABLED">启用</option>
          <option value="DISABLED">禁用</option>
        </select>
      </div>
      <div class="row">
        <button class="primary" :disabled="loading" @click="savePermEdit">保存</button>
        <button class="ghost" :disabled="loading" @click="editingPerm = null">取消</button>
      </div>
    </section>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="success" class="success">{{ success }}</p>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'

type ApiResponse<T> = {
  code: number
  message: string
  data: T
  traceId: string
}

type UserItem = { id: number; username: string; displayName: string; status: string; roleCodes: string[] }
type RoleItem = { id: number; roleCode: string; roleName: string; status: string }
type PermItem = { id: number; permCode: string; permName: string; status: string }

const router = useRouter()
const loading = ref(false)
const error = ref('')
const success = ref('')
const users = ref<UserItem[]>([])
const roles = ref<RoleItem[]>([])
const permissions = ref<PermItem[]>([])

const newUser = ref({
  username: '',
  password: '',
  displayName: '',
  status: 'ENABLED',
  roleIds: [] as number[]
})

const newRole = ref({
  roleCode: '',
  roleName: '',
  status: 'ENABLED'
})

const newPerm = ref({
  permCode: '',
  permName: '',
  status: 'ENABLED'
})

const editingUser = ref<UserItem | null>(null)
const editUserForm = ref({
  displayName: '',
  status: 'ENABLED'
})

const bindingUser = ref<UserItem | null>(null)
const bindUserRoleIds = ref<number[]>([])

const editingRole = ref<RoleItem | null>(null)
const editRoleForm = ref({
  roleName: '',
  status: 'ENABLED'
})

const bindingRole = ref<RoleItem | null>(null)
const bindRolePermIds = ref<number[]>([])

const editingPerm = ref<PermItem | null>(null)
const editPermForm = ref({
  permName: '',
  status: 'ENABLED'
})

function statusLabel(value?: string) {
  if (value === 'ENABLED') return '启用'
  if (value === 'DISABLED') return '禁用'
  return value || '-'
}

function clearTips() {
  error.value = ''
  success.value = ''
}

function showError(e: any, fallback: string) {
  error.value = e?.response?.data?.message || e?.message || fallback
}

async function loadAll() {
  loading.value = true
  clearTips()
  try {
    const [u, r, p] = await Promise.all([
      axios.get<ApiResponse<UserItem[]>>('/api/system/users'),
      axios.get<ApiResponse<RoleItem[]>>('/api/system/roles'),
      axios.get<ApiResponse<PermItem[]>>('/api/system/permissions')
    ])
    users.value = u.data.data || []
    roles.value = r.data.data || []
    permissions.value = p.data.data || []
  } catch (e: any) {
    showError(e, '加载系统管理数据失败。')
  } finally {
    loading.value = false
  }
}

async function createUser() {
  loading.value = true
  clearTips()
  try {
    await axios.post('/api/system/users', newUser.value)
    success.value = '用户创建成功。'
    newUser.value = { username: '', password: '', displayName: '', status: 'ENABLED', roleIds: [] }
    await loadAll()
  } catch (e: any) {
    showError(e, '创建用户失败。')
    loading.value = false
  }
}

function openUserEdit(u: UserItem) {
  editingUser.value = u
  editUserForm.value = {
    displayName: u.displayName || '',
    status: u.status || 'ENABLED'
  }
}

async function saveUserEdit() {
  if (!editingUser.value) return
  loading.value = true
  clearTips()
  try {
    await axios.put(`/api/system/users/${editingUser.value.id}`, editUserForm.value)
    success.value = '用户更新成功。'
    editingUser.value = null
    await loadAll()
  } catch (e: any) {
    showError(e, '更新用户失败。')
    loading.value = false
  }
}

function openUserRoles(u: UserItem) {
  bindingUser.value = u
  const roleCodes = new Set(u.roleCodes || [])
  bindUserRoleIds.value = roles.value.filter((r) => roleCodes.has(r.roleCode)).map((r) => r.id)
}

async function saveUserRoles() {
  if (!bindingUser.value) return
  loading.value = true
  clearTips()
  try {
    await axios.post(`/api/system/users/${bindingUser.value.id}/roles`, {
      roleIds: bindUserRoleIds.value
    })
    success.value = '用户角色保存成功。'
    bindingUser.value = null
    await loadAll()
  } catch (e: any) {
    showError(e, '保存用户角色失败。')
    loading.value = false
  }
}

async function resetPassword(u: UserItem) {
  const pwd = window.prompt(`请为 ${u.username} 输入新密码`)
  if (!pwd) return
  loading.value = true
  clearTips()
  try {
    await axios.put(`/api/system/users/${u.id}/password`, { newPassword: pwd })
    success.value = '密码重置成功。'
  } catch (e: any) {
    showError(e, '重置密码失败。')
  } finally {
    loading.value = false
  }
}

async function createRole() {
  loading.value = true
  clearTips()
  try {
    await axios.post('/api/system/roles', newRole.value)
    success.value = '角色创建成功。'
    newRole.value = { roleCode: '', roleName: '', status: 'ENABLED' }
    await loadAll()
  } catch (e: any) {
    showError(e, '创建角色失败。')
    loading.value = false
  }
}

function openRoleEdit(r: RoleItem) {
  editingRole.value = r
  editRoleForm.value = {
    roleName: r.roleName || '',
    status: r.status || 'ENABLED'
  }
}

async function saveRoleEdit() {
  if (!editingRole.value) return
  loading.value = true
  clearTips()
  try {
    await axios.put(`/api/system/roles/${editingRole.value.id}`, editRoleForm.value)
    success.value = '角色更新成功。'
    editingRole.value = null
    await loadAll()
  } catch (e: any) {
    showError(e, '更新角色失败。')
    loading.value = false
  }
}

function openRolePerms(r: RoleItem) {
  bindingRole.value = r
  bindRolePermIds.value = []
}

async function saveRolePerms() {
  if (!bindingRole.value) return
  loading.value = true
  clearTips()
  try {
    await axios.post(`/api/system/roles/${bindingRole.value.id}/permissions`, {
      permissionIds: bindRolePermIds.value
    })
    success.value = '角色权限保存成功。'
    bindingRole.value = null
    await loadAll()
  } catch (e: any) {
    showError(e, '保存角色权限失败。')
    loading.value = false
  }
}

async function createPermission() {
  loading.value = true
  clearTips()
  try {
    await axios.post('/api/system/permissions', newPerm.value)
    success.value = '权限创建成功。'
    newPerm.value = { permCode: '', permName: '', status: 'ENABLED' }
    await loadAll()
  } catch (e: any) {
    showError(e, '创建权限失败。')
    loading.value = false
  }
}

function openPermEdit(p: PermItem) {
  editingPerm.value = p
  editPermForm.value = {
    permName: p.permName || '',
    status: p.status || 'ENABLED'
  }
}

async function savePermEdit() {
  if (!editingPerm.value) return
  loading.value = true
  clearTips()
  try {
    await axios.put(`/api/system/permissions/${editingPerm.value.id}`, editPermForm.value)
    success.value = '权限更新成功。'
    editingPerm.value = null
    await loadAll()
  } catch (e: any) {
    showError(e, '更新权限失败。')
    loading.value = false
  }
}

function goCreateAi() {
  router.push('/projects/create-ai')
}

function goAiDocgen() {
  router.push('/docgen')
}

onMounted(loadAll)
</script>

<style scoped>
.container {
  max-width: 1200px;
  margin: 24px auto;
  padding: 0 16px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, "PingFang SC", "Hiragino Sans GB",
    "Microsoft YaHei", sans-serif;
}

.row {
  display: flex;
  gap: 10px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-top: 8px;
}

.checkbox-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.check-item {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 6px 8px;
  font-size: 13px;
}

.input {
  width: 100%;
  box-sizing: border-box;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #d1d5db;
}

.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 14px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.panel-head h3,
.panel h3 {
  margin: 0;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 12px;
}

.table th,
.table td {
  border: 1px solid #e5e7eb;
  padding: 8px;
  font-size: 13px;
}

.list {
  margin: 12px 0 0;
  padding-left: 18px;
}

.list li {
  margin-bottom: 8px;
}

.inline-actions {
  margin-left: 8px;
  display: inline-flex;
  gap: 8px;
}

.primary,
.ghost,
.mini {
  border-radius: 8px;
  border: 1px solid #d1d5db;
  padding: 8px 12px;
  cursor: pointer;
}

.primary {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}

.ghost,
.mini {
  background: #f3f4f6;
}

.mini {
  padding: 5px 9px;
  font-size: 12px;
}

.muted {
  color: #6b7280;
}

.error {
  color: #b91c1c;
  margin-top: 10px;
}

.success {
  color: #166534;
  margin-top: 10px;
}

@media (max-width: 980px) {
  .grid,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
