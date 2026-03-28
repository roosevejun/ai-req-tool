<template>
  <section class="card tree">
    <div class="tree-head">
      <div>
        <h3>项目与需求目录</h3>
        <p class="caption">支持按项目快速定位，再切换到项目概览、AI 协同或需求管理。</p>
      </div>
      <button class="ghost mini" :disabled="loading" @click="$emit('reload-projects')">刷新</button>
    </div>

    <input v-model.trim="searchText" class="search-input" placeholder="搜索项目名称或项目 Key" />

    <div v-if="filteredProjects.length === 0" class="empty">当前没有可显示的项目。</div>

    <ul v-else class="tree-list">
      <li v-for="project in filteredProjects" :key="project.id" class="tree-project">
        <div class="tree-row">
          <button class="toggle" @click="$emit('toggle-project', project.id)">{{ isExpanded(project.id) ? '-' : '+' }}</button>
          <button
            class="tree-label"
            :class="{ active: selectedProjectId === project.id }"
            :title="project.projectName"
            @click="$emit('select-project', project.id)"
          >
            <span class="project-key">{{ project.projectKey }}</span>
            <span class="project-name">{{ project.projectName }}</span>
          </button>
        </div>

        <ul v-if="isExpanded(project.id)" class="tree-children">
          <li v-if="requirementsOf(project.id).length === 0" class="empty small">当前项目下还没有需求。</li>
          <li v-for="(requirement, idx) in requirementsOf(project.id)" :key="requirement.id">
            <div class="tree-row req-row">
              <button
                class="tree-label req"
                :class="{ active: selectedRequirementId === requirement.id }"
                @click="$emit('select-requirement', project.id, requirement.id)"
              >
                <span class="req-index">需求 {{ idx + 1 }}</span>
                <span class="req-title">{{ requirement.title }}</span>
              </button>
            </div>

            <ul v-if="selectedRequirementId === requirement.id && versionsOf(requirement.id).length > 0" class="tree-children ver">
              <li v-for="version in versionsOf(requirement.id)" :key="version.id" class="ver-item">
                {{ version.versionNo }}
                <span v-if="version.isCurrent" class="current">当前版本</span>
              </li>
            </ul>
          </li>
        </ul>
      </li>
    </ul>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ProjectItem, RequirementItem, VersionItem } from './types'

const props = defineProps<{
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
  background: #fff;
  border: 1px solid #dbe2ea;
  border-radius: 16px;
  padding: 14px;
}
.tree {
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
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
  line-height: 1.5;
}
.search-input {
  width: 100%;
  box-sizing: border-box;
  margin-top: 12px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 9px 12px;
  background: #fff;
}
.tree-list,
.tree-children {
  list-style: none;
  margin: 12px 0 0;
  padding: 0;
}
.tree-children {
  padding-left: 18px;
  border-left: 1px dashed #c7d2e0;
}
.tree-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 6px 0;
}
.toggle {
  width: 24px;
  height: 24px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #f8fafc;
  cursor: pointer;
}
.tree-label {
  flex: 1;
  display: grid;
  gap: 2px;
  text-align: left;
  border: 1px solid transparent;
  background: transparent;
  padding: 6px 8px;
  border-radius: 10px;
  cursor: pointer;
  color: #1f2937;
}
.tree-label:hover {
  background: #eef4ff;
}
.tree-label.active {
  background: #2563eb;
  color: #fff;
}
.project-key,
.req-index {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: #64748b;
}
.tree-label.active .project-key,
.tree-label.active .req-index {
  color: rgba(255, 255, 255, 0.78);
}
.project-name,
.req-title {
  font-size: 13px;
  font-weight: 600;
}
.ver-item {
  color: #4b5563;
  font-size: 12px;
  margin: 5px 0;
}
.current {
  color: #2563eb;
  font-weight: 600;
}
.empty {
  color: #6b7280;
  margin-top: 14px;
}
.small {
  font-size: 12px;
}
.ghost,
.mini {
  border-radius: 8px;
  border: 1px solid #d1d5db;
  padding: 8px 12px;
  cursor: pointer;
  background: #f3f4f6;
}
.mini {
  padding: 5px 9px;
  font-size: 12px;
}
</style>
