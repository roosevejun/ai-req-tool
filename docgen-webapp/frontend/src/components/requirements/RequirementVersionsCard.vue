<template>
  <section class="card">
    <div class="panel-head">
      <h3>版本列表</h3>
      <button class="ghost mini" :disabled="loading" @click="$emit('refresh')">刷新</button>
    </div>
    <table class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>版本号</th>
          <th>生成时间</th>
          <th>当前版本</th>
          <th>来源任务</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="version in versions" :key="version.id" :class="{ current: !!version.isCurrent }">
          <td>{{ version.id }}</td>
          <td>{{ version.versionNo }}</td>
          <td>{{ version.generatedAt || '-' }}</td>
          <td>{{ version.isCurrent ? '是' : '否' }}</td>
          <td>{{ version.sourceJobId || '-' }}</td>
          <td class="ops">
            <button class="mini" @click="$emit('select-version', version.id)">查看</button>
            <button class="mini" @click="$emit('download-version', version.id)">下载</button>
          </td>
        </tr>
        <tr v-if="versions.length === 0">
          <td colspan="6" class="empty small">暂无版本记录</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup lang="ts">
import type { RequirementVersionItem } from './types'

defineProps<{
  loading: boolean
  versions: RequirementVersionItem[]
}>()

defineEmits<{
  (event: 'refresh'): void
  (event: 'select-version', versionId: number): void
  (event: 'download-version', versionId: number): void
}>()
</script>

<style scoped>
.card { background: #fff; border: 1px solid #dbe2ea; border-radius: 12px; padding: 10px; margin-bottom: 12px; }
.panel-head { display: flex; justify-content: space-between; align-items: center; }
.table { width: 100%; border-collapse: collapse; margin-top: 8px; }
.table th, .table td { border: 1px solid #e5e7eb; padding: 8px; font-size: 13px; }
.ops { display: flex; gap: 8px; }
.current { background: #eff6ff; }
.empty { color: #6b7280; }
.small { font-size: 12px; }
.ghost, .mini { border-radius: 8px; border: 1px solid #d1d5db; background: #f3f4f6; cursor: pointer; }
.ghost { padding: 8px 12px; }
.mini { padding: 5px 9px; font-size: 12px; }
</style>
