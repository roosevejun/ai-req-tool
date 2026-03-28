<template>
  <aside class="sidebar">
    <section class="guide-card">
      <p class="eyebrow">业务功能导航</p>
      <h3>先选业务入口，再定位项目对象</h3>
      <p class="summary">左侧负责功能导航和项目定位，右侧负责查看当前项目的办理信息、需求情况和协同结果。</p>
      <div class="quick-actions">
        <button class="primary" type="button" @click="$emit('create-project')">选择创建方式</button>
        <button class="ghost" type="button" @click="$emit('create-ai')">AI 项目孵化</button>
        <button class="ghost" type="button" @click="$emit('go-docgen')">需求管理中心</button>
      </div>
    </section>

    <ProjectTreeCard
      :loading="loading"
      :projects="projects"
      :selected-project-id="selectedProjectId"
      :selected-requirement-id="selectedRequirementId"
      :is-expanded="isExpanded"
      :requirements-of="requirementsOf"
      :versions-of="versionsOf"
      @reload-projects="$emit('reload-projects')"
      @toggle-project="$emit('toggle-project', $event)"
      @select-project="$emit('select-project', $event)"
      @select-requirement="(projectId, requirementId) => $emit('select-requirement', projectId, requirementId)"
    />
  </aside>
</template>

<script setup lang="ts">
import ProjectTreeCard from './ProjectTreeCard.vue'
import type { ProjectItem, RequirementItem, VersionItem } from './types'

defineProps<{
  loading: boolean
  projects: ProjectItem[]
  selectedProjectId: number | null
  selectedRequirementId: number | null
  isExpanded: (projectId: number) => boolean
  requirementsOf: (projectId: number) => RequirementItem[]
  versionsOf: (requirementId: number) => VersionItem[]
}>()

defineEmits<{
  (event: 'reload-projects'): void
  (event: 'toggle-project', projectId: number): void
  (event: 'select-project', projectId: number): void
  (event: 'select-requirement', projectId: number, requirementId: number): void
  (event: 'create-project'): void
  (event: 'create-ai'): void
  (event: 'go-docgen'): void
}>()
</script>

<style scoped>
.sidebar {
  min-height: 70vh;
  display: grid;
  gap: 14px;
}
.guide-card {
  border: 1px solid #cfd9e8;
  border-radius: 12px;
  padding: 16px;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
}
.eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #0f4c81;
  font-weight: 700;
}
h3 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}
.summary {
  margin: 8px 0 0;
  color: #475569;
  line-height: 1.6;
  font-size: 13px;
}
.quick-actions {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}
.primary,
.ghost {
  width: 100%;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
  padding: 10px 12px;
  cursor: pointer;
  text-align: left;
}
.primary {
  background: #1d4ed8;
  border-color: #1d4ed8;
  color: #fff;
}
.ghost {
  background: #fff;
  color: #0f172a;
}
</style>
