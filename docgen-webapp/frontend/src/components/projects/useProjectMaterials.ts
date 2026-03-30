import { computed, reactive, ref, type Ref } from 'vue'
import axios from 'axios'
import type {
  FileDraftState,
  ProjectConversationView,
  ProjectKnowledgeDocumentListItem,
  ProjectSourceMaterial,
  TextDraftState,
  UrlDraftState
} from './types'

type MaterialsDeps = {
  projectConversation: Ref<ProjectConversationView | null>
  projectConversationLoading: Ref<boolean>
  projectMaterialKnowledgeMap: Ref<Record<number, ProjectKnowledgeDocumentListItem[]>>
  error: Ref<string>
  success: Ref<string>
  ensureProjectConversation: () => Promise<void>
}

export function useProjectMaterials({
  projectConversation,
  projectConversationLoading,
  projectMaterialKnowledgeMap,
  error,
  success,
  ensureProjectConversation
}: MaterialsDeps) {
  const projectPendingMaterials = ref<ProjectSourceMaterial[]>([])
  const projectSelectedFile = ref<File | null>(null)
  const projectMaterialFilter = ref<'ALL' | 'URL' | 'TEXT' | 'FILE'>('ALL')
  const projectMaterialsCollapsed = ref(false)
  const projectUrlDraft = reactive<UrlDraftState>({ title: '', sourceUri: '' })
  const projectTextDraft = reactive<TextDraftState>({ title: '', rawContent: '' })
  const projectFileDraft = reactive<FileDraftState>({ title: '' })

  const canSaveProjectMaterials = computed(() => !!projectConversation.value?.sessionId && projectPendingMaterials.value.length > 0)
  const canUploadProjectFile = computed(() => !!projectConversation.value?.sessionId && !!projectSelectedFile.value)
  const filteredProjectMaterials = computed(() => {
    const materials = projectConversation.value?.materials || []
    if (projectMaterialFilter.value === 'ALL') {
      return materials
    }
    return materials.filter((item) => item.materialType === projectMaterialFilter.value)
  })

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

  function resetProjectMaterials() {
    projectPendingMaterials.value = []
    projectSelectedFile.value = null
    projectMaterialFilter.value = 'ALL'
    projectMaterialsCollapsed.value = false
    projectUrlDraft.title = ''
    projectUrlDraft.sourceUri = ''
    projectTextDraft.title = ''
    projectTextDraft.rawContent = ''
    projectFileDraft.title = ''
    projectMaterialKnowledgeMap.value = {}
  }

  return {
    projectPendingMaterials,
    projectSelectedFile,
    projectMaterialFilter,
    projectMaterialsCollapsed,
    projectUrlDraft,
    projectTextDraft,
    projectFileDraft,
    canSaveProjectMaterials,
    canUploadProjectFile,
    filteredProjectMaterials,
    projectMaterialPreview,
    projectKnowledgeStatusText,
    handleProjectFileSelect,
    addProjectUrlMaterial,
    addProjectTextMaterial,
    clearProjectPendingMaterials,
    saveProjectMaterials,
    uploadProjectFileMaterial,
    deleteProjectMaterial,
    resetProjectMaterials
  }
}
