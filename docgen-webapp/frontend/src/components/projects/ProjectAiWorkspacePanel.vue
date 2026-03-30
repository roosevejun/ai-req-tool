<template>
  <WorkspaceSection
    eyebrow="AI 协同"
    title="AI 辅助维护项目信息"
    description="这里的核心任务不是单独聊天，而是通过沟通让 AI 帮你补全项目描述，再把结果回填到项目信息表单。"
    tint
  >
    <div class="panel-grid">
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
        @apply-project-ai="$emit('apply-project-ai')"
        @update-project-ai-input="$emit('update-project-ai-input', $event)"
        @load-project-knowledge-preview="$emit('load-project-knowledge-preview')"
      />
    </div>
  </WorkspaceSection>
</template>

<script setup lang="ts">
import ProjectConversationPanel from './ProjectConversationPanel.vue'
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
}>()

defineEmits<{
  (event: 'cancel-edit'): void
  (event: 'save-edit'): void
  (event: 'delete-project'): void
  (event: 'refresh-project-ai'): void
  (event: 'send-project-ai'): void
  (event: 'apply-project-ai'): void
  (event: 'update-project-ai-input', value: string): void
  (event: 'load-project-knowledge-preview'): void
}>()
</script>

<style scoped>
.panel-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

@media (max-width: 1180px) {
  .panel-grid {
    grid-template-columns: 1fr;
  }
}
</style>
