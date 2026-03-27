<template>
  <section v-if="selectedProjectId" class="card">
    <h3>项目 {{ selectedProjectId }} 的需求</h3>
    <table class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>编号</th>
          <th>标题</th>
          <th>优先级</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="requirement in requirements" :key="requirement.id">
          <td>{{ requirement.id }}</td>
          <td>{{ requirement.requirementNo }}</td>
          <td>{{ requirement.title }}</td>
          <td>{{ priorityLabel(requirement.priority) }}</td>
          <td>{{ requirementStatusLabel(requirement.status) }}</td>
          <td class="ops">
            <button class="mini" @click="$emit('select-requirement', selectedProjectId, requirement.id)">查看</button>
            <button class="mini" @click="$emit('open-workbench', requirement.id)">工作台</button>
            <button class="mini" @click="$emit('open-versions', requirement.id)">版本页</button>
          </td>
        </tr>
        <tr v-if="requirements.length === 0">
          <td colspan="6" class="empty small">当前项目下暂无需求</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup lang="ts">
import { priorityLabel, requirementStatusLabel } from './helpers'
import type { RequirementItem } from './types'

defineProps<{
  selectedProjectId: number | null
  requirements: RequirementItem[]
}>()

defineEmits<{
  (event: 'select-requirement', projectId: number, requirementId: number): void
  (event: 'open-workbench', requirementId: number): void
  (event: 'open-versions', requirementId: number): void
}>()
</script>

<style scoped>
.card { background: #fff; border: 1px solid #dbe2ea; border-radius: 12px; padding: 12px; margin-bottom: 12px; }
.table { width: 100%; border-collapse: collapse; margin-top: 8px; }
.table th, .table td { border: 1px solid #e5e7eb; padding: 8px; font-size: 13px; }
.ops { display: flex; gap: 8px; flex-wrap: wrap; }
.mini { border-radius: 8px; border: 1px solid #d1d5db; padding: 5px 9px; cursor: pointer; font-size: 12px; background: #f3f4f6; }
.empty { color: #6b7280; }
.small { font-size: 12px; }
</style>
