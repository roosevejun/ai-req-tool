<template>
  <div class="page">
    <div class="layout">
      <ProjectsSidebar
        :loading="loading"
        :project-ai-loading="projectAiLoading"
        :can-guide-project-info="canGuideProjectInfo"
        :has-project-ai-suggestions="hasProjectAiSuggestions"
        :project-ai-message="projectAiMessage"
        :project-ai-questions="projectAiQuestions"
        :project-ai-answers="projectAiAnswers"
        :project-ai-suggestions="projectAiSuggestions"
        :project-form="projectForm"
        :projects="projects"
        :user-options="userOptions"
        :selected-project-id="selectedProjectId"
        :selected-requirement-id="selectedRequirementId"
        :is-expanded="isExpanded"
        :requirements-of="requirementsOf"
        :versions-of="versionsOf"
        @guide-project-info="guideProjectProductInfo"
        @apply-ai-suggestions="applyProjectAiSuggestions"
        @create-project="createProject"
        @reload-projects="loadProjects"
        @toggle-project="toggleProject"
        @select-project="selectProject"
        @select-requirement="selectRequirement"
      />

      <main class="content">
        <section class="card quick-actions">
          <div class="section-head">
            <h3>快捷操作</h3>
            <p class="summary">常用入口放在这里，避免和顶部全局导航重复。</p>
          </div>
          <div class="row">
            <button class="primary" @click="goCreateAi">AI 创建项目</button>
            <button class="ghost" @click="goAiDocgen">进入 AI 需求整理</button>
          </div>
        </section>

        <section v-if="!selectedProject" class="card empty-state">
          <h3>请选择一个项目</h3>
          <p>你可以先在左侧创建项目，再逐条新增需求，并进入 AI 需求整理与 PRD 生成流程。</p>
        </section>

        <template v-else>
          <ProjectDetailsCard
            :loading="loading"
            :editing-project="editingProject"
            :project="selectedProject"
            :project-edit-form="projectEditForm"
            :user-options="userOptions"
            :project-type-label="projectTypeLabel"
            :visibility-label="visibilityLabel"
            :project-status-label="projectStatusLabel"
            @start-edit="startEditProject"
            @cancel-edit="cancelProjectEdit"
            @save-edit="saveProjectEdit"
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

          <ProjectRequirementsCard
            :loading="loading"
            :project="selectedProject"
            :req-form="reqForm"
            :requirements="requirementsOf(selectedProject.id)"
            :selected-requirement="selectedRequirement"
            :priority-label="priorityLabel"
            :requirement-status-label="requirementStatusLabel"
            @create-requirement="createRequirement"
            @select-requirement="selectRequirement"
            @open-workbench="openWorkbench"
            @open-versions="openVersions"
          />
        </template>
      </main>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="success" class="success">{{ success }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import axios from 'axios'
import { useRoute, useRouter } from 'vue-router'
import ProjectDetailsCard from '../components/projects/ProjectDetailsCard.vue'
import ProjectMembersCard from '../components/projects/ProjectMembersCard.vue'
import ProjectRequirementsCard from '../components/projects/ProjectRequirementsCard.vue'
import ProjectsSidebar from '../components/projects/ProjectsSidebar.vue'
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
  MemberFormState,
  ProjectAiSuggestions,
  ProjectEditFormState,
  ProjectFormState,
  ProjectItem,
  ProjectMemberItem,
  ProjectProductGuideAnswer,
  ProjectProductGuideResult,
  RequirementFormState,
  RequirementItem,
  UserOption,
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

const projectAiLoading = ref(false)
const projectAiMessage = ref('')
const projectAiQuestions = ref<string[]>([])
const projectAiAnswers = ref<string[]>([])
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

const selectedProject = computed(() => projects.value.find((project) => project.id === selectedProjectId.value) || null)
const selectedRequirement = computed(() => {
  if (!selectedProjectId.value || !selectedRequirementId.value) {
    return null
  }
  return requirementsOf(selectedProjectId.value).find((item) => item.id === selectedRequirementId.value) || null
})
const canGuideProjectInfo = computed(() => !!(projectForm.projectName || projectForm.description || projectForm.projectBackground))
const hasProjectAiSuggestions = computed(() => Object.values(projectAiSuggestions).some(Boolean))

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
    error.value = e?.response?.data?.message || e?.message || '加载项目失败'
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
    success.value = '项目创建成功'
    const createdProjectId = res.data.data
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
  if (!selectedProject.value) {
    return
  }
  fillProjectEditForm(selectedProject.value)
  editingProject.value = true
  error.value = ''
  success.value = ''
}

function cancelProjectEdit() {
  editingProject.value = false
  if (selectedProject.value) {
    fillProjectEditForm(selectedProject.value)
    return
  }
  resetProjectEditForm()
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
    success.value = '项目删除成功'
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
    error.value = e?.response?.data?.message || e?.message || '删除项目失败'
  } finally {
    loading.value = false
  }
}

async function saveProjectEdit() {
  if (!selectedProject.value) {
    return
  }
  if (!projectEditForm.projectName.trim()) {
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

async function createRequirement() {
  if (!selectedProjectId.value) {
    return
  }
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
  if (!selectedProjectId.value) {
    return
  }
  const userId = Number(memberForm.selectedUserId || memberForm.userId)
  if (!userId || userId <= 0) {
    error.value = '请输入有效的用户 ID'
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
  if (!selectedProjectId.value) {
    return
  }
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
  max-width: 1320px;
  margin: 18px auto;
  padding: 0 14px 18px;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  color: #111827;
}
.layout {
  display: grid;
  grid-template-columns: 350px 1fr;
  gap: 14px;
}
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
.quick-actions .summary {
  margin: 0;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.row {
  display: flex;
  gap: 10px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.primary,
.ghost {
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
.ghost {
  background: #f3f4f6;
}
.summary {
  font-size: 13px;
  color: #4b5563;
}
.empty-state {
  min-height: 180px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.error {
  margin-top: 8px;
  color: #b91c1c;
}
.success {
  margin-top: 8px;
  color: #166534;
}
@media (max-width: 980px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
