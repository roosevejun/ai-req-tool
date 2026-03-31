import { computed, type ComputedRef, type Ref } from 'vue'
import { getProjectCompleteness, getProjectHealthTone } from './projectHealth'
import type {
  ProjectConversationView,
  ProjectItem,
  ProjectKnowledgeDocumentListItem,
  ProjectMemberItem,
  RequirementItem,
  WorkspaceTab
} from './types'

type AdviceDeps = {
  selectedProject: ComputedRef<ProjectItem | null>
  selectedRequirements: ComputedRef<RequirementItem[]>
  selectedMembers: ComputedRef<ProjectMemberItem[]>
  activeWorkspaceTab: Ref<WorkspaceTab>
  projectConversation: Ref<ProjectConversationView | null>
  projectMaterialKnowledgeMap: Ref<Record<number, ProjectKnowledgeDocumentListItem[]>>
}

export function useProjectWorkspaceAdvice({
  selectedProject,
  selectedRequirements,
  selectedMembers,
  activeWorkspaceTab,
  projectConversation,
  projectMaterialKnowledgeMap
}: AdviceDeps) {
  const projectCompleteness = computed(() => getProjectCompleteness(selectedProject.value))

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

  const pendingKnowledgeCount = computed(() => {
    return uniqueKnowledgeDocuments.value.filter((doc) => {
      return ['PENDING', 'RUNNING', 'PROCESSING'].includes(doc.status || '') || ['PENDING', 'RUNNING', 'PROCESSING'].includes(doc.latestTaskStatus || '')
    }).length
  })

  const failedKnowledgeCount = computed(() => {
    return uniqueKnowledgeDocuments.value.filter((doc) => doc.status === 'FAILED' || doc.latestTaskStatus === 'FAILED').length
  })

  const recommendedWorkspaceTab = computed<WorkspaceTab>(() => {
    if (!selectedProject.value) {
      return 'overview'
    }
    if (selectedMembers.value.length === 0) {
      return 'collaboration'
    }
    if (failedKnowledgeCount.value > 0 || pendingKnowledgeCount.value > 0) {
      return 'materials'
    }
    if (projectCompleteness.value.score < 75 || !projectConversation.value?.sessionId) {
      return 'ai'
    }
    return 'overview'
  })

  const nextActionTitle = computed(() => {
    if (!selectedProject.value) {
      return '请选择一个项目'
    }
    if (failedKnowledgeCount.value > 0) {
      return '先处理失败的知识任务'
    }
    if (pendingKnowledgeCount.value > 0) {
      return '先确认资料知识处理进度'
    }
    if (selectedMembers.value.length === 0) {
      return '先补充项目成员'
    }
    if (projectCompleteness.value.missingFieldLabels.length > 0) {
      return '继续补全项目信息'
    }
    if (selectedRequirements.value.length === 0) {
      return '可以开始创建第一条需求'
    }
    return '继续推进需求澄清与版本沉淀'
  })

  const nextActionDescription = computed(() => {
    if (!selectedProject.value) {
      return '先在左侧选择一个项目，系统会自动切换到当前最值得处理的工作区。'
    }
    if (failedKnowledgeCount.value > 0) {
      return `当前有 ${failedKnowledgeCount.value} 个知识处理任务失败，建议先进入“资料知识”查看失败原因并重试。`
    }
    if (pendingKnowledgeCount.value > 0) {
      return `当前有 ${pendingKnowledgeCount.value} 个知识任务仍在处理中，进入“资料知识”可以确认上下文是否已经可供 AI 使用。`
    }
    if (selectedMembers.value.length === 0) {
      return '项目还没有负责人或核心协作成员，建议先进入“团队协作”补充成员信息。'
    }
    if (projectCompleteness.value.missingFieldLabels.length > 0) {
      return `当前仍缺少 ${projectCompleteness.value.missingFieldLabels.slice(0, 3).join('、')} 等关键信息，建议进入“AI 补全”完善项目信息。`
    }
    if (selectedRequirements.value.length === 0) {
      return '项目概览已经比较完整，可以从项目信息出发直接创建第一条需求。'
    }
    return '项目基础、团队协作、AI 补全和资料知识已经形成闭环，可以继续围绕需求推进澄清、生成和版本管理。'
  })

  const workspaceAdvice = computed(() => {
    if (!selectedProject.value) {
      return '先在左侧选择一个项目，右侧会切换到项目工作台，并给出当前最值得处理的动作。'
    }
    if (activeWorkspaceTab.value === 'collaboration') {
      return '在“团队协作”工作区先确认负责人、产品、开发等核心角色，避免后续资料、AI 补全和需求无人维护。'
    }
    if (activeWorkspaceTab.value === 'overview' && selectedRequirements.value.length === 0) {
      return '当前已进入“项目概览”，建议先确认基础信息和产品背景，再创建第一条需求。'
    }
    if (activeWorkspaceTab.value === 'ai' && !projectConversation.value?.sessionId) {
      return '当前还没有 AI 会话，建议先启动会话，用自然语言说明项目背景、客户和价值，让 AI 生成待确认建议。'
    }
    if (failedKnowledgeCount.value > 0) {
      return `当前有 ${failedKnowledgeCount.value} 个知识任务失败，建议优先进入“资料知识”处理异常，避免 AI 使用不完整上下文。`
    }
    if (activeWorkspaceTab.value === 'materials') {
      return '“资料知识”工作区的目标是为 AI 补全提供高质量上下文，不是单纯堆资料。优先关注处理状态和失败原因。'
    }
    return '当前可以继续与 AI 协同，把建议确认后回填到项目表单，再基于更完整的项目信息推进需求工作台。'
  })

  return {
    projectCompleteness,
    recommendedWorkspaceTab,
    nextActionTitle,
    nextActionDescription,
    healthTone: computed(() => getProjectHealthTone(projectCompleteness.value.score)),
    pendingKnowledgeCount,
    failedKnowledgeCount,
    workspaceAdvice
  }
}
