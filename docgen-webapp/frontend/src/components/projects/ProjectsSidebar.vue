<template>
  <aside class="sidebar">
    <ProjectCreateCard
      :loading="loading"
      :project-ai-loading="projectAiLoading"
      :can-guide-project-info="canGuideProjectInfo"
      :has-project-ai-suggestions="hasProjectAiSuggestions"
      :project-ai-message="projectAiMessage"
      :project-ai-questions="projectAiQuestions"
      :project-ai-answers="projectAiAnswers"
      :project-ai-suggestions="projectAiSuggestions"
      :project-form="projectForm"
      :user-options="userOptions"
      @guide-project-info="$emit('guide-project-info')"
      @apply-ai-suggestions="$emit('apply-ai-suggestions')"
      @create-project="$emit('create-project')"
    />

    <ProjectTreeCard
      :loading="loading"
      :projects="projects"
      :selected-project-id="selectedProjectId"
      :selected-requirement-id="selectedRequirementId"
      :is-expanded="isExpanded"
      :requirements-of="requirementsOf"
      :versions-of="versionsOf"
      @reload-projects="$emit('reload-projects')"
      @toggle-project="$emit('toggle-project', $event)"
      @select-project="$emit('select-project', $event)"
      @select-requirement="(projectId, requirementId) => $emit('select-requirement', projectId, requirementId)"
    />
  </aside>
</template>

<script setup lang="ts">
import ProjectCreateCard from './ProjectCreateCard.vue'
import ProjectTreeCard from './ProjectTreeCard.vue'
import type {
  ProjectAiSuggestions,
  ProjectFormState,
  ProjectItem,
  RequirementItem,
  UserOption,
  VersionItem
} from './types'

defineProps<{
  loading: boolean
  projectAiLoading: boolean
  canGuideProjectInfo: boolean
  hasProjectAiSuggestions: boolean
  projectAiMessage: string
  projectAiQuestions: string[]
  projectAiAnswers: string[]
  projectAiSuggestions: ProjectAiSuggestions
  projectForm: ProjectFormState
  projects: ProjectItem[]
  userOptions: UserOption[]
  selectedProjectId: number | null
  selectedRequirementId: number | null
  isExpanded: (projectId: number) => boolean
  requirementsOf: (projectId: number) => RequirementItem[]
  versionsOf: (requirementId: number) => VersionItem[]
}>()

defineEmits<{
  (event: 'guide-project-info'): void
  (event: 'apply-ai-suggestions'): void
  (event: 'create-project'): void
  (event: 'reload-projects'): void
  (event: 'toggle-project', projectId: number): void
  (event: 'select-project', projectId: number): void
  (event: 'select-requirement', projectId: number, requirementId: number): void
}>()
</script>

<style scoped>
.sidebar {
  min-height: 70vh;
}
</style>
