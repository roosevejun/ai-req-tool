<template>
  <section class="card tree">
    <div class="tree-head">
      <div>
        <h3>项目列表</h3>
        <p class="caption">先找到最值得处理的项目，再进入项目定义、AI 补全或资料知识工作区。</p>
      </div>
      <button class="ghost mini" :disabled="loading" @click="$emit('reload-projects')">刷新</button>
    </div>

    <div class="filter-bar">
      <input v-model.trim="searchText" class="search-input" placeholder="搜索项目名称或项目 Key" />
      <select v-model="filterMode" class="filter-select">
        <option value="ALL">全部项目</option>
        <option value="ATTENTION">待补全</option>
        <option value="READY">较完整</option>
      </select>
    </div>

    <div v-if="filteredProjects.length === 0" class="empty">当前没有可显示的项目。</div>

    <ul v-else class="tree-list">
      <li v-for="project in filteredProjects" :key="project.id">
        <button
          class="tree-label"
          :class="{ active: selectedProjectId === project.id }"
          :title="project.projectName"
          @click="$emit('select-project', project.id)"
        >
          <div class="project-head">
            <span class="project-key">{{ project.projectKey || `项目 #${project.id}` }}</span>
            <span class="health-dot" :class="healthClass(project)"></span>
          </div>
          <span class="project-name">{{ project.projectName }}</span>
          <div class="project-foot">
            <span class="project-meta">{{ project.status || 'ACTIVE' }} · {{ project.priority || 'P2' }}</span>
            <span class="project-score">{{ completenessOf(project).score }}%</span>
          </div>
          <span class="project-hint">
            {{
              completenessOf(project).missingFieldLabels.length
                ? `缺少 ${completenessOf(project).missingFieldLabels.slice(0, 2).join('、')}`
                : '项目信息较完整'
            }}
          </span>
        </button>
      </li>
    </ul>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { getProjectCompleteness, getProjectHealthTone } from './projectHealth'
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
const filterMode = ref<'ALL' | 'ATTENTION' | 'READY'>('ALL')

function completenessOf(project: ProjectItem) {
  return getProjectCompleteness(project)
}

function healthClass(project: ProjectItem) {
  return `health-dot--${getProjectHealthTone(completenessOf(project).score)}`
}

const filteredProjects = computed(() => {
  const keyword = searchText.value.trim().toLowerCase()
  return props.projects.filter((project) => {
    const matchedKeyword =
      !keyword ||
      (project.projectName || '').toLowerCase().includes(keyword) ||
      (project.projectKey || '').toLowerCase().includes(keyword)
    const score = completenessOf(project).score
    const matchedFilter =
      filterMode.value === 'ALL' ||
      (filterMode.value === 'ATTENTION' && score < 75) ||
      (filterMode.value === 'READY' && score >= 75)
    return matchedKeyword && matchedFilter
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

.filter-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px;
  gap: 10px;
  margin-top: 12px;
}

.search-input,
.filter-select {
  width: 100%;
  box-sizing: border-box;
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
  gap: 6px;
  text-align: left;
  border: 1px solid #dbe5f0;
  background: #f8fafc;
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  color: #1f2937;
}

.project-head,
.project-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
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

.health-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  flex: none;
}

.health-dot--danger {
  background: #ef4444;
}

.health-dot--warning {
  background: #f59e0b;
}

.health-dot--success {
  background: #10b981;
}

.tree-label.active .project-key {
  color: rgba(255, 255, 255, 0.78);
}

.project-name {
  font-size: 14px;
  font-weight: 700;
}

.project-meta,
.project-score,
.project-hint {
  font-size: 12px;
}

.project-meta,
.project-hint {
  color: #64748b;
}

.project-score {
  color: #0f172a;
  font-weight: 700;
}

.project-hint {
  line-height: 1.5;
}

.tree-label.active .project-meta,
.tree-label.active .project-score,
.tree-label.active .project-hint {
  color: rgba(255, 255, 255, 0.86);
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

@media (max-width: 860px) {
  .filter-bar {
    grid-template-columns: 1fr;
  }
}
</style>
