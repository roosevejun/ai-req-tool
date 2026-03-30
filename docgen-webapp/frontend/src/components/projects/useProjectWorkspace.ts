import { computed, ref, type ComputedRef, type Ref } from 'vue'
import axios from 'axios'
import type {
  ApiResponse,
  MemberFormState,
  ProjectEditFormState,
  ProjectItem,
  ProjectMemberItem,
  RequirementItem,
  UserOption
} from './types'
import { assignProjectEditForm, createProjectEditFormState, fillProjectEditForm } from './projectWorkspaceForm'

type WorkspaceDeps = {
  routeProjectId: ComputedRef<number | null>
  error: Ref<string>
  success: Ref<string>
  projectEditForm: ProjectEditFormState
  memberForm: MemberFormState
  resetProjectConversation: () => void
  resetProjectMaterials: () => void
  closeProjectKnowledgeDetail: () => void
}

export function useProjectWorkspace({
  routeProjectId,
  error,
  success,
  projectEditForm,
  memberForm,
  resetProjectConversation,
  resetProjectMaterials,
  closeProjectKnowledgeDetail
}: WorkspaceDeps) {
  const loading = ref(false)
  const projects = ref<ProjectItem[]>([])
  const requirementsMap = ref<Record<number, RequirementItem[]>>({})
  const projectMembersMap = ref<Record<number, ProjectMemberItem[]>>({})
  const userOptions = ref<UserOption[]>([])
  const selectedProjectId = ref<number | null>(null)
  const activeWorkspaceTab = ref<'overview' | 'ai' | 'materials'>('overview')

  const selectedProject = computed(() => projects.value.find((project) => project.id === selectedProjectId.value) || null)

  function resetProjectEditForm() {
    assignProjectEditForm(projectEditForm, createProjectEditFormState())
  }

  function requirementsOf(projectId: number): RequirementItem[] {
    return requirementsMap.value[projectId] || []
  }

  function membersOf(projectId: number): ProjectMemberItem[] {
    return projectMembersMap.value[projectId] || []
  }

  function resetWorkspaceState() {
    resetProjectEditForm()
    resetProjectConversation()
    resetProjectMaterials()
    closeProjectKnowledgeDetail()
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

  async function selectProject(projectId: number) {
    selectedProjectId.value = projectId
    activeWorkspaceTab.value = 'overview'
    resetWorkspaceState()

    await Promise.all([loadRequirements(projectId), loadProjectMembers(projectId)])
    if (selectedProject.value) {
      fillProjectEditForm(projectEditForm, selectedProject.value)
    }
  }

  async function loadProjects() {
    loading.value = true
    error.value = ''
    try {
      const res = await axios.get<ApiResponse<ProjectItem[]>>('/api/projects')
      projects.value = res.data.data || []

      const queryProjectId = routeProjectId.value || 0
      if (queryProjectId > 0 && projects.value.some((project) => project.id === queryProjectId)) {
        await selectProject(queryProjectId)
        return
      }

      if (selectedProjectId.value && projects.value.some((project) => project.id === selectedProjectId.value)) {
        if (selectedProject.value) {
          fillProjectEditForm(projectEditForm, selectedProject.value)
        }
        return
      }

      if (projects.value.length > 0) {
        await selectProject(projects.value[0].id)
        return
      }

      selectedProjectId.value = null
      resetWorkspaceState()
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
      delete requirementsMap.value[project.id]
      delete projectMembersMap.value[project.id]
      selectedProjectId.value = fallbackProjectId
      resetWorkspaceState()
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
      const projectId = selectedProject.value.id
      await loadProjects()
      await selectProject(projectId)
      activeWorkspaceTab.value = 'overview'
    } catch (e: any) {
      error.value = e?.response?.data?.message || e?.message || '更新项目信息失败。'
    } finally {
      loading.value = false
    }
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

  return {
    loading,
    projects,
    requirementsMap,
    projectMembersMap,
    userOptions,
    selectedProjectId,
    activeWorkspaceTab,
    selectedProject,
    resetProjectEditForm,
    requirementsOf,
    membersOf,
    loadProjects,
    loadUserOptions,
    loadRequirements,
    loadProjectMembers,
    selectProject,
    deleteProject,
    saveProjectEdit,
    addProjectMember,
    removeProjectMember
  }
}
