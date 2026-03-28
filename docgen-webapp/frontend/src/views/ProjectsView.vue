<template>
  <div class="page">
    <div class="layout">
      <ProjectsSidebar
        :loading="loading"
        :projects="projects"
        :selected-project-id="selectedProjectId"
        :project-count="projects.length"
        :requirement-count="selectedProject ? requirementsOf(selectedProject.id).length : 0"
        :pending-knowledge-count="pendingKnowledgeCount"
        @reload-projects="loadProjects"
        @select-project="selectProject"
      />

      <main class="content">
        <EmptyWorkspaceState
          v-if="!selectedProject"
          title="请选择一个项目开始协同"
          description="先在左侧选择项目，这里会切换到项目概览、AI 协同和需求管理工作区。"
        />

        <template v-else>
          <ProjectWorkspaceHeader
            :project="selectedProject"
            :active-tab="activeWorkspaceTab"
            :project-type-label="projectTypeLabel"
            :visibility-label="visibilityLabel"
            :project-status-label="projectStatusLabel"
            @change-tab="handleWorkspaceTabChange"
            @enter-ai="ensureAiWorkspace"
          />

          <section v-if="activeWorkspaceTab === 'overview'" class="workspace-stack">
            <ProjectOverviewPanel
              :loading="loading"
              :project="selectedProject"
              :project-type-label="projectTypeLabel"
              :visibility-label="visibilityLabel"
              :project-status-label="projectStatusLabel"
              @start-edit="startEditProject"
              @enter-ai="ensureAiWorkspace"
              @delete-project="deleteProject"
            />

            <ProjectMembersCard
              :loading="loading"
              :member-form="memberForm"
              :user-options="userOptions"
              :members="membersOf(selectedProject.id)"
              :project-role-label="projectRoleLabel"
              :member-status-label="memberStatusLabel"
              @add-member="addProjectMember"
              @remove-member="removeProjectMember"
            />
          </section>

          <section v-else class="workspace-stack">
            <ProjectAiWorkspacePanel
              :loading="loading"
              :project="selectedProject"
              :project-edit-form="projectEditForm"
              :project-conversation-loading="projectConversationLoading"
              :project-conversation="projectConversation"
              :project-conversation-input="projectConversationInput"
              :can-send-project-conversation="canSendProjectConversation"
              :project-url-draft="projectUrlDraft"
              :project-text-draft="projectTextDraft"
              :project-file-draft="projectFileDraft"
              :project-selected-file="projectSelectedFile"
              :project-pending-materials="projectPendingMaterials"
              :project-material-knowledge-map="projectMaterialKnowledgeMap"
              :project-material-filter="projectMaterialFilter"
              :filtered-project-materials="filteredProjectMaterials"
              :project-materials-collapsed="projectMaterialsCollapsed"
              :can-save-project-materials="canSaveProjectMaterials"
              :can-upload-project-file="canUploadProjectFile"
              :project-material-preview="projectMaterialPreview"
              :project-knowledge-status-text="projectKnowledgeStatusText"
              :project-knowledge-preview-visible="projectKnowledgePreviewVisible"
              :project-knowledge-preview-loading="projectKnowledgePreviewLoading"
              :project-knowledge-preview="projectKnowledgePreview"
              :project-knowledge-preview-query-text="projectKnowledgePreviewQueryText"
              :user-options="userOptions"
              @cancel-edit="cancelProjectEdit"
              @save-edit="saveProjectEdit"
              @delete-project="deleteProject"
              @refresh-project-ai="ensureProjectConversation"
              @send-project-ai="sendProjectConversation"
              @apply-project-ai="applyConversationStructuredInfo"
              @update-project-ai-input="projectConversationInput = $event"
              @add-project-url-material="addProjectUrlMaterial"
              @add-project-text-material="addProjectTextMaterial"
              @clear-project-pending-materials="clearProjectPendingMaterials"
              @save-project-materials="saveProjectMaterials"
              @select-project-file="handleProjectFileSelect"
              @upload-project-file="uploadProjectFileMaterial"
              @delete-project-material="deleteProjectMaterial"
              @open-project-knowledge-detail="goProjectKnowledgeLibrary"
              @retry-project-knowledge-document="retryProjectKnowledgeDocument"
              @load-project-knowledge-preview="loadProjectKnowledgePreview"
              @update-project-material-filter="projectMaterialFilter = $event"
              @toggle-project-materials-collapse="projectMaterialsCollapsed = !projectMaterialsCollapsed"
            />
          </section>
        </template>
      </main>
    </div>

    <ProjectKnowledgeDetailModal
      :visible="projectKnowledgeDetailVisible"
      :loading="projectKnowledgeDetailLoading"
      :reprocessing="projectKnowledgeReprocessing"
      :detail="projectKnowledgeDetail"
      :chunk-expanded="projectKnowledgeChunkExpanded"
      :visible-chunks="visibleProjectKnowledgeChunks"
      @close="closeProjectKnowledgeDetail"
      @reprocess="reprocessProjectKnowledgeDetail"
      @toggle-chunks="projectKnowledgeChunkExpanded = !projectKnowledgeChunkExpanded"
    />

    <div class="feedback-stack">
      <FeedbackPanel title="下一步建议" :message="workspaceAdvice" tone="warning" />
      <FeedbackPanel title="处理提示" :message="error" tone="danger" />
      <FeedbackPanel title="最新进展" :message="success" tone="success" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import axios from 'axios'
import { useRoute, useRouter } from 'vue-router'
import EmptyWorkspaceState from '../components/projects/EmptyWorkspaceState.vue'
import FeedbackPanel from '../components/projects/FeedbackPanel.vue'
import ProjectAiWorkspacePanel from '../components/projects/ProjectAiWorkspacePanel.vue'
import ProjectKnowledgeDetailModal from '../components/projects/ProjectKnowledgeDetailModal.vue'
import ProjectMembersCard from '../components/projects/ProjectMembersCard.vue'
import ProjectOverviewPanel from '../components/projects/ProjectOverviewPanel.vue'
import ProjectsSidebar from '../components/projects/ProjectsSidebar.vue'
import ProjectWorkspaceHeader from '../components/projects/ProjectWorkspaceHeader.vue'
import {
  memberStatusLabel,
  priorityLabel,
  projectRoleLabel,
  projectStatusLabel,
  projectTypeLabel,
  requirementStatusLabel,
  visibilityLabel
} from '../components/projects/labels'
import type {
  ApiResponse,
  FileDraftState,
  MemberFormState,
  ProjectConversationView,
  ProjectAiSuggestions,
  ProjectKnowledgeDocumentChunk,
  ProjectKnowledgeDocumentDetail,
  ProjectKnowledgePreview,
  ProjectEditFormState,
  ProjectFormState,
  ProjectKnowledgeDocumentListItem,
  ProjectItem,
  ProjectSourceMaterial,
  ProjectMemberItem,
  ProjectProductGuideAnswer,
  ProjectProductGuideResult,
  RequirementFormState,
  TextDraftState,
  RequirementItem,
  UserOption,
  UrlDraftState,
  VersionItem
} from '../components/projects/types'

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
const activeWorkspaceTab = ref<'overview' | 'ai'>('overview')

const projectAiLoading = ref(false)
const projectAiMessage = ref('')
const projectAiQuestions = ref<string[]>([])
const projectAiAnswers = ref<string[]>([])
const projectConversationLoading = ref(false)
const projectConversationInput = ref('')
const projectConversation = ref<ProjectConversationView | null>(null)
const projectMaterialKnowledgeMap = ref<Record<number, ProjectKnowledgeDocumentListItem[]>>({})
const projectPendingMaterials = ref<ProjectSourceMaterial[]>([])
const projectSelectedFile = ref<File | null>(null)
const projectMaterialFilter = ref<'ALL' | 'URL' | 'TEXT' | 'FILE'>('ALL')
const projectMaterialsCollapsed = ref(false)
const projectKnowledgeDetailVisible = ref(false)
const projectKnowledgeDetailLoading = ref(false)
const projectKnowledgeReprocessing = ref(false)
const projectKnowledgeDetail = ref<ProjectKnowledgeDocumentDetail | null>(null)
const projectKnowledgeChunkExpanded = ref(false)
const projectKnowledgePreviewVisible = ref(false)
const projectKnowledgePreviewLoading = ref(false)
const projectKnowledgePreview = ref<ProjectKnowledgePreview | null>(null)
const projectAiSuggestions = reactive<ProjectAiSuggestions>({
  projectBackground: '',
  similarProducts: '',
  targetCustomerGroups: '',
  commercialValue: '',
  coreProductValue: ''
})

const projectForm = reactive<ProjectFormState>(createProjectFormState())
const projectEditForm = reactive<ProjectEditFormState>(createProjectEditFormState())
const reqForm = reactive<RequirementFormState>({
  title: '',
  summary: '',
  priority: 'P2',
  status: 'DRAFT'
})
const memberForm = reactive<MemberFormState>({
  selectedUserId: '',
  userId: '',
  projectRole: 'DEV'
})
const projectUrlDraft = reactive<UrlDraftState>({ title: '', sourceUri: '' })
const projectTextDraft = reactive<TextDraftState>({ title: '', rawContent: '' })
const projectFileDraft = reactive<FileDraftState>({ title: '' })

const selectedProject = computed(() => projects.value.find((project) => project.id === selectedProjectId.value) || null)
const selectedRequirement = computed(() => {
  if (!selectedProjectId.value || !selectedRequirementId.value) {
    return null
  }
  return requirementsOf(selectedProjectId.value).find((item) => item.id === selectedRequirementId.value) || null
})
const canGuideProjectInfo = computed(() => !!(projectForm.projectName || projectForm.description || projectForm.projectBackground))
const hasProjectAiSuggestions = computed(() => Object.values(projectAiSuggestions).some(Boolean))
const canSendProjectConversation = computed(() => !!projectConversationInput.value.trim())
const canSaveProjectMaterials = computed(() => !!projectConversation.value?.sessionId && projectPendingMaterials.value.length > 0)
const canUploadProjectFile = computed(() => !!projectConversation.value?.sessionId && !!projectSelectedFile.value)
const filteredProjectMaterials = computed(() => {
  const materials = projectConversation.value?.materials || []
  if (projectMaterialFilter.value === 'ALL') {
    return materials
  }
  return materials.filter((item) => item.materialType === projectMaterialFilter.value)
})
const visibleProjectKnowledgeChunks = computed<ProjectKnowledgeDocumentChunk[]>(() => {
  const chunks = projectKnowledgeDetail.value?.chunks || []
  return projectKnowledgeChunkExpanded.value ? chunks : chunks.slice(0, 3)
})
const projectKnowledgePreviewQueryText = computed(() => {
  return projectKnowledgePreview.value?.query || projectConversationInput.value.trim() || projectEditForm.projectName || selectedProject.value?.projectName || ''
})
const uniqueKnowledgeDocuments = computed(() => {
  const docs = Object.values(projectMaterialKnowledgeMap.value).flat()
  const seen = new Map<number, ProjectKnowledgeDocumentListItem>()
  docs.forEach((doc) => {
    if (!seen.has(doc.id)) {
      seen.set(doc.id, doc)
    }
  })
  return Array.from(seen.values())
})
const knowledgeCount = computed(() => uniqueKnowledgeDocuments.value.length)
const pendingKnowledgeCount = computed(() => uniqueKnowledgeDocuments.value.filter((doc) => ['PENDING', 'RUNNING', 'PROCESSING'].includes(doc.status || '') || ['PENDING', 'RUNNING', 'PROCESSING'].includes(doc.latestTaskStatus || '')).length)
const failedKnowledgeCount = computed(() => uniqueKnowledgeDocuments.value.filter((doc) => doc.status === 'FAILED' || doc.latestTaskStatus === 'FAILED').length)
const workspaceAdvice = computed(() => {
  if (!selectedProject.value) {
    return '先在左侧选择一个项目，右侧将切换到项目对象信息与维护区域。'
  }
  if (failedKnowledgeCount.value > 0) {
    return `当前有 ${failedKnowledgeCount.value} 个知识任务失败，建议先进入 AI 协同处理异常，再继续维护项目。`
  }
  if (!projectConversation.value?.sessionId) {
    return '建议先进入 AI 协同页，建立项目会话并完成一轮项目框架优化。'
  }
  return '当前项目基础信息已经就绪，可以继续维护项目资料、补充成员或使用 AI 校准项目框架。'
})

function createProjectFormState(): ProjectFormState {
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

function createProjectEditFormState(): ProjectEditFormState {
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

function assignProjectForm(target: ProjectFormState, source: ProjectFormState) {
  Object.assign(target, source)
}

function assignProjectEditForm(target: ProjectEditFormState, source: ProjectEditFormState) {
  Object.assign(target, source)
}

function resetCreateProjectForm() {
  assignProjectForm(projectForm, createProjectFormState())
}

function resetProjectEditForm() {
  assignProjectEditForm(projectEditForm, createProjectEditFormState())
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

function resetProjectConversation() {
  activeWorkspaceTab.value = 'overview'
  projectConversation.value = null
  projectConversationInput.value = ''
  projectMaterialKnowledgeMap.value = {}
  projectPendingMaterials.value = []
  projectSelectedFile.value = null
  projectMaterialFilter.value = 'ALL'
  projectMaterialsCollapsed.value = false
  projectKnowledgeDetailVisible.value = false
  projectKnowledgeDetailLoading.value = false
  projectKnowledgeDetail.value = null
  projectKnowledgeChunkExpanded.value = false
  projectKnowledgePreviewVisible.value = false
  projectKnowledgePreviewLoading.value = false
  projectKnowledgePreview.value = null
  projectUrlDraft.title = ''
  projectUrlDraft.sourceUri = ''
  projectTextDraft.title = ''
  projectTextDraft.rawContent = ''
  projectFileDraft.title = ''
}

function projectMaterialPreview(value?: string) {
  if (!value) return '-'
  return value.length > 80 ? `${value.slice(0, 80)}...` : value
}

function projectKnowledgeStatusText(status?: string) {
  if (!status) return '未知'
  if (status === 'READY' || status === 'SUCCESS') return '处理完成'
  if (status === 'FAILED') return '处理失败'
  if (status === 'PROCESSING' || status === 'RUNNING') return '处理中'
  if (status === 'PENDING') return '待处理'
  return status
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

async function toggleProject(projectId: number) {
  if (isExpanded(projectId)) {
    expandedProjectIds.value = expandedProjectIds.value.filter((id) => id !== projectId)
    return
  }
  expandedProjectIds.value.push(projectId)
  await loadRequirements(projectId)
}

async function loadProjects() {
  loading.value = true
  error.value = ''
  try {
    const res = await axios.get<ApiResponse<ProjectItem[]>>('/api/projects')
    projects.value = res.data.data || []

    const queryProjectId = Number(route.query.projectId || 0)
    if (queryProjectId > 0 && projects.value.some((project) => project.id === queryProjectId)) {
      await selectProject(queryProjectId)
      return
    }

    if (selectedProjectId.value && projects.value.some((project) => project.id === selectedProjectId.value)) {
      if (editingProject.value && selectedProject.value) {
        fillProjectEditForm(selectedProject.value)
      }
      return
    }

    if (projects.value.length > 0) {
      await selectProject(projects.value[0].id)
      return
    }

    selectedProjectId.value = null
    selectedRequirementId.value = null
    editingProject.value = false
    resetProjectEditForm()
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载项目失败。'
  } finally {
    loading.value = false
  }
}

async function loadUserOptions() {
  try {
    const res = await axios.get<ApiResponse<UserOption[]>>('/api/system/users')
    userOptions.value = (res.data.data || []).filter((item) => item.status !== 'DISABLED')
  } catch {
    userOptions.value = []
  }
}

async function loadRequirements(projectId: number) {
  try {
    const res = await axios.get<ApiResponse<RequirementItem[]>>(`/api/projects/${projectId}/requirements`)
    requirementsMap.value = { ...requirementsMap.value, [projectId]: res.data.data || [] }
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载需求失败。'
  }
}

async function loadProjectMembers(projectId: number) {
  try {
    const res = await axios.get<ApiResponse<ProjectMemberItem[]>>(`/api/projects/${projectId}/members`)
    projectMembersMap.value = { ...projectMembersMap.value, [projectId]: res.data.data || [] }
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载项目成员失败。'
  }
}

async function loadVersions(requirementId: number) {
  try {
    const res = await axios.get<ApiResponse<VersionItem[]>>(`/api/requirements/${requirementId}/versions`)
    versionsMap.value = { ...versionsMap.value, [requirementId]: res.data.data || [] }
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载版本失败。'
  }
}

async function createProject() {
  if (!projectForm.projectKey.trim() || !projectForm.projectName.trim()) {
    error.value = '请先填写项目 Key 和项目名称。'
    success.value = ''
    return
  }
  if (projectForm.startDate && projectForm.targetDate && projectForm.targetDate < projectForm.startDate) {
    error.value = '计划结束日期不能早于计划开始日期。'
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
    success.value = '项目创建成功。'
    const createdProjectId = res.data.data
    resetCreateProjectForm()
    resetProjectAiGuide()
    await loadProjects()
    if (createdProjectId) {
      await selectProject(createdProjectId)
    }
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '创建项目失败。'
  } finally {
    loading.value = false
  }
}

function startCreateProject() {
  void router.push('/projects/create')
}

function handleWorkspaceTabChange(tab: 'overview' | 'ai') {
  if (tab === 'ai') {
    void ensureAiWorkspace()
    return
  }
  activeWorkspaceTab.value = tab
}

async function ensureAiWorkspace() {
  if (!selectedProject.value) {
    return
  }
  activeWorkspaceTab.value = 'ai'
  if (!editingProject.value) {
    await startEditProject()
    return
  }
  if (!projectConversation.value) {
    await ensureProjectConversation()
  }
}

async function startEditProject() {
  if (!selectedProject.value) {
    return
  }
  fillProjectEditForm(selectedProject.value)
  editingProject.value = true
  activeWorkspaceTab.value = 'ai'
  error.value = ''
  success.value = ''
  await ensureProjectConversation()
}

function cancelProjectEdit() {
  editingProject.value = false
  activeWorkspaceTab.value = 'overview'
  if (selectedProject.value) {
    fillProjectEditForm(selectedProject.value)
    return
  }
  resetProjectEditForm()
}

async function ensureProjectConversation() {
  if (!selectedProject.value) {
    return
  }
  projectConversationLoading.value = true
  error.value = ''
  try {
    const res = await axios.post<ApiResponse<ProjectConversationView>>(
      `/api/projects/ai/conversations/projects/${selectedProject.value.id}/resume`
    )
    projectConversation.value = res.data.data || null
    await refreshProjectMaterialKnowledgeStatuses()
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载项目 AI 会话失败。'
  } finally {
    projectConversationLoading.value = false
  }
}

async function refreshProjectMaterialKnowledgeStatuses() {
  const targets = (projectConversation.value?.materials || []).filter((item) => typeof item.id === 'number')
  if (targets.length === 0) {
    projectMaterialKnowledgeMap.value = {}
    return
  }
  const entries = await Promise.all(
    targets.map(async (item) => {
      const res = await axios.get<ApiResponse<ProjectKnowledgeDocumentListItem[]>>(
        `/api/knowledge-documents/source-materials/${item.id}`
      )
      return [item.id as number, res.data.data || []] as const
    })
  )
  projectMaterialKnowledgeMap.value = Object.fromEntries(entries)
}

async function openProjectKnowledgeDetail(documentId: number) {
  projectKnowledgeDetailVisible.value = true
  projectKnowledgeDetailLoading.value = true
  projectKnowledgeDetail.value = null
  try {
    const res = await axios.get<ApiResponse<ProjectKnowledgeDocumentDetail>>(`/api/knowledge-documents/${documentId}`)
    projectKnowledgeDetail.value = res.data.data
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载知识文档详情失败。'
  } finally {
    projectKnowledgeDetailLoading.value = false
  }
}

function goProjectKnowledgeLibrary(documentId: number) {
  void router.push({
    path: '/knowledge',
    query: {
      projectId: selectedProjectId.value ? String(selectedProjectId.value) : undefined,
      requirementId: selectedRequirementId.value ? String(selectedRequirementId.value) : undefined,
      documentId: String(documentId)
    }
  })
}

async function retryProjectKnowledgeDocument(documentId: number) {
  projectConversationLoading.value = true
  error.value = ''
  success.value = ''
  try {
    await axios.post(`/api/knowledge-documents/${documentId}/reprocess`)
    await refreshProjectMaterialKnowledgeStatuses()
    if (projectKnowledgeDetail.value?.document.id === documentId) {
      await openProjectKnowledgeDetail(documentId)
    }
    success.value = '知识文档已重新处理。'
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '重新处理知识文档失败。'
  } finally {
    projectConversationLoading.value = false
  }
}

async function reprocessProjectKnowledgeDetail() {
  if (!projectKnowledgeDetail.value) {
    return
  }
  projectKnowledgeReprocessing.value = true
  error.value = ''
  success.value = ''
  try {
    const documentId = projectKnowledgeDetail.value.document.id
    const res = await axios.post<ApiResponse<ProjectKnowledgeDocumentDetail>>(
      `/api/knowledge-documents/${documentId}/reprocess`
    )
    projectKnowledgeDetail.value = res.data.data
    await refreshProjectMaterialKnowledgeStatuses()
    success.value = '知识文档已重新处理。'
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '重新处理知识文档失败。'
  } finally {
    projectKnowledgeReprocessing.value = false
  }
}

function closeProjectKnowledgeDetail() {
  projectKnowledgeDetailVisible.value = false
  projectKnowledgeDetail.value = null
  projectKnowledgeChunkExpanded.value = false
  projectKnowledgeReprocessing.value = false
}

async function loadProjectKnowledgePreview() {
  if (!selectedProject.value || !projectConversation.value) {
    return
  }
  projectKnowledgePreviewVisible.value = true
  projectKnowledgePreviewLoading.value = true
  try {
    const query = projectConversationInput.value.trim()
    const res = await axios.get<ApiResponse<ProjectKnowledgePreview>>(
      `/api/projects/ai/conversations/projects/${selectedProject.value.id}/knowledge-preview`,
      {
        params: {
          sessionId: projectConversation.value.sessionId,
          query: query || undefined
        }
      }
    )
    projectKnowledgePreview.value = res.data.data
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载项目检索上下文失败。'
  } finally {
    projectKnowledgePreviewLoading.value = false
  }
}

async function sendProjectConversation() {
  if (!selectedProject.value || !projectConversation.value || !projectConversationInput.value.trim()) {
    return
  }
  projectConversationLoading.value = true
  error.value = ''
  success.value = ''
  try {
    await axios.post(`/api/projects/ai/conversations/projects/${selectedProject.value.id}/chat`, {
      sessionId: projectConversation.value.sessionId,
      message: projectConversationInput.value.trim()
    })
    projectConversationInput.value = ''
    await ensureProjectConversation()
    success.value = '项目 AI 已生成新的优化建议。'
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '发送项目 AI 消息失败。'
  } finally {
    projectConversationLoading.value = false
  }
}

function applyConversationStructuredInfo() {
  const structured = projectConversation.value?.structuredInfo
  if (!structured) {
    error.value = '当前没有可应用的 AI 结果。'
    return
  }
  projectEditForm.projectName = structured.projectName || projectEditForm.projectName
  projectEditForm.description = structured.description || projectEditForm.description
  projectEditForm.projectBackground = structured.projectBackground || projectEditForm.projectBackground
  projectEditForm.similarProducts = structured.similarProducts || projectEditForm.similarProducts
  projectEditForm.targetCustomerGroups = structured.targetCustomerGroups || projectEditForm.targetCustomerGroups
  projectEditForm.commercialValue = structured.commercialValue || projectEditForm.commercialValue
  projectEditForm.coreProductValue = structured.coreProductValue || projectEditForm.coreProductValue
  success.value = 'AI 优化结果已回填到项目编辑表单。'
}

function handleProjectFileSelect(file: File | null) {
  projectSelectedFile.value = file
}

function addProjectUrlMaterial() {
  if (!projectUrlDraft.sourceUri.trim()) {
    error.value = '请先填写网站链接。'
    return
  }
  projectPendingMaterials.value.push({
    materialType: 'URL',
    title: projectUrlDraft.title.trim() || undefined,
    sourceUri: projectUrlDraft.sourceUri.trim()
  })
  projectUrlDraft.title = ''
  projectUrlDraft.sourceUri = ''
  error.value = ''
}

function addProjectTextMaterial() {
  if (!projectTextDraft.rawContent.trim()) {
    error.value = '请先填写文本资料内容。'
    return
  }
  projectPendingMaterials.value.push({
    materialType: 'TEXT',
    title: projectTextDraft.title.trim() || undefined,
    rawContent: projectTextDraft.rawContent.trim()
  })
  projectTextDraft.title = ''
  projectTextDraft.rawContent = ''
  error.value = ''
}

function clearProjectPendingMaterials() {
  projectPendingMaterials.value = []
}

async function saveProjectMaterials() {
  if (!projectConversation.value?.sessionId || projectPendingMaterials.value.length === 0) {
    return
  }
  projectConversationLoading.value = true
  error.value = ''
  success.value = ''
  try {
    await axios.post(`/api/projects/ai/conversations/${projectConversation.value.sessionId}/materials`, {
      materials: projectPendingMaterials.value
    })
    projectPendingMaterials.value = []
    await ensureProjectConversation()
    success.value = '项目资料已保存到当前 AI 会话。'
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '保存项目资料失败。'
  } finally {
    projectConversationLoading.value = false
  }
}

async function uploadProjectFileMaterial() {
  if (!projectConversation.value?.sessionId || !projectSelectedFile.value) {
    return
  }
  projectConversationLoading.value = true
  error.value = ''
  success.value = ''
  try {
    const formData = new FormData()
    formData.append('file', projectSelectedFile.value)
    if (projectFileDraft.title.trim()) {
      formData.append('title', projectFileDraft.title.trim())
    }
    await axios.post(`/api/projects/ai/conversations/${projectConversation.value.sessionId}/materials/upload`, formData)
    projectSelectedFile.value = null
    projectFileDraft.title = ''
    await ensureProjectConversation()
    success.value = '项目文件资料已上传，AI 会自动继续学习。'
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '上传项目文件资料失败。'
  } finally {
    projectConversationLoading.value = false
  }
}

async function deleteProjectMaterial(materialId?: number) {
  if (!materialId) {
    return
  }
  const confirmed = window.confirm('确认删除这条资料吗？删除后，对应知识文档也会一并清理。')
  if (!confirmed) {
    return
  }
  projectConversationLoading.value = true
  error.value = ''
  success.value = ''
  try {
    await axios.delete(`/api/projects/ai/conversations/materials/${materialId}`)
    await ensureProjectConversation()
    success.value = '项目资料已删除。'
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '删除项目资料失败。'
  } finally {
    projectConversationLoading.value = false
  }
}

function findProjectFallbackId(projectId: number): number | null {
  const currentIndex = projects.value.findIndex((project) => project.id === projectId)
  if (currentIndex < 0) {
    return projects.value[0]?.id ?? null
  }
  const nextProject = projects.value[currentIndex + 1]
  if (nextProject) {
    return nextProject.id
  }
  return projects.value[currentIndex - 1]?.id ?? null
}

async function deleteProject() {
  if (!selectedProject.value) {
    return
  }

  const project = selectedProject.value
  const fallbackProjectId = findProjectFallbackId(project.id)
  const confirmed = window.confirm(
    `确认删除项目“${project.projectName}”吗？\n\n` +
      '删除后将同时清理以下内容：\n' +
      '1. 项目基础信息\n' +
      '2. 项目成员\n' +
      '3. 项目下的全部需求\n' +
      '4. 需求版本与需求对话记录\n' +
      '5. 项目 AI 会话、聊天消息与来源资料\n\n' +
      '该操作不可恢复。'
  )
  if (!confirmed) {
    return
  }

  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await axios.delete(`/api/projects/${project.id}`)
    success.value = '项目删除成功。'
    editingProject.value = false
    delete requirementsMap.value[project.id]
    delete projectMembersMap.value[project.id]
    selectedProjectId.value = fallbackProjectId
    selectedRequirementId.value = null
    resetProjectEditForm()
    await loadProjects()
    if (fallbackProjectId && projects.value.some((item) => item.id === fallbackProjectId)) {
      await selectProject(fallbackProjectId)
    }
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '删除项目失败。'
  } finally {
    loading.value = false
  }
}

async function saveProjectEdit() {
  if (!selectedProject.value) {
    return
  }
  if (!projectEditForm.projectName.trim()) {
    error.value = '请填写项目名称。'
    return
  }
  if (projectEditForm.startDate && projectEditForm.targetDate && projectEditForm.targetDate < projectEditForm.startDate) {
    error.value = '计划结束日期不能早于计划开始日期。'
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
    success.value = '项目信息更新成功。'
    editingProject.value = false
    const projectId = selectedProject.value.id
    await loadProjects()
    await selectProject(projectId)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '更新项目信息失败。'
  } finally {
    loading.value = false
  }
}

async function guideProjectProductInfo() {
  if (!canGuideProjectInfo.value) {
    error.value = '请至少填写项目名称、项目描述或项目背景中的一项后，再使用 AI 补全。'
    return
  }

  projectAiLoading.value = true
  error.value = ''
  success.value = ''
  try {
    const answers: ProjectProductGuideAnswer[] = projectAiQuestions.value.map((question, index) => ({
      question,
      answer: projectAiAnswers.value[index] || ''
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
    projectAiQuestions.value.forEach((question, index) => {
      existingAnswers.set(question, projectAiAnswers.value[index] || '')
    })
    projectAiQuestions.value = nextQuestions
    projectAiAnswers.value = nextQuestions.map((question) => existingAnswers.get(question) || '')
    success.value = nextQuestions.length > 0 ? 'AI 已生成建议并提出补充问题。' : 'AI 已生成完整建议。'
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || 'AI 补全失败。'
  } finally {
    projectAiLoading.value = false
  }
}

function applyProjectAiSuggestions() {
  if (!hasProjectAiSuggestions.value) {
    error.value = '当前没有可应用的 AI 建议。'
    return
  }
  projectForm.projectBackground = projectAiSuggestions.projectBackground || projectForm.projectBackground
  projectForm.similarProducts = projectAiSuggestions.similarProducts || projectForm.similarProducts
  projectForm.targetCustomerGroups = projectAiSuggestions.targetCustomerGroups || projectForm.targetCustomerGroups
  projectForm.commercialValue = projectAiSuggestions.commercialValue || projectForm.commercialValue
  projectForm.coreProductValue = projectAiSuggestions.coreProductValue || projectForm.coreProductValue
  success.value = 'AI 建议已应用到项目表单。'
}

async function createRequirement() {
  if (!selectedProjectId.value) {
    return
  }
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await axios.post(`/api/projects/${selectedProjectId.value}/requirements`, reqForm)
    success.value = '需求创建成功。'
    reqForm.title = ''
    reqForm.summary = ''
    reqForm.priority = 'P2'
    reqForm.status = 'DRAFT'
    await loadRequirements(selectedProjectId.value)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '创建需求失败。'
  } finally {
    loading.value = false
  }
}

async function selectProject(projectId: number) {
  selectedProjectId.value = projectId
  selectedRequirementId.value = null
  activeWorkspaceTab.value = 'overview'
  editingProject.value = false
  resetProjectEditForm()
  resetProjectConversation()

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
  activeWorkspaceTab.value = 'requirements'
  editingProject.value = false
  resetProjectConversation()

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
  if (!selectedProjectId.value) {
    return
  }
  const userId = Number(memberForm.selectedUserId || memberForm.userId)
  if (!userId || userId <= 0) {
    error.value = '请输入有效的用户 ID。'
    return
  }

  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await axios.post(`/api/projects/${selectedProjectId.value}/members`, {
      userId,
      projectRole: memberForm.projectRole
    })
    success.value = '项目成员添加成功。'
    memberForm.selectedUserId = ''
    memberForm.userId = ''
    await loadProjectMembers(selectedProjectId.value)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '添加项目成员失败。'
  } finally {
    loading.value = false
  }
}

async function removeProjectMember(userId: number) {
  if (!selectedProjectId.value) {
    return
  }
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await axios.delete(`/api/projects/${selectedProjectId.value}/members/${userId}`)
    success.value = '项目成员移除成功。'
    await loadProjectMembers(selectedProjectId.value)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '移除项目成员失败。'
  } finally {
    loading.value = false
  }
}

function openWorkbench(requirementId: number) {
  if (!selectedProjectId.value) {
    return
  }
  void router.push(`/requirements/${requirementId}/workbench?projectId=${selectedProjectId.value}`)
}

function openVersions(requirementId: number) {
  if (!selectedProjectId.value) {
    return
  }
  void router.push(`/requirements/${requirementId}/versions?projectId=${selectedProjectId.value}`)
}

function goCreateAi() {
  void router.push('/projects/create-ai')
}

function goAiDocgen() {
  if (selectedRequirement.value && selectedProjectId.value) {
    void router.push(`/requirements/${selectedRequirement.value.id}/workbench?projectId=${selectedProjectId.value}`)
    return
  }
  void router.push('/docgen')
}

watch(
  () => route.query.projectId,
  async (value) => {
    const projectId = Number(value || 0)
    if (projectId > 0 && projects.value.some((project) => project.id === projectId) && selectedProjectId.value !== projectId) {
      await selectProject(projectId)
    }
  }
)

onMounted(async () => {
  await Promise.all([loadProjects(), loadUserOptions()])
})
</script>

<style scoped>
.page {
  max-width: 1560px;
  margin: 18px auto;
  padding: 0 14px 18px;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  color: #111827;
  background: #f4f7fb;
}
.layout {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}
.content {
  min-width: 0;
  display: grid;
  gap: 14px;
}
.workspace-stack {
  display: grid;
  gap: 14px;
}
.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.info-block {
  border: 1px solid #d4dde8;
  border-radius: 8px;
  padding: 14px;
  background: #ffffff;
}
.info-block h4 {
  margin: 0 0 10px;
  font-size: 16px;
  color: #0f172a;
}
.info-block p {
  margin: 0 0 6px;
  color: #475569;
  line-height: 1.6;
  white-space: pre-wrap;
}
.feedback-stack {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}
@media (max-width: 1280px) {
  .layout {
    grid-template-columns: 320px minmax(0, 1fr);
  }
}
@media (max-width: 980px) {
  .layout {
    grid-template-columns: 1fr;
  }
  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>

