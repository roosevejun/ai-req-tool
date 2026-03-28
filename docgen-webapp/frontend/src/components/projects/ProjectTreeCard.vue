<template>
  <section class="card tree">
    <div class="tree-head">
      <div>
        <h3>项目列表</h3>
        <p class="caption">在左侧选择项目，右侧查看项目基础信息、维护信息和 AI 协同结果。</p>
      </div>
      <button class="ghost mini" :disabled="loading" @click="$emit('reload-projects')">刷新</button>
    </div>

    <input v-model.trim="searchText" class="search-input" placeholder="搜索项目名称或项目 Key" />

    <div v-if="filteredProjects.length === 0" class="empty">当前没有可显示的项目。</div>

    <ul v-else class="tree-list">
      <li v-for="project in filteredProjects" :key="project.id">
        <button
          class="tree-label"
          :class="{ active: selectedProjectId === project.id }"
          :title="project.projectName"
          @click="$emit('select-project', project.id)"
        >
          <span class="project-key">{{ project.projectKey || `项目 #${project.id}` }}</span>
          <span class="project-name">{{ project.projectName }}</span>
          <span class="project-meta">{{ project.status || 'ACTIVE' }} · {{ project.priority || 'P2' }}</span>
        </button>
      </li>
    </ul>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ProjectItem } from './types'

const props = defineProps<{
  loading: boolean
  projects: ProjectItem[]
  selectedProjectId: number | null
}>()

defineEmits<{
  (event: 'reload-projects'): void
  (event: 'select-project', projectId: number): void
}>()

const searchText = ref('')

const filteredProjects = computed(() => {
  const keyword = searchText.value.trim().toLowerCase()
  if (!keyword) return props.projects
  return props.projects.filter((project) => {
    return (project.projectName || '').toLowerCase().includes(keyword) || (project.projectKey || '').toLowerCase().includes(keyword)
  })
})
</script>

<style scoped>
.card {
  background: #ffffff;
  border: 1px solid #d4dde8;
  border-top: 4px solid #1d4ed8;
  border-radius: 10px;
  padding: 14px;
}

.tree-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

h3 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

.caption {
  margin: 6px 0 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.6;
}

.search-input {
  width: 100%;
  box-sizing: border-box;
  margin-top: 12px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 9px 12px;
  background: #ffffff;
}

.tree-list {
  list-style: none;
  margin: 12px 0 0;
  padding: 0;
  display: grid;
  gap: 8px;
}

.tree-label {
  width: 100%;
  display: grid;
  gap: 4px;
  text-align: left;
  border: 1px solid #dbe5f0;
  background: #f8fafc;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  color: #1f2937;
}

.tree-label:hover {
  background: #eff6ff;
  border-color: #93c5fd;
}

.tree-label.active {
  background: #1d4ed8;
  border-color: #1d4ed8;
  color: #ffffff;
}

.project-key {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: #64748b;
}

.tree-label.active .project-key {
  color: rgba(255, 255, 255, 0.78);
}

.project-name {
  font-size: 14px;
  font-weight: 700;
}

.project-meta {
  font-size: 12px;
  color: #64748b;
}

.tree-label.active .project-meta {
  color: rgba(255, 255, 255, 0.82);
}

.empty {
  color: #6b7280;
  margin-top: 14px;
}

.ghost,
.mini {
  border-radius: 6px;
  border: 1px solid #d1d5db;
  padding: 5px 9px;
  cursor: pointer;
  background: #f8fafc;
  font-size: 12px;
}
</style>
