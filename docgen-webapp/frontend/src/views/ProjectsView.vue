<template>
  <div class="page">
    <header class="topbar">
      <div>
        <h1>项目 / 需求管理</h1>
        <p class="sub">按“项目 -> 需求 -> 版本”组织，支持创建项目、维护项目信息、创建需求与进入 AI 工作台。</p>
      </div>
      <div class="top-actions">
        <button class="ghost" @click="goCreateAi">AI 创建项目</button>
        <button class="ghost" @click="goDocgen">进入 AI 需求整理页</button>
      </div>
    </header>

    <div class="layout">
      <aside class="sidebar">
        <section class="card">
          <h3>创建项目</h3>

          <div class="form-grid form-grid-project">
            <input v-model.trim="projectForm.projectKey" class="input" placeholder="项目Key（如 AI-REQ）" />
            <input v-model.trim="projectForm.projectName" class="input" placeholder="项目名称" />
            <select v-model="projectForm.projectType" class="input">
              <option value="">项目类型（可选）</option>
              <option value="PRODUCT">产品型</option>
              <option value="PLATFORM">平台型</option>
              <option value="OPS">运维型</option>
              <option value="INTEGRATION">集成型</option>
            </select>
            <select v-model="projectForm.priority" class="input">
              <option value="P0">P0</option>
              <option value="P1">P1</option>
              <option value="P2">P2</option>
              <option value="P3">P3</option>
            </select>
            <input v-model="projectForm.startDate" type="date" class="input" />
            <input v-model="projectForm.targetDate" type="date" class="input" />
            <select v-model="projectForm.ownerUserId" class="input">
              <option value="">负责人（默认当前登录用户）</option>
              <option v-for="u in userOptions" :key="u.id" :value="String(u.id)">
                {{ u.displayName || u.username }} ({{ u.username }})
              </option>
            </select>
            <select v-model="projectForm.visibility" class="input">
              <option value="PRIVATE">PRIVATE</option>
              <option value="ORG">ORG</option>
            </select>
            <input v-model.trim="projectForm.tags" class="input" placeholder="标签（逗号分隔，如 车联网,地图）" />
          </div>

          <textarea v-model="projectForm.description" class="input" placeholder="项目描述（可选）" />

          <div class="product-info-box">
            <div class="section-head">
              <h4>产品信息</h4>
              <div class="row compact">
                <button class="ghost mini" :disabled="projectAiLoading || !canGuideProjectInfo" @click="guideProjectProductInfo">
                  {{ projectAiQuestions.length > 0 ? '继续AI补全' : 'AI引导补全' }}
                </button>
                <button class="ghost mini" :disabled="projectAiLoading" @click="applyProjectAiSuggestions">
                  应用AI建议
                </button>
              </div>
            </div>

            <textarea v-model="projectForm.projectBackground" class="input" placeholder="项目背景（可选）" />
            <textarea v-model="projectForm.similarProducts" class="input" placeholder="类似产品 / 参考产品（可选）" />
            <textarea v-model="projectForm.targetCustomerGroups" class="input" placeholder="目标客户群体（可选）" />
            <textarea v-model="projectForm.commercialValue" class="input" placeholder="商业价值（可选）" />
            <textarea v-model="projectForm.coreProductValue" class="input" placeholder="产品核心价值（可选）" />

            <div v-if="projectAiMessage" class="ai-assistant">
              <strong>AI建议：</strong> {{ projectAiMessage }}
            </div>

            <div v-if="projectAiQuestions.length > 0" class="ai-qa-list">
              <div v-for="(question, idx) in projectAiQuestions" :key="question + idx" class="ai-question-card">
                <div class="ai-question">{{ idx + 1 }}. {{ question }}</div>
                <textarea
                  v-model="projectAiAnswers[idx]"
                  class="input"
                  :placeholder="`请补充回答：${question}`"
                />
              </div>
            </div>

            <div v-if="hasProjectAiSuggestions" class="ai-preview">
              <h4>AI补全建议预览</h4>
              <div class="preview-grid">
                <div class="preview-item">
                  <strong>项目背景</strong>
                  <p>{{ projectAiSuggestions.projectBackground || '-' }}</p>
                </div>
                <div class="preview-item">
                  <strong>类似产品</strong>
                  <p>{{ projectAiSuggestions.similarProducts || '-' }}</p>
                </div>
                <div class="preview-item">
                  <strong>目标客户群体</strong>
                  <p>{{ projectAiSuggestions.targetCustomerGroups || '-' }}</p>
                </div>
                <div class="preview-item">
                  <strong>商业价值</strong>
                  <p>{{ projectAiSuggestions.commercialValue || '-' }}</p>
                </div>
                <div class="preview-item">
                  <strong>核心价值</strong>
                  <p>{{ projectAiSuggestions.coreProductValue || '-' }}</p>
                </div>
              </div>
            </div>
          </div>

          <button
            class="primary block"
            :disabled="loading"
            @click="createProject"
          >
            创建项目
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
                      {{ v.versionNo }} <span v-if="v.isCurrent" class="current">当前版本</span>
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
          <h3>请选择一个项目</h3>
          <p>你可以先在左侧创建项目，再逐条新增需求并进入 AI 澄清与 PRD 生成流程。</p>
        </section>

        <template v-else>
          <section class="card">
            <div class="section-head">
              <h3>项目详情</h3>
              <div class="row compact">
                <button v-if="!editingProject" class="ghost mini" @click="startEditProject">编辑项目</button>
                <template v-else>
                  <button class="primary mini" :disabled="loading || !projectEditForm.projectName" @click="saveProjectEdit">
                    保存修改
                  </button>
                  <button class="ghost mini" :disabled="loading" @click="cancelProjectEdit">取消</button>
                </template>
              </div>
            </div>

            <template v-if="!editingProject">
              <div class="meta-grid">
                <div><strong>ID:</strong> {{ selectedProject.id }}</div>
                <div><strong>项目Key:</strong> {{ selectedProject.projectKey }}</div>
                <div><strong>项目名称:</strong> {{ selectedProject.projectName }}</div>
                <div><strong>项目类型:</strong> {{ selectedProject.projectType || '-' }}</div>
                <div><strong>优先级:</strong> {{ selectedProject.priority || '-' }}</div>
                <div><strong>可见性:</strong> {{ selectedProject.visibility }}</div>
                <div><strong>计划开始:</strong> {{ selectedProject.startDate || '-' }}</div>
                <div><strong>计划结束:</strong> {{ selectedProject.targetDate || '-' }}</div>
                <div><strong>状态:</strong> {{ selectedProject.status }}</div>
                <div><strong>标签:</strong> {{ selectedProject.tags || '-' }}</div>
              </div>
              <p class="summary">{{ selectedProject.description || '暂无项目描述' }}</p>
              <div class="detail-section">
                <h4>项目背景</h4>
                <p class="summary">{{ selectedProject.projectBackground || '暂无项目背景' }}</p>
              </div>
              <div class="detail-section">
                <h4>类似产品参考</h4>
                <p class="summary">{{ selectedProject.similarProducts || '暂无类似产品参考' }}</p>
              </div>
              <div class="detail-section">
                <h4>目标客户群体</h4>
                <p class="summary">{{ selectedProject.targetCustomerGroups || '暂无目标客户群体' }}</p>
              </div>
              <div class="detail-section">
                <h4>商业价值</h4>
                <p class="summary">{{ selectedProject.commercialValue || '暂无商业价值描述' }}</p>
              </div>
              <div class="detail-section">
                <h4>产品核心价值</h4>
                <p class="summary">{{ selectedProject.coreProductValue || '暂无产品核心价值' }}</p>
              </div>
            </template>

            <template v-else>
              <div class="meta-grid">
                <div><strong>ID:</strong> {{ selectedProject.id }}</div>
                <div><strong>项目Key:</strong> {{ selectedProject.projectKey }}</div>
              </div>

              <div class="form-grid form-grid-project detail-edit-grid">
                <input v-model.trim="projectEditForm.projectName" class="input" placeholder="项目名称" />
                <select v-model="projectEditForm.projectType" class="input">
                  <option value="">项目类型（可选）</option>
                  <option value="PRODUCT">产品型</option>
                  <option value="PLATFORM">平台型</option>
                  <option value="OPS">运维型</option>
                  <option value="INTEGRATION">集成型</option>
                </select>
                <select v-model="projectEditForm.priority" class="input">
                  <option value="P0">P0</option>
                  <option value="P1">P1</option>
                  <option value="P2">P2</option>
                  <option value="P3">P3</option>
                </select>
                <select v-model="projectEditForm.visibility" class="input">
                  <option value="PRIVATE">PRIVATE</option>
                  <option value="ORG">ORG</option>
                </select>
                <select v-model="projectEditForm.status" class="input">
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="ARCHIVED">ARCHIVED</option>
                  <option value="PAUSED">PAUSED</option>
                </select>
                <select v-model="projectEditForm.ownerUserId" class="input">
                  <option value="">负责人（保持不变）</option>
                  <option v-for="u in userOptions" :key="u.id" :value="String(u.id)">
                    {{ u.displayName || u.username }} ({{ u.username }})
                  </option>
                </select>
                <input v-model="projectEditForm.startDate" type="date" class="input" />
                <input v-model="projectEditForm.targetDate" type="date" class="input" />
                <input v-model.trim="projectEditForm.tags" class="input" placeholder="标签（逗号分隔）" />
              </div>

              <textarea v-model="projectEditForm.description" class="input" placeholder="项目描述（可选）" />

              <div class="detail-section">
                <h4>产品信息维护</h4>
                <textarea v-model="projectEditForm.projectBackground" class="input" placeholder="项目背景（可选）" />
                <textarea v-model="projectEditForm.similarProducts" class="input" placeholder="类似产品 / 参考产品（可选）" />
                <textarea v-model="projectEditForm.targetCustomerGroups" class="input" placeholder="目标客户群体（可选）" />
                <textarea v-model="projectEditForm.commercialValue" class="input" placeholder="商业价值（可选）" />
                <textarea v-model="projectEditForm.coreProductValue" class="input" placeholder="产品核心价值（可选）" />
              </div>
            </template>
          </section>

          <section class="card">
            <h3>项目成员管理</h3>
            <div class="form-grid">
              <select v-model="memberForm.selectedUserId" class="input">
                <option value="">从用户列表选择（可选）</option>
                <option v-for="u in userOptions" :key="u.id" :value="String(u.id)">
                  {{ u.displayName || u.username }} ({{ u.id }})
                </option>
              </select>
              <input v-model.trim="memberForm.userId" class="input" placeholder="用户ID（如 1）" />
              <select v-model="memberForm.projectRole" class="input">
                <option value="OWNER">OWNER</option>
                <option value="PM">PM</option>
                <option value="DEV">DEV</option>
                <option value="QA">QA</option>
                <option value="VIEWER">VIEWER</option>
              </select>
              <button class="primary" :disabled="loading || !(memberForm.selectedUserId || memberForm.userId)" @click="addProjectMember">
                添加成员
              </button>
            </div>

            <table class="table">
              <thead>
                <tr>
                  <th>用户ID</th>
                  <th>用户名</th>
                  <th>显示名</th>
                  <th>项目角色</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="m in membersOf(selectedProject.id)" :key="m.id">
                  <td>{{ m.userId }}</td>
                  <td>{{ m.username || '-' }}</td>
                  <td>{{ m.displayName || '-' }}</td>
                  <td>{{ m.projectRole }}</td>
                  <td>{{ m.status }}</td>
                  <td class="ops">
                    <button class="mini" @click="removeProjectMember(m.userId)">移除</button>
                  </td>
                </tr>
                <tr v-if="membersOf(selectedProject.id).length === 0">
                  <td colspan="6" class="empty small">暂无项目成员</td>
                </tr>
              </tbody>
            </table>
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
                    <button class="mini" @click="selectRequirement(selectedProject.id, r.id)">查看</button>
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
            <p class="summary">{{ selectedRequirement.summary || '暂无需求摘要' }}</p>
            <div class="row">
              <button class="primary" @click="openWorkbench(selectedRequirement.id)">进入 AI 工作台</button>
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
  projectBackground?: string
  similarProducts?: string
  targetCustomerGroups?: string
  commercialValue?: string
  coreProductValue?: string
  projectType?: string
  priority?: string
  startDate?: string
  targetDate?: string
  tags?: string
  visibility: string
  status: string
  ownerUserId?: number
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
type UserOption = {
  id: number
  username: string
  displayName?: string
  status?: string
}
type ProjectMemberItem = {
  id: number
  projectId: number
  userId: number
  username?: string
  displayName?: string
  projectRole: string
  status: string
}
type ProjectProductGuideAnswer = {
  question: string
  answer: string
}
type ProjectProductGuideResult = {
  assistantMessage: string
  followUpQuestions: string[]
  projectBackground: string
  similarProducts: string
  targetCustomerGroups: string
  commercialValue: string
  coreProductValue: string
}

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const error = ref('')
const success = ref('')

const projects = ref<ProjectItem[]>([])
const requirementsMap = ref<Record<number, RequirementItem[]>>({})
const versionsMap = ref<Record<number, VersionItem[]>>({})
const projectMembersMap = ref<Record<number, ProjectMemberItem[]>>({})
const userOptions = ref<UserOption[]>([])
const expandedProjectIds = ref<number[]>([])
const selectedProjectId = ref<number | null>(null)
const selectedRequirementId = ref<number | null>(null)
const editingProject = ref(false)

const projectAiLoading = ref(false)
const projectAiMessage = ref('')
const projectAiQuestions = ref<string[]>([])
const projectAiAnswers = ref<string[]>([])
const projectAiSuggestions = reactive({
  projectBackground: '',
  similarProducts: '',
  targetCustomerGroups: '',
  commercialValue: '',
  coreProductValue: ''
})

const projectForm = reactive(createProjectFormState())
const projectEditForm = reactive(createProjectEditFormState())

const reqForm = reactive({
  title: '',
  summary: '',
  priority: 'P2',
  status: 'DRAFT'
})
const memberForm = reactive({
  selectedUserId: '',
  userId: '',
  projectRole: 'DEV'
})

const selectedProject = computed(() => projects.value.find((p) => p.id === selectedProjectId.value) || null)
const selectedRequirement = computed(() => {
  if (!selectedProjectId.value || !selectedRequirementId.value) return null
  return requirementsOf(selectedProjectId.value).find((r) => r.id === selectedRequirementId.value) || null
})
const canGuideProjectInfo = computed(() => !!(projectForm.projectName || projectForm.description || projectForm.projectBackground))
const hasProjectAiSuggestions = computed(() => !!(
  projectAiSuggestions.projectBackground ||
  projectAiSuggestions.similarProducts ||
  projectAiSuggestions.targetCustomerGroups ||
  projectAiSuggestions.commercialValue ||
  projectAiSuggestions.coreProductValue
))

function createProjectFormState() {
  return {
    projectKey: '',
    projectName: '',
    description: '',
    projectBackground: '',
    similarProducts: '',
    targetCustomerGroups: '',
    commercialValue: '',
    coreProductValue: '',
    projectType: '',
    priority: 'P2',
    startDate: '',
    targetDate: '',
    tags: '',
    ownerUserId: '',
    visibility: 'PRIVATE'
  }
}

function createProjectEditFormState() {
  return {
    projectName: '',
    description: '',
    projectBackground: '',
    similarProducts: '',
    targetCustomerGroups: '',
    commercialValue: '',
    coreProductValue: '',
    projectType: '',
    priority: 'P2',
    startDate: '',
    targetDate: '',
    tags: '',
    ownerUserId: '',
    visibility: 'PRIVATE',
    status: 'ACTIVE'
  }
}

function assignProjectForm(target: ReturnType<typeof createProjectFormState>, source: ReturnType<typeof createProjectFormState>) {
  Object.assign(target, source)
}

function assignProjectEditForm(target: ReturnType<typeof createProjectEditFormState>, source: ReturnType<typeof createProjectEditFormState>) {
  Object.assign(target, source)
}

function resetCreateProjectForm() {
  assignProjectForm(projectForm, createProjectFormState())
}

function resetProjectEditForm() {
  assignProjectEditForm(projectEditForm, createProjectEditFormState())
}

function fillProjectEditForm(project: ProjectItem) {
  assignProjectEditForm(projectEditForm, {
    projectName: project.projectName || '',
    description: project.description || '',
    projectBackground: project.projectBackground || '',
    similarProducts: project.similarProducts || '',
    targetCustomerGroups: project.targetCustomerGroups || '',
    commercialValue: project.commercialValue || '',
    coreProductValue: project.coreProductValue || '',
    projectType: project.projectType || '',
    priority: project.priority || 'P2',
    startDate: project.startDate || '',
    targetDate: project.targetDate || '',
    tags: project.tags || '',
    ownerUserId: project.ownerUserId ? String(project.ownerUserId) : '',
    visibility: project.visibility || 'PRIVATE',
    status: project.status || 'ACTIVE'
  })
}

function requirementsOf(projectId: number): RequirementItem[] {
  return requirementsMap.value[projectId] || []
}

function versionsOf(requirementId: number): VersionItem[] {
  return versionsMap.value[requirementId] || []
}

function membersOf(projectId: number): ProjectMemberItem[] {
  return projectMembersMap.value[projectId] || []
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
    if (selectedProjectId.value && projects.value.some((p) => p.id === selectedProjectId.value)) {
      if (editingProject.value && selectedProject.value) {
        fillProjectEditForm(selectedProject.value)
      }
      return
    }
    if (projects.value.length > 0) {
      await selectProject(projects.value[0].id)
    } else {
      selectedProjectId.value = null
      selectedRequirementId.value = null
      editingProject.value = false
      resetProjectEditForm()
    }
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载项目失败'
  } finally {
    loading.value = false
  }
}

async function loadUserOptions() {
  try {
    const res = await axios.get<ApiResponse<UserOption[]>>('/api/system/users')
    userOptions.value = (res.data.data || []).filter((u) => u.status !== 'DISABLED')
  } catch {
    userOptions.value = []
  }
}

async function loadRequirements(projectId: number) {
  try {
    const res = await axios.get<ApiResponse<RequirementItem[]>>(`/api/projects/${projectId}/requirements`)
    requirementsMap.value = { ...requirementsMap.value, [projectId]: res.data.data || [] }
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载需求失败'
  }
}

async function loadProjectMembers(projectId: number) {
  try {
    const res = await axios.get<ApiResponse<ProjectMemberItem[]>>(`/api/projects/${projectId}/members`)
    projectMembersMap.value = { ...projectMembersMap.value, [projectId]: res.data.data || [] }
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载项目成员失败'
  }
}

async function loadVersions(requirementId: number) {
  try {
    const res = await axios.get<ApiResponse<VersionItem[]>>(`/api/requirements/${requirementId}/versions`)
    versionsMap.value = { ...versionsMap.value, [requirementId]: res.data.data || [] }
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载版本失败'
  }
}

async function createProject() {
  if (!projectForm.projectKey.trim() || !projectForm.projectName.trim()) {
    error.value = '请先填写项目 Key 和项目名称'
    success.value = ''
    return
  }

  if (projectForm.startDate && projectForm.targetDate && projectForm.targetDate < projectForm.startDate) {
    error.value = '计划结束日期不能早于计划开始日期'
    return
  }

  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const res = await axios.post<ApiResponse<number>>('/api/projects', {
      ...projectForm,
      ownerUserId: projectForm.ownerUserId ? Number(projectForm.ownerUserId) : null,
      startDate: projectForm.startDate || null,
      targetDate: projectForm.targetDate || null,
      projectType: projectForm.projectType || null,
      tags: projectForm.tags || null
    })
    const createdProjectId = res.data.data

    success.value = '项目创建成功'
    resetCreateProjectForm()
    resetProjectAiGuide()
    await loadProjects()
    if (createdProjectId) {
      await selectProject(createdProjectId)
    }
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '创建项目失败'
  } finally {
    loading.value = false
  }
}

function startEditProject() {
  if (!selectedProject.value) return
  fillProjectEditForm(selectedProject.value)
  editingProject.value = true
  error.value = ''
  success.value = ''
}

function cancelProjectEdit() {
  editingProject.value = false
  if (selectedProject.value) {
    fillProjectEditForm(selectedProject.value)
  } else {
    resetProjectEditForm()
  }
}

async function saveProjectEdit() {
  if (!selectedProject.value) return
  if (!projectEditForm.projectName) {
    error.value = '请填写项目名称'
    return
  }
  if (projectEditForm.startDate && projectEditForm.targetDate && projectEditForm.targetDate < projectEditForm.startDate) {
    error.value = '计划结束日期不能早于计划开始日期'
    return
  }

  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await axios.put(`/api/projects/${selectedProject.value.id}`, {
      projectName: projectEditForm.projectName,
      description: projectEditForm.description || null,
      projectBackground: projectEditForm.projectBackground || null,
      similarProducts: projectEditForm.similarProducts || null,
      targetCustomerGroups: projectEditForm.targetCustomerGroups || null,
      commercialValue: projectEditForm.commercialValue || null,
      coreProductValue: projectEditForm.coreProductValue || null,
      projectType: projectEditForm.projectType || null,
      priority: projectEditForm.priority || null,
      startDate: projectEditForm.startDate || null,
      targetDate: projectEditForm.targetDate || null,
      tags: projectEditForm.tags || null,
      visibility: projectEditForm.visibility || null,
      status: projectEditForm.status || null,
      ownerUserId: projectEditForm.ownerUserId ? Number(projectEditForm.ownerUserId) : null
    })
    success.value = '项目信息更新成功'
    editingProject.value = false
    const projectId = selectedProject.value.id
    await loadProjects()
    await selectProject(projectId)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '更新项目信息失败'
  } finally {
    loading.value = false
  }
}

async function guideProjectProductInfo() {
  if (!canGuideProjectInfo.value) {
    error.value = '请至少填写项目名称、项目描述或项目背景中的一项后再使用 AI 补全'
    return
  }
  projectAiLoading.value = true
  error.value = ''
  success.value = ''
  try {
    const answers: ProjectProductGuideAnswer[] = projectAiQuestions.value.map((question, idx) => ({
      question,
      answer: projectAiAnswers.value[idx] || ''
    }))
    const res = await axios.post<ApiResponse<ProjectProductGuideResult>>('/api/projects/ai/product-info/guide', {
      projectName: projectForm.projectName || null,
      description: projectForm.description || null,
      projectBackground: projectForm.projectBackground || null,
      similarProducts: projectForm.similarProducts || null,
      targetCustomerGroups: projectForm.targetCustomerGroups || null,
      commercialValue: projectForm.commercialValue || null,
      coreProductValue: projectForm.coreProductValue || null,
      answers
    })
    const data = res.data.data
    projectAiMessage.value = data.assistantMessage || ''
    projectAiSuggestions.projectBackground = data.projectBackground || ''
    projectAiSuggestions.similarProducts = data.similarProducts || ''
    projectAiSuggestions.targetCustomerGroups = data.targetCustomerGroups || ''
    projectAiSuggestions.commercialValue = data.commercialValue || ''
    projectAiSuggestions.coreProductValue = data.coreProductValue || ''

    const nextQuestions = data.followUpQuestions || []
    const existingAnswers = new Map<string, string>()
    projectAiQuestions.value.forEach((question, idx) => {
      existingAnswers.set(question, projectAiAnswers.value[idx] || '')
    })
    projectAiQuestions.value = nextQuestions
    projectAiAnswers.value = nextQuestions.map((question) => existingAnswers.get(question) || '')
    success.value = nextQuestions.length > 0 ? 'AI 已生成建议并提出补充问题' : 'AI 已生成完整建议'
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || 'AI 补全失败'
  } finally {
    projectAiLoading.value = false
  }
}

function applyProjectAiSuggestions() {
  if (!hasProjectAiSuggestions.value) {
    error.value = '当前没有可应用的 AI 建议'
    return
  }
  projectForm.projectBackground = projectAiSuggestions.projectBackground || projectForm.projectBackground
  projectForm.similarProducts = projectAiSuggestions.similarProducts || projectForm.similarProducts
  projectForm.targetCustomerGroups = projectAiSuggestions.targetCustomerGroups || projectForm.targetCustomerGroups
  projectForm.commercialValue = projectAiSuggestions.commercialValue || projectForm.commercialValue
  projectForm.coreProductValue = projectAiSuggestions.coreProductValue || projectForm.coreProductValue
  success.value = 'AI 建议已应用到项目表单'
}

function resetProjectAiGuide() {
  projectAiMessage.value = ''
  projectAiQuestions.value = []
  projectAiAnswers.value = []
  projectAiSuggestions.projectBackground = ''
  projectAiSuggestions.similarProducts = ''
  projectAiSuggestions.targetCustomerGroups = ''
  projectAiSuggestions.commercialValue = ''
  projectAiSuggestions.coreProductValue = ''
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
    error.value = e?.response?.data?.message || e?.message || '创建需求失败'
  } finally {
    loading.value = false
  }
}

async function selectProject(projectId: number) {
  selectedProjectId.value = projectId
  selectedRequirementId.value = null
  editingProject.value = false
  resetProjectEditForm()
  if (!isExpanded(projectId)) {
    expandedProjectIds.value.push(projectId)
  }
  await Promise.all([loadRequirements(projectId), loadProjectMembers(projectId)])
  if (selectedProject.value) {
    fillProjectEditForm(selectedProject.value)
  }
}

async function selectRequirement(projectId: number, requirementId: number) {
  selectedProjectId.value = projectId
  selectedRequirementId.value = requirementId
  editingProject.value = false
  if (!isExpanded(projectId)) {
    expandedProjectIds.value.push(projectId)
  }
  if (!requirementsMap.value[projectId]) {
    await loadRequirements(projectId)
  }
  if (selectedProject.value) {
    fillProjectEditForm(selectedProject.value)
  }
  await loadVersions(requirementId)
}

async function addProjectMember() {
  if (!selectedProjectId.value) return
  const uid = Number(memberForm.selectedUserId || memberForm.userId)
  if (!uid || uid <= 0) {
    error.value = '请输入有效的用户ID'
    return
  }
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await axios.post(`/api/projects/${selectedProjectId.value}/members`, {
      userId: uid,
      projectRole: memberForm.projectRole
    })
    success.value = '项目成员添加成功'
    memberForm.selectedUserId = ''
    memberForm.userId = ''
    await loadProjectMembers(selectedProjectId.value)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '添加项目成员失败'
  } finally {
    loading.value = false
  }
}

async function removeProjectMember(userId: number) {
  if (!selectedProjectId.value) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await axios.delete(`/api/projects/${selectedProjectId.value}/members/${userId}`)
    success.value = '项目成员移除成功'
    await loadProjectMembers(selectedProjectId.value)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '移除项目成员失败'
  } finally {
    loading.value = false
  }
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

function goCreateAi() {
  router.push('/projects/create-ai')
}

onMounted(async () => {
  await Promise.all([loadProjects(), loadUserOptions()])
})
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
.top-actions {
  display: flex;
  gap: 8px;
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
.form-grid-project,
.detail-edit-grid {
  grid-template-columns: repeat(2, minmax(120px, 1fr));
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
  white-space: pre-wrap;
}
.product-info-box,
.ai-preview,
.detail-section {
  margin-top: 12px;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.section-head h3,
.section-head h4,
.ai-preview h4,
.detail-section h4 {
  margin: 0;
  font-size: 14px;
}
.compact {
  margin-top: 0;
}
.ai-assistant {
  margin-top: 10px;
  padding: 10px;
  border-radius: 8px;
  background: #eef4ff;
  color: #1e3a8a;
  font-size: 13px;
}
.ai-qa-list {
  display: grid;
  gap: 10px;
  margin-top: 10px;
}
.ai-question-card {
  border: 1px solid #dbe2ea;
  border-radius: 10px;
  padding: 10px;
  background: #fafcff;
}
.ai-question {
  font-size: 13px;
  color: #1f2937;
  margin-bottom: 6px;
}
.preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(180px, 1fr));
  gap: 10px;
  margin-top: 10px;
}
.preview-item {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px;
  background: #fbfdff;
}
.preview-item p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #4b5563;
  white-space: pre-wrap;
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
  .preview-grid {
    grid-template-columns: 1fr;
  }
  .form-grid,
  .form-grid-project,
  .detail-edit-grid {
    grid-template-columns: 1fr;
  }
}
</style>
