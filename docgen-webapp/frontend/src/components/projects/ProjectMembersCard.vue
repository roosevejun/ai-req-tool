<template>
  <section class="card">
    <h3>项目成员管理</h3>
    <div class="form-grid">
      <select v-model="memberForm.selectedUserId" class="input">
        <option value="">从用户列表选择，可选</option>
        <option v-for="u in userOptions" :key="u.id" :value="String(u.id)">
          {{ u.displayName || u.username }} ({{ u.id }})
        </option>
      </select>
      <input v-model.trim="memberForm.userId" class="input" placeholder="用户 ID，例如 1" />
      <select v-model="memberForm.projectRole" class="input">
        <option value="OWNER">负责人</option>
        <option value="PM">产品经理</option>
        <option value="DEV">开发</option>
        <option value="QA">测试</option>
        <option value="VIEWER">只读</option>
      </select>
      <button class="primary" :disabled="loading || !(memberForm.selectedUserId || memberForm.userId)" @click="$emit('add-member')">
        添加成员
      </button>
    </div>

    <table class="table">
      <thead>
        <tr>
          <th>用户 ID</th>
          <th>用户名</th>
          <th>显示名称</th>
          <th>项目角色</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="m in members" :key="m.id">
          <td>{{ m.userId }}</td>
          <td>{{ m.username || '-' }}</td>
          <td>{{ m.displayName || '-' }}</td>
          <td>{{ projectRoleLabel(m.projectRole) }}</td>
          <td>{{ memberStatusLabel(m.status) }}</td>
          <td class="ops">
            <button class="mini" @click="$emit('remove-member', m.userId)">移除</button>
          </td>
        </tr>
        <tr v-if="members.length === 0">
          <td colspan="6" class="empty small">暂无项目成员</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup lang="ts">
import type { MemberFormState, ProjectMemberItem, UserOption } from './types'

defineProps<{
  loading: boolean
  memberForm: MemberFormState
  userOptions: UserOption[]
  members: ProjectMemberItem[]
  projectRoleLabel: (value?: string) => string
  memberStatusLabel: (value?: string) => string
}>()

defineEmits<{
  (event: 'add-member'): void
  (event: 'remove-member', userId: number): void
}>()
</script>

<style scoped>
.card {
  background: #fff;
  border: 1px solid #dbe2ea;
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 12px;
}
.form-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr auto;
  gap: 10px;
}
.input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 8px 10px;
  margin-top: 8px;
  background: #fff;
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
.ops {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.primary,
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
.mini {
  padding: 5px 9px;
  font-size: 12px;
  background: #f3f4f6;
}
.empty {
  color: #6b7280;
}
.small {
  font-size: 12px;
}
@media (max-width: 980px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
