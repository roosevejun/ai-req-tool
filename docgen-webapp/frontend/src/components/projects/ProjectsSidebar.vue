<template>
  <aside class="sidebar">
    <section class="nav-card">
      <p class="eyebrow">业务功能导航</p>
      <h3>项目管理业务入口</h3>
      <p class="summary">
        先选择办理入口，再定位项目对象。左侧负责入口与对象导航，右侧负责项目概况、事项推进与结果展示。
      </p>

      <div class="nav-group">
        <button class="nav-button nav-button--primary" type="button" @click="$emit('create-project')">
          选择创建方式
          <small>新建项目或进入 AI 项目孵化</small>
        </button>
        <button class="nav-button" type="button" @click="$emit('create-ai')">
          AI 项目孵化
          <small>通过对话提炼项目框架，再决定是否立项</small>
        </button>
        <button class="nav-button" type="button" @click="$emit('go-docgen')">
          需求管理中心
          <small>进入需求澄清、PRD 生成与版本跟踪流程</small>
        </button>
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
  align-content: start;
}

.nav-card {
  padding: 16px;
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
  font-size: 20px;
  color: #0f172a;
}

.summary {
  margin: 8px 0 0;
  color: #475569;
  line-height: 1.7;
  font-size: 13px;
}

.nav-group {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.nav-button {
  display: grid;
  gap: 4px;
  width: 100%;
  padding: 12px 14px;
  border-radius: 6px;
  border: 1px solid #d4dde8;
  background: #f8fafc;
  color: #0f172a;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease;
}

.nav-button:hover {
  background: #eff6ff;
  border-color: #93c5fd;
}

.nav-button small {
  color: #64748b;
  font-size: 12px;
}

.nav-button--primary {
  background: #1d4ed8;
  border-color: #1d4ed8;
  color: #ffffff;
}

.nav-button--primary:hover {
  background: #1e40af;
  border-color: #1e40af;
}

.nav-button--primary small {
  color: rgba(255, 255, 255, 0.82);
}
</style>
