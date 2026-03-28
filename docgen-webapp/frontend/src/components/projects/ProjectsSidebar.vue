<template>
  <aside class="sidebar">
    <section class="summary-card">
      <p class="eyebrow">项目管理中心</p>
      <h3>项目概览</h3>
      <div class="stat-grid">
        <div class="stat-item">
          <span>项目总数</span>
          <strong>{{ projectCount }}</strong>
        </div>
        <div class="stat-item">
          <span>当前项目需求</span>
          <strong>{{ requirementCount }}</strong>
        </div>
        <div class="stat-item">
          <span>待处理知识任务</span>
          <strong>{{ pendingKnowledgeCount }}</strong>
        </div>
      </div>
    </section>

    <ProjectTreeCard
      :loading="loading"
      :projects="projects"
      :selected-project-id="selectedProjectId"
      @reload-projects="$emit('reload-projects')"
      @select-project="$emit('select-project', $event)"
    />
  </aside>
</template>

<script setup lang="ts">
import ProjectTreeCard from './ProjectTreeCard.vue'
import type { ProjectItem } from './types'

defineProps<{
  loading: boolean
  projects: ProjectItem[]
  selectedProjectId: number | null
  projectCount: number
  requirementCount: number
  pendingKnowledgeCount: number
}>()

defineEmits<{
  (event: 'reload-projects'): void
  (event: 'select-project', projectId: number): void
}>()
</script>

<style scoped>
.sidebar {
  min-height: 70vh;
  display: grid;
  gap: 14px;
  align-content: start;
}

.summary-card {
  padding: 14px;
  background: #ffffff;
  border: 1px solid #d4dde8;
  border-top: 4px solid #1d4ed8;
  border-radius: 10px;
}

.eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #1d4ed8;
  font-weight: 700;
}

h3 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

.stat-grid {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.stat-item {
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.stat-item span {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.stat-item strong {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  font-size: 22px;
}

</style>
