<template>
  <section class="card tree">
    <div class="tree-head">
      <h3>项目树</h3>
      <button class="ghost mini" :disabled="loading" @click="loadProjects">刷新</button>
    </div>

    <div v-if="projects.length === 0" class="empty">暂无项目</div>

    <ul v-else class="tree-list">
      <ProjectTreeNode
        v-for="project in projects"
        :key="project.id"
        :project="project"
        :expanded="isExpanded(project.id)"
        :requirements="requirementsOf(project.id)"
        :selected-project-id="selectedProjectId"
        :selected-requirement-id="selectedRequirementId"
        :versions-of="versionsOf"
        @toggle-project="toggleProject"
        @select-project="selectProject"
        @select-requirement="selectRequirement"
      />
    </ul>
  </section>
</template>

<script setup lang="ts">
import axios from 'axios'
import { onMounted, ref, watch } from 'vue'
import ProjectTreeNode from './project-tree/ProjectTreeNode.vue'
import type {
  ApiResponse,
  ProjectTreeProjectItem,
  ProjectTreeRequirementItem,
  ProjectTreeVersionItem
} from './project-tree/types'

const props = defineProps<{
  selectedProjectId: number | null
  selectedRequirementId: number | null
}>()

const emit = defineEmits<{
  (event: 'select-project', projectId: number): void
  (event: 'select-requirement', payload: { projectId: number; requirementId: number }): void
  (event: 'error', message: string): void
}>()

const loading = ref(false)
const projects = ref<ProjectTreeProjectItem[]>([])
const requirementsMap = ref<Record<number, ProjectTreeRequirementItem[]>>({})
const versionsMap = ref<Record<number, ProjectTreeVersionItem[]>>({})
const expandedProjectIds = ref<number[]>([])

function requirementsOf(projectId: number): ProjectTreeRequirementItem[] {
  return requirementsMap.value[projectId] || []
}

function versionsOf(requirementId: number): ProjectTreeVersionItem[] {
  return versionsMap.value[requirementId] || []
}

function isExpanded(projectId: number): boolean {
  return expandedProjectIds.value.includes(projectId)
}

function ensureExpanded(projectId: number) {
  if (!isExpanded(projectId)) {
    expandedProjectIds.value.push(projectId)
  }
}

function emitError(e: any, fallback: string) {
  emit('error', e?.response?.data?.message || e?.message || fallback)
}

async function loadProjects() {
  loading.value = true
  try {
    const res = await axios.get<ApiResponse<ProjectTreeProjectItem[]>>('/api/projects')
    projects.value = res.data.data || []
    if (props.selectedProjectId) {
      ensureExpanded(props.selectedProjectId)
      await loadRequirements(props.selectedProjectId)
    }
  } catch (e: any) {
    emitError(e, '加载项目失败。')
  } finally {
    loading.value = false
  }
}

async function loadRequirements(projectId: number) {
  try {
    const res = await axios.get<ApiResponse<ProjectTreeRequirementItem[]>>(`/api/projects/${projectId}/requirements`)
    requirementsMap.value = { ...requirementsMap.value, [projectId]: res.data.data || [] }
  } catch (e: any) {
    emitError(e, '加载需求失败。')
  }
}

async function loadVersions(requirementId: number) {
  try {
    const res = await axios.get<ApiResponse<ProjectTreeVersionItem[]>>(`/api/requirements/${requirementId}/versions`)
    versionsMap.value = { ...versionsMap.value, [requirementId]: res.data.data || [] }
  } catch (e: any) {
    emitError(e, '加载版本失败。')
  }
}

async function toggleProject(projectId: number) {
  if (isExpanded(projectId)) {
    expandedProjectIds.value = expandedProjectIds.value.filter((id) => id !== projectId)
    return
  }
  expandedProjectIds.value.push(projectId)
  await loadRequirements(projectId)
}

async function selectProject(projectId: number) {
  ensureExpanded(projectId)
  await loadRequirements(projectId)
  emit('select-project', projectId)
}

async function selectRequirement(projectId: number, requirementId: number) {
  ensureExpanded(projectId)
  await loadRequirements(projectId)
  await loadVersions(requirementId)
  emit('select-requirement', { projectId, requirementId })
}

watch(
  () => props.selectedProjectId,
  async (projectId) => {
    if (projectId) {
      ensureExpanded(projectId)
      await loadRequirements(projectId)
    }
  },
  { immediate: true }
)

watch(
  () => props.selectedRequirementId,
  async (requirementId) => {
    if (requirementId) {
      await loadVersions(requirementId)
    }
  },
  { immediate: true }
)

onMounted(loadProjects)
</script>

<style scoped>
.card {
  background: #fff;
  border: 1px solid #dbe2ea;
  border-radius: 12px;
  padding: 12px;
}
.tree {
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}
.tree-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.tree-list {
  list-style: none;
  margin: 10px 0 0;
  padding: 0;
}
.empty {
  color: #6b7280;
}
.ghost,
.mini {
  border-radius: 8px;
  border: 1px solid #d1d5db;
  background: #f3f4f6;
  cursor: pointer;
}
.ghost {
  padding: 8px 12px;
}
.mini {
  padding: 5px 9px;
  font-size: 12px;
}
</style>
