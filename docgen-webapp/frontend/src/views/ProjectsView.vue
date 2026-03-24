<template>
  <div class="page">
    <header class="topbar">
      <div>
        <h1>项目 / 需求工作区</h1>
        <p class="sub">层级视图：项目 -> 需求1/2/3 -> 版本</p>
      </div>
      <div class="top-actions">
        <button class="ghost" @click="goDocgen">返回AI需求工作台</button>
      </div>
    </header>

    <div class="layout">
      <aside class="sidebar">
        <section class="card">
          <h3>创建项目</h3>
          <input v-model.trim="projectForm.projectKey" class="input" placeholder="项目编码，如 AI-REQ" />
          <input v-model.trim="projectForm.projectName" class="input" placeholder="项目名称" />
          <select v-model="projectForm.visibility" class="input">
            <option value="PRIVATE">PRIVATE</option>
            <option value="ORG">ORG</option>
          </select>
          <button
            class="primary block"
            :disabled="loading || !projectForm.projectKey || !projectForm.projectName"
            @click="createProject"
          >
            创建
          </button>
        </section>

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
      </aside>

      <main class="content">
        <section v-if="!selectedProject" class="card empty-state">
          <h3>请先在左侧选择一个项目</h3>
          <p>然后你可以创建/管理需求并进入AI工作台。</p>
        </section>

        <template v-else>
          <section class="card">
            <h3>项目详情</h3>
            <div class="meta-grid">
              <div><strong>ID:</strong> {{ selectedProject.id }}</div>
              <div><strong>编码:</strong> {{ selectedProject.projectKey }}</div>
              <div><strong>名称:</strong> {{ selectedProject.projectName }}</div>
              <div><strong>状态:</strong> {{ selectedProject.status }}</div>
              <div><strong>可见性:</strong> {{ selectedProject.visibility }}</div>
            </div>
          </section>

          <section class="card">
            <h3>在当前项目下创建需求</h3>
            <div class="form-grid">
              <input v-model.trim="reqForm.title" class="input" placeholder="需求标题" />
              <select v-model="reqForm.priority" class="input">
                <option value="P0">P0</option>
                <option value="P1">P1</option>
                <option value="P2">P2</option>
                <option value="P3">P3</option>
              </select>
              <select v-model="reqForm.status" class="input">
                <option value="DRAFT">DRAFT</option>
                <option value="CLARIFYING">CLARIFYING</option>
                <option value="READY_REVIEW">READY_REVIEW</option>
                <option value="DONE">DONE</option>
              </select>
            </div>
            <textarea v-model="reqForm.summary" class="input" placeholder="需求摘要（可选）" />
            <div class="row">
              <button class="primary" :disabled="loading || !reqForm.title" @click="createRequirement">创建需求</button>
            </div>
          </section>

          <section class="card">
            <h3>{{ selectedProject.projectName }} 的需求列表</h3>
            <table class="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>编号</th>
                  <th>标题</th>
                  <th>优先级</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="r in requirementsOf(selectedProject.id)" :key="r.id">
                  <td>{{ r.id }}</td>
                  <td>{{ r.requirementNo }}</td>
                  <td>{{ r.title }}</td>
                  <td>{{ r.priority }}</td>
                  <td>{{ r.status }}</td>
                  <td class="ops">
                    <button class="mini" @click="selectRequirement(selectedProject.id, r.id)">选中</button>
                    <button class="mini" @click="openWorkbench(r.id)">AI工作台</button>
                    <button class="mini" @click="openVersions(r.id)">版本页</button>
                  </td>
                </tr>
                <tr v-if="requirementsOf(selectedProject.id).length === 0">
                  <td colspan="6" class="empty small">暂无需求</td>
                </tr>
              </tbody>
            </table>
          </section>

          <section v-if="selectedRequirement" class="card">
            <h3>当前选中需求</h3>
            <div class="meta-grid">
              <div><strong>ID:</strong> {{ selectedRequirement.id }}</div>
              <div><strong>编号:</strong> {{ selectedRequirement.requirementNo }}</div>
              <div><strong>标题:</strong> {{ selectedRequirement.title }}</div>
              <div><strong>优先级:</strong> {{ selectedRequirement.priority }}</div>
              <div><strong>状态:</strong> {{ selectedRequirement.status }}</div>
            </div>
            <p class="summary">{{ selectedRequirement.summary || '暂无摘要' }}</p>
            <div class="row">
              <button class="primary" @click="openWorkbench(selectedRequirement.id)">进入AI工作台</button>
              <button class="ghost" @click="openVersions(selectedRequirement.id)">查看版本页</button>
            </div>
          </section>
        </template>
      </main>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="success" class="success">{{ success }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import axios from 'axios'
import { useRoute, useRouter } from 'vue-router'

type ApiResponse<T> = { code: number; message: string; data: T; traceId: string }
type ProjectItem = {
  id: number
  projectKey: string
  projectName: string
  description?: string
  visibility: string
  status: string
}
type RequirementItem = {
  id: number
  requirementNo: string
  title: string
  summary: string
  priority: string
  status: string
}
type VersionItem = {
  id: number
  versionNo: string
  isCurrent: boolean
}

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const error = ref('')
const success = ref('')

const projects = ref<ProjectItem[]>([])
const requirementsMap = ref<Record<number, RequirementItem[]>>({})
const versionsMap = ref<Record<number, VersionItem[]>>({})
const expandedProjectIds = ref<number[]>([])
const selectedProjectId = ref<number | null>(null)
const selectedRequirementId = ref<number | null>(null)

const projectForm = reactive({
  projectKey: '',
  projectName: '',
  visibility: 'PRIVATE',
  description: ''
})

const reqForm = reactive({
  title: '',
  summary: '',
  priority: 'P2',
  status: 'DRAFT'
})

const selectedProject = computed(() => projects.value.find((p) => p.id === selectedProjectId.value) || null)
const selectedRequirement = computed(() => {
  if (!selectedProjectId.value || !selectedRequirementId.value) return null
  return requirementsOf(selectedProjectId.value).find((r) => r.id === selectedRequirementId.value) || null
})

function requirementsOf(projectId: number): RequirementItem[] {
  return requirementsMap.value[projectId] || []
}

function versionsOf(requirementId: number): VersionItem[] {
  return versionsMap.value[requirementId] || []
}

function isExpanded(projectId: number): boolean {
  return expandedProjectIds.value.includes(projectId)
}

function toggleProject(projectId: number) {
  if (isExpanded(projectId)) {
    expandedProjectIds.value = expandedProjectIds.value.filter((id) => id !== projectId)
    return
  }
  expandedProjectIds.value.push(projectId)
  void loadRequirements(projectId)
}

async function loadProjects() {
  loading.value = true
  error.value = ''
  try {
    const res = await axios.get<ApiResponse<ProjectItem[]>>('/api/projects')
    projects.value = res.data.data || []
    const queryProjectId = Number(route.query.projectId || 0)
    if (queryProjectId > 0 && projects.value.some((p) => p.id === queryProjectId)) {
      await selectProject(queryProjectId)
      return
    }
    if (!selectedProjectId.value && projects.value.length > 0) {
      await selectProject(projects.value[0].id)
    }
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || 'Failed to load projects'
  } finally {
    loading.value = false
  }
}

async function loadRequirements(projectId: number) {
  try {
    const res = await axios.get<ApiResponse<RequirementItem[]>>(`/api/projects/${projectId}/requirements`)
    requirementsMap.value = { ...requirementsMap.value, [projectId]: res.data.data || [] }
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || 'Failed to load requirements'
  }
}

async function loadVersions(requirementId: number) {
  try {
    const res = await axios.get<ApiResponse<VersionItem[]>>(`/api/requirements/${requirementId}/versions`)
    versionsMap.value = { ...versionsMap.value, [requirementId]: res.data.data || [] }
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || 'Failed to load versions'
  }
}

async function createProject() {
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await axios.post('/api/projects', projectForm)
    success.value = '项目创建成功'
    projectForm.projectKey = ''
    projectForm.projectName = ''
    projectForm.visibility = 'PRIVATE'
    projectForm.description = ''
    await loadProjects()
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || 'Failed to create project'
  } finally {
    loading.value = false
  }
}

async function createRequirement() {
  if (!selectedProjectId.value) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await axios.post(`/api/projects/${selectedProjectId.value}/requirements`, reqForm)
    success.value = '需求创建成功'
    reqForm.title = ''
    reqForm.summary = ''
    reqForm.priority = 'P2'
    reqForm.status = 'DRAFT'
    await loadRequirements(selectedProjectId.value)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || 'Failed to create requirement'
  } finally {
    loading.value = false
  }
}

async function selectProject(projectId: number) {
  selectedProjectId.value = projectId
  selectedRequirementId.value = null
  if (!isExpanded(projectId)) {
    expandedProjectIds.value.push(projectId)
  }
  await loadRequirements(projectId)
}

async function selectRequirement(projectId: number, requirementId: number) {
  selectedProjectId.value = projectId
  selectedRequirementId.value = requirementId
  if (!isExpanded(projectId)) {
    expandedProjectIds.value.push(projectId)
  }
  if (!requirementsMap.value[projectId]) {
    await loadRequirements(projectId)
  }
  await loadVersions(requirementId)
}

function openWorkbench(requirementId: number) {
  if (!selectedProjectId.value) return
  router.push(`/requirements/${requirementId}/workbench?projectId=${selectedProjectId.value}`)
}

function openVersions(requirementId: number) {
  if (!selectedProjectId.value) return
  router.push(`/requirements/${requirementId}/versions?projectId=${selectedProjectId.value}`)
}

function goDocgen() {
  router.push('/docgen')
}

onMounted(loadProjects)
</script>

<style scoped>
.page {
  max-width: 1320px;
  margin: 18px auto;
  padding: 0 14px 18px;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  color: #111827;
}
.topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}
h1 {
  margin: 0;
  font-size: 24px;
  letter-spacing: 0.2px;
}
.sub {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
}
.layout {
  display: grid;
  grid-template-columns: 350px 1fr;
  gap: 14px;
}
.sidebar,
.content {
  min-height: 70vh;
}
.card {
  background: #fff;
  border: 1px solid #dbe2ea;
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 12px;
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
.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(180px, 1fr));
  gap: 10px;
  font-size: 14px;
}
.form-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 10px;
}
.input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 8px 10px;
  margin-top: 8px;
  background: #fff;
}
textarea.input {
  min-height: 70px;
  resize: vertical;
}
.table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 8px;
}
.table th,
.table td {
  border: 1px solid #e5e7eb;
  padding: 8px;
  font-size: 13px;
}
.ops {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.row {
  display: flex;
  gap: 10px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.primary,
.ghost,
.mini {
  border-radius: 8px;
  border: 1px solid #d1d5db;
  padding: 8px 12px;
  cursor: pointer;
}
.primary {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}
.ghost,
.mini {
  background: #f3f4f6;
}
.mini {
  padding: 5px 9px;
  font-size: 12px;
}
.block {
  width: 100%;
}
.summary {
  font-size: 13px;
  color: #4b5563;
}
.error {
  margin-top: 8px;
  color: #b91c1c;
}
.success {
  margin-top: 8px;
  color: #166534;
}
.empty {
  color: #6b7280;
}
.small {
  font-size: 12px;
}
.empty-state {
  min-height: 180px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
@media (max-width: 980px) {
  .layout {
    grid-template-columns: 1fr;
  }
  .meta-grid {
    grid-template-columns: 1fr;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
