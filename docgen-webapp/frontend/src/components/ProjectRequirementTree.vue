<template>
  <section class="card tree">
    <div class="tree-head">
      <h3>项目树</h3>
      <button class="ghost mini" :disabled="loading" @click="loadProjects">刷新</button>
    </div>

    <div v-if="projects.length === 0" class="empty">暂无项目</div>

    <ul v-else class="tree-list">
      <li v-for="p in projects" :key="p.id" class="tree-project">
        <div class="tree-row">
          <button class="toggle" @click="toggleProject(p.id)">{{ isExpanded(p.id) ? '-' : '+' }}</button>
          <button
            class="tree-label"
            :class="{ active: selectedProjectId === p.id }"
            @click="selectProject(p.id)"
            :title="p.projectName"
          >
            {{ p.projectKey }} / {{ p.projectName }}
          </button>
        </div>

        <ul v-if="isExpanded(p.id)" class="tree-children">
          <li v-if="requirementsOf(p.id).length === 0" class="empty small">暂无需求</li>
          <li v-for="(r, idx) in requirementsOf(p.id)" :key="r.id">
            <div class="tree-row req-row">
              <button
                class="tree-label req"
                :class="{ active: selectedRequirementId === r.id }"
                @click="selectRequirement(p.id, r.id)"
              >
                需求{{ idx + 1 }}：{{ r.title }}
              </button>
            </div>

            <ul v-if="selectedRequirementId === r.id && versionsOf(r.id).length > 0" class="tree-children ver">
              <li v-for="v in versionsOf(r.id)" :key="v.id" class="ver-item">
                {{ v.versionNo }} <span v-if="v.isCurrent" class="current">（当前）</span>
              </li>
            </ul>
          </li>
        </ul>
      </li>
    </ul>
  </section>
</template>

<script setup lang="ts">
import axios from 'axios'
import { onMounted, ref, watch } from 'vue'

type ApiResponse<T> = { code: number; message: string; data: T; traceId: string }
type ProjectItem = { id: number; projectKey: string; projectName: string }
type RequirementItem = { id: number; title: string }
type VersionItem = { id: number; versionNo: string; isCurrent: boolean }

const props = defineProps<{
  selectedProjectId: number | null
  selectedRequirementId: number | null
}>()

const emit = defineEmits<{
  (e: 'select-project', projectId: number): void
  (e: 'select-requirement', payload: { projectId: number; requirementId: number }): void
  (e: 'error', message: string): void
}>()

const loading = ref(false)
const projects = ref<ProjectItem[]>([])
const requirementsMap = ref<Record<number, RequirementItem[]>>({})
const versionsMap = ref<Record<number, VersionItem[]>>({})
const expandedProjectIds = ref<number[]>([])

function requirementsOf(projectId: number): RequirementItem[] {
  return requirementsMap.value[projectId] || []
}

function versionsOf(requirementId: number): VersionItem[] {
  return versionsMap.value[requirementId] || []
}

function isExpanded(projectId: number): boolean {
  return expandedProjectIds.value.includes(projectId)
}

async function loadProjects() {
  loading.value = true
  try {
    const res = await axios.get<ApiResponse<ProjectItem[]>>('/api/projects')
    projects.value = res.data.data || []
    if (props.selectedProjectId && !isExpanded(props.selectedProjectId)) {
      expandedProjectIds.value.push(props.selectedProjectId)
      await loadRequirements(props.selectedProjectId)
    }
  } catch (e: any) {
    emit('error', e?.response?.data?.message || e?.message || 'Failed to load projects')
  } finally {
    loading.value = false
  }
}

async function loadRequirements(projectId: number) {
  try {
    const res = await axios.get<ApiResponse<RequirementItem[]>>(`/api/projects/${projectId}/requirements`)
    requirementsMap.value = { ...requirementsMap.value, [projectId]: res.data.data || [] }
  } catch (e: any) {
    emit('error', e?.response?.data?.message || e?.message || 'Failed to load requirements')
  }
}

async function loadVersions(requirementId: number) {
  try {
    const res = await axios.get<ApiResponse<VersionItem[]>>(`/api/requirements/${requirementId}/versions`)
    versionsMap.value = { ...versionsMap.value, [requirementId]: res.data.data || [] }
  } catch (e: any) {
    emit('error', e?.response?.data?.message || e?.message || 'Failed to load versions')
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
  if (!isExpanded(projectId)) {
    expandedProjectIds.value.push(projectId)
    await loadRequirements(projectId)
  }
  emit('select-project', projectId)
}

async function selectRequirement(projectId: number, requirementId: number) {
  if (!isExpanded(projectId)) {
    expandedProjectIds.value.push(projectId)
    await loadRequirements(projectId)
  }
  await loadVersions(requirementId)
  emit('select-requirement', { projectId, requirementId })
}

watch(
  () => props.selectedProjectId,
  async (pid) => {
    if (pid && !isExpanded(pid)) {
      expandedProjectIds.value.push(pid)
      await loadRequirements(pid)
    }
  },
  { immediate: true }
)

watch(
  () => props.selectedRequirementId,
  async (rid) => {
    if (rid) {
      await loadVersions(rid)
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
.tree-list,
.tree-children {
  list-style: none;
  margin: 10px 0 0;
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
  width: 22px;
  height: 22px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #f8fafc;
  cursor: pointer;
}
.tree-label {
  flex: 1;
  text-align: left;
  border: 1px solid transparent;
  background: transparent;
  padding: 4px 6px;
  border-radius: 6px;
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
.tree-label.req {
  font-size: 13px;
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
}
.small {
  font-size: 12px;
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
