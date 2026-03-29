<template>
  <aside class="side-stack">
    <ProjectAiSetupCard
      ref="setupCardRef"
      :loading="loading"
      :session-id="sessionId"
      :start-form="startForm"
      :url-draft="urlDraft"
      :text-draft="textDraft"
      :file-draft="fileDraft"
      :selected-file="selectedFile"
      :can-start-conversation="canStartConversation"
      :pending-materials="pendingMaterials"
      :saved-materials="savedMaterials"
      :material-knowledge-map="materialKnowledgeMap"
      @add-url-material="$emit('add-url-material')"
      @add-text-material="$emit('add-text-material')"
      @clear-pending-materials="$emit('clear-pending-materials')"
      @start-conversation="$emit('start-conversation')"
      @save-materials="$emit('save-materials')"
      @upload-file="$emit('upload-file')"
      @file-selected="$emit('file-selected', $event)"
      @open-knowledge-detail="$emit('open-knowledge-detail', $event)"
      @retry-knowledge-document="$emit('retry-knowledge-document', $event)"
    />

    <FeedbackPanel title="下一步建议" :message="workspaceAdvice" tone="warning" />
    <FeedbackPanel title="处理提示" :message="error" tone="danger" />
    <FeedbackPanel title="最新进展" :message="success" tone="success" />
  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import FeedbackPanel from '../projects/FeedbackPanel.vue'
import ProjectAiSetupCard from './ProjectAiSetupCard.vue'
import type {
  FileDraftState,
  KnowledgeDocumentListItem,
  SourceMaterial,
  StartFormState,
  TextDraftState,
  UrlDraftState
} from './types'

const setupCardRef = ref<InstanceType<typeof ProjectAiSetupCard> | null>(null)

defineProps<{
  loading: boolean
  sessionId: number | null
  startForm: StartFormState
  urlDraft: UrlDraftState
  textDraft: TextDraftState
  fileDraft: FileDraftState
  selectedFile: File | null
  canStartConversation: boolean
  pendingMaterials: SourceMaterial[]
  savedMaterials: SourceMaterial[]
  materialKnowledgeMap: Record<number, KnowledgeDocumentListItem[]>
  workspaceAdvice: string
  error: string
  success: string
}>()

defineEmits<{
  (event: 'add-url-material'): void
  (event: 'add-text-material'): void
  (event: 'clear-pending-materials'): void
  (event: 'start-conversation'): void
  (event: 'save-materials'): void
  (event: 'upload-file'): void
  (event: 'file-selected', file: File | null): void
  (event: 'open-knowledge-detail', documentId: number): void
  (event: 'retry-knowledge-document', documentId: number): void
}>()

defineExpose({
  resetFileInput() {
    setupCardRef.value?.resetFileInput()
  }
})
</script>

<style scoped>
.side-stack {
  display: grid;
  gap: 14px;
  align-content: start;
}
</style>
