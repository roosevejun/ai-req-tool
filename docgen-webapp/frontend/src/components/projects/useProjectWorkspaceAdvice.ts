import { computed, type ComputedRef, type Ref } from 'vue'
import type { ProjectConversationView, ProjectItem, ProjectKnowledgeDocumentListItem } from './types'

type WorkspaceTab = 'overview' | 'ai' | 'materials'

type AdviceDeps = {
  selectedProject: ComputedRef<ProjectItem | null>
  activeWorkspaceTab: Ref<WorkspaceTab>
  projectConversation: Ref<ProjectConversationView | null>
  projectMaterialKnowledgeMap: Ref<Record<number, ProjectKnowledgeDocumentListItem[]>>
}

export function useProjectWorkspaceAdvice({
  selectedProject,
  activeWorkspaceTab,
  projectConversation,
  projectMaterialKnowledgeMap
}: AdviceDeps) {
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

  const workspaceAdvice = computed(() => {
    if (!selectedProject.value) {
      return '先在左侧选择一个项目，右侧会切换到项目概览、AI 协同和资料知识工作区。'
    }
    if (activeWorkspaceTab.value === 'overview') {
      return '先看清当前项目信息是否缺失，再进入 AI 补全区，通过沟通补足背景、客户和价值。'
    }
    if (activeWorkspaceTab.value === 'ai' && !projectConversation.value?.sessionId) {
      return '当前还没有 AI 会话，建议先启动会话，用自然语言告诉 AI 这个项目做什么、面向谁、为什么值得做。'
    }
    if (failedKnowledgeCount.value > 0) {
      return `当前有 ${failedKnowledgeCount.value} 个知识任务失败，建议优先进入“资料与知识”工作区处理异常。`
    }
    if (activeWorkspaceTab.value === 'materials') {
      return '资料页的作用是给 AI 补全提供上下文，不是替代项目维护本身。'
    }
    return '当前可以继续与 AI 沟通，并把整理后的结果回填到项目信息表单再保存。'
  })

  return {
    pendingKnowledgeCount,
    failedKnowledgeCount,
    workspaceAdvice
  }
}
