<template>
  <section class="panel">
    <div class="section-head">
      <h3>需求列表</h3>
    </div>
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
            <button class="mini" @click="$emit('open-workbench', requirement.id)">AI 工作台</button>
            <button class="mini" @click="$emit('open-versions', requirement.id)">版本页</button>
          </td>
        </tr>
        <tr v-if="requirements.length === 0">
          <td colspan="6" class="muted">暂无需求</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup lang="ts">
import { priorityLabel, requirementStatusLabel } from './helpers'
import type { RequirementItem } from './types'

defineProps<{
  requirements: RequirementItem[]
}>()

defineEmits<{
  (event: 'open-workbench', requirementId: number): void
  (event: 'open-versions', requirementId: number): void
}>()
</script>

<style scoped>
.panel { background:#fff; border:1px solid #e5e7eb; border-radius:10px; padding:12px; margin-top:12px; }
.section-head { display:flex; justify-content:space-between; align-items:center; gap:12px; margin-bottom:8px; }
.section-head h3 { margin:0; }
.table { width:100%; border-collapse:collapse; margin-top:8px; }
.table th,.table td { border:1px solid #e5e7eb; padding:8px; font-size:13px; }
.ops { display:flex; gap:8px; }
.mini { border:1px solid #e5e7eb; border-radius:8px; padding:8px 12px; cursor:pointer; background:#f3f4f6; }
.muted { color:#6b7280; }
</style>
