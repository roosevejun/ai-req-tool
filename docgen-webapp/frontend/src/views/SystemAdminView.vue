<template>
  <div class="container">
    <section class="panel">
      <div class="panel-head">
        <h3>閻劍鍩涚粻锛勬倞</h3>
        <button class="ghost" :disabled="loading" @click="loadAll">閸掗攱鏌?/button>
      </div>

      <div class="form-grid">
        <input v-model.trim="newUser.username" class="input" placeholder="閻劍鍩涢崥? />
        <input v-model="newUser.password" type="password" class="input" placeholder="閸掓繂顫愮€靛棛鐖? />
        <input v-model.trim="newUser.displayName" class="input" placeholder="閺勫墽銇氶崥? />
        <select v-model="newUser.status" class="input">
          <option value="ENABLED">ENABLED</option>
          <option value="DISABLED">DISABLED</option>
        </select>
      </div>
      <div class="checkbox-row">
        <label v-for="r in roles" :key="`nu-${r.id}`" class="check-item">
          <input type="checkbox" :value="r.id" v-model="newUser.roleIds" />
          {{ r.roleCode }}
        </label>
      </div>
      <div class="row">
        <button class="primary" :disabled="loading || !newUser.username || !newUser.password" @click="createUser">
          閺傛澘缂撻悽銊﹀煕
        </button>
      </div>

      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>閻劍鍩涢崥?/th>
            <th>閺勫墽銇氶崥?/th>
            <th>閻樿埖鈧?/th>
            <th>鐟欐帟澹?/th>
            <th>閹垮秳缍?/th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.id }}</td>
            <td>{{ u.username }}</td>
            <td>{{ u.displayName }}</td>
            <td>{{ u.status }}</td>
            <td>{{ (u.roleCodes || []).join(', ') }}</td>
            <td>
              <button class="mini" @click="openUserEdit(u)">缂傛牞绶?/button>
              <button class="mini" @click="openUserRoles(u)">閸掑棝鍘ょ憴鎺曞</button>
              <button class="mini" @click="resetPassword(u)">闁插秶鐤嗙€靛棛鐖?/button>
            </td>
          </tr>
          <tr v-if="users.length === 0">
            <td colspan="6" class="muted">閺嗗倹妫ら弫鐗堝祦</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="grid">
      <div class="panel">
        <h3>鐟欐帟澹婄粻锛勬倞</h3>
        <div class="form-grid">
          <input v-model.trim="newRole.roleCode" class="input" placeholder="鐟欐帟澹婄紓鏍垳閿涘苯顩?ADMIN" />
          <input v-model.trim="newRole.roleName" class="input" placeholder="鐟欐帟澹婇崥宥囆? />
          <select v-model="newRole.status" class="input">
            <option value="ENABLED">ENABLED</option>
            <option value="DISABLED">DISABLED</option>
          </select>
        </div>
        <div class="row">
          <button class="primary" :disabled="loading || !newRole.roleCode || !newRole.roleName" @click="createRole">
            閺傛澘缂撶憴鎺曞
          </button>
        </div>
        <ul class="list">
          <li v-for="r in roles" :key="r.id">
            <span>{{ r.roleCode }} - {{ r.roleName }} ({{ r.status }})</span>
            <span class="inline-actions">
              <button class="mini" @click="openRoleEdit(r)">缂傛牞绶?/button>
              <button class="mini" @click="openRolePerms(r)">閸掑棝鍘ら弶鍐</button>
            </span>
          </li>
          <li v-if="roles.length === 0" class="muted">閺嗗倹妫ら弫鐗堝祦</li>
        </ul>
      </div>

      <div class="panel">
        <h3>閺夊啴妾虹粻锛勬倞</h3>
        <div class="form-grid">
          <input v-model.trim="newPerm.permCode" class="input" placeholder="閺夊啴妾虹紓鏍垳閿涘苯顩?SYSTEM:MANAGE" />
          <input v-model.trim="newPerm.permName" class="input" placeholder="閺夊啴妾洪崥宥囆? />
          <select v-model="newPerm.status" class="input">
            <option value="ENABLED">ENABLED</option>
            <option value="DISABLED">DISABLED</option>
          </select>
        </div>
        <div class="row">
          <button class="primary" :disabled="loading || !newPerm.permCode || !newPerm.permName" @click="createPermission">
            閺傛澘缂撻弶鍐
          </button>
        </div>
        <ul class="list">
          <li v-for="p in permissions" :key="p.id">
            <span>{{ p.permCode }} - {{ p.permName }} ({{ p.status }})</span>
            <button class="mini" @click="openPermEdit(p)">缂傛牞绶?/button>
          </li>
          <li v-if="permissions.length === 0" class="muted">閺嗗倹妫ら弫鐗堝祦</li>
        </ul>
      </div>
    </section>

    <section v-if="editingUser" class="panel">
      <h3>缂傛牞绶悽銊﹀煕閿涙{ editingUser.username }}</h3>
      <div class="form-grid">
        <input v-model.trim="editUserForm.displayName" class="input" placeholder="閺勫墽銇氶崥? />
        <select v-model="editUserForm.status" class="input">
          <option value="ENABLED">ENABLED</option>
          <option value="DISABLED">DISABLED</option>
        </select>
      </div>
      <div class="row">
        <button class="primary" :disabled="loading" @click="saveUserEdit">娣囨繂鐡?/button>
        <button class="ghost" :disabled="loading" @click="editingUser = null">閸欐牗绉?/button>
      </div>
    </section>

    <section v-if="bindingUser" class="panel">
      <h3>閸掑棝鍘ょ憴鎺曞閿涙{ bindingUser.username }}</h3>
      <div class="checkbox-row">
        <label v-for="r in roles" :key="`bur-${r.id}`" class="check-item">
          <input type="checkbox" :value="r.id" v-model="bindUserRoleIds" />
          {{ r.roleCode }} - {{ r.roleName }}
        </label>
      </div>
      <div class="row">
        <button class="primary" :disabled="loading" @click="saveUserRoles">娣囨繂鐡ㄧ憴鎺曞</button>
        <button class="ghost" :disabled="loading" @click="bindingUser = null">閸欐牗绉?/button>
      </div>
    </section>

    <section v-if="editingRole" class="panel">
      <h3>缂傛牞绶憴鎺曞閿涙{ editingRole.roleCode }}</h3>
      <div class="form-grid">
        <input v-model.trim="editRoleForm.roleName" class="input" placeholder="鐟欐帟澹婇崥宥囆? />
        <select v-model="editRoleForm.status" class="input">
          <option value="ENABLED">ENABLED</option>
          <option value="DISABLED">DISABLED</option>
        </select>
      </div>
      <div class="row">
        <button class="primary" :disabled="loading" @click="saveRoleEdit">娣囨繂鐡?/button>
        <button class="ghost" :disabled="loading" @click="editingRole = null">閸欐牗绉?/button>
      </div>
    </section>

    <section v-if="bindingRole" class="panel">
      <h3>閸掑棝鍘ら弶鍐閿涙{ bindingRole.roleCode }}</h3>
      <div class="checkbox-row">
        <label v-for="p in permissions" :key="`brp-${p.id}`" class="check-item">
          <input type="checkbox" :value="p.id" v-model="bindRolePermIds" />
          {{ p.permCode }} - {{ p.permName }}
        </label>
      </div>
      <div class="row">
        <button class="primary" :disabled="loading" @click="saveRolePerms">娣囨繂鐡ㄩ弶鍐</button>
        <button class="ghost" :disabled="loading" @click="bindingRole = null">閸欐牗绉?/button>
      </div>
    </section>

    <section v-if="editingPerm" class="panel">
      <h3>缂傛牞绶弶鍐閿涙{ editingPerm.permCode }}</h3>
      <div class="form-grid">
        <input v-model.trim="editPermForm.permName" class="input" placeholder="閺夊啴妾洪崥宥囆? />
        <select v-model="editPermForm.status" class="input">
          <option value="ENABLED">ENABLED</option>
          <option value="DISABLED">DISABLED</option>
        </select>
      </div>
      <div class="row">
        <button class="primary" :disabled="loading" @click="savePermEdit">娣囨繂鐡?/button>
        <button class="ghost" :disabled="loading" @click="editingPerm = null">閸欐牗绉?/button>
      </div>
    </section>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="success" class="success">{{ success }}</p>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import axios from 'axios'

type ApiResponse<T> = {
  code: number
  message: string
  data: T
  traceId: string
}

type UserItem = { id: number; username: string; displayName: string; status: string; roleCodes: string[] }
type RoleItem = { id: number; roleCode: string; roleName: string; status: string }
type PermItem = { id: number; permCode: string; permName: string; status: string }

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
    showError(e, '閸旂姾娴囨径杈Е')
  } finally {
    loading.value = false
  }
}

async function createUser() {
  loading.value = true
  clearTips()
  try {
    await axios.post('/api/system/users', newUser.value)
    success.value = '閻劍鍩涢崚娑樼紦閹存劕濮?
    newUser.value = { username: '', password: '', displayName: '', status: 'ENABLED', roleIds: [] }
    await loadAll()
  } catch (e: any) {
    showError(e, '閸掓稑缂撻悽銊﹀煕婢惰精瑙?)
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
    success.value = '閻劍鍩涢弴瀛樻煀閹存劕濮?
    editingUser.value = null
    await loadAll()
  } catch (e: any) {
    showError(e, '閺囧瓨鏌婇悽銊﹀煕婢惰精瑙?)
    loading.value = false
  }
}

function openUserRoles(u: UserItem) {
  bindingUser.value = u
  const roleCodes = new Set(u.roleCodes || [])
  bindUserRoleIds.value = roles.value.filter(r => roleCodes.has(r.roleCode)).map(r => r.id)
}

async function saveUserRoles() {
  if (!bindingUser.value) return
  loading.value = true
  clearTips()
  try {
    await axios.post(`/api/system/users/${bindingUser.value.id}/roles`, {
      roleIds: bindUserRoleIds.value
    })
    success.value = '閻劍鍩涚憴鎺曞缂佹垵鐣鹃幋鎰'
    bindingUser.value = null
    await loadAll()
  } catch (e: any) {
    showError(e, '缂佹垵鐣剧憴鎺曞婢惰精瑙?)
    loading.value = false
  }
}

async function resetPassword(u: UserItem) {
  const pwd = window.prompt(`鐠囩柉绶崗銉ф暏閹?${u.username} 閻ㄥ嫭鏌婄€靛棛鐖滈敍姝?
  if (!pwd) return
  loading.value = true
  clearTips()
  try {
    await axios.put(`/api/system/users/${u.id}/password`, { newPassword: pwd })
    success.value = '鐎靛棛鐖滈柌宥囩枂閹存劕濮?
  } catch (e: any) {
    showError(e, '闁插秶鐤嗙€靛棛鐖滄径杈Е')
  } finally {
    loading.value = false
  }
}

async function createRole() {
  loading.value = true
  clearTips()
  try {
    await axios.post('/api/system/roles', newRole.value)
    success.value = '鐟欐帟澹婇崚娑樼紦閹存劕濮?
    newRole.value = { roleCode: '', roleName: '', status: 'ENABLED' }
    await loadAll()
  } catch (e: any) {
    showError(e, '閸掓稑缂撶憴鎺曞婢惰精瑙?)
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
    success.value = '鐟欐帟澹婇弴瀛樻煀閹存劕濮?
    editingRole.value = null
    await loadAll()
  } catch (e: any) {
    showError(e, '閺囧瓨鏌婄憴鎺曞婢惰精瑙?)
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
    success.value = '鐟欐帟澹婇弶鍐缂佹垵鐣鹃幋鎰'
    bindingRole.value = null
    await loadAll()
  } catch (e: any) {
    showError(e, '缂佹垵鐣鹃弶鍐婢惰精瑙?)
    loading.value = false
  }
}

async function createPermission() {
  loading.value = true
  clearTips()
  try {
    await axios.post('/api/system/permissions', newPerm.value)
    success.value = '閺夊啴妾洪崚娑樼紦閹存劕濮?
    newPerm.value = { permCode: '', permName: '', status: 'ENABLED' }
    await loadAll()
  } catch (e: any) {
    showError(e, '閸掓稑缂撻弶鍐婢惰精瑙?)
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
    success.value = '閺夊啴妾洪弴瀛樻煀閹存劕濮?
    editingPerm.value = null
    await loadAll()
  } catch (e: any) {
    showError(e, '閺囧瓨鏌婇弶鍐婢惰精瑙?)
    loading.value = false
  }
}
`r`nonMounted(loadAll)
</script>

<style scoped>
.container {
  max-width: 1200px;
  margin: 24px auto;
  padding: 0 16px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, "PingFang SC", "Hiragino Sans GB",
    "Microsoft YaHei", sans-serif;
}
`r`n.row {
  display: flex;
  gap: 10px;
  margin-top: 10px;
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
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 8px;
}

.ghost,
.primary,
.mini {
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  padding: 8px 12px;
  cursor: pointer;
}

.primary {
  background: #2563eb;
  border-color: #2563eb;
  color: #fff;
}

.mini {
  padding: 4px 8px;
  font-size: 12px;
  margin-right: 6px;
}

.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 12px;
  margin-top: 12px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 8px;
}

.table th,
.table td {
  border: 1px solid #e5e7eb;
  padding: 8px;
  font-size: 13px;
}

.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.list {
  list-style: none;
  margin: 10px 0 0 0;
  padding: 0;
}

.list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px dashed #e5e7eb;
  padding: 8px 0;
}

.inline-actions {
  display: inline-flex;
  gap: 6px;
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
  .form-grid {
    grid-template-columns: 1fr 1fr;
  }
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>

