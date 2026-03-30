import { computed, ref, type Ref } from 'vue'
import axios from 'axios'
import type {
  ApiResponse,
  ProjectKnowledgeDocumentChunk,
  ProjectKnowledgeDocumentDetail
} from './types'

export function useProjectKnowledgeDetail(error: Ref<string>, success: Ref<string>, afterReprocess: () => Promise<void>) {
  const projectKnowledgeDetailVisible = ref(false)
  const projectKnowledgeDetailLoading = ref(false)
  const projectKnowledgeReprocessing = ref(false)
  const projectKnowledgeDetail = ref<ProjectKnowledgeDocumentDetail | null>(null)
  const projectKnowledgeChunkExpanded = ref(false)

  const visibleProjectKnowledgeChunks = computed<ProjectKnowledgeDocumentChunk[]>(() => {
    const chunks = projectKnowledgeDetail.value?.chunks || []
    return projectKnowledgeChunkExpanded.value ? chunks : chunks.slice(0, 3)
  })

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
      await afterReprocess()
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

  return {
    projectKnowledgeDetailVisible,
    projectKnowledgeDetailLoading,
    projectKnowledgeReprocessing,
    projectKnowledgeDetail,
    projectKnowledgeChunkExpanded,
    visibleProjectKnowledgeChunks,
    openProjectKnowledgeDetail,
    reprocessProjectKnowledgeDetail,
    closeProjectKnowledgeDetail
  }
}
