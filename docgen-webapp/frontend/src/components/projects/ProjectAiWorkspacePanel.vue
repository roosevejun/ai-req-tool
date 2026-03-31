<template>
  <WorkspaceSection
    eyebrow="AI 补全"
    title="AI 结构化补全"
    description="先和 AI 对话，再逐项确认需要变更的字段。"
    tint
  >
    <div class="workspace-flow">
      <ProjectConversationPanel
        :project-conversation-loading="projectConversationLoading"
        :project-conversation="projectConversation"
        :project-conversation-input="projectConversationInput"
        :can-send-project-conversation="canSendProjectConversation"
        :project-knowledge-preview-visible="projectKnowledgePreviewVisible"
        :project-knowledge-preview-loading="projectKnowledgePreviewLoading"
        :project-knowledge-preview="projectKnowledgePreview"
        :project-knowledge-preview-query-text="projectKnowledgePreviewQueryText"
        @refresh-project-ai="$emit('refresh-project-ai')"
        @send-project-ai="$emit('send-project-ai')"
        @update-project-ai-input="$emit('update-project-ai-input', $event)"
        @load-project-knowledge-preview="$emit('load-project-knowledge-preview')"
      />

      <ProjectAiProposalPanel
        :loading="projectConversationLoading"
        :project-conversation="projectConversation"
        :project-edit-form="projectEditForm"
        @apply-project-ai="$emit('apply-project-ai', $event)"
      />

      <ProjectEditPanel
        :loading="loading"
        :project="project"
        :project-edit-form="projectEditForm"
        :ai-updated-fields="aiUpdatedFields"
        :user-options="userOptions"
        @cancel-edit="$emit('cancel-edit')"
        @save-edit="$emit('save-edit')"
        @delete-project="$emit('delete-project')"
      />
    </div>
  </WorkspaceSection>
</template>

<script setup lang="ts">
import ProjectConversationPanel from './ProjectConversationPanel.vue'
import ProjectAiProposalPanel from './ProjectAiProposalPanel.vue'
import ProjectEditPanel from './ProjectEditPanel.vue'
import WorkspaceSection from './WorkspaceSection.vue'
import type {
  ProjectConversationView,
  ProjectEditFormState,
  ProjectItem,
  ProjectKnowledgePreview,
  UserOption
} from './types'

defineProps<{
  loading: boolean
  project: ProjectItem
  projectEditForm: ProjectEditFormState
  aiUpdatedFields: Array<keyof ProjectEditFormState>
  projectConversationLoading: boolean
  projectConversation: ProjectConversationView | null
  projectConversationInput: string
  canSendProjectConversation: boolean
  projectKnowledgePreviewVisible: boolean
  projectKnowledgePreviewLoading: boolean
  projectKnowledgePreview: ProjectKnowledgePreview | null
  projectKnowledgePreviewQueryText: string
  userOptions: UserOption[]
  completenessScore: number
  missingFieldLabels: string[]
}>()

defineEmits<{
  (event: 'cancel-edit'): void
  (event: 'save-edit'): void
  (event: 'delete-project'): void
  (event: 'refresh-project-ai'): void
  (event: 'send-project-ai'): void
  (event: 'apply-project-ai', fields: Array<keyof ProjectEditFormState>): void
  (event: 'update-project-ai-input', value: string): void
  (event: 'load-project-knowledge-preview'): void
}>()
</script>

<style scoped>
.workspace-flow {
  display: grid;
  gap: 14px;
}
</style>
