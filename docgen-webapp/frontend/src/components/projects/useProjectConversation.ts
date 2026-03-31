import { computed, ref, type ComputedRef, type Ref } from 'vue'
import axios from 'axios'
import type {
  ApiResponse,
  ProjectConversationView,
  ProjectEditFormState,
  ProjectItem,
  ProjectKnowledgeDocumentListItem,
  ProjectKnowledgePreview
} from './types'

type ConversationDeps = {
  selectedProject: ComputedRef<ProjectItem | null>
  projectEditForm: ProjectEditFormState
  error: Ref<string>
  success: Ref<string>
}

export function useProjectConversation({ selectedProject, projectEditForm, error, success }: ConversationDeps) {
  const projectConversationLoading = ref(false)
  const projectConversationInput = ref('')
  const projectConversation = ref<ProjectConversationView | null>(null)
  const aiUpdatedFields = ref<Array<keyof ProjectEditFormState>>([])
  const projectMaterialKnowledgeMap = ref<Record<number, ProjectKnowledgeDocumentListItem[]>>({})
  const projectKnowledgePreviewVisible = ref(false)
  const projectKnowledgePreviewLoading = ref(false)
  const projectKnowledgePreview = ref<ProjectKnowledgePreview | null>(null)

  const canSendProjectConversation = computed(() => !!projectConversationInput.value.trim())
  const projectKnowledgePreviewQueryText = computed(() => {
    return (
      projectKnowledgePreview.value?.query ||
      projectConversationInput.value.trim() ||
      projectEditForm.projectName ||
      selectedProject.value?.projectName ||
      ''
    )
  })

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
      success.value = '项目 AI 已生成新的字段建议。'
    } catch (e: any) {
      error.value = e?.response?.data?.message || e?.message || '发送项目 AI 消息失败。'
    } finally {
      projectConversationLoading.value = false
    }
  }

  function applyConversationStructuredInfo(selectedFields?: Array<keyof ProjectEditFormState>) {
    const structured = projectConversation.value?.structuredInfo
    if (!structured) {
      error.value = '当前没有可应用的 AI 结果。'
      return
    }

    const nextFields: Array<keyof ProjectEditFormState> = []
    const fieldMap: Array<[keyof ProjectEditFormState, string | undefined]> = [
      ['projectName', structured.projectName],
      ['description', structured.description],
      ['projectBackground', structured.projectBackground],
      ['similarProducts', structured.similarProducts],
      ['targetCustomerGroups', structured.targetCustomerGroups],
      ['commercialValue', structured.commercialValue],
      ['coreProductValue', structured.coreProductValue]
    ]
    const selectedSet = new Set(selectedFields || fieldMap.map(([field]) => field))

    fieldMap.forEach(([field, incomingValue]) => {
      if (!selectedSet.has(field)) {
        return
      }
      const normalizedValue = incomingValue?.trim()
      if (!normalizedValue) {
        return
      }
      if (projectEditForm[field] !== normalizedValue) {
        projectEditForm[field] = normalizedValue
        nextFields.push(field)
      }
    })

    aiUpdatedFields.value = nextFields
    success.value = nextFields.length
      ? `AI 已应用 ${nextFields.length} 个字段变更，请确认后保存。`
      : '没有发现需要应用的字段变更。'
  }

  function resetProjectConversation() {
    projectConversation.value = null
    projectConversationInput.value = ''
    aiUpdatedFields.value = []
    projectMaterialKnowledgeMap.value = {}
    projectKnowledgePreviewVisible.value = false
    projectKnowledgePreviewLoading.value = false
    projectKnowledgePreview.value = null
  }

  function clearAiUpdatedFields() {
    aiUpdatedFields.value = []
  }

  return {
    projectConversationLoading,
    projectConversationInput,
    projectConversation,
    aiUpdatedFields,
    projectMaterialKnowledgeMap,
    projectKnowledgePreviewVisible,
    projectKnowledgePreviewLoading,
    projectKnowledgePreview,
    canSendProjectConversation,
    projectKnowledgePreviewQueryText,
    refreshProjectMaterialKnowledgeStatuses,
    ensureProjectConversation,
    loadProjectKnowledgePreview,
    sendProjectConversation,
    applyConversationStructuredInfo,
    clearAiUpdatedFields,
    resetProjectConversation
  }
}
