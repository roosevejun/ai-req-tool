<template>
  <section class="workspace-shell workspace-header">
    <div class="project-summary">
      <p class="eyebrow">当前项目</p>
      <div class="title-row">
        <h3>{{ project.projectName }}</h3>
        <StatusBadge :label="projectStatusLabel(project.status)" variant="info" />
      </div>
      <div class="meta-row">
        <StatusBadge :label="project.projectKey" small />
        <StatusBadge :label="projectTypeLabel(project.projectType)" small />
        <StatusBadge :label="visibilityLabel(project.visibility)" small />
        <StatusBadge :label="project.priority || '-'" variant="warning" small />
        <StatusBadge v-if="project.tags" :label="project.tags" variant="ai" small />
      </div>
      <p class="summary">{{ project.description || '当前项目还没有补充描述信息，建议先进入 AI 协同把项目框架整理完整。' }}</p>
    </div>

    <div class="header-actions">
      <button class="primary" type="button" @click="$emit('enter-ai')">继续用 AI 校准项目框架</button>
    </div>

    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        class="tab"
        :class="{ active: activeTab === tab.value }"
        type="button"
        @click="$emit('change-tab', tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import StatusBadge from './StatusBadge.vue'
import type { ProjectItem } from './types'

defineProps<{
  project: ProjectItem
  activeTab: 'overview' | 'ai' | 'requirements'
  projectTypeLabel: (value?: string) => string
  visibilityLabel: (value?: string) => string
  projectStatusLabel: (value?: string) => string
}>()

defineEmits<{
  (event: 'change-tab', tab: 'overview' | 'ai' | 'requirements'): void
  (event: 'enter-ai'): void
}>()

const tabs: Array<{ value: 'overview' | 'ai' | 'requirements'; label: string }> = [
  { value: 'overview', label: '概览' },
  { value: 'ai', label: 'AI 协同' },
  { value: 'requirements', label: '需求管理' }
]
</script>

<style scoped>
.workspace-shell {
  background: #fff;
  border: 1px solid #dbe2ea;
  border-radius: 18px;
  padding: 18px;
}
.workspace-header {
  display: grid;
  gap: 14px;
  margin-bottom: 14px;
}
.eyebrow {
  margin: 0 0 6px;
  color: #0f766e;
  font-size: 12px;
  letter-spacing: .08em;
  text-transform: uppercase;
  font-weight: 700;
}
.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
h3 {
  margin: 0;
  font-size: 26px;
  color: #0f172a;
}
.meta-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.summary {
  margin: 0;
  color: #475569;
  line-height: 1.6;
}
.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-start;
}
.tabs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  padding-top: 6px;
  border-top: 1px solid #e5edf5;
}
.tab,
.primary,
.ghost {
  border-radius: 10px;
  border: 1px solid #d1d5db;
  padding: 9px 14px;
  cursor: pointer;
}
.tab,
.ghost {
  background: #f8fafc;
  color: #334155;
}
.tab.active {
  background: #2563eb;
  border-color: #2563eb;
  color: #fff;
}
.primary {
  background: #0f766e;
  border-color: #0f766e;
  color: #fff;
}
</style>
