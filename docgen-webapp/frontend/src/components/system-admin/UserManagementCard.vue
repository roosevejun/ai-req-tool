<template>
  <section class="panel">
    <div class="panel-head">
      <div>
        <h3>用户管理</h3>
        <p class="copy">创建、编辑用户，并维护账号状态和角色绑定。</p>
      </div>
      <button class="ghost" :disabled="loading" @click="$emit('refresh')">刷新</button>
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
      <label v-for="role in roles" :key="`new-user-role-${role.id}`" class="check-item">
        <input v-model="newUser.roleIds" type="checkbox" :value="role.id" />
        {{ role.roleCode }}
      </label>
    </div>

    <div class="actions">
      <button class="primary" :disabled="loading || !newUser.username || !newUser.password" @click="$emit('create-user')">
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
        <tr v-for="user in users" :key="user.id">
          <td>{{ user.id }}</td>
          <td>{{ user.username }}</td>
          <td>{{ user.displayName || '-' }}</td>
          <td>{{ statusLabel(user.status) }}</td>
          <td>{{ (user.roleCodes || []).join(', ') || '-' }}</td>
          <td class="table-actions">
            <button class="mini" @click="$emit('open-user-edit', user)">编辑</button>
            <button class="mini" @click="$emit('open-user-roles', user)">绑定角色</button>
            <button class="mini" @click="$emit('reset-password', user)">重置密码</button>
          </td>
        </tr>
        <tr v-if="users.length === 0">
          <td colspan="6" class="muted">暂无用户数据。</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup lang="ts">
import { statusLabel } from './helpers'
import type { NewUserForm, RoleItem, UserItem } from './types'

defineProps<{
  loading: boolean
  newUser: NewUserForm
  roles: RoleItem[]
  users: UserItem[]
}>()

defineEmits<{
  (event: 'refresh'): void
  (event: 'create-user'): void
  (event: 'open-user-edit', user: UserItem): void
  (event: 'open-user-roles', user: UserItem): void
  (event: 'reset-password', user: UserItem): void
}>()
</script>

<style scoped>
.panel {
  background: #fff;
  border: 1px solid #dbe2ea;
  border-radius: 18px;
  padding: 18px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.panel-head h3 {
  margin: 0;
  font-size: 22px;
  color: #0f172a;
}

.copy {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.6;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.checkbox-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.check-item {
  border: 1px solid #dbe2ea;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  color: #334155;
}

.input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
  background: #fff;
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}

.table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 16px;
}

.table th,
.table td {
  border: 1px solid #e5edf5;
  padding: 10px;
  font-size: 13px;
  text-align: left;
  vertical-align: top;
}

.table th {
  background: #f8fafc;
  color: #334155;
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.primary,
.ghost,
.mini {
  border-radius: 10px;
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
  background: #f8fafc;
}

.mini {
  padding: 6px 10px;
  font-size: 12px;
}

.muted {
  color: #64748b;
  text-align: center;
}

@media (max-width: 980px) {
  .panel-head {
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
