<template>
  <section class="panel">
    <div class="panel-head">
      <h3>用户管理</h3>
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
      <label v-for="r in roles" :key="`nu-${r.id}`" class="check-item">
        <input v-model="newUser.roleIds" type="checkbox" :value="r.id" />
        {{ r.roleCode }}
      </label>
    </div>
    <div class="row">
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
        <tr v-for="u in users" :key="u.id">
          <td>{{ u.id }}</td>
          <td>{{ u.username }}</td>
          <td>{{ u.displayName }}</td>
          <td>{{ statusLabel(u.status) }}</td>
          <td>{{ (u.roleCodes || []).join(', ') }}</td>
          <td>
            <button class="mini" @click="$emit('open-user-edit', u)">编辑</button>
            <button class="mini" @click="$emit('open-user-roles', u)">角色</button>
            <button class="mini" @click="$emit('reset-password', u)">重置密码</button>
          </td>
        </tr>
        <tr v-if="users.length === 0">
          <td colspan="6" class="muted">暂无用户</td>
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
.panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 14px; margin-bottom: 14px; }
.panel-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.panel-head h3 { margin: 0; }
.row { display: flex; gap: 10px; margin-top: 10px; flex-wrap: wrap; }
.form-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-top: 8px; }
.checkbox-row { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.check-item { border: 1px solid #e5e7eb; border-radius: 8px; padding: 6px 8px; font-size: 13px; }
.input { width: 100%; box-sizing: border-box; padding: 8px 10px; border-radius: 8px; border: 1px solid #d1d5db; }
.table { width: 100%; border-collapse: collapse; margin-top: 12px; }
.table th, .table td { border: 1px solid #e5e7eb; padding: 8px; font-size: 13px; }
.primary, .ghost, .mini { border-radius: 8px; border: 1px solid #d1d5db; padding: 8px 12px; cursor: pointer; }
.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.ghost, .mini { background: #f3f4f6; }
.mini { padding: 5px 9px; font-size: 12px; }
.muted { color: #6b7280; }
@media (max-width: 980px) { .form-grid { grid-template-columns: 1fr; } }
</style>
